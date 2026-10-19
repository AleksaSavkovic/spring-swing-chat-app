package org.chatapp.client.ui.components;

import org.chatapp.client.interfaces.ThemedComponent;
import org.chatapp.client.ui.ThemeManager;

import javax.swing.*;
import java.awt.*;

public class ReplyInfoPanel extends JPanel implements ThemedComponent {

    private final JLabel replyInfoLabel;
    private final String replier;

    public ReplyInfoPanel(String replier, String originalPoster, boolean isSent){
        // Create wrapper box that handles the date bubble positioning in the message area based on the sender (active user or another user)
        super(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 5));
        setOpaque(false);
        this.replier = replier;
        replyInfoLabel = new JLabel(replier + " replied to " + originalPoster);
        replyInfoLabel.setForeground(ThemeManager.getTheme().searchPromptColor());

        add(replyInfoLabel);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, this.getPreferredSize().height));
    }

    // Set original poster, this is used when the original poster is not known at render time
    public void setOriginalPoster(String originalPoster) {
        replyInfoLabel.setText(replier + " replied to " + originalPoster);
    }

    @Override
    public void applyTheme() {
        replyInfoLabel.setForeground(ThemeManager.getTheme().searchPromptColor());
    }
}
