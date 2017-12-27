package org.teamtators.agitator.dashboard

import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import java.net.URL
import java.util.*
import java.util.concurrent.Callable

class ConnectionLabel() : HBox(), Initializable {
    companion object {
        val FXML_FILE = "/fxml/ConnectionLabel.fxml"
        val FXML_RESOURCE = ConnectionLabel::class.java.getResource(FXML_FILE)
                ?: throw RuntimeException("Could not load $FXML_FILE")
        val FXML_LOADER = FXMLLoader(FXML_RESOURCE)
    }

    enum class State(val text: String, val style: String) {
        CONNECTED("Connected", "connected"), DISCONNECTED("Disconnected", "disconnected"), CONNECTING("Connecting...", "connecting"), UNKNOWN("???", "unknown");

        companion object {
            fun fromBool(value: Boolean): State {
                return if (value) CONNECTED else DISCONNECTED
            }
        }
    }

    private val titleProperty = SimpleStringProperty("")
    fun titleProperty() = titleProperty
    var title: String by PropertyDelegate(titleProperty)

    private val stateProperty = SimpleObjectProperty<State>(State.UNKNOWN)
    fun stateProperty() = stateProperty
    var state: State by PropertyDelegate(stateProperty)

    init {
        FXML_LOADER.setController(this)
        FXML_LOADER.setRoot(this)
        FXML_LOADER.load<HBox>()
    }

    @field:FXML
    private lateinit var label: Label
    @field:FXML
    private lateinit var colorBox: Pane

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        label.textProperty().bind(
                Bindings.createStringBinding(Callable { "$title ${state.text}" },
                        titleProperty, stateProperty))

        stateProperty.addListener { observable ->
            colorBox.styleClass[1] = state.style
        }
    }
}