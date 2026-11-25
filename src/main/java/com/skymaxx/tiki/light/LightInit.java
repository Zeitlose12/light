package com.skymaxx.tiki.light;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.gui.SkyMaxxScreen;
import java.util.Locale;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

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
            })))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Achievements.tick();
            if (openRequested) {
                openRequested = false;
                client.gui.setScreen(SkyMaxxScreen.create());
            }
        });
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
