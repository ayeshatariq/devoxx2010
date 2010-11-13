package net.peterkuterna.android.apps.devoxxsched.util;

import android.net.Uri;

public class UriUtils {

    public static boolean readBooleanQueryParameter(Uri uri, String parameter, boolean defaultValue) {
        // Manually parse the query, which is much faster than calling uri.getQueryParameter
        String query = uri.getEncodedQuery();
        if (query == null) {
            return defaultValue;
        }

        int index = query.indexOf(parameter);
        if (index == -1) {
            return defaultValue;
        }

        index += parameter.length();

        return !matchQueryParameter(query, index, "=0", false)
                && !matchQueryParameter(query, index, "=false", true);
    }

    private static boolean matchQueryParameter(String query, int index, String value,
            boolean ignoreCase) {
        int length = value.length();
        return query.regionMatches(ignoreCase, index, value, 0, length)
                && (query.length() == index + length || query.charAt(index + length) == '&');
    }

}
