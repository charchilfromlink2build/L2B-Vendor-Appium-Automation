package com.l2b.vendor.modules.earning.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Wallet and earnings. */
public final class EarningEnvironment {

    private EarningEnvironment() {
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
