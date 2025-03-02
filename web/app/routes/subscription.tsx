import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Grid,
  TextField,
  Typography,
} from "@mui/material";
import { useEffect, useState } from "react";
import { useOutletContext } from "react-router";
import { axiosInstance, useUtils } from "~/utils";
import type { Route } from "./+types/home";

const validateCardNumber = (cardNumber: string) => {
  const sanitized = cardNumber.replace(/\D/g, "");

  if (sanitized.length < 13 || sanitized.length > 19) return false;

  let sum = 0;
  let shouldDouble = false;

  for (let i = sanitized.length - 1; i >= 0; i--) {
    let digit = parseInt(sanitized[i]);

    if (shouldDouble) {
      digit *= 2;
      if (digit > 9) digit -= 9;
    }

    sum += digit;
    shouldDouble = !shouldDouble;
  }

  return sum % 10 === 0;
};

const validateExpiryDate = (mmyy: string) => {
  if (!/^\d{4}$/.test(mmyy)) return false;

  const month = parseInt(mmyy.substring(0, 2), 10);
  const year = parseInt(mmyy.substring(2, 4), 10);

  if (month < 1 || month > 12) return false;

  const now = new Date();
  const currentYear = now.getFullYear() % 100;
  const currentMonth = now.getMonth() + 1;

  if (year < currentYear || (year === currentYear && month < currentMonth)) {
    return false;
  }

  return true;
};

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Dogtube" },
    { name: "description", content: "Get access to PREMIUM content!" },
  ];
}

export default () => {
  const { userData }: any = useOutletContext();
  const { redirect } = useUtils();

  useEffect(() => {
    if (userData.subscription_level === "PREMIUM") redirect("/");
  }, [userData]);

  const [formData, setFormData] = useState({
    cardNumber: "",
    expiry: "",
    cvv: "",
    name: "",
  });

  const handleChange = (e: any) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e: any) => {
    e.preventDefault();

    try {
      if (!validateCardNumber(formData.cardNumber))
        throw Error("Invalid card number.");
      if (!validateExpiryDate(formData.expiry))
        throw Error("Invalid expirity date.");

      axiosInstance
        .post("api/subscription", formData)
        .then(() => redirect("/"));
    } catch (e: any) {
      alert(e.message);
    }
  };

  return (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        height: "calc(100vh - 128px)",
      }}
    >
      <Container maxWidth="sm">
        <Typography variant="h6" gutterBottom align="center">
          Get access to{" "}
          <Typography
            variant="h6"
            component="a"
            sx={{
              color: "secondary.main",
            }}
          >
            PREMIUM
          </Typography>{" "}
          content only for $9.99/month!
        </Typography>

        <Card sx={{ mt: 2, p: 2 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Enter Your Payment Details
            </Typography>
            <form onSubmit={handleSubmit}>
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <TextField
                    label="Card Number"
                    name="cardNumber"
                    value={formData.cardNumber.match(/\d{1,4}/g)?.join(" ")}
                    onChange={(e) =>
                      setFormData((prev) => ({
                        ...prev,
                        [e.target.name]: e.target.value.replaceAll(" ", ""),
                      }))
                    }
                    fullWidth
                    required
                    type="text"
                    inputProps={{ maxLength: 16 + 3 }}
                    placeholder="1234 5678 9012 3456"
                  />
                </Grid>
                <Grid item xs={6}>
                  <TextField
                    label="Expiry Date"
                    name="expiry"
                    value={formData.expiry.match(/\d{1,2}/g)?.join("/")}
                    onChange={(e) =>
                      setFormData({
                        ...formData,
                        [e.target.name]: e.target.value.replace("/", ""),
                      })
                    }
                    fullWidth
                    required
                    type="text"
                    placeholder="MM/YY"
                    inputProps={{ maxLength: 4 + 1 }}
                  />
                </Grid>
                <Grid item xs={6}>
                  <TextField
                    label="CVV"
                    name="cvv"
                    value={formData.cvv}
                    onChange={handleChange}
                    fullWidth
                    required
                    type="password"
                    inputProps={{ maxLength: 4 }}
                    placeholder="123"
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    label="Cardholder Name"
                    name="name"
                    value={formData.name}
                    onChange={handleChange}
                    fullWidth
                    required
                    type="text"
                    placeholder="John Doe"
                  />
                </Grid>
                <Grid
                  item
                  xs={12}
                  style={{ textAlign: "center", marginTop: 20 }}
                >
                  <Button type="submit" variant="contained" color="primary">
                    Subscribe for $9.99
                  </Button>
                </Grid>
              </Grid>
            </form>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
};
