package com.skymaxx.tiki.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.anim.AnimationDirector;
import com.skymaxx.tiki.anim.Prize;
import com.skymaxx.tiki.boa.BoaDirector;
import com.skymaxx.tiki.boa.BoaPrize;
import com.skymaxx.tiki.config.DropItem;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.config.TikiStats;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

public final class SkyMaxxCommand {
    private static volatile boolean openRequested;
    private static final float MAX_DELAY = 60.0f;
    private static final Random PREVIEW_DICE;

    private SkyMaxxCommand() {
    }

    public static boolean consumeOpenRequest() {
        if (openRequested) {
            openRequested = false;
            return true;
        }
        return false;
    }

    public static void register(AnimationDirector director, BoaDirector boa, TikiConfig cfg, TikiStats stats) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal((String)"skymaxx").executes(c -> SkyMaxxCommand.open((FabricClientCommandSource)c.getSource()))).then(ClientCommands.literal((String)"tiki").executes(c -> {
                cfg.tikiEnabled = !cfg.tikiEnabled;
                cfg.save();
                return SkyMaxxCommand.say((FabricClientCommandSource)c.getSource(), cfg.tikiEnabled ? "\u00a7aThe tiki totem is on." : "\u00a7cThe tiki totem is off.");
            }))).then(ClientCommands.literal((String)"titanoboa").executes(c -> {
                cfg.titanoboaEnabled = !cfg.titanoboaEnabled;
                cfg.save();
                return SkyMaxxCommand.say((FabricClientCommandSource)c.getSource(), cfg.titanoboaEnabled ? "\u00a7aThe Titanoboa rifle is on." : "\u00a7cThe Titanoboa rifle is off.");
            }))).then(ClientCommands.literal((String)"reload").executes(c -> {
                cfg.load();
                return SkyMaxxCommand.say((FabricClientCommandSource)c.getSource(), "\u00a76Config reloaded from disk.");
            }))).then(ClientCommands.literal((String)"droplog").executes(c -> {
                List<String> lines = TikiGambleClient.dropLog();
                if (lines.isEmpty()) {
                    return SkyMaxxCommand.say((FabricClientCommandSource)c.getSource(), "\u00a77Nothing seen yet.");
                }
                for (String line : lines) {
                    SkyMaxxCommand.say((FabricClientCommandSource)c.getSource(), line);
                }
                return 1;
            })));
            dispatcher.register((LiteralArgumentBuilder)ClientCommands.literal((String)"light").executes(c -> SkyMaxxCommand.open((FabricClientCommandSource)c.getSource())));
            dispatcher.register((LiteralArgumentBuilder)ClientCommands.literal((String)"sm").executes(c -> SkyMaxxCommand.open((FabricClientCommandSource)c.getSource())));
            dispatcher.register((LiteralArgumentBuilder)ClientCommands.literal((String)"tiki").executes(c -> SkyMaxxCommand.open((FabricClientCommandSource)c.getSource())));
            dispatcher.register((LiteralArgumentBuilder)ClientCommands.literal((String)"tikipreview").executes(c -> SkyMaxxCommand.previewTiki((FabricClientCommandSource)c.getSource(), director, boa)));
            dispatcher.register((LiteralArgumentBuilder)ClientCommands.literal((String)"boapreview").executes(c -> SkyMaxxCommand.previewBoaKill((FabricClientCommandSource)c.getSource(), boa, director)));
            LiteralArgumentBuilder test = (LiteralArgumentBuilder)ClientCommands.literal((String)"tikitest").executes(c -> {
                if (!SkyMaxxCommand.allowed((FabricClientCommandSource)c.getSource())) {
                    return 0;
                }
                director.beginTest(Prize.MASK);
                return SkyMaxxCommand.say((FabricClientCommandSource)c.getSource(), "\u00a76Running a full-totem preview.");
            });
            for (Prize prize : Prize.values()) {
                LiteralArgumentBuilder word = (LiteralArgumentBuilder)ClientCommands.literal((String)SkyMaxxCommand.tikiWord(prize)).executes(c -> {
                    if (!SkyMaxxCommand.allowed((FabricClientCommandSource)c.getSource())) {
                        return 0;
                    }
                    director.beginTest(prize);
                    String line = prize.announcement();
                    return SkyMaxxCommand.say((FabricClientCommandSource)c.getSource(), "\u00a77Preview: \u00a76" + (line != null ? line : SkyMaxxCommand.describe(prize)));
                });
                if (prize.dropped()) {
                    for (Prize also : Prize.values()) {
                        if (!also.dropped() || also == prize) continue;
                        word = (LiteralArgumentBuilder)word.then(ClientCommands.literal((String)SkyMaxxCommand.tikiWord(also)).executes(c -> SkyMaxxCommand.previewTwoDrops((FabricClientCommandSource)c.getSource(), director, boa, prize, also)));
                    }
                }
                test = (LiteralArgumentBuilder)test.then((ArgumentBuilder)word);
            }
            test = (LiteralArgumentBuilder)test.then(ClientCommands.literal((String)"scare").executes(c -> {
                if (!SkyMaxxCommand.allowed((FabricClientCommandSource)c.getSource())) {
                    return 0;
                }
                if (boa.owningScreen()) {
                    return SkyMaxxCommand.say((FabricClientCommandSource)c.getSource(), "\u00a7cThe Titanoboa is on screen; try again when it has finished.");
                }
                director.beginTestScare();
                return 1;
            }));
            LiteralArgumentBuilder tikiDouble = ClientCommands.literal((String)"double");
            for (Prize firstPrize : Prize.values()) {
                LiteralArgumentBuilder branch = ClientCommands.literal((String)SkyMaxxCommand.tikiWord(firstPrize));
                for (Prize secondPrize : Prize.values()) {
                    branch = (LiteralArgumentBuilder)branch.then(((LiteralArgumentBuilder)ClientCommands.literal((String)SkyMaxxCommand.tikiWord(secondPrize)).executes(c -> SkyMaxxCommand.previewPair((FabricClientCommandSource)c.getSource(), director, boa, firstPrize, secondPrize, 0.0f))).then(ClientCommands.argument((String)"seconds", (ArgumentType)FloatArgumentType.floatArg((float)0.0f, (float)60.0f)).executes(c -> SkyMaxxCommand.previewPair((FabricClientCommandSource)c.getSource(), director, boa, firstPrize, secondPrize, FloatArgumentType.getFloat((CommandContext)c, (String)"seconds")))));
                }
                tikiDouble = (LiteralArgumentBuilder)tikiDouble.then((ArgumentBuilder)branch);
            }
            test = (LiteralArgumentBuilder)test.then((ArgumentBuilder)tikiDouble);
            dispatcher.register((LiteralArgumentBuilder)test.then(ClientCommands.argument((String)"masks", (ArgumentType)IntegerArgumentType.integer((int)0, (int)4)).executes(c -> {
                int n;
                if (!SkyMaxxCommand.allowed((FabricClientCommandSource)c.getSource())) {
                    return 0;
                }
                n = IntegerArgumentType.getInteger((CommandContext)c, (String)"masks");
                director.beginTest(n, n >= 4 ? Prize.MASK : Prize.NONE);
                return SkyMaxxCommand.say((FabricClientCommandSource)c.getSource(), "\u00a76Preview with \u00a7e" + n + "\u00a76/4 masks.");
            })));
            LiteralArgumentBuilder boaTest = (LiteralArgumentBuilder)ClientCommands.literal((String)"boatest").executes(c -> SkyMaxxCommand.previewBoa((FabricClientCommandSource)c.getSource(), boa, director, BoaPrize.SHED, 0));
            for (BoaPrize prize : BoaPrize.values()) {
                LiteralArgumentBuilder word = (LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommands.literal((String)SkyMaxxCommand.boaWord(prize)).executes(c -> SkyMaxxCommand.previewBoa((FabricClientCommandSource)c.getSource(), boa, director, prize, 0))).then(ClientCommands.argument((String)"bullets", (ArgumentType)IntegerArgumentType.integer((int)5, (int)8)).executes(c -> SkyMaxxCommand.previewBoa((FabricClientCommandSource)c.getSource(), boa, director, prize, IntegerArgumentType.getInteger((CommandContext)c, (String)"bullets"))));
                if (prize.dropped()) {
                    for (BoaPrize also : BoaPrize.values()) {
                        if (!also.dropped() || also == prize) continue;
                        word = (LiteralArgumentBuilder)word.then(((LiteralArgumentBuilder)ClientCommands.literal((String)SkyMaxxCommand.boaWord(also)).executes(c -> SkyMaxxCommand.previewBoaDrops((FabricClientCommandSource)c.getSource(), boa, director, prize, also, 0))).then(ClientCommands.argument((String)"bullets", (ArgumentType)IntegerArgumentType.integer((int)5, (int)8)).executes(c -> SkyMaxxCommand.previewBoaDrops((FabricClientCommandSource)c.getSource(), boa, director, prize, also, IntegerArgumentType.getInteger((CommandContext)c, (String)"bullets")))));
                    }
                }
                boaTest = (LiteralArgumentBuilder)boaTest.then((ArgumentBuilder)word);
            }
            LiteralArgumentBuilder boaDouble = ClientCommands.literal((String)"double");
            for (BoaPrize firstPrize : BoaPrize.values()) {
                LiteralArgumentBuilder branch = ClientCommands.literal((String)SkyMaxxCommand.boaWord(firstPrize));
                for (BoaPrize secondPrize : BoaPrize.values()) {
                    branch = (LiteralArgumentBuilder)branch.then(((LiteralArgumentBuilder)ClientCommands.literal((String)SkyMaxxCommand.boaWord(secondPrize)).executes(c -> SkyMaxxCommand.previewBoaPair((FabricClientCommandSource)c.getSource(), boa, director, firstPrize, secondPrize, 0.0f))).then(ClientCommands.argument((String)"seconds", (ArgumentType)FloatArgumentType.floatArg((float)0.0f, (float)60.0f)).executes(c -> SkyMaxxCommand.previewBoaPair((FabricClientCommandSource)c.getSource(), boa, director, firstPrize, secondPrize, FloatArgumentType.getFloat((CommandContext)c, (String)"seconds")))));
                }
                boaDouble = (LiteralArgumentBuilder)boaDouble.then((ArgumentBuilder)branch);
            }
            dispatcher.register((LiteralArgumentBuilder)boaTest.then((ArgumentBuilder)boaDouble));
            dispatcher.register(SkyMaxxCommand.drops(stats));
        });
    }

    private static int open(FabricClientCommandSource source) {
        openRequested = true;
        return 1;
    }

    private static int previewTiki(FabricClientCommandSource source, AnimationDirector totem, BoaDirector boa) {
        if (SkyMaxxCommand.busy(source, totem, boa)) {
            return 0;
        }
        ArrayList<Prize> rolled = new ArrayList<Prize>();
        for (Prize prize : Prize.values()) {
            if (!prize.dropped() || !(PREVIEW_DICE.nextDouble() < prize.dropRate())) continue;
            rolled.add(prize);
        }
        totem.beginPreview(rolled);
        return SkyMaxxCommand.say(source, "\u00a77Preview: a Wiki Tiki kill, rolled like a real one. Nothing is counted.");
    }

    private static int previewBoaKill(FabricClientCommandSource source, BoaDirector boa, AnimationDirector totem) {
        if (SkyMaxxCommand.busy(source, totem, boa)) {
            return 0;
        }
        ArrayList<BoaPrize> rolled = new ArrayList<BoaPrize>();
        for (BoaPrize prize : BoaPrize.values()) {
            if (!prize.dropped() || !(PREVIEW_DICE.nextDouble() < prize.dropRate())) continue;
            rolled.add(prize);
        }
        boa.beginPreview(rolled);
        return SkyMaxxCommand.say(source, "\u00a77Preview: a Titanoboa kill, rolled like a real one. Nothing is counted.");
    }

    private static boolean busy(FabricClientCommandSource source, AnimationDirector totem, BoaDirector boa) {
        if (totem.owningScreen() || boa.owningScreen()) {
            SkyMaxxCommand.say(source, "\u00a7cA reveal is on screen; try again when it has finished.");
            return true;
        }
        return false;
    }

    private static String boaWord(BoaPrize prize) {
        return switch (prize) {
            default -> throw new MatchException(null, null);
            case BoaPrize.NONE -> "none";
            case BoaPrize.LILY_PAD -> "lilypad";
            case BoaPrize.BOBBIN -> "bobbin";
            case BoaPrize.SHED -> "shed";
            case BoaPrize.SNAKE_EYES -> "snakeeyes";
            case BoaPrize.AQUAMARINE -> "aquamarine";
        };
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> drops(TikiStats stats) {
        LiteralArgumentBuilder root = (LiteralArgumentBuilder)ClientCommands.literal((String)"skymaxxdrops").executes(c -> SkyMaxxCommand.show((FabricClientCommandSource)c.getSource(), stats));
        for (boolean adding : new boolean[]{true, false}) {
            int sign = adding ? 1 : -1;
            LiteralArgumentBuilder verb = ClientCommands.literal((String)(adding ? "add" : "remove"));
            for (DropItem item : DropItem.values()) {
                verb = (LiteralArgumentBuilder)verb.then(((LiteralArgumentBuilder)ClientCommands.literal((String)item.word()).executes(c -> SkyMaxxCommand.move((FabricClientCommandSource)c.getSource(), stats, item, sign))).then(ClientCommands.argument((String)"amount", (ArgumentType)IntegerArgumentType.integer((int)1, (int)1000000)).executes(c -> SkyMaxxCommand.move((FabricClientCommandSource)c.getSource(), stats, item, sign * IntegerArgumentType.getInteger((CommandContext)c, (String)"amount")))));
            }
            root = (LiteralArgumentBuilder)root.then((ArgumentBuilder)verb);
        }
        return root;
    }

    private static int move(FabricClientCommandSource source, TikiStats stats, DropItem item, int delta) {
        int before = stats.counted(item);
        int now = stats.adjust(item, delta);
        int moved = now - before;
        if (moved == 0) {
            return SkyMaxxCommand.say(source, "\u00a77" + item.title() + "\u00a78 is already on \u00a770\u00a78; nothing to remove.");
        }
        return SkyMaxxCommand.say(source, (moved > 0 ? "\u00a7a+" : "\u00a7c") + moved + " \u00a77" + item.title() + "\u00a78 - you are now on \u00a76" + now + "\u00a78.");
    }

    private static int show(FabricClientCommandSource source, TikiStats stats) {
        SkyMaxxCommand.say(source, "\u00a76Your drops:");
        for (DropItem item : DropItem.values()) {
            SkyMaxxCommand.say(source, "\u00a78 - \u00a77" + item.plural() + "\u00a78: \u00a76" + stats.counted(item));
        }
        return SkyMaxxCommand.say(source, "\u00a78/skymaxxdrops add|remove <item> [amount] to put one right.");
    }

    private static String tikiWord(Prize prize) {
        return switch (prize) {
            case Prize.NONE -> "none";
            case Prize.MASK -> "mask";
            default -> prize.faceAsset();
        };
    }

    private static int previewPair(FabricClientCommandSource source, AnimationDirector totem, BoaDirector boa, Prize first, Prize second, float delay) {
        if (!SkyMaxxCommand.allowed(source)) {
            return 0;
        }
        if (boa.owningScreen()) {
            return SkyMaxxCommand.say(source, "\u00a7cThe Titanoboa is on screen; try again when it has finished.");
        }
        totem.beginTestPair(first, second, delay);
        return SkyMaxxCommand.say(source, "\u00a77Preview: two totems - \u00a76" + SkyMaxxCommand.describe(first) + "\u00a77 and \u00a76" + SkyMaxxCommand.describe(second) + "\u00a77" + SkyMaxxCommand.after(delay) + ".");
    }

    private static int previewTwoDrops(FabricClientCommandSource source, AnimationDirector totem, BoaDirector boa, Prize a, Prize b) {
        if (!SkyMaxxCommand.allowed(source)) {
            return 0;
        }
        if (boa.owningScreen()) {
            return SkyMaxxCommand.say(source, "\u00a7cThe Titanoboa is on screen; try again when it has finished.");
        }
        totem.beginTestDrops(a, b);
        return SkyMaxxCommand.say(source, "\u00a77Preview: one Wiki Tiki that dropped \u00a76" + SkyMaxxCommand.describe(a) + "\u00a77 and \u00a76" + SkyMaxxCommand.describe(b) + "\u00a77.");
    }

    private static String after(float delay) {
        if (delay <= 0.0f) {
            return ", together";
        }
        return ", the second " + String.format(Locale.ROOT, "%.1f", Float.valueOf(delay)).replace(".0", "") + "s after the first";
    }

    private static String describe(Prize prize) {
        return prize.dropped() ? prize.title() : "nothing";
    }

    private static int previewBoa(FabricClientCommandSource source, BoaDirector boa, AnimationDirector totem, BoaPrize prize, int bullets) {
        if (!SkyMaxxCommand.allowed(source)) {
            return 0;
        }
        if (totem.owningScreen()) {
            return SkyMaxxCommand.say(source, "\u00a7cThe totem is on screen; try again when it has finished.");
        }
        boa.beginTest(prize, bullets);
        String what = prize.dropped() ? prize.title() : "a kill that dropped nothing";
        return SkyMaxxCommand.say(source, "\u00a77Preview: \u00a76" + what + (String)(bullets > 0 ? "\u00a77 with \u00a7e" + bullets + "\u00a77 bullets." : "\u00a77."));
    }

    private static int previewBoaPair(FabricClientCommandSource source, BoaDirector boa, AnimationDirector totem, BoaPrize first, BoaPrize second, float delay) {
        if (!SkyMaxxCommand.allowed(source)) {
            return 0;
        }
        if (totem.owningScreen()) {
            return SkyMaxxCommand.say(source, "\u00a7cThe totem is on screen; try again when it has finished.");
        }
        boa.beginTestPair(first, second, delay);
        return SkyMaxxCommand.say(source, "\u00a77Preview: two Titanoboas - \u00a76" + (first.dropped() ? first.title() : "nothing") + "\u00a77 and \u00a76" + (second.dropped() ? second.title() : "nothing") + "\u00a77" + SkyMaxxCommand.after(delay) + ".");
    }

    private static int previewBoaDrops(FabricClientCommandSource source, BoaDirector boa, AnimationDirector totem, BoaPrize a, BoaPrize b, int bullets) {
        if (!SkyMaxxCommand.allowed(source)) {
            return 0;
        }
        if (totem.owningScreen()) {
            return SkyMaxxCommand.say(source, "\u00a7cThe totem is on screen; try again when it has finished.");
        }
        boa.beginTestDrops(a, b, bullets);
        return SkyMaxxCommand.say(source, "\u00a77Preview: one Titanoboa that dropped \u00a76" + a.title() + "\u00a77 and \u00a76" + b.title() + (String)(bullets > 0 ? "\u00a77 with \u00a7e" + bullets + "\u00a77 bullets." : "\u00a77."));
    }

    private static boolean allowed(FabricClientCommandSource source) {
        if (TikiGambleClient.canTest()) {
            return true;
        }
        SkyMaxxCommand.say(source, "\u00a7cYou do not have permission to use this command. \u00a77Try \u00a76/tikipreview\u00a77 or \u00a76/boapreview\u00a77.");
        return false;
    }

    private static int say(FabricClientCommandSource source, String message) {
        source.sendFeedback((Component)Component.literal((String)message));
        return 1;
    }

    static {
        PREVIEW_DICE = new Random();
    }
}

