package org.teamtators.limbo.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.Drive;

public class DriveShift extends Command implements Configurable<DriveShift.Config> {
    private Drive drive;
    private Drive.Gear gear;

    public DriveShift(TatorRobot robot) {
        super("DriveShift");
        drive = robot.getSubsystems().getDrive();
    }

    @Override
    protected void initialize() {
        logger.info("Drive shifting to {}", gear);
        drive.setGear(gear);
    }

    @Override
    protected void finish(boolean interrupted) {
    }

    @Override
    protected boolean step() {
        return true;
    }

    public void configure(Config config) {
        this.gear = config.gear;
    }

    public static class Config {
        public Drive.Gear gear;
    }
}
