import { DoneRounded, KeyboardArrowLeftRounded } from "@mui/icons-material";
import {
  Box,
  Button,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import axios from "axios";
import { useState } from "react";
import { useNavigate } from "react-router";
import { useAdminAuth } from "~/utils";
import type { Route } from "./+types/home";

function shuffleArray<T>(array: T[]): T[] {
  for (let i = array.length - 1; i > 0; i--) {
    // Generate a random index
    const j = Math.floor(Math.random() * (i + 1));

    // Swap elements at indices i and j
    [array[i], array[j]] = [array[j], array[i]];
  }
  return array;
}

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Upload a new video" },
    { name: "description", content: "Upload a new video" },
  ];
}

export default () => {
  const adminAuth = useAdminAuth();
  const navigate = useNavigate();

  const [title, setTitle] = useState("");
  const [isPremium, setIsPremium] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const [isUploading, setIsUploading] = useState(false);
  const [loadingMsg, setLoadingMsg] = useState("");
  const [loadingCompleted, setLoadingCompleted] = useState(false);

  const handleUpload = async () => {
    try {
      if (!selectedFile) return;
      setIsUploading(true);
      setLoadingMsg("Uploading video...");

      const formData = new FormData();
      formData.append("name", title);
      formData.append("isPremium", isPremium ? "true" : "false");
      formData.append("file", selectedFile);

      await Promise.all([
        adminAuth.axiosInstance().post(`video/v2`, formData, {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        }),
        new Promise((resolve) => setTimeout(resolve, 1500)),
      ]);

      setLoadingCompleted(true);
      setLoadingMsg("Video was uploaded!");
      await new Promise((resolve) => setTimeout(resolve, 2000));
      navigate("/admin");
    } catch (e: any) {
      setIsUploading(false);
      console.log(e.response);
      window.alert(e?.response?.data || e.message);
    }
  };

  return isUploading ? (
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
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            label="Video Title"
            variant="outlined"
            fullWidth
          />

          <FormControlLabel
            control={
              <Checkbox
                checked={isPremium}
                onChange={() => setIsPremium((prev) => !prev)}
                color="primary"
              />
            }
            label="Premium Video"
          />

          <input
            type="file"
            accept="video/*"
            style={{ display: "none" }}
            id="video-upload"
            onChange={(e) => {
              if (e.target.files && e.target.files.length > 0) {
                setSelectedFile(e.target.files[0]);
                console.log(e.target.files[0]);
              }
            }}
          />
          <label htmlFor="video-upload">
            <Button
              variant="contained"
              color="secondary"
              fullWidth
              component="span"
            >
              {selectedFile ? selectedFile.name : "Select Video"}
            </Button>
          </label>

          <Button
            variant="contained"
            color="primary"
            fullWidth
            disabled={!title.length || !selectedFile}
            onClick={handleUpload}
          >
            Upload
          </Button>
        </Stack>
      </Paper>
    </>
  );
};
