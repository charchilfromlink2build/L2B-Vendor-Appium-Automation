package com.l2b.vendor.modules.faq.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Profile drawer → FAQ. */
public final class FaqEnvironment {
    private FaqEnvironment() {
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
