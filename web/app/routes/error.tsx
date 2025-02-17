import { useEffect, useState } from "react";
import { useLocation } from "react-router";
import { darkTheme, useUtils } from "~/utils";
import {
  Container,
  Typography,
  Button,
  CssBaseline,
  ThemeProvider,
  createTheme,
} from "@mui/material";

export default function ErrorPage() {
  const theme = createTheme(darkTheme);
  const { redirect } = useUtils();
  const location = useLocation();
  const [error, setError] = useState<string>("");

  useEffect(() => {
    const queryParams = new URLSearchParams(location.search);
    setError(queryParams.get("error") || "");
  }, []);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Container
        maxWidth="sm"
        sx={{
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          mt: 4,
          marginTop: "30%",
        }}
      >
        <Typography variant="h6" color="error" gutterBottom>
          {error || "An unexpected error occurred."}
        </Typography>
        <Button
          variant="contained"
          color="primary"
          onClick={() => redirect()}
          sx={{ mt: 2 }}
        >
          Return Home
        </Button>
      </Container>
    </ThemeProvider>
  );
}
