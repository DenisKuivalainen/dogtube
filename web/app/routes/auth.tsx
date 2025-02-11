import type { Route } from "./+types/home";
import { Welcome } from "../welcome/welcome";
import { TextField } from "@mui/material";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "DodTube Admin Login" },
    { name: "description", content: "Login to DodTube Admin!" },
  ];
}

export default function Home() {
  return (
    <>
      <TextField id="outlined-basic" label="Username" variant="outlined" />
      <TextField
        id="outlined-basic"
        label="Password"
        variant="outlined"
        type="password"
      />
    </>
  );
}
