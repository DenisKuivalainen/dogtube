import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.TableImpl
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

object AdminUsers : TableImpl<Record>(DSL.name("admin_users")) {
    val USERNAME: Field<String> = DSL.field(DSL.name("username"), String::class.java)
    val PASSWORD: Field<String> = DSL.field(DSL.name("password"), String::class.java)
}

enum class VideoStatus(val value: String) {
    PENDING("PENDING"), PENDING_UPLOAD("PENDING_UPLOAD"), UPLOADING("UPLOADING"), PROCESSING("PROCESSING"), READY("READY"), DELETING(
        "DELETING"
    );

    override fun toString(): String {
        return value
    }
}

object Videos : TableImpl<Record>(DSL.name("videos")) {
    val ID: Field<UUID> = DSL.field(DSL.name("id"), UUID::class.java)
    val NAME: Field<String> = DSL.field(DSL.name("name"), String::class.java)
    val CREATEDAT: Field<Timestamp> = DSL.field(DSL.name("created_at"), Timestamp::class.java)
    val UPLOADEDAT: Field<Timestamp?> = DSL.field(DSL.name("uploaded_at"), Timestamp::class.java)
    val STATUS: Field<String> = DSL.field(DSL.name("status"), String::class.java)
    val ISPREMIUM: Field<Boolean> = DSL.field(DSL.name("is_premium"), Boolean::class.java)
}

object VideosTemp : TableImpl<Record>(DSL.name("videos_temp")) {
    val ID: Field<UUID> = DSL.field(DSL.name("id"), UUID::class.java)
    val FILENAME: Field<String> = DSL.field(DSL.name("filename"), String::class.java)
}

object VideoChunks : TableImpl<Record>(DSL.name("video_chunks")) {
    val ID: Field<UUID> = DSL.field(DSL.name("id"), UUID::class.java)
    val VIDEOID: Field<UUID> = DSL.field(DSL.name("video_id"), UUID::class.java)
    val CHUNKSIZE: Field<Int> = DSL.field(DSL.name("chunk_size"), Int::class.java)
    val START: Field<Int> = DSL.field(DSL.name("start_position"), Int::class.java)
    val END: Field<Int> = DSL.field(DSL.name("end_position"), Int::class.java)
}

object Views : TableImpl<Record>(DSL.name("views")) {
    val VIDEOID: Field<UUID> = DSL.field(DSL.name("video_id"), UUID::class.java)
    val USERID: Field<UUID> = DSL.field(DSL.name("user_id"), UUID::class.java)
    val VIEWEDAT: Field<Timestamp> = DSL.field(DSL.name("viewed_at"), Timestamp::class.java)
}

object Likes : TableImpl<Record>(DSL.name("likes")) {
    val VIDEOID: Field<UUID> = DSL.field(DSL.name("video_id"), UUID::class.java)
    val USERID: Field<UUID> = DSL.field(DSL.name("user_id"), UUID::class.java)
    val LIKEDAT: Field<Timestamp> = DSL.field(DSL.name("liked_at"), Timestamp::class.java)
}

class DBHelpers {
    companion object {
        private val db = DB.getInstance()

        suspend fun putAdminUser(username: String, password: String) = ioOperation {
            db.getDSLContext().insertInto(AdminUsers).columns(AdminUsers.USERNAME, AdminUsers.PASSWORD)
                .values(username, password).onConflict(DSL.field("username")).doNothing().execute()
        }

        suspend fun getAdminUser(username: String): Record? = ioOperation {
            db.getDSLContext().select().from(AdminUsers).where(AdminUsers.USERNAME.eq(username)).fetchOne()
        }

        suspend fun updateVideoSatus(id: UUID, status: VideoStatus) = ioOperation {
            db.getDSLContext().update(Videos).set(Videos.STATUS, status.value).where(Videos.ID.eq(id)).execute()
        }

        suspend fun setVideoReady(id: UUID) = ioOperation {
            db.getDSLContext().update(Videos).set(Videos.STATUS, VideoStatus.READY.value)
                .set(Videos.UPLOADEDAT, Timestamp(System.currentTimeMillis())).where(Videos.ID.eq(id)).execute()
        }

        suspend fun getVideo(id: UUID): Record? = ioOperation {
            db.getDSLContext().select().from(Videos).where(Videos.ID.eq(id)).fetchOne()
        }

        suspend fun getVideoFilename(id: UUID): String? = ioOperation {
            db.getDSLContext().select().from(VideosTemp).where(VideosTemp.ID.eq(id))
                .fetchOne { record -> record.get(VideosTemp.FILENAME) }
        }

        suspend fun deleteVideoTemp(id: UUID) = ioOperation {
            db.getDSLContext().deleteFrom(VideosTemp).where(VideosTemp.ID.eq(id)).execute()
        }

        suspend fun createVideoTemp(id: UUID, filename: String) = ioOperation {
            db.getDSLContext().insertInto(VideosTemp).columns(
                VideosTemp.ID, VideosTemp.FILENAME
            ).values(id, filename).onConflict().doNothing().execute()
        }

        private val CHUNK_SIZE = 5 * 1024 * 1024

        suspend fun createChunks(videoId: UUID, bufferSize: Int) = ioOperation {
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

        suspend fun getChunk(id: UUID, videoId: UUID): Chunk? = ioOperation {
            db.getDSLContext().select(VideoChunks.ID, VideoChunks.CHUNKSIZE, VideoChunks.START, VideoChunks.END)
                .from(VideoChunks).where(VideoChunks.ID.eq(id)).and(VideoChunks.VIDEOID.eq(videoId))
                .fetchOne { record ->
                    Chunk(
                        id = record.get(VideoChunks.ID).toString(),
                        chunkSize = record.get(VideoChunks.CHUNKSIZE),
                        start = record.get(VideoChunks.START),
                        end = record.get(VideoChunks.END)
                    )
                }
        }

        suspend fun deleteChunk(id: UUID, videoId: UUID) = ioOperation {
            db.getDSLContext().deleteFrom(VideoChunks).where(VideoChunks.ID.eq(id)).and(VideoChunks.VIDEOID.eq(videoId))
                .execute()
        }

        suspend fun getAllChunks(videoId: UUID): List<Chunk> = ioOperation {
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

        suspend fun createVideoV2(
            name: String, isPremium: Boolean, bufferSize: Int, extension: String
        ): VideoCreationResponse? = ioOperation {
            try {
                val id = UUID.randomUUID()
                db.getDSLContext().insertInto(Videos).columns(
                    Videos.ID, Videos.NAME, Videos.ISPREMIUM, Videos.STATUS
                ).values(id, name, isPremium, VideoStatus.PENDING.name).execute()

                createVideoTemp(id, "$id.${extension}")
                createChunks(id, bufferSize)

                VideoCreationResponse(
                    id.toString(), getAllChunks(id)
                )
            } catch (e: SQLException) {
                null
            }
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

        suspend fun getAllVideosAdmin(): List<AdminVideos> = ioOperation {
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

        suspend fun getVideoAdmin(id: UUID): AdminVideo? = ioOperation {
            db.getDSLContext().select().from(Videos).where(Videos.ID.eq(id)).fetchOne { record ->
                AdminVideo(
                    id = record[Videos.ID].toString(),
                    name = record[Videos.NAME],
                    createdAt = record[Videos.CREATEDAT].toString(),
                    uploadedAt = record[Videos.UPLOADEDAT].toString(),
                    status = record[Videos.STATUS],
                    isPremium = record[Videos.ISPREMIUM],
                )
            }
        }

        @Serializable
        data class VideoToDelete(
            val id: String, val filename: String?
        )

        suspend fun getVideosToDelete(): List<VideoToDelete> = ioOperation {
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
    }
}