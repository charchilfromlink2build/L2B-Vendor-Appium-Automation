package com.l2b.vendor.modules.policies.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Profile drawer → Policies. */
public final class PoliciesEnvironment {
    private PoliciesEnvironment() {
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
