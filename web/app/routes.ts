import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
  index("routes/home.tsx"),
  route("admin/auth", "routes/admin/auth.tsx"),
  route("admin", "routes/admin/wrapper.tsx", [
    index("routes/admin/home.tsx"),
    route("video/add", "routes/admin/addVideo.tsx"),
    route("video/edit/:videoId", "routes/admin/editVideo.tsx"),
  ]),
] satisfies RouteConfig;
