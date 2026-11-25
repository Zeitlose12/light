package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import com.skymaxx.tiki.config.TikiConfig;
import com.skymaxx.tiki.detect.DropLines;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Light: blocks the "Sell Sacks Now" button in the Hypixel Bazaar so sacks
 * cannot be insta-sold by accident. It only cancels the player's own click
 * on the client - nothing is sent to the server and nothing is automated.
 *
 * Three independent layers use this class:
 *  1. the mouse click on the screen is swallowed,
 *  2. the slot click inside the screen is cancelled,
 *  3. the click packet itself is never built (last line of defence).
 * Slots where the button has ever been seen are remembered. While the server
 * lags and that slot is still empty, a click on it is blocked too, because the
 * server may already have the button there.
 */
public final class BazaarLock {
    private static final String BUTTON = "sell sacks now";
    private static long lastNoticeMs;

    private BazaarLock() {
    }

    private static boolean enabled() {
        return TikiGambleClient.config != null && TikiGambleClient.config.lockBazaarSellSacks;
    }

    public static boolean isBazaar(Screen screen) {
        if (screen == null) {
            return false;
        }
        String title = DropLines.stripFormatting(screen.getTitle().getString()).toLowerCase(Locale.ROOT);
        return title.contains("bazaar");
    }

    public static boolean isButton(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return DropLines.stripFormatting(stack.getHoverName().getString()).toLowerCase(Locale.ROOT).contains(BUTTON);
    }

    /** True when this stack is the locked Sell Sacks Now button in a Bazaar screen (no chat message). */
    public static boolean isLockedButton(Screen screen, ItemStack stack) {
        return BazaarLock.enabled() && BazaarLock.isBazaar(screen) && BazaarLock.isButton(stack);
    }

    /** Decides for a menu slot id. Remembers where the button lives. */
    public static boolean shouldBlock(Screen screen, AbstractContainerMenu menu, int slotId) {
        if (!BazaarLock.enabled() || !BazaarLock.isBazaar(screen)) {
            return false;
        }
        ItemStack stack = ItemStack.EMPTY;
        if (menu != null && slotId >= 0 && slotId < menu.slots.size()) {
            stack = menu.slots.get(slotId).getItem();
        }
        if (BazaarLock.isButton(stack)) {
            BazaarLock.remember(slotId);
            return true;
        }
        // Lag guard: the slot has held the button before and the client does not know yet what is there now
        return (stack == null || stack.isEmpty()) && BazaarLock.known().contains(slotId);
    }

    public static boolean shouldBlock(Screen screen, Slot slot) {
        if (slot == null || !(screen instanceof AbstractContainerScreen<?> container)) {
            return false;
        }
        AbstractContainerMenu menu = container.getMenu();
        int id = menu.slots.indexOf(slot);
        return id >= 0 && BazaarLock.shouldBlock(screen, menu, id);
    }

    /** Layer 3: called right before the click packet would be built. */
    public static boolean blocksPacket(int slotId) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.gui == null ? null : mc.gui.screen();
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return false;
        }
        return BazaarLock.shouldBlock(screen, container.getMenu(), slotId);
    }

    /** Kept for the first layer: same decision, plus the chat notice. */
    public static boolean blocks(Screen screen, Slot slot) {
        if (!BazaarLock.shouldBlock(screen, slot)) {
            return false;
        }
        BazaarLock.notice();
        return true;
    }

    private static Set<Integer> known() {
        LinkedHashSet<Integer> out = new LinkedHashSet<Integer>();
        TikiConfig cfg = TikiGambleClient.config;
        if (cfg == null || cfg.lightSellSacksSlots == null || cfg.lightSellSacksSlots.isBlank()) {
            return out;
        }
        for (String part : cfg.lightSellSacksSlots.split(",")) {
            try {
                out.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    private static void remember(int slotId) {
        Set<Integer> slots = BazaarLock.known();
        if (slotId < 0 || !slots.add(slotId)) {
            return;
        }
        TikiConfig cfg = TikiGambleClient.config;
        StringBuilder sb = new StringBuilder();
        for (Integer i : slots) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        cfg.lightSellSacksSlots = sb.toString();
        cfg.save();
    }

    public static void notice() {
        long now = System.currentTimeMillis();
        if (now - lastNoticeMs < 3000L) {
            return;
        }
        lastNoticeMs = now;
        TikiGambleClient.chat("§e[Light] §7Sell Sacks Now is locked. Use Sell Offers or turn the lock off in /light.");
    }
}
