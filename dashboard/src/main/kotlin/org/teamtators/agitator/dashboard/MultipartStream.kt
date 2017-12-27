package org.teamtators.agitator.dashboard

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.functions.Cancellable
import io.reactivex.schedulers.Schedulers
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * @author Alex Mikhalev
 */

class MultipartStream(val url: URL) {
    companion object {
        private val logger = loggerFor<MultipartStream>()
        private val CONTENT_TYPE = "multipart/x-mixed-replace"
        private val BOUNDARY_PART = "boundary="
        private val CONTENT_TYPE_HEADER = "content-length"
    }

    val observable =
            Observable.create<ByteArray> {
                val conn = url.openConnection() as HttpURLConnection
                conn.readTimeout = 0
                logger.debug("Connecting to mjpg stream at $url")
                conn.connect()
                val contentType = conn.contentType
                if (contentType != null && !contentType.startsWith(CONTENT_TYPE)) {
                    throw IOException("Invalid content type: $contentType")
                }

                val boundary = contentType.substring(contentType.indexOf(BOUNDARY_PART) + BOUNDARY_PART.length)

                val streamer = Streamer(it, conn, boundary)
                it.setCancellable(streamer)
                streamer.start()
            }.subscribeOn(Schedulers.io())

    class Streamer(val emitter: ObservableEmitter<ByteArray>,
                   val conn: HttpURLConnection,
                   val boundary: String) : AutoCloseable, Cancellable {
        val stream = BufferedReader(InputStreamReader(conn.inputStream))

        fun start() {
//            readUntilBoundary()
            while (!emitter.isDisposed) {
            }
        }

        private fun readLine(): String {
            return stream.readLine()
        }

//        https://github.com/ArseniKavalchuk/multipart-x-mixed-replace-java-player/tree/master/src/main/java/ru/synesis/media/player
//        private fun readUntilBoundary(): Array<Byte> {
//
//        }

        override fun close() {
            stream.close()
            conn.disconnect()
        }

        override fun cancel() {
            close()
        }
    }
}
