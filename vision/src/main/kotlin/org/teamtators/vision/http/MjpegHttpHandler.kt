package org.teamtators.vision.http

import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DefaultSubscriber
import org.glassfish.grizzly.http.server.HttpHandler
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.reactivestreams.Publisher
import org.teamtators.vision.loggerFor
import java.awt.image.BufferedImage
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO


class MjpegHttpHandler constructor(val imageSource: Flowable<BufferedImage>,
                                   val maxFps : Double) :
        HttpHandler("MjpegHttpHandler") {
    companion object {
        private val logger = loggerFor<MjpegHttpHandler>()
    }

    private val BOUNDARY = "--JPEG_BOUNDARY\r\nContent-type: image/jpg\r\n\r\n".toByteArray()

    @Throws(Exception::class)
    override fun service(request: Request, response: Response) {
        response.contentType = "multipart/x-mixed-replace; boundary=--JPEG_BOUNDARY"
        response.addHeader("Connection", "close")
        response.addHeader("Max-Age", "0")
        response.addHeader("Expires", "0")
        response.addHeader("Cache-Control", "no-cache, private")
        response.addHeader("Pragma", "no-cache")

        response.suspend()

        logger.debug("Client connected to mjpeg stream. Starting image stream")
        imageSource
                .throttleLast((1000 / maxFps).toLong(), TimeUnit.MILLISECONDS)
                .onBackpressureDrop { logger.debug("MJPEG dropped frame") }
                .observeOn(Schedulers.io())
                .subscribe(MjpegWriter(response))
    }

    private inner class MjpegWriter(private val response: Response) : DefaultSubscriber<BufferedImage>() {
        val out: OutputStream = response.outputStream

        override fun onComplete() {
            out.close()
        }

        override fun onError(t: Throwable) {
            logger.error("Received error", t)
            val message: String? = t.message
            if (message != null) {
                out.write(message.toByteArray());
            }
            out.flush()
            out.close()
        }

        private var finished = false

        override fun onNext(image: BufferedImage) {
            if (response.outputBuffer.isClosed || finished)
                return
            try {
                writeImage(image, out)
                return
            } catch (e: IOException) {
                logger.error("Error writing image {}", e.message)
            }

            logger.debug("Unsubscribing MjpegWriter")
            this.cancel()
            finished = true
        }

        @Throws(IOException::class)
        private fun writeImage(image: BufferedImage, out: OutputStream) {
            out.write(BOUNDARY)
            ImageIO.write(image, "jpg", out)
            out.write("\r\n\r\n".toByteArray())
            out.flush()
        }
    }
}
