package org.firstinspires.ftc.teamcode.opmodes.base;

import android.util.Log;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.ColorSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.VuforiaTrackable;
import org.firstinspires.ftc.teamcode.components.ArmSystem;
import org.firstinspires.ftc.teamcode.components.DriveSystem;
import org.firstinspires.ftc.teamcode.components.IMUSystem;
import org.firstinspires.ftc.teamcode.components.IntakeSystem;
import org.firstinspires.ftc.teamcode.components.LatchSystem;
import org.firstinspires.ftc.teamcode.components.LightSystem;
import org.firstinspires.ftc.teamcode.components.Vuforia;
import org.firstinspires.ftc.teamcode.components.Vuforia.CameraChoice;

import java.util.EnumMap;

public abstract class BaseOpMode extends OpMode {

    protected DriveSystem driveSystem;
    protected LatchSystem latchSystem;
    protected IntakeSystem intakeSystem;
    protected LightSystem lightSystem;
    protected Vuforia vuforia;
    protected VuforiaTrackable skystone;
    protected VuforiaTrackable rearPerimeter;
    protected ArmSystem armSystem;
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
        lightSystem.off();

        EnumMap<IntakeSystem.MotorNames, DcMotor> intakeMap = new EnumMap<>(IntakeSystem.MotorNames.class);
        for(IntakeSystem.MotorNames name : IntakeSystem.MotorNames.values()){
            intakeMap.put(name,hardwareMap.get(DcMotor.class, name.toString()));
        }
        intakeSystem = new IntakeSystem(intakeMap, hardwareMap.get(Servo.class, "BOTTOM_INTAKE"));

        EnumMap<ArmSystem.ServoNames, Servo> servoEnumMap = new EnumMap<>(ArmSystem.ServoNames.class);

        for (ArmSystem.ServoNames name : ArmSystem.ServoNames.values()) {
            servoEnumMap.put(name, hardwareMap.get(Servo.class, name.toString()));
        }
        DcMotor slider = hardwareMap.get(DcMotor.class, "SLIDER_MOTOR");
        slider.setDirection(DcMotorSimple.Direction.REVERSE);
        armSystem = new ArmSystem(servoEnumMap, hardwareMap.get(DcMotor.class, "SLIDER_MOTOR"));

    }

    protected void setCamera(CameraChoice cameraChoice){

        vuforia = new Vuforia(hardwareMap, cameraChoice);
        skystone = vuforia.targetsSkyStone.get(0);


    }

    public final boolean isStopRequested() {
        return this.stopRequested || Thread.currentThread().isInterrupted();
    }

    @Override
    public void stop() {
        stopRequested = true;
        super.stop();
    }
}
