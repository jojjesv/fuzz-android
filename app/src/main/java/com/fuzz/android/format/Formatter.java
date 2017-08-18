package com.fuzz.android.format;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats in various ways.
 */
public class Formatter {
    private static Locale locale = new Locale("sv");

    public static String formatCurrency(double number) {
        boolean isInt = number % 1 == 0;

        if (isInt && number < 1000) {
            //  No formatting needed
            return "" + (int) number;
        }

        NumberFormat numberFormatter = NumberFormat.getNumberInstance(locale);

        String formattedNumber = numberFormatter.format(number);

        if (isInt) {
            //  No decimals
            return formattedNumber;
        }

        //  Append zero if < 2 & > 0 decimals
        int decimalPointIndex = formattedNumber.indexOf(',');
        if (decimalPointIndex != -1 && decimalPointIndex >= formattedNumber.length() - 2) {
            formattedNumber += "0";
        }

        return formattedNumber;
    }

    /**
     * Glues strings together in a semantic matter.
     *
     * @return
     */
    public static String formatStrings(String... strings) {
        return formatStrings(999, strings);
    }

    /**
     * Glues strings together in a semantic matter.
     *
     * @return
     */
    public static String formatStrings(int limit, String... strings) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0, n = strings.length - 1; i <= n; i++) {
            builder.append(strings[i]);

            if (i > limit){
                //  Reached limit
                builder.append(" m.fl.");
                break;
            }

            if (i < n) {
                builder.append(", ");
            } else {
                builder.append(" och ");
            }
        }

        return builder.toString();
    }
}
