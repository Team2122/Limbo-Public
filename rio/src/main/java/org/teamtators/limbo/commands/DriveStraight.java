package org.teamtators.limbo.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.TrapezoidalProfile;
import org.teamtators.common.scheduler.Command;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.Drive;

public class DriveStraight extends Command implements Configurable<DriveStraight.Config> {
    private Drive drive;
    private Config config;
    private TrapezoidalProfile profile;

    public DriveStraight(TatorRobot robot) {
        super("DriveStraight");
        drive = robot.getSubsystems().getDrive();
        requires(drive);
    }

    @Override
    protected void initialize() {
        String angleStr;
        double angle;
        if (Double.isNaN(config.angle)) {
            angle = drive.getYawAngle();
            angleStr = "current angle (" + angle + ")";
        } else {
            angle = config.angle;
            angleStr = "angle " + angle + " (currently at " + drive.getYawAngle() + ")";
        }
        logger.info("Driving at {} for distance of {} at top speed of {}",
                angleStr, config.distance, config.speed);
        drive.driveStraightProfile(angle, profile);
    }

    @Override
    protected boolean step() {
        return drive.isStraightProfileOnTarget();
    }

    @Override
    protected void finish(boolean interrupted) {
        drive.stop();
        double distance = drive.getStraightMotionFollower().getCurrentPosition();
        double angle = drive.getYawAngle();
        String logString = String.format(" at distance %s (target %s), angle %s (target %s)",
                distance, config.distance, angle, drive.getYawAngleController().getSetpoint());
        if (interrupted) {
            logger.warn("Interrupted" + logString);
        } else {
            logger.info("Finishing" + logString);
        }
    }

    public void configure(Config config) {
        this.config = config;
        profile = new TrapezoidalProfile(config.distance, 0, Math.copySign(config.speed, config.distance),
                Math.copySign(config.endSpeed, config.distance), config.maxAcceleration);
    }

    public static class Config {
        public double angle = Double.NaN;
        public double speed;
        public double endSpeed;
        public double maxAcceleration;
        public double distance;
    }
}

