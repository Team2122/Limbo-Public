package org.teamtators.limbo.subsystems;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.vision.MqttChooser;
import org.teamtators.vision.MqttTopics;
import org.teamtators.vision.RobotData;
import org.teamtators.vision.VisionData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MqttUpdater extends Subsystem implements Configurable<MqttUpdater.Config> {
    public static final Logger logger = LoggerFactory.getLogger(MqttUpdater.class);
    public static final String CLIENT_ID = "Limbo-Vision";
    private static final long CONNECT_RETRY_MS = 5000;
    private final Vision vision;
    private final MqttConnectOptions mqttOptions;
    private ObjectMapper jsonMapper = new ObjectMapper(new JsonFactory());
    private MqttAsyncClient mqttClient;
    private List<MqttChooser> choosers = new ArrayList<>();
    private Subject<RobotData> robotDataSubject = BehaviorSubject.create();

    public MqttUpdater(Vision vision) {
        super("MqttUpdater");
        this.vision = vision;

        mqttOptions = new MqttConnectOptions();
        try {
            byte[] willPayload = jsonMapper.writeValueAsBytes(false);
            mqttOptions.setWill(MqttTopics.ROBOT_CONNECTED, willPayload, 1, true);
        } catch (JsonProcessingException e) {
            logger.error("cats are falling from the sky");
        }
        mqttOptions.setAutomaticReconnect(true);
    }

    public void connect() {
        logger.debug("Attempting to connect to mqtt broker at " + mqttClient.getServerURI());
        Observable.create(emit -> {
            mqttClient.connect(mqttOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    emit.onNext(true);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    emit.onError(exception);
                }
            });
        })
                .retryWhen(errors -> errors.flatMap(error -> {
                    logger.warn("Error connecting to mqtt broker. Retrying in {} ms", CONNECT_RETRY_MS);
                    return Observable.timer(CONNECT_RETRY_MS, TimeUnit.MILLISECONDS);
                }))
                .subscribe();

        robotDataSubject
                .throttleLast(100, TimeUnit.MILLISECONDS)
                .subscribe(robotData -> {
                    try {
                        if (mqttClient.isConnected()) {
                            byte[] data = jsonMapper.writeValueAsBytes(robotData);
                            mqttClient.publish(MqttTopics.ROBOT_DATA, data, 1, false);
                        }
                    } catch (MqttException | JsonProcessingException e) {
                        logger.error("Error publishing RobotData", e);
                    }
                });
    }

    protected void publishConnected(boolean connected) {
        try {
            byte[] payload = jsonMapper.writeValueAsBytes(connected);
            mqttClient.publish(MqttTopics.ROBOT_CONNECTED, payload, 1, true);
        } catch (Exception e) {
            logger.error("Error publishing connection status", e);
        }
    }

    private void onDashboardChoices(MqttMessage message) {
        String[] choices;
        try {
            choices = jsonMapper.readValue(message.getPayload(), String[].class);
        } catch (Exception e) {
            logger.error("Error reading mqtt chooser choices", e);
            return;
        }
        for (int i = 0; i < choices.length; i++) {
            choosers.get(i).updateChoice(choices[i]);
        }
    }

    @Override
    public void configure(Config config) {
        choosers.clear();
        try {
            mqttClient = new MqttAsyncClient(config.serverURI, CLIENT_ID, new MemoryPersistence());
            mqttClient.setCallback(new MqttCallback());
        } catch (MqttException e) {
            throw new RuntimeException("Error creating MqttAsyncClient", e);
        }
    }

    public Observable<String> createChooser(String name, String[] options) {
        MqttChooser chooser = new MqttChooser(name, options);
        choosers.add(chooser);
        updateOptions();
        return chooser.getObservable();
    }

    private void updateOptions() {
        List<MqttChooser> packet = choosers;
        try {
            if (mqttClient.isConnected()) {
                byte[] data = jsonMapper.writeValueAsBytes(packet);
                mqttClient.publish(MqttTopics.DASHBOARD_CHOOSERS, data, 1, true);
            }
        } catch (JsonProcessingException | MqttException e) {
            logger.error("Error publishing choosers", e);
        }
    }

    protected void onVisionData(MqttMessage message) {
        VisionData visionData;
        try {
            visionData = jsonMapper.readValue(message.getPayload(), VisionData.class);
        } catch (Exception e) {
            logger.error("Error reading vision data from mqtt", e);
            return;
        }
        vision.updateVisionData(visionData);
    }

    @Override
    public void update(double delta) {
        robotDataSubject.onNext(vision.getRobotData());
    }

    public MqttAsyncClient getClient() {
        return mqttClient;
    }

    public static class Config {
        public String serverURI;
    }

    private class MqttCallback implements MqttCallbackExtended {
        @Override
        public void connectionLost(Throwable cause) {
            logger.warn("Lost connection to mqtt broker", cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            try {
                publishConnected(true);

                mqttClient.subscribe(MqttTopics.VISION_DATA, 1, (topic, message) ->
                        onVisionData(message)
                );

                updateOptions();
                mqttClient.subscribe(MqttTopics.DASHBOARD_CHOICES, 1, (topic, message) -> onDashboardChoices(message));
                logger.info("Connected to mqtt broker at " + serverURI);
            } catch (Throwable e) {
                logger.error("Error during connection to broker", e);
            }
        }
    }

}
