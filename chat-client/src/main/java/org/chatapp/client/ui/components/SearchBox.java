package org.chatapp.client.ui.components;

import org.chatapp.client.ui.MainFrame;
import org.chatapp.client.ui.ThemeManager;
import org.chatapp.client.interfaces.ThemedComponent;
import org.chatapp.client.ui.records.Theme;
import org.chatapp.common.model.UserDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;

public class SearchBox extends JPanel implements ThemedComponent {

    private final JPopupMenu suggestionPopup;
    private final JList<UserDTO> suggestionList;
    private final MyTextField searchTxt;
    private final JScrollPane scroll;
    private final MainFrame mainFrame;
    private JButton createGCButton;
    private CreateGroupDialog modal;

    private final Timer debounceTimer;
    private static final int DEBOUNCE_DELAY_MS = 300;

    private Color navbarColor;
    private Color searchBarColor;
    private Color searchBarPromptColor;
    private Color textColor;

    public SearchBox(MainFrame mainframe) {
        super();
        // Create sidebar search bar container
        this.mainFrame = mainframe;
        Theme theme = ThemeManager.getTheme();
        navbarColor = theme.navbarColor();
        searchBarColor = theme.searchbarColor();
        searchBarPromptColor = theme.searchPromptColor();
        textColor = theme.textColor();
        setBackground(navbarColor);

        // Search panel layout manager set up
        this.setLayout(new BorderLayout(8, 0));
        this.setBorder(new EmptyBorder(5, 10, 0, 10));

        // Create the search bar using custom text field class
        searchTxt = new MyTextField("Start new conversation", searchBarColor, textColor, searchBarPromptColor, 16);

        // Search results list set up
        suggestionPopup = new JPopupMenu();

        // This enables us to keep writing in the search box after suggestions(popup) appear
        suggestionPopup.setLightWeightPopupEnabled(false);
        suggestionPopup.setFocusable(false);

        // Create jList with custom renderer
        suggestionList = new JList<>();
        suggestionList.setCellRenderer(new UserListCellRenderer());
        suggestionList.setFixedCellHeight(44);

        // Create scroll pane
        scroll = new JScrollPane(suggestionList);
        scroll.setPreferredSize(new Dimension(searchTxt.getPreferredSize().width, 200));
        suggestionPopup.add(scroll);

        // Adding the mouse listeners for suggestion list
        addMouseListeners();

        // Create button used for creating group chat
        setCreateGCButton();

        // Add search bar to the search box panel
        add(searchTxt, BorderLayout.CENTER);
        add(createGCButton, BorderLayout.EAST);

        // Debounce timer sends a search query if the text field had text in it a set amount of time after user typing
        debounceTimer = new Timer(DEBOUNCE_DELAY_MS, actionEvent -> {
            if (searchTxt.getText() == null || searchTxt.getText().isBlank()) {
                // Check if the popup was visible from before the search field was empty
                if (suggestionPopup.isVisible()) {
                    suggestionPopup.setVisible(false);
                }
                return;
            }

            // Passing off the search query to the main frame method
            mainframe.sendSearchQuery(searchTxt.getText().trim());
        });

        // Fire only once per start
        debounceTimer.setRepeats(false);

        // Restart on each key event, this means that the query is sent when there is DEBOUNCE_DELAY_MS
        // between the final key typed and the next one
        searchTxt.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                debounceTimer.restart();
            }
        });

        searchTxt.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Only handle when popup is shown
                if (!suggestionPopup.isVisible()) {
                    return;
                }

                int key = e.getKeyCode();
                int selected = suggestionList.getSelectedIndex();
                int size = suggestionList.getModel().getSize();

                if (key == KeyEvent.VK_DOWN) {
                    if (selected < size - 1) {
                        suggestionList.setSelectedIndex(selected + 1);
                        suggestionList.ensureIndexIsVisible(selected + 1);
                    }
                    e.consume(); // Prevent text field from moving caret

                } else if (key == KeyEvent.VK_UP) {
                    if (selected > 0) {
                        suggestionList.setSelectedIndex(selected - 1);
                        suggestionList.ensureIndexIsVisible(selected - 1);
                    } else if (selected == 0) {
                        suggestionList.clearSelection();
                    }
                    e.consume();

                } else if (key == KeyEvent.VK_ENTER) {
                    UserDTO selectedUser = suggestionList.getSelectedValue();
                    if (selectedUser != null) {
                        mainframe.openNewChat(selectedUser);
                        searchTxt.setText("");
                        suggestionPopup.setVisible(false);
                    }
                    e.consume();

                } else if (key == KeyEvent.VK_ESCAPE) {
                    suggestionPopup.setVisible(false);
                    e.consume();
                }
            }
        });

    }

    // Creates the logout button with the logout icon
    private void setCreateGCButton() {
        ImageIcon createGCIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/icons/create-gc-icon.png"))
        );
        createGCButton = new RoundButton(createGCIcon);
        createGCButton.setMaximumSize(new Dimension(40, 40));
        createGCButton.setPreferredSize(new Dimension(40,40));
        createGCButton.setBackground(navbarColor);
//        createGCButton.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, null);
//        createGCButton.setInputMap(JComponent.WHEN_FOCUSED, null);
//        createGCButton.setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);



        createGCButton.addActionListener(e -> {
            mainFrame.setModalOpen(true);
            modal = new CreateGroupDialog(mainFrame);
            modal.setVisible(true);
        });
    }

    // This method update the suggestion list for the search
    public void updateSearchResults(List<UserDTO> searchResults) {
        if (searchResults == null || searchResults.isEmpty()) {
            suggestionPopup.setVisible(false);
            return;
        }

        // Add all the elements to the suggestion list
        DefaultListModel<UserDTO> model = new DefaultListModel<>();
        searchResults.forEach(model::addElement);
        suggestionList.setModel(model);

        // Match search bar width
        int width = searchTxt.getWidth();
        if (width < 200) width = 300; // Minimum reasonable width
        // Height is set to hold up to 5 results and if it exceeds that it is fixed and it should become scrollable
        scroll.setPreferredSize(new Dimension(width, Math.min((searchResults.size() * 44 + 10), (230))));

        // Repack popup to apply new size
        suggestionPopup.pack();

        // Show below search field
        if (!suggestionPopup.isVisible()) {
            suggestionPopup.show(searchTxt, 0, searchTxt.getHeight());
        }

        suggestionList.setSelectedIndex(0);
        suggestionList.ensureIndexIsVisible(0);
    }

    public void addMouseListeners() {
        suggestionList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2 || evt.getClickCount() == 1) {
                    UserDTO selectedUser = suggestionList.getSelectedValue();
                    if (selectedUser != null) {
                        mainFrame.openNewChat(selectedUser);
                        searchTxt.setText("");
                        suggestionPopup.setVisible(false);
                    }
                }
                searchTxt.requestFocusInWindow();
            }
        });
    }

    // Returns the suggestion popup menu
    public JPopupMenu getSuggestionPopup() {
        return suggestionPopup;
    }

    // Returns the modal
    public CreateGroupDialog getModal(){
        return modal;
    }

    @Override
    public void applyTheme() {
        Theme theme = ThemeManager.getTheme();
        navbarColor = theme.navbarColor();
        searchBarColor = theme.searchbarColor();
        searchBarPromptColor = theme.searchPromptColor();
        textColor = theme.textColor();

        // Change component colors to new colors
        searchTxt.setBackgroundColor(searchBarColor);
        searchTxt.setPlaceholder(searchTxt.getPlaceholder(), textColor, searchBarPromptColor);
        searchTxt.setForeground(textColor);
        searchTxt.repaint();
        setBackground(navbarColor);

        // Change search button background color
        createGCButton.setBackground(navbarColor);
        suggestionList.updateUI();
        scroll.updateUI();
        scroll.getVerticalScrollBar().updateUI();
    }


    // Custom renderer for prettier menu
    private static class UserListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof UserDTO user) {
                setText(user.getUsername());  // Only show username
            }

            // Bigger font
            setFont(getFont().deriveFont(16f));

            // Padding and alignment
            setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            // Get theme from theme manager
            Theme theme = ThemeManager.getTheme();

            // Background colors
            if (isSelected) {
                setBackground(theme.sidebarActiveColor());
                setForeground(theme.textColor());
            } else {
                setBackground(theme.sidebarColor());
                setForeground(theme.textColor());
            }

	    return this;
	}
    }

}
