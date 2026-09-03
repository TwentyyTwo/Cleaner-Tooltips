package net.twentyytwo.cleanertooltips.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.twentyytwo.cleanertooltips.mixin.EditBoxAccessor;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BiFunction;

public class AutoEditBox extends EditBox {
    private final List<String> suggestions;
    private String previousSuggestion = "";
    private int shiftAmount = 0;

    public AutoEditBox(List<String> suggestions) {
        super(Minecraft.getInstance().font, 0, 0, 100, 18, Component.empty());
        this.suggestions = suggestions;
        this.onValueChange(this.getValue());
    }

    public void onValueChange(String text) {
        this.onValueChange(text, false);
    }

    public void onValueChange(String text, boolean indexChanged) {
        List<String> validSuggestions = suggestions.stream()
                .filter(id -> id.startsWith(text))
                .toList();

        if (!validSuggestions.isEmpty()) {
            shiftAmount %= validSuggestions.size();
            if (shiftAmount < 0) shiftAmount += validSuggestions.size();

            // If the shift amount hasn't changed, adjust it correctly in case it is misaligned.
            // Misalignment occurs if the currently displayed suggestion is still valid, but
            // suggestions that are alphabetically in front of the displayed suggestion aren't.
            if (!indexChanged && validSuggestions.contains(previousSuggestion)) {
                shiftAmount = validSuggestions.indexOf(previousSuggestion);
            }
            String suggestion = validSuggestions.get(shiftAmount);

            if (!text.equals(suggestion)) {
                previousSuggestion = suggestion;
                this.setSuggestion(suggestion.replaceFirst(text, ""));
                return;
            }
        }
        this.setSuggestion(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB && (modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            if (handleShiftTab(this)) return true;
        } else if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (handleTab(this)) return true;
        } else if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_RIGHT) {
            shiftAmount++;
            this.onValueChange(this.getValue(), true);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_LEFT) {
            shiftAmount--;
            this.onValueChange(this.getValue(), true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean handleTab(EditBox box) {
        String text = box.getValue();
        String suggestion = ((EditBoxAccessor) box).getSuggestion();

        BiFunction<String, Character, Boolean> autoComplete = (fullText, character) -> {
            int index = fullText.indexOf(character);
            if (index != -1) {
                box.setValue(fullText.substring(0, index + 1));
                return true;
            }
            return false;
        };

        if (suggestion != null && !suggestion.isEmpty()) {
            if (!text.contains(":") && suggestion.indexOf(':') > 0) {
                return autoComplete.apply((text + suggestion), ':');
            } else if (!text.contains(".") && suggestion.indexOf('.') > 0) {
                return autoComplete.apply((text + suggestion), '.');
            } else {
                box.setValue(text + suggestion);
                box.setSuggestion(null);
                return true;
            }
        }
        return false;
    }

    private static boolean handleShiftTab(EditBox box) {
        String text = box.getValue();

        if (text.isEmpty()) return false;
        // If there isn't a colon, or it is the last and only one, empty the box.
        if (!text.contains(":") || (StringUtils.countMatches(text, ':') == 1 && text.endsWith(":"))) {
            box.setValue("");
            return true;
        } else if (text.contains(":")) {
            box.setValue(text.substring(0, text.indexOf(':') + 1));
            return true;
        }
        return false;
    }
}
