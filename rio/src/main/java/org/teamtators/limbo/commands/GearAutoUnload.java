package org.teamtators.limbo.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.Drive;
import org.teamtators.limbo.subsystems.Gear;
import org.teamtators.limbo.subsystems.Vision;
import org.teamtators.vision.VisionData;
import org.teamtators.vision.VisionMode;

public class GearAutoUnload extends Command implements Configurable<GearAutoUnload.Config> {
    private final Vision vision;
    private final Drive drive;
    private final Gear gear;
    private Config config;

    private Double gearDistance;
    private double startDistance;
    private double distanceFromGear;

    public GearAutoUnload(TatorRobot robot) {
        super("GearAutoUnload");
        this.drive = robot.getSubsystems().getDrive();
        this.vision = robot.getSubsystems().getVision();
        this.gear = robot.getSubsystems().getGear();
        requires(drive);
        requires(vision);
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    @Override
    protected void initialize() {
        logger.info("Driving towards gear peg");
        vision.setVisionMode(VisionMode.GEAR);
        gearDistance = null;
        startDistance = drive.getAverageDistance();
    }

    @Override
    protected boolean step() {
        VisionData visionData = vision.getVisionData();
        Double angle = vision.getNewRobotAngle(visionData);
        Double distance = vision.getGearDistance(visionData);
        if (angle != null && distance != null && (gearDistance == null || distanceFromGear >= config.minGearVision)) {
            gearDistance = distance + visionData.robotData.driveDistance;
        }
        if (angle != null && gearDistance != null) {
            distanceFromGear = gearDistance - drive.getAverageDistance();
            double velocity = (distanceFromGear <= config.closeDistance) ? config.closeVelocity : config.velocity;
            drive.driveHeading(angle, velocity);
            logger.trace("at distance from gear {} (target {})", distanceFromGear, config.gearDistance);
            return distanceFromGear <= config.gearDistance || gear.isPegIn();
        }
        return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        double totalDistance = drive.getAverageDistance() - startDistance;
        logger.info((interrupted ? "Interrupted" : "Finished") +
                " at distance from gear {} (target {}), total distance {}",
                distanceFromGear, config.gearDistance, totalDistance);
        vision.setVisionMode(VisionMode.NONE);
        if (interrupted) {
            gear.requestHome();
        } else {
            gear.requestReleased();
        }
        drive.stop();
    }

    public static class Config {
        public double velocity;
        public double closeDistance;
        public double closeVelocity;
        public double gearDistance;
        public Double minGearVision;
    }
}
