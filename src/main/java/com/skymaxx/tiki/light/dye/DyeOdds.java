package com.skymaxx.tiki.light.dye;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Light: base drop chance of each fishing dye per sea creature (before Magic Find / Vincent).
 * Chances work by sea creature rarity. The bracket tables and the creature -> rarity lists
 * follow DyeAddons by anlg (Apache-2.0, https://modrinth.com/mod/dyeaddons).
 * Hypixel changes these from time to time.
 */
public final class DyeOdds {
    public enum Rarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC }

    /** 1 in N per rarity: COMMON .. MYTHIC. */
    private static final Map<Dye, long[]> BRACKETS = new EnumMap<Dye, long[]>(Dye.class);
    private static final Map<String, Map<Dye, Rarity>> RARITY = new LinkedHashMap<String, Map<Dye, Rarity>>();
    private static final Map<String, String> ALIASES = new LinkedHashMap<String, String>();

    static {
        BRACKETS.put(Dye.AQUAMARINE, new long[]{10_000_000L, 5_000_000L, 2_500_000L, 500_000L, 50_000L, 25_000L});
        BRACKETS.put(Dye.CARMINE, new long[]{10_000_000L, 5_000_000L, 2_500_000L, 500_000L, 50_000L, 25_000L});
        BRACKETS.put(Dye.ICEBERG, new long[]{2_500_000L, 1_000_000L, 500_000L, 100_000L, 25_000L, 10_000L});
        BRACKETS.put(Dye.MIDNIGHT, new long[]{1_000_000L, 1_000_000L, 1_000_000L, 100_000L, 50_000L, 10_000L});

        // Aquamarine - every water sea creature, event ones included
        put(Dye.AQUAMARINE, Rarity.COMMON, "Squid", "Sea Walker", "Frog Man", "Trash Gobbler", "Atoll Croaker", "Bogged",
            "Haggard", "Frozen Steve", "Jumpin' Jack");
        put(Dye.AQUAMARINE, Rarity.UNCOMMON, "Sea Witch", "Sea Archer", "Rider of the Deep", "Inkling", "Oasis Sheep",
            "Oasis Rabbit", "Dumpster Diver", "Lotus Guardian", "Nurse Shark", "Wetwing", "Brineling", "Frosty", "Scarecrow");
        put(Dye.AQUAMARINE, Rarity.RARE, "Catfish", "Sea Leech", "Snapping Turtle", "Water Worm", "Poisoned Water Worm",
            "Banshee", "gorF", "Blue Shark", "Tadgang", "Carrot King", "Agarimoo", "Sprawl", "Grinch", "Nightmare");
        put(Dye.AQUAMARINE, Rarity.EPIC, "Guardian Defender", "Deep Sea Protector", "Manta Ray", "Bayou Sludge",
            "Drowned Captain", "Tiger Shark", "Ent", "Torrid", "Nutcracker", "Werewolf");
        put(Dye.AQUAMARINE, Rarity.LEGENDARY, "Water Hydra", "Blue Ringed Octopus", "Abyssal Miner", "Alligator",
            "Puddle Jumper", "Great White Shark", "The Loch Emperor", "Silkbreeze", "Yeti", "Phantom Fisher");
        put(Dye.AQUAMARINE, Rarity.MYTHIC, "Wiki Tiki", "Titanoboa", "Frog Prince", "Nessie", "Giant Isopod", "Grim Reaper");

        // Carmine - lava sea creatures
        put(Dye.CARMINE, Rarity.COMMON, "Fried Chicken");
        put(Dye.CARMINE, Rarity.UNCOMMON, "Magma Slug", "Moogma", "Volcanic Snail");
        put(Dye.CARMINE, Rarity.RARE, "Lava Leech", "Pyroclastic Worm", "Lava Flame", "Fire Eel", "Fireproof Witch",
            "Flaming Worm", "Stridersurfer");
        put(Dye.CARMINE, Rarity.EPIC, "Taurus", "Magma Pillar", "Lava Blaze", "Lava Pigman");
        put(Dye.CARMINE, Rarity.LEGENDARY, "Thunder", "Fiery Scuttler", "Plhlegblast");
        put(Dye.CARMINE, Rarity.MYTHIC, "Lord Jawbus", "Ragnarok");

        // Iceberg - winter sea creatures
        put(Dye.ICEBERG, Rarity.COMMON, "Frozen Steve");
        put(Dye.ICEBERG, Rarity.UNCOMMON, "Frosty");
        put(Dye.ICEBERG, Rarity.RARE, "Grinch");
        put(Dye.ICEBERG, Rarity.EPIC, "Nutcracker");
        put(Dye.ICEBERG, Rarity.LEGENDARY, "Yeti");
        put(Dye.ICEBERG, Rarity.MYTHIC, "Reindrake");

        // Midnight - Spooky Festival sea creatures
        put(Dye.MIDNIGHT, Rarity.COMMON, "Jumpin' Jack");
        put(Dye.MIDNIGHT, Rarity.UNCOMMON, "Scarecrow");
        put(Dye.MIDNIGHT, Rarity.RARE, "Nightmare");
        put(Dye.MIDNIGHT, Rarity.EPIC, "Werewolf");
        put(Dye.MIDNIGHT, Rarity.LEGENDARY, "Phantom Fisher");
        put(Dye.MIDNIGHT, Rarity.MYTHIC, "Grim Reaper");

        ALIASES.put("Loch Emperor", "The Loch Emperor");
        ALIASES.put("Nesse", "Nessie");
        ALIASES.put("Jumpin Jack", "Jumpin' Jack");
        ALIASES.put("Pyroclasitc Worm", "Pyroclastic Worm");
    }

    private DyeOdds() {
    }

    private static void put(Dye dye, Rarity rarity, String... creatures) {
        for (String c : creatures) {
            RARITY.computeIfAbsent(c, k -> new EnumMap<Dye, Rarity>(Dye.class)).put(dye, rarity);
        }
    }

    private static List<String> names() {
        ArrayList<String> all = new ArrayList<String>(RARITY.keySet());
        all.addAll(ALIASES.keySet());
        return all;
    }

    private static String canon(String known) {
        return ALIASES.getOrDefault(known, known);
    }

    /** Matches a bestiary or nametag name ("Squid 15", "Lord Jawbus XII") to a known creature, or null. */
    public static String creature(String rawName) {
        if (rawName == null) {
            return null;
        }
        String name = rawName.replaceAll("§.", "").trim();
        name = name.replaceAll("\\s+([0-9]+|[IVXLC]+)$", "").trim();
        String lower = name.toLowerCase(Locale.ROOT);
        String best = null;
        for (String known : names()) {
            String k = known.toLowerCase(Locale.ROOT);
            if (k.equals(lower)) {
                return canon(known);
            }
            if (lower.endsWith(" " + k) && (best == null || known.length() > best.length())) {
                best = known;
            }
        }
        return best == null ? null : canon(best);
    }

    /** Finds the longest known creature name inside a nametag like "[Lv100] Lord Jawbus 1.2M/1.2M". */
    public static String creatureIn(String text) {
        if (text == null) {
            return null;
        }
        String t = " " + text.replaceAll("§.", "").replaceAll("[^A-Za-z' ]", " ").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT) + " ";
        String best = null;
        for (String known : names()) {
            if (t.contains(" " + known.toLowerCase(Locale.ROOT) + " ") && (best == null || known.length() > best.length())) {
                best = known;
            }
        }
        return best == null ? null : canon(best);
    }

    public static Rarity rarity(String creature, Dye dye) {
        Map<Dye, Rarity> m = RARITY.get(creature);
        return m == null ? null : m.get(dye);
    }

    /** Base chance (no Magic Find, no Vincent) for one own kill. */
    public static double chance(String creature, Dye dye) {
        Rarity r = DyeOdds.rarity(creature, dye);
        return r == null ? 0.0 : 1.0 / BRACKETS.get(dye)[r.ordinal()];
    }

    /** Loot share rolls at 1/5 of the normal chance. */
    public static double lootShareChance(String creature, Dye dye) {
        return DyeOdds.chance(creature, dye) / 5.0;
    }

    public static List<String> creatures(Dye dye) {
        ArrayList<String> out = new ArrayList<String>();
        for (Map.Entry<String, Map<Dye, Rarity>> e : RARITY.entrySet()) {
            if (e.getValue().containsKey(dye)) {
                out.add(e.getKey());
            }
        }
        return Collections.unmodifiableList(out);
    }
}
