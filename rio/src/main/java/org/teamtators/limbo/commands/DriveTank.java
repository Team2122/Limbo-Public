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

public class DriveTank extends Command implements Configurable<DriveTank.Config> {
    private OperatorInterface oi;
    private Drive drive;
    private Timer timer;
    private DriveOutputRamper ramper = new DriveOutputRamper();
    private Config config;

    public DriveTank(TatorRobot robot) {
        super("DriveTank");
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
        Drive.Output output = new Drive.Output(
                settings.apply(oi.getDriveLeft()),
                settings.apply(oi.getDriveRight())
        );
        output = ramper.update(delta, output);

        drive.drivePowers(output);
        return false;
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

        public class Settings extends JoystickModifiers {
//            public JoystickModifiers left, right;
        }
    }
}
