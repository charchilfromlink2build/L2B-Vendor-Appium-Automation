package com.l2b.vendor.utils;

import io.appium.java_client.AppiumDriver;

/** Thread-confined driver holder — same contract as Customer {@code DriverManager}. */
public final class DriverManager {

    private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
    }

    public static void set(AppiumDriver driver) {
        DRIVER.set(driver);
    }

    public static AppiumDriver get() {
        AppiumDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "No Appium session on thread '" + Thread.currentThread().getName()
                            + "'. Did the test extend BaseTest?");
        }
        return driver;
    }

    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    public static void quit() {
        AppiumDriver driver = DRIVER.get();
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } finally {
            DRIVER.remove();
        }
    }
}
