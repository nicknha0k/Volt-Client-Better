package com.volt.utils.mc;

import com.volt.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.profiling.jfr.event.PacketEvent;

import java.util.Objects;

@UtilityClass
public final class InventoryUtil implements IMinecraft {
    public static void swapToSlot(Item item) {
        for (byte i = 0; i < 9; i++) {
            assert mc.player != null;
            var stack = mc.player.getInventory().getStack(i);

            if (stack.isEmpty()) continue;
            if (stack.getItem().equals(item)) {
                mc.player.getInventory().selectedSlot = i;
                return;
            }
        }
    }
    public static boolean hasItem(Item item) {
        for (byte i = 0; i < Objects.requireNonNull(mc.player).getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) {
                return true;
            }
        }
        return false;
    }

    public static void swapToWeapon(Class<? extends Item> weaponClass) {
        int slot = findHotbarSlot(weaponClass);
        if (slot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = slot;
        }
    }

    /** Procura o item apenas na hotbar (slots 0-8). Retorna -1 se não achar. */
    public static int findHotbarSlot(Class<? extends Item> weaponClass) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (weaponClass.isInstance(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    /** Procura o item apenas na hotbar (slots 0-8). Retorna -1 se não achar. */
    public static int findHotbarSlot(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem().equals(item)) {
                return i;
            }
        }
        return -1;
    }

}

