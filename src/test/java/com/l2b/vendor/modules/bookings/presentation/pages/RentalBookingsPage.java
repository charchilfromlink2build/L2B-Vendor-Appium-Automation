package com.l2b.vendor.modules.bookings.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Rental Bookings list for {@code 9000000001} — reached from Home → Upcoming
 * Booking → {@code See all}. Read-only queries only: every locator below is taken
 * from the 21 Sep live dumps, and no state-changing action lives in this page yet.
 *
 * <p>Dump evidence ({@code /tmp/l2b-rental-flow-0001-20260921/}):
 * <ul>
 *   <li>{@code 11-see-all-upcoming}, {@code 14-bookings-upcoming} — Upcoming list</li>
 *   <li>{@code 12-bookings-active} — empty state {@code No active bookings.}</li>
 *   <li>{@code 13-bookings-completed} — completed card</li>
 *   <li>{@code 16-first-upcoming-card} — View More Details expanded</li>
 * </ul>
 *
 * <p>Screen identity is the tab triplet plus one of the three headers. Quick Booking
 * (app bar {@code Close}) and Home (greeting / Current Earning) must never satisfy it.
 *
 * <p>Accept, Decline, Assign, Change, and Confirm helpers are deliberately absent.
 * They are added only when the live rental booking seed exists, so no queue can be
 * consumed by accident before then.
 */
public class RentalBookingsPage extends SplashScreen {

    /** Header title per tab, from the dumps. */
    public static final String TITLE_UPCOMING = "Upcoming Booking";
    public static final String TITLE_ACTIVE = "Active Order";
    public static final String TITLE_COMPLETED = "Completed Order";

    public static final String TAB_UPCOMING = "Upcoming";
    public static final String TAB_ACTIVE = "Active";
    public static final String TAB_COMPLETED = "Completed";

    public static final String EMPTY_ACTIVE = "No active bookings.";

    private static final By BACK = By.xpath("//*[@content-desc='Back']");
    private static final By HELP = By.xpath("//android.widget.TextView[@text='Help']");
    private static final By MAP = By.xpath("//*[@content-desc='Google Map']");
    private static final By GET_DIRECTION = ComposeLocators.textView("Get Direction");
    private static final By VIEW_MORE_DETAILS = ComposeLocators.textView("View More Details");
    private static final By BOOKING_FOR = ComposeLocators.textView("Booking for");
    private static final By OPERATOR_UNASSIGNED = ComposeLocators.textView("Operator Not Assigned");
    private static final By ASSIGN = ComposeLocators.textView("Assign");
    private static final By CHANGE = ComposeLocators.textView("Change");
    private static final By OPERATOR_ASSIGNED =
            ComposeLocators.textViewContains("Operator : ");
    private static final By AMOUNT = ComposeLocators.textViewContains("Amount · Online Mode");
    private static final By SOURCE_TAG = ComposeLocators.textView("Quick Booking");

    /** {@code 27 Aug 11:36 AM - 29 Aug 2026 11:36 AM (2 days)} and the date-only variant. */
    private static final Pattern DATE_RANGE =
            Pattern.compile(".+ - .+ \\(\\d+ days?\\)");

    /** Fleet plate, same shape the Home upcoming strip uses. */
    private static final Pattern PLATE =
            Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z]{1,3}[0-9]{4}");

    @Override
    @Step("Wait for Rental Bookings list")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Rental Bookings list did not appear", Duration.ofSeconds(15));
    }

    @Step("Check Rental Bookings identity (three tabs + a bookings header)")
    public boolean isDisplayedNow() {
        if (isQuickBookingAppBar()) {
            return false;
        }
        return areTabsVisible() && !headerTitleNow().isEmpty();
    }

    @Step("Check Upcoming / Active / Completed tabs are visible")
    public boolean areTabsVisible() {
        return isPresent(ComposeLocators.textView(TAB_UPCOMING))
                && isPresent(ComposeLocators.textView(TAB_ACTIVE))
                && isPresent(ComposeLocators.textView(TAB_COMPLETED));
    }

    /** {@code Upcoming Booking} / {@code Active Order} / {@code Completed Order}, else empty. */
    @Step("Visible Bookings header title")
    public String headerTitleNow() {
        for (String title : new String[] {TITLE_UPCOMING, TITLE_ACTIVE, TITLE_COMPLETED}) {
            if (isPresent(ComposeLocators.textView(title))) {
                return title;
            }
        }
        return "";
    }

    @Step("Check Back affordance is visible")
    public boolean isBackVisible() {
        return isPresent(BACK);
    }

    @Step("Check Help affordance is visible")
    public boolean isHelpVisible() {
        return isPresent(HELP);
    }

    @Step("Check Active empty state copy")
    public boolean isActiveEmptyStateVisible() {
        return isPresent(ComposeLocators.textView(EMPTY_ACTIVE));
    }

    @Step("Count booking cards by amount rows")
    public int cardCountNow() {
        return driver.findElements(AMOUNT).size();
    }

    @Step("Count View More Details rows")
    public int viewMoreDetailsCount() {
        return driver.findElements(VIEW_MORE_DETAILS).size();
    }

    @Step("Count Get Direction controls")
    public int getDirectionCount() {
        return driver.findElements(GET_DIRECTION).size();
    }

    @Step("Count Google Map tiles")
    public int mapCount() {
        return driver.findElements(MAP).size();
    }

    @Step("Count Assign controls (unassigned operator)")
    public int assignCount() {
        return driver.findElements(ASSIGN).size();
    }

    @Step("Count Change controls (assigned operator)")
    public int changeCount() {
        return driver.findElements(CHANGE).size();
    }

    @Step("Count Operator Not Assigned rows")
    public int operatorUnassignedCount() {
        return driver.findElements(OPERATOR_UNASSIGNED).size();
    }

    @Step("Count assigned operator rows")
    public int operatorAssignedCount() {
        return driver.findElements(OPERATOR_ASSIGNED).size();
    }

    @Step("Count Booking for rows")
    public int bookingForCount() {
        return driver.findElements(BOOKING_FOR).size();
    }

    @Step("Count Quick Booking source tags")
    public int sourceTagCount() {
        return driver.findElements(SOURCE_TAG).size();
    }

    /** Amount rows as shown, e.g. {@code Amount · Online Mode ₹102,150}. */
    @Step("Visible amount rows")
    public List<String> amountsNow() {
        return textsOf(AMOUNT);
    }

    /** Completed cards show a bare rupee figure, e.g. {@code ₹1,02,000}. */
    @Step("Visible bare rupee figures")
    public List<String> rupeeFiguresNow() {
        List<String> out = new ArrayList<>();
        for (WebElement el : driver.findElements(ComposeLocators.textViewContains("₹"))) {
            String text = safeText(el);
            if (!text.isEmpty() && !text.startsWith("Amount")) {
                out.add(text);
            }
        }
        return out;
    }

    @Step("Visible date ranges")
    public List<String> dateRangesNow() {
        List<String> out = new ArrayList<>();
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String text = safeText(el);
            if (DATE_RANGE.matcher(text).matches()) {
                out.add(text);
            }
        }
        return out;
    }

    @Step("Visible plates")
    public List<String> platesNow() {
        List<String> out = new ArrayList<>();
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String text = safeText(el);
            if (PLATE.matcher(text).matches()) {
                out.add(text);
            }
        }
        return out;
    }

    /** Address line printed under each {@code Booking for} label, blank included. */
    @Step("Visible Booking-for addresses")
    public List<String> bookingForAddressesNow() {
        List<WebElement> all = driver.findElements(By.className("android.widget.TextView"));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if ("Booking for".equals(safeText(all.get(i))) && i + 1 < all.size()) {
                out.add(safeText(all.get(i + 1)));
            }
        }
        return out;
    }

    /** Machine titles, e.g. {@code Excavator 20 Tonnes}, {@code 14 Ft Truck}. */
    @Step("Visible machine titles")
    public List<String> machineTitlesNow() {
        List<String> out = new ArrayList<>();
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String text = safeText(el);
            if (text.isEmpty() || isChromeCopy(text)) {
                continue;
            }
            if (PLATE.matcher(text).matches() || DATE_RANGE.matcher(text).matches()) {
                continue;
            }
            if (text.startsWith("Amount") || text.startsWith("₹")
                    || text.startsWith("Operator")) {
                continue;
            }
            out.add(text);
        }
        return out;
    }

    private static boolean isChromeCopy(String text) {
        return TAB_UPCOMING.equals(text) || TAB_ACTIVE.equals(text) || TAB_COMPLETED.equals(text)
                || TITLE_UPCOMING.equals(text) || TITLE_ACTIVE.equals(text)
                || TITLE_COMPLETED.equals(text)
                || "Help".equals(text) || "Booking for".equals(text)
                || "Get Direction".equals(text) || "View More Details".equals(text)
                || "View Less Details".equals(text) || "Assign".equals(text)
                || "Change".equals(text) || "Quick Booking".equals(text)
                || EMPTY_ACTIVE.equals(text);
    }

    private List<String> textsOf(By locator) {
        List<String> out = new ArrayList<>();
        for (WebElement el : driver.findElements(locator)) {
            out.add(safeText(el));
        }
        return out;
    }

    private static String safeText(WebElement el) {
        String text = el.getText();
        return text == null ? "" : text.trim();
    }

    /** Quick Booking owns the {@code Close} app bar; Bookings must not be confused with it. */
    private boolean isQuickBookingAppBar() {
        return isPresent(By.xpath("//*[@content-desc='Close']"));
    }
}
