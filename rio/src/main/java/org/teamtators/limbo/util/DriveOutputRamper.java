package org.teamtators.limbo.util;

import org.teamtators.common.config.Configurable;
import org.teamtators.common.control.Updatable;
import org.teamtators.common.util.Ramper;
import org.teamtators.limbo.subsystems.Drive;

public class DriveOutputRamper implements Updatable, Configurable<Ramper.Config> {
    private Ramper left = new Ramper();
    private Ramper right = new Ramper();

    @Override
    public void configure(Ramper.Config config) {
        left.configure(config);
        right.configure(config);
    }

    public void setInput(Drive.Output input) {
        left.setValue(input.leftPower);
        right.setValue(input.rightPower);
    }

    public Drive.Output getOutput() {
        return new Drive.Output(left.getOutput(), right.getOutput());
    }

    @Override
    public void update(double delta) {
        left.update(delta);
        right.update(delta);
    }

    public Drive.Output update(double delta, Drive.Output input) {
        setInput(input);
        update(delta);
        return getOutput();
    }
}
