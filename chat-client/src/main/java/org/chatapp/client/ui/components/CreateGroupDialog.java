package org.chatapp.client.ui.components;
import org.chatapp.client.ui.MainFrame;
import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.interfaces.ThemedComponent;
import org.chatapp.client.ui.records.Theme;
import org.chatapp.common.model.UserDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CreateGroupDialog extends JDialog implements ThemedComponent {

    private final MainFrame mainFrame;

    // Top section
    private MyTextField groupNameField;
    private JPanel chatNameSection;
    // Main content
    private JPanel contentPanel;
    private JList<UserDTO> suggestionList;
    private final Set<UserDTO> selectedUsersSet;
    private JPanel selectedPanel;
    private MyTextField searchTxt;
    private JScrollPane scroll;
    private JPanel selectedMembersSection;
    private JButton searchButton;
    private JPanel buttonsPanel;

    // Bottom buttons
    private JButton cancelButton;
    private JButton createButton;

    // Colors
    private Color backgroundColor;
    private Color panelColor;
    private Color accentColor;

    public CreateGroupDialog(MainFrame mainFrame) {
        super(mainFrame, "Create Group Chat", true);

        this.mainFrame = mainFrame;
        selectedUsersSet = new HashSet<>();

        // Get colors from theme manger
        Theme theme = ThemeManager.getTheme();
        backgroundColor = theme.modalBackground();
        panelColor = theme.sidebarColor();
        accentColor = theme.sidebarActiveColor();


        initializeDialog();
        initializeComponents();
        updateSearchResults(mainFrame.getMutualUsers().stream().toList(),true);

        // Set the modal open flag in mainframe to false if modal is closed either by clicking cancel button or exit sign
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                mainFrame.setModalOpen(false);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

    }

    private void initializeDialog() {
        setSize(500, 650);
        setMinimumSize(new Dimension(450, 550));
        setLocationRelativeTo(mainFrame);

        setLayout(new BorderLayout());

        getContentPane().setBackground(backgroundColor);
    }

    private void initializeComponents() {
        // Main content panel
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(backgroundColor);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Modal title
        setModalHeading(contentPanel);
        // Group name section
        setGroupNameSection(contentPanel);
        // Add members section
        setAddMembersPanel(contentPanel);
        // Selected users section
        setSelectedMembersPanel(contentPanel);

        // Bottom buttons panel
        buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.setBackground(backgroundColor);
        buttonsPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        cancelButton = new JButton("Cancel");
        createButton = new JButton("Create");

        styleButton(cancelButton, panelColor);
        styleButton(createButton, accentColor);

        cancelButton.addActionListener(e -> dispose());

        createButton.addActionListener(e -> {
            if(groupNameField.getCleanText().isBlank() || groupNameField.getCleanText().length() < 2) {
                JOptionPane.showMessageDialog(this,
                        "Group chat name is required and must be at least 2 characters long",
                        "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if(selectedUsersSet.size() < 3) {
                JOptionPane.showMessageDialog(this, "You cannot create a group chat with less than 3 members!",
                        "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // create group request
            mainFrame.openNewGroupChat(groupNameField.getCleanText(), selectedUsersSet.stream().toList());
            dispose();
        });

        buttonsPanel.add(cancelButton);
        buttonsPanel.add(createButton);

        // Add the content and buttons panels
        add(contentPanel, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    // Sets up the add members label, search bar and panel
    private void setAddMembersPanel(JPanel contentPanel) {
        // Create add members section panel to hold all the section's elements
        JPanel addMembersSection = new JPanel();
        addMembersSection.setLayout(new BoxLayout(addMembersSection, BoxLayout.Y_AXIS));
        addMembersSection.setOpaque(false);
        // Create add members label
        JLabel addMembersLabel = new JLabel("Add Members");
//        addMembersLabel.setForeground(textColor);
        addMembersLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        addMembersLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create search members panel
        JPanel searchMembersPanel = new JPanel();
        searchMembersPanel.setLayout(new BorderLayout(8, 0));
        searchMembersPanel.setOpaque(false);
        // Create search text field
        searchTxt = new MyTextField("Search for users", panelColor,
                new Color(255, 255, 255), new Color(179, 178, 178), 16);
        // Create search button
        ImageIcon searchIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/icons/search-icon.png"))
        );

        searchButton = new RoundButton(searchIcon);
        searchButton.setBackground(backgroundColor);
        searchButton.setPreferredSize(new Dimension(40,40));
        searchButton.setMaximumSize(new Dimension(40,40));
        searchButton.addActionListener(actionEvent -> {
            // If search query is empty string just load the mutual chatters
            if(searchTxt.getCleanText().isBlank()){
                updateSearchResults(mainFrame.getMutualUsers().stream().toList(), true);
                return;
            }
            mainFrame.sendSearchQuery(searchTxt.getCleanText());
        });
        // Add button and text field to their panel
        searchMembersPanel.add(searchTxt, BorderLayout.CENTER);
        searchMembersPanel.add(searchButton, BorderLayout.EAST);

        // Create jList with custom renderer
        suggestionList = new JList<>();
        suggestionList.setCellRenderer(new UserListCellRenderer());
        suggestionList.setFixedCellHeight(44);
        suggestionList.setBackground(panelColor);
        addJListMouseListeners();

        // Create JScrollPane to hold the jList
        scroll = new JScrollPane(suggestionList);
        scroll.setPreferredSize(new Dimension(400, 200));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        scroll.setOpaque(false);
        scroll.setBorder(
                BorderFactory.createLineBorder(accentColor, 1)
        );


        // Add elements to the members section
        addMembersSection.add(addMembersLabel);
        addMembersSection.add(Box.createVerticalStrut(8));
        searchMembersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        addMembersSection.add(searchMembersPanel);
        addMembersSection.add(Box.createVerticalStrut(10));
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        addMembersSection.add(scroll);

        // Add members section to the content panel
        contentPanel.add(addMembersSection);
        contentPanel.add(Box.createVerticalStrut(10));
    }

    // Set up the selected users panel
    private void setSelectedMembersPanel(JPanel contentPanel) {
        JLabel selectedLabel = new JLabel("Selected");
        selectedLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        selectedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(selectedLabel);
        contentPanel.add(Box.createVerticalStrut(8));

        // Create the parent panel to hold the distinct jPanels
        selectedPanel = new JPanel();
        selectedPanel.setBackground(backgroundColor);
        selectedPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        selectedPanel.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));

        // This sets the preferred width to 400, and the height to the number of rows * 50
        selectedPanel.setPreferredSize(new Dimension(400, ((selectedPanel.getComponentCount()+2)/3)*50));

        // Create the scroll pane to hold the selected panel
        JScrollPane selectedScrollPane = new JScrollPane(selectedPanel);
        selectedScrollPane.setPreferredSize(new Dimension(400, 200));
        selectedScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        selectedScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        selectedScrollPane.getVerticalScrollBar()
                .setUnitIncrement(16);
        selectedScrollPane.setOpaque(false);
        selectedScrollPane.setBorder(null);

        // Create the parent panel which holds the selected members section
        selectedMembersSection = new JPanel(new BorderLayout(0, 8));
        selectedMembersSection.setBackground(backgroundColor);
        selectedMembersSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectedMembersSection.add(selectedLabel, BorderLayout.NORTH);
        selectedMembersSection.add(selectedScrollPane, BorderLayout.CENTER);

        contentPanel.add(selectedMembersSection);
        contentPanel.add(Box.createVerticalStrut(10));
    }

    // Creates the selected user button
    private JButton createSelectedUserButton(UserDTO userDTO){
        JButton selectedUserButton = new JButton(userDTO.getUsername());
        Dimension buttonSize = new Dimension(140, 40);
        selectedUserButton.setPreferredSize(buttonSize);
        selectedUserButton.setMaximumSize(buttonSize);
        selectedUserButton.setMinimumSize(buttonSize);
        selectedUserButton.setBackground(accentColor);
        selectedUserButton.setForeground(Color.WHITE);
        // Add hover effects to the button
        selectedUserButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                selectedUserButton.setBackground(Color.RED);
                selectedUserButton.setText("REMOVE");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                selectedUserButton.setBackground(accentColor);
                selectedUserButton.setText(userDTO.getUsername());
            }
        });
        // Add on click functionality to the button
        selectedUserButton.addActionListener(actionEvent -> {
            DefaultListModel<UserDTO> model = (DefaultListModel<UserDTO>) suggestionList.getModel();
            model.addElement(userDTO);
            // Remove user both from the selected users dto list and panel
            selectedUsersSet.remove(userDTO);
            selectedPanel.remove(selectedUserButton);
            calculateSelectedPanelSize();
        });
        return selectedUserButton;
    }

    // Method that sets up the set group name section (label and text field)
    private void setGroupNameSection(JPanel contentPanel) {
        JLabel groupNameLabel = new JLabel("Group Name");
//        groupNameLabel.setForeground(textColor);
        groupNameLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        groupNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        groupNameField = new MyTextField("Enter the group chat name", panelColor,
                new Color(255, 255, 255), new Color(179, 178, 178), 16);
        groupNameField.setPreferredSize(new Dimension(400, 40));
        groupNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        groupNameField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accentColor, 1),
                        new EmptyBorder(8, 10, 8, 10)
                )
        );

        chatNameSection = new JPanel(new BorderLayout(0, 8));
        chatNameSection.setBackground(backgroundColor);
        chatNameSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        chatNameSection.add(groupNameLabel, BorderLayout.NORTH);
        chatNameSection.add(groupNameField, BorderLayout.CENTER);

        contentPanel.add(chatNameSection);
        contentPanel.add(Box.createVerticalStrut(10));
    }

    // Section that sets up the modal heading/title
    private void setModalHeading(JPanel contentPanel) {
        JLabel titleLabel = new JLabel("Create Group Chat");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
//        titleLabel.setForeground(textColor);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(25));
    }

    // This method update the suggestion list for the search
    public void updateSearchResults(List<UserDTO> searchResults, boolean mutuals) {
        // Add all the elements to the suggestion list
        DefaultListModel<UserDTO> model = new DefaultListModel<>();
        // If displaying mutuals (local db results and not results of the search) show "Mutual" panel at the top,
        // otherwise show search result panel
        if(mutuals){
            model.addElement(UserDTO.builder()
                            .username("Users you have mutual chats with:")
                    .build());
        }else {
            model.addElement(UserDTO.builder()
                    .username("Search results:")
                    .build());
        }

        // Check if there are any search results before trying to filter them and add them to the jlist
        if(searchResults!=null && !searchResults.isEmpty()){
            // Remove the users already in the selected list from the search result
            searchResults = searchResults.stream().filter(userDTO -> !selectedUsersSet.contains(userDTO)).toList();
            // Add all the search results to the JList
            searchResults.forEach(model::addElement);
        }
        suggestionList.setModel(model);

        // Match search bar width
        int width = searchTxt.getWidth();
        // Height is set to hold up to 5 results and if it exceeds that it is fixed and it should become scrollable
        scroll.setPreferredSize(new Dimension(width, Math.min(((searchResults == null ? 1 : searchResults.size()+1) * 44 + 10), (230))));

        suggestionList.setSelectedIndex(0);
        suggestionList.ensureIndexIsVisible(0);
    }

    private void styleButton(JButton button, Color background) {
        button.setFocusPainted(false);
//        button.setForeground(textColor);
        button.setBackground(background);

        button.setPreferredSize(new Dimension(110, 40));

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accentColor, 1),
                        new EmptyBorder(5, 15, 5, 15)
                )
        );
    }

    // Adds mouse listener to the search bar results jList
    public void addJListMouseListeners() {
        suggestionList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 || evt.getClickCount() == 1) {
                    UserDTO selectedUser = suggestionList.getSelectedValue();
                    if (selectedUser != null) {
                        // If the user id is null it means that it isn't an actual user but just the placeholder value
                        //  at the top of the list, in that case we don't do anything on click
                        if(selectedUser.getId() == null) {
                            return;
                        }

                        // Remove the user from the add members list
                        DefaultListModel<UserDTO> model = (DefaultListModel<UserDTO>) suggestionList.getModel();
                        int selectedIndex = suggestionList.getSelectedIndex();
                        if(selectedIndex != -1) {
                            model.remove(selectedIndex);
                        }
                        // Add the selected user to the selected members panel and dto list
                        selectedPanel.add(createSelectedUserButton(selectedUser));
                        selectedUsersSet.add(selectedUser);
                        // Recalculate the selected panel height and repaint it
                        calculateSelectedPanelSize();
                    }
                }
                searchTxt.requestFocusInWindow();
            }
        });
    }

    // Calculates selected panel size based on the number of child components inside it
    private void calculateSelectedPanelSize() {
        selectedPanel.setPreferredSize(new Dimension(400, ((selectedPanel.getComponentCount()+2)/3)*50));
        selectedPanel.revalidate();
        selectedPanel.repaint();
    }

    @Override
    public void applyTheme() {
        Theme theme = ThemeManager.getTheme();
        backgroundColor = theme.modalBackground();
        panelColor = theme.sidebarColor();
        accentColor = theme.sidebarActiveColor();

        // Change background color of all items where needed
        getContentPane().setBackground(backgroundColor);
        chatNameSection.setBackground(backgroundColor);
        selectedMembersSection.setBackground(backgroundColor);
        selectedPanel.setBackground(backgroundColor);
        searchButton.setBackground(backgroundColor);
        buttonsPanel.setBackground(backgroundColor);
        contentPanel.setBackground(backgroundColor);

    }

    // Custom renderer for prettier menu
    private static class UserListCellRenderer extends DefaultListCellRenderer {
        private final Theme theme = ThemeManager.getTheme();
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof UserDTO user) {
                setText(user.getUsername());  // Only show username
            }

            // Bigger font
            setFont(getFont().deriveFont(16f));

            // Padding and alignment
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            // Background colors
            if (isSelected && index != 0) {
                setBackground(theme.sidebarActiveColor());
                setForeground(theme.textColor());
            } else {
                setBackground(ThemeManager.getTheme().sidebarColor());
                setForeground(theme.textColor());
            }

            // Disable the first button which is not an actual list value, making the text gray
            if (index == 0) {
                comp.setEnabled(false);
            }

            return this;
        }
    }
}
