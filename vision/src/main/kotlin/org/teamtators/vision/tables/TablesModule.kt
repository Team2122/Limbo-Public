package org.teamtators.vision.tables

import org.teamtators.vision.MainModule
import org.teamtators.vision.Module
import org.teamtators.vision.vision.VisionModule


class TablesModule(main: MainModule, vision: VisionModule) : Module {
    val networkTablesUpdater = NetworkTablesUpdater(main.config, vision.processResults)

    override fun start() {
        networkTablesUpdater.start()
    }

    override fun stop() {
        networkTablesUpdater.stop()
    }

}