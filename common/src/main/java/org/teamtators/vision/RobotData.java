package org.teamtators.vision;

/**
 * @author Alex Mikhalev
 */
public class RobotData {
    /**
     * The relative timestamp on the robot at the time this data was taken
     *
     * @see org.teamtators.common.control.Timer#getTimestamp()
     */
    public double time = 0.0;

    /**
     * The current vision mode
     */
    public VisionMode visionMode = VisionMode.FUEL;

    /**
     * The current angle of the gyro
     */
    public double gyroAngle = Double.NaN;

    public double driveDistance = Double.NaN;

    @Override
    public String toString() {
        return "RobotData{" +
                "time=" + time +
                ", visionMode=" + visionMode +
                ", gyroAngle=" + gyroAngle +
                '}';
    }
}
