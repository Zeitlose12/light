package com.skymaxx.tiki.mixin;

import com.skymaxx.tiki.light.BazaarLock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Light: every click in a container screen (left, right, shift, number keys,
 * middle click) goes through slotClicked before a packet is sent. Cancelling
 * it here means the Sell Sacks Now click never leaves the client. The button
 * also gets a red X and a tooltip line, the tooltip itself stays readable.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class BazaarSellLockMixin {
    @Shadow
    protected Slot hoveredSlot;

    // Layer 1: swallow the mouse click before the screen handles it
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void light$blockMouseClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (this.hoveredSlot != null && BazaarLock.blocks((Screen)(Object)this, this.hoveredSlot)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, require = 0)
    private void light$blockMouseRelease(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.hoveredSlot != null && BazaarLock.shouldBlock((Screen)(Object)this, this.hoveredSlot)) {
            cir.setReturnValue(true);
        }
    }

    // Layer 2: cancel the slot click (also covers number keys, drop key and shift clicks)
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void light$lockSellSacks(Slot slot, int slotId, int button, ContainerInput type, CallbackInfo ci) {
        if (slot != null ? BazaarLock.blocks((Screen)(Object)this, slot) : false) {
            ci.cancel();
        }
    }

    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Inject(method = "extractSlots", at = @At("TAIL"), require = 0)
    private void light$crossOutSellSacks(GuiGraphicsExtractor ctx, int mouseX, int mouseY, CallbackInfo ci) {
        Screen screen = (Screen)(Object)this;
        boolean layered = false;
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive() || !BazaarLock.isLockedButton(screen, slot.getItem())) continue;
            if (!layered) {
                ctx.nextStratum();
                layered = true;
            }
            int x = slot.x;
            int y = slot.y;
            ctx.fill(x, y, x + 16, y + 16, 0x88000000);
            for (int i = 0; i < 16; ++i) {
                ctx.fill(x + i, y + i, x + i + 2, y + i + 1, 0xFFFF3B3B);
                ctx.fill(x + 14 - i, y + i, x + 16 - i, y + i + 1, 0xFFFF3B3B);
            }
        }
    }

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true, require = 0)
    private void light$lockedTooltip(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
        if (!BazaarLock.isLockedButton((Screen)(Object)this, stack)) {
            return;
        }
        ArrayList<Component> lines = new ArrayList<Component>(cir.getReturnValue());
        lines.add(Component.literal(""));
        lines.add(Component.literal("§c✖ Locked by Light §7- use Sell Offers"));
        lines.add(Component.literal("§8Turn off in /light → Bazaar"));
        cir.setReturnValue(lines);
    }
}
