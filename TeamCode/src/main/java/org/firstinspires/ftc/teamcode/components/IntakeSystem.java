package org.firstinspires.ftc.teamcode.components;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import java.util.EnumMap;

public class IntakeSystem {

    public enum MotorNames {
        RIGHT_INTAKE, LEFT_INTAKE, BOTTOM_INTAKE
    }

    public enum SuckDirection {
        SUCK, UNSUCK
    }
    private EnumMap<IntakeSystem.MotorNames, DcMotor> motors;

    public IntakeSystem(EnumMap<IntakeSystem.MotorNames, DcMotor> motors) {
        this.motors = motors;
        initMotors();
    }

    public void spin(boolean leftBumper, boolean rightBumper) {
        if (leftBumper) {
            setMotorsPower(1.0);
        } else if (rightBumper) {
            setMotorsPower(-1.0);
        } else {
            setMotorsPower(0.0);
        }
    }
    public void spin(SuckDirection direction) {
        if (direction == SuckDirection.SUCK) {
            spin(true, false);
        } else {
            spin(false, true);
        }
    }

    public void stop() {
        spin(false, false);
    }

    private void initMotors() {
        motors.forEach((name, motor) -> {
            motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            if (name == MotorNames.RIGHT_INTAKE) {
                motor.setDirection(DcMotorSimple.Direction.REVERSE);
            } else {
                motor.setDirection(DcMotorSimple.Direction.FORWARD);
            }
            motor.setPower(0.0);
        });
    }

    private void setMotorsPower(double power) {
        for (DcMotor motor : motors.values()) {
            motor.setPower(power);
        }
    }
}
