package com.l2b.vendor.environment;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Global machine/Appium/Allure setup. Feature modules call this from their
 * own {@code *Environment.prepareSuite()} — not from {@code BaseTest}.
 */
public final class SuiteEnvironment {

    private static final AtomicBoolean CHECKED = new AtomicBoolean(false);
    private static final AtomicBoolean ALLURE = new AtomicBoolean(false);

    private SuiteEnvironment() {
    }

    public static void prepare() {
        if (CHECKED.compareAndSet(false, true)) {
            EnvironmentCheck.verify();
        }
        if (ALLURE.compareAndSet(false, true)) {
            AllureEnvironment.write();
        }
    }
}
