import { useEffect, useState } from "react";
import type { Route } from "./+types/home";
import { useOutletContext, useSearchParams } from "react-router";
import axios from "axios";
import {
  Box,
  Card,
  CardContent,
  CardMedia,
  Grid,
  IconButton,
  Typography,
} from "@mui/material";
import { Favorite, Star, Visibility } from "@mui/icons-material";
import { axiosInstance, useAdminAuth, useUtils } from "~/utils";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Dogtube" },
    { name: "description", content: "Watch doggo videos!" },
  ];
}

export default () => {
  const { userData }: any = useOutletContext();
  const { redirect } = useUtils();

  const [videos, setVideos] = useState<any[]>([]);
  const [searchParams] = useSearchParams();
  const search = decodeURIComponent(searchParams.get("search") || "");
  const [videoClicked, setVideoClicked] = useState(false);

  const fetchVideos = async () => {
    const res = await axiosInstance.get(`/api/video?search=${search}`);

    setVideos(res.data);
  };

  useEffect(() => {
    fetchVideos();
  }, [search]);

  const handleVideoClick =
    (id: string, isPremiumVideo: boolean) => async () => {
      if (videoClicked) return;

      setVideoClicked(true);

      if (isPremiumVideo && userData.subscription_level != "PREMIUM") {
        redirect("/subscription");
      } else {
        await axiosInstance.post(`/api/video/${id}/view`).catch(() => {});
        redirect(`/${id}`);
      }
    };

  return (
    <Grid container spacing={2} style={{ padding: 16 }}>
      {videos.map((video) => (
        <Grid
          item
          xs={12}
          sm={6}
          md={3}
          key={video.id}
          onClick={handleVideoClick(video.id, video.isPremium)}
          style={{ cursor: "pointer" }}
        >
          <Card sx={{ position: "relative" }}>
            <CardMedia
              component="img"
              height="240"
              image={`/api/video/${video.id}/thumbnail`}
              alt={video.name}
              onError={(e: any) => {
                e.target.onerror = null;
                e.target.src = "/default.png";
              }}
            />
            <CardContent>
              <Grid container alignItems="center">
                {/* Star icon for premium videos */}
                {video.isPremium &&
                  userData.subscription_level !== "PREMIUM" && (
                    <Star color="secondary" style={{ marginRight: 8 }} />
                  )}

                {/* Video name */}
                <Typography variant="h6" component="div">
                  {video.name}
                </Typography>
              </Grid>

              {/* Views and Likes */}
              <Grid container spacing={2} alignItems="center">
                <Grid item>
                  <IconButton disabled>
                    <Visibility
                      sx={video.viewed ? { color: "primary.main" } : {}}
                    />
                  </IconButton>
                  <Typography variant="body2" component="span">
                    {video.views}
                  </Typography>
                </Grid>

                <Grid item>
                  <IconButton disabled>
                    <Favorite
                      sx={video.liked ? { color: "secondary.main" } : {}}
                    />
                  </IconButton>
                  <Typography variant="body2" component="span">
                    {video.likes}
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  );
};
