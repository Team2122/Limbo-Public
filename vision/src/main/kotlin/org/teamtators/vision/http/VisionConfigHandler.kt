package org.teamtators.vision.http

import com.fasterxml.jackson.databind.ObjectMapper
import org.glassfish.grizzly.http.Method
import org.glassfish.grizzly.http.server.HttpHandler
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.glassfish.grizzly.http.util.HttpStatus
import org.teamtators.vision.config.Config
import org.teamtators.vision.loggerFactory

class VisionConfigHandler constructor(
        val objectMapper: ObjectMapper,
        val _config: Config
) : WebHandler() {
    companion object {
        val logger by loggerFactory()
    }

    override fun serve(request: Request, response: Response) {
        val writer = objectMapper.writer()
        if (request.method == Method.GET) {
            val out = response.getOutputStream()
            writer.writeValue(out, _config.vision)
        } else if (request.method == Method.PUT) {
            val input = request.getInputStream()
            val reader = objectMapper.readerForUpdating(_config.vision)
            _config.vision = reader.readValue(input)
            val out = response.getOutputStream()
            writer.writeValue(out, _config.vision)
            logger.debug("Updated vision config with data");
        } else {
            response.setStatus(HttpStatus.METHOD_NOT_ALLOWED_405)
        }
    }
}