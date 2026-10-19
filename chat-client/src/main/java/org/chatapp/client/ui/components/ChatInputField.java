package org.chatapp.client.ui.components;

import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.interfaces.ThemedComponent;
import org.chatapp.client.ui.records.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public class ChatInputField extends JPanel implements ThemedComponent {

    private final JFileChooser attachmentPicker;
    private final RoundButton addAttachmentButton;
    private final JButton removeAttachmentButton;
    private final JPanel contentPanel;
    private final JPanel imageLabelParent;
    private final JLabel imageLabel;
    private ReplyMessagePanel replyPanel;
    private final MyTextField messageTxt;
    private final RoundButton sendButton;
    private Path messageAttachment;
    private final Component horizontalGlue = Box.createHorizontalGlue();

    public ChatInputField(Runnable handleSend){
        super();
        // Load color theme to set starting colors
        Theme theme = ThemeManager.getTheme();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        // Setting up the panel appearance
        setPanelAppearance(theme);

        // Initialize the file chooser to be used for picking the message attachment
        attachmentPicker = new JFileChooser();
        setupAttachmentPicker();

        // Creating the remove attachment button and image label, not visible at first
        removeAttachmentButton = new JButton();
        setupRemoveAttachmentButton(theme);
        horizontalGlue.setVisible(false);
        imageLabel = new JLabel();
        setupImageLabel();
        addMessageImageEventListeners(handleSend);

        // Creating the text field
        messageTxt = new MyTextField("Enter message", theme.sidebarColor(), theme.textColor(),
                theme.searchPromptColor(), 16);
        addMessageTxtEventListeners(handleSend);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(theme.sidebarColor());
        imageLabelParent = new JPanel();
        imageLabelParent.setLayout(new BoxLayout(imageLabelParent, BoxLayout.X_AXIS));
        imageLabelParent.setBorder(new EmptyBorder(0, 0, 0, 0));
        imageLabel.setAlignmentY(Component.TOP_ALIGNMENT);
        imageLabelParent.setMaximumSize(new Dimension(1920, 1080));
        imageLabelParent.add(Box.createHorizontalStrut(10));
        imageLabelParent.add(imageLabel);
        imageLabelParent.add(Box.createHorizontalGlue());
        imageLabelParent.setOpaque(false);

        contentPanel.add(imageLabelParent);
        contentPanel.add(messageTxt);

        // Creating a circle shaped button and setting its icon,dimensions and color
        sendButton = new RoundButton(null);
        setupSendButton(handleSend, theme);

        // Creating a circle shaped button and setting its icon, dimensions and color
        addAttachmentButton = new RoundButton(null);
        setupAddAttachmentButton(theme);

        // Add all child elements to the chatInputField
        addElements();
    }

    private void setupImageLabel() {
        imageLabel.setVisible(false);
        imageLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
    }

    private void setupRemoveAttachmentButton(Theme theme) {
        ImageIcon closeIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/icons/close-icon.png"))
        );
        removeAttachmentButton.setBorder(new EmptyBorder(5,5,5,5));
        removeAttachmentButton.setRolloverEnabled(false);
        removeAttachmentButton.setFocusPainted(false);
        removeAttachmentButton.setContentAreaFilled(false);
        removeAttachmentButton.setIcon(closeIcon);
        removeAttachmentButton.setVisible(false);
        removeAttachmentButton.setMaximumSize(new Dimension(50, 50));
        removeAttachmentButton.setPreferredSize(new Dimension(50,50));
        removeAttachmentButton.setBackground(theme.sidebarColor());
        // When removing the attachment reset the input field to normal form, making elements visible and not visible
        removeAttachmentButton.addActionListener(e -> handleRemoveAttachment());
    }

    // Removing the attachment from the input field and all related changes
    private void handleRemoveAttachment() {
        imageLabel.setIcon(null);
        messageAttachment = null;
        addAttachmentButton.setVisible(true);
        removeAttachmentButton.setVisible(false);
        messageTxt.setVisible(true);
        if(contentPanel.getComponentCount() > 2) {
            handleRemoveAttachmentForReply();
        } else {
            setPreferredSize(new Dimension(0, 50));
            setMaximumSize(new Dimension(1920, 50));
        }
        horizontalGlue.setVisible(false);
        imageLabel.setVisible(false);
        sendButton.setAlignmentY(Component.CENTER_ALIGNMENT);
        removeAttachmentButton.setAlignmentY(Component.CENTER_ALIGNMENT);
    }

    // Adds back the horizontal strut before the reply and glue after it so it stays at the start of its parent panel
    // Also removes the horizontal strut added at the end of the parent when an attachment was picked
    private void handleRemoveAttachmentForReply() {
        JPanel replyParent = (JPanel)contentPanel.getComponent(0);
        // If reply panel is at index 1 remove component at index 0 otherwise remove component at index 1
        if(replyParent.getComponent(1) instanceof ReplyMessagePanel) {
            replyParent.remove(0);
        } else {
            replyParent.remove(1);
        }
        replyParent.add(Box.createHorizontalStrut(10), 0);
        replyParent.add(Box.createHorizontalGlue());
        replyParent.revalidate();
        replyParent.repaint();
        setPreferredSize(new Dimension(0, (int)replyParent.getPreferredSize().getHeight() + 40));
        setMaximumSize(new Dimension(1920, 110));
    }

    // Handles the attachment picker option menu response, processes the file if approved otherwise closes
    private void handleFileChoice(int response) {
        if(response == JFileChooser.APPROVE_OPTION) {
            File selectedFile = attachmentPicker.getSelectedFile();
            messageAttachment = selectedFile.toPath();
            ImageIcon pickedImage = getScaledDownImage();
            // Add the image to the input field
            handlePickedAttachment(pickedImage);
        }
    }

    // Adding the attachment to the input field and all related changes
    private void handlePickedAttachment(ImageIcon pickedImage) {
        imageLabel.setIcon(pickedImage);
        imageLabel.setMaximumSize(new Dimension(100, 100));
        removeAttachmentButton.setVisible(true);
        addAttachmentButton.setVisible(false);
        imageLabel.setVisible(true);
        if(contentPanel.getComponentCount() > 2) {
            handlePickedAttachmentForReply();
        } else {
            setPreferredSize(new Dimension(0, 110));
            setMaximumSize(new Dimension(1920, 110));
        }
        horizontalGlue.setVisible(true);
        contentPanel.revalidate();
        contentPanel.repaint();
        messageTxt.setVisible(false);
        sendButton.setAlignmentY(Component.TOP_ALIGNMENT);
        removeAttachmentButton.setAlignmentY(Component.TOP_ALIGNMENT);
    }

    // Removes the horizontal glue after and horizontal strut before the reply bubble. Adds horizontal strut after reply
    //  bubble so if a bubble is shorter than image panel it still stays at the start of the reply parent container
    private void handlePickedAttachmentForReply() {
        JPanel replyParent = (JPanel)contentPanel.getComponent(0);
        replyParent.remove(2);
        replyParent.remove(0);
        replyParent.add(Box.createHorizontalStrut(100));
        replyParent.revalidate();
        replyParent.repaint();
        setPreferredSize(new Dimension(0, 200));
        setMaximumSize(new Dimension(1920, 250));
    }

    // Handles the attachment picker option menu response, processes the file if approved otherwise closes
    public void handleReplyPicked(MessageBubble bubble) {
        // Return if the message has not been sent yet
        if(bubble.getMessageId()==null) {
            JOptionPane.showMessageDialog(this, "You can't reply to messages that haven't been sent yet!",
                    "Replying to pending message", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // If there is more than 2 components inside content panel it means there is already a reply panel showing,
        // in that case we remove the old reply panel before adding the new one
        if(contentPanel.getComponentCount() > 2) {
            contentPanel.remove(0);
        }

        // Format the message sent time before passing it to the reply panel
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
        String formatedTime = timeFormat.format(bubble.getSentTime());

        // Build the reply panel and its container and add the to the content panel
        replyPanel = new ReplyMessagePanel(bubble.getMessageId(), bubble.getSender(), formatedTime, bubble.getBubbleContent(), bubble.getMessageAttachmentPath());
        JPanel replyParentPanel = createReplyBubbleContainer(replyPanel);

        // Adding action listener to the remove button
        replyPanel.getRemoveButton().addActionListener(e -> {
            contentPanel.remove(0);
            contentPanel.revalidate();
            contentPanel.repaint();
            replyPanel = null;
            System.out.println("Image label icon is null: " + (imageLabel.getIcon()==null ? "YES" : "NO"));
            if(imageLabel.getIcon() == null) {
                setPreferredSize(new Dimension(0, 50));
                setMaximumSize(new Dimension(1920, 50));
            } else {
                setPreferredSize(new Dimension(0, 110));
                setMaximumSize(new Dimension(1920, 110));
            }
        });

        // If horizontal glue is not visible make it visible
        if(!horizontalGlue.isVisible()) {
            horizontalGlue.setVisible(true);
        }
        setPreferredSize(new Dimension(0, (int)replyParentPanel.getPreferredSize().getHeight() + 40));
        setMaximumSize(new Dimension(1920, 110));

        contentPanel.add(replyParentPanel, 0);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // Wraps the reply bubble in a container in order to be place into the input fields content panel
    private JPanel createReplyBubbleContainer(JPanel replyPanel) {
        JPanel replyParentPanel = new JPanel();
        replyParentPanel.setOpaque(false);
        replyParentPanel.setLayout(new BoxLayout(replyParentPanel, BoxLayout.X_AXIS));
        replyParentPanel.add(Box.createHorizontalStrut(10));
        replyParentPanel.add(replyPanel);
        replyParentPanel.add(Box.createHorizontalGlue());
        replyParentPanel.setOpaque(false);
        return replyParentPanel;
    }

    // This method obtains the image from the messageAttachment file path and scales it down to be displayed in the input field
    private ImageIcon getScaledDownImage() {
        // Scale the image icon to be displayed in the message input field
        ImageIcon originalImage = new ImageIcon(
                Objects.requireNonNull(messageAttachment.toAbsolutePath().toString())
        );
        Image rawImage = originalImage.getImage();
        Image scaledImage;
        if(rawImage.getHeight(null) >= rawImage.getWidth(null)) {
            scaledImage = rawImage.getScaledInstance(-1, 90, Image.SCALE_SMOOTH);
        } else {
            scaledImage = rawImage.getScaledInstance(200, -1, Image.SCALE_SMOOTH);
            if(scaledImage.getHeight(null) < 50 || scaledImage.getHeight(null) > 90) {
                scaledImage = rawImage.getScaledInstance(200, 50, Image.SCALE_SMOOTH);
            }
        }
        return new ImageIcon(scaledImage);
    }

    private void setupAttachmentPicker() {
        attachmentPicker.setCurrentDirectory(new File(System.getProperty("user.home")));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image files (*.jpg, *.jpeg, *.png, *.gif, *.bmp)",
                "jpg", "jpeg", "png", "gif", "bmp");
        attachmentPicker.setFileFilter(filter);
        attachmentPicker.setMultiSelectionEnabled(false);
    }

    private void setPanelAppearance(Theme theme) {
        // This line is needed in for the corners to be round
        setOpaque(false);
        setMaximumSize(new Dimension(1920, 50));
        setPreferredSize(new Dimension(0, 50));
        setBorder(new EmptyBorder(4, 10, 4, 10));
        setBackground(theme.sidebarColor());
    }

    // Adds required the event handling to addMessageTxt text field
    private void addMessageTxtEventListeners(Runnable handleSend) {
        // Does the same when a user presses enter as on send button click
        messageTxt.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("ENTER"), "send");
        messageTxt.getActionMap()
                .put("send", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        // Does not fire send the message if there is no text(the placeholder is only text)
                        if(messageTxt.getCleanText().isBlank()) {
                            System.out.println("Is placeholder!!!");
                            return;
                        }
                        handleSend.run();
                        if(replyPanel != null) {
                            replyPanel=null;
                            contentPanel.remove(0);
                            revalidate();
                            repaint();
                        }
                    }
                });
    }

    // Adds required the event handling to imageLabel label
    private void addMessageImageEventListeners(Runnable handleSend) {
        // Does the same when a user presses enter as on the send button click
        imageLabel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ENTER"), "sendImage");

        imageLabel.getActionMap().put("sendImage", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(messageAttachment==null) {
                    System.out.println("No image shown!!!");
                    return;
                }
                handleSend.run();
                messageTxt.revertToPlaceholderText();
                handleRemoveAttachment();
                if(replyPanel != null) {
                    replyPanel=null;
                    contentPanel.remove(0);
                    revalidate();
                    repaint();
                }
            }
        });
    }

    // Adds all 3 child elements to the parent element
    private void addElements() {
        add(addAttachmentButton);
        add(removeAttachmentButton);
        add(contentPanel);
        add(horizontalGlue);
        add(sendButton);
    }

    // Removes everything from the input field
    public void clearInputField() {
        if(messageAttachment!=null) {
            handleRemoveAttachment();
        }
        messageTxt.revertToPlaceholderText();
        if(replyPanel != null) {
            replyPanel=null;
            contentPanel.remove(0);
        }
        setMaximumSize(new Dimension(1920, 50));
        setPreferredSize(new Dimension(0, 50));
    }

    private void setupSendButton(Runnable handleSend, Theme theme) {
        // Loading up the icon for our send button
        ImageIcon sendIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/icons/send-icon.png"))
        );
        sendButton.setIcon(sendIcon);
        sendButton.setMaximumSize(new Dimension(50, 50));
        sendButton.setPreferredSize(new Dimension(50,50));
        sendButton.setBackground(theme.sidebarColor());
        sendButton.setBorder(new EmptyBorder(5,5,5,5));

        sendButton.addActionListener(e -> {
            if(messageTxt.getCleanText().isBlank() && messageAttachment==null) {
                System.out.println("Is placeholder!!!");
                return;
            }
            handleSend.run();
            if(messageAttachment!=null) {
                messageTxt.revertToPlaceholderText();
                handleRemoveAttachment();
            }
            if(replyPanel != null) {
                replyPanel=null;
                contentPanel.remove(0);
                revalidate();
                repaint();
            }
        });
    }

    private void setupAddAttachmentButton(Theme theme) {
        // Adding the text field and button to the panel
        ImageIcon createGCIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/icons/create-gc-icon.png"))
        );
        addAttachmentButton.setIcon(createGCIcon);
        addAttachmentButton.setMaximumSize(new Dimension(40, 40));
        addAttachmentButton.setPreferredSize(new Dimension(40,40));
        addAttachmentButton.setBackground(theme.sidebarColor());
        addAttachmentButton.addActionListener(e ->
                handleFileChoice(attachmentPicker.showOpenDialog(this.getParent())));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        g2.dispose();
    }

    // Updates the colors of child objects on theme change
    @Override
    public void applyTheme(){
        Theme theme = ThemeManager.getTheme();
        setBackground(theme.sidebarColor());
        contentPanel.setBackground(theme.sidebarColor());

        messageTxt.setBackgroundColor(theme.sidebarColor());
        messageTxt.setPlaceholder(messageTxt.getPlaceholder(), theme.textColor(), theme.searchPromptColor());
        messageTxt.setForeground(theme.textColor());

        sendButton.setBackground(theme.sidebarColor());
        addAttachmentButton.setBackground(theme.sidebarColor());
        attachmentPicker.updateUI();
    }

    public MyTextField getMessageTxt() {
        return messageTxt;
    }

    public RoundButton getSendButton() {
        return sendButton;
    }

    public UUID getReplyId() {
        return replyPanel == null ? null : replyPanel.getMessageId();
    }

    // Return the reply panel copy so it can be added to the message bubble, make its remove button invisible as it will
    //  be added to message bubble
    public ReplyMessagePanel getReplyPanelCopy() {
        if(replyPanel == null) {
            return null;
        }
        ReplyMessagePanel replyCopy = ReplyMessagePanel.copyReplyPanel(replyPanel);
        replyCopy.getRemoveButton().setVisible(false);
        return replyCopy;
    }

    public Path getMessageAttachment() {
        return messageAttachment;
    }
}
