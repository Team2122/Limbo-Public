package org.teamtators.limbo.commands;

import org.teamtators.common.control.PidController;
import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.Gear;
import org.teamtators.limbo.subsystems.OperatorInterface;

public class GearContinuous extends Command {
    private Gear gear;
    private TatorRobot robot;
    private OperatorInterface operatorInterface;
    private Gear.Config config;
    private PidController gearPivotController;
    private Gear.State lastState = null;

    public GearContinuous(TatorRobot robot) {
        super("GearContinuous");
        gear = robot.getSubsystems().getGear();
        config = gear.getConfig();
        gearPivotController = gear.getGearPivotController();
        this.robot = robot;
        this.operatorInterface = robot.getSubsystems().getOperatorInterface();
    }

    @Override
    protected boolean step() {
        Gear.State state = gear.getState();
        RobotState robotState = robot.getState();
        if (state == Gear.State.PICK && gear.isGearIn()) {
            state = Gear.State.HOME;
            gear.setState(state);
        }

        if (gear.isPegIn() && robotState == RobotState.TELEOP) {
            operatorInterface.setRumble(operatorInterface.getDriverJoystick());
        }

        switch (state) {
            case IDLE:
                idle();
                break;
            case HOME:
                gearPivotController.start();
                gearPivotController.setSetpoint(config.homeAngle);
                gearPivotController.setHoldPower(config.homeHoldPower);
                gear.setJawDropped(false);
                gear.setPickGearPower(0.0);
                break;
            case RELEASED:
                gearPivotController.start();
                gearPivotController.setSetpoint(config.releasedAngle);
                gearPivotController.setHoldPower(0);
                gear.setJawDropped(true);
                gear.setPickGearPower(0.0);
                break;
            case PICK:
                gearPivotController.setSetpoint(config.pickAngle);
                gearPivotController.setHoldPower(config.pickPower);
                gearPivotController.start();
                gear.setJawDropped(false);
                gear.setPickGearPower(config.pickPower);
                break;
        }
        if (state != lastState) {
            logger.debug("Switched from the {} state to the {} state", lastState, state);
            lastState = state;
        }
        return false;
    }

    @Override
    protected void finish(boolean interrupted) {
        idle();
        super.finish(interrupted);
    }

    private void idle() {
        gearPivotController.stop();
        gear.setJawDropped(false);
        gear.setPivotPower(0.0);
        gear.setPickGearPower(0.0);
    }

}
