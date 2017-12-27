package org.teamtators.limbo.subsystems;

import edu.wpi.first.wpilibj.PowerDistributionPanel;
import edu.wpi.first.wpilibj.SpeedController;
import org.teamtators.common.config.SpeedControllerConfig;
import org.teamtators.common.datalogging.Dashboard;
import org.teamtators.common.datalogging.DashboardUpdatable;
import org.teamtators.common.scheduler.Subsystem;
import org.teamtators.common.tester.ManualTestGroup;
import org.teamtators.common.tester.components.SpeedControllerTest;
import org.teamtators.limbo.TatorRobot;

/**
 * Climbs up the rope and has a motor that moves it up and does current monitoring to see when to stop
 */
public class Climber extends Subsystem implements DashboardUpdatable {
    private SpeedController climberMotor;
    private PowerDistributionPanel pdp;
    private int powerChannel;
    private SpeedControllerConfig climberMotorConfig;


    public Climber(TatorRobot robot) {
        super("Climber");
        pdp = robot.getPDP();
        robot.addSmartDashboardUpdatable(this);
    }

    /**
     * @return the current of the climber motor in amps
     */
    public double getCurrent() {
        return climberMotorConfig.getTotalCurrent(pdp);
    }

    /**
     * @param power the power that the climbermotor needs to be set to
     */
    public void setPower(double power) {
        climberMotor.set(power);
    }

    public void configure(Config config) {
        climberMotor = config.climberMotor.create();
        climberMotorConfig = config.climberMotor;
    }

    @Override
    public ManualTestGroup createManualTests() {
        ManualTestGroup manualTestGroup = super.createManualTests();
        manualTestGroup.addTest(new SpeedControllerTest("climberMotor", climberMotor, pdp, climberMotorConfig));
        return manualTestGroup;
    }

    @Override
    public void updateDashboard(Dashboard dashboard) {
        dashboard.putNumber("Climber/Power", climberMotor.get());
        dashboard.putNumber("Climber/Current", getCurrent());
    }

    public static class Config {
        public SpeedControllerConfig climberMotor;
    }
}
