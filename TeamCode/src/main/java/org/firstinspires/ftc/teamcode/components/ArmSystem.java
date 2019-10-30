package org.firstinspires.ftc.teamcode.components;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import  com.qualcomm.robotcore.hardware.Servo;
import java.util.EnumMap;

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
    private Servo gripper;
    private Servo wrist;
    private Servo elbow;
    private Servo pivot;
    private DcMotor slider;
    private DigitalChannel limitSwitch; // true is unpressed, false is pressed
    private final double WRIST_HOME = 0;
    private final double ELBOW_HOME = 0;
    private final double PIVOT_HOME = 0;
    private final double GRIPPER_OPEN = 0.47;
    private final double GRIPPER_CLOSE = 0;
    private int origin;
    private int targetHeight;
    private final int distanceConstant = 1000; // used for calculating motor speed

    // Use these so we can change it easily if the motor is put on backwards
    private final DcMotor.Direction UP = DcMotor.Direction.FORWARD;
    private final DcMotor.Direction DOWN = DcMotor.Direction.REVERSE;
    protected Position QueuedPosition;
    protected int QueuedHeight;

    // These fields are used only for calibration. Don't touch them outside of that method.
    private boolean calibrated = false;
    private boolean direction = true; // true is up, false is down

    // Don't change this unless in calibrate(), is read in the calculateHeight method
    private int calibrationDistance = 0;

    // This can actually be more, like 5000, but we're not going to stack that high
    // for the first comp and the servo wires aren't long enough yet
    public final int MAX_HEIGHT = 3000;
    public final int INCREMENT_HEIGHT = 564; // how much the ticks increase when a block is added
    public final int START_HEIGHT = 366;

    // I know in terms of style points these should be private and just have getters and setters but
    // I want to make them easily incrementable
    public Position queuedPosition;
    public int queuedHeight;

    public enum Position {
        POSITION_HOME, POSITION_WEST, POSITION_SOUTH, POSITION_EAST, POSITION_NORTH
    }

    public enum ServoNames {
        GRIPPER, WRIST, ELBOW, PIVOT
    }

    public static final String tag = "arm"; // for debugging

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

     Probably should be controlled by the D pad or something.
     */
    public ArmSystem(EnumMap<ServoNames, Servo> servos, DcMotor slider, DigitalChannel limitSwitch) {
        this.gripper = servos.get(ServoNames.GRIPPER);
        this.wrist = servos.get(ServoNames.WRIST);
        this.elbow = servos.get(ServoNames.ELBOW);
        this.pivot = servos.get(ServoNames.PIVOT);
        this.slider = slider;
        this.limitSwitch = limitSwitch;

    }

    // Create an ArmSystem object without servos, used for testing just the slider
    public ArmSystem(DcMotor slider, DigitalChannel limitSwitch) {
        this.slider = slider;
        this.limitSwitch = limitSwitch;
    }

    public void moveGripper(double pos) {
        gripper.setPosition(pos);
    }

    public void moveWrist(double pos) {
        wrist.setPosition(pos);
    }

    public void moveElbow(double pos) {
        elbow.setPosition(pos);
    }

    public void movePivot(double pos) {
        pivot.setPosition(pos);
    }

    public void increaseGripper(double pos) {
        if (gripper.getPosition() + pos <= 1 && gripper.getPosition() + pos >= 0) {
            gripper.setPosition(gripper.getPosition());
        }
    }

    public void increaseWrist(double pos) {
        if (wrist.getPosition() + pos <= 1 && wrist.getPosition() + pos >= 0) {
            wrist.setPosition(wrist.getPosition());
        }
    }

    public void increaseElbow(double pos) {
        if (elbow.getPosition() + pos <= 1 && elbow.getPosition() + pos >= 0) {
            elbow.setPosition(elbow.getPosition());
        }
    }

    public void increasePivot(double pos) {
        if (pivot.getPosition() + pos <= 1 && pivot.getPosition() + pos >= 0) {
            gripper.setPosition(gripper.getPosition());
        }
    }
    public double getGripper() {
        return gripper.getPosition();
    }

    public double getWrist() {
        return wrist.getPosition();
    }

    public double getElbow() {
        return elbow.getPosition();
    }

    public double getPivot() {
        return 0;
    }

    // Moves the arm to the "home state" - the grabber is open, right above the block in the intake.
    // The values of the servos in the home state can be set by editing the final variables.
    public void goHome() {
        openGripper();
        moveWrist(WRIST_HOME);
        moveElbow(ELBOW_HOME);
        movePivot(PIVOT_HOME);
    }

    public void openGripper() {
        moveGripper(GRIPPER_OPEN);
    }

    public void closeGripper() {
        moveGripper(GRIPPER_CLOSE);
    }

    public void movePresetPosition(Position pos) {
        switch(pos) {
            case POSITION_HOME:
                moveWrist(0);
                moveElbow(0.35);
                movePivot(0.86);
                break;
            case POSITION_NORTH:
                moveWrist(0.88);
                moveElbow(0.9);
                movePivot(0.1);
                break;
            case POSITION_EAST:
                moveWrist(0.55);
                moveElbow(0.9);
                movePivot(0.1);
                break;
            case POSITION_WEST:
                moveWrist(0.1);
                moveElbow(0.45);
                movePivot(0.1);
                break;
            case POSITION_SOUTH:
                moveWrist(0.55);
                moveElbow(0.45);
                movePivot(0.1);
                break;
        }
    }

    public void setQueuedPosition(Position position, int height) {
        this.QueuedPosition = position;
    }

    public void go() {
        this.movePresetPosition(queuedPosition);
    }

    /*
    Takes in the slack in the linear slide. Used to calibrate the encoder.
    Must be called every iteration of init_loop.
     */
    public void calibrate() {
        slider.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        slider.setDirection(direction? DOWN : UP);
        slider.setPower(0.1);

        if (limitSwitch.getState()) { // If the limit switch isn't pressed
            direction = false;
        }

        // If we're going down and we hit the switch, then we're done
        if (!direction && !limitSwitch.getState()) {
            slider.setPower(0);
            calibrated = true;
            calibrationDistance = slider.getCurrentPosition();
            targetHeight = calibrationDistance;
        }
    }

    public boolean isCalibrated() {
        return calibrated;
    }

    public void stop() {
        slider.setPower(0);
    }

    // Pos should be the # of blocks high it should be
    public void setSliderHeight(int pos) {
        if (calculateHeight(pos) > MAX_HEIGHT) throw new IllegalArgumentException();
        targetHeight = calculateHeight(pos);
    }

    // Little helper method for setSliderHeight
    private int calculateHeight(int pos) {
        return START_HEIGHT + pos * INCREMENT_HEIGHT + calibrationDistance;
    }

    // Should be called every loop
    public void updateHeight() {
        slider.setPower(1);
        slider.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        slider.setTargetPosition(targetHeight);
    }

    // Use for debugging
    public boolean getSwitchState() {
        return limitSwitch.getState();
    }
}
