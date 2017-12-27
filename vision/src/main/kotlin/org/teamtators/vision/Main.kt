package org.teamtators.vision

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.common.collect.Streams
import com.google.common.io.ByteStreams
import io.reactivex.subjects.CompletableSubject
import org.opencv.core.Core
import org.slf4j.bridge.SLF4JBridgeHandler
import org.teamtators.vision.config.Config
import org.teamtators.vision.config.ConfigManager
import org.teamtators.vision.config.OpenCVModule
import org.teamtators.vision.display.DisplayModule
import org.teamtators.vision.http.HttpModule
import org.teamtators.vision.mqtt.MqttModule
import org.teamtators.vision.tables.TablesModule
import org.teamtators.vision.util.NotNull
import org.teamtators.vision.vision.VisionModule
import java.io.File
import java.io.FileOutputStream
import java.util.stream.Stream

private val TATORVISION_HEADER = "\n" +
        "┌─────────────────────────────┐\n" +
        "│╺┳╸┏━┓╺┳╸┏━┓┏━┓╻ ╻╻┏━┓╻┏━┓┏┓╻│\n" +
        "│ ┃ ┣━┫ ┃ ┃ ┃┣┳┛┃┏┛┃┗━┓┃┃ ┃┃┗┫│\n" +
        "│ ╹ ╹ ╹ ╹ ┗━┛╹┗╸┗┛ ╹┗━┛╹┗━┛╹ ╹│\n" +
        "└─────────────────────────────┘\n"

fun onShutdown(task: () -> Unit) {
    Runtime.getRuntime()
            .addShutdownHook(Thread(task))
}

interface Module {
    fun start()
    fun stop()
}

class MainModule : Module {
    val yamlMapper: ObjectMapper = YAMLMapper()
            .registerModules(KotlinModule(), OpenCVModule())
    val jsonMapper: ObjectMapper = ObjectMapper(JsonFactory())
            .registerModules(KotlinModule(), OpenCVModule())
    val configManager = ConfigManager(yamlMapper)

    var config: Config by NotNull("MainModule must be started before getting config")

    override fun start() {
        loadOpenCv()

        config = configManager.loadConfig()
    }

    override fun stop() {
    }

    private fun loadOpenCv() {
        Main.logger.debug("Loading OpenCV Version: {}", Core.VERSION)

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
    }
}

class Main {
    companion object {
        val logger = loggerFor<Main>()
    }

    val onStop: CompletableSubject = CompletableSubject.create()

    fun startModule(module: Module) {
        module.start()
        onStop.subscribe({
            module.stop()
        })
    }

    fun start() {
        logger.info(TATORVISION_HEADER)


        val main = MainModule()
        startModule(main)

        val vision = VisionModule(main)
        startModule(vision)

        if (main.config.server.enabled)
            startModule(HttpModule(main, vision))

        if (main.config.tables.enabled)
            startModule(TablesModule(main, vision))

        if (main.config.mqtt.enabled)
            startModule(MqttModule(main, vision))

        if (main.config.display)
            startModule(DisplayModule(main, vision))
    }

    fun stop() {
        logger.info("Stopping...")
        onStop.onComplete()
    }
}

fun main(args: Array<String>) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    val main = Main()
    main.start()

    onShutdown {
        main.stop()
    }

    // waits for interrupt
    val lock = Object()
    synchronized(lock) {
        while (true) {
            lock.wait()
        }
    }
}

