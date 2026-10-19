package org.chatapp.client.ui.components;

import org.chatapp.client.interfaces.UiComponentListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class MessageScrollPane extends JScrollPane implements UiComponentListener {
    // Child object of the scroll pane holding all the messages
    private final MessageAreaPanel messageArea;
    private final UiComponentListener parentObject;

    private Timer debounceTimer;
    // Amount of time it needs to pass before another load chat event can be triggered
    private static final int DEBOUNCE_DELAY_MS = 300;

    // Checks if the load event can be triggered again, if it is still loading it can't
    private boolean loading;
    // Keeps track if we reached the end of the chat and there are no more messages to load
    private boolean reachedEnd;
    // Used to keep track of what pageNumber we are currently on
    private int pageNumber;
    private final int pageSize;

    public MessageScrollPane(MessageAreaPanel messageArea, UiComponentListener parentObject) {
        super(messageArea);
        this.messageArea = messageArea;
        this.messageArea.setParentScrollPane(this);
        this.parentObject = parentObject;
        loading = false;
        reachedEnd = false;
        pageNumber = 1;
        pageSize = 25;

        // Adjusting scrollbar settings
        getVerticalScrollBar().setUnitIncrement(10);
        setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setBorder(new EmptyBorder(0, 0, 0, 0));

        // Setting up the event listener for when the user reaches the top of scroll bar
        getVerticalScrollBar().addAdjustmentListener(e -> {
            // Check if the end is reached or there are more chats to load.
            if(reachedEnd){
                return;
            }

            JScrollBar vertical = getVerticalScrollBar();
            // If top is reached need to just fire off an event listener to the parent which will hande this
            if (vertical.getMaximum() > vertical.getVisibleAmount()) {
                if(loading) {
                    debounceTimer.restart();
                    return;
                }
                debounceTimer = new Timer(DEBOUNCE_DELAY_MS, actionEvent -> {
                    loading = false;
                });

                if(vertical.getValue() == 0) {
                    loading = true;
                    pageNumber++;
                    onScrollToTop(pageNumber, pageSize);
                }
            }
        });
        loading = true;
        debounceTimer = new Timer(500, actionEvent -> {
            loading = false;
        });
        debounceTimer.start();

    }

    // Scrolls to the bottom of the message scroll pane
    public void scrollToBottom() {
        JScrollBar vertical = getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }

    // Scrolls to the element with the provided y coordinate
//    public void scrollToElement(int y){
//        System.out.println("ELEMENT HEIGHT - " + y);
//        JScrollBar vertical = getVerticalScrollBar();
//        vertical.setValue(y);
//    }

    @Override
    public void onScrollToBottom() {
        SwingUtilities.invokeLater(this::scrollToBottom);
    }

    @Override
    public void onScrollToTop(int pageNumber, int pageSize) {
        parentObject.onScrollToTop(pageNumber, pageSize);
    }

    @Override
    public void onLogoutEvent() {

    }

    @Override
    public void onMessageMenuEvent(String event, MessageBubble messageBubble) {
        parentObject.onMessageMenuEvent(event, messageBubble);
    }

    @Override
    public void onThemeChange(String themeName) {

    }

    // Gets the reached end variable which checks if all the chats have been loaded
    public boolean isReachedEnd() {
        return reachedEnd;
    }

    // Sets the reached end variable to true for when there are no more chats to load, or false when a new chat is opened
    //  and the message area is refreshed
    public void setReachedEnd(boolean reachedEnd) {
        this.reachedEnd = reachedEnd;
    }

    // If a chat is switched reset page number back to 0
    public void resetPageNumber(){
        pageNumber = 1;
    }

    public MessageAreaPanel getMessageArea() {
        return messageArea;
    }
}
