package com.l2b.vendor.modules.quickbooking.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/**
 * Quick Booking module environment. First-launch path (language → OTP → landing)
 * needs a data-clear, so {@code noReset=false} and one Appium session per method.
 */
public final class QuickBookingEnvironment {

    private QuickBookingEnvironment() {
    }

    public static void prepareSuite() {
        SuiteEnvironment.prepare();
    }

    public static boolean noReset() {
        return false;
    }

    public static boolean newSessionPerMethod() {
        return true;
    }
}
