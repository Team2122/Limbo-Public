package org.teamtators.limbo.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.Drive;
import org.teamtators.limbo.subsystems.Shooter;
import org.teamtators.limbo.subsystems.Vision;
import org.teamtators.vision.VisionData;
import org.teamtators.vision.VisionMode;

public class RotateToBoiler extends DriveRotateBase implements Configurable<RotateToBoiler.Config> {
    private final Vision vision;
    private final Shooter shooter;
    private final Timer visionTimer = new Timer();
    private Config config;
    private boolean driving;

    public RotateToBoiler(TatorRobot robot) {
        super("RotateToBoiler", robot);
        this.vision = robot.getSubsystems().getVision();
        this.shooter = robot.getSubsystems().getShooter();
    }

    @Override
    protected void initialize() {
        vision.setVisionMode(VisionMode.FUEL);
        logger.info("Turning on vision and waiting for values.");
        visionTimer.reset();
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        if (interrupted) {
            logger.info("Interrupted");
        }
        vision.setVisionMode(VisionMode.NONE);
        driving = false;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
        super.configure(config);
        predicate = follower -> follower.isFinished() &&
                Math.abs(follower.getPositionError()) <= config.angleTolerance;
    }

    public static class Config extends DriveRotateBase.Config {
        public double maxOffset;
        public double visionDelay;
        public double angleTolerance;
        public boolean setShooterDistance;
    }

    @Override
    public boolean step() {
        VisionData visionData = vision.getVisionData();
        Double offset = vision.getYawOffset(visionData);
        Double angle = vision.getNewRobotAngle(visionData);
        Double distance = vision.getFuelDistance(visionData);
        if (distance != null && config.setShooterDistance) {
            shooter.updateCurrentDistance(distance);
        }
        if (!driving) {
            if (angle == null || (visionTimer.isRunning() && !visionTimer.hasPeriodElapsed(config.visionDelay))) {
                return false;
            }
            logger.info("RotateToBoiler rotating to angle {} (offset {})", angle, offset);
            this.angle = angle;
            super.initialize();
            driving = true;
        }
        boolean doneDriving = super.step();
        if (doneDriving) {
            if (offset != null && Math.abs(offset) <= config.maxOffset) {
                Double y = visionData.y;
                logger.info("Finished rotating at offset of {} (max {}), angle={}, y={}, dist={}",
                        offset, config.maxOffset, drive.getYawAngle(), y, distance);
                return true;
            } else {
                driving = false;
                visionTimer.start();
            }
        }
        return false;
    }
}
