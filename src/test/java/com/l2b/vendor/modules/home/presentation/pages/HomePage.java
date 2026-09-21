package com.l2b.vendor.modules.home.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import org.openqa.selenium.By;

/**
 * Logged-in Home. Identity from live dumps:
 * <ul>
 *   <li>18 Sep rental 0003 {@code /tmp/l2b-qb-discovery/rental-individual-0003/window.xml}</li>
 *   <li>21 Sep material 0017 {@code /tmp/l2b-home-material-0017-20260921/00-landing/window.xml}
 *       — {@code Good Morning!}, {@code Current Earning}, bottom tabs
 *       {@code Orders} / {@code Home} / {@code Earning} / {@code Inventory}.
 *       No {@code Close} app bar. Greeting is time-of-day specific.</li>
 * </ul>
 * Quick Booking is a different screen and must not count as Home.
 */
public class HomePage extends SplashScreen {

    @Override
    @Step("Wait for Home")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Home screen did not appear", Duration.ofSeconds(15));
    }

    @Step("Check Home identity (greeting or Current Earning, not Quick Booking app bar)")
    public boolean isDisplayedNow() {
        if (isQuickBookingAppBar()) {
            return false;
        }
        return isPresent(ComposeLocators.textView("Current Earning"))
                || isPresent(ComposeLocators.textView("Good Morning!"))
                || isPresent(ComposeLocators.textView("Good Afternoon!"))
                || isPresent(ComposeLocators.textView("Good Evening!"));
    }

    @Step("Check Current Earning is visible")
    public boolean isCurrentEarningVisible() {
        return isPresent(ComposeLocators.textView("Current Earning"));
    }

    /** Dump-0 amount like {@code ₹ 1,66,469}. Value can change; keep the rupee prefix. */
    @Step("Visible Current Earning rupee amount")
    public String currentEarningAmountNow() {
        var amounts = driver.findElements(ComposeLocators.textViewContains("₹"));
        if (amounts.isEmpty()) {
            return "";
        }
        return amounts.get(0).getText();
    }

    @Step("Check Current Earning percent / from-date line")
    public boolean isCurrentEarningTrendVisible() {
        return isPresent(ComposeLocators.textViewContains("%"))
                && isPresent(ComposeLocators.textViewContains("from"));
    }

    /**
     * Dump-0 / 0003 greetings. Time-of-day specific — do not pin one string.
     */
    @Step("Visible Home greeting from dump set")
    public String greetingNow() {
        for (String copy : new String[] {"Good Morning!", "Good Afternoon!", "Good Evening!"}) {
            if (isPresent(ComposeLocators.textView(copy))) {
                return copy;
            }
        }
        return "";
    }

    @Step("Check a dump greeting is visible")
    public boolean isGreetingVisible() {
        return !greetingNow().isEmpty();
    }

    @Step("Check Home bottom tab is visible")
    public boolean isHomeTabVisible() {
        return isPresent(By.xpath("//*[@content-desc='Home']"));
    }

    /** Dump-0 0017 bar: Orders · Home · Earning · Inventory. */
    @Step("Check material bottom tabs from Dump-0")
    public boolean isMaterialBottomTabsVisible() {
        return isPresent(By.xpath("//*[@content-desc='Orders']"))
                && isPresent(By.xpath("//*[@content-desc='Home']"))
                && isPresent(By.xpath("//*[@content-desc='Earning']"))
                && isPresent(By.xpath("//*[@content-desc='Inventory']"));
    }

    @Step("Check rental Fleet tab is visible")
    public boolean isFleetTabVisible() {
        return isPresent(By.xpath("//*[@content-desc='Fleet']"))
                || isPresent(ComposeLocators.textView("Fleet"));
    }

    @Step("Check rental Calendar tab is visible")
    public boolean isCalendarTabVisible() {
        return isPresent(By.xpath("//*[@content-desc='Calendar']"))
                || isPresent(ComposeLocators.textView("Calendar"));
    }

    @Step("Check Upcoming Orders material copy")
    public boolean isUpcomingOrdersVisible() {
        return isPresent(ComposeLocators.textView("Upcoming Orders"));
    }

    @Step("Check Profile affordance is visible")
    public boolean isProfileVisible() {
        return isPresent(By.xpath("//*[@content-desc='Profile']"));
    }

    @Step("Check Notifications bell is visible")
    public boolean isNotificationsVisible() {
        return isPresent(By.xpath("//*[@content-desc='Notifications']"));
    }

    @Step("Check Monthly / Select period is visible")
    public boolean isPeriodFilterVisible() {
        return isPresent(ComposeLocators.textView("Monthly"))
                || isPresent(By.xpath("//*[@content-desc='Select period']"));
    }

    @Step("Check See all is visible")
    public boolean isSeeAllVisible() {
        return isPresent(ComposeLocators.textView("See all"));
    }

    @Step("Check mid-page empty Orders copy")
    public boolean isEmptyOrdersCopyVisible() {
        return isPresent(ComposeLocators.textView("No orders right now."));
    }

    @Step("Check stat card label")
    public boolean isStatVisible(String label) {
        return isPresent(ComposeLocators.textView(label));
    }

    @Step("Check Confirmed upcoming card copy")
    public boolean isUpcomingConfirmedCardVisible() {
        return isPresent(ComposeLocators.textViewContains("Confirmed"))
                && isPresent(ComposeLocators.textViewContains("Pickup scheduled"));
    }

    @Step("Check Close / Quick Booking overlay is absent")
    public boolean isQuickBookingOverlayVisible() {
        return isQuickBookingAppBar();
    }

    @Step("Tap clickable outer wrapping content-desc")
    public void tapDesc(String desc) {
        tap(driver.findElement(By.xpath(
                "//android.view.View[@clickable='true'][.//*[@content-desc="
                        + xpathLit(desc) + "]]")));
    }

    @Step("Tap clickable outer wrapping text")
    public void tapText(String text) {
        tap(driver.findElement(ComposeLocators.clickableWithText(text)));
    }

    @Step("Tap Current Earning sparkline ImageView")
    public void tapCurrentEarningSparkline() {
        tap(driver.findElement(By.xpath(
                "//android.widget.ImageView[@content-desc='Current Earning'][@clickable='true']")));
    }

    @Step("Tap first upcoming Confirmed card")
    public void tapFirstUpcomingCard() {
        tap(driver.findElement(By.xpath(
                "//android.view.View[@clickable='true']"
                        + "[.//android.widget.TextView[contains(@text,'Pickup scheduled')]]")));
    }

    @Step("Swipe Upcoming Orders strip sideways")
    public void swipeUpcomingStripLeft() {
        driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                "left", 80,
                "top", 1120,
                "width", 900,
                "height", 300,
                "direction", "left",
                "percent", 0.8));
    }

    @Step("Swipe Home body upward")
    public void swipeHomeUp() {
        org.openqa.selenium.Dimension size = driver.manage().window().getSize();
        driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                "left", (int) (size.width * 0.15),
                "top", (int) (size.height * 0.30),
                "width", (int) (size.width * 0.70),
                "height", (int) (size.height * 0.40),
                "direction", "up",
                "percent", 0.7));
    }

    private static String xpathLit(String value) {
        return "'" + value + "'";
    }

    /**
     * Quick Booking app bar only. Home booking-order cards can reuse the words
     * {@code Quick Booking}, so that string is not a screen identity.
     */
    private boolean isQuickBookingAppBar() {
        return isPresent(By.xpath("//*[@content-desc='Close']"));
    }
}
