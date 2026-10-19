package org.chatapp.client.service;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateConvertor {

    private static final DateTimeFormatter HOUR_MIN_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    // Converts the local date time object to UTC String which can be saved to SQLITE local database
    public static String convertLocalToUTCString(LocalDateTime localDate){
        if(localDate == null) {
            return null;
        }
        return localDate.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
    }

    public static LocalDate toLocal(Instant instant){
        return LocalDate.ofInstant(instant, ZoneId.systemDefault());
    }

    public static LocalDateTime toLocalDateTime(Instant instant){
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    // Formats instant to HH:mm format string
    public static String toHourMinString(Instant instant) {
        return HOUR_MIN_FORMATTER.format(instant);
    }

}
