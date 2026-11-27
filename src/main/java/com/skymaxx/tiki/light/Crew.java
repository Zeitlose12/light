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
    public static final String[] LINES = {"Standard", "Aurora", "Blitz", "Flamme", "Frost", "Galaxie", "Goldkette",
        "Regenbogen", "Seelenfeuer", "Kirschbl\u00fcte", "Ender", "Candy", "Gift"};
    public static final String[] BOBBERS = {"Standard", "Leucht-Orb", "Stern", "Mini-Tiki", "Feuerball", "Eiskristall",
        "K\u00fcrbis", "Diamant", "Totenkopf", "Ender-Perle", "Herz", "Mini-Boa"};
    private static final String[] BOBBER_FILES = {null, "orb", "star", "tiki", "fireball", "ice", "pumpkin", "diamond", "skull", "pearl", "heart", "boa"};

    public static Identifier bobberTexture(int b) {
        return Identifier.fromNamespaceAndPath("light", "textures/entity/bobber/" + BOBBER_FILES[b] + ".png");
    }

    /** Texture size in pixels (the Mini-Tiki is drawn finer). */
    public static int bobberSize(int b) {
        return b == 3 ? 32 : 16;
    }
    private static final RenderType[] BOBBER_TYPES = new RenderType[BOBBER_FILES.length];
    private static final String URL = "https://raw.githubusercontent.com/Zeitlose12/light/main/crew.json";
    private static final long REFRESH_MS = 5L * 60L * 1000L;
    /** Grundsteuer (Creator) - the only one who can use /light crew (together with his own GitHub token). */
    public static final String DEV_UUID = "48cedfb2c84b472fb99087e1e0bab713";
    private static final String BETA_UUID = "20e1f4c8145d46499f9dc35f85dfdd6b";

    /** uuid (32 hex, no dashes) -> member. Built in, then updated from crew.json. */
    private static final Map<String, Member> MEMBERS = new ConcurrentHashMap<>();
    private static volatile Object lastLevel;
    private static volatile long lastWorldFetch;
    private static volatile long fetchedAt;
    private static volatile boolean fetching;

    /** Set while the bobber / line of a crew member is drawn (render thread only). */
    public static int bobberNow;
    public static int lineNow;

    /** role: "Creator", "Beta & ideas" (built in) or "Supporter" / "Beta" (crew.json). */
    public record Member(String uuid, String name, String role, int line, int bobber) {
        /** Creator and Beta get every theme; Supporters only the line and bobber looks. */
        public boolean allThemes() {
            return !"Supporter".equalsIgnoreCase(this.role);
        }

        public boolean builtIn() {
            return DEV_UUID.equals(this.uuid) || BETA_UUID.equals(this.uuid);
        }
    }

    static {
        MEMBERS.put(DEV_UUID, new Member(DEV_UUID, "Grundsteuer", "Creator", 0, 0));
        MEMBERS.put(BETA_UUID, new Member(BETA_UUID, "Gesundheitskarte", "Beta & ideas", 0, 0));
    }

    public static String key(java.util.UUID id) {
        return id == null ? "" : id.toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    public static String key(String uuid) {
        return uuid == null ? "" : uuid.replace("-", "").toLowerCase(Locale.ROOT);
    }

    private Crew() {
    }

    private static TikiConfig cfg() {
        return TikiGambleClient.config;
    }

    /** By name - only for the party chat banner (chat lines carry no UUID). */
    public static Member byName(String name) {
        if (name == null) return null;
        for (Member m : MEMBERS.values()) if (m.name().equalsIgnoreCase(name)) return m;
        return null;
    }

    public static Member byUuid(String uuid) {
        return MEMBERS.get(key(uuid));
    }

    /** The account playing on this Minecraft, or null. */
    public static Member local() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getUser() == null) return null;
            return MEMBERS.get(key(mc.getUser().getProfileId()));
        } catch (Throwable t) {
            return null;
        }
    }

    /** The player using this Minecraft is in the Light Crew (any role). */
    public static boolean isLocalCrew() {
        return local() != null;
    }

    /** Creator and Beta: every theme unlocked. */
    public static boolean localAllThemes() {
        Member m = local();
        return m != null && m.allThemes();
    }

    /** Grundsteuer's account (checked by UUID). */
    public static boolean isLocalDev() {
        Member m = local();
        return m != null && DEV_UUID.equals(m.uuid());
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
            Member m = MEMBERS.get(key(owner.getUUID()));
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
            BOBBER_TYPES[b] = RenderTypes.entityCutoutCull(bobberTexture(b));
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
            case 7 -> 0xFF000000 | hsb(t + time * 0.33f, 0.9f, 1f);
            case 8 -> 0xFF000000 | hsb(0.51f + 0.03f * (float) Math.sin(t * 18f - time * 8f), 0.85f, 0.85f + 0.15f * (float) Math.sin(time * 10f + t * 9f));
            case 9 -> 0xFF000000 | hsb(0.93f, 0.45f + 0.1f * (float) Math.sin(t * 10f - time * 2f), 1f);
            case 10 -> 0xFF000000 | hsb(0.79f, 0.8f, 0.45f + 0.25f * (float) Math.sin(t * 14f + time * 3f));
            case 11 -> ((int) Math.floor(t * 20f - time * 4f) & 1) == 0 ? 0xFFE8323C : 0xFFFFFFFF;
            case 12 -> 0xFF000000 | hsb(0.28f, 0.85f, 0.6f + 0.25f * (float) Math.sin(t * 16f - time * 5f));
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

    // ------------------------------------------------------------------ particles

    private static int particleTick;

    /** Particles along crew lines and above crew bobbers. Client side only; never on the water where a bite shows. */
    private static void particles(Minecraft mc) {
        TikiConfig c = cfg();
        if (c == null || !c.crewParticles || mc.level == null || mc.player == null || mc.isPaused()) return;
        if (LavaScene.ownsScreen()) return;
        particleTick++;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.projectile.FishingHook hook)) continue;
            Player owner = hook.getPlayerOwner();
            if (owner == null) continue;
            int[] st = styleFor(owner);
            if (st[0] == 0 && st[1] == 0) continue;
            net.minecraft.world.phys.Vec3 h = hook.position().add(0, 0.25, 0);
            if (h.distanceToSqr(mc.player.position()) > 48 * 48) continue;
            boolean firstPerson = owner == mc.player && mc.options.getCameraType().isFirstPerson();
            if (st[0] != 0 && particleTick % 3 == 0) {
                net.minecraft.world.phys.Vec3 eye = owner.getEyePosition(1f);
                net.minecraft.world.phys.Vec3 look = owner.getLookAngle();
                net.minecraft.world.phys.Vec3 tip = eye.add(look.scale(1.2)).add(0, -0.35, 0);
                net.minecraft.world.phys.Vec3 d = tip.subtract(h);
                // close to your own camera the particles would get in the way
                float u = (float) Math.random() * (firstPerson ? 0.7f : 0.95f) + 0.05f;
                double px = h.x + d.x * u, py = h.y + d.y * (u * u + u) * 0.5, pz = h.z + d.z * u;
                lineParticle(mc, st[0], px, py, pz, u);
            }
            if (st[1] != 0 && particleTick % 6 == 0) {
                double a = Math.random() * Math.PI * 2;
                bobberParticle(mc, st[1], h.x + Math.cos(a) * 0.25, h.y + 0.3 + Math.random() * 0.25, h.z + Math.sin(a) * 0.25);
            }
        }
    }

    private static void add(Minecraft mc, net.minecraft.core.particles.ParticleOptions p, double x, double y, double z, double vx, double vy, double vz) {
        mc.level.addParticle(p, x, y, z, vx, vy, vz);
    }

    private static net.minecraft.core.particles.DustParticleOptions dust(int rgb, float size) {
        return new net.minecraft.core.particles.DustParticleOptions(rgb, size);
    }

    private static void lineParticle(Minecraft mc, int line, double x, double y, double z, float u) {
        float time = (System.currentTimeMillis() % 1_000_000L) / 1000f;
        switch (line) {
            case 1 -> add(mc, dust(hsb(0.42f + (float) Math.random() * 0.3f, 0.8f, 1f), 0.6f), x, y, z, 0, 0.01, 0);
            case 2 -> add(mc, net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
            case 3 -> add(mc, net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.005, 0);
            case 4 -> add(mc, net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, x, y, z, 0, -0.01, 0);
            case 5 -> add(mc, dust(Math.random() < 0.6 ? 0xFFFFFF : 0xC8A0FF, 0.45f), x, y, z, 0, 0, 0);
            case 6 -> add(mc, dust(0xFFE08A, 0.5f), x, y, z, 0, 0, 0);
            case 7 -> add(mc, dust(hsb(u + time * 0.33f, 0.9f, 1f), 0.6f), x, y, z, 0, 0.01, 0);
            case 8 -> add(mc, net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.005, 0);
            case 9 -> add(mc, net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES, x, y, z, 0, 0, 0);
            case 10 -> add(mc, net.minecraft.core.particles.ParticleTypes.PORTAL, x, y, z, 0, 0, 0);
            case 11 -> add(mc, dust(Math.random() < 0.5 ? 0xFF8090 : 0xFFFFFF, 0.55f), x, y, z, 0, 0.01, 0);
            case 12 -> add(mc, dust(0x7DFF4A, 0.6f), x, y, z, 0, -0.03, 0);
            default -> {
            }
        }
    }

    private static void bobberParticle(Minecraft mc, int b, double x, double y, double z) {
        switch (b) {
            case 1 -> add(mc, dust(0xFF8CFF, 0.6f), x, y, z, 0, 0.01, 0);
            case 2 -> add(mc, dust(0xFFD85A, 0.6f), x, y, z, 0, 0, 0);
            case 3 -> add(mc, dust(0xFFC600, 0.6f), x, y, z, 0, 0.01, 0);
            case 4, 6 -> add(mc, net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.005, 0);
            case 5 -> add(mc, net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, x, y, z, 0, -0.005, 0);
            case 7 -> add(mc, dust(0xBFFFF7, 0.55f), x, y, z, 0, 0, 0);
            case 8 -> add(mc, net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.005, 0);
            case 9 -> add(mc, net.minecraft.core.particles.ParticleTypes.PORTAL, x, y, z, 0, 0, 0);
            case 10 -> add(mc, dust(0xFF8AA0, 0.6f), x, y, z, 0, 0.01, 0);
            case 11 -> add(mc, dust(0x7DFF4A, 0.55f), x, y, z, 0, 0.01, 0);
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ crew.json

    /** Called every client tick: reloads crew.json every 30 minutes (only if crew looks are shown). */
    private static int sightTick;

    public static void tick() {
        try {
            Minecraft mc0 = Minecraft.getInstance();
            if (mc0.level != null && mc0.player != null && ++sightTick % 40 == 0) {
                for (Player p : mc0.level.players()) {
                    if (p != mc0.player && MEMBERS.containsKey(key(p.getUUID()))) {
                        Secrets.flag("CREW_SIGHTING");
                        break;
                    }
                }
            }
            particles(mc0);
        } catch (Throwable t) {
            TikiGambleClient.LOGGER.debug("[Light] crew particles failed", t);
        }
        TikiConfig c = cfg();
        if (c == null || !c.crewCosmetics || fetching) return;
        long now = System.currentTimeMillis();
        Object level = Minecraft.getInstance().level;
        boolean newWorld = level != null && level != lastLevel;
        lastLevel = level;
        // also on joining a world / changing lobby (at most once a minute)
        if (newWorld && now - lastWorldFetch > 60_000L) {
            lastWorldFetch = now;
        } else if (fetchedAt != 0L && now - fetchedAt < REFRESH_MS) {
            return;
        }
        fetching = true;
        fetchedAt = now;
        Thread th = new Thread(() -> {
            try {
                java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).build();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(URL))
                    .timeout(java.time.Duration.ofSeconds(8))
                    .header("User-Agent", "Light-Mod (Fabric, crew looks)").header("Cache-Control", "no-cache").GET().build();
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

    /**
     * { "members": [ { "uuid": "...", "name": "...", "role": "Supporter", "line": "Aurora", "bobber": "Stern" } ] }
     * Entries need a uuid (names can change hands). The built-in Creator / Beta keep their role.
     * Members removed from the file lose their looks at the next reload.
     */
    public static void read(String body) {
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonObject() || !root.getAsJsonObject().has("members")) return;
        Map<String, Member> fresh = new java.util.HashMap<>();
        for (JsonElement e : root.getAsJsonObject().getAsJsonArray("members")) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String uuid = key(str(o, "uuid"));
            String name = str(o, "name");
            if (!uuid.matches("[0-9a-f]{32}") || name == null || !name.matches("\\w{3,16}")) continue;
            String role = str(o, "role");
            if (role == null || role.equalsIgnoreCase("Creator")) role = "Supporter";
            Member old = MEMBERS.get(uuid);
            if (old != null && old.builtIn()) role = old.role();
            fresh.put(uuid, new Member(uuid, name, role, index(LINES, str(o, "line")), index(BOBBERS, str(o, "bobber"))));
        }
        for (Member m : MEMBERS.values()) {
            if (m.builtIn()) fresh.putIfAbsent(m.uuid(), new Member(m.uuid(), m.name(), m.role(), 0, 0));
        }
        MEMBERS.clear();
        MEMBERS.putAll(fresh);
    }

    /** Line / bobber by name; ASCII spellings work too (Kirschbluete, Kuerbis). */
    public static int lineIndex(String v) {
        return index(LINES, v);
    }

    public static int bobberIndex(String v) {
        return index(BOBBERS, v);
    }

    public static String ascii(String s) {
        return s.replace("\u00fc", "ue").replace("\u00e4", "ae").replace("\u00f6", "oe").replace("\u00dc", "Ue").replace(" ", "");
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : null;
    }

    private static int index(String[] names, String v) {
        if (v != null) for (int i = 0; i < names.length; i++) {
            if (names[i].equalsIgnoreCase(v) || ascii(names[i]).equalsIgnoreCase(ascii(v))) return i;
        }
        return 0;
    }

    // ------------------------------------------------------------------ party banner

    private static final Pattern PARTY = Pattern.compile("^Party > (?:\\[[^\\]]+\\] )?(\\w{3,16}): (.+)$");

    /** A crew member names a big drop in party chat -> a short Light Crew banner for everyone in the party with Light. */
    public static void onChat(Component message, boolean overlay) {
        if (overlay || message == null) return;
        TikiConfig c = cfg();
        if (c == null || !c.crewBanner) return;
        String text = message.getString().replaceAll("§.", "").trim();
        Matcher m = PARTY.matcher(text);
        if (!m.matches()) return;
        Member who = byName(m.group(1));
        if (who == null) return;
        // any drop from the gambling boards that Light prices (bazaar or auction house)
        String drop = LavaPrices.boardDropIn(m.group(2));
        if (drop != null) CosmeticPopup.crewDrop(who.name(), drop);
    }
}
