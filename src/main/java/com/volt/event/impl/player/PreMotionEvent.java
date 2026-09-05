package com.volt.event.impl.player;

import com.volt.IMinecraft;
import com.volt.event.types.Event;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.MathHelper;

@Getter
public class PreMotionEvent implements Event, IMinecraft {

    private final double posX;
    private final double posY;
    private final double posZ;

    @Setter
    private float yaw;
    @Setter
    private float pitch;
    private final float lastYaw;
    private final float lastPitch;

    private final boolean onGround;
    private final boolean isSprinting;
    private final boolean isSneaking;

    @Setter
    private boolean pitchChanged = false;

    public PreMotionEvent(double posX, double posY, double posZ,
                          float yaw, float pitch,
                          float lastYaw, float lastPitch,
                          boolean onGround, boolean isSprinting, boolean isSneaking) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.lastYaw = lastYaw;
        this.lastPitch = lastPitch;
        this.onGround = onGround;
        this.isSprinting = isSprinting;
        this.isSneaking = isSneaking;
    }

    public void turnHead() {
        assert mc.player != null;
        float f = MathHelper.wrapDegrees(yaw - mc.player.bodyYaw);
        mc.player.bodyYaw += f * 0.3F;
        float h = 50.0f;
        if (Math.abs(f) > h) {
            mc.player.bodyYaw += f - (float) MathHelper.sign(f) * h;
        }

        mc.player.headYaw = yaw;
    }
}
