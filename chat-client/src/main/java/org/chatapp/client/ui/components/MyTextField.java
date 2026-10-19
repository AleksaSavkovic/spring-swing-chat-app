package org.chatapp.client.ui.components;

import org.chatapp.client.ui.ThemeManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class MyTextField extends JTextField {

    private Shape shape;
    private final String placeholder;
    private Color backgroundColor;
    private boolean isPlaceholder;

    public MyTextField(String placeholder, Color backgroundColor, Color textColor, Color placeholderColor,  int fontSize){
        super();
        this.backgroundColor = backgroundColor;
        this.placeholder = placeholder;

        setFocusable(false);
        // Set background color, font size and color
        setBackground(backgroundColor);
        setForeground(textColor);
        Font currentFont = getFont();
        Font bigFont = currentFont.deriveFont(Font.PLAIN, fontSize);
        setFont(bigFont);

        // Add a thick border the same color as the search bar background to make padding effect
        Border searchBorder = BorderFactory.createLineBorder(backgroundColor, 8);
        setBorder(searchBorder);

        // Set placeholder and related events
        setForeground(placeholderColor);
        setText(placeholder);
        setPlaceholder(placeholder, textColor, placeholderColor);
        setOpaque(false);
        isPlaceholder = true;

        // This makes the elemet focusasble only on click
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setFocusable(true);
                requestFocusInWindow();
            }
        });
        // Make the caret visible
        setCaret(getCaret());
        getCaret().setBlinkRate(500);
        getCaret().setVisible(true);
    }

    // Add text field placeholder when it's empty and not focused
    public void setPlaceholder(String placeholder, Color textColor, Color placeholderColor){
        // Get all focus listeners (should be only one)
        FocusListener[] listeners = getFocusListeners();

        // Remove each listener from the component (should be only one)
        for (FocusListener listener : listeners) {
            removeFocusListener(listener);
        }
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if(isPlaceholder) {
                    if (getText().equals(placeholder)) {
                        setText("");
                    }
                    setForeground(textColor);
                    isPlaceholder = false;
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                setFocusable(false);
                if (getText().isEmpty()) {
                    setForeground(placeholderColor);
                    setText(placeholder);
                    isPlaceholder = true;
                }
            }
        });
    }

    // Put field into placeholder mode manually
    public void revertToPlaceholderText() {
        setFocusable(false);
        setForeground(ThemeManager.getTheme().searchPromptColor());
        setText(placeholder);
        isPlaceholder = true;
    }


    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        g.setColor(backgroundColor);
        g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
    }

    @Override
    public boolean contains(int x, int y) {
        if (shape == null || !shape.getBounds().equals(getBounds())) {
            shape = new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 10, 10);
        }
        return shape.contains(x, y);
    }

    public String getPlaceholder(){
        return placeholder;
    }

    public void setBackgroundColor(Color backgroundColor){
        this.backgroundColor = backgroundColor;
        setBackground(this.backgroundColor);
    }

    // Returns blank if the text is placeholder
    public String getCleanText(){
        return isPlaceholder ? "" : getText().trim();
    }
}
