package org.teamtators.vision.http

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.jsonSchema.customProperties.HyperSchemaFactoryWrapper
import io.reactivex.Flowable
import org.glassfish.grizzly.PortRange
import org.glassfish.grizzly.http.server.*
import org.reactivestreams.Publisher
import org.teamtators.vision.config.Config
import org.teamtators.vision.vision.VisionConfig
import org.teamtators.vision.loggerFor
import java.awt.image.BufferedImage
import java.io.IOException

class VisionServer constructor(
        _config: Config,
        objectMapper: ObjectMapper,
        imageSource: Flowable<BufferedImage>
) {
    companion object {
        private val logger = loggerFor<VisionServer>()
    }

    private val config = _config.server
    private val server = HttpServer()
    private val listener = NetworkListener("grizzly", "0.0.0.0", config.port)

    init {
        server.addListener(listener)
    }

    class VisionConfigSchemaHandler(val objectMapper: ObjectMapper) : WebHandler() {
        override fun serve(request: Request, response: Response) {
            val visitor = HyperSchemaFactoryWrapper()
            objectMapper.acceptJsonFormatVisitor(VisionConfig::class.java, visitor)
            val schema = visitor.finalSchema()
            val out = response.getOutputStream()
            objectMapper.writer().writeValue(out, schema)
        }
    }

    val mjpegHttpHandler = MjpegHttpHandler(imageSource, config.streamFps)
    val visionConfigHandler = VisionConfigHandler(objectMapper, _config)
    val visionConfigSchemaHandler = VisionConfigSchemaHandler(objectMapper)

    fun start() {
        server.serverConfiguration.apply {
            addHttpHandler(mjpegHttpHandler, "/stream.mjpg")
            addHttpHandler(visionConfigHandler, "/visionConfig")
            addHttpHandler(visionConfigSchemaHandler, "/visionConfigSchema")
            addHttpHandler(CLStaticHttpHandler(javaClass.classLoader, "www/"))
        }
        try {
            server.start()
            logger.info("Started HTTP server on port {}", config.port)
        } catch (e: IOException) {
            logger.error("Error starting HTTP server", e)
        }
    }

    fun stop() {
        server.shutdownNow()
    }
}