package com.example.product.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SearchUtils {

    // Do not allow to create an Object as all the methods are static
    private SearchUtils() {
    }

    /**
     * Converts wildcard search pattern to MongoDB regex pattern.
     * Supports:
     * - * (asterisk) -> matches any sequence of characters (.*)
     * - ? (question mark) -> matches any single character (.)
     * - Escapes other regex special characters
     *
     * @param searchPattern The search pattern with wildcards
     * @return Regex pattern string for MongoDB query
     */
    public static String convertWildcardToRegex(String searchPattern) {
        if (searchPattern == null || searchPattern.isEmpty()) {
            return "";
        }

        // Manually escape special regex chars except * and ? which are converted to wildcards
        StringBuilder regex = new StringBuilder();
        for (char c : searchPattern.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.', '^', '$', '+', '(', ')', '[', ']', '{', '}', '|', '\\':
                    regex.append("\\").append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }

        log.debug("convertWildcardToRegex:: searchPattern - {}, regex - {}", searchPattern, regex.toString());
        return regex.toString();
    }
}

