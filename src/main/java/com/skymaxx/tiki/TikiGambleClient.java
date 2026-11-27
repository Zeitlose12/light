package com.skymaxx.tiki;

import com.skymaxx.tiki.anim.AnimationDirector;
import com.skymaxx.tiki.anim.Prize;
import com.skymaxx.tiki.audio.AudioDirector;
import com.skymaxx.tiki.audio.BoaAudio;
import com.skymaxx.tiki.audio.SoundHold;
import com.skymaxx.tiki.boa.BoaDirector;
import com.skymaxx.tiki.boa.BoaPrize;
import com.skymaxx.tiki.command.SkyMaxxCommand;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.config.TikiStats;
import com.skymaxx.tiki.detect.DropLines;
import com.skymaxx.tiki.detect.InventoryWatch;
import com.skymaxx.tiki.detect.SeaCreatureDetector;
import com.skymaxx.tiki.gui.SkyMaxxScreen;
import com.skymaxx.tiki.render.HotspotText;
import com.skymaxx.tiki.render.HudFilter;
import com.skymaxx.tiki.render.JungleOverlay;
import com.skymaxx.tiki.render.ScreenFx;
import com.skymaxx.tiki.render.StampOverlay;
import com.skymaxx.tiki.render.TikiAssets;
import com.skymaxx.tiki.render.TotemHud;
import com.skymaxx.tiki.render.boa.BoaAssets;
import com.skymaxx.tiki.render.boa.BoaHud;
import com.skymaxx.tiki.render.boa.BoaStamp;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TikiGambleClient
implements ClientModInitializer {
    public static final String MOD_ID = "tikigamble";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"SkyMaxx");
    public static TikiConfig config;
    public static TikiStats stats;
    public static AnimationDirector director;
    public static AudioDirector audio;
    public static StampOverlay stamp;
    private static SeaCreatureDetector<Prize> tikiWatch;

    /** Light: every server chat line, read from the packet before other mods (FEESH etc.) hide or rewrite it. */
    public static void onServerChat(net.minecraft.network.chat.Component message, boolean overlay) {
        try {
            if (inventory != null) inventory.onChat(message, overlay);
        } catch (Throwable t) {
            LOGGER.warn("[Light] inventory chat failed", t);
        }
        try {
            if (tikiWatch != null) tikiWatch.onChat(message, overlay);
            if (boaWatch != null) boaWatch.onChat(message, overlay);
        } catch (Throwable t) {
            LOGGER.warn("[Light] creature chat failed", t);
        }
        try {
            com.skymaxx.tiki.light.dye.DyeTracker.onChat(message, overlay);
        } catch (Throwable t) {
            LOGGER.warn("[Light] dye chat failed", t);
        }
        com.skymaxx.tiki.light.LavaScene.onChat(message, overlay);
        com.skymaxx.tiki.light.SackValue.onChat(message, overlay);
        try {
            com.skymaxx.tiki.light.Crew.onChat(message, overlay);
        } catch (Throwable t) {
            LOGGER.warn("[Light] crew chat failed", t);
        }
    }
    private static TotemHud totem;
    private static JungleOverlay jungle;
    private static InventoryWatch inventory;
    public static BoaDirector boa;
    public static BoaStamp boaStamp;
    private static SeaCreatureDetector<BoaPrize> boaWatch;
    private static BoaHud boaHud;

    /** Light: the Titanoboa HUD, so other scenes can borrow its rifle and scope (render/boa/LightRifle). */
    public static BoaHud boaHud() {
        return boaHud;
    }
    private static final Identifier[] VANILLA_HUD;
    private static final float HUD_FADE_SECONDS = 0.35f;
    private static long ownedSinceNanos;
    private static boolean blurClaimed;
    private static final UUID OWNER;

    public void onInitializeClient() {
        com.skymaxx.tiki.light.LightFiles.migrate();
        File dir = FabricLoader.getInstance().getConfigDir().resolve(com.skymaxx.tiki.light.LightFiles.FOLDER).toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            LOGGER.warn("[SkyMaxx] could not create config directory {}", (Object)dir);
        }
        config = new TikiConfig();
        config.bind(dir);
        config.load();
        stats = new TikiStats();
        stats.bind(dir);
        stats.load();
        TikiAssets.bind(dir);
        File staleAccess = new File(dir, "access.json");
        if (staleAccess.isFile() && !staleAccess.delete()) {
            LOGGER.warn("[SkyMaxx] could not delete the old early access file {}", (Object)staleAccess);
        }
        audio = new AudioDirector(config);
        ScreenFx fx = new ScreenFx(config);
        jungle = new JungleOverlay();
        stamp = new StampOverlay(audio);
        director = new AnimationDirector(config, stats, audio, stamp);
        tikiWatch = new SeaCreatureDetector<Prize>(config, new SeaCreatureDetector.Quarry(() -> TikiGambleClient.config.tikiEnabled, () -> TikiGambleClient.config.creatureName, () -> TikiGambleClient.config.excludeNames, () -> TikiGambleClient.config.muteFromHealth, () -> TikiGambleClient.config.tikiCatchMessage, () -> TikiGambleClient.config.lootShareMessage, false, () -> TikiGambleClient.config.tikiOwnCatchesOnly), director, new SeaCreatureDetector.DropReader<Prize>(){

            @Override
            public Prize prizeFor(String itemName, String skyblockId) {
                return DropLines.prizeFor(itemName, skyblockId, config);
            }

            @Override
            public Prize none() {
                return Prize.NONE;
            }
        }, () -> boa != null && boa.owningScreen());
        totem = new TotemHud(config, director, stamp, fx, jungle, audio);
        BoaAssets.bind(dir);
        BoaAudio boaAudio = new BoaAudio(config, audio);
        boaStamp = new BoaStamp(boaAudio, config);
        boa = new BoaDirector(config, stats, boaAudio, new BoaDirector.Hooks(){

            @Override
            public void chat(String message) {
                TikiGambleClient.chat(message);
            }

            @Override
            public void stamp(BoaPrize prize, int number) {
                if (TikiGambleClient.config.stampsEnabled) {
                    boaStamp.trigger(prize, number);
                }
            }
        });
        boaWatch = new SeaCreatureDetector<BoaPrize>(config, new SeaCreatureDetector.Quarry(() -> TikiGambleClient.config.titanoboaEnabled, () -> TikiGambleClient.config.titanoboaName, () -> TikiGambleClient.config.titanoboaExcludeNames, () -> TikiGambleClient.config.titanoboaMuteFromHealth, () -> TikiGambleClient.config.titanoboaCatchMessage, () -> TikiGambleClient.config.lootShareMessage, true, () -> TikiGambleClient.config.titanoboaOwnCatchesOnly), boa, new SeaCreatureDetector.DropReader<BoaPrize>(){

            @Override
            public BoaPrize prizeFor(String itemName, String skyblockId) {
                return (BoaPrize)DropLines.prizeFor((String)itemName, (String)skyblockId, (TikiConfig)config, (DropLines.Named[])BoaPrize.values(), (DropLines.Named)BoaPrize.NONE);
            }

            @Override
            public BoaPrize none() {
                return BoaPrize.NONE;
            }
        }, () -> director.owningScreen());
        boaHud = new BoaHud(config, boa, boaStamp, audio);
        HudElementRegistry.addLast((Identifier)TikiGambleClient.id("totem"), (HudElement)TikiGambleClient.guarded("totem", totem::renderAboveHud));
        HudElementRegistry.addLast((Identifier)TikiGambleClient.id("titanoboa"), (HudElement)TikiGambleClient.guarded("titanoboa", boaHud::renderAboveHud));
        for (Identifier element : VANILLA_HUD) {
            boolean spoiler = element.equals(VanillaHudElements.CHAT) || element.equals(VanillaHudElements.TITLE_AND_SUBTITLE)
                || element.equals(VanillaHudElements.OVERLAY_MESSAGE);
            HudElementRegistry.replaceElement((Identifier)element, original -> {
                HudElement inner = TikiGambleClient.suppressWhileRunning(original);
                if (!spoiler) return inner;
                // Light: chat and titles stay hidden while a gambling scene sits in a corner (no spoiler)
                return (ctx, delta) -> {
                    boolean hide = false;
                    try {
                        hide = com.skymaxx.tiki.light.LavaScene.hidesChat();
                    } catch (Throwable ignored) {
                    }
                    if (!hide) inner.extractRenderState(ctx, delta);
                };
            });
        }
        inventory = new InventoryWatch(config);
        inventory.listen(tikiWatch::onItemGained);
        inventory.listen(boaWatch::onItemGained);
        inventory.listen((n, id, c, sack) -> {
            try {
                com.skymaxx.tiki.light.LavaScene.onItemGained(n, id, c, sack);
            } catch (Throwable ignored) {
            }
        });
        // Light: chat now comes straight from the packet (see LightPacketMixin), before other mods can hide it
        Runnable standDown = () -> {
            if (director.owningScreen()) {
                director.abort();
            }
            if (boa.owningScreen()) {
                boa.abort();
            }
            audio.forceRestore();
            stamp.abort();
            jungle.abort();
            tikiWatch.reset();
            boaStamp.abort();
            boaWatch.reset();
            inventory.reset();
            SoundHold.clear();
            HotspotText.clear();
        };
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            try {
                HotspotText.submit(context);
            }
            catch (Throwable t) {
                LOGGER.error("[SkyMaxx] hotspot text failed to draw", t);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (SkyMaxxCommand.consumeOpenRequest()) {
                client.gui.setScreen((Screen)SkyMaxxScreen.create());
            }
            if (client.level == null) {
                standDown.run();
                return;
            }
            TikiGambleClient.tick(client);
        });
        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick(){
            private boolean started;

            public void onEndTick(Minecraft client) {
                if (!this.started) {
                    this.started = true;
                    audio.init();
                }
            }
        });
        SkyMaxxCommand.register(director, boa, config, stats);
        LOGGER.info("[SkyMaxx] ready");
    }

    private static void tick(Minecraft client) {
        if (director.stalled()) {
            LOGGER.warn("[SkyMaxx] the totem stopped being drawn; ending it");
            director.abort();
            stamp.abort();
            jungle.abort();
        }
        if (boa.stalled()) {
            LOGGER.warn("[SkyMaxx] the Titanoboa stopped being drawn; ending it");
            boa.abort();
            boaStamp.abort();
        }
        if (!TikiGambleClient.gameShouldBeSilent()) {
            audio.setMuted(false);
        }
        audio.tick();
        inventory.tick(client);
        tikiWatch.onTick();
        boaWatch.onTick();
        HotspotText.tick(client, config);
        SoundHold.tick(TikiGambleClient.gameShouldBeSilent());
        if (TikiGambleClient.chatShut() && client.gui.screen() instanceof ChatScreen) {
            client.gui.setScreen(null);
        }
    }

    private static boolean chatShut() {
        return director != null && (director.armed() || director.running()) || boa != null && (boa.armed() || boa.running());
    }

    private static HudElement suppressWhileRunning(HudElement original) {
        return (ctx, delta) -> {
            if (!TikiGambleClient.animationOwnsScreen() || HudFilter.fading()) {
                original.extractRenderState(ctx, delta);
            }
        };
    }

    private static HudElement guarded(String what, HudElement element) {
        return (ctx, delta) -> {
            try {
                HudFilter.untouched(() -> element.extractRenderState(ctx, delta));
            }
            catch (Throwable t) {
                LOGGER.error("[SkyMaxx] {} failed to draw over the HUD", (Object)what, (Object)t);
                TikiGambleClient.recoverPose(ctx);
            }
        };
    }

    public static float hudAlphaWhileOwned() {
        long now = System.nanoTime();
        if (ownedSinceNanos == 0L) {
            ownedSinceNanos = now;
        }
        float k = Math.min(1.0f, (float)(now - ownedSinceNanos) / 1.0E9f / 0.35f);
        return 1.0f - k * k * (3.0f - 2.0f * k);
    }

    private static void recoverPose(GuiGraphicsExtractor ctx) {
        try {
            ctx.pose().clear();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public static void claimBlur(GuiGraphicsExtractor ctx) {
        if (blurClaimed) {
            return;
        }
        try {
            ctx.blurBeforeThisStratum();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        blurClaimed = true;
    }

    public static boolean blurAlreadyClaimed() {
        return blurClaimed;
    }

    public static void releaseBlurClaim() {
        blurClaimed = false;
    }

    public static boolean canTest() {
        User user = Minecraft.getInstance().getUser();
        return user != null && OWNER.equals(user.getProfileId());
    }

    public static boolean renderTotemBeforeHud(GuiGraphicsExtractor ctx, DeltaTracker delta) {
        boolean owned = false;
        if (totem != null) {
            try {
                owned = totem.renderBeneathHud(ctx, delta);
            }
            catch (Throwable t) {
                LOGGER.error("[SkyMaxx] the totem failed to draw", t);
                TikiGambleClient.recoverPose(ctx);
            }
        }
        if (boaHud != null) {
            try {
                owned |= boaHud.renderBeneathHud(ctx, delta);
            }
            catch (Throwable t) {
                LOGGER.error("[SkyMaxx] the Titanoboa failed to draw", t);
                TikiGambleClient.recoverPose(ctx);
            }
        }
        if (!owned) {
            ownedSinceNanos = 0L;
        }
        return owned;
    }

    public static boolean animationOwnsScreen() {
        return director != null && director.owningScreen() || boa != null && boa.owningScreen();
    }

    public static boolean gameShouldBeSilent() {
        return director != null && (director.owningScreen() || director.inAftermath()) || stamp != null && stamp.visible() || boa != null && boa.owningScreen() || boaStamp != null && boaStamp.visible();
    }

    public static boolean holdGameSounds() {
        return tikiWatch != null && tikiWatch.engaged() || boaWatch != null && boaWatch.engaged();
    }

    public static boolean onScreenOpening(Screen screen) {
        return com.skymaxx.tiki.light.LightScreens.onScreenOpening(screen);
    }

    public static List<String> dropLog() {
        ArrayList<String> out = new ArrayList<String>();
        if (inventory == null) {
            return out;
        }
        out.add("\u00a76Arrived:");
        List<String> gains = inventory.recentGains();
        if (gains.isEmpty()) {
            out.add("\u00a78  nothing yet");
        }
        for (String line : gains) {
            out.add("  " + line);
        }
        out.add("\u00a76Wiki Tiki drops:");
        TikiGambleClient.addDecisions(out, tikiWatch);
        out.add("\u00a76Titanoboa drops:");
        TikiGambleClient.addDecisions(out, boaWatch);
        return out;
    }

    private static void addDecisions(List<String> out, SeaCreatureDetector<?> watch) {
        List<String> lines = watch == null ? List.<String>of() : watch.recentDecisions();
        if (lines.isEmpty()) {
            out.add("\u00a78  none yet");
        }
        for (String string : lines) {
            out.add("  " + string);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath((String)MOD_ID, (String)path);
    }

    public static void chat(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            mc.gui.hud.getChat().addClientSystemMessage((Component)Component.literal((String)message));
        }
    }

    static {
        VANILLA_HUD = new Identifier[]{VanillaHudElements.MISC_OVERLAYS, VanillaHudElements.CROSSHAIR, VanillaHudElements.SPECTATOR_MENU, VanillaHudElements.HOTBAR, VanillaHudElements.ARMOR_BAR, VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL, VanillaHudElements.HELD_ITEM_TOOLTIP, VanillaHudElements.SPECTATOR_TOOLTIP, VanillaHudElements.MOB_EFFECTS, VanillaHudElements.BOSS_BAR, VanillaHudElements.SLEEP, VanillaHudElements.DEMO_TIMER, VanillaHudElements.SCOREBOARD, VanillaHudElements.OVERLAY_MESSAGE, VanillaHudElements.TITLE_AND_SUBTITLE, VanillaHudElements.CHAT, VanillaHudElements.PLAYER_LIST, VanillaHudElements.SUBTITLES};
        OWNER = UUID.fromString("dd480f03-3279-4b5b-9b8e-231967e3aef3");
    }
}

