import type { Route } from "./+types/home";
import { useOutletContext } from "react-router";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Dogtube" },
    { name: "description", content: "Watch doggo videos!" },
  ];
}

export default () => {
  const { userData }: any = useOutletContext();

  console.log(userData);

  return <></>;
};
