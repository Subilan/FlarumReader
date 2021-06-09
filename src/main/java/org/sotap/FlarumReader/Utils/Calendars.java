package org.sotap.FlarumReader.Utils;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public final class Calendars {
    public final static String UTC_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";
    public final static String DATE_FORMAT_PATTERN = "yyyy/MM/dd HH:mm:ss";

    public static Date parse(String input, String pattern) {
        var sdf = new SimpleDateFormat(pattern);
        try {
            return sdf.parse(input);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String toString(Date input) {
        var sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return sdf.format(input);
    }
}
