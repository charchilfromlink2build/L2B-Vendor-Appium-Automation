package com.l2b.vendor.modules.notifications.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.notifications.presentation.pages.NotificationsPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Notifications landing — Home bell on {@code 9000000001}.
 * Dump {@code /tmp/l2b-notifications-0001-20260928}.
 */
@Epic("Vendor app")
@Feature("Notifications landing — rental 9000000001")
public class NotificationsLandingTest extends NotificationsBaseTest {

    @Test(priority = 1, description = "NT-L1: Home bell → Notification")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Tap Notifications bell. Expect classify=notifications, title, Unread/All.")
    public void bellOpensNotification() {
        NotificationsPage page = reachNotificationsViaBell();
        page.attachScreenshot("notif-nt-l1");
        assertThat(classifyRentalNow()).isEqualTo("notifications");
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 2, description = "NT-L2: title + Back chrome")
    @Severity(SeverityLevel.CRITICAL)
    public void titleAndBackChrome() {
        NotificationsPage page = reachNotificationsViaBell();
        page.attachScreenshot("notif-nt-l2");
        assertThat(page.isTitleVisible()).isTrue();
        assertThat(page.isBackVisible()).isTrue();
        assertThat(page.headerTitleBackAligned())
                .as("Back and Notification title share header band")
                .isTrue();
    }

    @Test(priority = 3, description = "NT-L3: Unread | All tabs")
    @Severity(SeverityLevel.CRITICAL)
    public void unreadAndAllTabs() {
        NotificationsPage page = reachNotificationsViaBell();
        assertThat(page.isUnreadTabVisible()).isTrue();
        assertThat(page.isAllTabVisible()).isTrue();
        assertThat(page.unreadAllTabsLayoutOk())
                .as("Unread and All side-by-side equal width")
                .isTrue();
    }

    @Test(priority = 4, description = "NT-L4: Unread empty copy")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 01: You're all caught up. on Unread for 0001.")
    public void unreadEmptyCopy() {
        NotificationsPage page = reachNotificationsViaBell();
        page.tapUnread();
        sleepQuiet(700);
        page.attachScreenshot("notif-nt-l4-empty");
        assertThat(page.isUnreadEmptyVisible()).isTrue();
        assertThat(page.isViewAllNotificationsVisible()).isTrue();
    }

    @Test(priority = 5, description = "NT-L5: View all notifications CTA")
    @Severity(SeverityLevel.CRITICAL)
    public void viewAllNotificationsCta() {
        NotificationsPage page = reachNotificationsViaBell();
        page.tapUnread();
        sleepQuiet(500);
        assertThat(page.isViewAllNotificationsVisible()).isTrue();
        page.tapViewAllNotifications();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        page.attachScreenshot("notif-nt-l5-view-all");
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isRentalsCategoryVisible() || page.isViewDetailsVisible()).isTrue();
    }

    @Test(priority = 6, description = "NT-L6: All tab Rentals category")
    @Severity(SeverityLevel.CRITICAL)
    public void allTabRentalsCategory() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.attachScreenshot("notif-nt-l6-all");
        assertThat(page.isRentalsCategoryVisible()).isTrue();
        assertThat(page.isSeeAllVisible()).isTrue();
    }

    @Test(priority = 7, description = "NT-L7: All list booking rows + View Details")
    @Severity(SeverityLevel.CRITICAL)
    public void allListBookingRows() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        Allure.parameter("hasRequest", String.valueOf(page.hasNewBookingRequest()));
        Allure.parameter("hasViewDetails", String.valueOf(page.isViewDetailsVisible()));
        Allure.parameter("hasAgo", String.valueOf(page.hasRelativeTime()));
        assertThat(page.hasNewBookingRequest()).isTrue();
        assertThat(page.isViewDetailsVisible()).isTrue();
        assertThat(page.hasRelativeTime()).isTrue();
    }

    @Test(priority = 8, description = "NT-L8: bottom tabs gone (stack screen)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Notification is a stack — Calendar/Earning/Fleet tabs must not show.")
    public void bottomTabsGone() {
        NotificationsPage page = reachNotificationsViaBell();
        assertThat(page.bottomTabsAbsent())
                .as("Publish: Notification stack hides bottom tabs")
                .isTrue();
    }

    @Test(priority = 9, description = "NT-L9: title left inset padding")
    @Severity(SeverityLevel.NORMAL)
    public void titleLeftInsetPublish() {
        NotificationsPage page = reachNotificationsViaBell();
        assertThat(page.titleLeftInsetOk())
                .as("Notification title x should be 80–180 after Back")
                .isTrue();
    }

    @Test(priority = 10, description = "NT-L10: stay Vendor package")
    @Severity(SeverityLevel.CRITICAL)
    public void staysOnVendorPackage() {
        NotificationsPage page = reachNotificationsViaBell();
        page.attachScreenshot("notif-nt-l10");
        assertThat(vendorPackage()).isEqualTo(com.l2b.vendor.environment.Config.get("app.package"));
        assertThat(classifyRentalNow()).isEqualTo("notifications");
    }
}
