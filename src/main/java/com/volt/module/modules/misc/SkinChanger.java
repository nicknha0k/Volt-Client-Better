package com.volt.module.modules.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.volt.module.Category;
import com.volt.module.Module;
import com.volt.module.setting.BooleanSetting;
import com.volt.module.setting.ModeSetting;
import com.volt.module.setting.StringSetting;
import com.volt.utils.mc.ChatUtils;
import com.volt.event.impl.player.TickEvent;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.SkinTextures;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SkinChanger client-side: copia a skin de outro nick (API da Mojang)
 * e aplica só no seu jogo (terceira pessoa / inventário).
 */
public class SkinChanger extends Module {

    public static final StringSetting skinName = new StringSetting("Skin Name", "Notch");
    public static final ModeSetting model = new ModeSetting("Model", "Auto", "Auto", "Classic", "Slim");
    public static final BooleanSetting showCape = new BooleanSetting("Cape", true);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Getter
    private static volatile SkinTextures customTextures = null;
    @Getter
    private static volatile boolean active = false;
    private static volatile String appliedName = null;
    private static volatile String lastRequested = null;
    private String lastSeen = "";
    private long lastSeenTime = 0;

    public SkinChanger() {
        super("Skin Changer", "Changes your skin client-side (uses other player's name)", Category.MISC);
        this.addSettings(skinName, model, showCape);
    }

    public static String getStatus() {
        if (customTextures != null && appliedName != null) return appliedName;
        return null;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        active = true;
        refreshSkin();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        active = false;
        customTextures = null;
        appliedName = null;
        lastRequested = null;
        setSuffix(null);
    }

    /** Reaplica sozinho ~1s depois que você termina de digitar o nick/model. */
    @EventHandler
    private void onTick(TickEvent event) {
        if (!isEnabled() || isNull()) return;
        String name = skinName.getValue() == null ? "" : skinName.getValue().trim();
        String current = name + "|" + model.getMode() + "|" + showCape.getValue();
        long now = System.currentTimeMillis();
        if (!current.equals(lastSeen)) {
            lastSeen = current;
            lastSeenTime = now;
            return;
        }
        if (!current.equals(lastRequested) && now - lastSeenTime > 800 && !name.isEmpty()) {
            refreshSkin();
        }
    }

    /** Chamado todo tick? Não — só sob demanda para não spammar a API da Mojang. */
    public void refreshSkin() {
        String name = skinName.getValue() == null ? "" : skinName.getValue().trim();
        if (name.isEmpty()) {
            ChatUtils.addChatMessage("§c[SkinChanger] Digite um nick em \"Skin Name\".");
            return;
        }
        String wanted = name + "|" + model.getMode() + "|" + showCape.getValue();
        lastRequested = wanted;
        setSuffix("loading...");
        appliedName = null;

        CompletableFuture.runAsync(() -> {
            try {
                UUID uuid = fetchUuid(name);
                if (uuid == null) {
                    ChatUtils.addChatMessage("§c[SkinChanger] Nick não encontrado: " + name);
                    mc.execute(() -> setSuffix("error"));
                    return;
                }
                Property textures = fetchTextures(uuid);
                if (textures == null) {
                    ChatUtils.addChatMessage("§c[SkinChanger] Sem skin para: " + name);
                    mc.execute(() -> setSuffix("error"));
                    return;
                }

                // UUID aleatório só para não colidir com o cache do provider
                GameProfile fake = new GameProfile(UUID.randomUUID(), name);
                fake.getProperties().put(textures.name(), textures);

                SkinTextures fetched = mc.getSkinProvider().fetchSkinTextures(fake).join();
                if (fetched == null) {
                    ChatUtils.addChatMessage("§c[SkinChanger] Falha ao baixar a skin.");
                    mc.execute(() -> setSuffix("error"));
                    return;
                }

                SkinTextures finalTextures = applyOverrides(fetched);
                customTextures = finalTextures;
                appliedName = name;
                mc.execute(() -> {
                    setSuffix(name);
                    ChatUtils.addChatMessage("§a[SkinChanger] Skin aplicada: " + name);
                });
            } catch (Exception e) {
                ChatUtils.addChatMessage("§c[SkinChanger] Erro: " + e.getMessage());
                if (mc != null) mc.execute(() -> setSuffix("error"));
            }
        });
    }

    private SkinTextures applyOverrides(SkinTextures base) {
        SkinTextures.Model wantedModel = base.model();
        if (model.isMode("Slim")) wantedModel = SkinTextures.Model.SLIM;
        else if (model.isMode("Classic")) wantedModel = SkinTextures.Model.WIDE;

        net.minecraft.util.Identifier cape = showCape.getValue() ? base.capeTexture() : null;

        if (wantedModel == base.model() && cape == base.capeTexture()) return base;
        return new SkinTextures(base.texture(), base.textureUrl(), cape, base.elytraTexture(), wantedModel, base.secure());
    }

    private static UUID fetchUuid(String name) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) return null;
        JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
        if (!obj.has("id")) return null;
        return parseMojangId(obj.get("id").getAsString());
    }

    private static Property fetchTextures(UUID uuid) throws Exception {
        String dashed = uuid.toString();
        String undashed = dashed.replace("-", "");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + undashed + "?unsigned=false"))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) return null;
        JsonObject obj = JsonParser.parseString(res.body()).getAsJsonObject();
        if (!obj.has("properties")) return null;
        JsonArray props = obj.getAsJsonArray("properties");
        for (JsonElement el : props) {
            JsonObject p = el.getAsJsonObject();
            if (!p.has("name") || !"textures".equals(p.get("name").getAsString())) continue;
            String value = p.get("value").getAsString();
            String signature = p.has("signature") ? p.get("signature").getAsString() : null;
            if (signature != null) return new Property("textures", value, signature);
            return new Property("textures", value);
        }
        return null;
    }

    private static UUID parseMojangId(String raw) {
        String hex = raw.replace("-", "");
        return UUID.fromString(hex.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }
}
