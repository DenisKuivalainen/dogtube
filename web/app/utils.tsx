import { createTheme } from "@mui/system";
import { useLocation, useNavigate } from "react-router";

export const useAdminAuth = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const logout = (redirect?: string) => {
    localStorage.removeItem("admin_jwt");
    navigate(
      `/admin/auth${
        redirect ? `?redirectUrl=${encodeURIComponent(redirect!)}` : ""
      }`
    );
  };

  const getAuthHeader = () => {
    const jwt = localStorage.getItem("admin_jwt");

    if (jwt) return `Bearer ${jwt}`;

    logout(location.pathname);
  };

  return {
    logout,
    getAuthHeader,
  };
};

export const useUtils = () => {
  const navigate = useNavigate();
  const location = useLocation();

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
