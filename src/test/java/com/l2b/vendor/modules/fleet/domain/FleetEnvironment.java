package com.l2b.vendor.modules.fleet.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Machine list and documents. */
public final class FleetEnvironment {

    private FleetEnvironment() {
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
