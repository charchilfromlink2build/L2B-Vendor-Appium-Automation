package com.l2b.vendor.modules.kyc.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Documents and verification. */
public final class KycEnvironment {

    private KycEnvironment() {
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
