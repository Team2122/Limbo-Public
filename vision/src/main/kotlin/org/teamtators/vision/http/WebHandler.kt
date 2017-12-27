package org.teamtators.vision.http

import org.glassfish.grizzly.http.server.HttpHandler
import org.glassfish.grizzly.http.server.Request
import org.glassfish.grizzly.http.server.Response
import org.glassfish.grizzly.http.util.HttpStatus

abstract class WebHandler : HttpHandler() {
    override fun service(request: Request?, response: Response?) {
        try {
            serve(request!!, response!!)
        } catch (e: Throwable) {
            VisionConfigHandler.logger.error("Error handling HTTP request", e)
            response!!.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500)
        }
    }

    abstract fun serve(request: Request, response: Response)
}