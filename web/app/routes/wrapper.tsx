import { AccountCircle, Pets, Star } from "@mui/icons-material";
import {
  AppBar,
  Box,
  Button,
  CssBaseline,
  IconButton,
  Menu,
  MenuItem,
  ThemeProvider,
  Toolbar,
  Typography,
  createTheme,
} from "@mui/material";
import axios from "axios";
import { useEffect, useState } from "react";
import { Outlet } from "react-router";
import { darkTheme, useUtils } from "~/utils";

const Navbar = ({
  isPremium,
  username,
  onUpgrade = () => {},
  onLogout = () => {},
  onSettings = () => {},
}: {
  isPremium: boolean;
  username: string;
  onUpgrade?: () => void | Promise<void>;
  onLogout?: () => void | Promise<void>;
  onSettings?: () => void | Promise<void>;
}) => {
  // State for the user menu
  const [anchorEl, setAnchorEl] = useState(null);
  const openMenu = Boolean(anchorEl);

  // Handle menu open
  const handleMenuClick = (event: any) => {
    setAnchorEl(event.currentTarget);
  };

  // Handle menu close
  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  // Handle specific menu item click
  const handleUpgradeClick = () => {
    onUpgrade();
    handleMenuClose();
  };

  const handleSettingsClick = () => {
    onSettings();
    handleMenuClose();
  };

  const handleLogoutClick = () => {
    onLogout();
    handleMenuClose();
  };

  return (
    <AppBar position="sticky">
      <Toolbar sx={{ display: "flex", justifyContent: "space-between" }}>
        {/* Left Side: DOGTUBE */}
        <Typography variant="h6" sx={{ fontWeight: "bold" }}>
          DOG
          <Pets />
          TUBE
        </Typography>

        {/* Middle: "Upgrade to premium" */}
        {!isPremium && (
          <Box
            sx={{
              flexGrow: 1,
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
            }}
          >
            <Typography variant="body1" sx={{ marginRight: 1 }}>
              Upgrade to premium today!
            </Typography>
            <Button
              variant="contained"
              onClick={onUpgrade}
              size="small"
              color="secondary"
            >
              Upgrade
            </Button>
          </Box>
        )}

        {/* Right Side: User icon and name */}
        <Box>
          <Button
            endIcon={<AccountCircle />}
            color="inherit"
            onClick={handleMenuClick}
          >
            {username}
          </Button>
          <Menu
            anchorEl={anchorEl}
            open={openMenu}
            onClose={handleMenuClose}
            anchorOrigin={{
              vertical: "top",
              horizontal: "right",
            }}
            transformOrigin={{
              vertical: "top",
              horizontal: "right",
            }}
          >
            {/* Upgrade to Premium if not already premium */}
            {!isPremium && (
              <MenuItem
                onClick={handleUpgradeClick}
                sx={{ color: "secondary.main" }}
              >
                <Star sx={{ marginRight: 1 }} /> Upgrade to premium
              </MenuItem>
            )}

            {/* Settings */}
            <MenuItem onClick={handleSettingsClick}>Settings</MenuItem>

            {/* Logout */}
            <MenuItem onClick={handleLogoutClick}>Logout</MenuItem>
          </Menu>
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default () => {
  const theme = createTheme(darkTheme);
  const { redirect } = useUtils();

  const [userData, setUserData] = useState<any>();

  useEffect(() => {
    axios
      .get("api/user", {
        withCredentials: true,
      })
      .then((res) => res.data)
      .then((res) => setUserData(res))
      .catch((e) => {
        switch (e?.response?.status) {
          case 401:
            redirect("/signin");
            break;
          default:
            redirect(
              `/error?error=${encodeURIComponent(
                e?.response?.data || e?.response?.statusText
              )}`
            );
            break;
        }
      });
  }, []);

  if (typeof window == "undefined") return <></>;
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {userData ? (
        <>
          <Navbar
            isPremium={userData.subscription_level == "PREMIUM"}
            username={userData.name}
          />{" "}
          <Outlet context={{ userData }} />
        </>
      ) : (
        <></>
      )}
    </ThemeProvider>
  );
};
