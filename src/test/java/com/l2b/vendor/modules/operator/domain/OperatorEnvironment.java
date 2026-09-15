package com.l2b.vendor.modules.operator.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Operator dashboard and tasks. */
public final class OperatorEnvironment {

    private OperatorEnvironment() {
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
