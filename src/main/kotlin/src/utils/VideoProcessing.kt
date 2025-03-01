package com.example.src.utils

import ioOperation

suspend fun resizeVideo(input: String, output: String): Boolean = ioOperation {
    try {
        val processBuilder = ProcessBuilder(
            "ffmpeg",
            "-i",
            FSHelpers.getVideoSrcFilePath(input),
            "-vf",
            """
            format=yuv420p,
            scale='if(gt(iw/ih,16/9),-2,min(iw,ih*16/9))':'if(gt(iw/ih,16/9),min(iw*9/16,ih),-2)',
            crop='min(iw,ih*16/9)':'min(iw*9/16,ih)',
            scale='if(lt(iw,1280),854,1280)':'if(lt(ih,720),480,720)'
            """.trimIndent(),
            "-c:v",
            "libx264",
            "-preset",
            "slow",
            "-crf",
            "18",
            "-c:a",
            "aac",
            "-b:a",
            "128k",
            "-movflags",
            "+faststart",
            FSHelpers.getProcessedVideoFilePath(output)
        )
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()
        process.inputStream.bufferedReader().use { it.lines().forEach(::println) }
        process.waitFor()

        if (process.exitValue() != 0) {
            throw Exception("Video was not processed.")
        }
        true
    } catch (e: Exception) {
        println(e.message)
        false
    }
}