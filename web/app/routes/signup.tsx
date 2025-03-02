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
    { title: "Sign up!" },
    { name: "description", content: "Create new account!" },
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
    axiosInstance
      .get("api/user")
      .then(() => redirect("/"))
      .catch(() => {});
  }, []);

  const onSubmit = async (data: any) => {
    try {
      await axiosInstance.post("/api/user/create", {
        username: data.username,
        name: data.name,
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

  const password = watch("password");

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
          Create a new account
        </Typography>
        <Box
          component="form"
          onSubmit={handleSubmit(onSubmit)}
          sx={{ width: 500, p: 3, bgcolor: "grey.900", borderRadius: 2 }}
        >
          <TextField
            fullWidth
            margin="normal"
            label="Name"
            variant="outlined"
            {...register("name", { required: "Name is required" })}
            error={!!errors.name}
            helperText={(errors.name?.message || " ") as string}
            autoComplete="off"
          />
          <TextField
            fullWidth
            margin="normal"
            label="Username"
            variant="outlined"
            {...register("username", { required: "Username is required" })}
            error={!!errors.username}
            helperText={(errors.username?.message || " ") as string}
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
              minLength: {
                value: 8,
                message: "Password must be at least 8 characters",
              },
              pattern: {
                value:
                  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/,
                message:
                  "Password must contain uppercase, lowercase, number, and special character",
              },
            })}
            error={!!errors.password}
            autoComplete="off"
          />
          <TextField
            fullWidth
            margin="normal"
            label="Repeat Password"
            type="password"
            variant="outlined"
            {...register("repeatPassword", {
              required: "Please confirm your password",
              validate: (value) =>
                value === password || "Passwords do not match",
            })}
            error={!!errors.repeatPassword}
            helperText={
              (errors.password?.message ||
                errors.repeatPassword?.message ||
                " ") as string
            }
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
            Already have an account?{" "}
            <Link
              href="/signin"
              color="primary"
              sx={{ textDecoration: "none" }}
            >
              Log in
            </Link>
          </Typography>
        </Box>
      </Box>
    </ThemeProvider>
  );
};
