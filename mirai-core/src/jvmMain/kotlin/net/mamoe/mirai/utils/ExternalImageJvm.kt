@file:Suppress("EXPERIMENTAL_API_USAGE")

package net.mamoe.mirai.utils

import io.ktor.util.asStream
import kotlinx.io.core.Input
import kotlinx.io.core.IoBuffer
import kotlinx.io.core.buildPacket
import kotlinx.io.errors.IOException
import kotlinx.io.streams.asInput
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.security.MessageDigest
import javax.imageio.ImageIO

/*
 * 将各类型图片容器转为 [ExternalImage]
 */


/**
 * 读取 [BufferedImage] 的属性, 然后构造 [ExternalImage]
 */
@Throws(IOException::class)
fun BufferedImage.toExternalImage(formatName: String = "gif"): ExternalImage {
    val digest = MessageDigest.getInstance("md5")
    digest.reset()

    val buffer = buildPacket {
        ImageIO.write(this@toExternalImage, formatName, object : OutputStream() {
            override fun write(b: Int) {
                b.toByte().let {
                    this@buildPacket.writeByte(it)
                    digest.update(it)
                }
            }
        })
    }

    return ExternalImage(width, height, digest.digest(), formatName, buffer)
}

/**
 * 读取文件头识别图片属性, 然后构造 [ExternalImage]
 */
@Throws(IOException::class)
fun File.toExternalImage(): ExternalImage {
    val input = ImageIO.createImageInputStream(this)
    val image = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: error("Unable to read file(${this.path}), no ImageReader found")
    image.input = input

    return ExternalImage(
        width = image.getWidth(0),
        height = image.getHeight(0),
        md5 = input.md5(),
        imageFormat = image.formatName,
        input = this.inputStream().asInput(IoBuffer.Pool),
        inputSize = this.length()
    )
}

/**
 * 下载文件到临时目录然后调用 [File.toExternalImage]
 */
@Throws(IOException::class)
fun URL.toExternalImage(): ExternalImage {
    val file = createTempFile().apply { deleteOnExit() }
    openStream().transferTo(FileOutputStream(file))
    return file.toExternalImage()
}

/**
 * 保存为临时文件然后调用 [File.toExternalImage]
 */
@Throws(IOException::class)
fun InputStream.toExternalImage(): ExternalImage {
    val file = createTempFile().apply { deleteOnExit() }
    this.transferTo(FileOutputStream(file))
    return file.toExternalImage()
}

/**
 * 保存为临时文件然后调用 [File.toExternalImage]
 */
@Throws(IOException::class)
fun Input.toExternalImage(): ExternalImage {
    val file = createTempFile().apply { deleteOnExit() }
    this.asStream().transferTo(FileOutputStream(file))
    return file.toExternalImage()
}