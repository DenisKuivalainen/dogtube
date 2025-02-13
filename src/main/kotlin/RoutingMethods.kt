package com.example

import com.example.src.Admin
import com.example.src.Users
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private suspend inline fun ApplicationCall.handleAdminUserRequest(
    crossinline action: suspend (String, String) -> Unit
) {
    val adminUserCredentials = receive<AdminUserCredentials>()
    val (username, password) = adminUserCredentials

    if (username.isBlank() || password.isBlank()) {
        respondText("Username and password cannot be empty.", status = HttpStatusCode.BadRequest)
        return
    }

    action(username, password)
}


fun Route.helloWorld() {
    get("/") {
        call.respondText("Hello World!")
    }
}


fun Route.adminUser() {
    route("/admin/user") {
        authenticate("admin_jwt") {
            get {
                val principal = call.principal<UserPrincipal>()

                if (principal != null) {
                    val res = Admin.getAdminUser(principal.username)
                    val resData = res.data
                    if (resData != null) {
                        call.respond(HttpStatusCode.OK, resData)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, res.error!!)
                    }
                } else {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token.")
                }
            }
        }

        authenticate("admin_api_key") {
            post {
                call.handleAdminUserRequest { username, password ->
                    val res = Admin.createAdminUser(username, password)
                    val resData = res.data
                    if (resData != null) {
                        call.respond(HttpStatusCode.Created, resData)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, res.error!!)
                    }
                }
            }
        }

        post("login") {
            call.handleAdminUserRequest { username, password ->
                val res = Admin.authAdminUser(username, password)
                val resData = res.data
                if (resData != null) {
                    call.respond(HttpStatusCode.OK, resData)
                } else {
                    call.respond(HttpStatusCode.BadRequest, res.error!!)
                }
            }
        }
    }
}

fun Route.adminVideos() {
    get("admin/video/{id}/thumbnail") {
        val videoId = call.parameters["id"]
        if (videoId == null) {
            call.respondText("Missing video ID", status = HttpStatusCode.BadRequest)
            return@get
        }

        val res = Admin.getThumbnail(videoId)
        val resData = res.data
        if (resData != null) {
            call.respondBytes(resData, ContentType.Image.JPEG, HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.BadRequest, res.error!!)
        }
    }

    authenticate("admin_jwt") {
        route("admin/video") {
            get { // Get all videos
                val res = Admin.getAllVideosAdmin()
                val resData = res.data
                if (resData != null) {
                    call.respond(HttpStatusCode.OK, resData)
                } else {
                    call.respond(HttpStatusCode.BadRequest, res.error!!)
                }
            }

            post { // Prepare for video multipart upload
                val body = call.receive<PostVideoRequest>()
                val res = Admin.createVideo(body.name, body.isPremium, body.bufferSize, body.extension)
                val resData = res.data
                if (resData != null) {
                    call.respond(HttpStatusCode.OK, resData)
                } else {
                    call.respond(HttpStatusCode.BadRequest, res.error!!)
                }
            }

            route("/{id}") {
                get { // Get video info
                    val videoId = call.parameters["id"]
                    if (videoId == null) {
                        call.respondText("Missing video ID", status = HttpStatusCode.BadRequest)
                        return@get
                    }

                    val res = Admin.getVideoAdmin(videoId)
                    val resData = res.data
                    if (resData != null) {
                        call.respond(HttpStatusCode.OK, resData)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, res.error!!)
                    }
                }

                delete { // delete video
                    val videoId = call.parameters["id"]
                    if (videoId == null) {
                        call.respondText("Missing video ID", status = HttpStatusCode.BadRequest)
                        return@delete
                    }

                    val res = Admin.deleteVideoAdmin(videoId)
                    val resData = res.data
                    if (resData != null) {
                        call.respond(HttpStatusCode.OK, resData)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, res.error!!)
                    }
                }

                put { // Multipart upload of a video
                    val videoId = call.parameters["id"]
                    if (videoId == null) {
                        call.respondText("Missing video ID", status = HttpStatusCode.BadRequest)
                        return@put
                    }

                    var chunkId: String? = null
                    var chunkData: ByteArray? = null

                    call.receiveMultipart().forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                if (part.name == "chunkId") {
                                    chunkId = part.value
                                }
                            }

                            is PartData.FileItem -> {
                                chunkData = part.streamProvider().readBytes()
                            }

                            else -> Unit
                        }
                        part.dispose()
                    }

                    if (chunkData == null || chunkId == null) {
                        call.respondText("Missing chunk data or metadata.", status = HttpStatusCode.BadRequest)
                        return@put
                    }

                    val res = Admin.uploadVideoChunk(videoId, chunkId!!, chunkData!!)
                    val resData = res.data
                    if (resData != null) {
                        call.respond(HttpStatusCode.OK, resData)
                    } else {
                        call.respond(HttpStatusCode.BadRequest, res.error!!)
                    }
                }
            }
        }
    }
}

fun Route.user() {
    route("user") {
        route("create") {
            post {
                val body = call.receive<CreateUserRequest>()

                val res = Users.createUser(body.username, body.name, body.password)
                val cookie = res.data

                if (cookie != null) {
                    call.response.cookies.append(cookie)
                    call.respond(HttpStatusCode.Created)
                } else {
                    call.respondText(res.error!!, status = HttpStatusCode.BadRequest)
                }
            }
        }

        route("login") {
            post {
                val body = call.receive<LoginUserRequest>()

                val res = Users.loginUser(body.username, body.password)
                val cookie = res.data

                if (cookie != null) {
                    call.response.cookies.append(cookie)
                    call.respond(HttpStatusCode.Created)
                } else {
                    call.respondText(res.error!!, status = HttpStatusCode.BadRequest)
                }
            }
        }
    }
}