package org.chatapp.client.ui.components;


import javax.swing.*;import java.awt.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class DatePanel extends JPanel {
    private final LocalDate date;

    public DatePanel(LocalDate date){
        // Create wrapper box that handles the date bubble positioning in the message area
        super(new FlowLayout(FlowLayout.CENTER, 10, 5));
        setOpaque(false);
        JLabel dateLabel = new RoundLabel(createDateString(date));

        add(dateLabel);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, this.getPreferredSize().height));
        this.date = date;
    }

    // This class decides how to display the message dates separators. For example if the date is 1 day before current
    // date it will be shown as 'Yesterday'
    private String createDateString(LocalDate date) {
        LocalDate today = LocalDate.now();

        // If date is same as current date show 'Today'
        if(date.equals(today)){
            return "Today";
        }else if(date.until(today, ChronoUnit.DAYS) == 1) { // If date is 1 day before current date show 'Yesterday'
            return "Yesterday";
        }else if(date.until(today, ChronoUnit.DAYS) < 7) { // If date is less than 7 days before current date show day of week
            return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        } else if(date.getYear() == today.getYear()){ // If day is
            return date.getDayOfMonth() + " " + date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        } else {
            return date.getDayOfMonth() + " " + date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + date.getYear();
        }
    }

    // Returns the date stored in this wrapper for comparison with new message dates
    public LocalDate getDate() {
        return date;
    }

}
