import {
  Box,
  Button,
  CssBaseline,
  Link,
  TextField,
  ThemeProvider,
  Typography,
  createTheme,
} from "@mui/material";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { axiosInstance, darkTheme, useUtils } from "~/utils";
import type { Route } from "../+types/root";

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
    setError,
    formState: { errors },
  } = useForm();
  const { redirect } = useUtils();
  const theme = createTheme(darkTheme);

  useEffect(() => {
    axiosInstance
      .get("api/user")
      .then(() => redirect("/"))
      .catch(() => {});
  }, []);

  const onSubmit = async (data: any) => {
    try {
      await axiosInstance.post("api/user/login", {
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
