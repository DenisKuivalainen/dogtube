import { useState } from "react";
import type { Route } from "./+types/home";
import { Button, TextField, Box } from "@mui/material";
import axios from "axios";
import { redirect, useLocation, useNavigate } from "react-router";
import { useAdminAuth } from "~/utils";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "DodTube Admin Login" },
    { name: "description", content: "Login to DodTube Admin!" },
  ];
}

export default () => {
  const [username, setUsername] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(false);

  const navigate = useNavigate();
  const location = useLocation();
  const adminAuth = useAdminAuth();

  const handleLogin = async () => {
    if (loading) return;
    setLoading(true);
    try {
      const jwt = await adminAuth
        .axiosInstance(false)
        .post("/user/login", { username, password })
        .then((res) => res.data.jwt);

      localStorage.setItem("admin_jwt", jwt);

      const queryParams = new URLSearchParams(location.search);
      const redirectValue = queryParams.get("redirectUrl");
      navigate(redirectValue || "/admin");
    } catch (e) {
      console.log(e);
      setLoading(false);
    }
  };

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      height="100vh"
      gap={2}
    >
      <TextField
        id="username"
        label="Username"
        variant="outlined"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        sx={{ width: 300, marginBottom: 2 }}
      />
      <TextField
        id="password"
        label="Password"
        variant="outlined"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        sx={{ width: 300, marginBottom: 2 }}
      />
      <Button variant="contained" onClick={handleLogin} disabled={loading}>
        Login
      </Button>
    </Box>
  );
};
