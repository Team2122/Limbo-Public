package org.teamtators.limbo.subsystems;

import edu.wpi.first.wpilibj.Joystick;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;

public class OperatorInterface extends Subsystem {
    private LogitechF310 driverJoystick = new LogitechF310();
    private LogitechF310 gunnerJoystick = new LogitechF310();
    private Joystick driverLeft = new Joystick(2);
    private Joystick driverRight = new Joystick(3);
    private Config config;

    private double rumbleTime;
    private double rumblePower;

    public OperatorInterface() {
        super("Operator Interface");
    }

    public LogitechF310 getDriverJoystick() {
        return driverJoystick;
    }

    public LogitechF310 getGunnerJoystick() {
        return gunnerJoystick;
    }

    public Joystick getDriverLeft() {
        return driverLeft;
    }

    public Joystick getDriverRight() {
        return driverRight;
    }

    public void setRumble(LogitechF310 joystick) {
        joystick.setRumble(LogitechF310.RumbleType.BOTH, rumblePower, rumbleTime);
    }

    public void configure(Config config) {
        this.config = config;
        driverJoystick.configure(config.driverJoystick);
        gunnerJoystick.configure(config.gunnerJoystick);
        this.rumbleTime = config.rumbleTime;
        this.rumblePower = config.rumblePower;
    }

    // For tank drive
    public double getDriveLeft() {
        return -driverJoystick.getAxisValue(LogitechF310.Axis.LEFT_STICK_Y);
//        return -driverLeft.getRawAxis(1);
    }

    public double getDriveRight() {
        return -driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_Y);
//        return -driverRight.getRawAxis(1);
    }

    // For field centric driving
    public double getDriveThrottle() {
//        return -driverLeft.getAxisValue(LogitechF310.Axis.LEFT_STICK_Y);
        return -driverLeft.getRawAxis(1);
    }

    public double getDriveTurn() {
//        return driverLeft.getAxisValue(LogitechF310.Axis.LEFT_STICK_X);
        return driverLeft.getRawAxis(0);
    }

    /**
     * @return The commanded target angle, in degrees with forward being 0 and positive clockwise, between -180 and 180
     */
    public double getDriveRotationAngle() {
//        double y = -driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_Y);
//        double x = driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_X);
        double y = -driverRight.getRawAxis(1);
        double x = driverRight.getRawAxis(0);
        double angle = 90 - Math.toDegrees(Math.atan2(y, x));
        if (angle > 180) {
            angle -= 360;
        }
        return angle;
    }

    /**
     * @return The magnitude of the commanded target angle, from 0 to 1
     */
    public double getDriveRotationMagnitude() {
//        double y = -driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_Y);
//        double x = driverJoystick.getAxisValue(LogitechF310.Axis.RIGHT_STICK_X);
        double y = -driverRight.getRawAxis(1);
        double x = driverRight.getRawAxis(0);
        return Math.hypot(x, y);
    }

    public static class Config {
        public LogitechF310.Config driverJoystick;
        public LogitechF310.Config gunnerJoystick;
        public double rumbleTime;
        public double rumblePower;
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup tests = super.createManualTests();
        tests.addTest(new OITest());
        return tests;
    }

    private class OITest extends ManualTest {
        public OITest() {
            super("OITest");
        }

        @Override
        public void start() {
            printTestInstructions("Press A to get statuses");
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            printTestInfo("Tank: Left = {}, Right = {}", getDriveLeft(), getDriveRight());
            printTestInfo("Field Centric: Throttle={}, Turn={}, Angle={}, AngleMagnitude={}",
                    getDriveThrottle(), getDriveTurn(), getDriveRotationAngle(), getDriveRotationMagnitude());
        }
    }
}
