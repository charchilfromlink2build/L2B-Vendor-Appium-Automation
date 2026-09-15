package com.l2b.vendor.modules.calendar.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Vendor schedule. */
public final class CalendarEnvironment {

    private CalendarEnvironment() {
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
