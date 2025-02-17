package com.example.src.utils

import ioOperation
import ioOperationWithErrorHandling
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private val height = 240
private val width = 320

suspend fun processImage(input: ByteArray): ByteArray = ioOperation {
    val image = ImageIO.read(ByteArrayInputStream(input))

    val scaledWidth: Int
    val scaledHeight: Int
    if (image.width > image.height) {
        scaledHeight = height
        scaledWidth = (image.width * height) / image.height
    } else {
        scaledHeight = (image.height * width) / image.width
        scaledWidth = width
    }
    val resizedImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH)

    val resizedImageBuffer = BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB)
    val g2d: Graphics2D = resizedImageBuffer.createGraphics()
    g2d.drawImage(resizedImage, 0, 0, null)
    g2d.dispose()

    val croppedImage = resizedImageBuffer.getSubimage(
        (resizedImageBuffer.width - width) / 2, (resizedImageBuffer.height - height) / 2, width, height
    )

    val byteArrayOutputStream = ByteArrayOutputStream()
    ImageIO.write(croppedImage, "jpg", byteArrayOutputStream)
    byteArrayOutputStream.toByteArray()
}

suspend fun generateThumbnail(videoId: String, srcVideoName: String) =
    ioOperationWithErrorHandling("Cannot generate thumbnail") {
        val process = ProcessBuilder(
            "ffmpeg",
            "-i",
            srcVideoName,
            "-vf",
            "select=eq(n\\,0)",
            "-vsync",
            "vfr",
            "-q:v",
            "2",
            FSHelpers.getTempThumbnailPath()
        ).start()
        process.waitFor()

        val tempThumbnailBytes = FSHelpers.getTempThumbnailBytes()

        FSHelpers.deleteTempThumbnail()
        FSHelpers.saveThumbnail(videoId, processImage(tempThumbnailBytes))
    }

suspend fun blurImage(inputImage: ByteArray): ByteArray = ioOperation {
    val inputStream = ByteArrayInputStream(inputImage)
    val image: BufferedImage = ImageIO.read(inputStream)

    val kernelSize = 20
    val kernel = FloatArray(kernelSize * kernelSize) { 1f / (kernelSize * kernelSize) } // Average blur
    val convolveOp = ConvolveOp(Kernel(kernelSize, kernelSize, kernel))

    val blurredImage = convolveOp.filter(image, null)

    val width = blurredImage.width
    val height = blurredImage.height
    val newWidth = (width * 0.9).toInt()
    val newHeight = (height * 0.9).toInt()

    val xOffset = (width - newWidth) / 2
    val yOffset = (height - newHeight) / 2

    val croppedImage = blurredImage.getSubimage(xOffset, yOffset, newWidth, newHeight)

    val outputStream = ByteArrayOutputStream()
    ImageIO.write(croppedImage, "jpg", outputStream)

    outputStream.toByteArray()
}