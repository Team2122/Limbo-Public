package org.teamtators.common.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Command implements CommandRunContext {
    protected Logger logger;
    private String name;
    private CommandRunContext context = null;
    private Set<Subsystem> requirements = null;
    private EnumSet<RobotState> validStates = EnumSet.of(RobotState.AUTONOMOUS, RobotState.TELEOP);

    public Command(String name) {
        checkNotNull(name);
        setName(name);
    }

    protected void initialize() {
        logger.info("{} initializing", getName());
    }

    protected abstract boolean step();

    protected void finish(boolean interrupted) {
        if (interrupted) {
            logger.info("{} interrupted", getName());
        } else {
            logger.info("{} ended", getName());
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        String loggerName = String.format("%s(%s)", this.getClass().getName(), name);
        this.logger = LoggerFactory.getLogger(loggerName);
    }

    CommandRunContext getContext() {
        return context;
    }

    private void setContext(CommandRunContext context) {
        this.context = context;
    }

    public boolean isRunning() {
        return this.context != null;
    }

    @Override
    public void startWithContext(Command command, CommandRunContext context) {
        if (this.context == null) {
            logger.error("Tried add command {} in parent context while not running", command.getName());
        } else
            this.context.startWithContext(command, context);
    }

    @Override
    public void cancelCommand(Command command) {
        if (this.context == null || command.getContext() == null) {
            logger.debug("Tried to cancel command that is not running");
        } else
            this.context.cancelCommand(command);
    }

    public void cancel() {
        this.cancelCommand(this);
    }

    protected void requires(Subsystem subsystem) {
        checkNotNull(subsystem, "Cannot require a null subsystem");
        if (requirements == null) {
            requirements = new HashSet<>();
        }
        requirements.add(subsystem);
    }

    protected void requiresAll(Collection<Subsystem> subsystems) {
        checkNotNull(subsystems);
        if (requirements == null) {
            requirements = new HashSet<>();
        }
        requirements.addAll(subsystems);
    }

    Set<Subsystem> getRequirements() {
        return requirements;
    }

    protected void setRequirements(Set<Subsystem> requirements) {
        this.requirements = requirements;
    }

    public boolean doesRequire(Subsystem subsystem) {
        return requirements != null && requirements.contains(subsystem);
    }

    private boolean isRequiring(Subsystem subsystem, CommandRunContext context) {
        Command requiringCommand = subsystem.getRequiringCommand();
        return requiringCommand == this ||
                context instanceof Command &&
                        ((Command) context).isRequiring(subsystem);
    }

    public boolean isRequiring(Subsystem subsystem) {
        return isRequiring(subsystem, getContext());
    }

    protected boolean checkRequirements(Iterable<Subsystem> requirements) {
        if (requirements == null)
            return true;
        for (Subsystem subsystem : requirements) {
            Command requiringCommand = subsystem.getRequiringCommand();
            if (requiringCommand == null || isRequiring(subsystem))
                continue;
            return false;
        }
        return true;
    }

    public boolean checkRequirements() {
        return checkRequirements(getRequirements());
    }

    private boolean takeRequirements(Iterable<Subsystem> requirements, CommandRunContext context) {
        if (requirements == null) return true;
        boolean anyRequiring = false;
        for (Subsystem subsystem : requirements) {
            Command requiringCommand = subsystem.getRequiringCommand();
            if (requiringCommand == null) {
                subsystem.setRequiringCommand(this);
                continue;
            }
            if (isRequiring(subsystem, context))
                continue;
            anyRequiring = true;
            requiringCommand.cancel();
        }
        return !anyRequiring;
    }

    protected boolean cancelRequiring(Subsystem... requirements) {
        return takeRequirements(Arrays.asList(requirements), null);
    }

    private boolean takeRequirements(CommandRunContext context) {
        return takeRequirements(this.requirements, context);
    }

    boolean startRun(CommandRunContext context) {
        if (isRunning() || !takeRequirements(context))
            return false;
        setContext(context);
        initialize();
        return true;
    }

    void finishRun(boolean cancelled) {
        if (isRunning()) {
            finish(cancelled);
            setContext(null);
        }
        releaseRequirements();
    }

    private void releaseRequirements() {
        if (requirements == null)
            return;
        for (Subsystem subsystem : requirements) {
            if (subsystem.getRequiringCommand() == this)
                subsystem.setRequiringCommand(null);
        }
    }

    public boolean isValidInState(RobotState state) {
        return validStates.contains(state);
    }

    protected void validIn(RobotState... states) {
        setValidStates(EnumSet.copyOf(Arrays.asList(states)));
    }

    public EnumSet<RobotState> getValidStates() {
        return validStates;
    }

    protected void setValidStates(EnumSet<RobotState> validStates) {
        this.validStates = validStates;
    }
}
