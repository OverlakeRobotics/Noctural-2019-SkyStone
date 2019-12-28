package org.firstinspires.ftc.teamcode.components;

import com.qualcomm.robotcore.hardware.DcMotor;
import  com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

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
        POSITION_HOME(new double[] {0.96, 0.15, 0.79}, 0),
        POSITION_WEST(new double[] {0.16, 0.22, 0.72}),
        POSITION_SOUTH(new double[] {0.16, 0.22, 0.37}),
        POSITION_EAST(new double[] {0.16, 0.58, 0.37}),
        POSITION_NORTH(new double[] {0.16, 0.58, 0.05}),
        POSITION_CAPSTONE(new double[] {0.56, 0.23, 0.82}, 0.5);

        private double[] posArr;
        private double height;

        Position(double[] positions, double height) {
            posArr = positions;
            this.height = height;
        }

        Position(double[] positions) {
            posArr = positions;
        }

        private double[] getPos() {
            return this.posArr;
        }
        private double getHeight() {return this.height; }
    }

    public enum ServoNames {
        GRIPPER, WRIST, ELBOW, PIVOT
    }
    public enum ArmState {
        STATE_CHECK_CLEARANCE,
        STATE_CLEAR_CHASSIS,
        STATE_ADJUST_ORIENTATION,
        STATE_SETTLE,
        STATE_RAISE,
        STATE_LOWER_HEIGHT,
        STATE_DROP,
        STATE_WAITING,
        STATE_OPEN,
        STATE_CLEAR_TOWER,
        STATE_HOME,
    }

    private ArmState mCurrentState;

    // Don't change this unless in calibrate() or init(), is read in the calculateHeight method
    private int mCalibrationDistance;

    private EnumMap<ServoNames, Servo> servoEnumMap;
    private DcMotor slider;

    // This is in block positions, not ticks
    public double mTargetHeight;
    // The queued position
    private double mQueuePos;
    // This variable is used for all the auto methods.
    private Deadline mWaiting;
    private boolean mQueuing;
    private boolean mHoming;
    private boolean mCapstoning;
    private boolean mPlacing;

    private final int MAX_HEIGHT = 7;
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
        mCurrentState = ArmState.STATE_CHECK_CLEARANCE;
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
        return moveInToPosition(Position.POSITION_CAPSTONE);
    }

    // Helper method for going to capstone or home
    private boolean moveInToPosition(Position position) {
        switch(mCurrentState) {
            case STATE_CHECK_CLEARANCE:
                boolean setState = (position == Position.POSITION_HOME) ? (mHoming = true) : (mCapstoning = true);
                checkIfOver();
                break;
            case STATE_CLEAR_CHASSIS:
                raise(position);
                break;
            case STATE_ADJUST_ORIENTATION:
                if(mWaiting.hasExpired()) {
                    openGripper();
                    setSliderHeight(position.getHeight());
                    mCurrentState = ArmState.STATE_SETTLE;
                }
                break;
            case STATE_SETTLE:
                if (runSliderToTarget()) {
                    mCurrentState = ArmState.STATE_CHECK_CLEARANCE;
                    boolean changeState = (position == Position.POSITION_HOME) ? (mHoming = false) : (mCapstoning = false);
                    return true;
                }
                break;
        }
        return false;
    }

    // Helper method for going out to the queued position
    private boolean moveOutToPosition(Position position) {
        switch(mCurrentState) {
            case STATE_CHECK_CLEARANCE:
                mQueuing = true;
                checkIfOver();
                break;
            case STATE_CLEAR_CHASSIS:
                if (runSliderToTarget()) {
                    setSliderHeight(mQueuePos);
                    mCurrentState = ArmState.STATE_RAISE;
                }
                break;
            case STATE_ADJUST_ORIENTATION:
                if(mWaiting.hasExpired()) {
                    mCurrentState = ArmState.STATE_CHECK_CLEARANCE;
                    incrementQueue();
                    mQueuing = false;
                    return true;
                }
                break;
            case STATE_RAISE:
                raise(position);
                break;
        }
        return false;
    }

    private void checkIfOver() {
        if (getSliderPos() < calculateHeight(2)) {
            setSliderHeight(2);
            mCurrentState = ArmState.STATE_CLEAR_CHASSIS;
        } else {
            mCurrentState = ArmState.STATE_ADJUST_ORIENTATION;
        }
    }

    private void raise(Position position) {
        if (runSliderToTarget()) {
            movePresetPosition(position);
            mCurrentState = ArmState.STATE_ADJUST_ORIENTATION;
            mWaiting.reset();
        }
    }

    // Go to the home position
    // Moves the slider up to one block high, moves the gripper to the home position, and then moves
    // back down so we can fit under the bridge.
    public boolean moveToHome() {
        return moveInToPosition(Position.POSITION_HOME);
    }

    public void openGripper() {
        servoEnumMap.get(ServoNames.GRIPPER).setPosition(GRIPPER_OPEN);
    }

    public void closeGripper() {
        servoEnumMap.get(ServoNames.GRIPPER).setPosition(GRIPPER_CLOSE);
    }

    public void toggleGripper() {
        if (servoEnumMap.get(ServoNames.GRIPPER).getPosition() == GRIPPER_CLOSE) {
            openGripper();
        } else {
            closeGripper();
        }
    }

    private void movePresetPosition(Position pos){
        double[] posArray = pos.getPos();
        servoEnumMap.get(ServoNames.PIVOT).setPosition(posArray[0]);
        servoEnumMap.get(ServoNames.ELBOW).setPosition(posArray[1]);
        servoEnumMap.get(ServoNames.WRIST).setPosition(posArray[2]);
    }

    // Pos should be the # of blocks high it should be
    // MUST BE CALLED before runSliderToTarget
    public void setSliderHeight(double pos) {
        mTargetHeight = Range.clip(pos, 0, MAX_HEIGHT);
        setPosTarget();
        slider.setMode(DcMotor.RunMode.RUN_TO_POSITION);
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
    public boolean runSliderToTarget(){
        if (slider.isBusy()) {
            return slider.getCurrentPosition() == slider.getTargetPosition();
        } else {
            slider.setPower(1.0);
            return false;
        }
    }

    public int getSliderPos() {
        return slider.getCurrentPosition();
    }

    private void setPosTarget() {
        slider.setTargetPosition(calculateHeight(mTargetHeight));
    }

    public boolean runToQueueHeight() {
        setSliderHeight(mQueuePos);
        boolean complete = runSliderToTarget();
        if (complete) {
            incrementQueue();
        }
        return complete;
    }

    public void resetQueue() {
        mQueuePos = 0;
    }

    public void incrementQueue() {
        mQueuePos++;
        if (mQueuePos > MAX_HEIGHT) {
            resetQueue();
        }
    }

    public void decrementQueue() {
        mQueuePos = Math.max(0, mQueuePos - 1);
    }

    public double getQueue() {
        return mQueuePos;
    }

    public boolean isHoming() {
        return mHoming;
    }
    public boolean isQueuing() {
        return mQueuing;
    }
    public boolean isCapstoning() {
        return mCapstoning;
    }
    public boolean isPlacing() {
        return mPlacing;
    }

    public void setHoming(boolean isHoming) {
        mHoming = isHoming;
    }
    public void setQueuing(boolean isQueuing) {
        mQueuing = isQueuing;
    }
    public void setCapstoning(boolean isCapstoning) {
        mCapstoning = isCapstoning;
    }
    public void setPlacing(boolean isPlacing) {
        mPlacing = isPlacing;
    }


    public boolean place() {
        switch(mCurrentState) {
            // Drops the block
            case STATE_LOWER_HEIGHT:
                mPlacing = true;
                setSliderHeight(getSliderPos() - 0.5);
                mCurrentState = ArmState.STATE_DROP;
            case STATE_DROP:
                if (runSliderToTarget()) {
                    mCurrentState = ArmState.STATE_WAITING;
                }
            case STATE_OPEN:
                openGripper();
                setSliderHeight(getSliderPos() + 0.5);
                mCurrentState = ArmState.STATE_CLEAR_TOWER;
                break;
            // Raises up a half-block
            case STATE_CLEAR_TOWER:
                if (runSliderToTarget()) {
                    mCurrentState = ArmState.STATE_LOWER_HEIGHT;
                    mPlacing = false;
                    return true;
                }
                break;
        }
        return false;
    }

    public boolean awaitingConfirmation() {
        return mCurrentState == ArmState.STATE_WAITING;
    }

    public void changePlaceState(ArmState state) {
        mCurrentState = state;
    }

    public void cancelAutoRoutine() {
        mCapstoning = false;
        mHoming = false;
        mPlacing = false;
        mQueuing = false;
    }

}