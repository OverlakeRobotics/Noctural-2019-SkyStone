package org.firstinspires.ftc.teamcode.components;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import  com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.internal.system.Deadline;

import java.util.EnumMap;
import java.util.concurrent.TimeUnit;

/*
    This class controls everything related to the arm, including driver assist features.

    IMPORTANT: When working on this class (and arm stuff in general),
    keep the servo names consistent: (from closest to the block to farthest)
        - Gripper
        - Wrist
        - Elbow
        - Pivot
 */
public class ArmSystem {
    public enum Position {
        // Double values ordered Pivot, elbow, wrist.
        POSITION_HOME(new double[] {0.96, 0.15, 0.79}),
        POSITION_WEST(new double[] {0.16, 0.22, 0.72}),
        POSITION_SOUTH(new double[] {0.16, 0.22, 0.37}),
        POSITION_EAST(new double[] {0.16, 0.58, 0.37}),
        POSITION_NORTH(new double[] {0.16, 0.58, 0.05}),
        POSITION_CAPSTONE(new double[] {0.56, 0.23, 0.82});

        private double[] posArr;

        Position(double[] positions) {
            posArr = positions;
        }

        private double[] getPos() {
            return this.posArr;
        }
    }

    public enum ServoNames {
        GRIPPER, WRIST, ELBOW, PIVOT
    }
    public enum ArmState {
        STATE_INITIAL,
        STATE_CLEAR_CHASSIS,
        STATE_CHANGE_POSITION,
        STATE_SETTLE,
    }

    private ArmState mCurrentState;

    // Don't change this unless in calibrate() or init(), is read in the calculateHeight method
    public int mCalibrationDistance;

    private EnumMap<ServoNames, Servo> servoEnumMap;
    private DcMotor slider;

    // This is in block positions, not ticks
    public double mTargetHeight;
    // These two variables are used for all the auto methods.
    private Deadline mWaiting;

    // This can actually be more, like 5000, but we're not going to stack that high
    // for the first comp and the servo wires aren't long enough yet
    private final int MAX_HEIGHT = calculateHeight(9);
    private final int INCREMENT_HEIGHT = 550; // how much the ticks increase when a block is added
    private final double GRIPPER_OPEN = 0.9;
    private final double GRIPPER_CLOSE = 0.3;
    private final int WAIT_TIME = 1000;

    public static final String TAG = "ArmSystem"; // for debugging

    /*
     If the robot is at the bottom of the screen, and X is the block:

     XO
     XO  <--- Position west

     OO
     XX  <--- Position south

     OX
     OX  <--- Position east

     XX
     OO  <--- Position north
     */
    public ArmSystem(EnumMap<ServoNames, Servo> servos, DcMotor slider) {
        servoEnumMap = servos;
        this.slider = slider;
        this.mCalibrationDistance = slider.getCurrentPosition();
        this.slider.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        mWaiting = new Deadline(WAIT_TIME, TimeUnit.MILLISECONDS);
        movePresetPosition(Position.POSITION_HOME);
        openGripper();
    }

    // Go to "west" position
    public void moveWest() {
        movePresetPosition(Position.POSITION_WEST);
    }

    // Go to "north" position
    public void moveNorth () {
        movePresetPosition(Position.POSITION_NORTH);
    }

    public void moveEast() {
        movePresetPosition(Position.POSITION_EAST);
    }

    public void moveSouth() {
        movePresetPosition(Position.POSITION_SOUTH);
    }

    // Go to capstone position
    public boolean moveToCapstone() {
        return moveToPlace(Position.POSITION_CAPSTONE);
    }

    private boolean moveToPlace(Position position) {
        switch(mCurrentState){
            case STATE_INITIAL:
                if (getSliderPos() < calculateHeight(2)) {
                    setSliderHeight(2);
                    mCurrentState = ArmState.STATE_CLEAR_CHASSIS;
                } else {
                    mCurrentState = ArmState.STATE_CHANGE_POSITION;
                }
                break;
            case STATE_CLEAR_CHASSIS:
                if(runSliderToTarget(1)){
                    mCurrentState = ArmState.STATE_CHANGE_POSITION;
                    mWaiting.reset();
                }
                break;
            case STATE_CHANGE_POSITION:
                movePresetPosition(position);
                openGripper();
                if(mWaiting.hasExpired()){
                    setSliderHeight(position == Position.POSITION_CAPSTONE ? 0.5 : 0);
                    mCurrentState = ArmState.STATE_SETTLE;
                }
                break;
            case STATE_SETTLE:
                if (runSliderToTarget(1)) {
                    mCurrentState = ArmState.STATE_INITIAL;
                    return true;
                }
                break;
        }
        return false;
    }

    // Go to the home position
    // Moves the slider up to one block high, moves the gripper to the home position, and then moves
    // back down so we can fit under the bridge.
    public boolean moveToHome() {
        return moveToPlace(Position.POSITION_HOME);
    }

    public void openGripper() {
        servoEnumMap.get(ServoNames.GRIPPER).setPosition(GRIPPER_OPEN);
    }

    public void closeGripper() {
        servoEnumMap.get(ServoNames.GRIPPER).setPosition(GRIPPER_CLOSE);
    }

    public void toggleGripper() {
        double gripPos = servoEnumMap.get(ServoNames.GRIPPER).getPosition();
        if (Math.abs(gripPos - GRIPPER_CLOSE) < Math.abs(gripPos - GRIPPER_OPEN)) {
            // If we're in here, the gripper is closer to its closed position
            openGripper();
        } else {
            closeGripper();
        }
    }

    private void placeStone() {
        openGripper();
        setSliderHeight(getSliderPos() + 1);
        movePresetPosition(Position.POSITION_HOME);
        moveToHome();
    }

    private void movePresetPosition(Position pos){
        double[] posArray = pos.getPos();
        servoEnumMap.get(ServoNames.PIVOT).setPosition(posArray[0]);
        servoEnumMap.get(ServoNames.ELBOW).setPosition(posArray[1]);
        servoEnumMap.get(ServoNames.WRIST).setPosition(posArray[2]);
    }

    // Pos should be the # of blocks high it should be
    public void setSliderHeight(double pos) {
        if (pos < 0) {
            mTargetHeight = 0;
        } else if (pos > 5) {
            mTargetHeight = 5;
        } else {
            mTargetHeight = pos;
        }
        setPosTarget();
        slider.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        runSliderToTarget(1);
    }

    public void setSliderHeight(int pos) {
        // * 1.0 converts to double
        setSliderHeight(pos * 1.0);
    }

    // Little helper method for setSliderHeight
    private int calculateHeight(double pos){
        return (int) (pos == 0 ? mCalibrationDistance - 20 : mCalibrationDistance + (pos * INCREMENT_HEIGHT));
    }

    // Must be called every loop
    public boolean runSliderToTarget(double speed){
        int targetHeight = calculateHeight(mTargetHeight);
        if (slider.getCurrentPosition() != targetHeight) {
            slider.setPower(speed);
            setPosTarget();
            return false;
        } else {
            return true;
        }
    }

    public int getSliderPos() {
        return slider.getCurrentPosition();
    }

    private void setPosTarget() {
        slider.setTargetPosition(calculateHeight(mTargetHeight));
    }
}