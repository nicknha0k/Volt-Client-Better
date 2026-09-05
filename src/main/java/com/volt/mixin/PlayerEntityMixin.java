package com.volt.mixin;

import com.volt.Volt;
import com.volt.module.modules.player.FastMine;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void modifyBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        if (Volt.INSTANCE == null) return;

        Optional<FastMine> optionalModule = Volt.INSTANCE.getModuleManager().getModule(FastMine.class);
        if (optionalModule.isEmpty()) return;

        FastMine fastMine = optionalModule.get();
        if (!fastMine.isEnabled()) return;

        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player != MinecraftClient.getInstance().player) return;

        float modifiedSpeed = cir.getReturnValue() * fastMine.getSpeed();
        cir.setReturnValue(modifiedSpeed);
    }
}
