package com.l2b.vendor.modules.home.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/**
 * Home interactions for {@code 9000000017}. Dump-0 21 Sep. Confirm tap destination only —
 * do not deep-test Bookings / Earning / Inventory. Isolated {@code home/home-interact.xml}.
 */
@Epic("Vendor app")
@Feature("Home interact — material vendor 9000000017")
public class HomeInteractTest extends HomeBaseTest {

    @Test(priority = 1, description = "H-I1: Monthly period filter opens options")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap Monthly / Select period. Record sheet options and whether amount changes.")
    public void monthlyPeriodFilter() {
        HomePage home = loginToHomeOrQueue();
        String before = home.currentEarningAmountNow();
        home.tapDesc("Select period");
        String src = DriverManager.get().getPageSource();
        boolean weekly = src.contains("Weekly") || src.contains("Week");
        boolean yearly = src.contains("Yearly") || src.contains("Year");
        boolean daily = src.contains("Daily") || src.contains("Today");
        Allure.parameter("amountBefore", before);
        Allure.parameter("weekly", String.valueOf(weekly));
        Allure.parameter("yearly", String.valueOf(yearly));
        Allure.parameter("daily", String.valueOf(daily));
        Allure.parameter("stillHome", String.valueOf(home.isDisplayedNow()));
        home.attachScreenshot("home-hi1-period");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(weekly || yearly || daily || home.isPeriodFilterVisible())
                .as("Period tap must open options or stay on the filter")
                .isTrue();
    }

    @Test(priority = 2, description = "H-I2: tap Current Earning label area — document destination")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump-0 label is not clickable; try tap. Record stay vs navigate. No Earning deep-test.")
    public void tapCurrentEarningTile() {
        HomePage home = loginToHomeOrQueue();
        tapTextSafe("Current Earning");
        String named = namedAfterTap(home, new QuickBookingPage());
        Allure.parameter("after", named);
        Allure.parameter("stillEarningTile", String.valueOf(home.isCurrentEarningVisible()));
        home.attachScreenshot("home-hi2-earning-tile");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(named).as("Must not crash").isNotEqualTo("otp");
    }

    @Test(priority = 3, description = "H-I3: tap Upcoming Orders stat — document destination")
    @Severity(SeverityLevel.NORMAL)
    public void tapUpcomingOrdersStat() {
        documentStatTap("Upcoming Orders", "home-hi3-upcoming-stat");
    }

    @Test(priority = 4, description = "H-I4: tap Earning Orders stat — document destination")
    @Severity(SeverityLevel.NORMAL)
    public void tapEarningOrdersStat() {
        documentStatTap("Earning Orders", "home-hi4-earning-stat");
    }

    @Test(priority = 5, description = "H-I5: tap Completed Orders stat — document destination")
    @Severity(SeverityLevel.NORMAL)
    public void tapCompletedOrdersStat() {
        documentStatTap("Completed Orders", "home-hi5-completed-stat");
    }

    @Test(priority = 6, description = "H-I6: tap Disputes Orders stat — document destination")
    @Severity(SeverityLevel.NORMAL)
    public void tapDisputesOrdersStat() {
        documentStatTap("Disputes Orders", "home-hi6-disputes-stat");
    }

    @Test(priority = 7, description = "H-I7: tap first Upcoming Confirmed card")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-0 clickable card Pickup scheduled 26 Aug. Record destination. No deep-test.")
    public void tapFirstUpcomingCard() {
        HomePage home = loginToHomeOrQueue();
        home.tapFirstUpcomingCard();
        String named = namedAfterTap(home, new QuickBookingPage());
        Allure.parameter("after", named);
        Allure.parameter("stillHome", String.valueOf(home.isDisplayedNow()));
        home.attachScreenshot("home-hi7-upcoming-card");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 8, description = "H-I8: horizontal swipe on Upcoming Orders strip")
    @Severity(SeverityLevel.NORMAL)
    public void swipeUpcomingStrip() {
        HomePage home = loginToHomeOrQueue();
        boolean before26 = pageHas("26 Aug 2026");
        boolean before28 = pageHas("28 Aug 2026");
        home.swipeUpcomingStripLeft();
        Allure.parameter("before26", String.valueOf(before26));
        Allure.parameter("before28", String.valueOf(before28));
        Allure.parameter("after26", String.valueOf(pageHas("26 Aug 2026")));
        Allure.parameter("after28", String.valueOf(pageHas("28 Aug 2026")));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        home.attachScreenshot("home-hi8-h-scroll");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow() || home.isMaterialBottomTabsVisible()).isTrue();
    }

    @Test(priority = 9, description = "H-I9: tap Orders tab — leaves Home, no deep-test")
    @Severity(SeverityLevel.CRITICAL)
    public void tapOrdersTab() {
        tapTabAndRecord("Orders", "home-hi9-orders-tab");
    }

    @Test(priority = 10, description = "H-I9b: tap Earning tab — leaves Home, no deep-test")
    @Severity(SeverityLevel.CRITICAL)
    public void tapEarningTab() {
        tapTabAndRecord("Earning", "home-hi9-earning-tab");
    }

    @Test(priority = 11, description = "H-I9c: tap Inventory tab — leaves Home, no deep-test")
    @Severity(SeverityLevel.CRITICAL)
    public void tapInventoryTab() {
        tapTabAndRecord("Inventory", "home-hi9-inventory-tab");
    }

    @Test(priority = 12, description = "H-I10: tap Profile — opens account drawer")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-0 Profile clickable outer. Expect Account + Logout drawer. Do not tap Logout.")
    public void tapProfileOpensDrawer() {
        HomePage home = loginToHomeOrQueue();
        home.tapDesc("Profile");
        boolean drawer = profileDrawerNow();
        Allure.parameter("drawer", String.valueOf(drawer));
        Allure.parameter("account", String.valueOf(pageHas("Account")));
        home.attachScreenshot("home-hi10-profile");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(drawer || pageHas("Account"))
                .as("Profile tap must open Account drawer")
                .isTrue();
    }

    @Test(priority = 13, description = "H-I11: tap Notifications bell")
    @Severity(SeverityLevel.CRITICAL)
    public void tapNotifications() {
        HomePage home = loginToHomeOrQueue();
        home.tapDesc("Notifications");
        String named = namedAfterTap(home, new QuickBookingPage());
        Allure.parameter("after", named);
        Allure.parameter("package", String.valueOf(vendorPackage()));
        home.attachScreenshot("home-hi11-notifications");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 14, description = "H-I12: pull-to-refresh — confirm exists or does not")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump-0 has no vertical Refresh. Swipe down from header. Record copy vs no-op.")
    public void pullToRefreshOrAbsent() {
        HomePage home = loginToHomeOrQueue();
        org.openqa.selenium.Dimension size = DriverManager.get().manage().window().getSize();
        DriverManager.get().executeScript("mobile: swipeGesture", java.util.Map.of(
                "left", (int) (size.width * 0.2),
                "top", (int) (size.height * 0.18),
                "width", (int) (size.width * 0.6),
                "height", (int) (size.height * 0.25),
                "direction", "down",
                "percent", 0.8));
        String src = DriverManager.get().getPageSource().toLowerCase();
        boolean refreshCopy = src.contains("refresh") || src.contains("updating") || src.contains("loading");
        Allure.parameter("refreshCopy", String.valueOf(refreshCopy));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        home.attachScreenshot("home-hi12-ptr");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow() || home.isMaterialBottomTabsVisible()).isTrue();
    }

    @Test(priority = 15, description = "H-I13: tap See all")
    @Severity(SeverityLevel.CRITICAL)
    public void tapSeeAll() {
        HomePage home = loginToHomeOrQueue();
        home.tapText("See all");
        String named = namedAfterTap(home, new QuickBookingPage());
        Allure.parameter("after", named);
        home.attachScreenshot("home-hi13-see-all");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 16, description = "H-I14: tap Current Earning sparkline ImageView")
    @Severity(SeverityLevel.NORMAL)
    public void tapEarningSparkline() {
        HomePage home = loginToHomeOrQueue();
        home.tapCurrentEarningSparkline();
        String named = namedAfterTap(home, new QuickBookingPage());
        Allure.parameter("after", named);
        home.attachScreenshot("home-hi14-sparkline");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 17, description = "H-I15: tap empty Orders copy — document destination")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump-0 No orders right now. is display copy. Tap and record stay vs navigate.")
    public void tapEmptyOrdersCopy() {
        HomePage home = loginToHomeOrQueue();
        tapTextSafe("No orders right now.");
        String named = namedAfterTap(home, new QuickBookingPage());
        Allure.parameter("after", named);
        Allure.parameter("emptyStill", String.valueOf(home.isEmptyOrdersCopyVisible()));
        home.attachScreenshot("home-hi15-empty-orders");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    private void documentStatTap(String label, String shot) {
        HomePage home = loginToHomeOrQueue();
        tapTextSafe(label);
        String named = namedAfterTap(home, new QuickBookingPage());
        Allure.parameter("label", label);
        Allure.parameter("after", named);
        Allure.parameter("dumpClickable", "false on stat card parent");
        home.attachScreenshot(shot);
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    private void tapTabAndRecord(String tab, String shot) {
        HomePage home = loginToHomeOrQueue();
        home.tapDesc(tab);
        boolean stillHomeTile = home.isCurrentEarningVisible();
        Allure.parameter("tab", tab);
        Allure.parameter("currentEarningStill", String.valueOf(stillHomeTile));
        Allure.parameter("tabsStill", String.valueOf(home.isMaterialBottomTabsVisible()));
        home.attachScreenshot(shot);
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isMaterialBottomTabsVisible()).as("Bottom bar survives").isTrue();
        assertThat(stillHomeTile)
                .as(tab + " tab should leave the Home earning tile")
                .isFalse();
    }

    private void tapTextSafe(String text) {
        try {
            WebElement el = DriverManager.get().findElement(ComposeLocators.textView(text));
            el.click();
        } catch (RuntimeException e) {
            Allure.parameter("tapSafe", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static boolean pageHas(String fragment) {
        return DriverManager.get().getPageSource().contains(fragment);
    }
}
