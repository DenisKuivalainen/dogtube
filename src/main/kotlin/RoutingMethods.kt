package com.example

import com.example.src.Admin
import com.example.src.ResponseFormat
import com.example.src.Users
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

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
                        call.respond(HttpStatusCode.InternalServerError, res.error!!)
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
                        call.respond(HttpStatusCode.InternalServerError, res.error!!)
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
                    call.respond(HttpStatusCode.InternalServerError, res.error!!)
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
            call.respond(HttpStatusCode.InternalServerError, res.error!!)
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
                    call.respond(HttpStatusCode.InternalServerError, res.error!!)
                }
            }

            post { // Prepare for video multipart upload
                val body = call.receive<PostVideoRequest>()
                val res = Admin.createVideo(body.name, body.isPremium, body.bufferSize, body.extension)
                val resData = res.data
                if (resData != null) {
                    call.respond(HttpStatusCode.OK, resData)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, res.error!!)
                }
            }

            post("v2") { // Save a video as stream
                val res = Admin.uploadVideoV2(call.receiveMultipart())
                val resData = res.data
                if (resData != null) {
                    call.respond(HttpStatusCode.Created, resData)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, res.error!!)
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
                        call.respond(HttpStatusCode.InternalServerError, res.error!!)
                    }
                }

                get("statistics") {
                    val videoId = call.parameters["id"]
                    if (videoId == null) {
                        call.respondText("Missing video ID", status = HttpStatusCode.BadRequest)
                        return@get
                    }

                    val res = Admin.getVideoStatisticsForPastMonth(videoId)
                    val resData = res.data
                    if (resData != null) {
                        call.respond(HttpStatusCode.OK, resData)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, res.error!!)
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
                        call.respond(HttpStatusCode.InternalServerError, res.error!!)
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
                        call.respond(HttpStatusCode.InternalServerError, res.error!!)
                    }
                }

                patch {
                    val videoId = call.parameters["id"]
                    val body = call.receive<EditVideoRequest>()
                    val name = body.name
                    val isPremium = body.isPremium

                    if (videoId == null || isPremium == null && name == null) {
                        call.respondText("Missing chunk data or metadata.", status = HttpStatusCode.BadRequest)
                        return@patch
                    }

                    val res = Admin.editVideo(videoId, name, isPremium)
                    val resData = res.data
                    if (resData != null) {
                        call.respond(HttpStatusCode.OK, resData)
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, res.error!!)
                    }
                }
            }
        }
    }
}

suspend inline fun <reified T> sessionResponseHandler(
    call: ApplicationCall,
    successStatus: HttpStatusCode = HttpStatusCode.OK,
    fn: (principal: UserSessionPrincipal) -> ResponseFormat<T>,
) {
    val principal = call.principal<UserSessionPrincipal>()

    if (principal != null) {
        val res = fn(principal)
        val resData = res.data
        if (resData != null) {
            call.sessions.set(UserSession(principal.session))
            call.respond(successStatus, resData)
        } else {
            call.respond(HttpStatusCode.InternalServerError, res.error!!)
        }
    } else {
        call.respond(HttpStatusCode.Unauthorized, "Invalid token.")
    }
}

fun Route.user() {
    route("user") {
        route("create") {
            post {
                val body = call.receive<CreateUserRequest>()


                val res = Users.createUser(body.username, body.name, body.password)
                val sessionId = res.data

                if (sessionId != null) {
                    call.sessions.set(UserSession(sessionId))
                    call.respond(HttpStatusCode.Created)
                } else {
                    call.respondText(res.error!!, status = HttpStatusCode.InternalServerError)
                }
            }
        }

        route("login") {
            post {
                val body = call.receive<LoginUserRequest>()

                val res = Users.loginUser(body.username, body.password)
                val sessionId = res.data

                if (sessionId != null) {
                    call.sessions.set(UserSession(sessionId))
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respondText(res.error!!, status = HttpStatusCode.InternalServerError)
                }
            }
        }

        authenticate("user_session") {
            get {
                sessionResponseHandler(call) { principal ->
                    Users.getUserData(principal.username)
                }
            }

            route("logout") {
                post {
                    val principal = call.principal<UserSessionPrincipal>()

                    if (principal == null) {
                        call.respond(HttpStatusCode.Unauthorized, "Not authenticated")
                        return@post
                    }

                    val sessionId = principal.session

                    Users.logoutUser(sessionId)
                    call.sessions.clear<UserSession>()
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

fun Route.videos() {
    authenticate("user_session") {
        route("video") {
            get {
                sessionResponseHandler(call) { principal ->
                    Users.getAllVideos(principal.username, call.request.queryParameters["search"])
                }
            }

            route("{videoId}") {
                get {
                    sessionResponseHandler(call) { principal ->
                        Users.getVideo(principal.username, call.parameters["videoId"]!!)
                    }
                }

                get("stream") {
                    val principal = call.principal<UserSessionPrincipal>()

                    if (principal != null) {
                        val res = Users.streamVideo(
                            principal.username,
                            call.parameters["videoId"]!!,
                            call.request.headers["Range"]!!
                        )
                        val resData = res.data
                        if (resData != null) {
                            call.response.header(HttpHeaders.AcceptRanges, "bytes")
                            call.response.header(HttpHeaders.ContentType, "video/mp4")
                            call.response.header(
                                HttpHeaders.ContentRange,
                                "bytes ${resData.start}-${resData.end}/${resData.length}"
                            )
                            call.respondBytes(resData.buffer, ContentType.Video.MP4, HttpStatusCode.PartialContent)

                        } else {
                            call.respond(HttpStatusCode.InternalServerError, res.error!!)
                        }
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token.")
                    }
                }

                post("view") {
                    sessionResponseHandler(call) { principal ->
                        Users.createVideoView(principal.username, call.parameters["videoId"]!!)
                    }
                }

                post("like") {
                    sessionResponseHandler(call) { principal ->
                        Users.likeVideo(principal.username, call.parameters["videoId"]!!)
                    }
                }

                get("thumbnail") {
                    val principal = call.principal<UserSessionPrincipal>()

                    if (principal != null) {
                        val res = Users.getThumbnail(principal.username, call.parameters["videoId"]!!)
                        val resData = res.data
                        if (resData != null) {
                            call.sessions.set(UserSession(principal.session))
                            call.respondBytes(resData, ContentType.Image.JPEG, HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, res.error!!)
                        }
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, "Invalid token.")
                    }
                }

                route("message") {
                    get {
                        sessionResponseHandler(call) { principal ->
                            Users.getMessages(principal.username, call.parameters["videoId"]!!)
                        }
                    }

                    post {
                        sessionResponseHandler(call) { principal ->
                            Users.postMessage(
                                principal.username,
                                call.parameters["videoId"]!!,
                                call.receive<PostMessageRequest>().message
                            )
                        }
                    }
                }
            }
        }
    }
}

fun Route.subscription() {
    authenticate("user_session") {
        route("subscription") {
            post {
                sessionResponseHandler(call) { principal ->
                    val body = call.receive<SubscriptionRequest>()
                    Users.subscribe(principal.username, body.cardNumber, body.expiry, body.cvv, body.name)
                }
            }
        }
    }
}