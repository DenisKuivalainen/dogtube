package com.example.src.utils

import ioOperation

suspend fun resizeVideo(input: String, output: String): Boolean = ioOperation {
    try {
        val processBuilder = ProcessBuilder(
            "ffmpeg",
            "-i",
            FSHelpers.getVideoSrcFilePath(input),
            "-vf",
            "scale=-1:720",
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