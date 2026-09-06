package net.twentyytwo.cleanertooltips.config.base;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.twentyytwo.cleanertooltips.mixin.EditBoxAccessor;
import org.jspecify.annotations.NonNull;

import java.util.function.Predicate;

public class FilterableEditBox extends EditBox {
    private Predicate<String> filter = _ -> true;

    public FilterableEditBox(Font font, int width, int height, Component narration) {
        this(font, 0, 0, width, height, narration);
    }

    public FilterableEditBox(Font font, int x, int y, int width, int height, Component narration) {
        super(font, x, y, width, height, narration);
    }

    public void setFilter(Predicate<String> filter) {
        this.filter = filter;
    }

    @Override
    public void setValue(@NonNull String value) {
        if (!this.filter.test(value)) return;

        super.setValue(value);
    }

    // Very messy function, but it works
    @Override
    public void insertText(@NonNull String input) {
        int start = Math.min(this.getCursorPosition(), ((EditBoxAccessor) this).getHighlightPos());
        int end = Math.max(this.getCursorPosition(), ((EditBoxAccessor) this).getHighlightPos());
        int maxInsertionLength = ((EditBoxAccessor) this).getMaxLength() - this.getValue().length() - (start - end);
        if (maxInsertionLength > 0) {
            String text = StringUtil.filterText(input);
            int insertionLength = text.length();
            if (maxInsertionLength < insertionLength) {
                if (Character.isHighSurrogate(text.charAt(maxInsertionLength - 1))) {
                    maxInsertionLength--;
                }

                text = text.substring(0, maxInsertionLength);
                insertionLength = maxInsertionLength;
            }

            String s = new StringBuilder(this.getValue()).replace(start, end, text).toString();
            if (!filter.test(s)) return;
            this.setValue(s);
            this.setCursorPosition(start + insertionLength);
            this.setHighlightPos(this.getCursorPosition());
            ((EditBoxAccessor) this).invokeOnValueChange(this.getValue());
        }
    }
}
