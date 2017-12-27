package org.teamtators.limbo.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Timer;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.util.JoystickModifiers;
import org.teamtators.common.util.Ramper;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.Drive;
import org.teamtators.limbo.subsystems.OperatorInterface;
import org.teamtators.limbo.util.DriveOutputRamper;

public class DriveFieldCentric extends Command implements Configurable<DriveFieldCentric.Config> {
    private OperatorInterface oi;
    private Drive drive;
    private Timer timer;
    private DriveOutputRamper ramper = new DriveOutputRamper();
    private Config config;

    public DriveFieldCentric(TatorRobot robot) {
        super("DriveFieldCentric");
        oi = robot.getSubsystems().getOperatorInterface();
        drive = robot.getSubsystems().getDrive();
        timer = new Timer();
        requires(drive);
    }

    @Override
    protected void initialize() {
        super.initialize();
        timer.start();
    }

    @Override
    protected boolean step() {
        Config.Settings settings = drive.getGear() == Drive.Gear.HIGH ? config.high : config.low;
        double delta = timer.restart();

        double currentAngle = drive.getYawAngle();

        double throttle = settings.throttle.apply(oi.getDriveThrottle());
        double turn = settings.turn.apply(oi.getDriveTurn());
        double angle = oi.getDriveRotationAngle();
        double angleMag = settings.angleMagnitude.apply(oi.getDriveRotationMagnitude());

        double angleDelta = getAngleDelta(currentAngle, angle);
        double angleOutput = angleDelta * settings.kAngleP;
        angleOutput = Math.max(Math.min(angleOutput, angleMag), -angleMag);

        logger.trace("currentAngle={}, angle={}, angleDelta={}, angleMag={}",
                currentAngle, angle, angleDelta, angleMag);

        Drive.Output output = new Drive.Output(
                throttle + turn + angleOutput, throttle - turn - angleOutput
        );
        output = ramper.update(delta, output);

        drive.drivePowers(output);
        return false;
    }

    private double getAngleDelta(double currentAngle, double angle) {
        double angleDelta = angle - currentAngle;
        while (angleDelta > 180) {
            angleDelta -= 360;
        }
        while (angleDelta < -180) {
            angleDelta += 360;
        }
        return angleDelta;
    }

    @Override
    public void configure(Config config) {
        ramper.configure(config.ramper);
        this.config = config;
    }

    @Override
    protected void finish(boolean interrupted) {
        drive.stop();
        super.finish(interrupted);
    }

    public static class Config {
        public Settings high, low;
        public Ramper.Config ramper;

        public class Settings {
            public JoystickModifiers throttle, turn, angleMagnitude;
            public double kAngleP;
        }
    }
}
