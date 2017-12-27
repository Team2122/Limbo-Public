package org.teamtators.vision;

/**
 * @author Alex Mikhalev
 */
public class MqttTopics {
    public static final String VISION = "TatorVision";
    public static final String VISION_CONNECTED = VISION + "/connected";
    public static final String VISION_DATA = VISION + "/visionData";

    public static final String ROBOT = "Robot";
    public static final String ROBOT_DATA = ROBOT + "/robotData";
    public static final String ROBOT_CONNECTED = ROBOT + "/connected";
    public static final String ROBOT_DASHBOARD_DATA = ROBOT + "/robotDashboardData";

    public static final String DASHBOARD = "TatorDashboard";
    public static final String DASHBOARD_CHOOSERS = DASHBOARD + "/options";
    public static final String DASHBOARD_CHOICES = DASHBOARD + "/choices";
}
