package com.skymaxx.tiki.light.dye;

import com.skymaxx.tiki.TikiGambleClient;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.Entity;

/**
 * Light: live sea creature kills, the way DyeAddons does it - with one difference:
 * the catch messages are read straight from the chat packet, before any other mod can
 * hide or rewrite them (FEESH and other custom catch messages).
 *
 *  1. Your catch message ("A Squid appeared.") puts that creature on your "mine" list.
 *     "It's a Double Hook!" doubles the next one, Water Hydra counts 2, Fiery Scuttler 6.
 *  2. A kill = the creature's nametag drops to 0 HP / disappears together with Hypixel's
 *     kill sound (experience orb pickup, pitch 1.4920635), within 20 blocks.
 *  3. Killed creature on your list -> your kill (full chance). Otherwise -> loot share (1/5).
 *  Tadgang's Lv1 tadpoles don't roll for dyes, so they are skipped.
 *
 * Catch messages: SkyHanni-REPO constants/SeaCreatures.json. Read-only, nothing is sent.
 */
public final class DyeKills {
    private static final Map<String, String> CATCH = new HashMap<String, String>();
    private static final Pattern LEVEL = Pattern.compile("Lv\\s*(\\d+)");
    private static final Pattern NUMBER = Pattern.compile("^\\s*([0-9][0-9.,]*)([kKmMbB]?)");
    private static final double KILL_RADIUS_SQ = 20.0 * 20.0;
    private static final int SOUND_WINDOW = 3;
    private static final long MINE_TTL_MS = 60000L;

    static {
        msg("Agarimoo", "Your Chumcap Bucket trembles, it's an Agarimoo.");
        msg("Carrot King", "Is this even a fish? It's the Carrot King!");
        msg("Squid", "A Squid appeared.");
        msg("Sea Walker", "You caught a Sea Walker.");
        msg("Sea Archer", "You reeled in a Sea Archer.");
        msg("Rider of the Deep", "The Rider of the Deep has emerged.");
        msg("Sea Witch", "It looks like you've disrupted the Sea Witch's brewing session. Watch out, she's furious!");
        msg("Catfish", "Huh? A Catfish!");
        msg("Sea Leech", "Gross! A Sea Leech!");
        msg("Guardian Defender", "You've discovered a Guardian Defender of the sea.");
        msg("Deep Sea Protector", "You have awoken the Deep Sea Protector, prepare for a battle!");
        msg("Water Hydra", "The Water Hydra has come to test your strength.");
        msg("Frozen Steve", "Frozen Steve fell into the pond long ago, never to resurface...until now!");
        msg("Frosty", "It's a snowman! He looks harmless.");
        msg("Grinch", "The Grinch stole Jerry's Gifts...get them back!");
        msg("Nutcracker", "You found a forgotten Nutcracker laying beneath the ice.");
        msg("Yeti", "What is this creature!?");
        msg("Reindrake", "A Reindrake forms from the depths.");
        msg("Scarecrow", "Phew! It's only a Scarecrow.");
        msg("Nightmare", "You hear trotting from beneath the waves, you caught a Nightmare.");
        msg("Werewolf", "It must be a full moon, a Werewolf appears.");
        msg("Phantom Fisher", "The spirit of a long lost Phantom Fisher has come to haunt you.");
        msg("Grim Reaper", "This can't be! The manifestation of death himself!");
        msg("Jumpin' Jack", "Watch out! It's Jumpin' Jack.");
        msg("Nurse Shark", "A tiny fin emerges from the water, you've caught a Nurse Shark.");
        msg("Blue Shark", "You spot a fin as blue as the water it came from, it's a Blue Shark.");
        msg("Tiger Shark", "A striped beast bounds from the depths, the wild Tiger Shark!");
        msg("Great White Shark", "Hide no longer, a Great White Shark has tracked your scent and thirsts for your blood!");
        msg("Oasis Sheep", "An Oasis Sheep appears from the water.");
        msg("Oasis Rabbit", "An Oasis Rabbit appears from the water.");
        msg("Lava Blaze", "A Lava Blaze has surfaced from the depths!");
        msg("Lava Pigman", "A Lava Pigman arose from the depths!");
        msg("Flaming Worm", "A Flaming Worm surfaces from the depths!");
        msg("Water Worm", "A Water Worm surfaces!");
        msg("Poisoned Water Worm", "A Poisoned Water Worm surfaces!");
        msg("Abyssal Miner", "An Abyssal Miner breaks out of the water!");
        msg("Moogma", "You hear a faint Moo from the lava... A Moogma appears.");
        msg("Magma Slug", "From beneath the lava appears a Magma Slug.");
        msg("Pyroclastic Worm", "You feel the heat radiating as a Pyroclastic Worm surfaces.");
        msg("Lava Flame", "A Lava Flame flies out from beneath the lava.");
        msg("Fire Eel", "A Fire Eel slithers out from the depths.");
        msg("Lava Leech", "A small but fearsome Lava Leech emerges.");
        msg("Taurus", "Taurus and his steed emerge.");
        msg("Thunder", "You hear a massive rumble as Thunder emerges.");
        msg("Lord Jawbus", "You have angered a legendary creature... Lord Jawbus has arrived.");
        msg("Plhlegblast", "WOAH! A Plhlegblast appeared.");
        msg("Trash Gobbler", "The Trash Gobbler is hungry for you!");
        msg("Banshee", "The desolate wail of a Banshee breaks the silence.");
        msg("Alligator", "A long snout breaks the surface of the water. It's an Alligator!");
        msg("Dumpster Diver", "A Dumpster Diver has emerged from the swamp!");
        msg("Bayou Sludge", "A swampy mass of slime emerges, the Bayou Sludge!");
        msg("Titanoboa", "A massive Titanoboa surfaces. Its body stretches as far as the eye can see.");
        msg("Ragnarok", "The sky darkens and the air thickens. The end times are upon us: Ragnarok is here.");
        msg("Volcanic Snail", "You feel a burning sensation as you reel in a Volcanic Snail!");
        msg("Fireproof Witch", "Trouble's brewing, it's a Fireproof Witch!");
        msg("Fried Chicken", "Smells of burning. Must be a Fried Chicken.");
        msg("Magma Pillar", "A Magma Pillar rises from the lava.");
        msg("Fiery Scuttler", "A Fiery Scuttler inconspicuously waddles up to you, friends in tow.");
        msg("Wiki Tiki", "The water bubbles and froths. A massive form emerges- you have disturbed the Wiki Tiki! You shall pay the price.");
        msg("Blue Ringed Octopus", "A garish set of tentacles arise. It's a Blue Ringed Octopus!");
        msg("Snapping Turtle", "A Snapping Turtle is coming your way, and it's ANGRY!");
        msg("Frog Man", "Is it a frog? Is it a man? Well, yes, sorta, IT'S FROG MAN!!!!!!");
        msg("Inkling", "You get an inkling that you've caught... an Inkling!");
        msg("Manta Ray", "A majestic creature rises from the water. It's a Manta Ray.");
        msg("Wetwing", "Look! A Wetwing emerges!");
        msg("Ent", "You've hooked an Ent, as ancient as the forest itself.");
        msg("Tadgang", "A gang of Liltads!");
        msg("Bogged", "You've hooked a Bogged!");
        msg("Stridersurfer", "You caught a Stridersurfer.");
        msg("The Loch Emperor", "The Loch Emperor arises from the depths.");
        msg("Nessie", "You've caused a disturbance in the loch. Could it be... Nessie?");
        msg("Atoll Croaker", "An inquisitive Atoll Croaker takes the bait!");
        msg("Lotus Guardian", "A Lotus Guardian emerges, ready to protect the Atoll.");
        msg("gorF", "What even is that?! A... gorF?");
        msg("Drowned Captain", "A Drowned Captain takes hold of your bobber!");
        msg("Puddle Jumper", "A Puddle Jumper is preparing for liftoff—cast your rod into it and hold on tight!");
        msg("Frog Prince", "Bow down before the Frog Prince... or pay the hefty price!");
        msg("Haggard", "A Haggard stumbles to the shore, ready for a fight!");
        msg("Brineling", "A Brineling interrupts you with a stream of bubbles!");
        msg("Sprawl", "A Sprawl emerges from the blue, and it's looking for you!");
        msg("Torrid", "The laughter of a Torrid echoes through the air.");
        msg("Silkbreeze", "Something zips through the air - it's a Silkbreeze!");
        msg("Giant Isopod", "A Giant Isopod was dredged up from the depths!");
    }

    private static void msg(String creature, String message) {
        CATCH.put(message.trim(), creature);
    }

    private static final class Tracked {
        final Entity entity;
        net.minecraft.world.entity.LivingEntity body;
        boolean hpZero;
        final String creature;
        final int level;
        boolean owned;
        final long firstSeenMs;
        int deadTick = -1;
        boolean seen;
        boolean pendingKill;
        double maxHp = -1, lastHp = -1;

        Tracked(Entity entity, String creature, int level, boolean owned, long firstSeenMs) {
            this.entity = entity;
            this.creature = creature;
            this.level = level;
            this.owned = owned;
            this.firstSeenMs = firstSeenMs;
        }
    }

    private record Mine(String creature, long ms) {
    }

    private static final ArrayDeque<Mine> MINE = new ArrayDeque<Mine>();
    private static final Map<Integer, Tracked> TRACKED = new HashMap<Integer, Tracked>();
    private static final Map<String, Long> LAST_KILL = new HashMap<String, Long>();
    private static int tick;
    private static net.minecraft.world.entity.LivingEntity killBody;
    private static int killSoundTick = -100;
    private static boolean doubleHook;
    private static Object lastLevel;

    private DyeKills() {
    }

    private static String strip(String s) {
        return s == null ? "" : s.replaceAll("\u00a7.", "");
    }

    private static boolean debug() {
        return TikiGambleClient.config != null && TikiGambleClient.config.debug;
    }

    // ---- chat (from the packet, before other mods) ----
    public static void onChatPacket(Component content, boolean overlay) {
        if (overlay || content == null) {
            return;
        }
        String text = strip(content.getString()).trim();
        if (text.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (text.startsWith("It's a Double Hook!")) {
            doubleHook = true;
            return;
        }
        if (text.contains("TROPHY FROG! You caught a Puddle Jumper")) {
            DyeKills.kill("Puddle Jumper", !DyeKills.takeMine("Puddle Jumper", now));
            return;
        }
        if (text.startsWith("DEFEATED! A Reindrake was slain")) {
            DyeKills.kill("Reindrake", !DyeKills.takeMine("Reindrake", now));
            return;
        }
        String creature = CATCH.get(text);
        if (creature == null) {
            return;
        }
        int n = 1;
        if (doubleHook) {
            n *= 2;
            doubleHook = false;
        }
        if (creature.equals("Water Hydra")) n *= 2;
        if (creature.equals("Fiery Scuttler")) n *= 6;
        for (int i = 0; i < n; ++i) {
            // the nametag may already be there - then it was ours, not loot share
            Tracked early = null;
            for (Tracked t : TRACKED.values()) {
                if (!t.owned && (t.deadTick < 0 || t.pendingKill) && t.creature.equals(creature) && now - t.firstSeenMs < 2000L) {
                    early = t;
                    break;
                }
            }
            if (early != null) {
                early.owned = true;
            } else {
                MINE.add(new Mine(creature, now));
            }
        }
        if (debug()) TikiGambleClient.LOGGER.info("[Light] catch: {} x{}", creature, n);
    }

    private static boolean takeMine(String creature, long now) {
        MINE.removeIf(m -> now - m.ms() > MINE_TTL_MS);
        Iterator<Mine> it = MINE.iterator();
        while (it.hasNext()) {
            if (it.next().creature().equals(creature)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    // ---- Hypixel's kill sound ----
    public static void onSound(ClientboundSoundPacket packet) {
        try {
            if (Math.abs(packet.getPitch() - 1.4920635f) > 0.001f) return;
            String id = packet.getSound().value().location().getPath();
            if (id.equals("entity.experience_orb.pickup")) {
                killSoundTick = tick;
            }
        } catch (Throwable ignored) {
        }
    }

    // ---- nametags ----
    private static double health(String text, String creature) {
        int i = text.toLowerCase(Locale.ROOT).lastIndexOf(creature.toLowerCase(Locale.ROOT));
        if (i < 0) return -1.0;
        Matcher m = NUMBER.matcher(text.substring(i + creature.length()));
        if (!m.find()) return -1.0;
        try {
            double v = Double.parseDouble(m.group(1).replace(",", ""));
            return switch (m.group(2).toLowerCase(Locale.ROOT)) {
                case "k" -> v * 1_000.0;
                case "m" -> v * 1_000_000.0;
                case "b" -> v * 1_000_000_000.0;
                default -> v;
            };
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }

    public static void tick(Minecraft mc) {
        ++tick;
        if (mc.level == null || mc.player == null) {
            TRACKED.clear();
            MINE.clear();
            lastLevel = null;
            return;
        }
        if (lastLevel != mc.level) {
            lastLevel = mc.level;
            TRACKED.clear();
            MINE.clear();
            doubleHook = false;
        }
        long now = System.currentTimeMillis();
        for (Tracked t : TRACKED.values()) t.seen = false;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!e.hasCustomName() || e.getCustomName() == null) continue;
            Tracked t = TRACKED.get(e.getId());
            String text = strip(e.getCustomName().getString());
            if (t == null) {
                if (!text.contains("Lv")) continue;
                if (e.distanceToSqr(mc.player) > KILL_RADIUS_SQ) continue;
                String creature = DyeOdds.creatureIn(text);
                if (creature == null) continue;
                double hp = DyeKills.health(text, creature);
                if (hp == 0.0) continue; // already dead when we first saw it
                Matcher lv = LEVEL.matcher(text);
                int level = lv.find() ? Integer.parseInt(lv.group(1)) : 0;
                t = new Tracked(e, creature, level, DyeKills.takeMine(creature, now), now);
                t.body = DyeKills.findBody(mc, e);
                TRACKED.put(e.getId(), t);
            }
            t.seen = true;
            double hpNow = DyeKills.health(text, t.creature);
            if (hpNow > 0) {
                t.maxHp = Math.max(t.maxHp, hpNow);
                t.lastHp = hpNow;
            }
            if (t.body == null) t.body = DyeKills.findBody(mc, e);
            if (t.deadTick < 0 && (DyeKills.health(text, t.creature) == 0.0 || DyeKills.bodyDead(t))) {
                t.hpZero = true;
                t.deadTick = tick;
            }
        }
        Iterator<Map.Entry<Integer, Tracked>> it = TRACKED.entrySet().iterator();
        while (it.hasNext()) {
            Tracked t = it.next().getValue();
            if (!t.seen && t.deadTick < 0) {
                // nametag gone: died or despawned. Some big ones (Ragnarok) vanish at once without showing 0 HP -
                // your own creature that was nearly dead and close to you when it vanished counts as killed
                boolean nearlyDead = t.maxHp > 0 && t.lastHp >= 0 && t.lastHp <= t.maxHp * 0.35;
                boolean close = t.entity.distanceToSqr(mc.player) <= KILL_RADIUS_SQ;
                t.hpZero = DyeKills.bodyDead(t) || (t.owned && nearlyDead && close);
                t.deadTick = tick;
            }
            if (t.pendingKill) {
                // died before its catch message arrived (one-shot kills): wait a moment for the message
                if (!t.owned && DyeKills.takeMine(t.creature, now)) t.owned = true;
                if (t.owned || now - t.firstSeenMs >= 1500L) {
                    if (!(t.creature.equals("Tadgang") && t.level == 1)) { killBody = t.body; DyeKills.kill(t.creature, !t.owned); }
                    it.remove();
                }
                continue;
            }
            if (t.deadTick < 0) continue;
            boolean nearSound = killSoundTick >= t.deadTick - SOUND_WINDOW && killSoundTick <= t.deadTick + SOUND_WINDOW;
            boolean inRange = t.entity.distanceToSqr(mc.player) <= KILL_RADIUS_SQ;
            // your own creature: it died, no matter who killed it (like SkyHanni)
            // someone else's: only with Hypixel's kill sound = you took part -> loot share
            if ((t.owned && t.hpZero) || (nearSound && inRange)) {
                if (!t.owned && now - t.firstSeenMs < 1500L) {
                    t.pendingKill = true;
                    continue;
                }
                if (!(t.creature.equals("Tadgang") && t.level == 1)) {
                    killBody = t.body;
                    DyeKills.kill(t.creature, !t.owned);
                }
                it.remove();
            } else if (tick > t.deadTick + SOUND_WINDOW) {
                if (t.owned && t.seen && debug()) {
                    TikiGambleClient.LOGGER.info("[Light] not counted: {} (yours) {} - last kill sound {} ticks away, distance {}",
                        t.creature, t.seen ? "hit 0 HP" : "nametag gone", tick - killSoundTick,
                        String.format("%.1f", Math.sqrt(t.entity.distanceToSqr(mc.player))));
                }
                if (t.owned && !t.seen) {
                    MINE.add(new Mine(t.creature, now)); // left render distance - still yours
                }
                it.remove();
            }
        }
    }

    /** The real mob under a Hypixel nametag: nearest living entity right below it. */
    private static net.minecraft.world.entity.LivingEntity findBody(Minecraft mc, Entity tag) {
        net.minecraft.world.entity.LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == tag || !(e instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            if (e instanceof net.minecraft.world.entity.decoration.ArmorStand || e instanceof net.minecraft.world.entity.player.Player) continue;
            double dx = e.getX() - tag.getX();
            double dz = e.getZ() - tag.getZ();
            double dy = tag.getY() - e.getY();
            if (dx * dx + dz * dz > 2.25 || dy < -1.0 || dy > 5.0) continue;
            double d = dx * dx + dz * dz + dy * dy * 0.1;
            if (d < bestD) {
                bestD = d;
                best = le;
            }
        }
        return best;
    }

    private static boolean bodyDead(Tracked t) {
        return t.body != null && (t.body.isDeadOrDying() || t.body.getHealth() <= 0.0f);
    }

    private static void kill(String creature, boolean lootShare) {
        net.minecraft.world.entity.LivingEntity body = killBody;
        killBody = null;
        long now = System.currentTimeMillis();
        if (creature.equals("Titanoboa") && now - LAST_KILL.getOrDefault(creature, 0L) < 5000L) {
            return; // one Titanoboa, many body parts
        }
        LAST_KILL.put(creature, now);
        DyeTracker.countKill(creature, lootShare);
        if (debug()) TikiGambleClient.LOGGER.info("[Light] kill: {} {}", creature, lootShare ? "(loot share)" : "(yours)");
        com.skymaxx.tiki.light.LavaScene.onKill(creature, lootShare, body);
        com.skymaxx.tiki.light.SharkPopup.onKill(creature, body);
    }
}
