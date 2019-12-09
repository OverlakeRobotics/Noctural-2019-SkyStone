package org.firstinspires.ftc.teamcode.opmodes.base;

import java.util.EnumMap;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.teamcode.components.ArmSystem;
import org.firstinspires.ftc.teamcode.components.DriveSystem;
import org.firstinspires.ftc.teamcode.components.IntakeSystem;
import org.firstinspires.ftc.teamcode.components.LatchSystem;
import org.firstinspires.ftc.teamcode.components.LightSystem;

public abstract class BaseOpMode extends OpMode {

    protected DriveSystem driveSystem;
    protected LatchSystem latchSystem;
    protected IntakeSystem intakeSystem;
    protected ArmSystem armSystem;
    protected LightSystem lightSystem;
    private boolean stopRequested;

    public void init(){
        stopRequested = false;
        this.msStuckDetectInit = 20000;
        this.msStuckDetectInitLoop = 20000;
        EnumMap<DriveSystem.MotorNames, DcMotor> driveMap = new EnumMap<>(DriveSystem.MotorNames.class);
        for(DriveSystem.MotorNames name : DriveSystem.MotorNames.values()){
            driveMap.put(name,hardwareMap.get(DcMotor.class, name.toString()));
        }
        driveSystem = new DriveSystem(driveMap);

        EnumMap<LatchSystem.Latch, Servo> latchMap = new EnumMap<>(LatchSystem.Latch.class);
        for(LatchSystem.Latch name : LatchSystem.Latch.values()){
            latchMap.put(name,hardwareMap.get(Servo.class, name.toString()));
        }
        latchSystem = new LatchSystem(latchMap);

        lightSystem = new LightSystem(hardwareMap.get(DigitalChannel.class, "right_light"), hardwareMap.get(DigitalChannel.class, "left_light"));

        EnumMap<IntakeSystem.MotorNames, DcMotor> intakeMap = new EnumMap<>(IntakeSystem.MotorNames.class);
        for(IntakeSystem.MotorNames name : IntakeSystem.MotorNames.values()){
            intakeMap.put(name,hardwareMap.get(DcMotor.class, name.toString()));
        }
        intakeSystem = new IntakeSystem(intakeMap, hardwareMap.get(Servo.class, "BOTTOM_INTAKE"));

        EnumMap<ArmSystem.ServoNames, Servo> servoEnumMap = new EnumMap<ArmSystem.ServoNames, Servo>(ArmSystem.ServoNames.class);
        servoEnumMap.put(ArmSystem.ServoNames.GRIPPER, hardwareMap.get(Servo.class, "GRIPPER"));
        servoEnumMap.put(ArmSystem.ServoNames.ELBOW, hardwareMap.get(Servo.class, "ELBOW"));
        servoEnumMap.put(ArmSystem.ServoNames.WRIST, hardwareMap.get(Servo.class, "WRIST"));
        servoEnumMap.put(ArmSystem.ServoNames.PIVOT, hardwareMap.get(Servo.class, "PIVOT"));
        armSystem = new ArmSystem(
                servoEnumMap,
                hardwareMap.get(DcMotor.class, "SLIDER_MOTOR"),
                hardwareMap.get(DigitalChannel.class, "SLIDER_SWITCH"));

    }
}
