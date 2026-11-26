/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.AbstractWidget
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.ConfirmScreen
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.input.MouseButtonEvent
 *  net.minecraft.client.renderer.RenderPipelines
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.Identifier
 *  org.joml.Matrix3x2fStack
 */
package com.skymaxx.tiki.gui;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.anim.AnimationDirector;
import com.skymaxx.tiki.audio.AudioDirector;
import com.skymaxx.tiki.config.DropItem;
import com.skymaxx.tiki.config.GameplayCorner;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.config.TikiStats;
import com.skymaxx.tiki.gui.ConfigSlider;
import com.skymaxx.tiki.render.TikiAssets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

public class SkyMaxxScreen
extends Screen {
    private static final int BACKDROP_TOP = -435482612;
    private static final int BACKDROP_BOTTOM = -234551290;
    private static final int CARD = 1712330513;
    private static final int CARD_EDGE = 1429094451;
    private static final int TAB_ACTIVE = 1712857109;
    private static final int TAB_HOVER = 857219093;
    private static final int ACCENT = -671174;
    private static final int TEXT = -1514280;
    private static final int TEXT_DIM = -7695484;
    private static final int RULE = 1143881779;
    private static final int SCROLLBAR = 1442169402;
    private static final int SCROLLBAR_LIVE = -856309190;
    private static final int SCROLLBAR_TRACK = 0x22000000;
    private static final int TITLE_MAX_WIDTH = 250;
    private static final int TITLE_BAND = 44;
    private static final int TAB_TOP = 50;
    private static final int TAB_HEIGHT = 22;
    private static final int PAD = 14;
    private static final int SCROLLBAR_WIDTH = 3;
    private static Tab activeTab = Tab.GENERAL;
    private static boolean tikiOpen;
    private static boolean boaOpen;
    private static boolean animOpen;
    private static boolean lavaOpen;
    private static boolean spookyOpen;
    private static boolean winterOpen;
    /** Gambling tab: only one category is open at a time. */
    private static String openGamble = "";
    private static boolean sacksOpen;
    private static boolean searchAll;
    private static boolean stampsOpen;
    private static final int SCROLLBAR_GRAB = 5;
    private boolean draggingScrollbar;
    private double grabOffset;
    private static final int SECTION_INDENT = 10;
    private static final int SECTION_GROUND = 0x14FFFFFF;
    private final TikiConfig cfg;
    private final TikiStats stats;
    private final AnimationDirector director;
    private final AudioDirector audio;
    private final Screen parent;
    private final List<Row> rows = new ArrayList<Row>();
    private final List<Label> labels = new ArrayList<Label>();
    private final List<Panel> panels = new ArrayList<Panel>();
    private int indent;
    private final int[] tabX = new int[Tab.values().length];
    private int tabWidth;
    private int scroll;
    private int contentHeight;
    private int cardLeft;
    private int cardRight;
    private int cardTop;
    private int cardBottom;
    private int viewTop;
    private int viewBottom;
    private static final int STAT_ROW_HEIGHT = 15;
    private static final int ITEM_ROWS;
    private static final int TIKI_STAT_ROWS = 6;
    private static final int BOA_STAT_ROWS = 7;
    private int winLeft;
    private int winTop;
    private int winRight;
    private int winBottom;
    private int headerBottom;
    private int sideRight;
    private static String query = "";
    private EditBox searchBox;
    private static final int GLASS = 0xD01A0F1C;
    private static final int GLASS_HEADER = 0xE02A1428;
    private static final int GLASS_SIDE = 0x50000000;
    private static final int PINK = 0xFFFF8CC8;
    private static final int PINK_SOFT = 0x99FF8CC8;
    private static final int PINK_LINE = 0x55FF8CC8;
    private static final int PINK_ACTIVE = 0x55FF6FB0;
    private static final int SIDE_TAB_TOP = 8;
    private static final int SIDE_TAB_STEP = 24;
    private static final int SIDE_TAB_HEIGHT = 20;
    private int tikiStatsTop;
    private int luckTop;
    private int achievementsTop;
    private int dyesTop;
    private int boaStatsTop;

    public SkyMaxxScreen(TikiConfig cfg, TikiStats stats, AnimationDirector director, AudioDirector audio, Screen parent) {
        super((Component)Component.literal((String)"Light"));
        this.cfg = cfg;
        this.stats = stats;
        this.director = director;
        this.audio = audio;
        this.parent = parent;
    }

    protected void init() {
        TikiAssets.init();
        this.rows.clear();
        this.labels.clear();
        this.panels.clear();
        this.indent = 0;
        // Light: pink glass window at ~75% of the screen with a sidebar and a search bar
        int ww = Math.min(this.width - 10, Math.max(340, (int)((float)this.width * 0.75f)));
        int wh = Math.min(this.height - 10, Math.max(220, (int)((float)this.height * 0.75f)));
        this.winLeft = (this.width - ww) / 2;
        this.winTop = (this.height - wh) / 2;
        this.winRight = this.winLeft + ww;
        this.winBottom = this.winTop + wh;
        this.headerBottom = this.winTop + 30;
        this.sideRight = this.winLeft + Math.max(92, Math.min(132, ww / 5));
        this.cardLeft = this.sideRight;
        this.cardRight = this.winRight;
        this.cardTop = this.headerBottom;
        this.cardBottom = this.winBottom;
        this.viewTop = this.cardTop + 12;
        this.viewBottom = this.cardBottom - 12;
        int sw = Math.min(170, Math.max(90, ww / 3));
        this.searchBox = new EditBox(this.font, this.winRight - 10 - sw, this.winTop + 7, sw, 16, (Component)Component.literal("Search"));
        this.searchBox.setMaxLength(40);
        this.searchBox.setHint((Component)Component.literal("\u00a78Search..."));
        this.searchBox.setValue(query);
        this.searchBox.moveCursorToEnd(false);
        this.searchBox.setResponder(text -> {
            if (!text.equals(query)) {
                query = text;
                this.scroll = 0;
                this.rebuildWidgets();
            }
        });
        int y = 0;
        int w = this.cardRight - this.cardLeft - 28 - 3 - 4;
        int left = this.cardLeft + 14;
        if (!query.isBlank()) {
            y = this.buildSearch(left, w);
        } else {
            switch (activeTab) {
                case GAMBLING -> y = this.buildGambling(left, y, w);
                case SOUND -> y = this.buildSound(left, y, w);
                case BAZAAR -> y = this.buildBazaar(left, y, w);
                case DYES -> y = this.buildDyes(left, y, w);
                case HUDS -> y = this.buildHuds(left, y, w);
                case SETTINGS -> y = this.buildSettings(left, y, w);
                case GENERAL -> y = this.buildGeneral(left, y, w);
                case STATS -> y = this.buildStats(left, y, w);
                case ACHIEVEMENTS -> y = this.buildAchievements(left, y, w);
                case HISTORY -> y = this.buildHistory(left, y, w);
            }
        }
        this.contentHeight = y;
        for (Row row : this.rows) {
            this.addWidget(row.widget());
        }
        this.applyScroll();
        this.addRenderableWidget(Button.builder((Component)Component.literal((String)"Done"), b -> this.onClose()).bounds(this.winLeft + 8, this.winBottom - 28, this.sideRight - this.winLeft - 16, 20).build());
        this.addRenderableWidget(this.searchBox);
        if (!query.isEmpty()) {
            this.setFocused(this.searchBox);
        }
    }

    private int buildBazaar(int x, int y, int w) {
        y = this.heading(y, "Sack value");
        y = this.toggle(x, y, w, "Sack value HUD", () -> this.cfg.sackHudEnabled, v -> {
            this.cfg.sackHudEnabled = v;
        });
        int half = (w - 4) / 2;
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal(SkyMaxxScreen.sackPriceLabel(this.cfg.sackPriceMode)), b -> {
            this.cfg.sackPriceMode = "INSTA_SELL".equals(this.cfg.sackPriceMode) ? "SELL_OFFER" : "INSTA_SELL";
            this.cfg.save();
            b.setMessage((Component)Component.literal(SkyMaxxScreen.sackPriceLabel(this.cfg.sackPriceMode)));
        }).bounds(x, y, half, 20).build(), y));
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal(SkyMaxxScreen.sackStyleLabel(this.cfg.sackHudStyle)), b -> {
            this.cfg.sackHudStyle = "TEXT".equals(this.cfg.sackHudStyle) ? "BOX" : "TEXT";
            this.cfg.save();
            b.setMessage((Component)Component.literal(SkyMaxxScreen.sackStyleLabel(this.cfg.sackHudStyle)));
        }).bounds(x + half + 4, y, w - half - 4, 20).build(), y));
        y += 22;
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal("\u00a7dMove and resize sack value HUD"), b -> {
            if (this.minecraft != null) this.minecraft.gui.setScreen((Screen)new com.skymaxx.tiki.light.SackEditScreen((Screen)this));
        }).bounds(x, y, w, 20).build(), y));
        y += 22;
        y = this.toggle(x, y, w, "Compact numbers (902,97M / 1,21B)", () -> this.cfg.sackCompact, v -> {
            this.cfg.sackCompact = v;
        });
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal(SkyMaxxScreen.refreshLabel(this.cfg.sackRefreshMinutes)), b -> {
            int m = this.cfg.sackRefreshMinutes;
            this.cfg.sackRefreshMinutes = m <= 5 ? 10 : (m <= 10 ? 15 : 5);
            this.cfg.save();
            b.setMessage((Component)Component.literal(SkyMaxxScreen.refreshLabel(this.cfg.sackRefreshMinutes)));
        }).bounds(x, y, w, 20).build(), y));
        y += 22;
        y = this.sectionHeader(x, y + 2, w, "Counted sacks", null, sacksOpen, () -> {
            sacksOpen = !sacksOpen;
        });
        if (sacksOpen) {
            java.util.List<String> known = com.skymaxx.tiki.light.SackValue.knownSacks();
            if (known.isEmpty()) y = this.note(y, "\u00a77No sacks read yet - open them once in game.");
            for (String sack : known) {
                y = this.toggle(x + 10, y, w - 10, sack, () -> !com.skymaxx.tiki.light.SackValue.excluded(sack), v -> com.skymaxx.tiki.light.SackValue.setExcluded(sack, !v));
            }
            y += 4;
        }
        y = this.note(y, "\u00a77Open each of your sacks once - after that the [Sacks]");
        y = this.note(y, "\u00a77messages keep it up to date. Prices: Hypixel bazaar API,");
        y = this.note(y, "\u00a77one request for all items at the interval above.");
        y = this.heading(y, "Bazaar");
        y = this.toggle(x, y, w, "Lock Sell Sacks Now", () -> this.cfg.lockBazaarSellSacks, v -> {
            this.cfg.lockBazaarSellSacks = v;
        });
        y = this.note(y, "Blocks your click on Sell Sacks Now in the Bazaar,");
        y = this.note(y, "so sacks are never insta-sold by accident.");
        y = this.note(y, "Sell with Sell Offers instead - nothing is sent to");
        y = this.note(y, "the server, only your own click is stopped.");
        return y;
    }

    private int buildSearch(int x, int w) {
        boolean t = tikiOpen;
        boolean b = boaOpen;
        boolean s = stampsOpen;
        stampsOpen = true;
        boaOpen = true;
        tikiOpen = true;
        searchAll = true;
        int y = this.buildGambling(x, 0, w);
        searchAll = false;
        y = this.buildSound(x, y + 10, w);
        y = this.buildBazaar(x, y + 10, w);
        y = this.buildDyes(x, y + 10, w);
        this.buildGeneral(x, y + 10, w);
        tikiOpen = t;
        boaOpen = b;
        stampsOpen = s;
        String q = query.trim().toLowerCase(Locale.ROOT);
        ArrayList<AbstractWidget> hits = new ArrayList<AbstractWidget>();
        for (Row row : this.rows) {
            String text = row.widget().getMessage().getString().replaceAll("\u00a7.", "").toLowerCase(Locale.ROOT);
            if (text.startsWith("\u25bc") || text.startsWith("\u25b6")) continue;
            if (text.contains(q)) {
                hits.add(row.widget());
            }
        }
        this.rows.clear();
        this.labels.clear();
        this.panels.clear();
        y = this.heading(0, "Results for \"" + query.trim() + "\"");
        for (AbstractWidget widget : hits) {
            widget.setX(x);
            widget.setY(y);
            this.rows.add(new Row(widget, y));
            y += widget.getHeight() + 2;
        }
        if (hits.isEmpty()) {
            y = this.note(y, "No setting matches your search.");
        }
        return y;
    }

    private int buildGambling(int x, int y, int w) {
        y = this.category(x, y, w, "tiki", "Wiki Tiki", this.cfg.tikiEnabled, () -> this.buildTiki(x + 10, 0, w - 10));
        y = this.category(x, y, w, "boa", "Titanoboa", this.cfg.titanoboaEnabled, () -> this.buildTitanoboa(x + 10, 0, w - 10));
        y = this.category(x, y, w, "lava", "Lava Fishing  \u00a78(Test)", this.cfg.lavaGambleEnabled, () -> this.buildLavaGambling(x + 10, 0, w - 10));
        y = this.category(x, y, w, "spooky", "Spooky Fishing  \u00a78(Test)", this.cfg.spookyGambleEnabled, () -> this.buildSpooky(x + 10, 0, w - 10));
        y = this.category(x, y, w, "winter", "Winter Fishing  \u00a78(Test)", this.cfg.winterGambleEnabled, () -> this.buildWinter(x + 10, 0, w - 10));
        y = this.category(x, y, w, "shark", "Shark Pop-up  \u00a78(Debug)", null, () -> this.buildFishingAnims(x + 10, 0, w - 10));
        return y;
    }

    /** One collapsible category; opening it closes the others. */
    private int category(int x, int y, int w, String key, String title, Boolean on, IntSupplier body) {
        boolean open = searchAll || key.equals(openGamble);
        y = this.sectionHeader(x, y, w, title, on, open, () -> {
            openGamble = key.equals(openGamble) ? "" : key;
        });
        if (open) y = this.openSection(y, body);
        return y + 2;
    }

    private int sectionHeader(int x, int y, int w, String title, Boolean enabled, boolean open, Runnable flip) {
        String text = (open ? "\u00a76\u25bc " : "\u00a76\u25b6 ") + "\u00a7f" + title + (String)(enabled == null ? "" : "  " + (enabled != false ? "\u00a7aON" : "\u00a77OFF"));
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal((String)text), b -> {
            flip.run();
            this.rebuildWidgets();
        }).bounds(x, y, w, 20).build(), y));
        return y + 24;
    }

    private int openSection(int y, IntSupplier build) {
        int i;
        int firstRow = this.rows.size();
        int firstLabel = this.labels.size();
        this.indent = 10;
        int height = build.getAsInt();
        this.indent = 0;
        for (i = firstRow; i < this.rows.size(); ++i) {
            Row row = this.rows.get(i);
            row.widget().setY(y + 4 + row.baseY());
            this.rows.set(i, new Row(row.widget(), y + 4 + row.baseY()));
        }
        for (i = firstLabel; i < this.labels.size(); ++i) {
            Label label = this.labels.get(i);
            this.labels.set(i, new Label(y + 4 + label.baseY(), label.text(), label.heading(), label.indent()));
        }
        int bottom = y + 4 + height + 6;
        this.panels.add(new Panel(y - 2, bottom));
        return bottom + 2;
    }

    private int buildFishingAnims(int x, int y, int w) {
        y = this.note(y, "\u00a77Great White Shark: Megalodon / Shark Tooth pop-up.");
        y = this.buildSharkControls(x, y, w);
        return y;
    }

    private int buildLavaGambling(int x, int y, int w) {
        y = this.note(y, "\u00a77Jawbus, Thunder, Ragnarok, Plhlegblast - always your real drop.");
        int half = (w - 4) / 2;
        this.toggle(x, y, half, "Lava Gambling", () -> this.cfg.lavaGambleEnabled, v -> {
            this.cfg.lavaGambleEnabled = v;
        });
        y = this.toggle(x + half + 4, y, w - half - 4, "Own kills only", () -> this.cfg.lavaGambleOwnOnly, v -> {
            this.cfg.lavaGambleOwnOnly = v;
        });
        y = this.slider(x, y, w, "Volume", 0.0f, 1.0f, () -> Float.valueOf(this.cfg.lavaGambleVolume), v -> {
            this.cfg.lavaGambleVolume = v.floatValue();
        }, ConfigSlider.Format.PERCENT, null);
        y = this.note(y + 4, "\u00a78Mob  \u00b7  Speed  \u00b7  Preview");
        y = this.mobRow(x, y, w, "jawbus", "Lord Jawbus", () -> this.cfg.lavaGambleJawbus, v -> this.cfg.lavaGambleJawbus = v, false);
        y = this.mobRow(x, y, w, "thunder", "Thunder", () -> this.cfg.lavaGambleThunder, v -> this.cfg.lavaGambleThunder = v, false);
        y = this.mobRow(x, y, w, "ragnarok", "Ragnarok", () -> this.cfg.lavaGambleRagnarok, v -> this.cfg.lavaGambleRagnarok = v, false);
        y = this.mobRow(x, y, w, "plhlegblast", "Plhlegblast", () -> this.cfg.lavaGamblePlhlegblast, v -> this.cfg.lavaGamblePlhlegblast = v, false);
        return y;
    }

    private int buildSpooky(int x, int y, int w) {
        y = this.note(y, "\u00a77Grim Reaper - always your real drop.");
        y = this.toggle(x, y, w, "Spooky Fishing", () -> this.cfg.spookyGambleEnabled, v -> {
            this.cfg.spookyGambleEnabled = v;
        });
        y = this.note(y + 4, "\u00a78Mob  \u00b7  Speed  \u00b7  Preview");
        y = this.mobRow(x, y, w, "grim", "Grim Reaper", () -> this.cfg.gambleGrimReaper, v -> this.cfg.gambleGrimReaper = v, false);
        return y;
    }

    private int buildWinter(int x, int y, int w) {
        y = this.note(y, "\u00a77Yeti and Reindrake - always your real drop.");
        y = this.toggle(x, y, w, "Winter Fishing", () -> this.cfg.winterGambleEnabled, v -> {
            this.cfg.winterGambleEnabled = v;
        });
        y = this.note(y + 4, "\u00a78Mob  \u00b7  Speed  \u00b7  Preview");
        y = this.mobRow(x, y, w, "yeti", "Yeti", () -> this.cfg.gambleYeti, v -> this.cfg.gambleYeti = v, false);
        y = this.mobRow(x, y, w, "reindrake", "Reindrake", () -> this.cfg.gambleReindrake, v -> this.cfg.gambleReindrake = v, false);
        return y;
    }

    /** One mob in one line: on/off, speed (50 - 200 %) and a preview button. */
    private int mobRow(int x, int y, int w, String key, String name, BooleanSupplier on, Consumer<Boolean> set, boolean debugOnly) {
        int pw = 22, gap = 4;
        int tw = (w - pw - 2 * gap) / 2;
        int sw = w - pw - 2 * gap - tw;
        this.toggle(x, y, tw, name, on, set);
        this.slider(x + tw + gap, y, sw, "Speed", 0.5f, 2.0f, () -> Float.valueOf(this.cfg.gambleSpeedOf(key)), v -> {
            this.cfg.gambleSpeed.put(key, Math.round(v.floatValue() * 20.0f) / 20.0f);
        }, ConfigSlider.Format.PERCENT, null);
        Button play = Button.builder((Component)Component.literal("\u00a7b\u25b6"), b -> {
            if (debugOnly && !this.cfg.debug) {
                com.skymaxx.tiki.TikiGambleClient.chat("\u00a7c[Light] Turn on \"Debug log\" (General) to test this one.");
                return;
            }
            this.cfg.clamp();
            this.cfg.save();
            this.audio.applyVolume();
            if (this.minecraft != null) this.minecraft.gui.setScreen(null);
            com.skymaxx.tiki.light.LavaScene.test(key, false);
        }).bounds(x + tw + gap + sw + gap, y, pw, 20).build();
        play.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Preview (random result)")));
        this.rows.add(new Row((AbstractWidget)play, y));
        return y + 22;
    }

    private int buildSharkControls(int x, int y, int w) {
        y = this.toggle(x, y, w, "Shark pop-up without a drop", () -> this.cfg.sharkPopupNoDrop, v -> {
            this.cfg.sharkPopupNoDrop = v;
        });
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal((String)"\u00a7bMove and resize shark pop-up"), b -> {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen((Screen)new com.skymaxx.tiki.light.SharkEditScreen((Screen)this));
            }
        }).bounds(x, y, w, 20).build(), y));
        return y + 22;
    }

    private static String bazaarLabel(String mode) {
        return "Bazaar price: \u00a7f" + ("SELL_OFFER".equals(mode) ? "Sell offer" : "Insta-sell");
    }

    private static String auctionLabel(String mode) {
        return "Auction price: \u00a7f" + ("MEDIAN_5".equals(mode) ? "Median of 5 lowest BINs" : "Lowest BIN");
    }

    private int buildSettings(int x, int y, int w) {
        y = this.heading(y, "Prices (SkyCofl)");
        y = this.toggle(x, y, w, "Show drop prices", () -> this.cfg.lavaGamblePrices, v -> {
            this.cfg.lavaGamblePrices = v;
        });
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal(SkyMaxxScreen.bazaarLabel(this.cfg.priceBazaarMode)), b -> {
            this.cfg.priceBazaarMode = "SELL_OFFER".equals(this.cfg.priceBazaarMode) ? "INSTA_SELL" : "SELL_OFFER";
            this.cfg.save();
            b.setMessage((Component)Component.literal(SkyMaxxScreen.bazaarLabel(this.cfg.priceBazaarMode)));
        }).bounds(x, y, w, 20).build(), y));
        y += 22;
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal(SkyMaxxScreen.auctionLabel(this.cfg.priceAuctionMode)), b -> {
            this.cfg.priceAuctionMode = "MEDIAN_5".equals(this.cfg.priceAuctionMode) ? "LOWEST_BIN" : "MEDIAN_5";
            this.cfg.save();
            com.skymaxx.tiki.light.LavaPrices.clearCache();
            b.setMessage((Component)Component.literal(SkyMaxxScreen.auctionLabel(this.cfg.priceAuctionMode)));
        }).bounds(x, y, w, 20).build(), y));
        y += 22;
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal("Refresh prices now"), b -> {
            com.skymaxx.tiki.light.LavaPrices.clearCache();
            b.setMessage((Component)Component.literal("\u00a7aPrices cleared - fetched again on the next drop"));
        }).bounds(x, y, w, 20).build(), y));
        y += 22;
        y = this.note(y, "Insta-sell = what buy orders pay right now.");
        y = this.note(y, "Sell offer = what sellers ask (where your offer would sit).");
        y = this.note(y, "Lowest BIN = cheapest Buy-It-Now in the auction house.");
        y = this.note(y, "Median = middle of the 5 cheapest, ignores troll listings.");
        y = this.note(y, "\u00a78Data: SkyCofl - sky.coflnet.com/data. One request per drop,");
        y = this.note(y, "\u00a78cached 10 min, max 20 a minute. Off = Light uses no internet.");
        y = this.heading(y, "RNG fanfare (Beta)");
        y = this.note(y, "\u00a77Needs \"Beta features\" in General.");
        y = this.toggle(x, y, w, "RNG fanfare", () -> this.cfg.fanfareEnabled, v -> {
            this.cfg.fanfareEnabled = v;
        });
        y = this.slider(x, y, w, "Fanfare from", 1.0f, 200.0f, () -> Float.valueOf(this.cfg.fanfareMillions), v -> {
            this.cfg.fanfareMillions = Math.round(v.floatValue());
        }, ConfigSlider.Format.MILLIONS, null);
        y = this.toggle(x, y, w, "Screenshot on RNG", () -> this.cfg.fanfareScreenshot, v -> {
            this.cfg.fanfareScreenshot = v;
        });
        y = this.note(y, "\u00a77Dyes always get the fanfare.");
        return y;
    }

    private static String sackPriceLabel(String mode) {
        return "Price: " + ("INSTA_SELL".equals(mode) ? "\u00a7eInsta-Sell" : "\u00a7aSell Offer");
    }

    private static String refreshLabel(int minutes) {
        return "Update prices every: \u00a7f" + Math.max(5, minutes) + " min";
    }

    private static String sackStyleLabel(String style) {
        return "Look: " + ("TEXT".equals(style) ? "\u00a7fText only" : "\u00a7dBox");
    }

    private int buildHistory(int x, int y, int w) {
        java.util.List<com.skymaxx.tiki.light.GambleLog.Entry> list = com.skymaxx.tiki.light.GambleLog.entries();
        boolean withTest = com.skymaxx.tiki.light.GambleLog.hasTest();
        y = this.heading(y, "Summary" + (withTest ? " \u00a7e(with test entries)" : ""));
        com.skymaxx.tiki.light.GambleLog.Entry best = com.skymaxx.tiki.light.GambleLog.best(withTest);
        y = this.note(y, "Total  \u00a76" + com.skymaxx.tiki.light.LavaPrices.format(com.skymaxx.tiki.light.GambleLog.total(withTest)));
        y = this.note(y, "Best hit  " + (best == null ? "\u00a78-" : "\u00a7d" + best.drop + " \u00a78\u00b7 \u00a76" + com.skymaxx.tiki.light.LavaPrices.format(best.price)));
        y = this.note(y, "This session  \u00a7a" + com.skymaxx.tiki.light.LavaPrices.format(com.skymaxx.tiki.light.GambleLog.perHour()) + " \u00a77/ h");
        y = this.note(y, "Results  \u00a7f" + list.size() + " \u00a77(" + com.skymaxx.tiki.light.GambleLog.drops(null, withTest) + " with a drop)");
        if (this.cfg.debug) {
            this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal("\u00a76Open as Journal \u00a78(Debug)"), b -> {
                if (this.minecraft != null) this.minecraft.gui.setScreen(new com.skymaxx.tiki.light.JournalScreen(this));
            }).bounds(x, y + 4, w, 20).build(), y + 4));
            y += 26;
        }
        y = this.heading(y, "Drops");
        if (list.isEmpty()) {
            y = this.note(y, "\u00a77Nothing yet - every Jawbus, Thunder, Ragnarok, Plhlegblast,");
            y = this.note(y, "\u00a77Grim Reaper, Yeti and Reindrake animation lands here.");
        }
        java.text.SimpleDateFormat time = new java.text.SimpleDateFormat("HH:mm");
        java.text.SimpleDateFormat day = new java.text.SimpleDateFormat("dd.MM. HH:mm");
        long today = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        int shown = 0;
        for (com.skymaxx.tiki.light.GambleLog.Entry e : list) {
            if (++shown > 150) {
                y = this.note(y, "\u00a78... older ones are kept but not listed.");
                break;
            }
            String when = (e.ms >= today ? time : day).format(new java.util.Date(e.ms));
            String what = e.drop == null ? "\u00a78no drop"
                : "\u00a7d" + e.drop + (e.price > 0 ? " \u00a76" + com.skymaxx.tiki.light.LavaPrices.format(e.price) : "");
            y = this.note(y, (e.test ? "\u00a7e[Test] " : "") + "\u00a77" + when + "  \u00a7f" + e.mobName + (e.share ? " \u00a78(share)" : "") + (e.fanfare ? " \u00a76\u2605 RNG" : ""));
            y = this.note(y - 4, "   " + what) + 3;
        }
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal("\u00a7cClear history"), b -> {
            if (this.minecraft == null) return;
            this.minecraft.gui.setScreen((Screen)new ConfirmScreen(yes -> {
                if (yes) com.skymaxx.tiki.light.GambleLog.clearAll();
                this.minecraft.gui.setScreen((Screen)this);
            }, (Component)Component.literal("Clear the drop history?"), (Component)Component.literal("All gambling results, prices and dry streaks are deleted. This cannot be undone."),
                (Component)Component.literal("\u00a7cClear"), (Component)Component.literal("Cancel")));
        }).bounds(x, y + 8, w, 20).build(), y + 8));
        return y + 34;
    }

    private int buildHuds(int x, int y, int w) {
        y = this.note(y, "\u00a77Every HUD: switch it on or off, then \u00a7e\u271c Move\u00a77 to drag it");
        y = this.note(y, "\u00a77and scroll to resize. Right-click resets.");
        y = this.heading(y, "Dye HUD");
        y = this.hudRow(x, y, w, "Dye HUD", () -> this.cfg.dyeHudEnabled, v -> this.cfg.dyeHudEnabled = v,
            () -> new com.skymaxx.tiki.light.dye.DyeHudEditScreen((Screen)this));
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal(SkyMaxxScreen.hudMode(this.cfg.dyeHudOnlyFishing)), btn -> {
            this.cfg.dyeHudOnlyFishing = !this.cfg.dyeHudOnlyFishing;
            this.cfg.save();
            btn.setMessage((Component)Component.literal(SkyMaxxScreen.hudMode(this.cfg.dyeHudOnlyFishing)));
        }).bounds(x, y, w, 20).build(), y));
        y += 22;
        y = this.heading(y, "Sack value");
        y = this.hudRow(x, y, w, "Sack value HUD", () -> this.cfg.sackHudEnabled, v -> this.cfg.sackHudEnabled = v,
            () -> new com.skymaxx.tiki.light.SackEditScreen((Screen)this));
        y = this.heading(y, "Achievement pop-up");
        y = this.hudRow(x, y, w, "Achievement pop-up", () -> this.cfg.lightAchievementFx, v -> this.cfg.lightAchievementFx = v,
            () -> new com.skymaxx.tiki.light.AchievementPopupEditScreen((Screen)this));
        y = this.heading(y, "Shark pop-up \u00a78(Debug)");
        y = this.hudRow(x, y, w, "Also without a drop", () -> this.cfg.sharkPopupNoDrop, v -> this.cfg.sharkPopupNoDrop = v,
            () -> new com.skymaxx.tiki.light.SharkEditScreen((Screen)this));
        y = this.heading(y, "Titanoboa corner");
        y = this.toggle(x, y, w, "Gameplay during Animation", () -> this.cfg.boaGameplayView, v -> {
            this.cfg.boaGameplayView = v;
        });
        y = this.cornerButton(x, y, w);
        y = this.heading(y, "In the world");
        y = this.toggle(x, y, w, "Hotspot text on the water", () -> this.cfg.hotspotTextOnWater, v -> {
            this.cfg.hotspotTextOnWater = v;
        });
        return y;
    }

    /** One HUD: on/off on the left, a clear yellow Move button on the right. */
    private int hudRow(int x, int y, int w, String label, BooleanSupplier on, Consumer<Boolean> set, Supplier<Screen> mover) {
        int mw = Math.max(70, w / 3);
        this.toggle(x, y, w - mw - 4, label, on, set);
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal("\u00a7e\u271c Move"), b -> {
            if (this.minecraft != null) this.minecraft.gui.setScreen(mover.get());
        }).bounds(x + w - mw, y, mw, 20).build(), y));
        return y + 24;
    }

    private int buildTiki(int x, int y, int w) {
        y = this.heading(y, "The totem");
        y = this.toggle(x, y, w, "Tiki Gambling", () -> this.cfg.tikiEnabled, v -> {
            this.cfg.tikiEnabled = v;
        });
        y = this.slider(x, y, w, "Hide & mute below", 0.0f, 7.5E7f, () -> Float.valueOf((float)this.cfg.muteFromHealth), v -> {
            this.cfg.muteFromHealth = v.floatValue();
        }, ConfigSlider.Format.HEALTH, null);
        y = this.note(y, "Once a Wiki Tiki of yours - or one you are");
        y = this.note(y, "loot sharing - is below this health, the game");
        y = this.note(y, "goes silent and the HUD disappears, so nothing");
        y = this.note(y, "of the kill gives it away.");
        y = this.toggle(x, y, w, "Only My Wiki Tikis", () -> this.cfg.tikiOwnCatchesOnly, v -> {
            this.cfg.tikiOwnCatchesOnly = v;
        });
        y = this.note(y, "On: loot shared Wiki Tikis are left alone - no");
        y = this.note(y, "totem, no mute, no hidden HUD. Only the ones the");
        y = this.note(y, "game told you that you fished up yourself.");
        y = this.heading(y, "Length of the turn");
        y = this.slider(x, y, w, "First mask", 0.5f, 12.0f, () -> Float.valueOf(this.cfg.spinSeconds1), v -> {
            this.cfg.spinSeconds1 = v.floatValue();
        }, ConfigSlider.Format.SECONDS, null);
        y = this.slider(x, y, w, "Second mask", 0.5f, 12.0f, () -> Float.valueOf(this.cfg.spinSeconds2), v -> {
            this.cfg.spinSeconds2 = v.floatValue();
        }, ConfigSlider.Format.SECONDS, null);
        y = this.slider(x, y, w, "Third mask", 0.5f, 12.0f, () -> Float.valueOf(this.cfg.spinSeconds3), v -> {
            this.cfg.spinSeconds3 = v.floatValue();
        }, ConfigSlider.Format.SECONDS, null);
        y = this.slider(x, y, w, "Fourth mask", 0.5f, 12.0f, () -> Float.valueOf(this.cfg.spinSeconds4), v -> {
            this.cfg.spinSeconds4 = v.floatValue();
        }, ConfigSlider.Format.SECONDS, null);
        y = this.note(y, "Try your timings with \u00a76/tikipreview\u00a7r - a kill");
        y = this.note(y, "rolled like a real one, counted nowhere.");
        y = this.heading(y, "Pacing");
        y = this.slider(x, y, w, "Totem rise", 0.0f, 4.0f, () -> Float.valueOf(this.cfg.introSeconds), v -> {
            this.cfg.introSeconds = v.floatValue();
        }, ConfigSlider.Format.SECONDS, null);
        y = this.slider(x, y, w, "Pause on reveal", 0.0f, 3.0f, () -> Float.valueOf(this.cfg.revealPauseSeconds), v -> {
            this.cfg.revealPauseSeconds = v.floatValue();
        }, ConfigSlider.Format.SECONDS, null);
        y = this.slider(x, y, w, "Fail outro", 0.1f, 5.0f, () -> Float.valueOf(this.cfg.failOutroSeconds), v -> {
            this.cfg.failOutroSeconds = v.floatValue();
        }, ConfigSlider.Format.SECONDS, null);
        y = this.slider(x, y, w, "Win outro", 0.1f, 6.0f, () -> Float.valueOf(this.cfg.winOutroSeconds), v -> {
            this.cfg.winOutroSeconds = v.floatValue();
        }, ConfigSlider.Format.SECONDS, null);
        y = this.heading(y, "Tempo");
        y = this.slider(x, y, w, "At the first mask", 40.0f, 300.0f, () -> Float.valueOf(this.cfg.drumBpmMin), v -> {
            this.cfg.drumBpmMin = v.floatValue();
        }, ConfigSlider.Format.BPM, null);
        y = this.slider(x, y, w, "At the fourth mask", 40.0f, 400.0f, () -> Float.valueOf(this.cfg.drumBpmMax), v -> {
            this.cfg.drumBpmMax = v.floatValue();
        }, ConfigSlider.Format.BPM, null);
        y = this.note(y, "The drum bed steps between these, mask by mask.");
        y = this.heading(y, "Look");
        y = this.slider(x, y, w, "Background darkness", 0.0f, 1.0f, () -> Float.valueOf(this.cfg.scrimStrength), v -> {
            this.cfg.scrimStrength = v.floatValue();
        }, ConfigSlider.Format.PERCENT, null);
        return y;
    }

    private int buildTitanoboa(int x, int y, int w) {
        y = this.heading(y, "The rifle");
        y = this.toggle(x, y, w, "Titanoboa Gambling", () -> this.cfg.titanoboaEnabled, v -> {
            this.cfg.titanoboaEnabled = v;
        });
        y = this.slider(x, y, w, "Hide & mute below", 0.0f, 4.5E7f, () -> Float.valueOf((float)this.cfg.titanoboaMuteFromHealth), v -> {
            this.cfg.titanoboaMuteFromHealth = v.floatValue();
        }, ConfigSlider.Format.HEALTH, null);
        y = this.note(y, "Once a Titanoboa of yours - or one you are");
        y = this.note(y, "loot sharing - is below this health, the game");
        y = this.note(y, "goes silent and the HUD disappears, so nothing");
        y = this.note(y, "of the kill gives it away.");
        y = this.toggle(x, y, w, "Only My Titanoboas", () -> this.cfg.titanoboaOwnCatchesOnly, v -> {
            this.cfg.titanoboaOwnCatchesOnly = v;
        });
        y = this.note(y, "On: loot shared Titanoboas are left alone - no");
        y = this.note(y, "rifle, no mute, no hidden HUD. Only the ones the");
        y = this.note(y, "game told you that you fished up yourself.");
        y = this.heading(y, "Pacing");
        y = this.slider(x, y, w, "Speed", 0.6f, 2.0f, () -> Float.valueOf(this.cfg.boaPace), v -> {
            this.cfg.boaPace = v.floatValue();
        }, ConfigSlider.Format.PERCENT, null);
        y = this.note(y, "The whole run. The three below move one part of it.");
        y = this.slider(x, y, w, "Loading the chamber", 0.5f, 2.0f, () -> Float.valueOf(this.cfg.boaLoadPace), v -> {
            this.cfg.boaLoadPace = v.floatValue();
        }, ConfigSlider.Format.PERCENT, null);
        y = this.slider(x, y, w, "The shooting", 0.5f, 2.0f, () -> Float.valueOf(this.cfg.boaShotPace), v -> {
            this.cfg.boaShotPace = v.floatValue();
        }, ConfigSlider.Format.PERCENT, null);
        y = this.slider(x, y, w, "The reveal", 0.5f, 2.0f, () -> Float.valueOf(this.cfg.boaRevealPace), v -> {
            this.cfg.boaRevealPace = v.floatValue();
        }, ConfigSlider.Format.PERCENT, null);
        y = this.note(y, "Only the pacing. The odds are fixed and never shown.");
        y = this.note(y, "Try your timings with \u00a76/boapreview\u00a7r - a kill");
        y = this.note(y, "rolled like a real one, counted nowhere.");
        y = this.heading(y, "Look");
        y = this.slider(x, y, w, "Background darkness", 0.0f, 1.0f, () -> Float.valueOf(this.cfg.boaScrimStrength), v -> {
            this.cfg.boaScrimStrength = v.floatValue();
        }, ConfigSlider.Format.PERCENT, null);
        y = this.heading(y, "While it plays");
        y = this.toggle(x, y, w, "Gameplay during Animation", () -> this.cfg.boaGameplayView, v -> {
            this.cfg.boaGameplayView = v;
        });
        y = this.cornerButton(x, y, w);
        y = this.note(y, "Your game, very small, so you can see where you are");
        y = this.note(y, "and move. The HUD stays hidden and the sound off.");
        return y;
    }

    private int cornerButton(int x, int y, int w) {
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)this.cornerText(), b -> {
            GameplayCorner next = GameplayCorner.byName(this.cfg.boaGameplayCorner).next();
            this.cfg.boaGameplayCorner = next.name();
            this.cfg.save();
            b.setMessage(this.cornerText());
        }).bounds(x, y, w, 20).build(), y));
        return y + 22;
    }

    private Component cornerText() {
        return Component.literal((String)("Corner: \u00a7f" + GameplayCorner.byName(this.cfg.boaGameplayCorner).title()));
    }

    private int buildSound(int x, int y, int w) {
        y = this.heading(y, "Levels");
        y = this.slider(x, y, w, "Animation volume", 0.0f, 1.0f, () -> Float.valueOf(this.cfg.volume), v -> {
            this.cfg.volume = v.floatValue();
        }, ConfigSlider.Format.PERCENT, this.audio::applyVolume);
        y = this.slider(x, y, w, "Drum volume", 0.0f, 1.0f, () -> Float.valueOf(this.cfg.drumVolume), v -> {
            this.cfg.drumVolume = v.floatValue();
        }, ConfigSlider.Format.PERCENT, null);
        y = this.heading(y, "Switches");
        y = this.toggle(x, y, w, "Sounds", () -> this.cfg.soundsEnabled, v -> {
            this.cfg.soundsEnabled = v;
        });
        y = this.toggle(x, y, w, "Tribal drums", () -> this.cfg.drumsEnabled, v -> {
            this.cfg.drumsEnabled = v;
        });
        y = this.note(y, "Every sound is made by the mod as it plays, and the");
        y = this.note(y, "game stays silent while a reveal is on screen.");
        if (this.audio.audioBroken()) {
            y = this.note(y, "\u00a7cNo audio device found - the animation runs silently.");
        }
        return y;
    }

    private int buildGeneral(int x, int y, int w) {
        y = this.heading(y, "Mod");
        y = this.toggle(x, y, w, "Only on Hypixel", () -> this.cfg.requireSkyblock, v -> {
            this.cfg.requireSkyblock = v;
        });
        y = this.toggle(x, y, w, "Summary in chat", () -> this.cfg.chatSummary, v -> {
            this.cfg.chatSummary = v;
        });
        y = this.slider(x, y, w, "Menu background dim", 0.0f, 0.9f, () -> Float.valueOf(this.cfg.menuDim), v -> {
            this.cfg.menuDim = v.floatValue();
        }, ConfigSlider.Format.PERCENT, null);
        y = this.toggle(x, y, w, "Beta features", () -> this.cfg.betaFeatures, v -> {
            this.cfg.betaFeatures = v;
        });
        y = this.note(y, "\u00a77Dry streak + RNG fanfare in the gambling result.");
        y = this.note(y, "\u00a77Off = the animations run exactly as before.");
        y = this.toggle(x, y, w, "Achievement pop-up & sound", () -> this.cfg.lightAchievementFx, v -> {
            this.cfg.lightAchievementFx = v;
        });
        y = this.slider(x, y, w, "Achievement sound volume", 0.0f, 1.0f, () -> Float.valueOf(this.cfg.lightAchievementVolume), v -> {
            this.cfg.lightAchievementVolume = v.floatValue();
        }, ConfigSlider.Format.PERCENT, null);
        y = this.toggle(x, y, w, "Debug log", () -> this.cfg.debug, v -> {
            this.cfg.debug = v;
        });
        y = this.note(y, "Writes every detection step to logs/latest.log");
        y = this.note(y, "(lines starting with [SkyMaxx]). Leave off normally.");
        y = this.sectionHeader(x, y + 6, w, "Stamps", null, stampsOpen, () -> {
            stampsOpen = !stampsOpen;
        });
        if (stampsOpen) {
            y = this.openSection(y, () -> this.buildStamps(x + 10, 0, w - 10));
        }
        y = this.heading(y, "Fishing");
        y = this.toggle(x, y, w, "Hotspot text on the water", () -> this.cfg.hotspotTextOnWater, v -> {
            this.cfg.hotspotTextOnWater = v;
        });
        y = this.note(y, "The HOTSPOT sign and its buff lie flat on the");
        y = this.note(y, "water inside the ring - same font and colours,");
        y = this.note(y, "buffs shortened (FS, SCC, DHC, TC, TFC).");
        return y;
    }

    private int buildStamps(int x, int y, int w) {
        y = this.toggle(x, y, w, "Stamps", () -> this.cfg.stampsEnabled, v -> {
            this.cfg.stampsEnabled = v;
        });
        y = this.note(y, "Off: a drop is still counted and numbered.");
        y = this.heading(y, "Timings");
        y = this.slider(x, y, w, "Delay before stamp", 0.0f, 4.0f, () -> Float.valueOf(this.cfg.stampDelaySeconds), v -> {
            this.cfg.stampDelaySeconds = v.floatValue();
        }, ConfigSlider.Format.SECONDS, null);
        y = this.slider(x, y, w, "Fade minimum", 0.2f, 12.0f, () -> Float.valueOf(this.cfg.stampFadeMinSeconds), v -> {
            this.cfg.stampFadeMinSeconds = v.floatValue();
        }, ConfigSlider.Format.SECONDS, null);
        y = this.slider(x, y, w, "Fade maximum", 0.2f, 12.0f, () -> Float.valueOf(this.cfg.stampFadeMaxSeconds), v -> {
            this.cfg.stampFadeMaxSeconds = v.floatValue();
        }, ConfigSlider.Format.SECONDS, null);
        y = this.note(y, "For every stamp, both creatures'.");
        return y;
    }

    private int buildStats(int x, int y, int w) {
        this.tikiStatsTop = y + (ITEM_ROWS + 1) * 15;
        y = this.tikiStatsTop + 90 + 6;
        y = this.resetButton(x, y, w, "Reset Wiki Tiki statistics", "Wiki Tiki", this.stats::reset);
        this.boaStatsTop = y + 14;
        y = this.boaStatsTop + 105 + 6;
        y = this.resetButton(x, y, w, "Reset Titanoboa statistics", "Titanoboa", this.stats::resetTitanoboa);
        this.luckTop = y + 14;
        y = this.luckTop + 13 * 15 + 6;
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal((String)"\u00a7dSave stat card (PNG)"), b -> {
            try {
                java.io.File file = com.skymaxx.tiki.light.StatCard.save();
                TikiGambleClient.chat("\u00a7d[Light] \u00a7fStat card saved: \u00a77screenshots/" + file.getName());
                net.minecraft.util.Util.getPlatform().openFile(file);
            } catch (Throwable e) {
                TikiGambleClient.LOGGER.warn("[Light] stat card failed", e);
                TikiGambleClient.chat("\u00a7c[Light] Could not save the stat card: " + e.getMessage());
            }
        }).bounds(x, y, w, 20).build(), y));
        return y + 26;
    }

    private static final int DYE_STATUS_H = 66;
    private static final int DYE_CARD_H = 100;
    private static final int DYE_GAP = 8;

    private int buildDyes(int x, int y, int w) {
        if (query.isBlank()) {
            // overview cards are drawn first, the settings follow below them
            this.dyesTop = y + 4;
            y = this.dyesTop + DYE_STATUS_H + DYE_GAP + com.skymaxx.tiki.light.dye.Dye.values().length * (DYE_CARD_H + DYE_GAP);
        }
        y = this.heading(y, "Vincent dye bonus");
        int n = com.skymaxx.tiki.light.dye.Dye.values().length;
        int bw = (w - (n - 1) * 4) / n;
        int i = 0;
        for (com.skymaxx.tiki.light.dye.Dye dye : com.skymaxx.tiki.light.dye.Dye.values()) {
            final com.skymaxx.tiki.light.dye.Dye fd = dye;
            this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal(DyeHudAgo.vincentLabel(fd)), b -> {
                int next = com.skymaxx.tiki.light.dye.DyeTracker.vincent(fd) % 3 + 1;
                com.skymaxx.tiki.light.dye.DyeTracker.setVincent(fd, next);
                this.cfg.save();
                b.setMessage((Component)Component.literal(DyeHudAgo.vincentLabel(fd)));
            }).bounds(x + i * (bw + 4), y, bw, 20).build(), y));
            ++i;
        }
        y += 24;
        y = this.note(y, "§8Set automatically by Vincent or /dyes - click to change.");
        y = this.heading(y, "Dye HUD");
        y = this.toggle(x, y, w, "Dye HUD", () -> this.cfg.dyeHudEnabled, v -> {
            this.cfg.dyeHudEnabled = v;
        });
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal(SkyMaxxScreen.hudMode(this.cfg.dyeHudOnlyFishing)), btn -> {
            this.cfg.dyeHudOnlyFishing = !this.cfg.dyeHudOnlyFishing;
            this.cfg.save();
            btn.setMessage((Component)Component.literal(SkyMaxxScreen.hudMode(this.cfg.dyeHudOnlyFishing)));
        }).bounds(x, y, w, 20).build(), y));
        y += 22;
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal((String)"§dMove and resize HUD"), b -> {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen((Screen)new com.skymaxx.tiki.light.dye.DyeHudEditScreen((Screen)this));
            }
        }).bounds(x, y, w, 20).build(), y));
        y += 22;
        y = this.heading(y, "Dyes in the HUD");
        y = this.toggle(x, y, w, "Aquamarine", () -> this.cfg.dyeShowAquamarine, v -> {
            this.cfg.dyeShowAquamarine = v;
        });
        y = this.toggle(x, y, w, "Iceberg", () -> this.cfg.dyeShowIceberg, v -> {
            this.cfg.dyeShowIceberg = v;
        });
        y = this.toggle(x, y, w, "Carmine", () -> this.cfg.dyeShowCarmine, v -> {
            this.cfg.dyeShowCarmine = v;
        });
        y = this.toggle(x, y, w, "Midnight", () -> this.cfg.dyeShowMidnight, v -> {
            this.cfg.dyeShowMidnight = v;
        });
        y = this.heading(y, "How the data gets in");
        y = this.note(y, "§fKills: §7open §f/pv§7 (SkyBlock Profile Viewer) once.");
        y = this.note(y, "§7New catches are then counted live.");
        y = this.note(y, "§fMagic Find: §7SkyBlock Menu → hover \"Your SkyBlock Profile\".");
        y = this.note(y, "§fVincent: §7talk to him or /dyes, once per SkyBlock year.");
        y = this.note(y, "§8Chance tables based on DyeAddons by anlg (Apache-2.0).");
        return y + 10;
    }

    private static String hudMode(boolean rodOnly) {
        return "Show HUD: " + (rodOnly ? "\u00a7aOnly with fishing rod in hand" : "\u00a7eAlways");
    }

    /** One small "label ... value" cell of a two-column grid. */
    private void cell(GuiGraphicsExtractor ctx, int x, int w, int y, String key, String value) {
        ctx.text(this.font, (Component)Component.literal(key), x, y, -7695484);
        ctx.text(this.font, (Component)Component.literal(value), x + w - this.font.width(value), y, -1514280);
    }

    private void card(GuiGraphicsExtractor ctx, int x, int right, int y, int h, int accent) {
        ctx.fill(x, y, right, y + h, 0x2A000000);
        ctx.outline(x, y, right - x, h, 0x40FFFFFF);
        ctx.fill(x, y, x + 3, y + h, accent);
    }

    private void drawDyes(GuiGraphicsExtractor ctx) {
        int x = this.cardLeft + 14 - 4;
        int right = this.cardRight - 14 - 3;
        int y = this.viewTop + this.dyesTop - this.scroll;
        int inner = x + 10;
        int innerW = right - inner - 10;
        int colW = (innerW - 16) / 2;
        int col2 = inner + colW + 16;
        com.skymaxx.tiki.light.dye.DyeTracker.Data d = com.skymaxx.tiki.light.dye.DyeTracker.data();

        // ---- data sources ----
        this.card(ctx, x, right, y, DYE_STATUS_H, PINK);
        ctx.text(this.font, (Component)Component.literal("§lData"), inner, y + 7, PINK);
        String kills;
        if (d.pvReadMs > 0L) {
            kills = "§a✔ §fProfile Viewer §8· " + d.pvProfile + " · " + DyeHudAgo.ago(d.pvReadMs);
        } else {
            kills = "§c✖ §7open /pv for your own profile";
        }
        String mf = d.magicFind < 0 ? "§c✖ §7SkyBlock Menu" : "§a✔ §f" + Math.round(d.magicFind);
        String vin = com.skymaxx.tiki.light.dye.DyeTracker.vincentLoaded() ? "§a✔ §floaded" : "§e✖ §7Vincent or /dyes";
        this.cell(ctx, inner, innerW, y + 22, "Kills", kills);
        this.cell(ctx, inner, innerW, y + 36, "Magic Find", mf);
        this.cell(ctx, inner, innerW, y + 50, "Vincent bonus", vin);
        y += DYE_STATUS_H + DYE_GAP;

        // ---- one card per dye ----
        for (com.skymaxx.tiki.light.dye.Dye dye : com.skymaxx.tiki.light.dye.Dye.values()) {
            com.skymaxx.tiki.light.dye.DyeTracker.Progress p = com.skymaxx.tiki.light.dye.DyeTracker.progress(dye);
            this.card(ctx, x, right, y, DYE_CARD_H, dye.color);
            ctx.fill(x + 3, y, right, y + 20, 0x22FFFFFF);
            ctx.text(this.font, (Component)Component.literal("§l" + dye.itemName), inner, y + 6, dye.color);
            String pct = String.format("%.1f%%", p.chance() * 100.0);
            String mult = String.format("x%.2f  ", com.skymaxx.tiki.light.dye.DyeTracker.multiplier(dye));
            ctx.text(this.font, (Component)Component.literal("§l" + pct), right - 10 - this.font.width("§l" + pct), y + 6, -1);
            ctx.text(this.font, (Component)Component.literal("§8" + mult), right - 10 - this.font.width("§l" + pct) - this.font.width(mult), y + 6, -1);
            // progress bar towards "you should have one"
            int barY = y + 22;
            ctx.fill(x + 3, barY, right, barY + 3, 0x30FFFFFF);
            ctx.fill(x + 3, barY, x + 3 + (int)Math.round((right - x - 3) * Math.max(0.0, Math.min(1.0, p.chance()))), barY + 3, dye.color);

            java.util.List<String> all = com.skymaxx.tiki.light.dye.DyeOdds.creatures(dye);
            int have = 0;
            for (String c : all) {
                if (d.kills.containsKey(c)) ++have;
            }
            long k50 = p.killsFor(0.5);
            long k90 = p.killsFor(0.9);
            int ry = y + 32;
            this.cell(ctx, inner, colW, ry, "Your kills", String.format("%,d", p.kills()));
            this.cell(ctx, col2, colW, ry, "Loot share", p.lootShare() > 0L ? String.format("%,d §8(1/5)", p.lootShare()) : "-");
            ry += 13;
            this.cell(ctx, inner, colW, ry, "Expected", String.format("%.2f", p.expected()));
            this.cell(ctx, col2, colW, ry, "Found", String.valueOf(p.found()));
            ry += 13;
            this.cell(ctx, inner, colW, ry, "50% at", k50 < 0 ? "-" : String.format("%,d", k50));
            this.cell(ctx, col2, colW, ry, "90% at", k90 < 0 ? "-" : String.format("%,d", k90));
            ry += 13;
            this.cell(ctx, inner, colW, ry, "Since last", String.format("%,d", p.sinceLast()));
            this.cell(ctx, col2, colW, ry, "Creatures", (have == 0 ? "§c" : have < all.size() ? "§e" : "§a") + have + "/" + all.size());
            ry += 15;
            if (p.bestCreature() != null) {
                String best = String.format("1 in %,d", Math.round(1.0 / p.bestChance()));
                ctx.text(this.font, (Component)Component.literal("§8Best: §7" + p.bestCreature() + " §8· " + DyeHudAgo.where(dye)), inner, ry, -1);
                ctx.text(this.font, (Component)Component.literal("§7" + best), inner + innerW - this.font.width(best), ry, -1);
            }
            y += DYE_CARD_H + DYE_GAP;
        }
    }

    private int buildAchievements(int x, int y, int w) {
        this.achievementsTop = y + 20;
        int h = 0;
        for (com.skymaxx.tiki.light.Achievements.Category cat : com.skymaxx.tiki.light.Achievements.Category.values()) {
            int n = 0;
            for (com.skymaxx.tiki.light.Achievements a : com.skymaxx.tiki.light.Achievements.values()) if (a.category == cat) ++n;
            h += 26 + ((n + 1) / 2) * 46;
        }
        return this.achievementsTop + h + 6;
    }

    /** Achievements as a two-column grid of cards: gold star and frame when unlocked. */
    private void drawAchievements(GuiGraphicsExtractor ctx) {
        int x = this.cardLeft + 14;
        int right = this.cardRight - 14 - 3 - 4;
        int top = this.viewTop - this.scroll;
        int got = com.skymaxx.tiki.light.Achievements.count(this.cfg);
        int total = com.skymaxx.tiki.light.Achievements.values().length;
        ctx.text(this.font, (Component)Component.literal((String)("Achievements  \u00a7f" + got + " / " + total)), x, top + 3, PINK);
        int y = this.viewTop + this.achievementsTop - this.scroll;
        int colW = (right - x + 8 - 8) / 2;
        for (com.skymaxx.tiki.light.Achievements.Category cat : com.skymaxx.tiki.light.Achievements.Category.values()) {
            int inCat = 0;
            int doneInCat = 0;
            for (com.skymaxx.tiki.light.Achievements a : com.skymaxx.tiki.light.Achievements.values()) {
                if (a.category != cat) continue;
                ++inCat;
                if (a.isUnlocked(this.cfg)) ++doneInCat;
            }
            if (y > this.viewTop - 30 && y < this.viewBottom + 4) {
                ctx.text(this.font, (Component)Component.literal((String)(cat.title + "  \u00a77" + doneInCat + " / " + inCat)), x, y + 8, PINK);
                int from = x + this.font.width(cat.title + "  " + doneInCat + " / " + inCat) + 8;
                if (from < right) ctx.fill(from, y + 12, right, y + 13, PINK_LINE);
            }
            y += 26;
            int i = 0;
            for (com.skymaxx.tiki.light.Achievements a : com.skymaxx.tiki.light.Achievements.values()) {
                if (a.category != cat) continue;
                boolean done = a.isUnlocked(this.cfg);
                int cx = x - 4 + (i % 2) * (colW + 8);
                int cy = y + (i / 2) * 46;
                if (cy > this.viewTop - 46 && cy < this.viewBottom + 4) {
                    ctx.fill(cx, cy, cx + colW, cy + 40, done ? 0x22FFD24A : 0x40000000);
                    ctx.outline(cx, cy, colW, 40, done ? 0xFFFFD24A : 0xFF555555);
                    ctx.text(this.font, (Component)Component.literal(done ? "\u00a76\u2605" : "\u00a78\u2606"), cx + 7, cy + 16, -1);
                    ctx.text(this.font, (Component)Component.literal((String)a.title), cx + 20, cy + 8, done ? -1 : -5592406);
                    String desc = a.description;
                    while (desc.length() > 3 && this.font.width(desc) > colW - 26) desc = desc.substring(0, desc.length() - 2);
                    if (!desc.equals(a.description)) desc = desc.trim() + "\u2026";
                    ctx.text(this.font, (Component)Component.literal((String)desc), cx + 20, cy + 23, -7697782);
                }
                ++i;
            }
            y += ((i + 1) / 2) * 46;
        }
    }

    private int resetButton(int x, int y, int w, String label, String creature, Runnable reset) {
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)Component.literal((String)label), b -> {
            if (this.minecraft == null) {
                return;
            }
            this.minecraft.gui.setScreen((Screen)new ConfirmScreen(yes -> {
                if (yes) {
                    reset.run();
                }
                this.minecraft.gui.setScreen((Screen)this);
            }, (Component)Component.literal((String)"Are you sure?"), (Component)Component.literal((String)("Every " + creature + " statistic and its share of the items dropped will be reset to zero. This cannot be undone.")), (Component)Component.literal((String)"\u00a7cReset"), (Component)Component.literal((String)"Cancel")));
        }).bounds(x, y, w, 20).build(), y));
        return y + 26;
    }

    private int heading(int y, String text) {
        int top = y == 0 ? y : y + 10;
        this.labels.add(new Label(top, text, true, this.indent));
        return top + 16;
    }

    private int note(int y, String text) {
        this.labels.add(new Label(y, text, false, this.indent));
        return y + 15;
    }

    private int slider(int x, int y, int w, String label, float min, float max, Supplier<Float> getter, Consumer<Float> setter, ConfigSlider.Format format, Runnable onChange) {
        this.rows.add(new Row((AbstractWidget)new ConfigSlider(x, y, w, 20, label, min, max, getter, v -> {
            setter.accept((Float)v);
            this.cfg.clamp();
            this.cfg.save();
        }, format, onChange), y));
        return y + 22;
    }

    private int toggle(int x, int y, int w, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        this.rows.add(new Row((AbstractWidget)Button.builder((Component)SkyMaxxScreen.toggleText(label, getter.getAsBoolean()), b -> {
            boolean next = !getter.getAsBoolean();
            setter.accept(next);
            this.cfg.save();
            b.setMessage(SkyMaxxScreen.toggleText(label, next));
        }).bounds(x, y, w, 20).build(), y));
        return y + 22;
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal((String)(label + ": " + (on ? "\u00a7aON" : "\u00a77OFF")));
    }

    private int maxScroll() {
        return Math.max(0, this.contentHeight - (this.viewBottom - this.viewTop));
    }

    private void applyScroll() {
        this.scroll = Math.max(0, Math.min(this.scroll, this.maxScroll()));
        for (Row row : this.rows) {
            AbstractWidget widget = row.widget();
            int y = this.viewTop + row.baseY() - this.scroll;
            widget.setY(y);
            widget.active = widget.visible = y + widget.getHeight() > this.viewTop && y < this.viewBottom;
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= (double)this.cardLeft && mouseX <= (double)this.cardRight && mouseY >= (double)this.cardTop && mouseY <= (double)this.cardBottom) {
            this.scroll -= (int)(scrollY * 18.0);
            this.applyScroll();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int trackTop() {
        return this.viewTop - 2;
    }

    private int trackBottom() {
        return this.viewBottom + 2;
    }

    private int scrollbarX() {
        return this.cardRight - 7 - 3;
    }

    private int thumbHeight() {
        int track = this.trackBottom() - this.trackTop();
        return Math.max(24, (int)((long)track * (long)track / (long)(track + this.maxScroll())));
    }

    private int thumbTop() {
        int max = this.maxScroll();
        int travel = this.trackBottom() - this.trackTop() - this.thumbHeight();
        return this.trackTop() + (max <= 0 ? 0 : (int)((long)travel * (long)this.scroll / (long)max));
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        int x = this.scrollbarX();
        return this.maxScroll() > 0 && mouseX >= (double)(x - 5) && mouseX < (double)(x + 3 + 5) && mouseY >= (double)this.trackTop() && mouseY < (double)this.trackBottom();
    }

    private void dragScrollbarTo(double mouseY) {
        int travel = this.trackBottom() - this.trackTop() - this.thumbHeight();
        if (travel <= 0) {
            return;
        }
        double k = (mouseY - this.grabOffset - (double)this.trackTop()) / (double)travel;
        this.scroll = (int)Math.round(Math.max(0.0, Math.min(1.0, k)) * (double)this.maxScroll());
        this.applyScroll();
    }

    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.draggingScrollbar) {
            this.dragScrollbarTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.draggingScrollbar) {
            this.draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        int index;
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0 && this.overScrollbar(mouseX, mouseY)) {
            int top = this.thumbTop();
            int thumb = this.thumbHeight();
            this.draggingScrollbar = true;
            if (mouseY >= (double)top && mouseY < (double)(top + thumb)) {
                this.grabOffset = mouseY - (double)top;
            } else {
                this.grabOffset = (double)thumb / 2.0;
                this.dragScrollbarTo(mouseY);
            }
            return true;
        }
        if ((index = this.tabAt(mouseX, mouseY)) >= 0) {
            Tab target = Tab.values()[index];
            if (target != activeTab || !query.isEmpty()) {
                activeTab = target;
                query = "";
                this.scroll = 0;
                this.rebuildWidgets();
            }
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    /** Tab height: 24 when there is room, smaller on small windows so all tabs stay above the Done button. */
    private int tabStep() {
        int room = this.winBottom - 32 - (this.headerBottom + 8);
        return Math.max(12, Math.min(24, room / Tab.values().length));
    }

    private int tabAt(double mouseX, double mouseY) {
        if (mouseX < (double)(this.winLeft + 6) || mouseX >= (double)(this.sideRight - 6)) {
            return -1;
        }
        for (int i = 0; i < Tab.values().length; ++i) {
            int top = this.headerBottom + 8 + i * this.tabStep();
            if (!(mouseY >= (double)top) || !(mouseY < (double)(top + this.tabStep() - 2))) continue;
            return i;
        }
        return -1;
    }

    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partial) {
        this.drawCard(ctx);
        this.drawTitle(ctx);
        this.drawTabs(ctx, mouseX, mouseY);
        ctx.enableScissor(this.cardLeft + 1, this.viewTop - 4, this.cardRight - 1, this.viewBottom + 4);
        this.drawLabels(ctx);
        for (Row row : this.rows) {
            if (!row.widget().visible) continue;
            row.widget().extractRenderState(ctx, mouseX, mouseY, partial);
        }
        if (activeTab == Tab.ACHIEVEMENTS && query.isBlank()) {
            this.drawAchievements(ctx);
        }
        if (activeTab == Tab.DYES && query.isBlank()) {
            this.drawDyes(ctx);
        }
        if (activeTab == Tab.STATS && query.isBlank()) {
            this.drawStats(ctx);
        }
        ctx.disableScissor();
        this.drawScrollbar(ctx, mouseX, mouseY);
        super.extractRenderState(ctx, mouseX, mouseY, partial);
    }

    private void drawTitle(GuiGraphicsExtractor ctx) {
        Matrix3x2fStack pose = ctx.pose();
        pose.pushMatrix();
        pose.translate((float)(this.winLeft + 12), (float)(this.winTop + 8));
        pose.scale(2.0f, 2.0f);
        ctx.text(this.font, (Component)Component.literal((String)"\u00a7lLight"), 0, 0, PINK);
        pose.popMatrix();
        int titleWidth = this.font.width((net.minecraft.network.chat.FormattedText)Component.literal("Light").withStyle(net.minecraft.ChatFormatting.BOLD));
        int after = this.winLeft + 12 + titleWidth * 2 + 14;
        ctx.text(this.font, (Component)Component.literal((String)"Wiki Tiki \u00b7 Titanoboa \u00b7 Bazaar"), after, this.winTop + 15, -7695484);
        // Light: credits for the original mod
        int creditY = this.winBottom - 28 - 34;
        if (creditY > this.headerBottom + 8 + Tab.values().length * this.tabStep() + 4) {
            ctx.text(this.font, (Component)Component.literal((String)"Light by Grundsteuer"), this.winLeft + 10, creditY, -7695484);
            ctx.text(this.font, (Component)Component.literal((String)"Tiki & Boa: SkyMaxx"), this.winLeft + 10, creditY + 10, -7695484);
            ctx.text(this.font, (Component)Component.literal((String)"by Rachie \u00b7 MIT"), this.winLeft + 10, creditY + 20, -7695484);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float partial) {
        // Light: no full-screen blur in game, only a light dim so the world stays visible
        if (this.minecraft == null || this.minecraft.level == null) {
            super.extractBackground(ctx, mouseX, mouseY, partial);
            return;
        }
        int dim = Math.round(Math.max(0f, Math.min(0.9f, this.cfg == null ? 0.55f : this.cfg.menuDim)) * 255f);
        ctx.fill(0, 0, this.width, this.height, dim << 24);
    }

    private void drawTabs(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        int hovered = this.tabAt(mouseX, mouseY);
        int live = query.isBlank() ? activeTab.ordinal() : -1;
        Tab[] all = Tab.values();
        for (int i = 0; i < all.length; ++i) {
            int x0 = this.winLeft + 6;
            int x1 = this.sideRight - 6;
            int step = this.tabStep();
            int y0 = this.headerBottom + 8 + i * step;
            int y1 = y0 + step - 2;
            if (i == live) {
                ctx.fill(x0, y0, x1, y1, PINK_ACTIVE);
                ctx.fill(x0, y0, x0 + 2, y1, PINK);
            } else if (i == hovered) {
                ctx.fill(x0, y0, x1, y1, 0x22FFFFFF);
            }
            ctx.text(this.font, (Component)Component.literal((String)all[i].title), x0 + 10, y0 + (y1 - y0 - 8) / 2, i == live ? -1 : -3355444);
        }
    }

    private void drawCard(GuiGraphicsExtractor ctx) {
        ctx.fill(this.winLeft + 3, this.winTop + 3, this.winRight + 3, this.winBottom + 3, 0x40000000);
        ctx.fill(this.winLeft, this.winTop, this.winRight, this.winBottom, GLASS);
        ctx.fill(this.winLeft, this.winTop, this.winRight, this.headerBottom, GLASS_HEADER);
        ctx.fill(this.winLeft, this.headerBottom, this.sideRight, this.winBottom, GLASS_SIDE);
        ctx.fill(this.winLeft, this.headerBottom - 1, this.winRight, this.headerBottom, PINK_LINE);
        ctx.fill(this.sideRight - 1, this.headerBottom, this.sideRight, this.winBottom, PINK_LINE);
        ctx.outline(this.winLeft, this.winTop, this.winRight - this.winLeft, this.winBottom - this.winTop, PINK_SOFT);
    }

    private void drawLabels(GuiGraphicsExtractor ctx) {
        int right = this.cardRight - 14 - 3 - 4;
        for (Panel panel : this.panels) {
            int top = this.viewTop + panel.top() - this.scroll;
            int bottom = this.viewTop + panel.bottom() - this.scroll;
            if (bottom <= this.viewTop - 4 || top >= this.viewBottom + 4) continue;
            ctx.fill(this.cardLeft + 14 - 4, top, right + 4, bottom, 0x14FFFFFF);
            ctx.fill(this.cardLeft + 14 - 4, top, this.cardLeft + 14 - 3, bottom, PINK_SOFT);
        }
        for (Label label : this.labels) {
            int x = this.cardLeft + 14 + label.indent();
            int y = this.viewTop + label.baseY() - this.scroll;
            if (y < this.viewTop - 20 || y > this.viewBottom + 4) continue;
            if (label.heading()) {
                ctx.text(this.font, (Component)Component.literal((String)label.text()), x, y + 3, PINK);
                int from = x + this.font.width(label.text()) + 6;
                if (from >= right) continue;
                ctx.fill(from, y + 7, right, y + 8, 1143881779);
                continue;
            }
            ctx.text(this.font, (Component)Component.literal((String)label.text()), x, y + 3, -7695484);
        }
    }

    private void drawScrollbar(GuiGraphicsExtractor ctx, int mouseX, int mouseY) {
        if (this.maxScroll() <= 0) {
            return;
        }
        int x = this.scrollbarX();
        boolean live = this.draggingScrollbar || this.overScrollbar(mouseX, mouseY);
        int grow = live ? 1 : 0;
        ctx.fill(x - grow, this.trackTop(), x + 3 + grow, this.trackBottom(), 0x22000000);
        int top = this.thumbTop();
        ctx.fill(x - grow, top, x + 3 + grow, top + this.thumbHeight(), live ? -856309190 : 1442169402);
    }

    private void drawStats(GuiGraphicsExtractor ctx) {
        int x = this.cardLeft + 14;
        int right = this.cardRight - 14 - 3 - 4;
        int y = this.viewTop - this.scroll;
        int row = this.stat(ctx, x, right, y, 0, "\u00a76Items dropped", "");
        for (DropItem item : DropItem.values()) {
            row = this.stat(ctx, x, right, y, row, item.plural(), String.valueOf(this.stats.counted(item)));
        }
        int ty = this.viewTop + this.tikiStatsTop - this.scroll;
        int t = 0;
        t = this.stat(ctx, x, right, ty, t, "\u00a76Wiki Tiki", "");
        t = this.stat(ctx, x, right, ty, t, "Kills", String.valueOf(this.stats.kills));
        t = this.stat(ctx, x, right, ty, t, "Reached mask 4", this.stats.reachedFinalReel + "  (" + SkyMaxxScreen.percent(this.stats.reachedFinalReel, this.stats.kills) + ")");
        t = this.stat(ctx, x, right, ty, t, "Masks landed", this.stats.reelLandings[0] + " / " + this.stats.reelLandings[1] + " / " + this.stats.reelLandings[2] + " / " + this.stats.reelLandings[3]);
        t = this.stat(ctx, x, right, ty, t, "Current dry streak", this.stats.dryStreak + " kills");
        this.stat(ctx, x, right, ty, t, "Worst dry streak", this.stats.worstDryStreak + " kills");
        int by = this.viewTop + this.boaStatsTop - this.scroll;
        int r = 0;
        r = this.stat(ctx, x, right, by, r, "\u00a76Titanoboa", "");
        r = this.stat(ctx, x, right, by, r, "Kills", String.valueOf(this.stats.boaKills));
        r = this.stat(ctx, x, right, by, r, "Chambers of 5 / 6 / 7 / 8", this.stats.bulletRolls[0] + " / " + this.stats.bulletRolls[1] + " / " + this.stats.bulletRolls[2] + " / " + this.stats.bulletRolls[3]);
        r = this.stat(ctx, x, right, by, r, "Reached the head", this.stats.segmentsDown[3] + "  (" + SkyMaxxScreen.percent(this.stats.segmentsDown[3], this.stats.boaKills) + ")");
        r = this.stat(ctx, x, right, by, r, "Segments shot", this.stats.segmentsDown[0] + " / " + this.stats.segmentsDown[1] + " / " + this.stats.segmentsDown[2] + " / " + this.stats.segmentsDown[3] + " / " + this.stats.segmentsDown[4]);
        r = this.stat(ctx, x, right, by, r, "Current dry streak", this.stats.boaDryStreak + " kills");
        this.stat(ctx, x, right, by, r, "Worst dry streak", this.stats.boaWorstDryStreak + " kills");
        int ly = this.viewTop + this.luckTop - this.scroll;
        int l = 0;
        l = this.stat(ctx, x, right, ly, l, "\u00a7dLuck meter \u00a78(base chances, no Magic Find)", "");
        l = this.stat(ctx, x, right, ly, l, "Overall (without Aquamarine)", com.skymaxx.tiki.light.Luck.format(com.skymaxx.tiki.light.Luck.overall(this.stats)));
        l = this.stat(ctx, x, right, ly, l, "\u00a76Wiki Tiki", "");
        for (com.skymaxx.tiki.light.Luck.Line line : com.skymaxx.tiki.light.Luck.tiki(this.stats)) {
            l = this.stat(ctx, x, right, ly, l, line.name(), line.got() + " \u00a78/ " + String.format("%.1f", line.expected()) + "  " + com.skymaxx.tiki.light.Luck.format(line.percent()));
        }
        l = this.stat(ctx, x, right, ly, l, "\u00a76Titanoboa", "");
        for (com.skymaxx.tiki.light.Luck.Line line : com.skymaxx.tiki.light.Luck.boa(this.stats)) {
            l = this.stat(ctx, x, right, ly, l, line.name(), line.got() + " \u00a78/ " + String.format("%.1f", line.expected()) + "  " + com.skymaxx.tiki.light.Luck.format(line.percent()));
        }
    }

    private int stat(GuiGraphicsExtractor ctx, int x, int right, int top, int row, String key, String value) {
        if (key.isEmpty()) {
            return row + 1;
        }
        int y = top + row * 15;
        if (row % 2 == 0) {
            ctx.fill(x - 4, y - 2, right + 4, y + 15 - 3, 0x18FFFFFF);
        }
        ctx.text(this.font, (Component)Component.literal((String)key), x, y + 2, -7695484);
        ctx.text(this.font, (Component)Component.literal((String)value), right - this.font.width(value), y + 2, -1514280);
        return row + 1;
    }

    private static String percent(int part, int whole) {
        return whole <= 0 ? "-" : String.format("%.1f%%", 100.0 * (double)part / (double)whole);
    }

    public void onClose() {
        this.cfg.clamp();
        this.cfg.save();
        this.audio.applyVolume();
        if (this.parent != null && this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }

    public boolean isPauseScreen() {
        return false;
    }

    public static SkyMaxxScreen create() {
        return SkyMaxxScreen.create(null);
    }

    public static SkyMaxxScreen create(Screen parent) {
        return new SkyMaxxScreen(TikiGambleClient.config, TikiGambleClient.stats, TikiGambleClient.director, TikiGambleClient.audio, parent);
    }

    static {
        ITEM_ROWS = 1 + DropItem.values().length;
    }

    private static enum Tab {
        GENERAL("General"),
        GAMBLING("Gambling"),
        DYES("Dyes"),
        SOUND("Sound"),
        BAZAAR("Bazaar"),
        HUDS("HUDs"),
        STATS("Stats"),
        HISTORY("History"),
        ACHIEVEMENTS("Achievements"),
        SETTINGS("Settings");

        final String title;

        private Tab(String title) {
            this.title = title;
        }
    }

    private record Row(AbstractWidget widget, int baseY) {
    }

    private record Label(int baseY, String text, boolean heading, int indent) {
    }

    private record Panel(int top, int bottom) {
    }
}

