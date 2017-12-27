package org.teamtators.agitator.dashboard

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.web.WebView
//import org.controlsfx.glyphfont.FontAwesome
//import org.controlsfx.glyphfont.Glyph
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.teamtators.vision.MqttChooser
import org.teamtators.vision.MqttTopics
import java.net.URL
import java.util.*
import kotlin.collections.HashMap

class DashboardController() : BorderPane(), Initializable {
    companion object {
        val DASHBOARD_RESOURCE = "/fxml/DashboardControl.fxml"
        val FXML_RESOURCE = DashboardController::class.java.getResource(DASHBOARD_RESOURCE) ?:
                throw RuntimeException("Could not load $DASHBOARD_RESOURCE")
        val FXML_LOADER = FXMLLoader(FXML_RESOURCE)

        val logger = loggerFor<DashboardController>()

        val clientId: String
            get() = "TatorDashboard-" + (Math.random() * Short.MAX_VALUE).toShort()
    }

    val connectOptions = MqttConnectOptions()
    val jsonMapper = ObjectMapper(JsonFactory())
    val client = MqttAsyncClient(Preferences.serverUri, clientId, MemoryPersistence())

    @field:FXML
    private lateinit var chooserArea: FlowPane
    private val choices = ArrayList<String>()
    @field:FXML
    private lateinit var logPane: LogPane

    @field:FXML
    private lateinit var connectionLabelBroker: ConnectionLabel
    @field:FXML
    private lateinit var connectionLabelVision: ConnectionLabel
    @field:FXML
    private lateinit var connectionLabelRobot: ConnectionLabel

    @field:FXML
    private lateinit var visionStream: WebView

    @field:FXML
    private lateinit var connectButton: Button

    private var valueMap : HashMap<String, String> = HashMap()

    @field:FXML
    private lateinit var dataTable : TableView<Any?>

    @field:FXML
    private lateinit var filterText : TextField

    var connectionState: ConnectionLabel.State = ConnectionLabel.State.DISCONNECTED
        set(value) {
            field = value
            Platform.runLater {
                connectionLabelBroker.state = value
                connectButton.text = if (value == ConnectionLabel.State.DISCONNECTED) "Connect" else "Disconnect"
//                connectButton.graphic = Glyph("FontAwesome", FontAwesome.Glyph.PLUG)

                if (value == ConnectionLabel.State.DISCONNECTED) {
                    connectionLabelVision.state = ConnectionLabel.State.UNKNOWN
                    connectionLabelRobot.state = ConnectionLabel.State.UNKNOWN
                } else if (value == ConnectionLabel.State.CONNECTED) {
                    connectionLabelVision.state = ConnectionLabel.State.DISCONNECTED
                    connectionLabelRobot.state = ConnectionLabel.State.DISCONNECTED
                }
            }
        }

    init {
        connectOptions.isAutomaticReconnect = true
        client.setCallback(MqttCallback())

        FXML_LOADER.setRoot(this)
        FXML_LOADER.setController(this)
        FXML_LOADER.load<BorderPane>()
    }

    override fun initialize(location: URL, resources: ResourceBundle?) {
        visionStream.engine.onError = EventHandler {
            logger.error("Vision stream error", it)
            visionStream.engine.reload()
        }
        visionStream.engine.load(DashboardController::class.java.getResource(Preferences.streamUri).toString())

        connect()

        /*Observable.interval(1, TimeUnit.SECONDS)
                .subscribe {
                    logger.error("ERROR TEST")
                    logger.warn("WARN TEST")
                    logger.info("INFO TEST "+Math.random())
                    logger.debug("DEBUG TEST")
                    logger.trace("TRACE TEST")
                }*/
    }

    private fun sendChoices() {
        client.publish(MqttTopics.DASHBOARD_CHOICES, jsonMapper.writeValueAsBytes(choices), 1, true)
    }

    private fun updateDashboardOptions(message: MqttMessage) {
        val choosers: Array<MqttChooser>
        try {
            choosers = jsonMapper.readValue(message.payload, Array<MqttChooser>::class.java);
        } catch(e: Exception) {
            logger.error("Error reading dashboard options", e)
            return
        }

        val choiceCache: Array<String> = choices.toArray(arrayOf())

        chooserArea.children.clear()
        choices.clear()

        for (chooser in choosers.withIndex()) {
            val desiredValue = if (choiceCache.size > chooser.index) {
                choiceCache[chooser.index]
            } else {
                null
            }
            val value = if (desiredValue.let { chooser.value.options.contains(it) }) {
                desiredValue
            } else {
                chooser.value.options[0]
            } ?: ""

            choices.add(value)

            val dropdown = DashboardChoice()
            dropdown.mqttChooser = chooser.value
            chooserArea.children.add(dropdown)
            dropdown.value = value

            dropdown.observable?.subscribe {
                choices[chooser.index] = it
                sendChoices()
            }
        }
    }

    private fun updateDashboardData(message: MqttMessage) {
        val data: Map<*, *>
        try {
            data = jsonMapper.readValue(message.payload, Map::class.java);
        } catch(e: Exception) {
            logger.error("Error reading dashboard options", e)
            return
        }
        dataTable.items.clear()
        for(dataElement: Map.Entry<*, *> in data) {
            if((dataElement.key as String).contains(filterText.characters))
            dataTable.items.add(DashboardRow(dataElement.key as String, dataElement.value as String))
        }
    }

    private inner class MqttCallback : MqttCallbackExtended {
        override fun connectComplete(reconnect: Boolean, serverURI: String?) {
            logger.info("Dashboard connected to $serverURI")
            Platform.runLater {
                connectionState = ConnectionLabel.State.CONNECTED
            }

            subscribeLabel(connectionLabelVision, MqttTopics.VISION_CONNECTED)
            subscribeLabel(connectionLabelRobot, MqttTopics.ROBOT_CONNECTED)

            client.subscribe(MqttTopics.DASHBOARD_CHOOSERS, 1) { s: String, message: MqttMessage ->
                Platform.runLater {
                    updateDashboardOptions(message);
                }
            }

            client.subscribe(MqttTopics.ROBOT_DASHBOARD_DATA, 1) { s: String, message: MqttMessage ->
                Platform.runLater {
                    updateDashboardData(message)
                }
            }
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {

        }

        override fun connectionLost(cause: Throwable?) {
            logger.warn("Dashboard connection lost", cause)
            Platform.runLater {
                connectionState = ConnectionLabel.State.DISCONNECTED
            }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {

        }
    }

    private fun subscribeLabel(label: ConnectionLabel, topic: String) {
        client.subscribe(topic, 1) { s: String, message: MqttMessage ->
            logger.trace("{} updated", topic)
            Platform.runLater {
                label.state = ConnectionLabel.State.fromBool(jsonMapper.readValue(message.payload, Boolean::class.java))
            }
        }
    }

    fun connect() {
        try {
            client.connect(connectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // should be handled already by the other callback
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    logger.info("Failed to connect")
                    logger.error(exception?.cause.toString())
                    connectionState = ConnectionLabel.State.DISCONNECTED
                }

            })
            connectionState = ConnectionLabel.State.CONNECTING
        } catch (e: Exception) {
            logger.error("Error connecting to broker", e)
        }
    }

    @FXML
    fun toggleConnect() {
        if (connectionState == ConnectionLabel.State.DISCONNECTED) {
            connect()
        } else {
            disconnect()
        }

    }

    @FXML
    fun clearLogs() {
        logPane.clear()
    }

    fun disconnect() {
        client.disconnect(null, object : IMqttActionListener {
            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                logger.error("Failed to disconnect", exception)
            }

            override fun onSuccess(asyncActionToken: IMqttToken?) {
                logger.info("Disconnected")
                connectionState = ConnectionLabel.State.DISCONNECTED
            }
        })
    }
}
