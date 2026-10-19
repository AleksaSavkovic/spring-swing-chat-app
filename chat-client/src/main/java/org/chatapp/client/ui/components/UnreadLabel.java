package org.chatapp.client.ui.components;

import javax.swing.*;
import java.awt.*;

public class UnreadLabel extends JLabel {
    private final int radius;

    public UnreadLabel(String text, int radius) {
        super(text);
        this.radius = radius;
        setOpaque(false);
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g2.dispose();

        super.paintComponent(g);

    }
}
