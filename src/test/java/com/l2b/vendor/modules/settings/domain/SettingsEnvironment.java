package com.l2b.vendor.modules.settings.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Account, language, permissions. */
public final class SettingsEnvironment {

    private SettingsEnvironment() {
    }

    public static void prepareSuite() {
        SuiteEnvironment.prepare();
    }

    public static boolean noReset() {
        return true;
    }

    public static boolean newSessionPerMethod() {
        return true;
    }
}
