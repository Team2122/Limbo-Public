/*----------------------------------------------------------------------------*/
/* Copyright (c) FIRST 2008-2017. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package org.teamtators.common;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.hal.FRCNetComm.tInstances;
import edu.wpi.first.wpilibj.hal.FRCNetComm.tResourceType;
import edu.wpi.first.wpilibj.hal.HAL;
import edu.wpi.first.wpilibj.internal.HardwareHLUsageReporting;
import edu.wpi.first.wpilibj.internal.HardwareTimer;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.networktables.NetworkTable;
import edu.wpi.first.wpilibj.util.WPILibVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.limbo.TatorRobot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.Manifest;

/**
 * Implement a Robot Program framework. The Robot class is intended to be subclassed by a user
 * creating a robot program. Overridden autonomous() and operatorControl() methods are called at the
 * appropriate time as the match proceeds. In the current implementation, the Autonomous code will
 * run to completion before the OperatorControl code could start. In the future the Autonomous code
 * might be spawned as a task, then killed at the end of the Autonomous period.
 */
public class Robot {
    public static Logger logger = LoggerFactory.getLogger(Robot.class);

    protected final DriverStation m_ds;
    protected final String configDir;
    private org.teamtators.common.scheduler.RobotState robotState = null;
    private TatorRobotBase robot;

    /**
     * Constructor for a generic robot program. User code should be placed in the constructor that
     * runs before the Autonomous or Operator Control period starts. The constructor will run to
     * completion before Autonomous is entered.
     * <p>
     * <p>This must be used to ensure that the communications code starts. In the future it would be
     * nice
     * to put this code into it's own task that loads on boot so ensure that it runs.
     */
    protected Robot(String configDir) {
        // TODO: StartCAPI();
        // TODO: See if the next line is necessary
        // Resource.RestartProgram();

        NetworkTable.setNetworkIdentity("Robot");
        NetworkTable.setPersistentFilename("/home/lvuser/networktables.ini");
        NetworkTable.setServerMode();// must be before b
        m_ds = DriverStation.getInstance();
        NetworkTable.getTable(""); // forces network tables to initialize
        NetworkTable.getTable("LiveWindow").getSubTable("~STATUS~").putBoolean("LW Enabled", false);
        this.configDir = configDir;
    }

    /**
     * @return If the robot is running in simulation.
     */
    public static boolean isSimulation() {
        return false;
    }

    /**
     * @return If the robot is running in the real world.
     */
    public static boolean isReal() {
        return true;
    }

    @SuppressWarnings("JavadocMethod")
    public static boolean getBooleanProperty(String name, boolean defaultValue) {
        String propVal = System.getProperty(name);
        if (propVal == null) {
            return defaultValue;
        }
        if (propVal.equalsIgnoreCase("false")) {
            return false;
        } else if (propVal.equalsIgnoreCase("true")) {
            return true;
        } else {
            throw new IllegalStateException(propVal);
        }
    }

    /**
     * Common initialization for all robot programs.
     */
    public static void initializeHardwareConfiguration() {
        int rv = HAL.initialize(0);
        assert rv == 1;

        // Set some implementations so that the static methods work properly
        Timer.SetImplementation(new HardwareTimer());
        HLUsageReporting.SetImplementation(new HardwareHLUsageReporting());
        RobotState.SetImplementation(DriverStation.getInstance());

        // Load opencv
        /*
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError ex) {
            System.out.println("OpenCV Native Libraries could not be loaded.");
            System.out.println("Please try redeploying, or reimage your roboRIO and try again.");
            ex.printStackTrace();
        }
        */
    }

    /**
     * Starting point for the applications.
     */
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public static void main(String... args) {
        initializeHardwareConfiguration();

        HAL.report(tResourceType.kResourceType_Language, tInstances.kLanguage_Java);

        if (args.length < 1) {
            System.err.println("Config directory must be specified as first argument");
            System.exit(1);
        }

        Robot robot = new Robot(args[0]);

        try {
            final File file = new File("/tmp/frc_versions/FRC_Lib_Version.ini");

            if (file.exists()) {
                file.delete();
            }

            file.createNewFile();

            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write("Java ".getBytes());
                output.write(WPILibVersion.Version.getBytes());
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        boolean errorOnExit = false;
        try {
            System.out.println("********** Robot program starting **********");
            robot.startCompetition();
        } catch (Throwable throwable) {
            DriverStation.reportError(
                    "ERROR Unhandled exception: " + throwable.toString() + " at "
                            + Arrays.toString(throwable.getStackTrace()), false);
            errorOnExit = true;
        } finally {
            // startCompetition never returns unless exception occurs....
            System.err.println("WARNING: Robots don't quit!");
            if (errorOnExit) {
                System.err
                        .println("---> The startCompetition() method (or methods called by it) should have "
                                + "handled the exception above.");
            } else {
                System.err.println("---> Unexpected return from startCompetition() method.");
            }
        }
        System.exit(1);
    }

    /**
     * Free the resources for a Robot class.
     */
    public void free() {
    }

    /**
     * Determine if the Robot is currently disabled.
     *
     * @return True if the Robot is currently disabled by the field controls.
     */
    public boolean isDisabled() {
        return m_ds.isDisabled();
    }

    /**
     * Determine if the Robot is currently enabled.
     *
     * @return True if the Robot is currently enabled by the field controls.
     */
    public boolean isEnabled() {
        return m_ds.isEnabled();
    }

    /**
     * Determine if the robot is currently in Autonomous mode as determined by the field
     * controls.
     *
     * @return True if the robot is currently operating Autonomously.
     */
    public boolean isAutonomous() {
        return m_ds.isAutonomous();
    }

    /**
     * Determine if the robot is currently in Test mode as determined by the driver
     * station.
     *
     * @return True if the robot is currently operating in Test mode.
     */
    public boolean isTest() {
        return m_ds.isTest();
    }

    /**
     * Determine if the robot is currently in Operator Control mode as determined by the field
     * controls.
     *
     * @return True if the robot is currently operating in Tele-Op mode.
     */
    public boolean isOperatorControl() {
        return m_ds.isOperatorControl();
    }

    /**
     * Indicates if new data is available from the driver station.
     *
     * @return Has new data arrived over the network since the last time this function was called?
     */
    public boolean isNewDataAvailable() {
        return m_ds.isNewControlData();
    }

    public void startCompetition() {
        try {
            configureLogging();

            doStartCompetition();
        } catch (Throwable t) {
            logger.error("Unhandled exception thrown!", t);
            System.exit(1);
        }
    }

    private void configureLogging() {
        File logbackConfig = new File(this.configDir, "logback.xml");
        // assume SLF4J is bound to logback in the current environment
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            // Call context.reset() to clear any previous configuration, e.g. default
            // configuration. For multi-update configuration, omit calling context.reset().
            context.reset();
            configurator.doConfigure(logbackConfig);
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    private void doStartCompetition() {
        // Report that we are using java just to match what IterativeRobot does
        HAL.report(tResourceType.kResourceType_Framework,
                tInstances.kFramework_Iterative);

        // Actually initialize user robot code
        robotInit();

        // Tell the DS that the robot is ready to be enabled
        HAL.observeUserProgramStarting();

        // loop forever, calling the appropriate mode-dependent functions
        LiveWindow.setEnabled(false);

        //noinspection InfiniteLoopStatement
        while (true) {
            // Update the state if it has changed
            if (isDisabled()) {
                if (robotState != org.teamtators.common.scheduler.RobotState.DISABLED) {
                    robotState = org.teamtators.common.scheduler.RobotState.DISABLED;
                    robot.onEnterRobotState(robotState);
                }
            } else if (isTest()) {
                if (robotState != org.teamtators.common.scheduler.RobotState.TEST) {
                    robotState = org.teamtators.common.scheduler.RobotState.TEST;
                    robot.onEnterRobotState(org.teamtators.common.scheduler.RobotState.TEST);
                }
            } else if (isAutonomous()) {
                if (robotState != org.teamtators.common.scheduler.RobotState.AUTONOMOUS) {
                    robotState = org.teamtators.common.scheduler.RobotState.AUTONOMOUS;
                    robot.onEnterRobotState(org.teamtators.common.scheduler.RobotState.AUTONOMOUS);
                }
            } else {
                if (robotState != org.teamtators.common.scheduler.RobotState.TELEOP) {
                    robotState = org.teamtators.common.scheduler.RobotState.TELEOP;
                    robot.onEnterRobotState(org.teamtators.common.scheduler.RobotState.TELEOP);
                }
            }
            // If we have new control data, update stuff also
            if (isNewDataAvailable()) {
                switch (robotState) {
                    case DISABLED:
                        HAL.observeUserProgramDisabled();
                        break;
                    case AUTONOMOUS:
                        HAL.observeUserProgramAutonomous();
                        break;
                    case TELEOP:
                        HAL.observeUserProgramTeleop();
                        break;
                    case TEST:
                        HAL.observeUserProgramTest();
                        break;
                }
                robot.onDriverStationData();
            }
            // Enable LiveWindow if in test mode
//            LiveWindow.setEnabled(robotState == RobotState.TEST);
            // Wait for new data from the driver station. Should be at a ~20ms period
            m_ds.waitForData();
        }
    }

    private void initialize() throws IOException {
        logger.info("Robot initializing with config directory " + this.configDir);

        String robotName = "";
        Enumeration<URL> resources = RobotBase.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
        while (resources != null && resources.hasMoreElements()) {
            try {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                robotName = manifest.getMainAttributes().getValue("Robot-Class");
                break;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        try {
            /*robot = (TatorRobotBase) Class.forName(robotName)
                    .getConstructor(String.class)
                    .newInstance(this.configDir);*/
            //TEMP FIX
            robot = new TatorRobot(this.configDir);
        } catch (Throwable throwable) {
            DriverStation.reportError("ERROR Unhandled exception instantiating robot " + robotName + " "
                    + throwable.toString() + " at " + Arrays.toString(throwable.getStackTrace()), false);
            System.err.println("ERROR: Could not instantiate robot " + robotName + "!");
            System.err.println("Does the class exist, and does it have a public constructor which takes the name of the" +
                    "config directory?");
            System.exit(1);
            return;
        }

        robot.initialize();
    }

    public void robotInit() {
        try {
            initialize();
        } catch (Throwable t) {
            logger.error("Exception during robot init", t);
            System.exit(1);
        }
    }
}
