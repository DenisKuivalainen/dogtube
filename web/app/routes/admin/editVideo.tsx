import { useEffect, useState } from "react";
import type { Route } from "../+types/home";
import axios from "axios";
import { useAdminAuth } from "~/utils";
import { useNavigate, useParams } from "react-router";
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  TextField,
  Typography,
  Checkbox,
  Stack,
  Paper,
  CircularProgress,
} from "@mui/material";
import {
  Done,
  DoneRounded,
  KeyboardArrowLeftRounded,
} from "@mui/icons-material";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Edit a video" },
    { name: "description", content: "Edit a video" },
  ];
}

export default () => {
  const adminAuth = useAdminAuth();
  const { videoId } = useParams<{ videoId: string }>();
  const navigate = useNavigate();

  const [videoData, setVideoData] = useState<any>();

  useEffect(() => {
    axios
      .get(`http://0.0.0.0:4000/admin/video/${videoId}`, {
        headers: {
          Authorization: adminAuth.getAuthHeader(),
        },
      })
      .then((res) => res.data)
      .then((res) => setVideoData(res));
  }, []);

  const [isDeleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const handleDeleteDialogOpen = () => {
    setDeleteDialogOpen(true);
  };
  const handleDeleteDialogClose = () => {
    setDeleteDialogOpen(false);
  };

  const [isLoading, setIsLoading] = useState(false);
  const [loadingCompleted, setLoadingCompleted] = useState(false);
  const [loadingMsg, setLoadingMsg] = useState("");

  const handleDelete = async () => {
    try {
      setIsLoading(true);
      setLoadingMsg("Deleting...");

      await Promise.all([
        axios.delete(`http://0.0.0.0:4000/admin/video/${videoId}`, {
          headers: {
            Authorization: adminAuth.getAuthHeader(),
          },
        }),
        new Promise((resolve) => setTimeout(resolve, 1500)),
      ]);

      setLoadingCompleted(true);
      setLoadingMsg("Deletion completed!");
      await new Promise((resolve) => setTimeout(resolve, 3000));
      navigate("/admin");
    } catch (e: any) {
      setIsLoading(false);
      window.alert(e.message);
    }
  };

  return typeof window == "undefined" || !videoData ? (
    <></>
  ) : isLoading ? (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        justifyContent: "center",
        alignItems: "center",
        padding: 2,
        minHeight: "200px",
      }}
    >
      <Box
        sx={{
          position: "relative",
        }}
      >
        <CircularProgress
          size={100}
          {...(loadingCompleted ? { variant: "determinate", value: 100 } : {})}
        />
        {loadingCompleted ? (
          <DoneRounded
            sx={{
              position: "absolute",
              top: "50%",
              left: "50%",
              transform: "translate(-50%, -50%)",
              fontWeight: "bold",
            }}
            style={{
              height: 70,
              width: 70,
            }}
            color="primary"
          />
        ) : (
          <></>
        )}
      </Box>

      <Typography variant="h6" sx={{ color: "text.secondary" }}>
        {loadingMsg}
      </Typography>
    </Box>
  ) : (
    <>
      <Button
        color="primary"
        onClick={() => navigate(-1)}
        sx={{ marginBottom: 2 }}
        startIcon={<KeyboardArrowLeftRounded />}
      >
        Back
      </Button>
      <Paper
        elevation={3}
        sx={{ padding: 3, maxWidth: 400, mx: "auto", mt: 4, borderRadius: 2 }}
      >
        <Stack spacing={2}>
          <TextField
            label="Video Name"
            variant="outlined"
            fullWidth
            defaultValue={videoData?.name || ""}
            onChange={(e) =>
              setVideoData((prev) => ({
                ...prev,
                name: e.target.value,
              }))
            }
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={videoData?.isPremium}
                onChange={() =>
                  setVideoData((prev) => ({
                    ...prev,
                    isPremium: !prev.isPremium,
                  }))
                }
              />
            }
            label="Premium"
          />

          <Button variant="contained" color="primary" sx={{ marginBottom: 2 }}>
            Save Changes
          </Button>

          <Button
            variant="outlined"
            color="error"
            onClick={handleDeleteDialogOpen}
          >
            Delete Video
          </Button>

          <Dialog open={isDeleteDialogOpen} onClose={handleDeleteDialogClose}>
            <DialogTitle>Confirm Deletion</DialogTitle>
            <DialogContent>
              <Typography variant="body1">
                Are you sure you want to delete this video?
              </Typography>
            </DialogContent>
            <DialogActions>
              <Button onClick={handleDeleteDialogClose} color="primary">
                Cancel
              </Button>
              <Button
                onClick={() => {
                  handleDeleteDialogClose();
                  handleDelete();
                }}
                color="error"
              >
                Delete
              </Button>
            </DialogActions>
          </Dialog>
        </Stack>
      </Paper>
    </>
  );
};
