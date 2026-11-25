package com.skymaxx.tiki.detect;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.detect.DropLines;
import com.skymaxx.tiki.detect.InventoryDiff;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class InventoryWatch {
    public static final long AFTER_CONTAINER_MS = 1500L;
    public static final long AFTER_WORLD_SWITCH_MS = 3000L;
    private static final int LOG_LINES = 30;
    private final TikiConfig cfg;
    private final List<Listener> listeners = new ArrayList<Listener>();
    private final InventoryDiff diff = new InventoryDiff();
    private final Map<InventoryDiff.Item, Integer> counts = new HashMap<InventoryDiff.Item, Integer>();
    private final ArrayDeque<String> recent = new ArrayDeque();
    private Object lastLevel;
    private Object lastPlayer;
    private long quietUntilMs;

    public InventoryWatch(TikiConfig cfg) {
        this.cfg = cfg;
    }

    public void listen(Listener listener) {
        this.listeners.add(listener);
    }

    public void tick(Minecraft mc) {
        boolean ownInventory;
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) {
            this.reset();
            return;
        }
        long now = System.currentTimeMillis();
        if (mc.level != this.lastLevel || player != this.lastPlayer) {
            this.lastLevel = mc.level;
            this.lastPlayer = player;
            this.quietUntilMs = Math.max(this.quietUntilMs, now + 3000L);
        }
        boolean bl = ownInventory = player.containerMenu == player.inventoryMenu;
        if (!ownInventory) {
            this.quietUntilMs = Math.max(this.quietUntilMs, now + 1500L);
        }
        this.counts.clear();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            this.add(inventory.getItem(i));
        }
        if (ownInventory) {
            this.add(player.inventoryMenu.getCarried());
            CraftingContainer grid = player.inventoryMenu.getCraftSlots();
            for (int i = 0; i < grid.getContainerSize(); ++i) {
                this.add(grid.getItem(i));
            }
        }
        for (InventoryDiff.Gain gain : this.diff.update(this.counts, now, now < this.quietUntilMs)) {
            this.report(gain.item().name(), gain.item().id(), gain.count(), false);
        }
    }

    private void add(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        String name = DropLines.stripFormatting(stack.getHoverName().getString()).trim();
        String id = "";
        CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            id = data.copyTag().getStringOr("id", "");
        }
        this.counts.merge(new InventoryDiff.Item(name, id), stack.getCount(), Integer::sum);
    }

    public void onChat(Component message, boolean overlay) {
        if (overlay || message == null) {
            return;
        }
        String line = DropLines.stripFormatting(message.getString()).trim();
        if (!DropLines.isSackReport(line)) {
            return;
        }
        LinkedHashSet<String> tooltips = new LinkedHashSet<String>();
        for (Component part : message.toFlatList()) {
            HoverEvent hoverEvent = part.getStyle().getHoverEvent();
            if (!(hoverEvent instanceof HoverEvent.ShowText)) continue;
            HoverEvent.ShowText text = (HoverEvent.ShowText)hoverEvent;
            tooltips.add(DropLines.stripFormatting(text.value().getString()));
        }
        for (String tooltip : tooltips) {
            for (DropLines.Added added : DropLines.sackAdditions(tooltip)) {
                this.report(added.name(), "", added.count(), true);
            }
        }
    }

    private void report(String name, String id, int count, boolean viaSack) {
        this.remember("\u00a7a+" + count + " \u00a7f" + name + (String)(id.isEmpty() ? "" : " \u00a78" + id) + (viaSack ? " \u00a77(sack)" : ""));
        if (this.cfg.debug) {
            TikiGambleClient.LOGGER.info("[SkyMaxx] +{} {} {}{}", new Object[]{count, name, id, viaSack ? " (sack)" : ""});
        }
        for (Listener listener : this.listeners) {
            listener.onItemGained(name, id, count, viaSack);
        }
    }

    private void remember(String line) {
        this.recent.addLast(line);
        while (this.recent.size() > 30) {
            this.recent.removeFirst();
        }
    }

    public List<String> recentGains() {
        return new ArrayList<String>(this.recent);
    }

    public void reset() {
        this.diff.reset();
        this.counts.clear();
        this.lastLevel = null;
        this.lastPlayer = null;
        this.quietUntilMs = 0L;
    }

    public static interface Listener {
        public void onItemGained(String var1, String var2, int var3, boolean var4);
    }
}

