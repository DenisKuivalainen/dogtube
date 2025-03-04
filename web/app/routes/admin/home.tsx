import type { Route } from "./+types/home";
import { Welcome } from "../../welcome/welcome";
import {
  Box,
  Button,
  Card,
  CardContent,
  CardMedia,
  Grid,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router";
import { useEffect, useState } from "react";
import axios from "axios";
import { useAdminAuth } from "~/utils";
import { VideoCall } from "@mui/icons-material";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "New React Router App" },
    { name: "description", content: "Welcome to React Router!" },
  ];
}

export default () => {
  const adminAuth = useAdminAuth();
  const navigate = useNavigate();

  const [videos, setVideos] = useState<any[]>([]);

  useEffect(() => {
    adminAuth
      .axiosInstance()
      .get("/video")
      .then((res) => res.data)
      .then((res) => setVideos(res));
  }, []);

  return (
    <>
      <Button
        variant="contained"
        color="primary"
        onClick={() => navigate("/admin/video/add/v2")}
        sx={{ marginBottom: 2 }}
        startIcon={<VideoCall />}
      >
        Create
      </Button>{" "}
      <Grid container spacing={3}>
        {videos.map((record) => (
          <Grid item xs={12} sm={4} md={3} key={record.id}>
            <Card
              onClick={() => {
                if (record.status === "READY") {
                  navigate(`/admin/video/edit/${record.id}`);
                }
              }}
              style={{
                cursor: record.status === "READY" ? "pointer" : "default",
              }}
            >
              <CardMedia
                component="img"
                alt={record.name}
                height="240"
                image={`/api/admin/video/${record.id}/thumbnail`}
                onError={(e: any) => {
                  e.target.onerror = null;
                  e.target.src = "/default.png";
                }}
              />
              <CardContent>
                <Typography variant="h6" component="div">
                  {record.name}
                </Typography>
                <Typography
                  variant="body1"
                  color={record.isPremium ? "gold" : "text.secondary"}
                >
                  {record.isPremium ? "Premium" : "Free"}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Created at: {new Date(record.createdAt).toLocaleString()}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Uploaded at:{" "}
                  {isNaN(new Date(record.uploadedAt).valueOf())
                    ? "-"
                    : new Date(record.uploadedAt).toLocaleString()}
                </Typography>
                <Box mt={2}>
                  <Typography variant="body2" color="text.secondary">
                    Status:{" "}
                    <a
                      style={{
                        color:
                          record.status == "DELETING"
                            ? "red"
                            : record.status == "READY"
                            ? "green"
                            : "",
                      }}
                    >
                      {record.status}
                    </a>
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Views: {record.uniqueViews} unique, {record.allViews} total
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Likes: {record.likes}
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </>
  );
};
