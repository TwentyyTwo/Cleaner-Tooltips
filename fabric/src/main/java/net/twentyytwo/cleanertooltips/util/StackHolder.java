package net.twentyytwo.cleanertooltips.util;

import net.minecraft.world.item.ItemStack;

public class StackHolder {
    private static StackHolder instance;
    private ItemStack itemStack = ItemStack.EMPTY;

    private StackHolder() {}

    public static synchronized StackHolder getInstance() {
        if (instance == null) {
            instance = new StackHolder();
        }
        return instance;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void resetStack() {
        itemStack = ItemStack.EMPTY;
    }
}
