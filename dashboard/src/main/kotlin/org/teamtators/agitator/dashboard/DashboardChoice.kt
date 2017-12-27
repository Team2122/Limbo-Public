package org.teamtators.agitator.dashboard

import io.reactivex.Observable
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import org.teamtators.vision.MqttChooser
import java.net.URL
import java.util.*

class DashboardChoice() : HBox(), Initializable {
    companion object {
        val FXML_FILE = "/fxml/DashboardChoice.fxml"
        val FXML_RESOURCE = DashboardChoice::class.java.getResource(FXML_FILE) ?:
                throw RuntimeException("Could not load resource $FXML_FILE")
        val FXML_LOADER = FXMLLoader(FXML_RESOURCE)
    }

    private val mqttChooserProperty = object : SimpleObjectProperty<MqttChooser>(this, "mqttChooser") {
        override fun invalidated() {
            val chooser = get()
            label.text = chooser?.name ?: ""
            setOptions(chooser?.options ?: arrayOf())
        }
    }

    fun mqttChooserProperty() = mqttChooserProperty
    var mqttChooser: MqttChooser? by PropertyDelegate(mqttChooserProperty)

    private val options = FXCollections.observableArrayList<String>()

    @field:FXML
    private lateinit var label: Label
    @field:FXML
    private lateinit var dropdown: ComboBox<String>

    init {
        FXML_LOADER.setRoot(this)
        FXML_LOADER.setController(this)
        FXML_LOADER.load<HBox>()
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        dropdown.items = options
        dropdown.valueProperty()
                .addListener { obs, old, new ->
                    updateChoice()
                }
    }

    private fun updateChoice() {
        mqttChooser?.updateChoice(dropdown.value ?: "")
    }

    private fun setOptions(options: Array<String>) {
        val selected = dropdown.value
        this.options.setAll(*options)
        if (this.options.contains(selected)) {
            value = selected
        } else {
            updateChoice()
        }
    }

    val observable: Observable<String>? get() = mqttChooser?.observable

    var value: String by PropertyDelegate(dropdown.valueProperty())
}