package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.components.DriveSystem;

public abstract class WaitAuto extends BaseAutonomous {
    private boolean waited;
    private boolean arrived;
    private ElapsedTime time;

    // In seconds
    public static final int WAIT_TIME = 15;

    public void init(Team team) {
        super.init(team);
        waited = false;
        arrived = false;
        time = new ElapsedTime();
    }

    @Override
    public void loop() {
        if (!(time.milliseconds() > WAIT_TIME * 1000)) {
            return;
        }
        if (!arrived) {
            if (driveSystem.driveToPosition(400, DriveSystem.Direction.FORWARD, 1.0)) {
                arrived = true;
                return;
            }
        }
    }

}
