package com.l2b.vendor.modules.bookings.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Rental booking list / accept / assign. */
public final class BookingsEnvironment {

    private BookingsEnvironment() {
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
