import {
  TextField,
  Button,
  Box,
  Typography,
  CssBaseline,
  ThemeProvider,
  createTheme,
  Link,
} from "@mui/material";
import { useForm } from "react-hook-form";
import type { Route } from "../+types/root";
import axios from "axios";
import { darkTheme, useUtils } from "~/utils";
import { useEffect } from "react";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Sign in!" },
    { name: "description", content: "Login into your account!" },
  ];
}

export default () => {
  const {
    register,
    handleSubmit,
    watch,
    setError,
    formState: { errors },
  } = useForm();
  const { redirect } = useUtils();
  const theme = createTheme(darkTheme);

  useEffect(() => {
    axios
      .get("api/user", {
        withCredentials: true,
      })
      .then(() => redirect("/"))
      .catch(() => {});
  }, []);

  const onSubmit = async (data: any) => {
    try {
      await axios.post("api/user/login", {
        username: data.username,
        password: data.password,
      });

      redirect();
    } catch (e: any) {
      setError("username", {
        type: "manual",
        message: e?.response?.data || e.message,
      });
    }
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box
        sx={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          height: "100vh",
          bgcolor: "background.default",
          color: "text.primary",
        }}
      >
        <Typography variant="h4" gutterBottom>
          Login into your account
        </Typography>
        <Box
          component="form"
          onSubmit={handleSubmit(onSubmit)}
          sx={{ width: 500, p: 3, bgcolor: "grey.900", borderRadius: 2 }}
        >
          <TextField
            fullWidth
            margin="normal"
            label="Username"
            variant="outlined"
            {...register("username", { required: "Username is required" })}
            error={!!errors.username}
            helperText={errors.username?.message || " "}
            autoComplete="off"
          />
          <TextField
            fullWidth
            margin="normal"
            label="Password"
            type="password"
            variant="outlined"
            {...register("password", {
              required: "Password is required",
            })}
            error={!!errors.password}
            autoComplete="off"
          />
          <Button
            type="submit"
            variant="contained"
            color="primary"
            fullWidth
            sx={{ mt: 2 }}
          >
            Sign Up
          </Button>
          <Typography variant="body2" sx={{ mt: 2, textAlign: "center" }}>
            Don't have an account?{" "}
            <Link
              href="/signup"
              color="primary"
              sx={{ textDecoration: "none" }}
            >
              Create new account
            </Link>
          </Typography>
        </Box>
      </Box>
    </ThemeProvider>
  );
};
