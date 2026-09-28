package com.l2b.vendor.modules.calendar.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.calendar.presentation.pages.CalendarPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

/**
 * Shared 9000000001 path onto Schedule/Calendar. Close QB only. Never Accept /
 * Decline / Log Out. Start Task only probed — no job completion.
 */
public abstract class CalendarBaseTest extends RentalHomeBaseTest {

    @Step("Reach Schedule via bottom Calendar tab")
    protected CalendarPage reachCalendarViaTab() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc("Calendar");
        CalendarPage cal = new CalendarPage();
        cal.waitUntilLoaded();
        Allure.parameter("entry", "bottom-tab");
        Allure.parameter("after", classifyRentalNow());
        return cal;
    }

    /**
     * Recover onto Schedule without a full re-login (re-login mid-case races Language
     * splash when Start Task / Back leaves a non-calendar Vendor surface).
     */
    @Step("Dismiss overlays until Schedule (tab tap preferred)")
    protected CalendarPage dismissToCalendar(CalendarPage cal) {
        for (int i = 0; i < 5; i++) {
            if (cal.isDisplayedNow()) {
                return cal;
            }
            String named = classifyRentalNow();
            if ("launcher".equals(named)) {
                Allure.parameter("dismissStop", "launcher");
                return cal;
            }
            try {
                if (cal.isRentalBottomTabsVisible()) {
                    cal.tapCalendarTab();
                    sleepQuiet(900);
                    if (cal.isDisplayedNow()) {
                        return cal;
                    }
                }
            } catch (RuntimeException ignored) {
                // fall through to device Back
            }
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        if (!cal.isDisplayedNow()) {
            try {
                if (cal.isRentalBottomTabsVisible()) {
                    cal.tapCalendarTab();
                    sleepQuiet(900);
                } else {
                    HomePage home = new HomePage();
                    if (home.isDisplayedNow()) {
                        home.tapDesc("Calendar");
                        sleepQuiet(900);
                    }
                }
            } catch (RuntimeException e) {
                Allure.parameter("dismissRecoverError", e.getClass().getSimpleName());
            }
        }
        return cal;
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
