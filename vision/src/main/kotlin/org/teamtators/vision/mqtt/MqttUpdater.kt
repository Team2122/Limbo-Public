package org.teamtators.vision.mqtt

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.reactivestreams.Subscriber
import org.teamtators.vision.MqttTopics
import org.teamtators.vision.RobotData
import org.teamtators.vision.VisionData
import org.teamtators.vision.VisionMode
import org.teamtators.vision.config.Config
import org.teamtators.vision.loggerFor
import org.teamtators.vision.vision.ProcessResult
import java.util.concurrent.TimeUnit

/**
 * @author Alex Mikhalev
 */
class MqttUpdater(
        val _config: Config,
        val jsonMapper: ObjectMapper,
        val processResults: Flowable<ProcessResult>,
        val robotDataSubscriber: Subscriber<RobotData>
) {
    companion object {
        val logger = loggerFor<MqttUpdater>()
        val CONNECT_RETRY_TIME_MS: Long = 5000
    }

    val config = _config.mqtt

    val client: MqttAsyncClient = MqttAsyncClient(config.serverUri, config.clientId, MemoryPersistence())

    init {
        client.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                logger.info("Disconnected from mqtt broker: ${cause?.message}")
            }

            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                logger.info(if (reconnect) {
                    "Reconnected"
                } else {
                    "Connected"
                } + " to broker at $serverURI")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
            }
        })
    }

    private var resultsSubscription: Disposable? = null

    fun start() {
        val connectOpts = MqttConnectOptions()
        val bytes = jsonMapper.writeValueAsBytes(false)
        connectOpts.setWill(MqttTopics.VISION_CONNECTED, bytes, 1, true) // at least once, and retain
        connectOpts.isAutomaticReconnect = true

        connectMqtt(connectOpts)
                .retryWhen { errors ->
                    errors.flatMap { e ->
                        logger.warn("Could not connect to MQTT broker: ${e.message}. " +
                                "Waiting for $CONNECT_RETRY_TIME_MS ms to retry")
                        Observable.timer(CONNECT_RETRY_TIME_MS, TimeUnit.MILLISECONDS)
                    }
                }
                .subscribe({ _ ->
                    publishConnected(true).waitForCompletion()

                    client.subscribe(MqttTopics.ROBOT_DATA, 1, { _, message -> onRobotData(message) })
                            .waitForCompletion()
                    resultsSubscription = processResults.subscribe({ publishProcessResult(it) })
                }, { t ->
                    throw RuntimeException("Error starting MqttUpdater", t)
                })

    }

    private fun connectMqtt(connectOpts: MqttConnectOptions): Observable<Boolean> {
        return Observable.create<Boolean> { emit ->
            logger.debug("Connecting to mqtt broker at ${client.serverURI}")
            client.connect(connectOpts, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    emit.onNext(true)
                    emit.onComplete()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, e: Throwable) {
                    emit.onError(e)
                }

            })
        }
                .observeOn(Schedulers.io())
    }

    fun stop() {
        if (client.isConnected) {
            logger.info("Disconnecting from mqtt broker")
            publishConnected(false).waitForCompletion()
            client.disconnect()
        }
        resultsSubscription?.dispose()
    }

    private fun onRobotData(message: MqttMessage) {
        try {
            val robotData = jsonMapper.readValue(message.payload, RobotData::class.java)
            _config.vision.visionMode = robotData.visionMode ?: VisionMode.FUEL;
            robotDataSubscriber.onNext(robotData)
        } catch (t: Throwable) {
            robotDataSubscriber.onError(Error("Error receiving RobotData", t))
        }
    }

    private fun publishProcessResult(result: ProcessResult): IMqttToken? {
        if (client.isConnected) {
            try {
                val bytes = jsonMapper.writeValueAsBytes(result.visionData)
                return client.publish(MqttTopics.VISION_DATA, bytes, 1, false)
            } catch (e: Exception) {
                logger.error("Error publishing vision data", e)
            }
        }
        return null
    }

    private fun publishConnected(connected: Boolean): IMqttToken {
        val bytes = jsonMapper.writeValueAsBytes(connected)
        return client.publish(MqttTopics.VISION_CONNECTED, bytes, 1, true)
    }
}

