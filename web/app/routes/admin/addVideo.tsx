import type { Route } from "./+types/home";
import { Welcome } from "../../welcome/welcome";
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
import { useState } from "react";
import axios from "axios";
import { getFirstFrameAsBlob, useAdminAuth } from "~/utils";
import { useNavigate } from "react-router";
import { KeyboardArrowLeftRounded } from "@mui/icons-material";

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
  const [uploadingStatus, setUploadingStatus] = useState(0);
  const [maxStatus, setMaxStatus] = useState(2);

  const handleUpload = async () => {
    const increaseStatus = () => setUploadingStatus((prev) => prev + 1);

    try {
      if (!selectedFile) return;
      setIsUploading(true);

      const { id: videoId, chunks } = await axios
        .post(
          "/api/admin/video",
          {
            name: title,
            isPremium,
            bufferSize: selectedFile.size,
            extension: selectedFile.name.split(".").pop() || "mp4",
          },
          {
            headers: {
              Authorization: adminAuth.getAuthHeader(),
            },
          }
        )
        .then((res) => res.data);

      setMaxStatus(chunks.length);

      for (const chunk of shuffleArray<{
        start: number;
        end: number;
        id: string;
      }>(chunks)) {
        const chunkData = selectedFile.slice(chunk.start, chunk.end);

        const chunkFormData = new FormData();
        chunkFormData.append("chunk", chunkData);
        chunkFormData.append("chunkId", chunk.id);

        await axios.put(`/api/admin/video/${videoId}`, chunkFormData, {
          headers: {
            "Content-Type": "multipart/form-data",
            Authorization: adminAuth.getAuthHeader(),
          },
        });
        increaseStatus();
      }

      await new Promise((resolve) => setTimeout(resolve, 3000));
      navigate("/admin");
    } catch (e: any) {
      setUploadingStatus(0);
      setIsUploading(false);
      console.log(e.response);
      window.alert(e?.response?.data || e.message);
    }
  };

  return isUploading ? (
    <>
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
            variant="determinate"
            value={(uploadingStatus * 100) / maxStatus}
            size={100}
          />
          <Typography
            variant="h6"
            sx={{
              position: "absolute",
              top: "50%",
              left: "50%",
              transform: "translate(-50%, -50%)",
              fontWeight: "bold",
            }}
          >
            {Math.round((uploadingStatus * 100) / maxStatus)}%
          </Typography>
        </Box>

        <Typography variant="h6" sx={{ color: "text.secondary" }}>
          {uploadingStatus == maxStatus
            ? "Video was uploaded!"
            : "Uploading..."}
        </Typography>
      </Box>
    </>
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
