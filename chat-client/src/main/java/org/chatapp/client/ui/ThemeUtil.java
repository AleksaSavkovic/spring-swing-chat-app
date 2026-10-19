package org.chatapp.client.ui;

import org.chatapp.client.interfaces.ThemedComponent;

import java.awt.*;

public final class ThemeUtil {
    private ThemeUtil() {}

    // Goes through swing component three and applies new theme
    public static void applyThemeToTree(Container root) {

        for (Component c : root.getComponents()) {

            if (c instanceof ThemedComponent themed) {
                themed.applyTheme();
            }

            if (c instanceof Container container) {
                applyThemeToTree(container);
            }
        }
    }
}
