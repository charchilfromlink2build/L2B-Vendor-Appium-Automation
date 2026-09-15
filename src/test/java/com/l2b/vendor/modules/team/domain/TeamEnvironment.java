package com.l2b.vendor.modules.team.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Drawer team members. */
public final class TeamEnvironment {

    private TeamEnvironment() {
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
