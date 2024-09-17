package org.firstinspires.ftc.teamcode.opmodes.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

@Autonomous(name = "WaitBlue", group="Autonomous")

public class WaitBlue extends WaitAuto{
    public void init() {
        super.init(BaseAutonomous.Team.BLUE);
    }
}
