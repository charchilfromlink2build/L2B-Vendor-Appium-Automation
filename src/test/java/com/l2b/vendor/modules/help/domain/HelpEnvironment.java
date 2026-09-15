package com.l2b.vendor.modules.help.domain;

import com.l2b.vendor.environment.SuiteEnvironment;

/** Support tickets and chat. */
public final class HelpEnvironment {

    private HelpEnvironment() {
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
