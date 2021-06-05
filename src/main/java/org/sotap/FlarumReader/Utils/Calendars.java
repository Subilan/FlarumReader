package org.sotap.FlarumReader.Utils;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public final class Calendars {
    public final static String UTC_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";

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
