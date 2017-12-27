package org.teamtators.limbo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.wpi.first.wpilibj.Timer;
import org.teamtators.common.SubsystemsBase;
import org.teamtators.common.TatorRobotBase;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.datalogging.Dashboard;
import org.teamtators.common.datalogging.DashboardUpdatable;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.RobotStateListener;
import org.teamtators.limbo.commands.CommandRegistrar;
import org.teamtators.limbo.subsystems.Subsystems;

/**
 * @author Alex Mikhalev
 */
public class TatorRobot extends TatorRobotBase implements RobotStateListener, Updatable, DashboardUpdatable {
    private final Subsystems subsystems;
    private final CommandRegistrar commandRegistrar = new CommandRegistrar(this);

    public TatorRobot(String configDir) {
        super(configDir);
        subsystems = new Subsystems(this);
        addSmartDashboardUpdatable(this);
    }

    public Subsystems getSubsystems() {
        return subsystems;
    }

    @Override
    public SubsystemsBase getSubsystemsBase() {
        return subsystems;
    }

    @Override
    protected void registerCommands() {
        commandRegistrar.register(getCommandStore());
    }

    @Override
    protected void startThreads() {
        super.startThreads();
        subsystems.getMqttUpdater().connect();
    }

    @Override
    public void update(double delta) {
        super.update(delta);
        if (getState() == RobotState.TEST) {
            subsystems.getMqttUpdater().update(delta);
        }
    }

    @Override
    protected void postInitialize() {
        logger.info("==> Limbo: insert funny pun relating to limbo <==");
    }

    @Override
    public LogitechF310 getGunnerJoystick() {
        return subsystems.getOperatorInterface().getGunnerJoystick();
    }

    @Override
    public LogitechF310 getDriverJoystick() {
        return subsystems.getOperatorInterface().getDriverJoystick();
    }

    @Override
    protected void configureTriggers() {
        logger.debug("Configuring triggers");
        ObjectNode triggersConfig = (ObjectNode) configLoader.load("Triggers.yaml");
        triggerBinder.setDriverJoystick(getDriverJoystick());
        triggerBinder.setGunnerJoystick(getGunnerJoystick());
        triggerBinder.bindTriggers(triggersConfig);
        triggerBinder.bindButtonsToJoystick(getSubsystems().getOperatorInterface().getDriverLeft());
        triggerBinder.bindButtonsToJoystick(getSubsystems().getOperatorInterface().getDriverRight());
    }

    @Override
    public String getName() {
        return "TatorRobot";
    }

    public void updateDashboard(Dashboard dashboard) {
        dashboard.putNumber("Robot/Timestamp", Timer.getFPGATimestamp());
        dashboard.putNumber("Robot/MatchTime", Timer.getMatchTime());
        dashboard.putNumber("Robot/StateTime", stateTimer.get());
        dashboard.putNumber("Robot/DeltaTime", lastDelta);
    }
}