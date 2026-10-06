package com.l2b.vendor.modules.refer.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Profile drawer → Refer & Earn. */
public final class ReferEnvironment {

    private ReferEnvironment() {
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
