package com.l2b.vendor.modules.notifications.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;
import com.l2b.vendor.modules.notifications.presentation.pages.NotificationsPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

/**
 * Shared 9000000001 path onto Notifications via Home bell. Close QB only.
 * Never Accept / Decline / Log Out.
 */
public abstract class NotificationsBaseTest extends RentalHomeBaseTest {

    @Step("Reach Notification via Home bell")
    protected NotificationsPage reachNotificationsViaBell() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc("Notifications");
        NotificationsPage page = new NotificationsPage();
        page.waitUntilLoaded();
        Allure.parameter("entry", "bell");
        Allure.parameter("after", classifyRentalNow());
        return page;
    }

    @Step("Dismiss toward Notification (no full re-login)")
    protected NotificationsPage dismissToNotifications(NotificationsPage page) {
        for (int i = 0; i < 6; i++) {
            if (page.isDisplayedNow()) {
                return page;
            }
            String named = classifyRentalNow();
            if ("launcher".equals(named)) {
                Allure.parameter("dismissStop", "launcher");
                return page;
            }
            if ("quick-booking".equals(named)) {
                page.tapCloseIfPresent();
                sleepQuiet(700);
                if (page.isDisplayedNow()) {
                    return page;
                }
            }
            if ("home".equals(named) || "extend-time".equals(named) || "quick-booking".equals(named)) {
                try {
                    new HomePage().tapDesc("Notifications");
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
        return page;
    }

    @Step("Ensure All tab list (via View all if Unread empty)")
    protected NotificationsPage ensureAllList(NotificationsPage page) {
        if (!page.isDisplayedNow()) {
            page = reachNotificationsViaBell();
        }
        if (page.isViewDetailsVisible() || page.isRentalsCategoryVisible()) {
            return page;
        }
        if (page.isViewAllNotificationsVisible()) {
            page.tapViewAllNotifications();
            sleepQuiet(900);
        } else {
            page.tapAll();
            sleepQuiet(900);
        }
        return page;
    }

    protected static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
