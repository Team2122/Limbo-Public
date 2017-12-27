package org.teamtators.vision.util;

import org.slf4j.Logger
import org.slf4j.event.Level
import org.teamtators.vision.loggerFor
import java.io.BufferedReader
import java.io.Closeable
import java.io.InputStream
import java.io.InputStreamReader

class InputStreamLogger(name: String,
                        val inputStream: InputStream,
                        var level: Level = Level.DEBUG) : Thread(name), Closeable {
    val streamReader = InputStreamReader(inputStream)
    val reader = BufferedReader(streamReader)
    val logger: Logger = loggerFor(javaClass)

    override fun close() {
        reader.close()
    }

    override fun run() {
        var line: String?
        while (true) {
            line = reader.readLine()
            if (line == null) break;
            val message = "$ ${line}"
            when (level) {
                Level.ERROR -> logger.error(message)
                Level.WARN -> logger.warn(message)
                Level.INFO -> logger.info(message)
                Level.DEBUG -> logger.debug(message)
                Level.TRACE -> logger.trace(message)
            }
        }
    }
}
