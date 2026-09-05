package com.volt.utils.mc;

import com.volt.IMinecraft;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@UtilityClass
@Getter
public final class PlayerUtil implements IMinecraft {
    private int offGroundTicks = 0;
    private int groundTicks = 0;

    public int getOffGroundTicks() {
        assert mc.player != null;
        if (mc.player.isOnGround()) {
            groundTicks++;
            offGroundTicks = 0;
        } else {
            groundTicks = 0;
            offGroundTicks++;
        }
        return offGroundTicks;
    }
}
