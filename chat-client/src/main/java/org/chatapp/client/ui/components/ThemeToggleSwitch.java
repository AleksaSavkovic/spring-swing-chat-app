package org.chatapp.client.ui.components;

import org.chatapp.client.interfaces.UiComponentListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class ThemeToggleSwitch extends JComponent {
    private boolean isDarkMode = true;

    // Animation variables
    private double animationProgress = 1.0; // 0.0 = Light (Left), 1.0 = Dark (Right)
    private final Timer animationTimer;
    // Controls smoothness (milliseconds)
    private final int animationSpeed = 50;

    // Sizing properties
    private final int width;
    private final int height;
    private final int knobSize;
    private final int padding = 4;
    private final int yPos;

    public ThemeToggleSwitch(int width, int height, int yPos, int knobSize, UiComponentListener mainFrame) {
        // Set component dimensions
        this.width = width;
        this.height = height;
        this.yPos = yPos;
        this.knobSize = knobSize;
        setPreferredSize(new Dimension(width, height));

        // Handle smooth transition sliding animation
        animationTimer = new Timer(15, e -> {
            // Move right
            if (isDarkMode) {
                animationProgress += 0.1;
                if (animationProgress >= 1.0) {
                    animationProgress = 1.0;
                    ((Timer)e.getSource()).stop();
                }
            } else {
                // Move left
                animationProgress -= 0.1;
                if (animationProgress <= 0.0) {
                    animationProgress = 0.0;
                    ((Timer)e.getSource()).stop();
                }
            }
            repaint();
        });

        // Toggle state upon click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {

                // Change button text
                if (isDarkMode) {
                    mainFrame.onThemeChange("light");
                } else {
                    mainFrame.onThemeChange("dark");
                }
                isDarkMode = !isDarkMode;
                animationTimer.start();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Enable anti-aliasing for smooth, non-pixelated rounded edges
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the pill-shaped background track
        Color trackColor = isDarkMode ? new Color(9, 38, 53) : new Color(210, 225, 240);
        g2.setColor(trackColor);
        g2.fillRoundRect(0, yPos, width, height, height, height);

        // Calculate dynamic knob position (gliding math)
        int minX = padding;
        int maxX = width - knobSize - padding;
        int currentKnobX = (int) (minX + (animationProgress * (maxX - minX)));
        int knobY = yPos + (height-knobSize)/2;

        // Draw the Knob background circle
        Color knobColor = isDarkMode ? new Color(35, 76, 106) : Color.WHITE;
        g2.setColor(knobColor);
        g2.fillOval(currentKnobX, knobY, knobSize, knobSize);

        // Getting theme Icons
        g2.setStroke(new BasicStroke(2));
        ImageIcon sunIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/icons/light-theme-icon.png"))
        );
        ImageIcon moonIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/icons/dark-theme-icon.png"))
        );

        // Setting icon based on animation progress
        if (animationProgress > 0.5) {
            g2.drawImage(moonIcon.getImage(), currentKnobX + padding, knobY + padding, 24, 24, null);
        } else {
            g2.drawImage(sunIcon.getImage(), currentKnobX + padding, knobY + padding, 24, 24, null);
        }
    }
}
