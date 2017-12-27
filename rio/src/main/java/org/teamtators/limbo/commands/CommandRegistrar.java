package org.teamtators.limbo.commands;

import org.teamtators.common.commands.WaitCommand;
import org.teamtators.common.config.ConfigCommandStore;
import org.teamtators.common.scheduler.Commands;
import org.teamtators.limbo.TatorRobot;
import org.teamtators.limbo.subsystems.Drive;
import org.teamtators.limbo.subsystems.Gear;
import org.teamtators.limbo.subsystems.Shooter;
import org.teamtators.limbo.subsystems.Vision;

public class CommandRegistrar {
    private final TatorRobot robot;

    public CommandRegistrar(TatorRobot robot) {
        this.robot = robot;
    }

    public void register(ConfigCommandStore commandStore) {
        commandStore.putCommand("NoAuto", Commands.instant(() -> TatorRobot.logger.info("No auto selected")));

        // Drive
        Drive drive = robot.getSubsystems().getDrive();
        commandStore.registerCommand("Drive.Tank", () -> new DriveTank(robot));
        commandStore.registerCommand("Drive.FieldCentric", () -> new DriveFieldCentric(robot));
        commandStore.registerCommand("Drive.Shift", () -> new DriveShift(robot));
        commandStore.registerCommand("Drive.Straight", () -> new DriveStraight(robot));
        commandStore.registerCommand("Drive.Rotate", () -> new DriveRotate(robot));
        commandStore.registerCommand("Drive.Arc", () -> new DriveArc(robot));
        commandStore.putCommand("Drive.ResetYaw", Commands.instant(drive::resetYawAngle));

        // Climber
        commandStore.registerCommand("Climber.Force", () -> new ClimberForce(robot));
        commandStore.registerCommand("Climber.Climb", () -> new ClimberClimb(robot));

        // Gear
        Gear gear = robot.getSubsystems().getGear();
        commandStore.putCommand("Gear.Continuous", new GearContinuous(robot));
        commandStore.putCommand("Gear.Idle", Commands.instant(gear::requestIdle));
        commandStore.putCommand("Gear.Home", Commands.instant(gear::requestHome));
        commandStore.putCommand("Gear.Release", Commands.instant(gear::requestReleased));
        commandStore.putCommand("Gear.Pick", Commands.instant(gear::requestPick));
        commandStore.putCommand("Gear.TogglePick", Commands.instant(gear::requestTogglePick));
        commandStore.registerCommand("GearAutoUnload", () -> new GearAutoUnload(robot));

        // Shooter
        Shooter shooter = robot.getSubsystems().getShooter();
        commandStore.putCommand("Shooter.Idle", Commands.instant(() -> shooter.setState(Shooter.State.IDLE)));
        commandStore.putCommand("Shooter.Shoot", Commands.instant(() -> shooter.setState(Shooter.State.SHOOTING)));
        commandStore.putCommand("Shooter.SpinUp", Commands.instant(() -> shooter.setState(Shooter.State.SPINNING_UP)));
        commandStore.putCommand("Shooter.Load", Commands.instant(() -> shooter.setState(Shooter.State.LOAD)));
        commandStore.registerCommand("Shooter.SetDesiredSpeed", () -> new ShooterSetDesiredSpeed(robot));

        Vision vision = robot.getSubsystems().getVision();
        commandStore.putCommand("Vision.ToggleMode", new VisionToggleMode(vision));


        // Other commands
        commandStore.registerCommand("RotateToBoiler", () -> new RotateToBoiler(robot));
        commandStore.registerCommand("Chooser", () -> new ChooserCommand(robot));
        commandStore.registerCommand("SelectColor", () -> new SelectColorCommand(robot));
        commandStore.registerCommand("Wait", () -> new WaitCommand(robot));
    }
}
