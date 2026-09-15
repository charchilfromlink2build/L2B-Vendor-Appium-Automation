package com.l2b.vendor.modules.home.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Logged-in rental/material home. Keep session (noReset). */
public final class HomeEnvironment {

    private HomeEnvironment() {
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
