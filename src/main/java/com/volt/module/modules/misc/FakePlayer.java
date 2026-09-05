package com.volt.module.modules.misc;

import com.mojang.authlib.GameProfile;
import com.volt.event.impl.world.WorldChangeEvent;
import com.volt.module.Category;
import com.volt.module.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.UUID;

public class FakePlayer extends Module {
    private OtherClientPlayerEntity fakePlayer;

    public FakePlayer() {
        super("Fake Player", "Spawns a fake player for making configs (Only works in single player)", Category.MISC);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        spawnFakePlayer();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        despawnFakePlayer();
    }

    @EventHandler
    private void onWorldChange(WorldChangeEvent event) {
        despawnFakePlayer();
    }

    private void spawnFakePlayer() {
        if (isNull()) return;
        if (!mc.isInSingleplayer()) return;
        if (fakePlayer != null) return;
        GameProfile original = mc.player.getGameProfile();
        GameProfile profile = new GameProfile(UUID.randomUUID(), original.getName());
        profile.getProperties().putAll(original.getProperties());

        OtherClientPlayerEntity other = new OtherClientPlayerEntity(mc.world, profile);
        other.copyPositionAndRotation(mc.player);
        other.setYaw(mc.player.getYaw());
        other.setPitch(mc.player.getPitch());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                other.equipStack(slot, stack.copy());
            }
        }

        mc.world.addEntity(other);

        fakePlayer = other;
    }

    private void despawnFakePlayer() {
        if (fakePlayer == null) return;
        if (!isNull()) {
            fakePlayer.discard();
        }
        fakePlayer = null;
    }
}