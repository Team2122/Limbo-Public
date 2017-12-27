package org.teamtators.limbo.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.Climber;

public class ClimberForce extends Command implements Configurable<ClimberForce.Config> {
    private Climber climber;
    private Config config;

    public ClimberForce(TatorRobot robot) {
        super("ClimberForce");
        climber = robot.getSubsystems().getClimber();
        requires(climber);
    }

    @Override
    protected void initialize() {
        logger.info("Started ClimberForce with power {}", config.power);
    }

    @Override
    protected boolean step() {
        climber.setPower(config.power);
        return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        climber.setPower(0.0);
    }

    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double power;
    }

}
