package org.teamtators.limbo.subsystems;

import edu.wpi.first.wpilibj.*;
import org.teamtators.common.config.EncoderConfig;
import org.teamtators.common.config.SolenoidConfig;
import org.teamtators.common.config.SpeedControllerConfig;
import org.teamtators.common.control.*;
import org.teamtators.common.datalogging.Dashboard;
import org.teamtators.common.datalogging.DashboardUpdatable;
import org.teamtators.common.hw.ADXRS453;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.ManualTestable;
import org.teamtators.common.tester.components.*;
import org.teamtators.limbo.TatorRobot;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;


public class Drive extends org.teamtators.common.scheduler.Subsystem implements ManualTestable
        , DashboardUpdatable {

    public static final Predicate<TrapezoidalProfileFollower> DEFAULT_PREDICATE = ControllerPredicates.finished();
    private SpeedController leftMotor;
    private SpeedController rightMotor;
    private Encoder leftEncoder;
    private Encoder rightEncoder;
    private Encoder leftTransEncoder;
    private Encoder rightTransEncoder;
    private Solenoid shifterSolenoidA;
    private Solenoid shifterSolenoidB;
    private ADXRS453 gyro;
    private PowerDistributionPanel pdp;

    private Gear gear;
    private PidController leftController = new PidController("DriveLeftController");
    private PidController rightController = new PidController("DriveRightController");
    private PidController rotationController = new PidController("DriveRotationController");

    private PidController leftTransController = new PidController("LeftTransController");
    private PidController rightTransController = new PidController("RightTransController");

    private TrapezoidalProfileFollower straightMotionFollower = new TrapezoidalProfileFollower("DriveMotionFollower");
    private PidController yawAngleController = new PidController("DriveYawAngleController");
    private OutputController outputController = new OutputController();

    private TrapezoidalProfileFollower rotationMotionFollower =
            new TrapezoidalProfileFollower("DriveRotationMotionFollower");

    private double straightVelocity;
    private boolean hasCalibratedGyro = false;

    private Config config;

    public Drive(TatorRobot tatorRobot) {
        super("Drive");
        tatorRobot.addSmartDashboardUpdatable(this);

        pdp = tatorRobot.getPDP();

        leftController.setInputProvider(this::getLeftRate);
        leftController.setOutputConsumer(this::setLeftMotorPower);

        rightController.setInputProvider(this::getRightRate);
        rightController.setOutputConsumer(this::setRightMotorPower);

        rightTransController.setInputProvider(this::getRightTransRate);
        rightTransController.setOutputConsumer(this::setRightMotorPower);

        leftTransController.setInputProvider(this::getLeftTransRate);
        leftTransController.setOutputConsumer(this::setLeftMotorPower);

        rotationController.setInputProvider(this::getYawAngle);
        rotationController.setOutputConsumer(output -> {
            leftController.setSetpoint(straightVelocity + output);
            rightController.setSetpoint(straightVelocity - output);
        });

        straightMotionFollower.setPositionProvider(this::getAverageDistance);
        straightMotionFollower.setVelocityProvider(this::getAverageRate);
        straightMotionFollower.setOutputConsumer(outputController::setStraightOutput);

        yawAngleController.setInputProvider(this::getYawAngle);
        yawAngleController.setOutputConsumer(outputController::setRotationOutput);

        rotationMotionFollower.setPositionProvider(this::getYawAngle);
        rotationMotionFollower.setVelocityProvider(this::getYawRate);
        rotationMotionFollower.setOutputConsumer(outputController::setRotationOutput);
    }

    public List<Updatable> getUpdatables() {
        return Arrays.asList(
                gyro, rotationController, leftController, rightController, yawAngleController,
                straightMotionFollower, yawAngleController, outputController, rotationMotionFollower,
                leftTransController, rightTransController
        );
    }

    private void setLeftMotorPower(double power) {
        leftMotor.set(power);
    }

    private void setRightMotorPower(double power) {
        rightMotor.set(power);
    }

    public PidController getLeftController() {
        return leftController;
    }

    public PidController getRightController() {
        return rightController;
    }

    public PidController getRotationController() {
        return rotationController;
    }

    public TrapezoidalProfileFollower getStraightMotionFollower() {
        return straightMotionFollower;
    }

    public PidController getYawAngleController() {
        return yawAngleController;
    }

    public OutputController getOutputController() {
        return outputController;
    }

    public TrapezoidalProfileFollower getRotationMotionFollower() {
        return rotationMotionFollower;
    }

    public void driveVelocities(double leftVelocity, double rightVelocity) {
        rotationController.stop();
        leftController.start();
        rightController.start();
        straightMotionFollower.stop();
        yawAngleController.stop();
        rotationMotionFollower.stop();
        outputController.stop();
        outputController.setMode(OutputMode.None);
        rightTransController.stop();
        leftTransController.stop();

        leftController.setSetpoint(leftVelocity);
        rightController.setSetpoint(rightVelocity);
    }

    public void driveHeading(double heading, double straightVelocity) {
        rotationController.start();
        leftController.start();
        rightController.start();
        straightMotionFollower.stop();
        yawAngleController.stop();
        rotationMotionFollower.stop();
        outputController.stop();
        outputController.setMode(OutputMode.None);
        rightTransController.stop();
        leftTransController.stop();
        this.straightVelocity = straightVelocity;
        rotationController.setSetpoint(heading);
    }

    public void drivePowers(double leftPower, double rightPower) {
        leftController.stop();
        rightController.stop();
        rotationController.stop();
        straightMotionFollower.stop();
        yawAngleController.stop();
        rotationMotionFollower.stop();
        outputController.stop();
        outputController.setMode(OutputMode.None);
        rightTransController.stop();
        leftTransController.stop();
        leftMotor.set(leftPower);
        rightMotor.set(rightPower);
    }

    public void drivePowers(Output output) {
        drivePowers(output.leftPower, output.rightPower);
    }

    public void driveStraightProfile(double heading, TrapezoidalProfile profile,
                                     Predicate<TrapezoidalProfileFollower> targetCondition) {
        leftController.stop();
        rightController.stop();
        rotationController.stop();
        rotationMotionFollower.stop();
        rightTransController.stop();
        leftTransController.stop();
        profile.setStartVelocity(getAverageRate());
        straightMotionFollower.setBaseProfile(profile);
        straightMotionFollower.setOnTargetPredicate(targetCondition);
        yawAngleController.setSetpoint(heading);
        outputController.setMode(OutputMode.StraightAndRotation);

        if (straightMotionFollower.isRunning()) {
            straightMotionFollower.updateProfile();
        } else {
            straightMotionFollower.start();
        }
        yawAngleController.start();
        outputController.start();
    }

    public void driveStraightProfile(double heading, TrapezoidalProfile profile) {
        driveStraightProfile(heading, profile, ControllerPredicates.finished());
    }

    public void driveRotationProfile(TrapezoidalProfile profile,
                                     Predicate<TrapezoidalProfileFollower> targetCondition) {
        rotationController.stop();
        leftController.stop();
        rightController.stop();
        straightMotionFollower.stop();
        yawAngleController.stop();
        rightTransController.stop();
        leftTransController.stop();
        profile.setStartVelocity(getYawRate());
        rotationMotionFollower.setBaseProfile(profile);
        rotationMotionFollower.setOnTargetPredicate(targetCondition);
        outputController.setMode(OutputMode.RotationOnly);

        if (rotationMotionFollower.isRunning()) {
            rotationMotionFollower.updateProfile();
        } else {
            rotationMotionFollower.start();
        }
        outputController.start();
    }

    public void driveRotationProfile(TrapezoidalProfile profile) {
        driveRotationProfile(profile, ControllerPredicates.finished());
    }

    public void driveArcProfile(TrapezoidalProfile straightProfile, TrapezoidalProfile rotationProfile) {
        leftController.stop();
        rightController.stop();
        rotationController.stop();
        yawAngleController.stop();
        rightTransController.stop();
        leftTransController.stop();
        straightMotionFollower.setBaseProfile(straightProfile);
        straightMotionFollower.setOnTargetPredicate(DEFAULT_PREDICATE);
        rotationMotionFollower.setBaseProfile(rotationProfile);
        rotationMotionFollower.setOnTargetPredicate(DEFAULT_PREDICATE);
        outputController.setMode(OutputMode.StraightAndRotation);

        if (straightMotionFollower.isRunning()) {
            straightMotionFollower.updateProfile();
        } else {
            straightMotionFollower.start();
        }
        if (rotationMotionFollower.isRunning()) {
            rotationMotionFollower.updateProfile();
        } else {
            rotationMotionFollower.start();
        }
        outputController.start();
    }

    public void stop() {
        drivePowers(0, 0);
    }

    public boolean isStraightProfileOnTarget() {
        return straightMotionFollower.isOnTarget();
    }

    public boolean isRotationProfileOnTarget() {
        return rotationMotionFollower.isOnTarget();
    }

    public boolean isArcOnTarget() {
        return straightMotionFollower.isOnTarget() && rotationMotionFollower.isOnTarget();
    }

    public double getLeftDistance() {
        return leftEncoder.getDistance();
    }

    public double getRightDistance() {
        return rightEncoder.getDistance();
    }

    public double getLeftRate() {
        return leftEncoder.getRate();
    }

    public double getRightRate() {
        return rightEncoder.getRate();
    }

    public double getLeftTransRate() {
        return leftTransEncoder.getRate();
    }

    public double getRightTransRate() {
        return rightTransEncoder.getRate();
    }

    public double getAverageRate() {
        return (getRightRate() + getLeftRate()) / 2.0;
    }

    public void resetDistances() {
        leftEncoder.reset();
        rightEncoder.reset();
    }

    public double getAverageDistance() {
        return (this.getLeftDistance() + this.getRightDistance()) / 2;
    }

    public double getRightCurrent() {
        return config.rightMotor.getTotalCurrent(pdp);
    }

    public double getLeftCurrent() {
        return config.leftMotor.getTotalCurrent(pdp);
    }

    public double getYawAngle() {
        return gyro.getAngle();
    }

    public void resetYawAngle() {
        logger.info("Resetting drive yaw gyro angle to 0");
        gyro.resetAngle();
    }

    public double getYawRate() {
        return gyro.getRate();
    }

    public ADXRS453 getGyro() {
        return gyro;
    }

    public Gear getGear() {
        return gear;
    }

    public void setGear(Gear gear) {
        this.gear = gear;
        switch (gear) {
            case LOW:
                shifterSolenoidA.set(false);
                shifterSolenoidB.set(true);
                break;
            case NEUTRAL:
                shifterSolenoidA.set(false);
                shifterSolenoidB.set(false);
                break;
            case HIGH:
                shifterSolenoidA.set(true);
                shifterSolenoidB.set(false);
                break;
        }
    }

    @Override
    public void onEnterRobotState(RobotState state) {
        switch (state) {
            case TELEOP:
            case AUTONOMOUS:
                gyro.finishCalibration();
                break;
        }
        if (state == RobotState.AUTONOMOUS) {
            gyro.resetAngle();
        }
    }

    public void configure(Config config) {
        this.config = config;
        this.leftMotor = config.leftMotor.create();
        this.rightMotor = config.rightMotor.create();
        this.leftEncoder = config.leftEncoder.create();
        this.rightEncoder = config.rightEncoder.create();
        this.leftTransEncoder = config.leftTransEncoder.create();
        this.rightTransEncoder = config.rightTransEncoder.create();
        this.shifterSolenoidA = config.shifterSolenoidA.create();
        this.shifterSolenoidB = config.shifterSolenoidB.create();
        this.gyro = new ADXRS453(SPI.Port.kOnboardCS0);
        this.leftController.configure(config.controllerLow);
        this.rightController.configure(config.controllerLow);
        this.rotationController.configure(config.rotationController);
        this.straightMotionFollower.configure(config.straightMotionFollower);
        this.yawAngleController.configure(config.yawAngleController);
        this.rotationMotionFollower.configure(config.rotationMotionFollower);
        this.leftTransController.configure(config.transController);
        this.rightTransController.configure(config.transController);
        gyro.start();
        gyro.startCalibration();
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup manualTests = super.createManualTests();
        manualTests.addTest(new SpeedControllerTest("leftMotor", leftMotor));
        manualTests.addTest(new EncoderTest("leftEncoder", leftEncoder));
        manualTests.addTest(new EncoderTest("leftTransEncoder", leftTransEncoder));
        manualTests.addTest(new SpeedControllerTest("rightMotor", rightMotor));
        manualTests.addTest(new EncoderTest("rightEncoder", rightEncoder));
        manualTests.addTest(new EncoderTest("rightTransEncoder", rightTransEncoder));
        manualTests.addTest(new ADXRS453Test("gyro", gyro));
        manualTests.addTest(new SolenoidTest("gearA", shifterSolenoidA));
        manualTests.addTest(new SolenoidTest("gearB", shifterSolenoidB));
        manualTests.addTest(new ControllerTest(leftController, config.maxTestVelocity));
        manualTests.addTest(new ControllerTest(rightController, config.maxTestVelocity));
        manualTests.addTest(new ControllerTest(rotationController, 180.0));
        manualTests.addTest(new ControllerTest(yawAngleController, 180.0));
        manualTests.addTest(new ControllerTest(leftTransController, 24));
        manualTests.addTest(new ControllerTest(rightTransController, 24));
        manualTests.addTest(new CalibrationTest());
        return manualTests;
    }

    @Override
    public void updateDashboard(Dashboard dashboard) {
        dashboard.putNumber("Drive/LeftPower", leftMotor.get());
        dashboard.putNumber("Drive/RightPower", rightMotor.get());
        dashboard.putNumber("Drive/LeftVelocity", getLeftRate());
        dashboard.putNumber("Drive/RightVelocity", getRightRate());
        dashboard.putNumber("Drive/LeftDistance", getLeftDistance());
        dashboard.putNumber("Drive/RightDistance", getRightDistance());
        dashboard.putNumber("Drive/LeftTransVelocity", getLeftTransRate());
        dashboard.putNumber("Drive/RightTransVelocity", getRightTransRate());
        dashboard.putNumber("Drive/LeftCurrent", getLeftCurrent());
        dashboard.putNumber("Drive/RightCurrent", getRightCurrent());
        dashboard.putNumber("Drive/YawAngle", getYawAngle());
        dashboard.putNumber("Drive/YawRate", getYawRate());
    }

    public enum Gear {
        HIGH,
        NEUTRAL,
        LOW
    }

    private enum CalibrationType {
        StraightVelocity, StraightAcceleration, AngularVelocity, AngularAcceleration
    }

    private enum OutputMode {
        StraightOnly, RotationOnly, StraightAndRotation, None
    }

    public static class Output {
        public double leftPower;
        public double rightPower;

        public Output(double leftPower, double rightPower) {
            this.leftPower = leftPower;
            this.rightPower = rightPower;
        }

        public Output() {
            this(0.0, 0.0);
        }
    }

    public static class Config {
        public SpeedControllerConfig leftMotor;
        public SpeedControllerConfig rightMotor;
        public EncoderConfig leftEncoder;
        public EncoderConfig rightEncoder;
        public EncoderConfig leftTransEncoder;
        public EncoderConfig rightTransEncoder;
        public SolenoidConfig shifterSolenoidA;
        public SolenoidConfig shifterSolenoidB;
        public PidController.Config controllerLow;
        public PidController.Config controllerHigh;
        public PidController.Config rotationController;
        public TrapezoidalProfileFollower.Config straightMotionFollower;
        public PidController.Config yawAngleController;
        public TrapezoidalProfileFollower.Config rotationMotionFollower;
        public double maxTestVelocity;
        public PidController.Config transController;
    }

    private class OutputController extends AbstractUpdatable {
        private double straightOutput;
        private double rotationOutput;
        private OutputMode mode = OutputMode.StraightOnly;

        public OutputMode getMode() {
            return mode;
        }

        public void setMode(OutputMode mode) {
            this.mode = mode;
        }

        public void setStraightOutput(double followerOutput) {
            this.straightOutput = followerOutput;
        }

        public void setRotationOutput(double rotationOutput) {
            this.rotationOutput = rotationOutput;
        }

        @Override
        protected void doUpdate(double delta) {
            double left = 0, right = 0;
            if (mode == OutputMode.StraightOnly || mode == OutputMode.StraightAndRotation) {
                left += straightOutput;
                right += straightOutput;
                straightOutput = Double.NaN;
            }
            if (mode == OutputMode.RotationOnly || mode == OutputMode.StraightAndRotation) {
                left += rotationOutput;
                right -= rotationOutput;
                rotationOutput = Double.NaN;
            }
            if (mode != OutputMode.None && !Double.isNaN(left) && !Double.isNaN(right)) {
                setLeftMotorPower(left);
                setRightMotorPower(right);
                logger.trace("driving at powers {}, {}", left, right);
            } else {
                setLeftMotorPower(0.0);
                setRightMotorPower(0.0);
                logger.trace("not driving, something was NaN: {}, {}", left, right);
            }
        }
    }

    private class CalibrationTest extends ManualTest {
        CalibrationType type;
        double lastAxis;
        double inputParam;
        double lastVel, lastVelTime;
        private boolean isRunning;
        private double vel, acc, accelerationPower;

        CalibrationTest() {
            super("CalibrationTest");
        }

        @Override
        public void start() {
            logger.info("Press X to cycle through values to calibrate, Y to set input parameter to joystick value");
            logger.info("Hold B to run, A to print info");
            if (type == null) {
                setType(CalibrationType.StraightVelocity);
            }
        }

        private void setType(CalibrationType type) {
            this.type = type;
            logger.info("Changed calibration type to {}", type);
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case X:
                    int numTypes = CalibrationType.values().length;
                    int nextIndex = (type.ordinal() + 1) % numTypes;
                    setType(CalibrationType.values()[nextIndex]);
                    break;
                case Y:
                    inputParam = lastAxis;
                    logger.info("Set input parameter to {}", inputParam);
                    break;
                case B:
                    this.startRun();
                    break;
                case A:
                    this.printInfo();
                    break;
            }
        }

        private void startRun() {
            isRunning = true;
            switch (type) {
                case StraightVelocity:
                case StraightAcceleration:
                    Drive.this.drivePowers(inputParam, inputParam);
                    break;

                case AngularVelocity:
                case AngularAcceleration:
                    Drive.this.drivePowers(inputParam, -inputParam);
                    break;
            }
        }

        private void stopRun() {
            isRunning = false;
            Drive.this.stop();
        }

        private void printInfo() {
            switch (type) {
                case StraightVelocity:
                case StraightAcceleration: {
                    logger.info("straight: power={}, velocity={}, acceleration={}, accelerationPower={}",
                            inputParam, vel, acc, accelerationPower);
                    break;
                }

                case AngularVelocity:
                case AngularAcceleration: {
                    logger.info("angular: power={}, velocity={}, acceleration={}, accelerationPower={}",
                            inputParam, vel, acc, accelerationPower);
                    break;
                }
            }
        }

        @Override
        public void onButtonUp(LogitechF310.Button button) {
            switch (button) {
                case B:
                    this.stopRun();
                    break;
            }
        }

        @Override
        public void updateAxis(double value) {
            this.lastAxis = value;
        }

        @Override
        public void update(double delta) {
            switch (type) {
                case StraightVelocity:
                case StraightAcceleration: {
                    vel = Drive.this.getAverageRate();
                    lastVelTime += delta;
                    if (lastVelTime >= 0.1) {
                        acc = (vel - lastVel) / lastVelTime;
                        lastVel = vel;
                        lastVelTime = 0;
                    }
                    accelerationPower = inputParam - vel * config.straightMotionFollower.kfV
                            - Math.copySign(config.straightMotionFollower.kMinOutput, vel);
                    break;
                }

                case AngularVelocity:
                case AngularAcceleration: {
                    vel = Drive.this.getYawRate();
                    lastVelTime += delta;
                    if (lastVelTime >= 0.1) {
                        acc = (vel - lastVel) / lastVelTime;
                        lastVel = vel;
                        lastVelTime = 0;
                    }
                    accelerationPower = inputParam - vel * config.rotationMotionFollower.kfV
                            - Math.copySign(config.rotationMotionFollower.kMinOutput, vel);
                    break;
                }
            }
        }

        @Override
        public void stop() {
            stopRun();
        }
    }
}
