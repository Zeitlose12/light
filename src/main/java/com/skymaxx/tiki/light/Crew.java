package com.skymaxx.tiki.light;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Light Crew: the people who build and test Light (Grundsteuer, Gesundheitskarte).
 * They get every theme and can pick a fishing line and a bobber look. Purely visual and client side:
 * only players with Light installed see it, nothing is sent to Hypixel. The looks of crew members are
 * read from crew.json in Light's GitHub repo (one small download every 30 minutes, can be switched off).
 */
public final class Crew {
    public static final String[] LINES = {"Standard", "Aurora", "Blitz", "Flamme", "Frost", "Galaxie", "Goldkette"};
    public static final String[] BOBBERS = {"Standard", "Leucht-Orb", "Stern", "Mini-Tiki", "Feuerball", "Eiskristall"};
    private static final String[] BOBBER_FILES = {null, "orb", "star", "tiki", "fireball", "ice"};
    private static final RenderType[] BOBBER_TYPES = new RenderType[BOBBER_FILES.length];
    private static final String URL = "https://raw.githubusercontent.com/Zeitlose12/light/main/crew.json";
    private static final long REFRESH_MS = 30L * 60L * 1000L;

    /** name (lower case) -> member. Built in, then updated from crew.json. */
    private static final Map<String, Member> MEMBERS = new ConcurrentHashMap<>();
    private static volatile long fetchedAt;
    private static volatile boolean fetching;

    /** Set while the bobber / line of a crew member is drawn (render thread only). */
    public static int bobberNow;
    public static int lineNow;

    public record Member(String name, String role, int line, int bobber) {
    }

    static {
        MEMBERS.put("grundsteuer", new Member("Grundsteuer", "Creator", 0, 0));
        MEMBERS.put("gesundheitskarte", new Member("Gesundheitskarte", "Beta & ideas", 0, 0));
    }

    private Crew() {
    }

    private static TikiConfig cfg() {
        return TikiGambleClient.config;
    }

    public static boolean isMember(String name) {
        return name != null && MEMBERS.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /** The player using this Minecraft is in the Light Crew. */
    public static boolean isLocalCrew() {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc != null && mc.getUser() != null && isMember(mc.getUser().getName());
        } catch (Throwable t) {
            return false;
        }
    }

    public static java.util.List<Member> members() {
        java.util.List<Member> out = new java.util.ArrayList<>(MEMBERS.values());
        out.sort((a, b) -> "Creator".equals(a.role()) ? -1 : "Creator".equals(b.role()) ? 1 : a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    /** {line, bobber} for this fishing hook's owner; {0, 0} = the normal look. */
    public static int[] styleFor(Player owner) {
        if (owner == null) return new int[]{0, 0};
        try {
            Minecraft mc = Minecraft.getInstance();
            TikiConfig c = cfg();
            if (c == null) return new int[]{0, 0};
            if (owner == mc.player) {
                if (!isLocalCrew()) return new int[]{0, 0};
                return new int[]{clamp(c.crewLine, LINES.length), clamp(c.crewBobber, BOBBERS.length)};
            }
            if (!c.crewCosmetics) return new int[]{0, 0};
            Member m = MEMBERS.get(owner.getName().getString().toLowerCase(Locale.ROOT));
            if (m == null) return new int[]{0, 0};
            return new int[]{clamp(m.line(), LINES.length), clamp(m.bobber(), BOBBERS.length)};
        } catch (Throwable t) {
            return new int[]{0, 0};
        }
    }

    private static int clamp(int v, int n) {
        return v < 0 || v >= n ? 0 : v;
    }

    public static RenderType bobberType(int b) {
        if (b <= 0 || b >= BOBBER_FILES.length) return null;
        if (BOBBER_TYPES[b] == null) {
            BOBBER_TYPES[b] = RenderTypes.entityCutoutCull(Identifier.fromNamespaceAndPath("light", "textures/entity/bobber/" + BOBBER_FILES[b] + ".png"));
        }
        return BOBBER_TYPES[b];
    }

    /** Colour of the fishing line at t (0 = bobber, 1 = rod). */
    public static int lineColor(int line, float t) {
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000f;
        int seg = (int) (t * 16f + 0.001f);
        return switch (line) {
            case 1 -> 0xFF000000 | hsb(0.42f + 0.18f * (float) Math.sin(t * 6.283f - time * 1.3f) + 0.08f * (float) Math.sin(time * 0.4f), 0.85f, 1f);
            case 2 -> {
                boolean flash = hash(seg * 31 + (int) (time * 14f)) > 0.82f;
                float pulse = 0.75f + 0.25f * (float) Math.sin(time * 9f + t * 20f);
                yield flash ? 0xFFFFFFFF : 0xFF000000 | hsb(0.55f, 0.55f, pulse);
            }
            case 3 -> 0xFF000000 | hsb(0.02f + 0.07f * (0.5f + 0.5f * (float) Math.sin(t * 22f - time * 9f)), 1f, 0.9f + 0.1f * (float) Math.sin(time * 13f + t * 7f));
            case 4 -> hash(seg * 17 + (int) (time * 5f)) > 0.8f ? 0xFFFFFFFF : 0xFFA8E8FF;
            case 5 -> hash(seg * 13 + (int) (time * 3f)) > 0.88f ? 0xFFFFFFFF
                : 0xFF000000 | hsb(0.70f + 0.12f * (float) Math.sin(t * 9f + time), 0.75f, 0.55f + 0.25f * (float) Math.sin(t * 5f - time * 0.7f));
            case 6 -> {
                float shine = (float) Math.pow(Math.max(0f, (float) Math.sin((t - time * 0.5f) * 6.283f)), 8);
                float link = 0.5f + 0.5f * (float) Math.sin(t * 50.27f);
                int r = (int) (160 + 64 * link + 31 * shine), g = (int) (115 + 61 * link + 70 * shine), b = (int) (20 + 28 * link + 150 * shine);
                yield 0xFF000000 | Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
            }
            default -> 0xFF000000;
        };
    }

    /** HSB to RGB without AWT (hue wraps around). */
    public static int hsb(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        v = Math.max(0f, Math.min(1f, v));
        float c = v * s, x = c * (1 - Math.abs((h * 6f) % 2f - 1)), m = v - c;
        float r, g, b;
        int k = (int) (h * 6f);
        switch (k) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }

    private static float hash(int i) {
        double v = Math.sin(i * 12.9898 + 78.233) * 43758.5453;
        return (float) (v - Math.floor(v));
    }

    // ------------------------------------------------------------------ crew.json

    /** Called every client tick: reloads crew.json every 30 minutes (only if crew looks are shown). */
    public static void tick() {
        TikiConfig c = cfg();
        if (c == null || !c.crewCosmetics || fetching) return;
        long now = System.currentTimeMillis();
        if (fetchedAt != 0L && now - fetchedAt < REFRESH_MS) return;
        fetching = true;
        fetchedAt = now;
        Thread th = new Thread(() -> {
            try {
                java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(URL))
                    .timeout(java.time.Duration.ofSeconds(8))
                    .header("User-Agent", "Light-Mod (Fabric, crew looks)").GET().build();
                java.net.http.HttpResponse<String> res = http.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) read(res.body());
            } catch (Throwable t) {
                TikiGambleClient.LOGGER.debug("[Light] crew.json not loaded", t);
            } finally {
                fetching = false;
            }
        }, "Light crew");
        th.setDaemon(true);
        th.start();
    }

    /** { "members": [ { "name": "...", "role": "...", "line": "Aurora", "bobber": "Stern" } ] } */
    static void read(String body) {
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonObject() || !root.getAsJsonObject().has("members")) return;
        for (JsonElement e : root.getAsJsonObject().getAsJsonArray("members")) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String name = str(o, "name");
            if (name == null || !name.matches("\\w{3,16}")) continue;
            String role = str(o, "role");
            MEMBERS.put(name.toLowerCase(Locale.ROOT), new Member(name, role == null ? "Crew" : role, index(LINES, str(o, "line")), index(BOBBERS, str(o, "bobber"))));
        }
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : null;
    }

    private static int index(String[] names, String v) {
        if (v != null) for (int i = 0; i < names.length; i++) if (names[i].equalsIgnoreCase(v)) return i;
        return 0;
    }

    // ------------------------------------------------------------------ party banner

    private static final Pattern PARTY = Pattern.compile("^Party > (?:\\[[^\\]]+\\] )?(\\w{3,16}): (.+)$");
    private static final String[] BIG = {
        "Aquamarine Dye", "Iceberg Dye", "Carmine Dye", "Midnight Dye", "Radioactive Vial", "Flash", "Magma Lord Fragment",
        "Bobbin' Scriptures", "Burnt Texts", "Tiki Mask", "Titanoboa Shed", "Snake Eyes", "Deep Sea Orb", "Baby Yeti",
        "True Ice", "Chain of the End Times", "Brimstone Handle", "Troubled Bubble", "Great White Shark Tooth", "Vampire Fang"};

    /** A crew member names a big drop in party chat -> a short Light Crew banner for everyone in the party with Light. */
    public static void onChat(Component message, boolean overlay) {
        if (overlay || message == null) return;
        TikiConfig c = cfg();
        if (c == null || !c.crewBanner) return;
        String text = message.getString().replaceAll("§.", "").trim();
        Matcher m = PARTY.matcher(text);
        if (!m.matches() || !isMember(m.group(1))) return;
        String said = m.group(2).toLowerCase(Locale.ROOT);
        for (String big : BIG) {
            if (said.contains(big.toLowerCase(Locale.ROOT))) {
                CosmeticPopup.crewDrop(MEMBERS.get(m.group(1).toLowerCase(Locale.ROOT)).name(), big);
                return;
            }
        }
    }
}
