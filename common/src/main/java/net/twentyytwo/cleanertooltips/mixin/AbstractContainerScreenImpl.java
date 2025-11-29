package net.twentyytwo.cleanertooltips.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.twentyytwo.cleanertooltips.IStackHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenImpl implements IStackHolder {

    @Unique
    private ItemStack cleanerTooltips$stack;

    @Override
    public void cleanerTooltips$setStack(ItemStack stack) {
        this.cleanerTooltips$stack = stack;
    }

    @Override
    public ItemStack cleanerTooltips$getStack() {
        return this.cleanerTooltips$stack;
    }
}
