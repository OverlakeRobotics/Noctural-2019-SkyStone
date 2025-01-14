package org.firstinspires.ftc.teamcode.opmodes.teleop;

import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.components.ArmSystem;
import org.firstinspires.ftc.teamcode.components.LatchSystem;
import org.firstinspires.ftc.teamcode.opmodes.base.BaseOpMode;

import static org.firstinspires.ftc.teamcode.components.ArmSystem.TAG;

@TeleOp(name = "Real Teleop", group="TeleOp")
public class DriveTeleop extends BaseOpMode {

    private boolean leftLatchHit = false;
    private boolean rightLatchHit = false;

    private final double SLIDER_SPEED = 1;
    private boolean gripped, down, up;
    // private boolean mPlacing;
    private boolean mCapstoning, mHoming, mQueuing;
    
    public void loop(){
        float rx = (float) Math.pow(gamepad1.right_stick_x, 3);
        float lx = (float) Math.pow(gamepad1.left_stick_x, 3);
        float ly = (float) Math.pow(gamepad1.left_stick_y, 3);
        driveSystem.slowDrive(gamepad1.left_trigger > 0.3f);
        driveSystem.drive(rx, lx, ly);


        if (gamepad1.left_bumper) {
            intakeSystem.unsuck(0.5);
        } else if (gamepad1.right_bumper) {
            intakeSystem.suck(0.5);
        } else {
            intakeSystem.stop();
        }

        if (gamepad1.b && !leftLatchHit) {
            leftLatchHit = true;
            latchSystem.toggle(LatchSystem.Latch.LEFT);
        } else if (!gamepad1.b) {
            leftLatchHit = false;
        }

        if (gamepad1.x && !rightLatchHit) {
            rightLatchHit = true;
            latchSystem.toggle(LatchSystem.Latch.RIGHT);
        } else if (!gamepad1.x) {
            rightLatchHit = false;
        }

        if (gamepad1.y) {
            latchSystem.bothUp();
        }

        if (gamepad1.a) {
            latchSystem.bothDown();
        }


        /*
        if (gamepad2.b) {
            mPlacing = !armSystem.place();
        } else if (mPlacing) {
            mPlacing = !armSystem.place();
        }
         */
        if (mHoming) {
            mHoming = !armSystem.moveToHome();
        } else if (mCapstoning) {
            mCapstoning = !armSystem.moveToCapstone();
        } else if (mQueuing) {
            mQueuing = !armSystem.moveOutToPosition(ArmSystem.Position.POSITION_WEST);
        } else if (gamepad2.x) {
            mHoming = !armSystem.moveToHome();
        } else if (gamepad2.y) {
            mCapstoning = !armSystem.moveToCapstone();
        } else if (gamepad2.back) {
            mQueuing = false;
            mCapstoning = false;
            mHoming = false;
            // mPlacing = false;
        } else if (gamepad2.right_stick_button) {
            mQueuing = !armSystem.moveOutToPosition(ArmSystem.Position.POSITION_WEST);
        }

        if (gamepad2.dpad_left) {
            armSystem.moveWest();
        } else if (gamepad2.dpad_right) {
            armSystem.moveEast();
        } else if (gamepad2.dpad_up) {
            armSystem.moveNorth();
        } else if (gamepad2.dpad_down) {
            armSystem.moveSouth();
        }

        if (gamepad2.a && !gripped) {
            armSystem.toggleGripper();
            gripped = true;
        } else if (!gamepad2.a) {
            gripped = false;
        }

        if (gamepad2.right_bumper && !up) {
            armSystem.setSliderHeight(armSystem.mTargetHeight + 1);
            up = true;
        } else if (!gamepad2.right_bumper) {
            up = false;
        }

        if (gamepad2.left_bumper && !down) {
            armSystem.setSliderHeight(armSystem.mTargetHeight - 1);
            down = true;
        } else if (!gamepad2.left_bumper) {
            down = false;
        }
        //telemetry.addData("Target height: ", armSystem);
        armSystem.runSliderToTarget();

    }
}