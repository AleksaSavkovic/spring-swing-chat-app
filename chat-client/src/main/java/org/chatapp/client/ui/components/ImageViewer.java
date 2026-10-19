package org.chatapp.client.ui.components;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ImageViewer extends JFrame {


    private final BufferedImage originalImage;
    private final JLabel imageLabel;
    private final JScrollPane scrollPane;

    public ImageViewer(Path imagePath) {
        setTitle("Image Viewer");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        try {
            originalImage = ImageIO.read(imagePath.toFile());

            if (originalImage == null) {
                throw new IOException("Could not decode image: " + imagePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load image: " + imagePath, e);
        }

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

        scrollPane = new JScrollPane(imageLabel);
        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );
        add(scrollPane);

        // Setting the size of the window
        Dimension screenSize = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds()
                .getSize();

        screenSize.setSize(new Dimension((int)screenSize.getWidth(), (int)screenSize.getHeight()-60));

        setSize(screenSize);
        setLocationRelativeTo(null);

        // Wait until the viewport has its actual size.
        SwingUtilities.invokeLater(this::fitImageInitially);
    }

    private void fitImageInitially() {
        int availableWidth = scrollPane.getViewport().getWidth();
        int availableHeight = scrollPane.getViewport().getHeight();

        int imageWidth = originalImage.getWidth();
        int imageHeight = originalImage.getHeight();

        double scale = Math.min(
                (double) availableWidth / imageWidth,
                (double) availableHeight / imageHeight
        );

        // Never upscale smaller images.
        scale = Math.min(scale, 1.0);

        int newWidth = (int) Math.round(imageWidth * scale);
        int newHeight = (int) Math.round(imageHeight * scale);

        Image displayedImage;

        if (scale == 1.0) {
            displayedImage = originalImage;
        } else {
            displayedImage = originalImage.getScaledInstance(
                    newWidth,
                    newHeight,
                    Image.SCALE_SMOOTH
            );
        }

        imageLabel.setIcon(new ImageIcon(displayedImage));
    }


}
