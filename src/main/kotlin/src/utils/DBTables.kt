package com.example.src.utils

import org.jooq.Field
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.TableImpl
import java.sql.Timestamp
import java.util.*

object AdminUsers : TableImpl<Record>(DSL.name("admin_users")) {
    val USERNAME: Field<String> = DSL.field(DSL.name("username"), String::class.java)
    val PASSWORD: Field<String> = DSL.field(DSL.name("password"), String::class.java)
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
    val USERID: Field<String> = DSL.field(DSL.name("user_id"), String::class.java)
    val VIEWEDAT: Field<Timestamp> = DSL.field(DSL.name("viewed_at"), Timestamp::class.java)
}

object Likes : TableImpl<Record>(DSL.name("likes")) {
    val VIDEOID: Field<UUID> = DSL.field(DSL.name("video_id"), UUID::class.java)
    val USERID: Field<String> = DSL.field(DSL.name("user_id"), String::class.java)
    val LIKEDAT: Field<Timestamp> = DSL.field(DSL.name("liked_at"), Timestamp::class.java)
}

object Users : TableImpl<Record>(DSL.name("users")) {
    val ID: Field<String> = DSL.field(DSL.name("id"), String::class.java)
    val NAME: Field<String> = DSL.field(DSL.name("name"), String::class.java)
    val PASSWORD: Field<String> = DSL.field(DSL.name("password"), String::class.java)
    val SUBSCRIPTIONLEVEL: Field<String> = DSL.field(DSL.name("subscription_level"), String::class.java)
    val SUBSCRIPTIONTILL: Field<Timestamp> = DSL.field(DSL.name("subscription_till"), Timestamp::class.java)
    val CREATEDAT: Field<Timestamp> = DSL.field(DSL.name("created_at"), Timestamp::class.java)
    val UPDATEDAT: Field<Timestamp> = DSL.field(DSL.name("updated_at"), Timestamp::class.java)
}

object Sessions : TableImpl<Record>(DSL.name("sessions")) {
    val ID: Field<UUID> = DSL.field(DSL.name("id"), UUID::class.java)
    val USERID: Field<String> = DSL.field(DSL.name("id"), String::class.java)
    val ACCESSEDAT: Field<Timestamp> = DSL.field(DSL.name("accessed_at"), Timestamp::class.java)
}
