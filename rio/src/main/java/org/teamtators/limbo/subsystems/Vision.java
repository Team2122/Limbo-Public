package org.teamtators.limbo.subsystems;

import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.Timer;
import org.teamtators.common.config.RelayConfig;
import org.teamtators.common.datalogging.Dashboard;
import org.teamtators.common.datalogging.DashboardUpdatable;
import org.teamtators.common.hw.LogitechF310;
import org.teamtators.common.math.Polynomial3;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTest;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.RelayTest;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.vision.RobotData;
import org.teamtators.vision.VisionData;
import org.teamtators.vision.VisionMode;

import java.util.concurrent.atomic.AtomicReference;

public class Vision extends Subsystem implements DashboardUpdatable {
    private final Drive drive;
    private final Shooter shooter;
    private final AtomicReference<VisionData> visionData = new AtomicReference<>(new VisionData());
    private Relay lightRelay;

    private Config config;
    private VisionMode visionMode = VisionMode.NONE;
    private boolean shouldUpdateShooter = false;

    public Vision(Drive drive, Shooter shooter, TatorRobot robot) {
        super("Vision");
        this.drive = drive;
        this.shooter = shooter;
        robot.addSmartDashboardUpdatable(this);
    }

    public RobotData getRobotData() {
        RobotData data = new RobotData();
        data.time = Timer.getFPGATimestamp();
        data.visionMode = visionMode;
        data.gyroAngle = drive.getYawAngle();
        data.driveDistance = drive.getAverageDistance();
        return data;
    }

    public VisionData getVisionData() {
        return visionData.get();
    }

    public Double getYawOffset(VisionData visionData) {
        if (visionData.x == null) return null;
        double offset;
        if (visionMode == VisionMode.FUEL) {
            offset = config.yawOffsetFuel;
        } else {
            offset = config.yawOffsetGear;
        }
        return (.5 * visionData.x * config.fovX) + offset;
    }

    public Double getLastYawOffset() {
        return getYawOffset(getVisionData());
    }

    public Double getFuelDistance(VisionData visionData) {
        if (visionData.robotData.visionMode != VisionMode.FUEL || visionData.y == null) {
            return null;
        }
        //noinspection SuspiciousNameCombination
        return config.distancePolyFuel.calculate(visionData.y);
    }

    public Double getGearDistance(VisionData visionData) {
        if (visionData.robotData.visionMode != VisionMode.GEAR ||
                visionData.separation == null) {
            return null;
        }
        return config.distanceGear.calculate(visionData.separation);
    }

    public Double getLastFuelDistance() {
        return getFuelDistance(getVisionData());
    }

    public Double getLastGearDistance() {
        return getGearDistance(getVisionData());
    }

    public Double getNewRobotAngle(VisionData data) {
        Double yawOffset = getYawOffset(data);
        if (yawOffset == null) {
            return null;
        }
        return yawOffset + data.robotData.gyroAngle;
    }

    public Double getLastRobotAngle() {
        return getNewRobotAngle(getVisionData());
    }

    public void updateVisionData(VisionData visionData) {
        this.visionData.set(visionData);
        if (shouldUpdateShooter) {
            Double distance = getFuelDistance(visionData);
            if (distance != null) {
                shooter.updateCurrentDistance(distance);
            }
        }
    }

    @Override
    public void updateDashboard(Dashboard dashboard) {
        VisionData visionData = getVisionData();
        Double distance = getFuelDistance(visionData);
        if (distance == null) {
            distance = getGearDistance(visionData);
        }
        if (distance == null) {
            distance = Double.NaN;
        }
        Double newRobotAngle = getNewRobotAngle(visionData);
        Double lastYawOffset = getYawOffset(visionData);
        dashboard.putNumber("Vision/NewDriveAngle", newRobotAngle == null ? Double.NaN : newRobotAngle);
        dashboard.putNumber("Vision/LastYawOffset", lastYawOffset == null ? Double.NaN : lastYawOffset);
        dashboard.putNumber("Vision/Distance", distance);
        dashboard.putString("Vision/Data", visionData == null ? "null" : visionData.toString());
    }

    public boolean shouldUpdateShooter() {
        return shouldUpdateShooter;
    }

    public void setShouldUpdateShooter(boolean shouldUpdateShooter) {
        this.shouldUpdateShooter = shouldUpdateShooter;
    }

    public static class Config {
        public RelayConfig lightRelay;
        public Polynomial3 distancePolyFuel;
        public Polynomial3 distanceGear;
        public Double fovX;
        public double yawOffsetFuel;
        public double yawOffsetGear;
    }

    public void setVisionMode(VisionMode visionMode) {
        logger.debug("Vision to mode " + visionMode);
        this.visionMode = visionMode;
        this.setLightState(visionMode);
    }

    public void toggleMode() {
        if (this.visionMode == VisionMode.NONE) {
            setVisionMode(VisionMode.FUEL);
        } else if (this.visionMode == VisionMode.FUEL) {
            setVisionMode(VisionMode.GEAR);
        } else {
            setVisionMode(VisionMode.NONE);
        }
    }

    public void setLightState(VisionMode visionMode) {
        switch (visionMode) {
            case NONE:
                lightRelay.set(Relay.Value.kOff);
                break;
            case FUEL:
                lightRelay.set(Relay.Value.kForward);
                break;
            case GEAR:
                lightRelay.set(Relay.Value.kReverse);
                break;
        }
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup manualTests = super.createManualTests();
        manualTests.addTest(new RelayTest("lightRelay", lightRelay));
        manualTests.addTest(new VisionTest());
        return manualTests;
    }

    public void configure(Config config) {
        this.config = config;
        lightRelay = config.lightRelay.create();
    }


    public class VisionTest extends ManualTest {
        public VisionTest() {
            super("VisionTest");
        }

        @Override
        public void start() {
            super.start();
            printTestInstructions("Testing Vision subsystem. Press A to read values, B to stop vision, Y for fuel vision, X for gear vision");
        }

        @Override
        public void onButtonDown(LogitechF310.Button button) {
            switch (button) {
                case A:
                    VisionData data = getVisionData();
                    printTestInfo("Vision data: {}, last yaw: {}, last gear distance: {}," +
                                    " new robot angle: {}", data, getLastYawOffset(),
                            getLastGearDistance(), getLastRobotAngle());
                    break;
                case B:
                    setVisionMode(VisionMode.NONE);
                    break;
                case Y:
                    setVisionMode(VisionMode.FUEL);
                    break;
                case X:
                    setVisionMode(VisionMode.GEAR);
                    break;
            }
        }
    }
}
