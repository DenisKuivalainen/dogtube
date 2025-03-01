import { useEffect, useRef, useState } from "react";
import type { Route } from "./+types/home";
import { useOutletContext, useParams } from "react-router";
import axios from "axios";
import {
  Box,
  Button,
  Card,
  CardMedia,
  IconButton,
  Paper,
  Slider,
  Snackbar,
  TextField,
  Typography,
} from "@mui/material";
import {
  Favorite,
  FavoriteBorder,
  Pause,
  PlayArrow,
  Replay,
  Reply,
  Send,
  Share,
  Visibility,
  VolumeOff,
  VolumeUp,
} from "@mui/icons-material";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Dogtube" },
    { name: "description", content: "Watch doggo videos!" },
  ];
}

function timeAgo(d: string) {
  const date = new Date(d);
  const now = new Date();
  const diffInSeconds = Math.floor((now.valueOf() - date.valueOf()) / 1000);

  const minutes = Math.floor(diffInSeconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  const weeks = Math.floor(days / 7);
  const months = Math.floor(days / 30);
  const years = Math.floor(days / 365);

  if (minutes == 0) return `less than a minute ago`;
  if (minutes < 60) return `${minutes} minute${minutes !== 1 ? "s" : ""} ago`;
  if (hours < 24) return `${hours} hour${hours !== 1 ? "s" : ""} ago`;
  if (days < 7) return `${days} day${days !== 1 ? "s" : ""} ago`;
  if (weeks < 4) return `${weeks} week${weeks !== 1 ? "s" : ""} ago`;
  if (months < 12) return `${months} month${months !== 1 ? "s" : ""} ago`;
  return `${years} year${years !== 1 ? "s" : ""} ago`;
}

export function useTimeAgo() {
  const nowRef = useRef(new Date());

  useEffect(() => {
    const interval = setInterval(() => {
      nowRef.current = new Date();
    }, 30000);

    return () => clearInterval(interval);
  }, []);

  return (d: string) => timeAgo(d);
}

export default () => {
  const getTimeAgo = useTimeAgo();

  const { videoId } = useParams<{ videoId: string }>();
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [playing, setPlaying] = useState(false);
  const [hovered, setHovered] = useState(false);
  const [progress, setProgress] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(
    parseInt(localStorage.getItem("volume") || "50")
  );
  const [showVolumeSlider, setShowVolumeSlider] = useState(false);
  const [ended, setEnded] = useState(false);

  // Toggle Play/Pause
  const togglePlay = () => {
    if (videoRef.current) {
      if (playing) {
        videoRef.current.pause();
      } else {
        videoRef.current.play();
      }
      setPlaying(!playing);
      setEnded(false);
    }
  };

  const handleReplay = () => {
    if (videoRef.current) {
      videoRef.current.currentTime = 0;
      videoRef.current.play();
      setPlaying(true);
      setEnded(false);
    }
  };

  useEffect(() => {
    if (!videoRef.current) return;

    const updateProgress = () => {
      if (videoRef.current) {
        setProgress(videoRef.current.currentTime);
        setDuration(videoRef.current.duration || 0);
      }
    };

    const handleVideoEnd = () => {
      setEnded(true);
      setPlaying(false);
    };

    videoRef.current.addEventListener("timeupdate", updateProgress);
    videoRef.current.addEventListener("loadedmetadata", updateProgress);
    videoRef.current.addEventListener("ended", handleVideoEnd);

    return () => {
      videoRef.current?.removeEventListener("timeupdate", updateProgress);
      videoRef.current?.removeEventListener("loadedmetadata", updateProgress);
      videoRef.current?.removeEventListener("ended", handleVideoEnd);
    };
  }, []);

  const handleSeek = (_event: Event, newValue: number | number[]) => {
    if (videoRef.current) {
      videoRef.current.currentTime = newValue as number;
      setProgress(newValue as number);

      if (videoRef.current.currentTime >= videoRef.current.duration) {
        setEnded(true);
      } else {
        setEnded(false);
      }
    }
  };

  const handleVolumeChange = (_event: Event, newValue: number | number[]) => {
    if (videoRef.current) {
      const volumeValue = (newValue as number) / 100;
      videoRef.current.volume = volumeValue;
      setVolume(newValue as number);
      if (muted) toggleMute();

      localStorage.setItem("volume", `${newValue as number}`);
    }
  };

  const [muted, setMuted] = useState(false);

  const toggleMute = () => {
    if (videoRef.current) {
      const newMuted = !muted;
      videoRef.current.muted = newMuted;
      setMuted(newMuted);
    }
  };

  const [snackbarOpen, setSnackbarOpen] = useState(false);

  const handleShare = async () => {
    const url = window.location.href;
    try {
      await navigator.clipboard.writeText(url);
      setSnackbarOpen(true);
    } catch (error) {
      console.error("Failed to copy:", error);
    }
  };

  const [videoData, setVideoData] = useState<{
    name: string;
    uploadedAt: string;
    views: number;
    likes: number;
    liked: boolean;
  }>();

  useEffect(() => {
    axios
      .get(`/api/video/${videoId}`, { withCredentials: true })
      .then((res) => res.data)
      .then(setVideoData);
  }, []);

  const like = () => {
    axios
      .post(`/api/video/${videoId}/like`, {}, { withCredentials: true })
      .then((res) => res.data)
      .then(setVideoData);
  };

  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState<
    {
      username: string;
      postedAt: string;
      message: string;
      myMessage: boolean;
    }[]
  >([]);
  const handleMessageChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    let newVal = e.target.value;

    while (encodeURIComponent(newVal).length > 1000) {
      newVal = newVal.slice(0, -1);
    }

    setMessage(newVal);
  };
  const submitMessage = async () => {
    if (message.length <= 0 && message.length > 1000) return;

    await axios.post(`/api/video/${videoId}/message`, {
      message: encodeURIComponent(message),
    });
    setMessage("");
    await fetchMessages();
  };

  const fetchMessages = () =>
    axios
      .get(`/api/video/${videoId}/message`)
      .then((res) => res.data)
      .then(setMessages);

  useEffect(() => {
    fetchMessages();
    const interval = setInterval(() => {
      fetchMessages();
    }, 60000);

    return () => clearInterval(interval);
  }, []);

  return (
    <>
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          marginTop: "50px",
        }}
      >
        <Card
          sx={{
            maxWidth: 800,
            borderRadius: "8px",
          }}
          onMouseEnter={() => setHovered(true)}
          onMouseLeave={() => {
            setHovered(false);
            setShowVolumeSlider(false);
          }}
        >
          <Box sx={{ position: "relative" }}>
            <CardMedia
              component="video"
              ref={videoRef}
              src={`/api/video/${videoId}/stream`}
              sx={{
                width: "100%",
                borderRadius: "8px",
                outline: "none",
              }}
            />
            <IconButton
              onClick={ended ? handleReplay : togglePlay}
              sx={{
                position: "absolute",
                top: "50%",
                left: "50%",
                transform: "translate(-50%, -50%)",
                bgcolor: "rgba(0,0,0,0.5)",
                color: "white",
                "&:hover": { bgcolor: "rgba(0,0,0,0.7)" },
                opacity: hovered || !playing || ended ? 1 : 0,
                transition: "opacity 0.3s ease-in-out",
              }}
            >
              {ended ? (
                <Replay fontSize="large" />
              ) : playing ? (
                <Pause fontSize="large" />
              ) : (
                <PlayArrow fontSize="large" />
              )}
            </IconButton>
          </Box>

          <Box sx={{ display: "flex", alignItems: "center", px: 2, py: 1 }}>
            <IconButton
              onClick={ended ? handleReplay : togglePlay}
              sx={{ color: "white" }}
            >
              {ended ? <Replay /> : playing ? <Pause /> : <PlayArrow />}
            </IconButton>

            <Slider
              value={progress}
              min={0}
              max={duration}
              onChange={handleSeek}
              step={0.001}
              sx={{
                color: "white",
                flex: 1,
                "& .MuiSlider-thumb": { display: hovered ? "block" : "none" },
                marginLeft: "10px",
                marginRight: "10px",
              }}
            />

            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                position: "relative",
              }}
              onMouseEnter={() => setShowVolumeSlider(true)}
              onMouseLeave={() => setShowVolumeSlider(false)}
            >
              <IconButton sx={{ color: "white" }} onClick={toggleMute}>
                {muted || volume === 0 ? <VolumeOff /> : <VolumeUp />}
              </IconButton>

              {showVolumeSlider && (
                <Slider
                  value={volume}
                  min={0}
                  max={100}
                  onChange={handleVolumeChange}
                  orientation="vertical"
                  sx={{
                    position: "absolute",
                    bottom: "40px",
                    right: "10px",
                    height: 80,
                    color: "white",
                    "& .MuiSlider-thumb": { display: "block" },
                    "& .MuiSlider-track": { bgcolor: "white" },
                  }}
                />
              )}
            </Box>
          </Box>
          <Box sx={{ display: "flex", alignItems: "center", px: 2, py: 1 }}>
            <Typography variant="h5" sx={{ mr: 2 }}>
              {videoData?.name || ""}
              <Box sx={{ display: "flex", alignItems: "center" }}>
                <Typography variant="caption">
                  {videoData?.views || 0} view{videoData?.views != 1 ? "s" : ""}
                  <a style={{ marginLeft: 15 }} />
                  {getTimeAgo(videoData?.uploadedAt!)}
                </Typography>
              </Box>
            </Typography>

            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                alignSelf: "flex-start",
                ml: "auto", // Push to the right
              }}
            >
              <Button
                sx={{ color: "white" }}
                onClick={like}
                startIcon={
                  videoData?.liked ? (
                    <Favorite color="secondary" />
                  ) : (
                    <FavoriteBorder />
                  )
                }
              >
                {videoData?.likes || 0}
              </Button>
              <Button
                sx={{ color: "white", ml: 1 }}
                onClick={handleShare}
                startIcon={<Reply style={{ transform: "scaleX(-1)" }} />}
              >
                Share
              </Button>
              <Snackbar
                open={snackbarOpen}
                autoHideDuration={2000}
                onClose={() => setSnackbarOpen(false)}
                message="URL copied to clipboard!"
                anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
                ContentProps={{
                  sx: {
                    bgcolor: "#333",
                    color: "white",
                    borderRadius: "8px",
                    fontSize: "1rem",
                  },
                }}
              />
            </Box>
          </Box>
        </Card>
      </Box>
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          marginTop: "30px",
          marginBottom: "50px",
        }}
      >
        <Box
          sx={{
            display: "flex",
            flexDirection: "column",
            alignItems: "flex-end",
            width: 700,
            gap: 2,
          }}
        >
          <Paper
            elevation={3}
            sx={{
              padding: 2,
              borderRadius: "16px",
              bgcolor: "background.paper",
              width: 400,
              display: "flex",
              flexDirection: "column",
              gap: 1,
            }}
          >
            <Box sx={{ display: "flex", alignItems: "center", width: "100%" }}>
              <TextField
                id="standard-basic"
                variant="standard"
                sx={{ flexGrow: 1 }}
                multiline
                value={message}
                onChange={handleMessageChange}
              />
              <IconButton
                onClick={submitMessage}
                sx={{ alignSelf: "flex-end" }}
              >
                <Send />
              </IconButton>
            </Box>
          </Paper>

          {messages.map((m) => (
            <Paper
              elevation={3}
              sx={{
                padding: 2,
                borderRadius: "16px",
                bgcolor: "background.paper",
                maxWidth: 400,
                display: "flex",
                flexDirection: "column",
                gap: 1,
                ...(m.myMessage ? {} : { alignSelf: "flex-start" }),
              }}
            >
              <Typography variant="caption" sx={{ color: "text.secondary" }}>
                <strong>{m.myMessage ? "You" : m.username}</strong> said{" "}
                {getTimeAgo(m.postedAt)}
              </Typography>
              <Typography variant="body1">
                {decodeURIComponent(m.message)}
              </Typography>
            </Paper>
          ))}
        </Box>
      </Box>
    </>
  );
};
