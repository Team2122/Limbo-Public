package org.teamtators.limbo.commands;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.CommandStore;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.MqttUpdater;

public class ChooserCommand extends Command implements Configurable<ChooserCommand.Config> {

    private MqttUpdater mqttUpdater;
    private CommandStore commandStore;

    private String choice;
    private Command command;
    private boolean hasStarted = false;

    public ChooserCommand(TatorRobot robot) {
        super("ChooserCommand");
        mqttUpdater = robot.getSubsystems().getMqttUpdater();
        commandStore = robot.getCommandStore();
    }

    public Command getCommand() {
        return commandStore.getCommand(choice);
    }

    @Override
    protected void initialize() {
        hasStarted = false;
        if (choice != null) {
            try {
                command = getCommand();
                logger.info("Starting chosen command: {}", command.getName());
                startWithContext(command, this);
            } catch (IllegalArgumentException e) {
                logger.warn("Chosen command not found", e);
            }
        }
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
            logger.info("Chooser cancelled, cancelling {}", command.getName());
            command.cancel();
        }
    }

    @Override
    public void configure(Config config) {
        for (String command : config.options) {
            commandStore.getCommand(command);
        }
        mqttUpdater.createChooser(
                getName(), config.options
        ).subscribe((s) -> choice = s);
    }

    public static class Config {
        public String[] options;
    }
}