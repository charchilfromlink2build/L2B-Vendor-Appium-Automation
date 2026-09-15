package com.l2b.vendor.modules.material.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Inventory and material orders. */
public final class MaterialEnvironment {

    private MaterialEnvironment() {
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
