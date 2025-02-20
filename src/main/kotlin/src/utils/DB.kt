import com.example.src.response
import com.example.src.utils.*
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.SQLDialect
import org.jooq.UpdateSetMoreStep
import org.jooq.impl.DSL
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Timestamp
import java.util.*

class DB private constructor() {

    private var connection: Connection? = null
    private var dslContext: DSLContext? = null

    private val dotenv = Dotenv.load()

    private val url: String = dotenv["DB_URL"] ?: "jdbc:postgresql://localhost:5432/mydatabase"
    private val user: String = dotenv["DB_USER"] ?: "myuser"
    private val password: String = dotenv["DB_PASSWORD"] ?: "mypassword"

    init {
        GlobalScope.launch {
            connect()
        }
    }

    companion object {
        @Volatile
        private var instance: DB? = null

        fun getInstance(): DB {
            return instance ?: synchronized(this) {
                instance ?: DB().also { instance = it }
            }
        }
    }

    private suspend fun connect() = ioOperation {
        try {
            if (connection == null || connection!!.isClosed) {
                connection = DriverManager.getConnection(url, user, password)
                dslContext = DSL.using(connection, SQLDialect.POSTGRES)
            }
        } catch (e: SQLException) {
            println("Database connection error: ${e.message}")
            reconnectWithDelay()
        }
    }

    private suspend fun reconnectWithDelay() {
        delay(5000)
        connect()
    }

    suspend fun getDSLContext(): DSLContext = ioOperation {
        if (dslContext == null) {
            connect()
        }

        dslContext!!
    }

    fun closeConnection() {
        connection?.close()
    }
}

enum class VideoStatus(val value: String) {
    PENDING("PENDING"), PENDING_UPLOAD("PENDING_UPLOAD"), UPLOADING("UPLOADING"), PROCESSING("PROCESSING"), READY("READY"), DELETING(
        "DELETING"
    );

    override fun toString(): String {
        return value
    }
}

class DBHelpers {
    companion object {
        private val db = DB.getInstance()

        suspend fun createAdminUser(username: String, password: String) =
            ioOperationWithErrorHandling("Cannot create user.") {
                db.getDSLContext().insertInto(AdminUsers).columns(AdminUsers.USERNAME, AdminUsers.PASSWORD)
                    .values(username, password).onConflict(DSL.field("username")).doNothing().execute()
            }

        suspend fun getAdminUser(username: String): Record = ioOperationWithErrorHandling("Cannot get user info.") {
            db.getDSLContext().select().from(AdminUsers).where(AdminUsers.USERNAME.eq(username)).fetchOne()
                ?: throw Exception()
        }

        suspend fun updateVideoSatus(id: UUID, status: VideoStatus) =
            ioOperationWithErrorHandling("Cannot update video satus.") {
                db.getDSLContext().update(Videos).set(Videos.STATUS, status.value).where(Videos.ID.eq(id)).execute()
            }

        suspend fun setVideoReady(id: UUID) = ioOperationWithErrorHandling("Cannot set video status ready.") {
            db.getDSLContext().update(Videos).set(Videos.STATUS, VideoStatus.READY.value)
                .set(Videos.UPLOADEDAT, Timestamp(System.currentTimeMillis())).where(Videos.ID.eq(id))
                .and(Videos.STATUS.ne(VideoStatus.READY.value)).execute()
        }

        suspend fun getVideo(id: UUID): Record = ioOperationWithErrorHandling("Cannot get video info.") {
            db.getDSLContext().select().from(Videos).where(Videos.ID.eq(id)).fetchOne() ?: throw Exception()
        }

        suspend fun getVideoFilename(id: UUID): String =
            ioOperationWithErrorHandling("Cannot get src video filename.") {
                db.getDSLContext().select().from(VideosTemp).where(VideosTemp.ID.eq(id))
                    .fetchOne { record -> record.get(VideosTemp.FILENAME) } ?: throw Exception()
            }

        suspend fun deleteVideoTemp(id: UUID) = ioOperationWithErrorHandling("Cannot delete src video.") {
            db.getDSLContext().deleteFrom(VideosTemp).where(VideosTemp.ID.eq(id)).execute()
        }

        suspend fun createVideoTemp(id: UUID, filename: String) =
            ioOperationWithErrorHandling("Cannot create src video.") {
                db.getDSLContext().insertInto(VideosTemp).columns(
                    VideosTemp.ID, VideosTemp.FILENAME
                ).values(id, filename).onConflict().doNothing().execute()
            }

        private val CHUNK_SIZE = 5 * 1024 * 1024

        suspend fun createChunks(videoId: UUID, bufferSize: Int) =
            ioOperationWithErrorHandling("Cannot create video chunks.") {
                val numChunks = (bufferSize + CHUNK_SIZE - 1) / CHUNK_SIZE // Calculate total chunks

                for (i in 0 until numChunks) {
                    val start = i * CHUNK_SIZE
                    val end = minOf((i + 1) * CHUNK_SIZE, bufferSize)
                    val size = (end - start)

                    db.getDSLContext().insertInto(VideoChunks).columns(
                        VideoChunks.ID, VideoChunks.VIDEOID, VideoChunks.CHUNKSIZE, VideoChunks.START, VideoChunks.END
                    ).values(
                        UUID.randomUUID(), videoId, size, start, end - 1
                    ).execute()
                }
            }

        @Serializable
        data class VideoCreationResponse(
            val id: String, val chunks: List<Chunk>
        )

        @Serializable
        data class Chunk(
            val id: String, val chunkSize: Int, val start: Int, val end: Int
        )

        suspend fun getChunk(id: UUID, videoId: UUID): Chunk = ioOperationWithErrorHandling("Cannot get video chunk.") {
            db.getDSLContext().select(VideoChunks.ID, VideoChunks.CHUNKSIZE, VideoChunks.START, VideoChunks.END)
                .from(VideoChunks).where(VideoChunks.ID.eq(id)).and(VideoChunks.VIDEOID.eq(videoId))
                .fetchOne { record ->
                    Chunk(
                        id = record.get(VideoChunks.ID).toString(),
                        chunkSize = record.get(VideoChunks.CHUNKSIZE),
                        start = record.get(VideoChunks.START),
                        end = record.get(VideoChunks.END)
                    )
                } ?: throw Exception("Chunk $id of video $videoId not found.")
        }

        suspend fun deleteChunk(id: UUID, videoId: UUID) = ioOperationWithErrorHandling("Cannot delete video chunk.") {
            db.getDSLContext().deleteFrom(VideoChunks).where(VideoChunks.ID.eq(id)).and(VideoChunks.VIDEOID.eq(videoId))
                .execute()
        }

        suspend fun getAllChunks(videoId: UUID): List<Chunk> = ioOperationWithErrorHandling("Cannot get all chunks.") {
            db.getDSLContext().select(
                VideoChunks.ID, VideoChunks.CHUNKSIZE, VideoChunks.START, VideoChunks.END
            ).from(VideoChunks).where(VideoChunks.VIDEOID.eq(videoId)).fetch { record ->
                Chunk(
                    id = record.get(VideoChunks.ID).toString(),
                    chunkSize = record.get(VideoChunks.CHUNKSIZE),
                    start = record.get(VideoChunks.START),
                    end = record.get(VideoChunks.END)
                )
            }
        }

        suspend fun createVideo(
            name: String, isPremium: Boolean, bufferSize: Int, extension: String
        ): VideoCreationResponse = ioOperationWithErrorHandling("Cannot create video.") {
            val id = UUID.randomUUID()
            db.getDSLContext().insertInto(Videos).columns(
                Videos.ID, Videos.NAME, Videos.ISPREMIUM, Videos.STATUS
            ).values(id, name, isPremium, VideoStatus.PENDING.name).execute()

            createVideoTemp(id, "$id.${extension}")
            createChunks(id, bufferSize)

            VideoCreationResponse(
                id.toString(), getAllChunks(id)
            )
        }

        suspend fun createVideoV2(
            id: UUID, extension: String, name: String, isPremium: Boolean
        ) = ioOperationWithErrorHandling("Cannot create video.") {
            db.getDSLContext().insertInto(Videos).columns(
                Videos.ID, Videos.NAME, Videos.ISPREMIUM, Videos.STATUS
            ).values(id, name, isPremium, VideoStatus.PROCESSING.name).execute()

            createVideoTemp(id, "$id.$extension")
        }


        @Serializable
        data class AdminVideos(
            val id: String,
            val name: String,
            val createdAt: String,
            val uploadedAt: String,
            val status: String,
            val isPremium: Boolean,
            val uniqueViews: Int,
            val allViews: Int,
            val likes: Int
        )

        suspend fun getAllVideosAdmin(): List<AdminVideos> =
            ioOperationWithErrorHandling("Cannot get all videos for admin.") {
                val unique_views = DSL.field("COUNT(DISTINCT views.user_id) AS unique_views")
                val all_views = DSL.field("COUNT(views.user_id) AS all_views")
                val likes = DSL.field("COUNT(likes.user_id) AS likes")

                val query = db.getDSLContext().select(
                    Videos.ID,
                    Videos.NAME,
                    Videos.CREATEDAT,
                    Videos.UPLOADEDAT,
                    Videos.STATUS,
                    Videos.ISPREMIUM,
                    unique_views,
                    all_views,
                    likes
                ).from(Videos).leftJoin(Views).on("videos.id = views.video_id").leftJoin(Likes)
                    .on("videos.id = likes.video_id").groupBy(DSL.field("videos.id"))

                query.fetch { record ->
                    AdminVideos(
                        id = record[Videos.ID].toString(),
                        name = record[Videos.NAME],
                        createdAt = record[Videos.CREATEDAT].toString(),
                        uploadedAt = record[Videos.UPLOADEDAT].toString(),
                        status = record[Videos.STATUS],
                        isPremium = record[Videos.ISPREMIUM],
                        uniqueViews = record[unique_views, Int::class.java] ?: 0,
                        allViews = record[all_views, Int::class.java] ?: 0,
                        likes = record[likes, Int::class.java] ?: 0
                    )
                }
            }

        @Serializable
        data class AdminVideo(
            val id: String,
            val name: String,
            val createdAt: String,
            val uploadedAt: String,
            val status: String,
            val isPremium: Boolean,
        )

        suspend fun getVideoAdmin(id: UUID): AdminVideo =
            ioOperationWithErrorHandling("Cannot get video info for admin.") {
                db.getDSLContext().select().from(Videos).where(Videos.ID.eq(id)).fetchOne { record ->
                    AdminVideo(
                        id = record[Videos.ID].toString(),
                        name = record[Videos.NAME],
                        createdAt = record[Videos.CREATEDAT].toString(),
                        uploadedAt = record[Videos.UPLOADEDAT].toString(),
                        status = record[Videos.STATUS],
                        isPremium = record[Videos.ISPREMIUM],
                    )
                } ?: throw Exception("No video info found for id $id.")

            }

        @Serializable
        data class VideoToDelete(
            val id: String, val filename: String?
        )

        suspend fun getVideosToDelete(): List<VideoToDelete> =
            ioOperationWithErrorHandling("Cannot get videos that should be deleted.") {
                val videosId = DSL.field("videos.id")
                val result = db.getDSLContext().select(videosId, VideosTemp.FILENAME).from(Videos).leftJoin(VideosTemp)
                    .on("videos.id = videos_temp.id").where(Videos.STATUS.ne(VideoStatus.READY.value))
                    .and(Videos.CREATEDAT.lessThan(DSL.field("NOW() - INTERVAL '2 hours'", Timestamp::class.java)))
                    .or(Videos.STATUS.eq(VideoStatus.DELETING.value)).fetch { record ->
                        VideoToDelete(
                            id = record[videosId].toString(), filename = record[VideosTemp.FILENAME]
                        )
                    }

                db.getDSLContext().deleteFrom(Videos).where(Videos.STATUS.ne(VideoStatus.READY.value))
                    .and(Videos.CREATEDAT.lessThan(DSL.field("NOW() - INTERVAL '2 hours'", Timestamp::class.java)))
                    .or(Videos.STATUS.eq(VideoStatus.DELETING.value)).execute()

                result
            }

        @Serializable
        data class User(
            val id: String, val name: String, val subscription_level: String, val subscription_till: String?
        )

        suspend fun getUser(username: String): User = ioOperationWithErrorHandling("Cannot get user.") {
            db.getDSLContext().select(Users.asterisk()).from(Users).where(Users.ID.eq(username)).fetchOne { record ->
                User(
                    id = record[Users.ID],
                    name = record[Users.NAME],
                    subscription_level = record[Users.SUBSCRIPTIONLEVEL],
                    subscription_till = record[Users.SUBSCRIPTIONTILL]?.toString()
                )
            } ?: throw Exception()
        }

        suspend fun getUserPassword(username: String): String = ioOperationWithErrorHandling("Cannot get user.") {
            db.getDSLContext().select().from(Users).where(Users.ID.eq(username))
                .fetchOne { record -> record[Users.PASSWORD] } ?: ""
        }

        suspend fun createUser(username: String, name: String, password: String) =
            ioOperationWithErrorHandling("Cannot create a new user.") {
                db.getDSLContext().insertInto(Users).columns(Users.ID, Users.NAME, Users.PASSWORD)
                    .values(username, name, password).onConflict(Users.ID).doNothing().execute()
            }

        suspend fun createSession(username: String): String = ioOperationWithErrorHandling("Cannot create a session.") {
            val id = UUID.randomUUID()

            db.getDSLContext().insertInto(Sessions).columns(Sessions.ID, Sessions.USERID).values(id, username).execute()

            id.toString()
        }

        suspend fun getSession(sessionId: UUID): String = ioOperationWithErrorHandling("Cannot find the session.") {
            val username =
                db.getDSLContext().select(Sessions.USERID).from(Sessions).where(Sessions.ID.eq(sessionId)).and(
                        Sessions.ACCESSEDAT.greaterThan(
                            DSL.field(
                                "NOW() -  INTERVAL '30 minutes'", Timestamp::class.java
                            )
                        )
                    ).fetchOne { record -> record[Sessions.USERID].toString() }
            username ?: throw Exception()

            db.getDSLContext().update(Sessions).set(Sessions.ACCESSEDAT, DSL.currentTimestamp())
                .where(Sessions.USERID.eq(username)).execute()

            username
        }

        suspend fun deleteSession(sessionId: UUID) = ioOperationWithErrorHandling("Cannot delete the session.") {
            db.getDSLContext().deleteFrom(Sessions).where(Sessions.ID.eq(sessionId)).execute()
        }

        @Serializable
        data class VideoData(
            val id: String,
            val name: String,
            val createdAt: String,
            val uploadedAt: String,
            val status: String,
            val isPremium: Boolean,
            val views: Int,
            val likes: Int,
            val viewed: Boolean,
            val liked: Boolean
        )

        suspend fun getAllVideos(username: String, searchText: String?): List<VideoData> =
            ioOperationWithErrorHandling("Cannot get all videos for admin.") {
                val views = DSL.field("COUNT(DISTINCT views.user_id) AS unique_views")
                val likes = DSL.field("COUNT(likes.user_id) AS likes")
                val viewed = DSL.field("COUNT(*) FILTER (WHERE views.user_id = ?) > 0 AS viewed", username)
                val liked = DSL.field("COUNT(*) FILTER (WHERE likes.user_id = ?) > 0 AS liked", username)

                val query = db.getDSLContext().select(
                    Videos.ID,
                    Videos.NAME,
                    Videos.CREATEDAT,
                    Videos.UPLOADEDAT,
                    Videos.STATUS,
                    Videos.ISPREMIUM,
                    views,
                    likes,
                    viewed,
                    liked
                ).from(Videos).leftJoin(Views).on("videos.id = views.video_id").leftJoin(Likes)
                    .on("videos.id = likes.video_id")
                    .where(Videos.STATUS.eq(VideoStatus.READY.value))

                if (searchText != null) query.and(Videos.NAME.likeIgnoreCase("%$searchText%"))

                query.groupBy(DSL.field("videos.id")).orderBy(Videos.UPLOADEDAT.desc())

                query.fetch { record ->
                    VideoData(
                        id = record[Videos.ID].toString(),
                        name = record[Videos.NAME],
                        createdAt = record[Videos.CREATEDAT].toString(),
                        uploadedAt = record[Videos.UPLOADEDAT].toString(),
                        status = record[Videos.STATUS],
                        isPremium = record[Videos.ISPREMIUM],
                        views = record[views, Int::class.java] ?: 0,
                        likes = record[likes, Int::class.java] ?: 0,
                        viewed = record[viewed, Boolean::class.java] ?: false,
                        liked = record[liked, Boolean::class.java] ?: false
                    )
                }
            }

        suspend fun createVideoView(username: String, videoId: UUID) =
            ioOperationWithErrorHandling("Cannot create a video view.") {
                db.getDSLContext().insertInto(Views).columns(Views.USERID, Views.VIDEOID).values(username, videoId)
                    .execute()
            }

        suspend fun editVideoAdmin(id: UUID, name: String?, isPremium: Boolean?) = ioOperationWithErrorHandling("Cannot edit a video.") {
            val query = db.getDSLContext().update(Videos)

            if(name != null) query.set(Videos.NAME, name)
            if(isPremium != null) query.set(Videos.ISPREMIUM, isPremium)

            (query as UpdateSetMoreStep<Record>).where(Videos.ID.eq(id)).execute()
        }
    }
}