package com.volt.mixin;

import com.volt.Volt;
import com.volt.module.modules.misc.SkinChanger;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerMixin {

    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void volt$overrideSkin(CallbackInfoReturnable<SkinTextures> cir) {
        if (Volt.INSTANCE == null || Volt.mc == null || Volt.mc.player == null) return;
        // Só o próprio player (terceira pessoa / inventário / espelhos)
        if ((Object) this != Volt.mc.player) return;
        if (!SkinChanger.isActive()) return;
        SkinTextures custom = SkinChanger.getCustomTextures();
        if (custom != null) cir.setReturnValue(custom);
    }
}
