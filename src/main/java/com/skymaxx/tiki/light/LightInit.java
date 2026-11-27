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
            })).then(ClientCommands.literal("test")
                .then(ClientCommands.literal("fanfare").then(ClientCommands.argument("mob", StringArgumentType.word()).suggests((c, b) -> {
                    for (String n : GambleLog.MOBS) b.suggest(n);
                    return b.buildFuture();
                }).then(ClientCommands.argument("loot", StringArgumentType.greedyString()).executes(c -> LightInit.testFanfare(StringArgumentType.getString(c, "mob"), StringArgumentType.getString(c, "loot"))))))
                .then(ClientCommands.literal("dry").then(ClientCommands.argument("mob", StringArgumentType.word()).suggests((c, b) -> {
                    for (String n : GambleLog.MOBS) b.suggest(n);
                    return b.buildFuture();
                }).then(ClientCommands.argument("kills", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0)).executes(c -> LightInit.testDry(StringArgumentType.getString(c, "mob"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "kills"))))))
                .then(ClientCommands.literal("history").executes(c -> LightInit.testHistory()))
                .then(ClientCommands.literal("clear").executes(c -> LightInit.testClear())))
            .then(ClientCommands.literal("journal").executes(c -> LightInit.openJournal()))
            .then(ClientCommands.literal("testtheme").then(ClientCommands.argument("theme", StringArgumentType.word()).suggests((c, b) -> {
                for (Theme t : Theme.values()) b.suggest(t.name().toLowerCase(Locale.ROOT));
                return b.buildFuture();
            }).executes(c -> LightInit.testTheme(StringArgumentType.getString(c, "theme")))))
            .then(ClientCommands.literal("testcrew").executes(c -> LightInit.testCrew()))
            .then(ClientCommands.literal("achievement")
                .then(ClientCommands.literal("list").executes(c -> LightInit.listAchievements()))
                .then(ClientCommands.literal("remove").then(ClientCommands.argument("name", StringArgumentType.word()).suggests((c, b) -> {
                    for (Achievements a : Achievements.values()) b.suggest(a.name().toLowerCase(Locale.ROOT));
                    return b.buildFuture();
                }).executes(c -> LightInit.removeAchievement(StringArgumentType.getString(c, "name")))))
                .then(ClientCommands.literal("restore").then(ClientCommands.argument("name", StringArgumentType.word()).suggests((c, b) -> {
                    for (String n : Achievements.revoked(TikiGambleClient.config)) b.suggest(n.toLowerCase(Locale.ROOT));
                    return b.buildFuture();
                }).executes(c -> LightInit.restoreAchievement(StringArgumentType.getString(c, "name"))))))
            .then(ClientCommands.literal("dragonfix").then(ClientCommands.argument("yaw", com.mojang.brigadier.arguments.FloatArgumentType.floatArg()).then(ClientCommands.argument("pitch", com.mojang.brigadier.arguments.FloatArgumentType.floatArg()).then(ClientCommands.argument("y", com.mojang.brigadier.arguments.FloatArgumentType.floatArg()).executes(c -> {
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
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("light", "achievement_popup"), AchievementPopup::render);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("light", "sack_value"), SackValue::render);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("light", "cosmetic_popup"), CosmeticPopup::render);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> SackValue.saveNow());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Achievements.tick();
            DyeTracker.tick(client);
            SackValue.tick(client);
            GambleLog.tick();
            Crew.tick();
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

    private static boolean needDebug(String what) {
        if (TikiGambleClient.config == null || !TikiGambleClient.config.debug) {
            TikiGambleClient.chat("\u00a7c[Light] Turn on \"Debug log\" in /light -> General to " + what + ".");
            return false;
        }
        return true;
    }

    private static boolean needBeta() {
        if (TikiGambleClient.config == null || !TikiGambleClient.config.betaFeatures) {
            TikiGambleClient.chat("\u00a7e[Light] Tip: turn on \"Beta features\" (General) - otherwise the dry streak and fanfare stay hidden.");
            return false;
        }
        return true;
    }

    /** /light test fanfare <mob> <loot>: the scene with this drop and the RNG fanfare. Counted nowhere. */
    private static int testFanfare(String mob, String loot) {
        if (!needDebug("test the fanfare")) return 0;
        needBeta();
        if (!LavaScene.testWith(mob.toLowerCase(Locale.ROOT), loot, true)) {
            TikiGambleClient.chat("\u00a7c[Light] Unknown mob: " + mob + " (" + String.join(", ", GambleLog.MOBS) + ")");
            return 0;
        }
        TikiGambleClient.chat("\u00a78[Light] Test only - nothing is counted.");
        return 1;
    }

    /** /light test dry <mob> <kills>: the next test scene shows this dry streak. */
    private static int testDry(String mob, int kills) {
        if (!needDebug("test the dry streak")) return 0;
        needBeta();
        GambleLog.dryOverride = kills;
        if (!LavaScene.test(mob.toLowerCase(Locale.ROOT), false)) {
            GambleLog.dryOverride = -1L;
            TikiGambleClient.chat("\u00a7c[Light] Unknown mob: " + mob + " (" + String.join(", ", GambleLog.MOBS) + ")");
            return 0;
        }
        TikiGambleClient.chat("\u00a78[Light] Test only - dry streak " + kills + ", nothing is counted.");
        return 1;
    }

    /** Debug: the centre "NEW THEME UNLOCKED" pop-up for this theme. Nothing is unlocked. */
    private static int testTheme(String name) {
        if (!needDebug("test the theme pop-up")) return 0;
        Theme t = null;
        for (Theme x : Theme.values()) if (x.name().equalsIgnoreCase(name)) t = x;
        if (t == null) {
            TikiGambleClient.chat("\u00a7c[Light] Unknown theme: " + name);
            return 0;
        }
        CosmeticPopup.theme(t, 0L);
        TikiGambleClient.chat("\u00a78[Light] Test only - nothing was unlocked.");
        return 1;
    }

    /** Debug: the Light Crew party banner. */
    private static int testCrew() {
        if (!needDebug("test the crew banner")) return 0;
        CosmeticPopup.crewDrop("Gesundheitskarte", "Radioactive Vial");
        TikiGambleClient.chat("\u00a78[Light] Test only.");
        return 1;
    }

    private static int testHistory() {
        if (!needDebug("fill a test history")) return 0;
        GambleLog.fillTest();
        TikiGambleClient.chat("\u00a78[Light] Test entries added to History (yellow [Test]). Remove them with /light test clear.");
        return 1;
    }

    private static int testClear() {
        if (!needDebug("clear test data")) return 0;
        GambleLog.clearTest();
        TikiGambleClient.chat("\u00a78[Light] Test entries removed. Your real history is untouched.");
        return 1;
    }

    private static int openJournal() {
        if (!needDebug("open the journal (test)")) return 0;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        mc.execute(() -> mc.gui.setScreen(new JournalScreen(null)));
        return 1;
    }

    private static int listAchievements() {
        if (!needDebug("list achievements")) return 0;
        StringBuilder b = new StringBuilder("\u00a78[Light] ");
        for (Achievements a : Achievements.values()) {
            b.append(a.isUnlocked(TikiGambleClient.config) ? "\u00a7a" : "\u00a77").append(a.name().toLowerCase(Locale.ROOT)).append("\u00a78, ");
        }
        TikiGambleClient.chat(b.toString());
        return 1;
    }

    /** Debug: removes an achievement (e.g. unlocked by a bug). It comes back if its rule is still met. */
    private static int removeAchievement(String name) {
        if (!needDebug("remove achievements")) return 0;
        Achievements a = Achievements.byName(name);
        if (a == null) {
            TikiGambleClient.chat("\u00a7c[Light] Unknown achievement: " + name + " - see /light achievement list");
            return 0;
        }
        boolean had = Achievements.remove(TikiGambleClient.config, a);
        TikiGambleClient.chat("\u00a7d[Light] \u00a7f" + (had ? "Removed: " : "Locked: ") + "\u00a7d" + a.title
            + " \u00a77- stays locked until \u00a7f/light achievement restore " + a.name().toLowerCase(Locale.ROOT));
        return 1;
    }

    private static int restoreAchievement(String name) {
        if (!needDebug("restore achievements")) return 0;
        Achievements a = Achievements.byName(name);
        if (a == null || !Achievements.restore(TikiGambleClient.config, a)) {
            TikiGambleClient.chat("\u00a7c[Light] Not removed: " + name);
            return 0;
        }
        TikiGambleClient.chat("\u00a7d[Light] \u00a7f" + a.title + " can be unlocked again.");
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
