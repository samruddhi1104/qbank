package com.qbank.qbank;

import java.util.List;

public final class Subjects {

    public static final List<String> ALL =
        List.of("DevOps", "Deep Learning (DL)", "NLP");

    private Subjects() { }

    /** Returns the official spelling of a subject, or null if it is not one of the three. */
    public static String canonical(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim().replaceAll("\\s+", " ");
        for (String x : ALL) {
            if (x.equalsIgnoreCase(s)) {
                return x;
            }
        }
        return null;
    }

    public static String icon(String subject) {
        if ("DevOps".equals(subject)) {
            return "\u2699\uFE0F";          // gear
        }
        if ("Deep Learning (DL)".equals(subject)) {
            return "\uD83E\uDDE0";          // brain
        }
        if ("NLP".equals(subject)) {
            return "\uD83D\uDCAC";          // speech bubble
        }
        return "\uD83D\uDCC1";              // folder
    }
}