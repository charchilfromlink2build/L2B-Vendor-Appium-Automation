package com.l2b.vendor.modules.notifications.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Notification feed. */
public final class NotificationsEnvironment {

    private NotificationsEnvironment() {
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
