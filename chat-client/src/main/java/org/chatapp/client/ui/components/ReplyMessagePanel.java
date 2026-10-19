package org.chatapp.client.ui.components;

import org.chatapp.client.interfaces.ThemedComponent;
import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.ui.helpers.ImageUtils;
import org.chatapp.client.ui.records.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class ReplyMessagePanel extends JPanel implements ThemedComponent {

    private final UUID messageId;
    public static final int PANEL_HEIGHT = 50;
    public static final Dimension MAX_IMAGE_DIMENSION = new Dimension(100, 100);
    public static final Dimension MIN_IMAGE_DIMENSION = new Dimension(50, 50);


    private JPanel topPanel;
    private JPanel bottomPanel;

    private JLabel senderLabel;
    private JLabel sentTimeLabel;
    private JLabel contentLabel;
    private JButton removeButton;
    private final String originalContent;
    private final Path imagePath;

    public ReplyMessagePanel(UUID messageId, String sender, String sentTimeFormatted, String content, Path imagePath) {
        super();
        this.messageId = messageId;
        this.originalContent = content;
        this.imagePath = imagePath;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Theme colorTheme = ThemeManager.getTheme();
        setBackground(colorTheme.navbarColor());
        // Setting opaque to false so that the sharp borders don't get painted by default before round borders
        setOpaque(false);

        // Building the top and bottom panels which form the reply message panel and adding them to the panel
        setTopPanel(sender, sentTimeFormatted, colorTheme);
        ImageIcon scaledDownImage = null;
        if(imagePath!=null) {
            // Loading a scaled down version of the image from file system
            try {
                BufferedImage bufferedImage = ImageUtils.readThumbnailFromLocalPath(imagePath,
                        (int)MAX_IMAGE_DIMENSION.getHeight());
                // Load and scale not found icon if image could not be found
                if(bufferedImage == null) {
                    bufferedImage = ImageIO.read(Objects.requireNonNull(getClass().getResource("/icons/image-not-found-icon.png")));
                    scaledDownImage = ImageUtils.getScaledDownImage(bufferedImage, MAX_IMAGE_DIMENSION, MIN_IMAGE_DIMENSION);
                } else {
                    scaledDownImage = new ImageIcon(bufferedImage);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(scaledDownImage!=null) {
            setBottomPanel(scaledDownImage);
        } else {
            setBottomPanel(content, colorTheme);
        }
        add(topPanel);
        add(bottomPanel);

        // Calculates panel width based on the content length and sets the width and given height for the panel
        calculatePanelWidth();
    }


    private void setPanelSize(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
    }

    private void setBottomPanel(String content, Theme colorTheme) {
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        setContentLabel(content, colorTheme);
        bottomPanel.add(contentLabel);
        bottomPanel.setOpaque(false);
    }

    private void setBottomPanel(ImageIcon scaledDownImage) {
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        setContentLabelImage(scaledDownImage);
        bottomPanel.add(contentLabel, BorderLayout.WEST);
        bottomPanel.setOpaque(false);
    }

    private void setTopPanel(String sender, String sentTimeFormatted, Theme colorTheme) {
        topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        setSenderLabel(sender, colorTheme);
        setSentTimeLabel(sentTimeFormatted, colorTheme);
        setRemoveButton(colorTheme);
        topPanel.add(Box.createHorizontalStrut(15));
        topPanel.add(senderLabel);
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(sentTimeLabel);
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(removeButton);
        topPanel.setOpaque(false);
    }

    private void setContentLabel(String content, Theme colorTheme) {
        contentLabel = new JLabel(shortenText(content));
        contentLabel.setBorder(new EmptyBorder(0, 15, 5, 0));
        contentLabel.setForeground(colorTheme.textColor());
    }

    private void setContentLabelImage(ImageIcon image) {
        contentLabel = new JLabel(image);
        contentLabel.setBorder(new EmptyBorder(0, 15, 5, 0));
    }

    // Read image from local file path, if a message reply is clicked on an image message and processed it means the image
    //  does exist in local file path
    public BufferedImage readImageFromLocalPath(Path path) {
        File imageFile = path.toFile();
        try {
            return ImageIO.read(imageFile);
        } catch (IOException e) {
            System.out.println("Error loading image from file path: " + path);
            return null;
        }
    }

    private void setRemoveButton(Theme colorTheme) {
        removeButton = new JButton("x");
        removeButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        removeButton.setForeground(colorTheme.textColor());
        removeButton.setContentAreaFilled(false);
    }

    private void setSentTimeLabel(String sentTimeFormatted, Theme colorTheme) {
        sentTimeLabel = new JLabel(sentTimeFormatted);
        sentTimeLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sentTimeLabel.setForeground(colorTheme.textColor());
    }

    private void setSenderLabel(String sender, Theme colorTheme) {
        senderLabel = new JLabel(sender);
        senderLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        senderLabel.setForeground(colorTheme.activeBubbleColor());
    }

    // Returns the message displayed in the reply panel
    public UUID getMessageId() {
        return messageId;
    }

    public JButton getRemoveButton(){
        return removeButton;
    }

    // Calculate the appropriate bubble dimensions based on the message content
    private void calculatePanelWidth() {
        // Calculating bubble sized based on content size
        int userSentTimeWidth = senderLabel.getText() == null ? 0 :
                senderLabel.getFontMetrics(senderLabel.getFont()).stringWidth(senderLabel.getText());
        userSentTimeWidth+= sentTimeLabel.getText() == null ? 0 :
                sentTimeLabel.getFontMetrics(sentTimeLabel.getFont()).stringWidth(sentTimeLabel.getText());
        // Adding additional width for the remove button
        userSentTimeWidth+= 10;
        int width;
        int height = 0;
        if(contentLabel.getIcon() != null) {
            width = Math.max(contentLabel.getIcon().getIconWidth(), userSentTimeWidth);
            width+= 40;
            height = contentLabel.getIcon().getIconHeight();
            height+= 40;
        } else {
            int textWidth =  Math.max(
                    contentLabel.getFontMetrics(contentLabel.getFont()).stringWidth(contentLabel.getText()),
                    userSentTimeWidth
            );
            width = textWidth + 40;
        }

        // Setting panel size to the calculated width and fixed height
        setPanelSize(width, Math.max(PANEL_HEIGHT, height));
    }

    // Helper method to shorten the text shown in the reply label if the message is longer than expected
    private String shortenText(String text) {
        if (text.length() > 41) {
            return text.substring(0, 41).concat("...");
        }

        return text;
    }

    // Returns the sender of the message in the reply panel
    public String getReplySender() {
        return senderLabel.getText();
    }

    // Returns the formatted sent time string
    public String getReplySentTime() {
        return sentTimeLabel.getText();
    }

    // Returns the original message content (not shortened)
    public String getMessageContent() {
        return originalContent;
    }

    // Returns the path to the image in the message (null if it's not a message with an attachment)
    public Path getImagePath() {
        return imagePath;
    }

    // Sets message content to be "Message deleted" and removes image if there is one
    public void updateContentOnDelete() {
        contentLabel.setIcon(null);
        contentLabel.setText("Message deleted");
        calculatePanelWidth();
    }

    // Creates a new instance of reply message panel with same elements and dimensions
    public static ReplyMessagePanel copyReplyPanel(ReplyMessagePanel replyMessagePanel){
        return new ReplyMessagePanel(replyMessagePanel.getMessageId(),
                replyMessagePanel.getReplySender(),
                replyMessagePanel.getReplySentTime(),
                replyMessagePanel.getMessageContent(),
                replyMessagePanel.getImagePath()
                );
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
        g2.dispose();
    }

    @Override
    public void applyTheme() {
        Theme colorTheme = ThemeManager.getTheme();
        setBackground(colorTheme.navbarColor());
        contentLabel.setForeground(colorTheme.textColor());
        removeButton.setForeground(colorTheme.textColor());
        sentTimeLabel.setForeground(colorTheme.textColor());
        senderLabel.setForeground(colorTheme.activeBubbleColor());
    }
}
