package com.l2b.vendor.modules.terms.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Profile drawer → Terms & Services. */
public final class TermsEnvironment {
    private TermsEnvironment() {
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
