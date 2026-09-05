package com.volt.module.modules.combat;


import com.volt.event.impl.player.TickEvent;
import com.volt.mixin.MinecraftClientAccessor;
import com.volt.module.Category;
import com.volt.module.Module;
import com.volt.module.setting.BooleanSetting;
import com.volt.module.setting.NumberSetting;
import com.volt.utils.math.MathUtils;
import com.volt.utils.math.TimerUtil;
import com.volt.utils.mc.InventoryUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.util.hit.EntityHitResult;

public final class ShieldBreaker extends Module {
    // Nomes mantidos para não quebrar configs antigas
    public static final NumberSetting maxMs = new NumberSetting("Reaction Max MS", 0, 2000, 200, 1);
    public static final NumberSetting minMS = new NumberSetting("Reaction Min MS", 0, 2000, 120, 1);

    public static final NumberSetting switchDelayMax = new NumberSetting("Switch Max MS", 0, 2000, 150, 1);
    public static final NumberSetting switchDelayMin = new NumberSetting("Switch Min MS", 0, 2000, 80, 1);

    public static final NumberSetting attackDelayMax = new NumberSetting("Attack Max MS", 0, 2000, 150, 1);
    public static final NumberSetting attackDelayMin = new NumberSetting("Attack Min MS", 0, 2000, 80, 1);

    public static final NumberSetting switchBackMax = new NumberSetting("Switch Back Max MS", 0, 2000, 180, 1);
    public static final NumberSetting switchBackMin = new NumberSetting("Switch Back Min MS", 0, 2000, 100, 1);

    public static final NumberSetting cooldownMs = new NumberSetting("Cooldown MS", 0, 2000, 350, 1);
    public static final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    public static final BooleanSetting requireAxe = new BooleanSetting("Require Axe", true);
    public static final BooleanSetting waitCooldown = new BooleanSetting("Wait Vanilla Cooldown", true);

    private enum Stage {
        IDLE, REACTION, SWITCH, ATTACK, SWITCH_BACK, COOLDOWN
    }

    private final TimerUtil stageTimer = new TimerUtil();
    private Stage stage = Stage.IDLE;
    private long pendingDelay = 0;
    private int previousSlot = -1;
    private int axeSlot = -1;

    public ShieldBreaker() {
        super("Shield Breaker", "Uses your axe to break their shield", -1, Category.COMBAT);
        this.addSettings(maxMs, minMS, switchDelayMax, switchDelayMin, attackDelayMax, attackDelayMin, switchBackMax, switchBackMin,
                cooldownMs, swapBack, requireAxe, waitCooldown);
    }

    @EventHandler
    private void onTickEvent(TickEvent event) {
        if (isNull()) return;

        sanitizeSettings();

        boolean shouldBreak = shouldBreakShield();
        if (!shouldBreak) {
            // Alvo parou de defender / perdeu mira: aborta e devolve o slot uma vez só
            abort();
            return;
        }

        switch (stage) {
            case IDLE -> {
                // Começou a defender agora: sorteia UMA vez o tempo de reação e espera
                previousSlot = mc.player.getInventory().selectedSlot;
                axeSlot = InventoryUtil.findHotbarSlot(AxeItem.class);
                if (axeSlot == -1) {
                    if (requireAxe.getValue()) {
                        enterStage(Stage.COOLDOWN, cooldownMs.getValueInt());
                        return;
                    }
                    axeSlot = previousSlot;
                }
                enterStage(Stage.REACTION, randomBetween(minMS.getValueInt(), maxMs.getValueInt()));
            }
            case REACTION -> {
                if (!stageTimer.hasElapsedTime(pendingDelay)) return;
                // Revalida o machado (pode ter trocado de slot no meio da reação)
                int fresh = InventoryUtil.findHotbarSlot(AxeItem.class);
                if (fresh != -1) axeSlot = fresh;
                if (axeSlot == -1 && requireAxe.getValue()) {
                    enterStage(Stage.COOLDOWN, cooldownMs.getValueInt());
                    return;
                }
                if (axeSlot != -1 && mc.player.getInventory().selectedSlot != axeSlot) {
                    mc.player.getInventory().selectedSlot = axeSlot;
                }
                enterStage(Stage.SWITCH, randomBetween(switchDelayMin.getValueInt(), switchDelayMax.getValueInt()));
            }
            case SWITCH -> {
                if (!stageTimer.hasElapsedTime(pendingDelay)) return;
                // Garante que ainda está de machado antes de bater
                if (axeSlot != -1 && mc.player.getInventory().selectedSlot != axeSlot) {
                    mc.player.getInventory().selectedSlot = axeSlot;
                    // Perdeu o timing da troca: espera mais um pouco
                    enterStage(Stage.SWITCH, randomBetween(switchDelayMin.getValueInt(), switchDelayMax.getValueInt()));
                    return;
                }
                enterStage(Stage.ATTACK, randomBetween(attackDelayMin.getValueInt(), attackDelayMax.getValueInt()));
            }
            case ATTACK -> {
                if (!stageTimer.hasElapsedTime(pendingDelay)) return;
                if (waitCooldown.getValue() && mc.player.getAttackCooldownProgress(0.5f) < 1.0f) return;
                ((MinecraftClientAccessor) mc).invokeDoAttack();
                enterStage(Stage.SWITCH_BACK, randomBetween(switchBackMin.getValueInt(), switchBackMax.getValueInt()));
            }
            case SWITCH_BACK -> {
                if (!stageTimer.hasElapsedTime(pendingDelay)) return;
                restoreSlot();
                enterStage(Stage.COOLDOWN, cooldownMs.getValueInt());
            }
            case COOLDOWN -> {
                if (!stageTimer.hasElapsedTime(pendingDelay)) return;
                // Cooldown acabou: se o alvo AINDA estiver defendendo, recomeça o ciclo
                stage = Stage.IDLE;
            }
        }
    }

    private boolean shouldBreakShield() {
        if (!(mc.crosshairTarget instanceof EntityHitResult hitResult)) return false;
        Entity target = hitResult.getEntity();
        if (!(target instanceof PlayerEntity playerTarget)) return false;
        if (!playerTarget.isBlocking()) return false;
        if (mc.player.isBlocking()) return false;
        if (mc.player.isUsingItem()) return false;
        return true;
    }

    private void enterStage(Stage next, long delay) {
        this.stage = next;
        this.pendingDelay = Math.max(0, delay);
        this.stageTimer.reset();
    }

    private void abort() {
        if (stage == Stage.SWITCH_BACK || stage == Stage.ATTACK || stage == Stage.SWITCH) {
            // Estava no meio do combo: devolve o slot imediatamente
            restoreSlot();
        }
        if (stage != Stage.COOLDOWN) {
            stage = Stage.IDLE;
            pendingDelay = 0;
        }
        // Se já está em COOLDOWN, deixa o cooldown terminar para não spammar
    }

    private void restoreSlot() {
        if (!swapBack.getValue()) {
            previousSlot = -1;
            return;
        }
        if (previousSlot != -1 && mc.player != null) {
            int clamped = Math.max(0, Math.min(8, previousSlot));
            mc.player.getInventory().selectedSlot = clamped;
        }
        previousSlot = -1;
    }

    private void sanitizeSettings() {
        if (minMS.getValueInt() >= maxMs.getValueInt()) maxMs.setValue(minMS.getValueInt() + 1);
        if (switchDelayMin.getValueInt() >= switchDelayMax.getValueInt())
            switchDelayMax.setValue(switchDelayMin.getValueInt() + 1);
        if (attackDelayMin.getValueInt() >= attackDelayMax.getValueInt())
            attackDelayMax.setValue(attackDelayMin.getValueInt() + 1);
        if (switchBackMin.getValueInt() >= switchBackMax.getValueInt())
            switchBackMax.setValue(switchBackMin.getValueInt() + 1);
    }

    private long randomBetween(int min, int max) {
        if (max <= min) return Math.max(0, min);
        return (long) MathUtils.randomDoubleBetween(min, max);
    }

    @Override
    public void onEnable() {
        stage = Stage.IDLE;
        pendingDelay = 0;
        previousSlot = -1;
        axeSlot = -1;
        stageTimer.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        restoreSlot();
        stage = Stage.IDLE;
        pendingDelay = 0;
        axeSlot = -1;
        stageTimer.reset();
        super.onDisable();
    }
}
