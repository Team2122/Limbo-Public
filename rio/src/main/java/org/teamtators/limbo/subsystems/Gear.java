package org.teamtators.limbo.subsystems;

import edu.wpi.first.wpilibj.Solenoid;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.AnalogPoteniometerConfig;
import org.teamtators.common.config.DigitalSensorConfig;
import org.teamtators.common.config.SolenoidConfig;
import org.teamtators.common.config.SpeedControllerConfig;
import org.teamtators.common.control.PidController;
import org.teamtators.common.datalogging.Dashboard;
import org.teamtators.common.datalogging.DashboardUpdatable;
import org.teamtators.common.hw.AnalogPotentiometer;
import org.teamtators.common.hw.DigitalSensor;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.*;
import org.teamtators.limbo.TatorRobot;

public class Gear extends Subsystem implements DashboardUpdatable {
    private SpeedController pickRollerMotor;
    private SpeedController pivotMotor;
    private AnalogPotentiometer pivotEncoder;
    private Solenoid dropJaw;
    private PidController gearPivotController;
    private DigitalSensor gearSensor;
    private DigitalSensor pegSensorA;
    private DigitalSensor pegSensorB;

    private State state = State.IDLE;
    private Config config;

    public Gear(TatorRobot robot) {
        super("Gear");
        robot.addSmartDashboardUpdatable(this);
        gearPivotController = new PidController("gearPivotController");
        gearPivotController.setInputProvider(this::getPivotAngle);
        gearPivotController.setOutputConsumer(this::setPivotPower);
    }

    public void setPickGearPower(double power) {
        pickRollerMotor.set(power);
    }

    public void setPivotPower(double power) {
        pivotMotor.set(power);
    }

    public double getPivotAngle() {
        return pivotEncoder.get();
    }

    public boolean getJawDropped() {
        return dropJaw.get();
    }

    public void setJawDropped(boolean isDropped) {
        dropJaw.set(isDropped);
    }

    public PidController getGearPivotController() {
        return gearPivotController;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void requestIdle() {
        this.setState(State.IDLE);
    }

    public void requestHome() {
        this.setState(State.HOME);
    }

    public void requestReleased() {
        this.setState(State.RELEASED);
    }

    public void requestPick() {
        this.setState(State.PICK);
    }

    public boolean isPegIn() {
        return pegSensorA.get() || pegSensorB.get();
    }

    public void requestTogglePick() {
        if (this.state == State.PICK) {
            this.requestHome();
        } else {
            this.requestPick();
        }
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup manualTests = super.createManualTests();
        manualTests.addTest(new SpeedControllerTest("pickRollerMotor", pickRollerMotor));
        manualTests.addTest(new SpeedControllerTest("pivotMotor", pivotMotor));
        manualTests.addTest(new AnalogPotentiometerTest("pivotEncoder", pivotEncoder));
        manualTests.addTest(new SolenoidTest("dropJaw", dropJaw));
        manualTests.addTest(new DigitalSensorTest("gearSensor", gearSensor));
        manualTests.addTest(new DigitalSensorTest("pegSensorA", pegSensorA));
        manualTests.addTest(new DigitalSensorTest("pegSensorB", pegSensorB));
        manualTests.addTest(new ControllerTest(gearPivotController));
        return manualTests;
    }

    public Config getConfig() {
        return config;
    }

    public boolean isGearIn() {
        return gearSensor.get();
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        if (state == RobotState.DISABLED) {
            setState(State.IDLE);
        }
    }

    public void configure(Config config) {
        dropJaw = config.dropJaw.create();
        pickRollerMotor = config.pickRollerMotor.create();
        pivotMotor = config.pivotMotor.create();
        pivotEncoder = config.pivotEncoder.create();
        gearPivotController.configure(config.gearPivotController);
        gearSensor = config.gearSensor.create();
        pegSensorA = config.pegSensorA.create();
        pegSensorB = config.pegSensorB.create();
        this.config = config;
        gearPivotController.setMinSetpoint(config.homeAngle);
        gearPivotController.setMaxSetpoint(config.pickAngle);
    }

    @Override
    public void updateDashboard(Dashboard dashboard) {
        dashboard.putNumber("Gear/CurrentAngle", getPivotAngle());
        dashboard.putNumber("Gear/DesiredAngle", gearPivotController.getSetpoint());
        dashboard.putString("Gear/State", state.toString());
        dashboard.putBoolean("Gear/GearSensor", isGearIn());
        dashboard.putBoolean("Gear/PegSensor", isPegIn());
    }

    public enum State {
        IDLE,
        HOME,
        PICK,
        RELEASED
    }

    public static class Config {
        public SolenoidConfig dropJaw;
        public SpeedControllerConfig pickRollerMotor;
        public SpeedControllerConfig pivotMotor;
        public AnalogPoteniometerConfig pivotEncoder;
        public PidController.Config gearPivotController;
        public DigitalSensorConfig gearSensor;
        public DigitalSensorConfig pegSensorA;
        public DigitalSensorConfig pegSensorB;
        public double homeAngle;
        public double releasedAngle;
        public double pickAngle;
        public double pickPower;
        public double homeHoldPower;
        public double pickHoldPower;
    }
}
