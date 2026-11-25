package com.skymaxx.tiki.light;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.gui.SkyMaxxScreen;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import com.skymaxx.tiki.light.dye.DyeHud;
import com.skymaxx.tiki.light.dye.DyeTracker;

/** Light: adds the /light command that opens the settings screen. */
public final class LightInit implements ClientModInitializer {
    private static volatile boolean openRequested;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
            dispatcher.register(ClientCommands.literal("light").executes(c -> {
                openRequested = true;
                return 1;
            }).then(ClientCommands.literal("testachievement").executes(c -> {
                return LightInit.testAchievement(null);
            }).then(ClientCommands.argument("name", StringArgumentType.word()).suggests((c, builder) -> {
                for (Achievements a : Achievements.values()) {
                    builder.suggest(a.name().toLowerCase(Locale.ROOT));
                }
                return builder.buildFuture();
            }).executes(c -> {
                return LightInit.testAchievement(StringArgumentType.getString(c, "name"));
            }))).then(ClientCommands.literal("pvcheck").executes(c -> {
                return com.skymaxx.tiki.light.dye.PvBridge.check();
            })).then(ClientCommands.literal("dragonfix").then(ClientCommands.argument("yaw", com.mojang.brigadier.arguments.FloatArgumentType.floatArg()).then(ClientCommands.argument("pitch", com.mojang.brigadier.arguments.FloatArgumentType.floatArg()).then(ClientCommands.argument("y", com.mojang.brigadier.arguments.FloatArgumentType.floatArg()).executes(c -> {
                com.skymaxx.tiki.light.MobView.dragonYawFix = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(c, "yaw");
                com.skymaxx.tiki.light.MobView.dragonPitchFix = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(c, "pitch");
                com.skymaxx.tiki.light.MobView.dragonYFix = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(c, "y");
                com.skymaxx.tiki.light.MobView.reset();
                TikiGambleClient.chat("\u00a78[Light] dragon view: yaw " + com.skymaxx.tiki.light.MobView.dragonYawFix + ", pitch " + com.skymaxx.tiki.light.MobView.dragonPitchFix + ", y " + com.skymaxx.tiki.light.MobView.dragonYFix);
                return 1;
            }))))).then(ClientCommands.literal("anim").then(ClientCommands.argument("creature", StringArgumentType.word()).suggests((c, builder) -> {
                for (String n : ANIMS) builder.suggest(n);
                return builder.buildFuture();
            }).executes(c -> {
                return LightInit.testAnim(StringArgumentType.getString(c, "creature"), false);
            }).then(ClientCommands.literal("loot").executes(c -> {
                return LightInit.testAnim(StringArgumentType.getString(c, "creature"), true);
            }))))));
        DyeTracker.load();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("light", "dye_hud"), DyeHud::render);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("light", "lava_scene"), LavaScene::render);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("light", "shark_popup"), SharkPopup::render);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Achievements.tick();
            DyeTracker.tick(client);
            if (openRequested) {
                openRequested = false;
                client.gui.setScreen(SkyMaxxScreen.create());
            }
        });
    }

    private static final String[] ANIMS = {"jawbus", "thunder", "ragnarok", "plhlegblast", "grim", "yeti", "reindrake", "shark", "sharktooth", "sharkmiss"};

    /** Debug only: plays a lava scene with a made-up drop. Nothing is counted. */
    private static int testAnim(String which, boolean loot) {
        if (TikiGambleClient.config == null || !TikiGambleClient.config.debug) {
            TikiGambleClient.chat("\u00a7c[Light] Turn on \"Debug log\" in /light -> General to test animations.");
            return 0;
        }
        String key = which.toLowerCase(Locale.ROOT);
        switch (key) {
            case "shark": SharkPopup.play("Megalodon", null); break;
            case "sharktooth": SharkPopup.play("Shark Tooth", null); break;
            case "sharkmiss": SharkPopup.play(null, null); break;
            default:
                if (!LavaScene.test(key, loot)) {
                    TikiGambleClient.chat("\u00a7c[Light] Unknown: " + which + " (jawbus, thunder, ragnarok, plhlegblast, grim, yeti, reindrake, shark, sharktooth, sharkmiss)");
                    return 0;
                }
        }
        TikiGambleClient.chat("\u00a78[Light] Test only - nothing was counted. The result is random.");
        return 1;
    }

    /** Debug only: plays the unlock title and sound. Nothing is saved as unlocked. */
    private static int testAchievement(String name) {
        if (TikiGambleClient.config == null || !TikiGambleClient.config.debug) {
            TikiGambleClient.chat("\u00a7c[Light] Turn on \"Debug log\" in /light -> General to test achievements.");
            return 0;
        }
        Achievements which = name == null ? null : Achievements.byName(name);
        if (name != null && which == null) {
            TikiGambleClient.chat("\u00a7c[Light] Unknown achievement: " + name);
            return 0;
        }
        Achievements.preview(which);
        TikiGambleClient.chat("\u00a78[Light] Test only - nothing was unlocked.");
        return 1;
    }
}
