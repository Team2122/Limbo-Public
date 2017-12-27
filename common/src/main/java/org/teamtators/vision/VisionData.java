package org.teamtators.vision;

public class VisionData {

    public VisionData() {
    }

    public VisionData(RobotData robotData, Double x, Double y, Double area, Double width, Double height, Double separation) {
        this.robotData = robotData;
        this.x = x;
        this.y = y;
        this.area = area;
        this.width = width;
        this.height = height;
        this.separation = separation;
    }

    /**
     * The robot data used to process this data
     */
    public RobotData robotData = new RobotData();

    /**
     * The horizontal position of the centroid of the targets, from -1 to 1. Where -1 is the left side of the camera
     * view and 1 is the right side. Or null if no target is found
     */
    public Double x = null;

    /**
     * The vertical position of the centroid of the targets, from -1 to 1. Where -1 is the top of the camera view and
     * 1 is the bottom. Or null if no target is found
     */
    public Double y = null;

    /**
     * The combined total area of the targets or null if no target is found
     */
    public Double area = null;

    /**
     * The total width of the targets, in a % of the width of the screen (from 0 to 1)
     */
    public Double width = null;

    /**
     * The total height of the targets, in a % of the height of the screen (from 0 to 1)
     */
    public Double height = null;

    public Double separation = null;

    @Override
    public String toString() {
        return "VisionData{" +
                "robotData=" + robotData +
                ", x=" + x +
                ", y=" + y +
                ", area=" + area +
                ", width=" + width +
                ", height=" + height +
                ", separation=" + separation +
                '}';
    }
}
