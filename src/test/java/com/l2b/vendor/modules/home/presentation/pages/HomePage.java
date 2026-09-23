package com.l2b.vendor.modules.home.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

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

    /**
     * Live 21 Sep {@code /tmp/l2b-rental-flow-0001-20260921/01-after-close}:
     * Calendar · Home · Earning · Fleet. Not material Orders / Inventory.
     */
    @Step("Check rental bottom tabs from 21 Sep dump")
    public boolean isRentalBottomTabsVisible() {
        return isCalendarTabVisible()
                && isHomeTabVisible()
                && isPresent(By.xpath("//*[@content-desc='Earning']"))
                && isFleetTabVisible();
    }

    @Step("Check material Orders tab is visible")
    public boolean isOrdersTabVisible() {
        return isPresent(By.xpath("//*[@content-desc='Orders']"));
    }

    @Step("Check material Inventory tab is visible")
    public boolean isInventoryTabVisible() {
        return isPresent(By.xpath("//*[@content-desc='Inventory']"));
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

    /**
     * Read the integer count printed on a stat card (e.g. the number on the
     * {@code Upcoming Booking} card). Heuristic: locate the label TextView in document
     * order, then return the nearest pure-integer TextView among its immediate neighbours
     * (the count sits just above/below the label). Returns {@code -1} if not found.
     */
    @Step("Read integer value on the {label} stat card")
    public int rentalStatValue(String label) {
        List<WebElement> all = driver.findElements(By.className("android.widget.TextView"));
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            String t = safeText(all.get(i));
            if (label.equals(t)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return -1;
        }
        int[] offsets = {-1, 1, -2, 2, -3, 3};
        for (int off : offsets) {
            int j = idx + off;
            if (j < 0 || j >= all.size()) {
                continue;
            }
            String t = safeText(all.get(j));
            if (t.matches("\\d{1,4}")) {
                return Integer.parseInt(t);
            }
        }
        return -1;
    }

    /**
     * Dump 21 Sep {@code 01-after-close} 2×2: Active Fleet, Total Completed Task,
     * Upcoming Booking, Earning Projected. Counts and rupees change — labels only.
     */
    @Step("Check rental Home stat cards from 21 Sep dump")
    public boolean areRentalStatCardsVisible() {
        return isStatVisible("Active Fleet")
                && isStatVisible("Total Completed Task")
                && isStatVisible("Upcoming Booking")
                && isStatVisible("Earning Projected");
    }

    /**
     * Dump 21 Sep {@code 01-after-close}: horizontal strip under Upcoming Booking
     * with a clickable card (machine + plate + Booking for). Second card is clipped.
     */
    @Step("Check Upcoming Booking strip has at least one card")
    public boolean isUpcomingStripCardVisible() {
        return isPresent(ComposeLocators.textView("Upcoming Booking"))
                && upcomingStripCardCount() >= 1
                && isPresent(ComposeLocators.textView("Booking for"));
    }

    @Step("Count clickable cards in the Upcoming Booking strip")
    public int upcomingStripCardCount() {
        return upcomingStripCards().size();
    }

    @Step("First Upcoming strip machine name")
    public String firstUpcomingMachineNow() {
        List<WebElement> texts = firstUpcomingCardTexts();
        for (WebElement el : texts) {
            String t = safeText(el);
            if (t.isBlank() || "Booking for".equals(t) || t.startsWith("See all")) {
                continue;
            }
            if (PLATE.matcher(t).matches() || t.contains(" day)")) {
                continue;
            }
            return t;
        }
        return "";
    }

    @Step("First Upcoming strip plate")
    public String firstUpcomingPlateNow() {
        for (WebElement el : firstUpcomingCardTexts()) {
            String t = safeText(el);
            if (PLATE.matcher(t).matches()) {
                return t;
            }
        }
        return "";
    }

    @Step("First Upcoming strip Booking-for address")
    public String firstUpcomingBookingForAddressNow() {
        List<WebElement> texts = firstUpcomingCardTexts();
        for (int i = 0; i < texts.size(); i++) {
            if ("Booking for".equals(safeText(texts.get(i))) && i + 1 < texts.size()) {
                return safeText(texts.get(i + 1));
            }
        }
        return "";
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

    @Step("Tap clickable node with content-desc, or clickable outer wrapping it")
    public void tapDesc(String desc) {
        tap(driver.findElement(By.xpath(
                "//*[@clickable='true'][@content-desc=" + xpathLit(desc) + "]"
                        + " | //android.view.View[@clickable='true'][.//*[@content-desc="
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

    @Step("Check Booking Orders feed (title, Timer, Decline, Accept) — do not tap actions")
    public boolean isBookingOrdersFeedVisible() {
        return isPresent(ComposeLocators.textView("Booking Orders"))
                && isPresent(ComposeLocators.textView("Timer"))
                && isPresent(ComposeLocators.textView("Decline"))
                && isPresent(ComposeLocators.textView("Accept"));
    }

    @Step("Visible Booking Orders timer values mm:ss")
    public List<String> bookingOrderTimersNow() {
        List<String> out = new java.util.ArrayList<>();
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el);
            if (TIMER.matcher(t).matches()) {
                out.add(t);
            }
        }
        return out;
    }

    @Step("Check View More Details is visible")
    public boolean isViewMoreDetailsVisible() {
        return isPresent(ComposeLocators.textView("View More Details"));
    }

    @Step("Count About this stat info icons")
    public int aboutThisStatCount() {
        return driver.findElements(By.xpath("//*[@content-desc='About this stat']")).size();
    }

    @Step("Count clickable See all rows")
    public int seeAllCount() {
        return seeAllRows().size();
    }

    @Step("Tap nth See all (0 Active Fleet, 1 Upcoming, 2 Booking Orders)")
    public void tapNthSeeAll(int index) {
        List<WebElement> rows = seeAllRows();
        if (index < 0 || index >= rows.size()) {
            throw new IllegalStateException("See all index " + index + " size " + rows.size());
        }
        tap(rows.get(index));
    }

    @Step("Tap first Upcoming strip card")
    public void tapFirstUpcomingStripCard() {
        List<WebElement> cards = upcomingStripCards();
        if (cards.isEmpty()) {
            throw new IllegalStateException("No Upcoming strip card");
        }
        tap(cards.get(0));
    }

    @Step("Swipe rental Upcoming strip")
    public void swipeRentalUpcomingStrip(String direction) {
        List<WebElement> strips = driver.findElements(By.xpath(
                "//android.view.View[@scrollable='true']"
                        + "[.//android.widget.TextView[@text='Booking for']]"
                        + "[not(.//android.widget.TextView[@text='Timer'])]"));
        if (strips.isEmpty()) {
            swipeUpcomingStripLeft();
            return;
        }
        org.openqa.selenium.Rectangle box = strips.get(0).getRect();
        // Inset from the left edge so the gesture is not Android Back (E1 / #25).
        int inset = Math.max(120, box.getWidth() / 5);
        driver.executeScript("mobile: swipeGesture", java.util.Map.of(
                "left", box.getX() + inset,
                "top", box.getY() + 16,
                "width", Math.max(box.getWidth() - inset * 2, 80),
                "height", Math.max(box.getHeight() - 32, 80),
                "direction", direction,
                "percent", 0.75));
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

    private static final Pattern PLATE = Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z]{1,3}[0-9]{4}");
    private static final Pattern TIMER = Pattern.compile("^\\d{2}:\\d{2}$");

    private static final By UPCOMING_STRIP_CARDS = By.xpath(
            "//android.view.View[@scrollable='true']"
                    + "[.//android.widget.TextView[@text='Booking for']]"
                    + "[not(.//android.widget.TextView[@text='Timer'])]"
                    + "[not(.//android.widget.TextView[@text='Booking Orders'])]"
                    + "/android.view.View[@clickable='true']");

    private List<WebElement> upcomingStripCards() {
        return driver.findElements(UPCOMING_STRIP_CARDS);
    }

    private List<WebElement> seeAllRows() {
        return driver.findElements(By.xpath(
                "//android.view.View[@clickable='true'][.//android.widget.TextView[@text='See all']]"
                        + " | //android.widget.TextView[@text='See all'][@clickable='true']"));
    }

    private List<WebElement> firstUpcomingCardTexts() {
        List<WebElement> cards = upcomingStripCards();
        if (cards.isEmpty()) {
            return List.of();
        }
        return cards.get(0).findElements(By.className("android.widget.TextView"));
    }

    private static String safeText(WebElement el) {
        String t = el.getText();
        return t == null ? "" : t.trim();
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
