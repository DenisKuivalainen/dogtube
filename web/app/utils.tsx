import axios from "axios";
import { useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router";

export const useAdminAuth = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const logout = (redirect?: string) => {
    if (typeof localStorage === "undefined") return;
    localStorage.removeItem("admin_jwt");
    navigate(
      `/admin/auth${
        redirect ? `?redirectUrl=${encodeURIComponent(redirect!)}` : ""
      }`
    );
  };

  const getAuthHeader = () => {
    if (typeof localStorage == "undefined") return "";

    const jwt = localStorage?.getItem("admin_jwt");

    if (jwt) return `Bearer ${jwt}`;

    logout(location.pathname);
  };

  const axiosInstance = axios.create({
    baseURL: "/api/admin",
    headers: {
      Authorization: getAuthHeader(),
    },
  });

  return {
    logout,
    getAuthHeader,
    axiosInstance,
  };
};

export const useUtils = () => {
  const navigate = useNavigate();

  return {
    redirect: (path?: string) => navigate(path || "/"),
  };
};

export const getFirstFrameAsBlob = (file: File): Promise<Blob> => {
  return new Promise((resolve, reject) => {
    const video = document.createElement("video");
    const canvas = document.createElement("canvas");
    const ctx = canvas.getContext("2d") as CanvasRenderingContext2D;

    const reader = new FileReader();

    reader.onload = () => {
      video.src = reader.result as any;

      video.onloadedmetadata = () => {
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;

        video.currentTime = 0;

        video.onseeked = () => {
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

          canvas.toBlob((blob) => {
            if (blob) {
              resolve(blob);
            } else {
              reject("Failed to convert canvas to blob");
            }
          }, "image/jpeg");
        };
      };
    };

    reader.readAsDataURL(file);
  });
};

export const darkTheme: any = {
  palette: {
    mode: "dark",
    primary: {
      main: "#90caf9",
    },
    secondary: {
      main: "#f48fb1",
    },
  },
};

export const axiosInstance = axios.create({
  headers: {
    withCredentials: true,
  },
});

export function useTimeAgo() {
  const nowRef = useRef(new Date());

  useEffect(() => {
    const interval = setInterval(() => {
      nowRef.current = new Date();
    }, 30000);

    return () => clearInterval(interval);
  }, []);

  return (d: string) => {
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
  };
}
