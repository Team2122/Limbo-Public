package org.teamtators.limbo.subsystems;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.config.ConfigException;
import org.teamtators.common.config.ConfigLoader;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.limbo.TatorRobot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Subsystems extends SubsystemsBase {
    private static final String SUBSYSTEMS_CONFIG_FILE = "Subsystems.yaml";
    private List<Subsystem> subsystemList;
    private Climber climber;
    private Gear gear;
    private Drive drive;
    private Shooter shooter;
    private OperatorInterface operatorInterface;
    private Vision vision;
    private MqttUpdater mqttUpdater;

    private List<Updatable> controllers = new ArrayList<>();

    public Subsystems(TatorRobotBase robot) {
        TatorRobot tatorRobot = (TatorRobot) robot;
        climber = new Climber(tatorRobot);
        gear = new Gear(tatorRobot);
        drive = new Drive(tatorRobot);
        shooter = new Shooter(tatorRobot);
        operatorInterface = new OperatorInterface();
        vision = new Vision(drive, shooter, tatorRobot);
        mqttUpdater = new MqttUpdater(vision);
        subsystemList = Arrays.asList(climber, gear, drive, shooter, operatorInterface, vision, mqttUpdater);
    }

    public Gear getGear() {
        return gear;
    }

    public Shooter getShooter() {
        return shooter;
    }

    public OperatorInterface getOperatorInterface() {
        return operatorInterface;
    }

    public Drive getDrive() {
        return drive;
    }

    public Climber getClimber() {
        return climber;
    }

    public Vision getVision() {
        return vision;
    }

    public MqttUpdater getMqttUpdater() {
        return mqttUpdater;
    }

    @Override
    public List<Subsystem> getSubsystemList() {
        return subsystemList;
    }

    @Override
    public void configure(ConfigLoader configLoader) {
        try {
            ObjectNode configNode = (ObjectNode) configLoader.load(SUBSYSTEMS_CONFIG_FILE);
            Config configObj = configLoader.getObjectMapper().treeToValue(configNode, Config.class);
            configure(configObj);
        } catch (Throwable e) {
            throw new ConfigException("Error configuring subsystems: ", e);
        }
    }

    public void configure(Config config) {
        TatorRobotBase.logger.trace("Configuring Subsystems");
        drive.configure(config.drive);
        climber.configure(config.climber);
        shooter.configure(config.shooter);
        gear.configure(config.gear);
        operatorInterface.configure(config.operatorInterface);
        vision.configure(config.vision);
        mqttUpdater.configure(config.mqttUpdater);

        controllers.clear();
        controllers.addAll(drive.getUpdatables());
        controllers.add(gear.getGearPivotController());
    }

    @Override
    public List<Updatable> getControllers() {
        return controllers;
    }

    @Override
    public MqttAsyncClient getMqttClient() {
        return mqttUpdater.getClient();
    }

    public static class Config {
        public Climber.Config climber;
        public Drive.Config drive;
        public Shooter.Config shooter;
        public Gear.Config gear;
        public OperatorInterface.Config operatorInterface;
        public Vision.Config vision;
        public MqttUpdater.Config mqttUpdater;
    }
}