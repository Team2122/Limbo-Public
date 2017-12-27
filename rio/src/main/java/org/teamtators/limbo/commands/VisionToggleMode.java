package org.teamtators.limbo.commands;

import org.teamtators.common.scheduler.Command;
import org.teamtators.common.scheduler.RobotState;
import org.teamtators.limbo.subsystems.Vision;

public class VisionToggleMode extends Command {
    private Vision vision;
    public VisionToggleMode(Vision vision) {
        super("VisionToggleMode");
        validIn(RobotState.DISABLED);
        this.vision = vision;
    }

    @Override
    protected boolean step() {
        vision.toggleMode();
        return true;
    }
}
