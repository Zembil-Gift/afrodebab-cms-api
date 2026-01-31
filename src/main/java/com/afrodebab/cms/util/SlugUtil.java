package com.afrodebab.cms.util;

public class SlugUtil {
    public static String toSlug(String input) {
        if (input == null) return "";
        String s = input.trim().toLowerCase();
        s = s.replaceAll("[^a-z0-9\\s-]", "");
        s = s.replaceAll("\\s+", "-");
        s = s.replaceAll("-{2,}", "-");
        return s;
    }
}
