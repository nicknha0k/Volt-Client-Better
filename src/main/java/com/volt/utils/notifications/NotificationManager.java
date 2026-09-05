package com.volt.utils.notifications;

import com.volt.IMinecraft;
import com.volt.Volt;
import com.volt.event.impl.render.EventRender2D;
import com.volt.module.Module;
import com.volt.module.modules.client.Client;
import com.volt.module.modules.client.ClickGUIModule;
import com.volt.utils.render.RenderUtils;
import meteordevelopment.orbit.EventHandler;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Notificações com bordas redondas ao ativar/desativar módulos.
 * Otimizado: usa textRenderer vanilla, caixas de tamanho fixo,
 * no máximo 5 visíveis e pula tudo quando não há nada na fila.
 */
public final class NotificationManager implements IMinecraft {

    public static final NotificationManager INSTANCE = new NotificationManager();

    private static final int BOX_WIDTH = 170;
    private static final int BOX_HEIGHT = 36;
    private static final int RADIUS = 9;
    private static final int GAP = 6;
    private static final int MARGIN = 10;
    private static final int MAX_VISIBLE = 5;
    private static final long DURATION_MS = 2600;

    private static final Color BG = new Color(18, 18, 26, 215);
    private static final Color ENABLED_ACCENT = new Color(85, 220, 130);
    private static final Color DISABLED_ACCENT = new Color(255, 95, 95);

    private final Deque<Notification> queue = new ArrayDeque<>();

    private NotificationManager() {
    }

    /** Chamado pelo {@link Module#setEnabled} — não chame de outros lugares. */
    public static void onModuleToggle(Module module) {
        if (Volt.INSTANCE == null || mc == null) return;
        if (mc.player == null || mc.world == null) return;
        if (module instanceof ClickGUIModule) return;
        if (!notificationsEnabled()) return;
        INSTANCE.push(module.getName(), module.isEnabled() ? "Enabled" : "Disabled", module.isEnabled());
    }

    private static boolean notificationsEnabled() {
        try {
            return Client.notifications.getValue();
        } catch (Exception e) {
            return true;
        }
    }

    public synchronized void push(String title, String description, boolean enabled) {
        // Troca rápida do mesmo módulo: atualiza em vez de empilhar
        queue.removeIf(n -> n.getTitle().equals(title));
        queue.addLast(new Notification(title, description, enabled, DURATION_MS));
        while (queue.size() > MAX_VISIBLE) queue.pollFirst();
    }

    public synchronized void clear() {
        queue.clear();
    }

    @EventHandler
    private void onRender2D(EventRender2D event) {
        if (mc == null || mc.player == null || mc.world == null) return;
        if (mc.textRenderer == null) return;

        List<Notification> visible;
        synchronized (this) {
            if (queue.isEmpty()) return;
            queue.removeIf(Notification::isExpired);
            if (queue.isEmpty()) return;
            visible = new ArrayList<>(queue);
            if (visible.size() > MAX_VISIBLE) visible = visible.subList(visible.size() - MAX_VISIBLE, visible.size());
        }

        var context = event.getContext();
        int screenW = event.getWidth();
        int screenH = event.getHeight();

        // Empilha de baixo para cima no canto inferior direito
        int index = 0;
        for (int i = visible.size() - 1; i >= 0; i--) {
            Notification n = visible.get(i);
            float alpha = n.alphaProgress();
            if (alpha <= 0.02f) {
                index++;
                continue;
            }
            float slide = n.slideProgress();
            int slideOffset = (int) ((1f - slide) * (BOX_WIDTH + 20));

            int x = screenW - BOX_WIDTH - MARGIN + slideOffset;
            int y = screenH - MARGIN - (index + 1) * (BOX_HEIGHT + GAP) + GAP;

            int bgAlpha = (int) (BG.getAlpha() * alpha);
            Color accent = n.isEnabled() ? ENABLED_ACCENT : DISABLED_ACCENT;

            RenderUtils.drawRoundedRect(context, x, y, BOX_WIDTH, BOX_HEIGHT, RADIUS,
                    new Color(BG.getRed(), BG.getGreen(), BG.getBlue(), bgAlpha).getRGB());

            // Barrinha de destaque à esquerda (arredondada)
            int barAlpha = (int) (255 * alpha);
            RenderUtils.drawRoundedRect(context, x + 6, y + 8, 3, BOX_HEIGHT - 16, 2,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), barAlpha).getRGB());

            // Textos
            int titleAlpha = (int) (255 * alpha);
            int descAlpha = (int) (160 * alpha);
            context.drawTextWithShadow(mc.textRenderer, n.getTitle(), x + 15, y + 7,
                    (titleAlpha << 24) | 0xFFFFFF);
            context.drawTextWithShadow(mc.textRenderer, n.getDescription(), x + 15, y + 19,
                    (descAlpha << 24) | (accent.getRGB() & 0xFFFFFF));

            // Barra de progresso embaixo
            int barW = (int) ((BOX_WIDTH - 16) * n.remainingFraction());
            if (barW > 0) {
                RenderUtils.drawRoundedRect(context, x + 8, y + BOX_HEIGHT - 5, barW, 2, 1,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), barAlpha).getRGB());
            }
            index++;
        }
    }
}
