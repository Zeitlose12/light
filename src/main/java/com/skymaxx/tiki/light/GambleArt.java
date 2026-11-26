package com.skymaxx.tiki.light;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.joml.Matrix3x2fStack;

/**
 * Light: drawing helpers for Lava Gambling - real item icons (the drop as Hypixel shows it: player-head
 * skins / item models from the NEU repo; the game loads the skin itself), framed cards, filled discs and
 * wheels, thick glowing lines and lightning. Everything is plain GUI drawing, nothing bundled.
 */
final class GambleArt {
    /** Ragnarok's head (NEU repo bestiary, ragnarok_666). */
    private static final String SKIN_RAGNAROK = "ewogICJ0aW1lc3RhbXAiIDogMTc0MTA5ODExNTMwMCwKICAicHJvZmlsZUlkIiA6ICJhNzdkNmQ2YmFjOWE0NzY3YTFhNzU1NjYxOTllYmY5MiIsCiAgInByb2ZpbGVOYW1lIiA6ICIwOEJFRDUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYThlMWZlMjE0YjcxZjZlYTY5YzU0MWE4NjFjNjRiYWZkYTdiZjliODVkZTVkZDE3YWIyYjZjY2QxZDMyYjAzOSIKICAgIH0KICB9Cn0=";
    private static final String SKIN_RADIOACTIVE_VIAL = "ewogICJ0aW1lc3RhbXAiIDogMTYxMDA0OTczMzc3OSwKICAicHJvZmlsZUlkIiA6ICIxOTI1MjFiNGVmZGI0MjVjODkzMWYwMmE4NDk2ZTExYiIsCiAgInByb2ZpbGVOYW1lIiA6ICJTZXJpYWxpemFibGUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWI1NzRjMDNiMWM4ZjUyNTM1Zjk2YTIwYTQxNzYwMjBiYjI4MGUwYWY1MmZjMTI4NWQzZWNiZDM1MTcyODBjNCIKICAgIH0KICB9Cn0=";
    private static final String UUID_RADIOACTIVE_VIAL = "402a4bac-f180-39c1-afda-c3ebacd70e91";
    private static final String SKIN_DYE_CARMINE = "ewogICJ0aW1lc3RhbXAiIDogMTc0MTE2MDk2ODI0MiwKICAicHJvZmlsZUlkIiA6ICJiMzMwZWE4OWE3MTY0OWZjYTMwZmI5ZjU5MTQ5ZjNjMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUQUxLX0FMT1RfIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzgxNTBiNDJlNjQ5NzEzNzIxN2Q1ZDhmNTY1MDNmOThkOGY1NDljYjAzNGZiZWFlNmZkZDU0MzE1NzA1MThlMzgiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
    private static final String UUID_DYE_CARMINE = "57b5261d-e97f-35af-9f4f-dbf9bb4cf7ec";
    private static final String SKIN_BURNT_TEXTS = "ewogICJ0aW1lc3RhbXAiIDogMTczOTY2NjM2MzI2MSwKICAicHJvZmlsZUlkIiA6ICI2MTU1NTMyOTY2OWM0ZDA5YmFiOGJlNDNkYWUwYTRjMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJmaTAxNSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83OGJkMTg4OWJmOGE2MzVlYzcxZmMwNWIxZWQ1NDg3NjM3NzhlM2QzNzE5MjZkMzlhNjJjMjlkMzUwYzQ4YjQ5IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";
    private static final String UUID_BURNT_TEXTS = "2d03788c-6ce3-3daf-be8f-ff5b6482b828";
    private static final String SKIN_CHAIN_END_TIMES = "ewogICJ0aW1lc3RhbXAiIDogMTczOTY2NDM4ODI2NCwKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzkzZDliYzYyOTRlY2I4NWQ5YzRhZjY2MTM2MDI3ZmFmNjc0MTNmMWRiYWJiZTFiNmRmZWUyODkwODNmYzIwNDkiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
    private static final String UUID_CHAIN_END_TIMES = "9f4b42e7-a7a7-3ea3-bc3f-acc1e5ffe167";

    private static final String SKIN_DYE_MIDNIGHT = "ewogICJ0aW1lc3RhbXAiIDogMTc0MTE2MTIzMjE2NiwKICAicHJvZmlsZUlkIiA6ICI4YmM3MjdlYThjZjA0YWUzYTI4MDVhY2YzNjRjMmQyNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJub3RpbnZlbnRpdmUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWJmYzg1OTYwZjRmZGI1N2JiZmEwMGM1YmE3NmMyMWJhYjFiOWY0ZmI3NDQ2NzNlNWI1ZTM4YjRmM2ExYThhOCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    private static final String SKIN_DYE_ICEBERG = "ewogICJ0aW1lc3RhbXAiIDogMTc0MTE2MTE1OTE2NSwKICAicHJvZmlsZUlkIiA6ICJjNTI3ZWI1ZTQ1MDE0NTRkYWQwMjhmM2Q0M2ZkZGViNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJDYWxsQmFkIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y3MzFjYWZmMjMzNTk3MGNjNWM3ZDM0NTkwMGIyMWZjNTdmZTc5YTE4YTdhNjEzNGEzMmJkZjM3MjIyZDMyYTkiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==";
    private static final String SKIN_BABY_YETI = "ewogICJ0aW1lc3RhbXAiIDogMTU5NTM1NjY0MjQwNSwKICAicHJvZmlsZUlkIiA6ICI3NTJkODM0NDViMDA0OTIwODMwYTE4MGVhNTVmMzNhMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJMaWxZZXRpIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2VmOWY2YTU3ZDhmNjg2ZmVkZDUxODVkMTc4NTUwOTEzZjQ4ZjkxYjU3YzkxNjJkNzBkMzY4YjFmZDlmZTJiNTIiCiAgICB9CiAgfQp9";
    private static final String SKIN_RED_GIFT = "ewogICJ0aW1lc3RhbXAiIDogMTYwMzAwMjIxMjY2MCwKICAicHJvZmlsZUlkIiA6ICJmZDYwZjM2ZjU4NjE0ZjEyYjNjZDQ3YzJkODU1Mjk5YSIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWFkIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzM0OWIyZjA4YWM0ZTk4NGVjOTk5YjVlZTI2NDg1NGNjNmE2ZGZmMjNjYmM5NzU3OGVkMTA1MjFmZGNkMGU4MzIiCiAgICB9CiAgfQp9";
    private static final String SKIN_GREEN_GIFT = "ewogICJ0aW1lc3RhbXAiIDogMTYwNTE4OTkwMTI3MywKICAicHJvZmlsZUlkIiA6ICJmZDYwZjM2ZjU4NjE0ZjEyYjNjZDQ3YzJkODU1Mjk5YSIsCiAgInByb2ZpbGVOYW1lIiA6ICJSZWFkIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzQ2ZjVjMTczOGQ1NTFjY2Y0NGZhYzJlODQ3ZmI1ZDZlOTY4M2E2NmMxZGI0YTAyMjNmZDQ1N2E4YmU5ZWI3NmYiCiAgICB9CiAgfQp9";
    private static final String SKIN_WHITE_GIFT = "eyJ0aW1lc3RhbXAiOjE1MTQzMjM1NTc0ODYsInByb2ZpbGVJZCI6IjcwOTU2NDU0NTJkOTRiYTI5YzcwZDFmYTY3YjhkYTQyIiwicHJvZmlsZU5hbWUiOiJIaWRkdXMiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzEwZjUzOTg1MTBiMWEwNWFmYzViMjAxZWFkOGJmYzU4M2U1N2Q3MjAyZjUxOTNiMGI3NjFmY2JkMGFlMiJ9fX0=";

    private static final String SKIN_DYE_AQUAMARINE = "ewogICJ0aW1lc3RhbXAiIDogMTc0MTE2MDg4OTYzMiwKICAicHJvZmlsZUlkIiA6ICI1ZTEwYjc5MTY2ZTA0MTNkYjI2MThkMzE3MTc2M2Y4NCIsCiAgInByb2ZpbGVOYW1lIiA6ICJPTUZfQmxvY2tCdXN0ZXIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDcxOTE1NjYzYzI3ZmNjNGViNTBiMzM0YWEwM2ZmZjE3NWVkMDM2M2Y0ZWYzZTAyOGI5Y2M3NzFjODFmNTQ1ZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    private static final String SKIN_YETI = "eyJ0aW1lc3RhbXAiOjE1NzU5ODY3MjQwMTksInByb2ZpbGVJZCI6ImRlNTcxYTEwMmNiODQ4ODA4ZmU3YzlmNDQ5NmVjZGFkIiwicHJvZmlsZU5hbWUiOiJNSEZfTWluZXNraW4iLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzRkZDJkM2M2ZDAxYzI3NjIyNmM3YjBkMzc3MTIyZTFhNjQ3YjJmZmI1YjliNTRmYTk4ZWFjMzdiYjFkMDlkM2EifX19";
    private static final String SKIN_GRIM_REAPER = "ewogICJ0aW1lc3RhbXAiIDogMTYwNDA1NDA5MDUwMCwKICAicHJvZmlsZUlkIiA6ICI5NGMzZGM3YTdiMmQ0NzQ1YmVlYjQzZDc2ZjRjNDVkYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJEb25fVml0b0Nvcmxlb25lIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzY4YTYxOTRhNWIyMTdiOWY1YTNkZmVjY2U1ZjNlZmU2OTY3NDA1MDM5YjgyZmEwYzRlODk1OTE3NWYzMmU3NWEiCiAgICB9CiAgfQp9";

    private static final Map<String, ItemStack> ICONS = new HashMap<>();

    private GambleArt() {
    }

    static int argb(float a, int rgb) {
        int al = Math.max(0, Math.min(255, Math.round(a * 255.0f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    private static ItemStack head(String skin, String uuid) {
        com.google.common.collect.Multimap<String, com.mojang.authlib.properties.Property> m = com.google.common.collect.LinkedHashMultimap.create();
        m.put("textures", new com.mojang.authlib.properties.Property("textures", skin));
        com.mojang.authlib.GameProfile gp = new com.mojang.authlib.GameProfile(java.util.UUID.fromString(uuid), "", new com.mojang.authlib.properties.PropertyMap(m));
        ItemStack s = new ItemStack(Items.PLAYER_HEAD);
        s.set(DataComponents.PROFILE, ResolvableProfile.createResolved(gp));
        return s;
    }

    private static ItemStack model(String model, net.minecraft.world.item.Item fallback) {
        try {
            net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.parse(model);
            Object m = Minecraft.getInstance().getModelManager().getItemModel(id);
            if (m != null && !(m instanceof net.minecraft.client.renderer.item.MissingItemModel)) {
                ItemStack s = new ItemStack(Items.PAPER);
                s.set(DataComponents.ITEM_MODEL, id);
                return s;
            }
        } catch (Throwable ignored) {
        }
        return new ItemStack(fallback);
    }

    /** The icon for a board field (by its label). */
    static ItemStack icon(String label) {
        ItemStack s = ICONS.get(label);
        if (s != null) return s;
        try {
            s = switch (label) {
                case "VIAL" -> head(SKIN_RADIOACTIVE_VIAL, UUID_RADIOACTIVE_VIAL);
                case "DYE" -> head(SKIN_DYE_CARMINE, UUID_DYE_CARMINE);
                case "TEXTS" -> head(SKIN_BURNT_TEXTS, UUID_BURNT_TEXTS);
                case "CHAIN" -> head(SKIN_CHAIN_END_TIMES, UUID_CHAIN_END_TIMES);
                case "HANDLE" -> model("hypixel_skyblock:item/fishing/misc/brimstone_handle", Items.BLAZE_ROD);
                case "BOOK" -> model("hypixel_skyblock:item/island_relevant/crimson_isle/bobbin_time/bobbin_scriptures", Items.ENCHANTED_BOOK);
                case "FANG" -> new ItemStack(Items.GHAST_TEAR);
                case "ORB" -> model("hypixel_skyblock:item/reforge_stones/deep_sea_orb", Items.HEART_OF_THE_SEA);
                case "MDYE" -> head(SKIN_DYE_MIDNIGHT, "f59ddfb7-676e-3c44-ac8b-81ec5065b979");
                case "IDYE" -> head(SKIN_DYE_ICEBERG, "894102dd-7077-39ab-81d4-4d9f61657c8d");
                case "BYETI" -> head(SKIN_BABY_YETI, "7dc4f3ba-6b6f-330a-9a22-791218c81017");
                case "ROD" -> new ItemStack(Items.FISHING_ROD);
                case "HILT" -> model("hypixel_skyblock:item/uncategorized/hilt_of_true_ice", Items.PACKED_ICE);
                case "PROSP" -> new ItemStack(Items.ENCHANTED_BOOK);
                case "RGIFT" -> head(SKIN_RED_GIFT, "4afe5d71-918f-3741-a969-5f785c5b2945");
                case "GGIFT" -> head(SKIN_GREEN_GIFT, "5fa813c0-5519-30c4-a53c-955945e93e10");
                case "WGIFT" -> head(SKIN_WHITE_GIFT, "3047a516-415b-3bf4-b597-b78fd2a9ccf4");
                case "PUMPKIN" -> new ItemStack(Items.JACK_O_LANTERN);
                case "ADYE" -> head(SKIN_DYE_AQUAMARINE, "90e7dd71-25a0-310f-9b1d-47d7a2367bcd");
                case "TFRAG" -> model("hypixel_skyblock:item/uncategorized/thunder_fragment", Items.PRISMARINE_CRYSTALS);
                case "RAGHEAD" -> head(SKIN_RAGNAROK, "feaa91b5-9a47-37bc-8974-13e7aa71fcd6");
                case "FRAG" -> model("hypixel_skyblock:item/uncategorized/magma_lord_fragment", Items.MAGMA_CREAM);
                case "JSHARD" -> new ItemStack(Items.PRISMARINE_SHARD);
                case "YETIHEAD" -> head(SKIN_YETI, "120ce1cf-04b3-383d-a10f-a478ae4e2692");
                case "GRIMHEAD" -> head(SKIN_GRIM_REAPER, "69e73584-c3f5-38b6-9622-3d0b4a95fb92");
                case "x" -> new ItemStack(Items.BARRIER);
                default -> ItemStack.EMPTY;
            };
        } catch (Throwable t) {
            s = ItemStack.EMPTY;
        }
        ICONS.put(label, s);
        return s;
    }

    /** An item drawn {@code size} GUI pixels big, centered on (cx, cy). Empty stack = a big gold "?". */
    static void item(GuiGraphicsExtractor ctx, Font font, ItemStack stack, float cx, float cy, float size, float a) {
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        float s = size / 16.0f;
        pose.scale(s, s);
        if (stack == null || stack.isEmpty()) {
            pose.scale(1.6f, 1.6f);
            ctx.centeredText(font, Component.literal("?").withStyle(net.minecraft.ChatFormatting.BOLD), 0, -4, argb(a, 0xFFCC33));
        } else if (a > 0.5f) {
            ctx.item(stack, -8, -8);
        }
        pose.popMatrix();
    }

    /** A framed card with bevel, inner gradient and - when lit - a pulsing gold glow. */
    static void card(GuiGraphicsExtractor ctx, Font font, ItemStack icon, int cx, int cy, int size, float a, boolean lit, float t, int accent) {
        int x = cx - size / 2, y = cy - size / 2;
        if (lit) {
            float pulse = 0.6f + 0.4f * (float) Math.sin(t * 12);
            for (int g = 1; g <= 4; g++) {
                ctx.outline(x - g * 2, y - g * 2, size + g * 4, size + g * 4, argb(a * pulse * (0.55f - g * 0.11f), 0xFFD24A));
            }
        }
        ctx.fill(x - 2, y - 2, x + size + 2, y + size + 2, argb(a, lit ? 0xFFCC33 : 0x3A2614));
        ctx.fill(x, y, x + size, y + size, argb(a, 0x120A06));
        ctx.fillGradient(x + 2, y + 2, x + size - 2, y + size - 2, argb(a, lit ? 0x4A3416 : 0x2E2016), argb(a, lit ? 0x24160A : 0x150D08));
        ctx.fill(x + 2, y + 2, x + size - 2, y + 4, argb(a * 0.5f, 0xFFFFFF & (lit ? 0xFFE7A0 : 0x7A5A3A)));
        ctx.fill(x + 2, y + size - 3, x + size - 2, y + size - 2, argb(a, accent));
        item(ctx, font, icon, cx, cy - 1, size * 0.68f, a);
    }

    /** Filled disc with a pixel step (GUI pixels). */
    static void disc(GuiGraphicsExtractor ctx, int cx, int cy, int r, int color, int step) {
        for (int dy = -r; dy <= r; dy += step) {
            int half = (int) Math.sqrt(Math.max(0, r * r - dy * dy));
            ctx.fill(cx - half, cy + dy, cx + half, cy + dy + step, color);
        }
    }

    /** Filled ellipse. */
    static void ellipse(GuiGraphicsExtractor ctx, int cx, int cy, int rx, int ry, int color, int step) {
        for (int dy = -ry; dy <= ry; dy += step) {
            double f = 1.0 - (dy * dy) / (double) (ry * ry);
            int half = (int) (rx * Math.sqrt(Math.max(0, f)));
            ctx.fill(cx - half, cy + dy, cx + half, cy + dy + step, color);
        }
    }

    /** Outline of an ellipse, made of small blocks. */
    static void ellipseRing(GuiGraphicsExtractor ctx, int cx, int cy, int rx, int ry, int th, int color) {
        int steps = Math.max(24, (rx + ry) * 2);
        for (int i = 0; i < steps; i++) {
            double a = i * 2 * Math.PI / steps;
            int x = cx + (int) Math.round(Math.cos(a) * rx), y = cy + (int) Math.round(Math.sin(a) * ry);
            ctx.fill(x - th / 2, y - th / 2, x - th / 2 + th, y - th / 2 + th, color);
        }
    }

    /** Iron card (Lord Jawbus): riveted iron frame, a glowing lava crack, the item. */
    static void ironCard(GuiGraphicsExtractor ctx, Font font, ItemStack icon, int cx, int cy, int size, float a, boolean lit, float t) {
        int h = size / 2, x = cx - h, y = cy - h;
        if (lit) ctx.fill(x - 6, y - 6, x + size + 6, y + size + 6, argb(a * (0.35f + 0.25f * (float) Math.sin(t * 12)), 0xFF7A1A));
        ctx.fill(x - 3, y - 3, x + size + 3, y + size + 3, argb(a, 0x4A4640));
        ctx.fillGradient(x, y, x + size, y + size, argb(a, 0x9E9890), argb(a, 0x5E5A54));
        ctx.fill(x + 4, y + 4, x + size - 4, y + size - 4, argb(a, 0x2A2622));
        float glow = lit ? 1.0f : 0.55f + 0.2f * (float) Math.sin(t * 5 + cx);
        int c = argb(a * glow, lit ? 0xFFB347 : 0xFF6A1A);
        ctx.fill(x + 2, y + 7, x + 4, y + 13, c);
        ctx.fill(x + 4, y + 12, x + 7, y + 14, c);
        ctx.fill(x + 6, y + 14, x + 8, y + 20, c);
        int[][] riv = {{x + 3, y + 3}, {x + size - 6, y + 3}, {x + 3, y + size - 6}, {x + size - 6, y + size - 6}};
        for (int[] r : riv) {
            ctx.fill(r[0], r[1], r[0] + 3, r[1] + 3, argb(a, 0xD8D3CC));
            ctx.fill(r[0] + 1, r[1] + 1, r[0] + 3, r[1] + 3, argb(a, 0x5A5650));
        }
        item(ctx, font, icon, cx, cy, size * 0.62f, a);
    }

    /** Prismarine card (Thunder): teal frame with guardian spikes that shoot out when lit. */
    static void prismarineCard(GuiGraphicsExtractor ctx, Font font, ItemStack icon, int cx, int cy, int size, float a, boolean lit, float t) {
        int h = size / 2, x = cx - h, y = cy - h;
        int sp = lit ? 7 + (int) (Math.sin(t * 14) * 2) : 3;
        int spike = argb(a, 0x3F6F68);
        ctx.fill(cx - 2, y - 2 - sp, cx + 2, y - 2, spike);
        ctx.fill(cx - 2, y + size + 2, cx + 2, y + size + 2 + sp, spike);
        ctx.fill(x - 2 - sp, cy - 2, x - 2, cy + 2, spike);
        ctx.fill(x + size + 2, cy - 2, x + size + 2 + sp, cy + 2, spike);
        ctx.fill(x - 2, y - 2, x + size + 2, y + size + 2, argb(a, lit ? 0x6FD0FF : 0x2F5D56));
        ctx.fillGradient(x, y, x + size, y + size, argb(a, 0x7FC8B8), argb(a, 0x2F6E64));
        ctx.fill(x + 4, y + 4, x + size - 4, y + size - 4, argb(a, 0x0A2420));
        ctx.outline(x + 6, y + 6, size - 12, size - 12, argb(a * 0.6f, 0x7FC8B8));
        item(ctx, font, icon, cx, cy, size * 0.6f, a);
        if (lit) ctx.outline(x - 5, y - 5, size + 10, size + 10, argb(a * (0.5f + 0.5f * (float) Math.sin(t * 16)), 0xFFF36B));
    }

    /** A ring of thickness th. */
    static void ring(GuiGraphicsExtractor ctx, int cx, int cy, int r, int th, int color, int step) {
        int ri = r - th;
        for (int dy = -r; dy <= r; dy += step) {
            int ho = (int) Math.sqrt(Math.max(0, r * r - dy * dy));
            int hi = Math.abs(dy) < ri ? (int) Math.sqrt(Math.max(0, ri * ri - dy * dy)) : -1;
            if (hi < 0) {
                ctx.fill(cx - ho, cy + dy, cx + ho, cy + dy + step, color);
            } else {
                ctx.fill(cx - ho, cy + dy, cx - hi, cy + dy + step, color);
                ctx.fill(cx + hi, cy + dy, cx + ho, cy + dy + step, color);
            }
        }
    }

    /** Wheel face: n wedges, colors alternate a/b, turned by rot (radians), drawn in step x step blocks. */
    static void wheel(GuiGraphicsExtractor ctx, int cx, int cy, int r, int n, double rot, int[] colors, float a, int step) {
        double seg = 2 * Math.PI / n;
        for (int dy = -r; dy <= r; dy += step) {
            int half = (int) Math.sqrt(Math.max(0, r * r - dy * dy));
            int runStart = -half;
            int runSeg = -1;
            for (int dx = -half; dx <= half; dx += step) {
                double ang = Math.atan2(dy + step / 2.0, dx + step / 2.0) - rot + Math.PI / 2;
                int s = (int) Math.floor(((ang % (2 * Math.PI)) + 2 * Math.PI) % (2 * Math.PI) / seg) % n;
                if (runSeg == -1) runSeg = s;
                if (s != runSeg) {
                    ctx.fill(cx + runStart, cy + dy, cx + dx, cy + dy + step, argb(a, colors[runSeg % colors.length]));
                    runStart = dx;
                    runSeg = s;
                }
            }
            if (runSeg >= 0) ctx.fill(cx + runStart, cy + dy, cx + half, cy + dy + step, argb(a, colors[runSeg % colors.length]));
        }
    }

    /** Thick line from (x0,y0) to (x1,y1). */
    static void line(GuiGraphicsExtractor ctx, float x0, float y0, float x1, float y1, int th, int color) {
        float dx = x1 - x0, dy = y1 - y0;
        int steps = (int) Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)) / Math.max(1, th * 0.6f));
        for (int i = 0; i <= steps; i++) {
            float u = i / (float) steps;
            int x = Math.round(x0 + dx * u), y = Math.round(y0 + dy * u);
            ctx.fill(x - th / 2, y - th / 2, x - th / 2 + th, y - th / 2 + th, color);
        }
    }

    /** A glowing lightning bolt between two points (wide faint halo, colored body, white core). */
    static void bolt(GuiGraphicsExtractor ctx, float x0, float y0, float x1, float y1, float a, long seed, int color) {
        Random r = new Random(seed);
        int segs = 7;
        float[] xs = new float[segs + 1], ys = new float[segs + 1];
        for (int i = 0; i <= segs; i++) {
            float u = i / (float) segs;
            float j = (i == 0 || i == segs) ? 0 : (r.nextFloat() - 0.5f) * 26;
            xs[i] = x0 + (x1 - x0) * u + j;
            ys[i] = y0 + (y1 - y0) * u + (i == 0 || i == segs ? 0 : (r.nextFloat() - 0.5f) * 10);
        }
        for (int pass = 0; pass < 3; pass++) {
            int th = pass == 0 ? 7 : (pass == 1 ? 3 : 1);
            int c = pass == 0 ? argb(a * 0.25f, color) : (pass == 1 ? argb(a, color) : argb(a, 0xFFFFFF));
            for (int i = 0; i < segs; i++) line(ctx, xs[i], ys[i], xs[i + 1], ys[i + 1], th, c);
        }
        // a small fork
        int f = 2 + r.nextInt(3);
        line(ctx, xs[f], ys[f], xs[f] + (r.nextFloat() - 0.5f) * 40, ys[f] + 18 + r.nextFloat() * 14, 2, argb(a * 0.8f, color));
    }

    /** Big shiny text, scaled, centered. */
    static void bigText(GuiGraphicsExtractor ctx, Font font, String text, float cx, float cy, float scale, int color, float a) {
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(scale, scale);
        Component c = Component.literal(text).withStyle(net.minecraft.ChatFormatting.BOLD);
        ctx.centeredText(font, c, 1, -3, argb(a * 0.6f, 0x2A1400));
        ctx.centeredText(font, c, 0, -4, argb(a, color));
        pose.popMatrix();
    }
}
