package org.chatapp.client.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

public class RoundButton extends JButton {

    public RoundButton(ImageIcon icon){
        super(icon);
        setContentAreaFilled(false);
        setFocusable(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        // Enable antialiasing for smooth, non-pixelated rounded edges
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill the circle
        if (getModel().isArmed()) {
            g2.setColor(new Color(30, 183, 229)); // Color when pressed
        } else {
            g2.setColor(getBackground());
        }
        g2.fillOval(0, 0, getSize().width - 1, getSize().height - 1);

        // Draw the border
        g2.setColor(getBackground());
        g2.drawOval(0, 0, getSize().width - 1, getSize().height - 1);

        // Draw the text
        super.paintComponent(g); // Calls the parent's paintComponent to draw text/icon
        g2.dispose();
    }

    @Override
    public boolean contains(int x, int y) {
        // Check if the point (x, y) is within the circular shape
        int radius = Math.min(getSize().width, getSize().height) / 2;
        return new Ellipse2D.Double(0, 0, 2 * radius, 2 * radius).contains(x, y);
    }
}
