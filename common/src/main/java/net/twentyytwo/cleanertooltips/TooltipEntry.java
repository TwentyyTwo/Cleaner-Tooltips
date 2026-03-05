package net.twentyytwo.cleanertooltips;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * An object storing all the necessary data for calculating and rendering a custom tooltip.<br>
 * Useful to avoid running the same code multiple times.
 * @param text      a mutable component of the attribute modifier value
 * @param textWidth the width of the {@code text}
 * @param icon      the texture of the corresponding attribute. can be {@code null}
 */
public record TooltipEntry(MutableComponent text, int textWidth, ResourceLocation icon) {}