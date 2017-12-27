package org.teamtators.limbo.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.Shooter;

public class ShooterSetDesiredSpeed extends Command implements Configurable<ShooterSetDesiredSpeed.Config> {
    private final Shooter shooter;
    private Config config;

    public ShooterSetDesiredSpeed(TatorRobot robot) {
        super("Shooter.SetDesiredSpeed");
        this.shooter = robot.getSubsystems().getShooter();
    }

    @Override
    protected boolean step() {
        logger.info("Setting desired shooter speed to {} rpm", config.speed);
        shooter.setDesiredSpeed(config.speed);
        return true;
    }

    @Override
    public void configure(Config config) {
        this.config = config;
    }

    public static class Config {
        public double speed;
    }
}
