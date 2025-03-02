import { AppBar, Button, Toolbar, Typography } from "@mui/material";
import axios from "axios";
import { useEffect, useState } from "react";
import { useLocation } from "react-router";
import { Outlet } from "react-router";
import { useAdminAuth } from "~/utils";

export default () => {
  const adminAuth = useAdminAuth();
  const location = useLocation();

  const [loading, setLoading] = useState(true);
  const [username, setUsername] = useState("");

  const loadUserData = async () => {
    try {
      const user = await adminAuth.axiosInstance.get("/user", {
        headers: {
          Authorization: adminAuth.getAuthHeader(),
        },
      });

      setUsername(user.data.username);
      setLoading(false);
    } catch (e) {
      adminAuth.logout(location.pathname);
    }
  };

  useEffect(() => {
    loadUserData();
  }, []);

  return loading ? (
    <></>
  ) : (
    <>
      <AppBar position="static" color="primary" sx={{ boxShadow: 2 }}>
        <Toolbar sx={{ display: "flex", justifyContent: "space-between" }}>
          <Typography variant="h6">Hello, {username}!</Typography>
          <Button color="inherit" onClick={() => adminAuth.logout()}>
            Log out
          </Button>
        </Toolbar>
      </AppBar>
      <div style={{ margin: 20 }}>
        <Outlet />
      </div>
    </>
  );
};
