package net.twentyytwo.cleanertooltips.api;

import net.minecraft.world.item.ItemStack;

public interface IStackHolder {
    void cleanerTooltips$setStack(ItemStack data);
    ItemStack cleanerTooltips$getStack();
}
