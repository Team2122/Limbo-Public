package org.teamtators.limbo.commands;

import edu.wpi.first.wpilibj.DriverStation;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.limbo.TatorRobot;

public class SelectColorCommand extends Command implements Configurable<SelectColorCommand.Config> {
    private final ConfigCommandStore commandStore;
    private final DriverStation driverStation;
    private Command redCommand;
    private Command blueCommand;
    private Command command;
    private boolean hasStarted;

    public SelectColorCommand(TatorRobot robot) {
        super("SelectColorCommand");
        this.commandStore = robot.getCommandStore();
        this.driverStation = robot.getDriverStation();
    }

    @Override
    protected void initialize() {
        hasStarted = false;
        switch (driverStation.getAlliance()) {
            case Red:
                command = redCommand;
                break;
            case Blue:
                command = blueCommand;
                break;
            default:
                logger.warn("Got invalid driver station alliance. Running red");
                command = redCommand;
                break;
        }
        logger.info("Starting chosen command: {}", command.getName());
        startWithContext(command, this);
    }

    @Override
    protected boolean step() {
        boolean running = command == null || command.isRunning();
        if (running)
            hasStarted = true;
        return command == null || (!running && hasStarted);
    }

    @Override
    protected void finish(boolean interrupted) {
        super.finish(interrupted);
        if (interrupted && command != null) {
            logger.info("Select color command cancelled, cancelling {}", command.getName());
            command.cancel();
        }
    }

    @Override
    public void configure(Config config) {
        this.redCommand = commandStore.getCommand(config.red);
        this.blueCommand = commandStore.getCommand(config.blue);
    }

    public static class Config {
        public String red;
        public String blue;
    }
}
