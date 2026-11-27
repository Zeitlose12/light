package com.skymaxx.tiki.light;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

/**
 * Light: /light crew add | remove | look | sync | list - only for Grundsteuer.
 * Two locks: the command only exists for his account (UUID), and it only works with his own GitHub token in
 * config/light/crew_token.txt (fine-grained, contents of the light repo only). The token never leaves his PC
 * except to api.github.com, is never written to the log or chat, and is not part of the mod.
 */
public final class CrewAdmin {
    private static final String API = "https://api.github.com/repos/Zeitlose12/light/contents/crew.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile boolean busy;

    private CrewAdmin() {
    }

    public static Path tokenFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(LightFiles.FOLDER).resolve("crew_token.txt");
    }

    /** The /light crew command exists only for the dev account. */
    public static boolean available() {
        return Crew.isLocalDev();
    }

    private static String token() {
        try {
            Path f = tokenFile();
            if (!Files.isRegularFile(f)) return null;
            String t = Files.readString(f, StandardCharsets.UTF_8).trim();
            int nl = t.indexOf('\n');
            if (nl >= 0) t = t.substring(0, nl).trim();
            return t.matches("[A-Za-z0-9_]{20,255}") ? t : null;
        } catch (Throwable e) {
            return null;
        }
    }

    private static void say(String msg) {
        Minecraft.getInstance().execute(() -> TikiGambleClient.chat(msg));
    }

    private interface Change {
        /** Changes the member list; returns the commit message, or null when there is nothing to change. */
        String apply(JsonArray members) throws Exception;
    }

    private static boolean ready() {
        if (!available()) return false;
        if (token() == null) {
            TikiGambleClient.chat("§c[Light] No GitHub token. Put it into §fconfig/light/crew_token.txt§c (one line, github_pat_...).");
            return false;
        }
        if (busy) {
            TikiGambleClient.chat("§7[Light] Still working on the last crew change...");
            return false;
        }
        return true;
    }

    private static void run(String what, Change change) {
        busy = true;
        Thread th = new Thread(() -> {
            try {
                for (int attempt = 0; attempt < 2; attempt++) {
                    int code = commit(change);
                    if (code == 409 && attempt == 0) continue; // someone changed the file at the same moment: once more
                    if (code != 0) say(error(code));
                    break;
                }
            } catch (Throwable t) {
                TikiGambleClient.LOGGER.warn("[Light] crew change failed: " + t.getClass().getSimpleName());
                say("§c[Light] " + what + " failed: " + t.getClass().getSimpleName());
            } finally {
                busy = false;
            }
        }, "Light crew admin");
        th.setDaemon(true);
        th.start();
    }

    private static String error(int code) {
        return switch (code) {
            case 401 -> "§c[Light] Token wrong or expired §8(401)§c - make a new one on GitHub.";
            case 403 -> "§c[Light] Token may not write §8(403)§c - Contents must be \"Read and write\".";
            case 404 -> "§c[Light] Repo or crew.json not found §8(404)§c - is \"Zeitlose12/light\" selected for the token?";
            case 422 -> "§c[Light] GitHub refused the change §8(422)§c - try again.";
            case -1 -> "§7[Light] Nothing to change.";
            default -> "§c[Light] GitHub answered " + code + " - try again in a minute.";
        };
    }

    private static HttpClient http() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    }

    private static HttpRequest.Builder gh(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer " + token())
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "Light-Mod crew admin");
    }

    /** Reads crew.json from GitHub, applies the change, writes it back. 0 = done, else an HTTP code (-1 = nothing to do). */
    private static int commit(Change change) throws Exception {
        HttpClient http = http();
        HttpResponse<String> got = http.send(gh(API + "?ref=main").GET().build(), HttpResponse.BodyHandlers.ofString());
        if (got.statusCode() != 200) return got.statusCode();
        JsonObject file = JsonParser.parseString(got.body()).getAsJsonObject();
        String sha = file.get("sha").getAsString();
        String text = new String(Base64.getMimeDecoder().decode(file.get("content").getAsString()), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(text).getAsJsonObject();
        JsonArray members = root.has("members") ? root.getAsJsonArray("members") : new JsonArray();
        String message = change.apply(members);
        if (message == null) return -1;
        root.add("members", members);
        String out = GSON.toJson(root) + "\n";
        JsonObject put = new JsonObject();
        put.addProperty("message", message);
        put.addProperty("content", Base64.getEncoder().encodeToString(out.getBytes(StandardCharsets.UTF_8)));
        put.addProperty("sha", sha);
        put.addProperty("branch", "main");
        HttpResponse<String> res = http.send(gh(API).PUT(HttpRequest.BodyPublishers.ofString(put.toString())).header("Content-Type", "application/json").build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200 && res.statusCode() != 201) return res.statusCode();
        Minecraft.getInstance().execute(() -> Crew.read(out));
        return 0;
    }

    /** Mojang: name -> {uuid (32 hex), exact name}, or null. */
    private static String[] profile(String name) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
            .timeout(Duration.ofSeconds(8)).header("User-Agent", "Light-Mod crew admin").GET().build();
        HttpResponse<String> res = http().send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) return null;
        JsonObject o = JsonParser.parseString(res.body()).getAsJsonObject();
        return new String[]{Crew.key(o.get("id").getAsString()), o.get("name").getAsString()};
    }

    private static JsonObject find(JsonArray members, String uuid, String name) {
        for (JsonElement e : members) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String u = o.has("uuid") ? Crew.key(o.get("uuid").getAsString()) : "";
            String n = o.has("name") ? o.get("name").getAsString() : "";
            if ((uuid != null && uuid.equals(u)) || (name != null && name.equalsIgnoreCase(n))) return o;
        }
        return null;
    }

    private static boolean validName(String name) {
        if (name != null && name.matches("\\w{3,16}")) return true;
        TikiGambleClient.chat("§c[Light] Not a Minecraft name: " + name);
        return false;
    }

    // ------------------------------------------------------------------ commands

    public static int add(String name, String role) {
        if (!ready() || !validName(name)) return 0;
        String r = role == null || role.equalsIgnoreCase("supporter") ? "Supporter" : role.equalsIgnoreCase("beta") ? "Beta" : null;
        if (r == null) {
            TikiGambleClient.chat("§c[Light] Role: supporter or beta");
            return 0;
        }
        TikiGambleClient.chat("§7[Light] Adding " + name + " as " + r + "...");
        run("Adding " + name, members -> {
            String[] p = profile(name);
            if (p == null) {
                say("§c[Light] Mojang does not know the name " + name + ".");
                return null;
            }
            if (p[0].equals(Crew.DEV_UUID)) {
                say("§7[Light] That's you - you are the Creator.");
                return null;
            }
            JsonObject o = find(members, p[0], null);
            if (o == null) {
                o = new JsonObject();
                o.addProperty("uuid", p[0]);
                o.addProperty("name", p[1]);
                o.addProperty("role", r);
                o.addProperty("line", "Standard");
                o.addProperty("bobber", "Standard");
                members.add(o);
            } else {
                o.addProperty("name", p[1]);
                o.addProperty("role", r);
            }
            say("§d[Light] §f" + p[1] + " §7is now §d" + r + "§7. Their Light picks it up within ~5 minutes (no restart).");
            return "Crew: " + p[1] + " is " + r;
        });
        return 1;
    }

    public static int remove(String name) {
        if (!ready() || !validName(name)) return 0;
        Crew.Member known = Crew.byName(name);
        if (known != null && known.builtIn()) {
            TikiGambleClient.chat("§c[Light] " + known.name() + " is built into Light and stays in the crew.");
            return 0;
        }
        TikiGambleClient.chat("§7[Light] Removing " + name + "...");
        run("Removing " + name, members -> {
            JsonObject o = find(members, null, name);
            if (o == null) {
                String[] p = profile(name);
                if (p != null) o = find(members, p[0], null);
            }
            if (o == null) {
                say("§7[Light] " + name + " is not in the crew.");
                return null;
            }
            members.remove(o);
            say("§d[Light] §f" + name + " §7was removed from the crew.");
            return "Crew: removed " + name;
        });
        return 1;
    }

    /** Sets the line / bobber others see for this member. */
    public static int look(String name, String line, String bobber) {
        if (!ready() || !validName(name)) return 0;
        int l = Crew.lineIndex(line), b = Crew.bobberIndex(bobber);
        if (l == 0 && !line.equalsIgnoreCase("standard") || b == 0 && !bobber.equalsIgnoreCase("standard")) {
            TikiGambleClient.chat("§c[Light] Unknown line or bobber - use the suggestions (Tab).");
            return 0;
        }
        run("Changing the look of " + name, members -> {
            JsonObject o = find(members, null, name);
            if (o == null) {
                Crew.Member m = Crew.byName(name);
                if (m == null) {
                    say("§7[Light] " + name + " is not in the crew - add them first.");
                    return null;
                }
                o = new JsonObject();
                o.addProperty("uuid", m.uuid());
                o.addProperty("name", m.name());
                o.addProperty("role", m.role());
                members.add(o);
            }
            o.addProperty("line", Crew.LINES[l]);
            o.addProperty("bobber", Crew.BOBBERS[b]);
            say("§d[Light] §f" + o.get("name").getAsString() + "§7: " + Crew.LINES[l] + " + " + Crew.BOBBERS[b] + " - others see it within ~5 minutes.");
            return "Crew: look of " + o.get("name").getAsString();
        });
        return 1;
    }

    /** Publishes your own line / bobber from the Cosmetics tab, so other Light users see it. */
    public static int sync() {
        TikiConfig c = TikiGambleClient.config;
        Crew.Member me = Crew.local();
        if (c == null || me == null) return 0;
        return look(me.name(), Crew.ascii(Crew.LINES[Math.max(0, Math.min(Crew.LINES.length - 1, c.crewLine))]),
            Crew.ascii(Crew.BOBBERS[Math.max(0, Math.min(Crew.BOBBERS.length - 1, c.crewBobber))]));
    }

    public static int list() {
        if (!available()) return 0;
        java.util.List<Crew.Member> all = Crew.members();
        all.sort((a, b) -> rank(a) != rank(b) ? rank(a) - rank(b) : a.name().compareToIgnoreCase(b.name()));
        TikiGambleClient.chat("§8§m          §r §d§lLight Crew §7(" + all.size() + ") §8§m          ");
        for (Crew.Member m : all) {
            String col = switch (rank(m)) {
                case 0 -> "§6";
                case 1 -> "§d";
                default -> "§b";
            };
            String icon = switch (rank(m)) {
                case 0 -> "★";
                case 1 -> "◆";
                default -> "●";
            };
            String role = rank(m) == 1 ? "Beta" : m.role();
            TikiGambleClient.chat(col + icon + " §l" + m.name() + "§r  " + col + role);
            TikiGambleClient.chat("   §7Line: §f" + Crew.LINES[m.line()] + " §8· §7Bobber: §f" + Crew.BOBBERS[m.bobber()]);
        }
        if (token() == null) TikiGambleClient.chat("§7[Light] No token yet: config/light/crew_token.txt");
        return 1;
    }

    /** 0 Creator, 1 Beta, 2 Supporter. */
    private static int rank(Crew.Member m) {
        if (Crew.DEV_UUID.equals(m.uuid())) return 0;
        return m.allThemes() ? 1 : 2;
    }

    public static String[] lineNames() {
        String[] out = new String[Crew.LINES.length];
        for (int i = 0; i < out.length; i++) out[i] = Crew.ascii(Crew.LINES[i]).toLowerCase(Locale.ROOT);
        return out;
    }

    public static String[] bobberNames() {
        String[] out = new String[Crew.BOBBERS.length];
        for (int i = 0; i < out.length; i++) out[i] = Crew.ascii(Crew.BOBBERS[i]).toLowerCase(Locale.ROOT);
        return out;
    }
}
