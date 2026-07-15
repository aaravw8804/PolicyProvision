package com.telusko.policyprovision.util;

/**
 * Masks PAN numbers for display in GET responses (bonus: masked PII).
 * Keeps the first two and last two characters visible and masks the rest,
 * e.g. "ABCDE1234F" -> "AB******4F".
 */
public final class PanMasker {

    private PanMasker() {
    }

    public static String mask(String pan) {
        if (pan == null || pan.isBlank()) {
            return null;
        }
        int len = pan.length();
        if (len <= 4) {
            return "*".repeat(len);
        }
        String visibleStart = pan.substring(0, 2);
        String visibleEnd = pan.substring(len - 2);
        String masked = "*".repeat(len - 4);
        return visibleStart + masked + visibleEnd;
    }
}
