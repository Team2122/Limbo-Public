package org.teamtators.vision.display

import org.teamtators.vision.MainModule
import org.teamtators.vision.Module
import org.teamtators.vision.vision.VisionModule


class DisplayModule(main: MainModule, vision: VisionModule) : Module {
    val visionDisplay = VisionDisplay(main.config, vision.displayImages)

    override fun start() {
        visionDisplay.start()
    }

    override fun stop() {
        visionDisplay.stop()
    }

}