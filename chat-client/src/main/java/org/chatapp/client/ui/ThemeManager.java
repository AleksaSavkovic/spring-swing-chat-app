package org.chatapp.client.ui;

import org.chatapp.client.ui.records.Theme;
import org.yaml.snakeyaml.Yaml;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.List;

public class ThemeManager {
    private static Theme currentTheme;

    // Returns current theme to user
    public static Theme getTheme(){
        return currentTheme;
    }

    // Set theme method loads a new theme based on the input theme name from same name yaml theme file
    public static void setTheme(String themeName){
        Yaml yaml = new Yaml();

        try (InputStream is =
                     ThemeManager.class.getResourceAsStream(
                             "/color-themes/" + themeName + ".yaml")) {
            Map<String, Object> data = yaml.load(is);
            currentTheme = new Theme(
                    color(data, "primary_color"),
                    color(data, "navbar_color"),
                    color(data, "sidebar_color"),
                    color(data, "sidebar_active_color"),
                    color(data, "searchbar_color"),
                    color(data, "search_prompt_color"),
                    color(data, "bubble_color"),
                    color(data, "active_bubble_color"),
                    color(data, "modal_background"),
                    color(data, "text_color"),
                    color(data, "deleted_bubble_color")
            );
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to load theme", e);
        }
    }

    // This method returns a color object based on the values from the rgb value obtained from yaml them file
    private static Color color(Map<String, Object> data, String key) {
        List<Integer> rgb = (List<Integer>) data.get(key);
        return new Color(rgb.get(0), rgb.get(1), rgb.get(2));
    }

}
