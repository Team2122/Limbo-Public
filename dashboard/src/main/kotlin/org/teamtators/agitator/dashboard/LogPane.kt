package org.teamtators.agitator.dashboard

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.pattern.Abbreviator
import ch.qos.logback.classic.pattern.TargetLengthBasedClassNameAbbreviator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.sun.javafx.binding.ObjectConstant
import com.sun.javafx.binding.StringConstant
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.css.PseudoClass
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.input.DataFormat
import javafx.scene.layout.HBox
import javafx.scene.text.Text
import javafx.util.Callback
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.text.spi.DateFormatProvider
import java.util.*

val LOGGER_ABBREVIATOR: Abbreviator = TargetLengthBasedClassNameAbbreviator(30)

class LogPane : ScrollPane(), Initializable {
    companion object {
        val FXML_FILE = "/fxml/LogPane.fxml"
        val FXML_RESOURCE = DashboardController::class.java.getResource(FXML_FILE) ?:
                throw RuntimeException("Could not load $FXML_FILE")
        val FXML_LOADER = FXMLLoader(FXML_RESOURCE)

        val logger = loggerFor<LogPane>()
    }

    private val logItems: ObservableList<ILoggingEvent> = FXCollections.observableArrayList()
    @field:FXML
    private lateinit var logItemsView: TableView<ILoggingEvent>

    var autoscroll = true

    init {
        FXML_LOADER.setController(this)
        FXML_LOADER.setRoot(this)
        FXML_LOADER.load<LogPane>()
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        val levelClassInfo = PseudoClass.getPseudoClass("levelInfo")
        val levelClassWarn = PseudoClass.getPseudoClass("levelWarn")
        val levelClassError = PseudoClass.getPseudoClass("levelError")
        val levelClassTrace = PseudoClass.getPseudoClass("levelTrace")
        val levelClassDebug = PseudoClass.getPseudoClass("levelDebug")
        logItemsView.items = logItems
        logItemsView.rowFactory = Callback {
            val row = TableRow<ILoggingEvent>()
            row.itemProperty().addListener { observable, oldValue, newValue ->
                val value = newValue ?: oldValue
                if(value != null) {
                    row.pseudoClassStateChanged(levelClassInfo, value.level == Level.INFO)
                    row.pseudoClassStateChanged(levelClassWarn, value.level == Level.WARN)
                    row.pseudoClassStateChanged(levelClassError, value.level == Level.ERROR)
                    row.pseudoClassStateChanged(levelClassTrace, value.level == Level.TRACE)
                    row.pseudoClassStateChanged(levelClassDebug, value.level == Level.DEBUG)
                }
                else {
                    logger.info(":(")
                    println(value)
                    println(newValue)
                }
            }
            row
        }
        startAppender()
    }

    private fun startAppender() {
        val appender = Appender()
        appender.start()
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        if (root is ch.qos.logback.classic.Logger) {
            root.addAppender(appender)
        } else {
            logger.warn("Could not add appender, not using Logback")
        }
    }


    fun addLogEvent(item: ILoggingEvent) {
        logItems.add(item)
        if (autoscroll)
            vvalue = 1.0
    }

    inner class Appender : AppenderBase<ILoggingEvent>() {
        override fun append(event: ILoggingEvent) {
            Platform.runLater {
                addLogEvent(event)
            }
        }
    }

    fun clear() {
        logItems.clear()
    }
}

class TimestampValueFactory<S : ILoggingEvent> : Callback<TableColumn.CellDataFeatures<S, *>, ObservableValue<*>> {
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS")

    override fun call(col: TableColumn.CellDataFeatures<S, *>): ObservableValue<*> {
        return ObjectConstant.valueOf(dateFormat.format(Date(col.value.timeStamp)))
    }
}

class LoggerNameValueFactory<S : ILoggingEvent> : Callback<TableColumn.CellDataFeatures<S, *>, ObservableValue<*>> {
    override fun call(col: TableColumn.CellDataFeatures<S, *>): ObservableValue<*> {
        return ObjectConstant.valueOf(LOGGER_ABBREVIATOR.abbreviate(col.value.loggerName))
    }
}