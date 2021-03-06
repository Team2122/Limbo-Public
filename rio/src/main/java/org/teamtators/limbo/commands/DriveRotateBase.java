package org.teamtators.limbo.commands;

import org.teamtators.common.control.ControllerPredicates;
import org.teamtators.common.control.TrapezoidalProfile;
import org.teamtators.common.control.TrapezoidalProfileFollower;
import org.teamtators.common.scheduler.Command;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.Drive;

import java.util.function.Predicate;

public abstract class DriveRotateBase extends Command {
    protected final Drive drive;

    private Config config;
    protected double angle;
    protected Predicate<TrapezoidalProfileFollower> predicate = ControllerPredicates.finished();

    protected DriveRotateBase(String name, TatorRobot robot) {
        super(name);
        this.drive = robot.getSubsystems().getDrive();
        requires(drive);
    }

    @Override
    protected void initialize() {
        double initialAngle = drive.getYawAngle();
        double delta = angle - initialAngle;
        TrapezoidalProfile profile = new TrapezoidalProfile(delta, 0,
                Math.copySign(config.rotationSpeed, delta), 0.0, config.maxAcceleration);
        drive.driveRotationProfile(profile, predicate);
    }

    protected boolean step() {
        return drive.isRotationProfileOnTarget();
    }

    @Override
    protected void finish(boolean interrupted) {
        drive.stop();
    }

    protected void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double rotationSpeed;
        public double maxAcceleration;
    }
}
