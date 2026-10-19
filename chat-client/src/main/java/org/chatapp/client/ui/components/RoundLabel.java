package org.chatapp.client.ui.components;

import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.interfaces.ThemedComponent;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class RoundLabel extends JLabel implements ThemedComponent {

    private Color backgroundColor;

    public RoundLabel(String text) {
        super(text);
        setOpaque(false);
        Border padding = BorderFactory.createEmptyBorder(5, 10, 5, 10);
        setBorder(padding);
        backgroundColor = ThemeManager.getTheme().sidebarColor();
        setBackground(backgroundColor);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(backgroundColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
        super.paintComponent(g2);
        g2.dispose();
    }

    @Override
    public void applyTheme() {
        backgroundColor = ThemeManager.getTheme().sidebarColor();
        setBackground(backgroundColor);
    }
}
