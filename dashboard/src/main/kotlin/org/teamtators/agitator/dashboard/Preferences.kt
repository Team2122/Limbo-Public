package org.teamtators.agitator.dashboard

import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.util.*

object Preferences {
    private val logger = LoggerFactory.getLogger(Preferences::class.java)

    private val props: Properties = Properties()

    val FILENAME = "dashboard.properties"

    init {
        try {
            props.load(FileInputStream(FILENAME))
        } catch (e: FileNotFoundException) {
            // ignore missing file
            logger.info("Preferences file missing, using defaults")
        }
        props.list(System.out)
    }

    fun get(key: String, defaultValue: String = ""): String {
        if (!props.containsKey(key)) {
            set(key, defaultValue)
        }
        return props.getProperty(key)
    }

    fun set(key: String, value: String) {
        props.setProperty(key, value)
        FileOutputStream(FILENAME).use {
            props.store(it, "Dashboard Preferences")
        }
    }

    var serverUri: String
        get() = get("SERVER_URI", "tcp://10.21.22.11:5800")
        set(value) = set("SERVER_URI", value)

    var streamUri: String
        get() = get("STREAM_URI", "http://10.21.22.11:5801/stream.mjpg")
        set(value) = set("STREAM_URI", value)
}