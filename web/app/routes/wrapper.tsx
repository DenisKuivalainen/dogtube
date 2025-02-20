import { AccountCircle, Pets, Search, Star } from "@mui/icons-material";
import {
  AppBar,
  Box,
  Button,
  CssBaseline,
  IconButton,
  InputAdornment,
  Menu,
  MenuItem,
  TextField,
  ThemeProvider,
  Toolbar,
  Typography,
  createTheme,
} from "@mui/material";
import axios from "axios";
import { useEffect, useState } from "react";
import { Outlet, useSearchParams } from "react-router";
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
  const [anchorEl, setAnchorEl] = useState(null);
  const openMenu = Boolean(anchorEl);

  const [searchParams] = useSearchParams();
  const [seach, setSearch] = useState<string>("");
  useEffect(() => {
    setSearch(decodeURIComponent(searchParams.get("search") || ""));
  }, [searchParams]);

  const { redirect } = useUtils();

  const handleMenuClick = (event: any) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

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

  const handleSearch = () => {
    redirect(`/?search=${encodeURIComponent(seach)}`);
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

        <Box
          sx={{
            flexGrow: 1,
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
          }}
        >
          <TextField
            variant="outlined"
            size="small"
            placeholder="Search dog videos..."
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton onClick={handleSearch} edge="end">
                    <Search />
                  </IconButton>
                </InputAdornment>
              ),
            }}
            sx={{ width: "50%", bgcolor: "background.paper", borderRadius: 1 }}
            onChange={(e) => setSearch(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            value={seach}
          />
        </Box>

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
            onLogout={() => {
              axios
                .post("api/user/logout", {
                  withCredentials: true,
                })
                .then(() => redirect("/signin"));
            }}
            onUpgrade={() => redirect("/subscription")}
          />{" "}
          <Outlet context={{ userData }} />
        </>
      ) : (
        <></>
      )}
    </ThemeProvider>
  );
};
