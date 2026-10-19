package org.chatapp.client.ui.helpers;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

public class ImageUtils {

    private ImageUtils() {

    }

    // Takes the buffered image as argument scales it down and creates an image icon from it
    public static ImageIcon getScaledDownImage(BufferedImage rawImage, Dimension maxDimension, Dimension minDimension) {
        // Scale the image icon to be displayed in the message bubble
        Image scaledImage = null;
        // Check if image is portrait or landscape in order to scale down the image correctly
        if(rawImage.getHeight() > rawImage.getWidth(null)) {
            if(rawImage.getHeight() > maxDimension.getHeight()) {
                scaledImage = rawImage.getScaledInstance(-1, (int)maxDimension.getHeight(), Image.SCALE_SMOOTH);
            } else if(rawImage.getHeight() < minDimension.getHeight()) {
                scaledImage = rawImage.getScaledInstance(-1, (int)minDimension.getHeight(), Image.SCALE_SMOOTH);
            }
        } else {
            if(rawImage.getWidth() > maxDimension.getWidth()) {
                scaledImage = rawImage.getScaledInstance((int)maxDimension.getWidth(), -1, Image.SCALE_SMOOTH);
            } else if(rawImage.getWidth() < minDimension.getWidth()) {
                scaledImage = rawImage.getScaledInstance((int)minDimension.getWidth(), -1, Image.SCALE_SMOOTH);
            }
        }
        return new ImageIcon(scaledImage == null ? rawImage : scaledImage);
    }

    // Reads a smaller compress image from the local path
    public static BufferedImage readThumbnailFromLocalPath(Path path, int maxDimension) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            // Throw exception if image is not found
            if(input == null) {
                System.out.println("Image could not be found in local file system path: " + path);
                return null;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw new IOException("No ImageReader found for: " + path);
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(input);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int longestSide = Math.max(width, height);

                if (longestSide <= maxDimension) {
                    return reader.read(0);
                }

                int sample = Math.max(1, longestSide / maxDimension);

                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceSubsampling(sample, sample, 0, 0);

                return reader.read(0, param);

            } finally {
                reader.dispose();
            }
        }
    }



}
