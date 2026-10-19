package org.chatapp.client.ui.components;

import org.chatapp.client.ui.MainFrame;
import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.interfaces.ThemedComponent;
import org.chatapp.client.ui.records.MessageStatusDisplay;
import org.chatapp.client.ui.records.Theme;
import org.chatapp.common.model.StatusType;

import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Objects;

public class SeenByModal extends JDialog implements ThemedComponent {

    private final MainFrame mainFrame;
    private final List<MessageStatusDisplay> statuses;
    // Colors
    private Color backgroundColor;
    private Color panelColor;
    private Color accentColor;
    private Color textColor;

    // Child components
    private JPanel contentPanel;
    private JPanel recipientsPanel;
    private JPanel statusPanel;
    private JScrollPane recipientsScrollPane;
    private JPanel headingPanel;
    private JLabel titleLabel;

    public SeenByModal(MainFrame mainFrame, List<MessageStatusDisplay> statuses){
        super(mainFrame, "Message viewers", true);
        // Initialize dialog
        this.mainFrame = mainFrame;
        this.statuses = statuses;
        Theme theme = ThemeManager.getTheme();
        backgroundColor = theme.modalBackground();
        panelColor = theme.bubbleColor();
        accentColor = theme.sidebarActiveColor();
        textColor = theme.textColor();
        initializeDialog();
        initializeComponents();
    }

    // Initialize components inside JDialog
    private void initializeComponents() {
        // Main content panel
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(backgroundColor);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Set Modal title
        setModalHeading(contentPanel);

        // Set recipients section
        setRecipientsSection(contentPanel);

        // Add the content panel to the content pane
        add(contentPanel);
    }

    // Creates the recipients section which shows statuses for each recipient
    private void setRecipientsSection(JPanel contentPanel) {
        recipientsPanel = new JPanel();
        recipientsPanel.setBackground(backgroundColor);
        recipientsPanel.setLayout(new BoxLayout(recipientsPanel, BoxLayout.Y_AXIS));
        recipientsPanel.setPreferredSize(new Dimension(350, 298));
        recipientsPanel.setMaximumSize(new Dimension(350, Integer.MAX_VALUE));

        // First add the heading panel
        recipientsPanel.add(createHeadingPanel());

        // Sort the statuses and remove the SENT statuses
        List<MessageStatusDisplay> sortedStatuses = statuses.stream()
                .filter(statusDisplay -> !statusDisplay.status().equals(StatusType.SENT))
                .sorted(
                        Comparator.comparing(MessageStatusDisplay::status).reversed()
                                .thenComparing(statusDisplay -> statusDisplay.user().getUsername())
                ).toList();

        // Fill recipients panel with the status panels for each recipient
        for(MessageStatusDisplay statusDTO : sortedStatuses){
            recipientsPanel.add(createStatusPanel(statusDTO));
        }

        // Create scroll pane to hold all the recipients panels containing recipient data
        recipientsScrollPane =  new JScrollPane(recipientsPanel);
        recipientsScrollPane.setPreferredSize(new Dimension(350, 300));
        recipientsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        recipientsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        recipientsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        recipientsScrollPane.setOpaque(false);
        recipientsScrollPane.setBorder(new LineBorder(panelColor, 1, true));

        // Add recipients scroll pane to the content panel
        contentPanel.add(recipientsScrollPane);
        contentPanel.add(Box.createVerticalStrut(25));
    }

    // Creates the recipient status panel
    private JPanel createStatusPanel(MessageStatusDisplay statusDisplay) {
        statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusPanel.setPreferredSize(new Dimension(350, 20));
        statusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        statusPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
        statusPanel.setBackground(backgroundColor);

        // Create and add the username label
        JLabel usernameLabel = new JLabel(statusDisplay.user().getUsername());
        statusPanel.add(usernameLabel);

        // Add horizontal glue so if status is added it's to the right as much as possible
        statusPanel.add(Box.createHorizontalGlue());

        // Create and add seen label if status is seen
        if(statusDisplay.status().equals(StatusType.SEEN)){
            ImageIcon seenIcon = new ImageIcon(
                    Objects.requireNonNull(getClass().getResource("/icons/seen-eye-icon.png"))
            );
            JLabel iconLabel = new JLabel(seenIcon);
            statusPanel.add(iconLabel);
        }

        return statusPanel;
    }

    // Creates panel to be placed first in the parent panel
    private JPanel createHeadingPanel() {
        headingPanel = new JPanel();
        headingPanel.setLayout(new BoxLayout(headingPanel, BoxLayout.X_AXIS));
        headingPanel.setPreferredSize(new Dimension(350, 20));
        headingPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        headingPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
        headingPanel.setBackground(panelColor);



        // Create and add username label
        JLabel usernameLabel = new JLabel("Username");
        headingPanel.add(usernameLabel);

        // Add horizontal glue so if status is added it's to the right as much as possible
        headingPanel.add(Box.createHorizontalGlue());

        // Create and add seen label
        JLabel seenLabel = new JLabel("Seen");
        headingPanel.add(seenLabel);

        return headingPanel;
    }


    // Section that sets up the modal heading/title
    private void setModalHeading(JPanel contentPanel) {
        // Create the title label
        titleLabel = new JLabel("Who received this message");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(textColor);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add title label to the content panel
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(25));
    }

    // Initialize the dialog
    private void initializeDialog() {
        setSize(450, 450);
        setMinimumSize(new Dimension(450, 450));
        setLocationRelativeTo(mainFrame);

        setLayout(new BorderLayout());

        getContentPane().setBackground(backgroundColor);
    }

    @Override
    public void applyTheme() {
        Theme theme = ThemeManager.getTheme();
        backgroundColor = theme.modalBackground();
        panelColor = theme.bubbleColor();
        accentColor = theme.sidebarActiveColor();
        textColor = theme.textColor();

        // Repaint child components
        getContentPane().setBackground(backgroundColor);
        headingPanel.setBackground(panelColor);
        statusPanel.setBackground(backgroundColor);
        recipientsScrollPane.setBorder(new LineBorder(panelColor, 1, true));
        recipientsPanel.setBackground(backgroundColor);
        contentPanel.setBackground(backgroundColor);
    }
}
