package org.teamtators.vision.http

import org.teamtators.vision.MainModule
import org.teamtators.vision.Module
import org.teamtators.vision.vision.VisionModule

class HttpModule(mainModule: MainModule, visionModule: VisionModule) : Module {
    val visionServer = VisionServer(mainModule.config, mainModule.jsonMapper, visionModule.displayImages)

    override fun start() {
        visionServer.start()
    }

    override fun stop() {
        visionServer.stop()
    }
}