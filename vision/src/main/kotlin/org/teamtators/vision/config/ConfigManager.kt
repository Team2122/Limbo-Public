package org.teamtators.vision.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.teamtators.vision.loggerFor
import java.io.File

class ConfigManager constructor(
        val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = loggerFor<ConfigManager>()
    }

    fun loadConfig(): Config {
        var configFile: String? = System.getenv("TATORVISION_CONFIG")
        if (configFile == null)
            configFile = "./config.yml"

        return loadConfig(configFile)
    }

    fun loadConfig(configFile: String): Config {
        try {
            val file = File(configFile).canonicalFile
            logger.debug("Reading configuration from {}", file)
            return objectMapper.readValue(file, Config::class.java)
        } catch (e: Throwable) {
            logger.error("Error reading configuration file", e)
            throw e;
        }
    }
}