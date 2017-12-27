package org.teamtators.vision.config

import org.teamtators.vision.http.ServerConfig
import org.teamtators.vision.mqtt.MqttConfig
import org.teamtators.vision.tables.TablesConfig
import org.teamtators.vision.vision.VisionConfig

class Config {
    var tables: TablesConfig = TablesConfig()
    var server: ServerConfig = ServerConfig()
    var vision: VisionConfig = VisionConfig()
    var mqtt: MqttConfig = MqttConfig()
    var profile = false
    var display = false
}