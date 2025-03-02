import { AccountCircle, Pets, Search } from "@mui/icons-material";
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
import { axiosInstance, darkTheme, useUtils } from "~/utils";

const Navbar = ({
  username,
  onLogout = () => {},
  onSettings = () => {},
}: {
  username: string;
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
        <Typography
          variant="h6"
          component="a"
          href="/"
          sx={{
            fontWeight: "bold",
            textDecoration: "none",
            color: "white",
          }}
        >
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
            <MenuItem onClick={handleSettingsClick}>Settings</MenuItem>
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
    axiosInstance
      .get("api/user")
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
            username={userData.name}
            onLogout={() => {
              axiosInstance
                .post("api/user/logout")
                .then(() => redirect("/signin"));
            }}
          />
          <Outlet context={{ userData }} />
        </>
      ) : (
        <></>
      )}
    </ThemeProvider>
  );
};
