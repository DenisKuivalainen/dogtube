import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
  route("/", "routes/wrapper.tsx", [
    index("routes/home.tsx"),
    route(":videoId", "routes/video.tsx"),
    route("subscription", "routes/subscription.tsx"),
  ]),
  route("admin/auth", "routes/admin/auth.tsx"),
  route("admin", "routes/admin/wrapper.tsx", [
    index("routes/admin/home.tsx"),
    route("video/add/v2", "routes/admin/addVideoV2.tsx"),
    route("video/edit/:videoId", "routes/admin/editVideo.tsx"),
  ]),
  route("signup", "routes/signup.tsx"),
  route("signin", "routes/signin.tsx"),
  route("error", "routes/error.tsx"),
] satisfies RouteConfig;
