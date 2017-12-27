package org.teamtators.limbo.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.util.Ramper;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.Climber;

public class ClimberClimb extends Command implements Configurable<ClimberClimb.Config> {
    private final Climber climber;
    private final Ramper ramper = new Ramper();
    private final LogitechF310 driverJoystick;
    private final LogitechF310 gunnerJoystick;
    private final Timer timer = new Timer();
    private Config config;

    public ClimberClimb(TatorRobot robot) {
        super("ClimberClimb");
        climber = robot.getSubsystems().getClimber();
        requires(climber);
        driverJoystick = robot.getSubsystems().getOperatorInterface().getDriverJoystick();
        gunnerJoystick = robot.getSubsystems().getOperatorInterface().getGunnerJoystick();
    }

    @Override
    protected boolean step() {
        double delta = timer.restart();
        double current = climber.getCurrent();
        if (current >= config.maxCurrent) {
            logger.info("Climber's current {}, is greater than max {}", current, config.maxCurrent);
            climber.setPower(0.0);
            return true;
        } else {
            //logger.info("Climber's current is {}, which is below the max {}", current, config.maxCurrent);
            double joystickInput = Math.max(driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_TRIGGER),
                    gunnerJoystick.getAxisValue(LogitechF310.Axis.RIGHT_TRIGGER));
            double power =  joystickInput * config.maxPower;
            ramper.setValue(power);
            ramper.update(delta);
            climber.setPower(ramper.getOutput());
            return false;
        }
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        climber.setPower(0.0);
    }

    public void configure(Config config) {
        this.config = config;
        ramper.configure(config.ramper);
    }

    public static class Config {
        public double maxPower;
        public double maxCurrent;
        public Ramper.Config ramper;
    }
}
