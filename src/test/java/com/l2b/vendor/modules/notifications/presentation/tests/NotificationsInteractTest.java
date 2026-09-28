package com.l2b.vendor.modules.notifications.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.notifications.presentation.pages.NotificationsPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Notifications interact — tabs, See all, View Details probe, Back on
 * {@code 9000000001}. Never Accept / Decline. Dump
 * {@code /tmp/l2b-notifications-0001-20260928}.
 */
@Epic("Vendor app")
@Feature("Notifications interact — rental 9000000001")
public class NotificationsInteractTest extends NotificationsBaseTest {

    @Test(priority = 1, description = "NT-I1: Unread → All tab")
    @Severity(SeverityLevel.BLOCKER)
    public void unreadToAllTab() {
        NotificationsPage page = reachNotificationsViaBell();
        page.tapUnread();
        sleepQuiet(500);
        page.tapAll();
        sleepQuiet(900);
        page.attachScreenshot("notif-nt-i1-all");
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isRentalsCategoryVisible() || page.isViewDetailsVisible()
                || page.isUnreadEmptyVisible()).isTrue();
    }

    @Test(priority = 2, description = "NT-I2: All → Unread tab")
    @Severity(SeverityLevel.CRITICAL)
    public void allToUnreadTab() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapUnread();
        sleepQuiet(800);
        page.attachScreenshot("notif-nt-i2-unread");
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isUnreadEmptyVisible() || page.hasNewBookingRequest()).isTrue();
    }

    @Test(priority = 3, description = "NT-I3: View all notifications → All content")
    @Severity(SeverityLevel.CRITICAL)
    public void viewAllSwitchesToList() {
        NotificationsPage page = reachNotificationsViaBell();
        page.tapUnread();
        sleepQuiet(500);
        if (page.isViewAllNotificationsVisible()) {
            page.tapViewAllNotifications();
            sleepQuiet(900);
        }
        assertThat(page.isRentalsCategoryVisible() || page.isViewDetailsVisible()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("notifications");
    }

    @Test(priority = 4, description = "NT-I4: See all → Rentals category + Mark all read")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 06: See all opens Rentals list with Mark all read.")
    public void seeAllOpensCategory() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapSeeAll();
        sleepQuiet(1000);
        Allure.parameter("after", classifyRentalNow());
        page.attachScreenshot("notif-nt-i4-see-all");
        assertThat(page.isCategoryListDisplayed() || page.isMarkAllReadVisible()).isTrue();
        assertThat(classifyRentalNow()).isIn("notifications-category", "notifications", "vendor-other");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 5, description = "NT-I5: Back from See all → Notification")
    @Severity(SeverityLevel.CRITICAL)
    public void backFromSeeAll() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapSeeAll();
        sleepQuiet(900);
        page.tapHeaderBack();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(page.isDisplayedNow() || "notifications".equals(classifyRentalNow())).isTrue();
    }

    @Test(priority = 6, description = "NT-I6: View Details → Quick Booking (no Accept)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Dump 08: View Details opens QB. Never tap Accept/Decline.")
    public void viewDetailsOpensQuickBooking() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapViewDetails();
        sleepQuiet(1200);
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        page.attachScreenshot("notif-nt-i6-view-details");
        assertThat(after).isIn("quick-booking", "vendor-other");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        // dismiss without Accept
        page.tapCloseIfPresent();
        sleepQuiet(700);
        if ("quick-booking".equals(classifyRentalNow())) {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        dismissToNotifications(page);
    }

    @Test(priority = 7, description = "NT-I7: Back from View Details → Notification")
    @Severity(SeverityLevel.CRITICAL)
    public void backFromViewDetails() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapViewDetails();
        sleepQuiet(1100);
        page.tapCloseIfPresent();
        sleepQuiet(700);
        if (!page.isDisplayedNow()) {
            DriverManager.get().navigate().back();
            sleepQuiet(800);
        }
        Allure.parameter("after", classifyRentalNow());
        page.attachScreenshot("notif-nt-i7-back");
        assertThat(page.isDisplayedNow() || "notifications".equals(classifyRentalNow())).isTrue();
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
    }

    @Test(priority = 8, description = "NT-I8: header Back → Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 12: header Back → Home.")
    public void headerBackToHome() {
        NotificationsPage page = reachNotificationsViaBell();
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isIn("home", "extend-time", "quick-booking");
    }

    @Test(priority = 9, description = "NT-I9: device Back → Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 14: device Back → Home.")
    public void deviceBackToHome() {
        NotificationsPage page = reachNotificationsViaBell();
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        assertThat(after).isNotEqualTo("launcher");
        assertThat(after).isIn("home", "extend-time", "quick-booking");
    }

    @Test(priority = 10, description = "NT-I10: reopen bell after Back")
    @Severity(SeverityLevel.NORMAL)
    public void reopenAfterBack() {
        NotificationsPage page = reachNotificationsViaBell();
        page.tapHeaderBack();
        sleepQuiet(800);
        new HomePage().tapDesc("Notifications");
        page.waitUntilLoaded();
        assertThat(classifyRentalNow()).isEqualTo("notifications");
        assertThat(page.isTitleVisible()).isTrue();
    }

    @Test(priority = 11, description = "NT-I11: scroll All keeps chrome")
    @Severity(SeverityLevel.NORMAL)
    public void scrollAllKeepsChrome() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.swipeListUp();
        sleepQuiet(600);
        page.attachScreenshot("notif-nt-i11-scroll");
        assertThat(page.isTitleVisible()).isTrue();
        assertThat(page.isUnreadTabVisible()).isTrue();
        assertThat(page.isAllTabVisible()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("notifications");
    }

    @Test(priority = 12, description = "NT-I12: Mark all read probe")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 06: Mark all read on Rentals category — stay Vendor.")
    public void markAllReadProbe() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapSeeAll();
        sleepQuiet(1000);
        Allure.parameter("hasMarkAll", String.valueOf(page.isMarkAllReadVisible()));
        if (page.isMarkAllReadVisible()) {
            page.tapMarkAllRead();
            sleepQuiet(1000);
        }
        page.attachScreenshot("notif-nt-i12-mark-all");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
        page.tapHeaderBack();
        sleepQuiet(700);
    }

    @Test(priority = 13, description = "NT-I13: Close on QB from View Details")
    @Severity(SeverityLevel.CRITICAL)
    public void closeQuickBookingFromDetails() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapViewDetails();
        sleepQuiet(1200);
        page.tapCloseIfPresent();
        sleepQuiet(900);
        if ("quick-booking".equals(classifyRentalNow())) {
            DriverManager.get().navigate().back();
            sleepQuiet(800);
        }
        Allure.parameter("after", classifyRentalNow());
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        dismissToNotifications(page);
        assertThat(page.isDisplayedNow() || "notifications".equals(classifyRentalNow())
                || "home".equals(classifyRentalNow())).isTrue();
    }

    @Test(priority = 14, description = "NT-I14: Unread empty after returning from All")
    @Severity(SeverityLevel.NORMAL)
    public void unreadEmptyAfterAll() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapUnread();
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isUnreadEmptyVisible() || page.hasNewBookingRequest()).isTrue();
    }
}
