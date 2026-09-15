package com.l2b.vendor.modules.onboarding.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/**
 * First-launch module environment: clear app data, one shared Appium session.
 */
public final class OnboardingEnvironment {

    private OnboardingEnvironment() {
    }

    public static void prepareSuite() {
        SuiteEnvironment.prepare();
    }

    public static boolean noReset() {
        return false;
    }

    public static boolean newSessionPerMethod() {
        return false;
    }
}
