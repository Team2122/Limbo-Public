package org.teamtators.vision.mqtt

import org.teamtators.vision.MainModule
import org.teamtators.vision.Module
import org.teamtators.vision.vision.VisionModule

/**
 * @author Alex Mikhalev
 */
class MqttModule(main: MainModule, vision: VisionModule) : Module {
    val mqttUpdater = MqttUpdater(main.config, main.jsonMapper, vision.processResults, vision.robotData)

    override fun start() {
        mqttUpdater.start()
    }

    override fun stop() {
        mqttUpdater.stop()
    }
}