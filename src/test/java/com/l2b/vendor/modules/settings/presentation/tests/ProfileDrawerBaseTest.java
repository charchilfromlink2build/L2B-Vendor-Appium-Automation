package com.l2b.vendor.modules.settings.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

/**
 * Shared 9000000001 path onto Profile drawer via Home Profile. Close QB only.
 * Never Accept / Decline / Log Out Confirm.
 */
public abstract class ProfileDrawerBaseTest extends RentalHomeBaseTest {

    @Step("Reach Profile drawer via Home Profile")
    protected ProfileDrawerPage reachProfileDrawer() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc("Profile");
        ProfileDrawerPage page = new ProfileDrawerPage();
        page.waitUntilLoaded();
        Allure.parameter("entry", "profile");
        Allure.parameter("after", classifyRentalNow());
        return page;
    }

    @Step("Dismiss toward Profile drawer (no full re-login)")
    protected ProfileDrawerPage dismissToDrawer(ProfileDrawerPage page) {
        for (int i = 0; i < 8; i++) {
            if (page.isDisplayedNow()) {
                return page;
            }
            String named = classifyRentalNow();
            if ("launcher".equals(named)) {
                Allure.parameter("dismissStop", "launcher");
                return page;
            }
            if ("home-drawer".equals(named)) {
                return page;
            }
            if (page.isLogoutDialogVisible()) {
                page.tapLogoutCancel();
                sleepQuiet(700);
                if (page.isDisplayedNow()) {
                    return page;
                }
            }
            if ("quick-booking".equals(named)) {
                try {
                    new com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage()
                            .tapClose();
                } catch (RuntimeException ignored) {
                }
                sleepQuiet(700);
            }
            if ("home".equals(named) || "extend-time".equals(named)) {
                try {
                    new HomePage().tapDesc("Profile");
                    sleepQuiet(900);
                    if (page.isDisplayedNow()) {
                        return page;
                    }
                } catch (RuntimeException ignored) {
                }
            }
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        if (!page.isDisplayedNow()) {
            return reachProfileDrawer();
        }
        return page;
    }

    @Step("Ensure drawer open (re-tap Profile if closed)")
    protected ProfileDrawerPage ensureDrawerOpen(ProfileDrawerPage page) {
        if (page.isDisplayedNow()) {
            return page;
        }
        if (page.isLogoutDialogVisible()) {
            page.tapLogoutCancel();
            sleepQuiet(700);
        }
        if ("home".equals(classifyRentalNow()) || new HomePage().isDisplayedNow()) {
            new HomePage().tapDesc("Profile");
            sleepQuiet(900);
            page.waitUntilLoaded();
            return page;
        }
        return dismissToDrawer(page);
    }

    protected static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
