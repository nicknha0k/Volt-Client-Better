package com.volt.utils.notifications;

import lombok.Getter;

/** Notificação de toggle com animação de slide + fade. Largura fixa, altura fixa (barato de renderizar). */
@Getter
public final class Notification {
    private final String title;
    private final String description;
    private final boolean enabled;
    private final long createdAt;
    private final long duration;

    public Notification(String title, String description, boolean enabled, long duration) {
        this.title = title;
        this.description = description;
        this.enabled = enabled;
        this.createdAt = System.currentTimeMillis();
        this.duration = duration;
    }

    public long age() {
        return System.currentTimeMillis() - createdAt;
    }

    public boolean isExpired() {
        return age() >= duration;
    }

    /** 0 -> 1 na entrada (300ms), 1 no meio, 1 -> 0 na saída (300ms). */
    public float alphaProgress() {
        long age = age();
        final long in = 250;
        final long out = 300;
        if (age < 0) return 0f;
        if (age < in) return (float) age / in;
        if (age > duration - out) return Math.max(0f, (float) (duration - age) / out);
        return 1f;
    }

    /** 0 -> 1 na entrada com ease-out. */
    public float slideProgress() {
        float t = Math.min(1f, (float) age() / 250f);
        return 1f - (1f - t) * (1f - t);
    }

    public float remainingFraction() {
        return Math.max(0f, 1f - (float) age() / duration);
    }
}
