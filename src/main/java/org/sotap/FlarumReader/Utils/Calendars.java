package org.sotap.FlarumReader.Utils;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public final class Calendars {
    public static Date parse(String input, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            return sdf.parse(input);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
