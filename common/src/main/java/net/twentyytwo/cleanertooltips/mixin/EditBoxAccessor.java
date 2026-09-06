package net.twentyytwo.cleanertooltips.mixin;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EditBox.class)
public interface EditBoxAccessor {

    @Accessor("suggestion")
    String getSuggestion();

    @Accessor("maxLength")
    int getMaxLength();

    @Accessor("highlightPos")
    int getHighlightPos();

    @Invoker("onValueChange")
    void invokeOnValueChange(String value);
}
