package net.twentyytwo.cleanertooltips.api;

import net.minecraft.world.item.ItemStack;

public interface IItemStackHolder {
    void cleanerTooltips$setStack(ItemStack data);
    ItemStack cleanerTooltips$getStack();
}
