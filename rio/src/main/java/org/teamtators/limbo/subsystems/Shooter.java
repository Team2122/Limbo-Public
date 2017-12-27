package org.teamtators.limbo.subsystems;

import com.ctre.CANTalon;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.CANTalonConfig;
import org.teamtators.common.config.SpeedControllerConfig;
import org.teamtators.common.datalogging.Dashboard;
import org.teamtators.common.datalogging.DashboardUpdatable;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.math.LinearFunction;
import org.teamtators.common.math.PiecewiseLinear;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.CANTalonTest;
import org.teamtators.common.tester.components.SpeedControllerTest;
import org.teamtators.limbo.TatorRobot;

import java.util.Map;

public class Shooter extends Subsystem implements DashboardUpdatable {

    private CANTalon shooterWheel;
    private SpeedController feederMotor;
    private CANTalon.TalonControlMode cachedMode;

    private Config config;
//    public PiecewiseLinear speedMap = new PiecewiseLinear();
    public LinearFunction speedMap;

    private double maxTestSpeed;
    private double desiredSpeed;
    private double speedOffset;
    private State state;

    public Shooter(TatorRobot robot) {
        super("Shooter");
        robot.addSmartDashboardUpdatable(this);
    }

    public void setTargetWheelSpeed(double speed) {
        if (cachedMode != CANTalon.TalonControlMode.Speed) {
            shooterWheel.changeControlMode(CANTalon.TalonControlMode.Speed);
            cachedMode = CANTalon.TalonControlMode.Speed;
        }
        shooterWheel.enable();
        shooterWheel.setSetpoint(speed);
    }

    public double getWheelPower() {
        return shooterWheel.getOutputVoltage();
    }

    public void setWheelPower(double power) {
        if (cachedMode != CANTalon.TalonControlMode.PercentVbus) {
            shooterWheel.changeControlMode(CANTalon.TalonControlMode.PercentVbus);
            cachedMode = CANTalon.TalonControlMode.PercentVbus;
        }
        shooterWheel.set(power);
    }

    public double getCurrentWheelSpeed() {
        return shooterWheel.getSpeed();
    }

    public void setFeederPower(double power) {
        feederMotor.set(power);
    }

    public void stopWheel() {
        this.setWheelPower(0.0);
    }

    public void stopFeeder() {
        this.setFeederPower(0.0);
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public double getDesiredSpeed() {
        return desiredSpeed;
    }

    public void setDesiredSpeed(double rpm) {
        this.desiredSpeed = rpm;
    }

    public double getSpeedOffset() {
        return speedOffset;
    }

    public void setSpeedOffset(double speedOffset) {
        logger.debug("Set speed offset to {}", speedOffset);
        this.speedOffset = speedOffset;
    }

    public double calculateDesiredSpeed() {
        return desiredSpeed + speedOffset;
    }

    public void updateCurrentDistance(double distance) {
        double desiredSpeed = speedMap.calculate(distance);
        logger.trace("Setting desired speed to {} from distance {}", desiredSpeed, distance);
        setDesiredSpeed(desiredSpeed);
    }

    @Override
    public void update(double delta) {
        switch (state) {
            case IDLE:
                this.stopFeeder();
                this.stopWheel();
                break;
            case SPINNING_UP:
                this.setTargetWheelSpeed(this.calculateDesiredSpeed());
                this.stopFeeder();
                break;
            case SHOOTING:
                this.setTargetWheelSpeed(this.calculateDesiredSpeed());
                this.setFeederPower(config.feederPower);
                break;
            case LOAD:
                this.setWheelPower(-0.1);
                this.setFeederPower(-0.5);
                break;
        }
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        if (state == RobotState.DISABLED) {
            setState(State.IDLE);
        }
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup manualTests = super.createManualTests();
        manualTests.addTest(new CANTalonTest("shooterWheel", shooterWheel, maxTestSpeed));
        manualTests.addTest(new SpeedControllerTest("feederMotor", feederMotor));
        manualTests.addTest(new CalibrationTest());
        return manualTests;
    }

    public void configure(Config config) {
        this.config = config;
        shooterWheel = config.shooterWheel.create();
        feederMotor = config.feederMotor.create();
        maxTestSpeed = config.maxTestSpeed;
        desiredSpeed = config.defaultRPM;
        state = config.state;
//        speedMap.configure(config.speedMap);
        speedMap = config.speedMap;
    }

    @Override
    public void updateDashboard(Dashboard dashboard) {
        dashboard.putNumber("Shooter/CurrentSpeed", getCurrentWheelSpeed());
        dashboard.putNumber("Shooter/DesiredSpeed", getDesiredSpeed());
        dashboard.putNumber("Shooter/WheelPower", getWheelPower());
        dashboard.putNumber("Shooter/FeederPower", feederMotor.get());
        dashboard.putString("Shooter/State", state.toString());
    }

    public enum State {
        IDLE,
        SPINNING_UP,
        SHOOTING,
        LOAD,
    }

    public static class Config {
        public CANTalonConfig shooterWheel;
        public SpeedControllerConfig feederMotor;
        public double maxTestSpeed;
        public double defaultRPM;
        public State state;

        public double feederPower;
//        public Map<Double, Double> speedMap;
        public LinearFunction speedMap;
    }

    private class CalibrationTest extends ManualTest {
        public static final int BUMP_OFFSET = 25;
        public static final int BUMP_OFFSET_BIG = 100;

        public CalibrationTest() {
            super("CalibrationTest");
        }

        @Override
        public void start() {
            logger.info("Press A to get info, hold B to shoot, press Y to spin up, hold X to load");
            logger.info("Press left/right bumper to bump down/up {} rpm", BUMP_OFFSET);
            logger.info("Press left/right trigger to bump down/up {} rpm", BUMP_OFFSET_BIG);
            setDesiredSpeed(3000);
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    logger.info("Desired RPM={}, Current RPM={}, Offset={}", calculateDesiredSpeed(),
                            getCurrentWheelSpeed(), getSpeedOffset());
                    break;
                case B:
                    setState(State.SHOOTING);
                    break;
                case Y:
                    setState(State.SPINNING_UP);
                    break;
                case X:
                    setState(State.LOAD);
                    break;
                case BUMPER_LEFT:
                    setSpeedOffset(getSpeedOffset() - BUMP_OFFSET);
                    break;
                case BUMPER_RIGHT:
                    setSpeedOffset(getSpeedOffset() + BUMP_OFFSET);
                    break;
                case TRIGGER_LEFT:
                    setSpeedOffset(getSpeedOffset() - BUMP_OFFSET_BIG);
                    break;
                case TRIGGER_RIGHT:
                    setSpeedOffset(getSpeedOffset() + BUMP_OFFSET_BIG);
                    break;
            }
        }

        @Override
        public void onButtonUp(LogitechF310.Button button) {
            switch (button) {
                case B:
                case X:
                    setState(State.IDLE);
                    break;
            }
        }

        @Override
        public void update(double delta) {
            Shooter.this.update(delta);
        }
    }
}
