package org.teamtators.agitator.dashboard

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Screen
import javafx.stage.Stage

class Dashboard : Application() {
    val logger = loggerFor<Dashboard>()
    val DS_HEIGHT = 240
    val SCENE_RESOURCE = "/fxml/Dashboard.fxml"

    override fun start(primaryStage: Stage) {
        primaryStage.title = "TatorDashboard"
        primaryStage.onCloseRequest = EventHandler {
            Platform.exit()
            System.exit(0)
        }
        primaryStage.x = 0.0
        primaryStage.y = 0.0
        val screenBounds = Screen.getPrimary().bounds
        primaryStage.width = screenBounds.width
        primaryStage.height = screenBounds.height - DS_HEIGHT

        val sceneUrl = Dashboard::class.java.getResource(SCENE_RESOURCE) ?:
                throw RuntimeException("Could not load scene from $SCENE_RESOURCE")
        val scene = FXMLLoader.load<Scene>(sceneUrl)
        primaryStage.scene = scene

        primaryStage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Dashboard::class.java, *args)
        }
    }
}