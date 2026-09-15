package com.l2b.vendor.core.locators;

import org.openqa.selenium.By;

/**
 * Compose locator factory matching L2B Customer {@code @AndroidFindBy} XPaths.
 *
 * <p>Customer screens do not use a class named ComposeLocators; they inline this same
 * pattern, e.g. {@code //android.view.View[@clickable='true'][.//android.widget.TextView[@text='English']]}.
 * This helper is that pattern in one place for vendor pages that need dynamic text.
 */
public final class ComposeLocators {

    private ComposeLocators() {
    }

    public static By clickableWithText(String text) {
        return By.xpath("//android.view.View[@clickable='true'][.//android.widget.TextView[@text="
                + xpathLiteral(text) + "]]");
    }

    public static By textView(String text) {
        return By.xpath("//android.widget.TextView[@text=" + xpathLiteral(text) + "]");
    }

    public static By textViewContains(String fragment) {
        return By.xpath("//android.widget.TextView[contains(@text," + xpathLiteral(fragment) + ")]");
    }

    public static String clickableWithTextXpath(String text) {
        return "//android.view.View[@clickable='true'][.//android.widget.TextView[@text="
                + xpathLiteral(text) + "]]";
    }

    static String xpathLiteral(String value) {
        if (value == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        String[] parts = value.split("'");
        StringBuilder concat = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                concat.append(",\"'\",");
            }
            concat.append("'").append(parts[i]).append("'");
        }
        concat.append(")");
        return concat.toString();
    }
}
