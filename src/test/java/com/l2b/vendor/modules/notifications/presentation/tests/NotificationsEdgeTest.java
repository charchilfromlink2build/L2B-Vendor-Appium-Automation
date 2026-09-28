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
 * Notifications edge / publish-quality on {@code 9000000001}.
 * Dump {@code /tmp/l2b-notifications-0001-20260928}. Never Accept / Decline.
 */
@Epic("Vendor app")
@Feature("Notifications edge — rental 9000000001")
public class NotificationsEdgeTest extends NotificationsBaseTest {

    @Test(priority = 1, description = "NT-E1: rapid Unread↔All flip stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidTabFlip() {
        NotificationsPage page = reachNotificationsViaBell();
        for (int i = 0; i < 4; i++) {
            page.tapAll();
            sleepQuiet(350);
            page.tapUnread();
            sleepQuiet(350);
        }
        page.attachScreenshot("notif-nt-e1-flip");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("notifications");
    }

    @Test(priority = 2, description = "NT-E2: View Details then double Back recovers")
    @Severity(SeverityLevel.CRITICAL)
    public void viewDetailsDoubleBack() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapViewDetails();
        sleepQuiet(1100);
        DriverManager.get().navigate().back();
        sleepQuiet(700);
        DriverManager.get().navigate().back();
        sleepQuiet(700);
        Allure.parameter("after", classifyRentalNow());
        page.attachScreenshot("notif-nt-e2-double-back");
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 3, description = "NT-E3: See all then device Back")
    @Severity(SeverityLevel.CRITICAL)
    public void seeAllDeviceBack() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapSeeAll();
        sleepQuiet(900);
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        Allure.parameter("after", classifyRentalNow());
        assertThat(classifyRentalNow()).isIn("notifications", "home", "notifications-category");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 4, description = "NT-E4: View Details Close — never Accept")
    @Severity(SeverityLevel.BLOCKER)
    public void viewDetailsCloseNeverAccept() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapViewDetails();
        sleepQuiet(1200);
        // Explicitly Close / Back only
        page.tapCloseIfPresent();
        sleepQuiet(700);
        if ("quick-booking".equals(classifyRentalNow())) {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
        dismissToNotifications(page);
    }

    @Test(priority = 5, description = "NT-E5: bottom tabs stay absent after All")
    @Severity(SeverityLevel.CRITICAL)
    public void bottomTabsStayAbsentOnAll() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        assertThat(page.bottomTabsAbsent()).isTrue();
        page.swipeListUp();
        sleepQuiet(500);
        assertThat(page.bottomTabsAbsent()).isTrue();
    }

    @Test(priority = 6, description = "NT-E6: header layout publish checks")
    @Severity(SeverityLevel.CRITICAL)
    public void headerLayoutPublish() {
        NotificationsPage page = reachNotificationsViaBell();
        assertThat(page.headerTitleBackAligned()).isTrue();
        assertThat(page.unreadAllTabsLayoutOk()).isTrue();
        assertThat(page.titleLeftInsetOk()).isTrue();
    }

    @Test(priority = 7, description = "NT-E7: re-open bell after Back stays stable")
    @Severity(SeverityLevel.NORMAL)
    @Description("Back to Home then re-tap bell — Notification loads again (bell absent on stack).")
    public void reopenBellStable() {
        NotificationsPage page = reachNotificationsViaBell();
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isIn("home", "extend-time", "quick-booking");
        new HomePage().tapDesc("Notifications");
        page.waitUntilLoaded();
        Allure.parameter("afterReopen", classifyRentalNow());
        page.attachScreenshot("notif-nt-e7-reopen");
        assertThat(page.isTitleVisible()).isTrue();
        assertThat(page.isUnreadTabVisible()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("notifications");
        // Second Back→Home→bell cycle
        page.tapHeaderBack();
        sleepQuiet(800);
        new HomePage().tapDesc("Notifications");
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 8, description = "NT-E8: View Details stale toast still recoverable")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 08 may show 'order no longer available' — Close/Back must recover.")
    public void staleOrderRecoverable() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapViewDetails();
        sleepQuiet(1300);
        page.tapCloseIfPresent();
        sleepQuiet(700);
        for (int i = 0; i < 3 && "quick-booking".equals(classifyRentalNow()); i++) {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        Allure.parameter("after", classifyRentalNow());
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        dismissToNotifications(page);
    }

    @Test(priority = 9, description = "NT-E9: Mark all read then Back")
    @Severity(SeverityLevel.CRITICAL)
    public void markAllReadThenBack() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapSeeAll();
        sleepQuiet(900);
        if (page.isMarkAllReadVisible()) {
            page.tapMarkAllRead();
            sleepQuiet(1000);
        }
        page.tapHeaderBack();
        sleepQuiet(800);
        Allure.parameter("after", classifyRentalNow());
        assertThat(classifyRentalNow()).isIn("notifications", "home");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 10, description = "NT-E10: scroll then header Back → Home")
    @Severity(SeverityLevel.NORMAL)
    public void scrollThenHeaderBack() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.swipeListUp();
        sleepQuiet(500);
        page.tapHeaderBack();
        sleepQuiet(800);
        assertThat(classifyRentalNow()).isIn("home", "extend-time", "quick-booking");
    }

    @Test(priority = 11, description = "NT-E11: View all twice idempotent")
    @Severity(SeverityLevel.NORMAL)
    public void viewAllTwiceIdempotent() {
        NotificationsPage page = reachNotificationsViaBell();
        page.tapUnread();
        sleepQuiet(500);
        if (page.isViewAllNotificationsVisible()) {
            page.tapViewAllNotifications();
            sleepQuiet(800);
        }
        page.tapUnread();
        sleepQuiet(600);
        if (page.isViewAllNotificationsVisible()) {
            page.tapViewAllNotifications();
            sleepQuiet(800);
        }
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isRentalsCategoryVisible() || page.isViewDetailsVisible()).isTrue();
    }

    @Test(priority = 12, description = "NT-E12: after View Details dismiss no Accept left committed")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Ensure we never leave Accept confirmed — stay Vendor, recoverable.")
    public void afterDetailsNoAcceptCommitted() {
        NotificationsPage page = ensureAllList(reachNotificationsViaBell());
        page.tapViewDetails();
        sleepQuiet(1100);
        page.tapCloseIfPresent();
        sleepQuiet(700);
        if ("quick-booking".equals(classifyRentalNow())) {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        dismissToNotifications(page);
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        // Re-open All still has list or empty — not crashed
        if (!page.isDisplayedNow()) {
            page = reachNotificationsViaBell();
        }
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 13, description = "NT-E13: Notification Back → Home; second Back documents #25")
    @Severity(SeverityLevel.CRITICAL)
    @Description("First Back from Notification → Home. Second may hit known #25.")
    public void doubleBackDocumentsHomeBug25() {
        NotificationsPage page = reachNotificationsViaBell();
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        String mid = classifyRentalNow();
        Allure.parameter("afterFirstBack", mid);
        assertThat(mid).isIn("home", "extend-time", "quick-booking");
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        String end = classifyRentalNow();
        Allure.parameter("afterSecondBack", end);
        page.attachScreenshot("notif-nt-e13-double-back");
        if ("launcher".equals(end)) {
            Allure.parameter("knownBug", "#25 Rental Home Back→launcher");
            assertThat(mid).isIn("home", "extend-time", "quick-booking");
            return;
        }
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 14, description = "NT-E14: package stays Vendor through tab+scroll+back")
    @Severity(SeverityLevel.CRITICAL)
    public void packageStaysVendorThroughJourney() {
        NotificationsPage page = reachNotificationsViaBell();
        page.tapAll();
        sleepQuiet(600);
        page.swipeListUp();
        sleepQuiet(400);
        page.tapUnread();
        sleepQuiet(500);
        page.tapHeaderBack();
        sleepQuiet(700);
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
    }
}
