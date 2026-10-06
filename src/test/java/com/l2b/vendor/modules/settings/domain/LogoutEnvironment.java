package com.l2b.vendor.modules.settings.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Profile drawer → Log Out Confirm + session checks. */
public final class LogoutEnvironment {
    private LogoutEnvironment() {
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
