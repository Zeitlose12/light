package com.skymaxx.tiki.light.dye;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skymaxx.tiki.TikiGambleClient;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;

/**
 * Light: optional hook into the "SkyBlock Profile Viewer" mod (meowdding).
 * When you open /pv, that mod hands the loaded profile to every mod that registered the
 * "skyblockapi:imc/pv-profile" entrypoint (the same way DyeAddons does it).
 * Light itself sends nothing anywhere - it only reads what the Profile Viewer already loaded.
 *
 * player_stats.kills = kills that were yours, bestiary.kills = all kills incl. loot share.
 * Mob ids follow the NEU repo (constants/bestiary.json).
 */
public final class PvBridge {
    /** Light creature name -> Hypixel API mob ids (without the _level suffix). */
    private static final Map<String, List<String>> IDS = new LinkedHashMap<String, List<String>>();

    static {
        id("Squid", "pond_squid");
        id("Sea Walker", "sea_walker");
        id("Frog Man", "frog_man");
        id("Trash Gobbler", "trash_gobbler");
        id("Atoll Croaker", "atoll_croaker");
        id("Bogged", "bogged");
        id("Haggard", "haggard");
        id("Frozen Steve", "frozen_steve");
        id("Jumpin' Jack", "sea_guardian"); // NEU bestiary: sea_guardian_10
        id("Sea Witch", "sea_witch");
        id("Sea Archer", "sea_archer");
        id("Rider of the Deep", "chicken_deep");
        id("Inkling", "night_squid"); // NEU bestiary: night_squid_6
        id("Oasis Sheep", "oasis_sheep");
        id("Oasis Rabbit", "oasis_rabbit");
        id("Dumpster Diver", "dumpster_diver");
        id("Lotus Guardian", "lotus_guardian");
        id("Nurse Shark", "nurse_shark");
        id("Wetwing", "wetwing_chicken");
        id("Brineling", "brineling");
        id("Frosty", "frosty_the_snowman");
        id("Scarecrow", "scarecrow");
        id("Catfish", "catfish");
        id("Sea Leech", "sea_leech");
        id("Snapping Turtle", "snapping_turtle");
        id("Water Worm", "water_worm");
        id("Poisoned Water Worm", "poisoned_water_worm");
        id("Banshee", "banshee");
        id("gorF", "gorf");
        id("Blue Shark", "blue_shark");
        id("Tadgang", "tadgang_frog");
        id("Carrot King", "carrot_king");
        id("Agarimoo", "agarimoo");
        id("Sprawl", "sprawl");
        id("Grinch", "grinch");
        id("Nightmare", "nightmare");
        id("Guardian Defender", "guardian_defender");
        id("Deep Sea Protector", "deep_sea_protector");
        id("Manta Ray", "manta_ray");
        id("Bayou Sludge", "bayou_sludge", "baby_bayou_sludge");
        id("Drowned Captain", "drowned_captain");
        id("Tiger Shark", "tiger_shark");
        id("Ent", "ent");
        id("Torrid", "torrid");
        id("Nutcracker", "nutcracker");
        id("Werewolf", "werewolf");
        id("Water Hydra", "water_hydra");
        id("Blue Ringed Octopus", "blue_ringed_octopus");
        id("Abyssal Miner", "zombie_miner");
        id("Alligator", "alligator");
        id("Puddle Jumper", "puddle_jumper");
        id("Great White Shark", "great_white_shark");
        id("The Loch Emperor", "guardian_emperor", "skeleton_emperor");
        id("Silkbreeze", "silkbreeze");
        id("Yeti", "yeti");
        id("Phantom Fisher", "phantom_fisherman");
        id("Wiki Tiki", "wiki_tiki");
        id("Titanoboa", "titanoboa");
        id("Frog Prince", "frog_prince");
        id("Nesse", "nessie");
        id("Giant Isopod", "giant_isopod");
        id("Grim Reaper", "grim_reaper");
        id("Reindrake", "reindrake");
        id("Fried Chicken", "fried_chicken");
        id("Magma Slug", "magma_slug", "baby_magma_slug");
        id("Moogma", "moogma");
        id("Volcanic Snail", "volcanic_snail");
        id("Lava Leech", "lava_leech");
        id("Pyroclastic Worm", "pyroclastic_worm");
        id("Lava Flame", "lava_flame");
        id("Fire Eel", "fire_eel");
        id("Fireproof Witch", "fireproof_witch");
        id("Flaming Worm", "flaming_worm");
        id("Stridersurfer", "strider");
        id("Taurus", "pig_rider");
        id("Magma Pillar", "magma_pillar");
        id("Lava Blaze", "lava_blaze");
        id("Lava Pigman", "lava_pigman");
        id("Thunder", "thunder");
        id("Fiery Scuttler", "fiery_scuttler");
        id("Plhlegblast", "plhlegblast");
        id("Lord Jawbus", "lord_jawbus");
        id("Ragnarok", "ragnarok");
    }

    /** Creatures whose API id is a guess - /light pvcheck reports them. */
    private static final List<String> GUESSED = List.of("Jumpin' Jack", "Inkling", "Nesse", "Stridersurfer", "Puddle Jumper", "The Loch Emperor");
    private static final List<String> HINTS = List.of("jack", "jump", "ink", "nes", "strider", "surf");
    private static volatile java.util.Set<String> lastIds = java.util.Set.of();
    private static volatile java.util.Set<String> lastFound = java.util.Set.of();

    private PvBridge() {
    }

    /** /light pvcheck - works without the debug switch, shows only what matters. */
    public static int check() {
        if (lastIds.isEmpty()) {
            TikiGambleClient.chat("\u00a7d[Light] \u00a77No Profile Viewer data yet - open /pv for your own profile first.");
            return 1;
        }
        TikiGambleClient.chat("\u00a7d[Light] \u00a77Profile Viewer check:");
        for (String c : GUESSED) {
            TikiGambleClient.chat((lastFound.contains(c) ? " \u00a7a\u2714 " : " \u00a7e? ") + "\u00a7f" + c + " \u00a78(" + String.join("/", IDS.get(c)) + ")");
        }
        java.util.HashSet<String> known = new java.util.HashSet<String>();
        IDS.values().forEach(known::addAll);
        java.util.TreeSet<String> hints = new java.util.TreeSet<String>();
        for (String id : lastIds) {
            if (known.contains(id)) continue;
            for (String h : HINTS) {
                if (id.contains(h)) {
                    hints.add(id);
                }
            }
        }
        TikiGambleClient.chat(" \u00a77Unknown ids that look related: \u00a7f" + (hints.isEmpty() ? "none" : String.join(", ", hints)));
        TikiGambleClient.LOGGER.info("[Light] pvcheck unknown ids: {}", hints);
        return 1;
    }

    private static void id(String creature, String... ids) {
        IDS.put(creature, List.of(ids));
    }

    /** "sea_walker_15" -> "sea_walker"; Plhlegblast is stored as pond_squid_300. */
    static String base(String key) {
        if (key.equals("pond_squid_300")) {
            return "plhlegblast";
        }
        return key.replaceAll("_\\d+$", "");
    }

    private static Map<String, Long> sums(JsonObject kills) {
        HashMap<String, Long> out = new HashMap<String, Long>();
        if (kills == null) {
            return out;
        }
        for (Map.Entry<String, JsonElement> e : kills.entrySet()) {
            JsonElement v = e.getValue();
            if (v == null || !v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) continue;
            out.merge(PvBridge.base(e.getKey()), (long)Math.max(0.0, v.getAsDouble()), Long::sum);
        }
        return out;
    }

    private static JsonObject obj(JsonObject o, String... path) {
        JsonObject cur = o;
        for (String p : path) {
            if (cur == null || !cur.has(p) || !cur.get(p).isJsonObject()) {
                return null;
            }
            cur = cur.getAsJsonObject(p);
        }
        return cur;
    }

    /** Entrypoint "skyblockapi:imc/pv-profile" - called by the Profile Viewer with the loaded profile. */
    public static void onPvLoad(JsonObject profile) {
        try {
            Minecraft mc = Minecraft.getInstance();
            UUID uuid = mc.getUser() == null ? null : mc.getUser().getProfileId();
            if (uuid == null || profile == null) {
                return;
            }
            JsonObject member = PvBridge.obj(profile, "members", uuid.toString().replace("-", ""));
            if (member == null) {
                return; // someone else's profile
            }
            String name = profile.has("cute_name") && profile.get("cute_name").isJsonPrimitive() ? profile.get("cute_name").getAsString() : "?";
            Map<String, Long> own = PvBridge.sums(PvBridge.obj(member, "player_stats", "kills"));
            Map<String, Long> all = PvBridge.sums(PvBridge.obj(member, "bestiary", "kills"));
            HashMap<String, long[]> split = new HashMap<String, long[]>();
            for (Map.Entry<String, List<String>> e : IDS.entrySet()) {
                long o = 0L;
                long a = 0L;
                for (String id : e.getValue()) {
                    o += own.getOrDefault(id, 0L);
                    a += all.getOrDefault(id, 0L);
                }
                if (o > 0L || a > 0L) {
                    split.put(e.getKey(), new long[]{o, Math.max(0L, a - o)});
                }
            }
            lastIds = new java.util.HashSet<String>(all.keySet());
            lastFound = new java.util.HashSet<String>(split.keySet());
            if (TikiGambleClient.config != null && TikiGambleClient.config.debug) {
                java.util.HashSet<String> known = new java.util.HashSet<String>();
                IDS.values().forEach(known::addAll);
                for (String k : all.keySet()) {
                    if (!known.contains(k)) TikiGambleClient.LOGGER.info("[Light] pv: unmapped bestiary id {}", k);
                }
            }
            mc.execute(() -> DyeTracker.applyProfileViewer(name, split));
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.warn("[Light] could not read Profile Viewer data", t);
        }
    }
}
