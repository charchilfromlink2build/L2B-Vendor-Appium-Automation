package com.l2b.vendor.modules.bookings.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

/**
 * Rental Bookings surfaces for {@code 9000000001}. Read queries plus mutation helpers
 * for Accept / Decline / Assign / Change / Confirm. Locators are dump-sourced.
 *
 * <p>Dump evidence ({@code /tmp/l2b-rental-flow-0001-20260921/} + free-op Assign sheet):
 * <ul>
 *   <li>{@code 11-see-all-upcoming}, {@code 14-bookings-upcoming}, {@code 16-first-upcoming-card}
 *       — Upcoming list: {@code Assign} / {@code Change} are clickable TextViews
 *       ({@code clickable=true} on the label itself, not an outer View)</li>
 *   <li>{@code 17-see-all-booking-orders} — Booking Orders list: Accept / Decline use the
 *       Compose clickable outer View pattern (inner TextView is {@code clickable=false}
 *       decoy). These CTAs are <b>not</b> on Upcoming / Active / Completed tabs</li>
 *   <li>{@code /tmp/l2b-free-op-ui/12-assign-machine} — Assign machine sheet: title,
 *       Skip (clickable TextView), Select an operator, Confirm enabled on outer View</li>
 * </ul>
 *
 * <p>Screen identity for the tabbed list is the tab triplet plus one of the three headers.
 * Quick Booking (app bar {@code Close}) must never satisfy tabbed identity. Accept /
 * Decline helpers refuse when the extend-time dialog (#15) is up, and refuse when those
 * labels are absent (do not invent taps on the tabbed list).
 *
 * <p>Change opens the same Assign machine sheet family in product (operator pick + Confirm).
 * Sheet chrome is asserted the same way as after Assign; a dedicated Change-sheet dump
 * was not captured yet — callers should dump on first live Change if the title differs.
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
    /** Dump 14: Assign is a clickable TextView, not an outer View. */
    private static final By ASSIGN_CLICKABLE = By.xpath(
            "//android.widget.TextView[@text='Assign'][@clickable='true']");
    /** Dump 14: Change is a clickable TextView, not an outer View. */
    private static final By CHANGE_CLICKABLE = By.xpath(
            "//android.widget.TextView[@text='Change'][@clickable='true']");
    private static final By OPERATOR_ASSIGNED =
            ComposeLocators.textViewContains("Operator : ");
    /** Amount row prefix is payment-mode agnostic: {@code Amount · Online Mode ₹...} and
     * {@code Amount · Cash on Delivery ₹...} both start with {@code Amount ·}. */
    private static final By AMOUNT = ComposeLocators.textViewContains("Amount \u00b7");
    private static final By SOURCE_TAG = ComposeLocators.textView("Quick Booking");
    private static final By ASSIGN_MACHINE = ComposeLocators.textView("Assign machine");
    private static final By ASSIGN_OPERATOR = ComposeLocators.textView("Assign Operator");
    private static final By SELECT_OPERATOR = ComposeLocators.textView("Select an operator");
    private static final By DECLINE_BOOKING = ComposeLocators.textView("Decline Booking?");
    private static final By EXTEND_TIME = ComposeLocators.textView("Request to extend time");

    /** {@code 27 Aug 11:36 AM - 29 Aug 2026 11:36 AM (2 days)} and the date-only variant. */
    private static final Pattern DATE_RANGE =
            Pattern.compile(".+ - .+ \\(\\d+ days?\\)");

    /** Fleet plate, same shape the Home upcoming strip uses. */
    private static final Pattern PLATE =
            Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z]{1,3}[0-9]{4}");

    private boolean lastMenuOptionEnabled = true;

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

    @Step("Tap header Back")
    public void tapBack() {
        refuseIfExtendTime("Back");
        List<WebElement> rows = driver.findElements(BACK);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Back affordance not on Bookings — refusing tap");
        }
        clickGestureOn(rows.get(0));
    }

    @Step("Check Help affordance is visible")
    public boolean isHelpVisible() {
        return isPresent(HELP);
    }

    @Step("Tap header Help")
    public void tapHelp() {
        refuseIfExtendTime("Help");
        List<WebElement> rows = driver.findElements(HELP);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Help not on Bookings — refusing tap");
        }
        clickGestureOn(clickableAncestorOrSelf(rows.get(0)));
    }

    @Step("Check Active empty state copy")
    public boolean isActiveEmptyStateVisible() {
        return isPresent(ComposeLocators.textView(EMPTY_ACTIVE));
    }

    /**
     * Tap a top tab (Upcoming / Active / Completed) and wait for the header to switch.
     * Navigation only — no booking state changes. Refuses if extend-time (#15) is up.
     */
    @Step("Tap the {tab} tab")
    public void tapTab(String tab) {
        refuseIfExtendTime("tab " + tab);
        String expectedHeader = headerForTab(tab);
        WebElement el = smallestClickableWithText(tab);
        if (el == null) {
            List<WebElement> labels = driver.findElements(ComposeLocators.textView(tab));
            if (labels.isEmpty()) {
                throw new IllegalStateException("Tab not on screen: " + tab);
            }
            el = clickableAncestorOrSelf(labels.get(0));
        }
        clickGestureOn(el);
        Waits.until(driver,
                d -> expectedHeader.equals(headerTitleNow()) ? Boolean.TRUE : null,
                "Header did not switch to " + expectedHeader + " after tapping " + tab,
                Duration.ofSeconds(10));
    }

    private static String headerForTab(String tab) {
        switch (tab) {
            case TAB_UPCOMING:
                return TITLE_UPCOMING;
            case TAB_ACTIVE:
                return TITLE_ACTIVE;
            case TAB_COMPLETED:
                return TITLE_COMPLETED;
            default:
                throw new IllegalArgumentException("Unknown tab: " + tab);
        }
    }

    @Step("Count booking cards by amount rows")
    public int cardCountNow() {
        return driver.findElements(AMOUNT).size();
    }

    /**
     * Scroll the list top-to-bottom and count DISTINCT cards. A LazyColumn only renders the
     * visible window, so {@link #cardCountNow()} alone undercounts. Cards are de-duplicated by
     * their amount row plus the date range printed just below it. Stops when a swipe reveals no
     * new card key or after {@code maxSwipes}.
     */
    @Step("Count distinct cards by scrolling the list")
    public int distinctCardCountByScrolling(int maxSwipes) {
        Set<String> keys = new LinkedHashSet<>();
        collectCardKeysInto(keys);
        for (int i = 0; i < maxSwipes; i++) {
            int before = keys.size();
            swipeListUp();
            try {
                Thread.sleep(600);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            collectCardKeysInto(keys);
            if (keys.size() == before) {
                break;
            }
        }
        return keys.size();
    }

    private void collectCardKeysInto(Set<String> keys) {
        List<WebElement> all = driver.findElements(By.className("android.widget.TextView"));
        List<String> texts = new ArrayList<>();
        for (WebElement el : all) {
            texts.add(safeText(el));
        }
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i).startsWith("Amount \u00b7")) {
                String amount = texts.get(i);
                String dr = "";
                for (int j = i + 1; j < Math.min(i + 6, texts.size()); j++) {
                    if (DATE_RANGE.matcher(texts.get(j)).matches()) {
                        dr = texts.get(j);
                        break;
                    }
                }
                keys.add(amount + "|" + dr);
            }
        }
    }

    /** Scroll the current tab top-to-bottom collecting every well-formed plate seen. */
    @Step("Collect plates across the whole list by scrolling")
    public Set<String> collectPlatesByScrolling(int maxSwipes) {
        Set<String> plates = new LinkedHashSet<>(platesNow());
        for (int i = 0; i < maxSwipes; i++) {
            int before = plates.size();
            swipeListUp();
            try {
                Thread.sleep(600);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            plates.addAll(platesNow());
            // Keep scrolling a couple of extra times even if no new plate, in case a screenful
            // had no plate (unassigned card). Stop only after two barren swipes.
            if (plates.size() == before && i > 0) {
                break;
            }
        }
        return plates;
    }

    /** Scroll the list body down by one screenful (content moves up). */
    @Step("Swipe the list up (scroll down one screenful)")
    public void swipeListUp() {
        Dimension size = driver.manage().window().getSize();
        driver.executeScript("mobile: swipeGesture", Map.of(
                "left", size.width / 2 - 5,
                "top", (int) (size.height * 0.30),
                "width", 10,
                "height", (int) (size.height * 0.45),
                "direction", "up",
                "percent", 0.85));
    }

    @Step("Count View More Details rows")
    public int viewMoreDetailsCount() {
        return driver.findElements(VIEW_MORE_DETAILS).size();
    }

    @Step("Count View Less Details rows")
    public int viewLessDetailsCount() {
        return driver.findElements(ComposeLocators.textView("View Less Details")).size();
    }

    /** 0-based index among visible {@code View More Details} rows. */
    @Step("Tap View More Details at index {index}")
    public void tapViewMoreDetailsAt(int index) {
        refuseIfExtendTime("View More Details");
        List<WebElement> rows = driver.findElements(VIEW_MORE_DETAILS);
        if (index < 0 || index >= rows.size()) {
            throw new IllegalStateException(
                    "View More Details index " + index + " out of range (count=" + rows.size() + ")");
        }
        clickGestureOn(rows.get(index));
        Waits.until(driver,
                d -> viewLessDetailsCount() > 0 ? Boolean.TRUE : null,
                "View Less Details did not appear after View More",
                Duration.ofSeconds(8));
    }

    @Step("Tap first View Less Details")
    public void tapFirstViewLessDetails() {
        refuseIfExtendTime("View Less Details");
        List<WebElement> rows = driver.findElements(ComposeLocators.textView("View Less Details"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("View Less Details not on screen — refusing tap");
        }
        clickGestureOn(rows.get(0));
        Waits.until(driver,
                d -> viewLessDetailsCount() == 0 ? Boolean.TRUE : null,
                "View Less Details stayed after collapse",
                Duration.ofSeconds(8));
    }

    @Step("Count Get Direction controls")
    public int getDirectionCount() {
        return driver.findElements(GET_DIRECTION).size();
    }

    @Step("Tap first Get Direction (clickable outer View)")
    public void tapFirstGetDirection() {
        refuseIfExtendTime("Get Direction");
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Get Direction"));
        if (rows.isEmpty()) {
            List<WebElement> labels = driver.findElements(GET_DIRECTION);
            if (labels.isEmpty()) {
                throw new IllegalStateException("Get Direction not on screen — refusing tap");
            }
            clickGestureOn(clickableAncestorOrSelf(labels.get(0)));
            return;
        }
        clickGestureOn(rows.get(0));
    }

    @Step("Tap first Google Map tile")
    public void tapFirstMapTile() {
        refuseIfExtendTime("Google Map");
        List<WebElement> rows = driver.findElements(MAP);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Google Map tile not on screen — refusing tap");
        }
        clickGestureOn(rows.get(0));
    }

    @Step("Count Google Map tiles")
    public int mapCount() {
        return driver.findElements(MAP).size();
    }

    @Step("Count Assign controls (unassigned operator)")
    public int assignCount() {
        return driver.findElements(ASSIGN_CLICKABLE).size();
    }

    @Step("Count Change controls (assigned operator)")
    public int changeCount() {
        return driver.findElements(CHANGE_CLICKABLE).size();
    }

    @Step("Count Operator Not Assigned rows")
    public int operatorUnassignedCount() {
        return driver.findElements(OPERATOR_UNASSIGNED).size();
    }

    @Step("Count assigned operator rows")
    public int operatorAssignedCount() {
        return driver.findElements(OPERATOR_ASSIGNED).size();
    }

    /** Labels like {@code Operator : Nauman Majid Pathan} (prefix stripped for callers that want the name). */
    @Step("Visible assigned operator rows")
    public List<String> assignedOperatorRowsNow() {
        return textsOf(OPERATOR_ASSIGNED);
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

    // --- Mutation helpers (Step 1 foundation) ---------------------------------

    /**
     * Dump 17 Booking Orders / Home Booking Orders card: Accept lives on the clickable
     * outer View. Not present on Upcoming / Active / Completed tabs (dumps 11/14/16).
     */
    @Step("Tap first Accept (Booking Orders / incoming card outer View)")
    public void tapFirstAccept() {
        refuseIfExtendTime("Accept");
        if (isAssignMachineVisible() || isDeclineBookingDialogVisible()) {
            throw new IllegalStateException("A sheet/dialog is open — refusing Accept");
        }
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Accept"));
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "Accept not on screen — Booking Orders / Quick Booking card required "
                            + "(Upcoming/Active/Completed tabs have Assign/Change only)");
        }
        clickGestureOn(rows.get(0));
    }

    /**
     * Dump 17: Decline outer View, same Compose pattern as Accept. Not on tabbed Bookings.
     */
    @Step("Tap first Decline (Booking Orders / incoming card outer View)")
    public void tapFirstDecline() {
        refuseIfExtendTime("Decline");
        if (isAssignMachineVisible() || isDeclineBookingDialogVisible()) {
            throw new IllegalStateException("A sheet/dialog is open — refusing Decline");
        }
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Decline"));
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "Decline not on screen — Booking Orders / Quick Booking card required "
                            + "(Upcoming/Active/Completed tabs have Assign/Change only)");
        }
        clickGestureOn(rows.get(0));
    }

    @Step("Read first Accept enabled on clickable outer View")
    public boolean isAcceptEnabled() {
        return anyOuterEnabled("Accept");
    }

    @Step("Read first Decline enabled on clickable outer View")
    public boolean isDeclineEnabled() {
        return anyOuterEnabled("Decline");
    }

    @Step("Count Accept outer Views")
    public int acceptCount() {
        return driver.findElements(ComposeLocators.clickableWithText("Accept")).size();
    }

    @Step("Count Decline outer Views")
    public int declineCount() {
        return driver.findElements(ComposeLocators.clickableWithText("Decline")).size();
    }

    /**
     * Dump 14: {@code Assign} TextView clickable=true on Operator Not Assigned cards.
     * Opens Assign machine (same sheet family as Quick Booking Accept).
     */
    @Step("Tap first Assign on Upcoming (clickable TextView)")
    public void tapFirstAssign() {
        refuseIfExtendTime("Assign");
        if (isAssignMachineVisible()) {
            throw new IllegalStateException("Assign machine already open — refusing Assign");
        }
        List<WebElement> rows = driver.findElements(ASSIGN_CLICKABLE);
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "Assign not on screen — need an Operator Not Assigned Upcoming card");
        }
        clickGestureOn(rows.get(0));
    }

    /**
     * Dump 14: {@code Change} TextView clickable=true beside {@code Operator : <name>}.
     */
    @Step("Tap first Change on Upcoming (clickable TextView)")
    public void tapFirstChange() {
        refuseIfExtendTime("Change");
        if (isAssignMachineVisible()) {
            throw new IllegalStateException("Assign machine already open — refusing Change");
        }
        List<WebElement> rows = driver.findElements(CHANGE_CLICKABLE);
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "Change not on screen — need an assigned-operator Upcoming card");
        }
        clickGestureOn(rows.get(0));
    }

    @Step("Check Assign machine / Assign Operator sheet")
    public boolean isAssignMachineVisible() {
        return isPresent(ASSIGN_MACHINE) || isPresent(ASSIGN_OPERATOR);
    }

    @Step("Check Select an operator on Assign machine")
    public boolean isSelectOperatorVisible() {
        return isPresent(SELECT_OPERATOR);
    }

    @Step("Check Assign machine Skip")
    public boolean isAssignSkipVisible() {
        return isPresent(ComposeLocators.textView("Skip"));
    }

    /**
     * Free-op dump 12: Confirm enabled lives on the clickable outer View; inner TextView
     * stays enabled=true decoy.
     */
    @Step("Read Assign Confirm enabled on clickable outer View")
    public boolean isAssignConfirmEnabled() {
        return anyOuterEnabled("Confirm");
    }

    @Step("Tap Assign machine Confirm via clickGesture")
    public void tapAssignConfirm() {
        dismissDropdownOverlay();
        if (!isAssignMachineVisible()) {
            throw new IllegalStateException("Assign machine not on screen — refusing Confirm");
        }
        WebElement target = smallestClickableWithText("Confirm");
        if (target == null) {
            throw new IllegalStateException("Confirm not on Assign machine");
        }
        clickGestureOn(target);
    }

    /** Free-op dump 12: Skip is a clickable TextView on the sheet header. */
    @Step("Tap Assign machine Skip")
    public void tapAssignSkip() {
        if (!isAssignMachineVisible()) {
            throw new IllegalStateException("Assign machine not on screen — refusing Skip");
        }
        List<WebElement> skips = driver.findElements(By.xpath(
                "//android.widget.TextView[@text='Skip'][@clickable='true']"));
        if (skips.isEmpty()) {
            WebElement fallback = smallestClickableWithText("Skip");
            if (fallback == null) {
                throw new IllegalStateException("Skip not on Assign machine");
            }
            clickGestureOn(fallback);
            return;
        }
        clickGestureOn(skips.get(0));
    }

    /**
     * Opens Select an operator and taps the first new menu row. Returns the chosen label.
     */
    @Step("Select first operator from Assign machine dropdown")
    public String selectFirstOperator() {
        if (!isAssignMachineVisible()) {
            throw new IllegalStateException("Assign machine not on screen — refusing operator pick");
        }
        String chosen = pickFirstDropdownOption("Select an operator", "Operator dropdown showed no options");
        dismissDropdownOverlay();
        return chosen;
    }

    /**
     * Prefer an operator row marked {@code Available} (inline Assign Operator list). Falls back
     * to the Select an operator dropdown when no Available label is on screen.
     */
    @Step("Select an Available operator on Assign sheet")
    public String selectAvailableOperator() {
        if (!isAssignMachineVisible()) {
            throw new IllegalStateException("Assign machine not on screen — refusing operator pick");
        }
        List<WebElement> available = driver.findElements(ComposeLocators.textView("Available"));
        if (!available.isEmpty()) {
            // Prefer a known free primary operator when listed.
            for (String name : new String[] {"Randanberno Ezung", "Randanberno"}) {
                List<WebElement> named = driver.findElements(ComposeLocators.textViewContains(name));
                if (!named.isEmpty()) {
                    clickGestureOn(clickableAncestorOrSelf(named.get(0)));
                    lastMenuOptionEnabled = true;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return safeText(named.get(0));
                }
            }
            clickGestureOn(clickableAncestorOrSelf(available.get(0)));
            lastMenuOptionEnabled = true;
            return "Available";
        }
        return selectFirstOperator();
    }

    @Step("Read whether the last dropdown row was enabled")
    public boolean lastMenuOptionEnabled() {
        return lastMenuOptionEnabled;
    }

    @Step("Check Decline Booking? reason dialog")
    public boolean isDeclineBookingDialogVisible() {
        return isPresent(DECLINE_BOOKING);
    }

    @Step("Check Select a reason on Decline Booking?")
    public boolean isSelectReasonVisible() {
        return isPresent(ComposeLocators.textView("Select a reason"));
    }

    @Step("Select first decline reason from dropdown")
    public String selectFirstDeclineReason() {
        if (!isDeclineBookingDialogVisible()) {
            throw new IllegalStateException("Decline Booking? dialog not on screen");
        }
        String chosen = pickFirstDropdownOption("Select a reason", "Reason dropdown showed no options");
        dismissDropdownOverlay();
        return chosen;
    }

    @Step("Read Confirm Decline enabled on clickable outer View")
    public boolean isConfirmDeclineEnabled() {
        return anyOuterEnabled("Confirm Decline");
    }

    @Step("Tap Confirm Decline on the reason dialog")
    public void tapConfirmDecline() {
        if (!isDeclineBookingDialogVisible()) {
            throw new IllegalStateException("Decline Booking? dialog not on screen — refusing Confirm Decline");
        }
        WebElement target = smallestClickableWithText("Confirm Decline");
        if (target == null) {
            List<WebElement> labels = driver.findElements(ComposeLocators.textView("Confirm Decline"));
            if (labels.isEmpty()) {
                throw new IllegalStateException("Confirm Decline not on reason dialog");
            }
            clickGestureOn(labels.get(0));
            return;
        }
        clickGestureOn(target);
    }

    @Step("Check Request to extend time dialog")
    public boolean isExtendTimeDialogVisible() {
        return isPresent(EXTEND_TIME);
    }

    /**
     * Operator/reason menus are a separate window. While open, Assign machine / Decline
     * Booking? leave the tree. Tap left of the popup — do not press Back from Bookings
     * without a known Back destination.
     */
    @Step("Dismiss dropdown overlay without leaving Bookings")
    public void dismissDropdownOverlay() {
        if (isAssignMachineVisible() || isDeclineBookingDialogVisible()) {
            return;
        }
        Dimension size = driver.manage().window().getSize();
        clickGestureAt((int) (size.width * 0.22), (int) (size.height * 0.42));
        try {
            Waits.until(driver,
                    d -> (isAssignMachineVisible() || isDeclineBookingDialogVisible()) ? Boolean.TRUE : null,
                    "Dropdown overlay did not dismiss",
                    Duration.ofSeconds(4));
        } catch (TimeoutException first) {
            clickGestureAt((int) (size.width * 0.50), (int) (size.height * 0.28));
            Waits.until(driver,
                    d -> (isAssignMachineVisible() || isDeclineBookingDialogVisible()) ? Boolean.TRUE : null,
                    "Dropdown overlay did not dismiss after second tap",
                    Duration.ofSeconds(4));
        }
    }

    private void refuseIfExtendTime(String action) {
        if (isExtendTimeDialogVisible()) {
            throw new IllegalStateException(
                    "Request to extend time is up (#15) — refusing " + action);
        }
    }

    private boolean anyOuterEnabled(String text) {
        for (WebElement row : driver.findElements(ComposeLocators.clickableWithText(text))) {
            if (Boolean.parseBoolean(row.getAttribute("enabled"))) {
                return true;
            }
        }
        return false;
    }

    private String pickFirstDropdownOption(String fieldLabel, String emptyMessage) {
        Set<String> before = visibleTexts();
        WebElement field = smallestClickableWithText(fieldLabel);
        if (field == null) {
            List<WebElement> labels = driver.findElements(ComposeLocators.textView(fieldLabel));
            if (labels.isEmpty()) {
                throw new IllegalStateException(fieldLabel + " not on screen");
            }
            clickGestureOn(labels.get(0));
        } else {
            clickGestureOn(field);
        }
        Waits.until(driver,
                d -> firstNewMenuNode(before) != null ? Boolean.TRUE : null,
                emptyMessage,
                Duration.ofSeconds(8));
        WebElement option = firstNewMenuNode(before);
        if (option == null) {
            throw new IllegalStateException(emptyMessage);
        }
        String chosen = option.getAttribute("text");
        if (chosen == null || "null".equalsIgnoreCase(chosen)) {
            chosen = "";
        }
        chosen = chosen.trim();
        WebElement row = clickableAncestorOrSelf(option);
        lastMenuOptionEnabled = Boolean.parseBoolean(row.getAttribute("enabled"));
        clickGestureOn(row);
        return chosen;
    }

    private WebElement firstNewMenuNode(Set<String> before) {
        for (WebElement node : driver.findElements(By.className("android.widget.TextView"))) {
            String raw = node.getAttribute("text");
            if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw)) {
                continue;
            }
            String text = raw.trim();
            if (before.contains(text) || text.length() >= 80) {
                continue;
            }
            if (text.contains("operators are busy")
                    || text.contains("Confirm the machine")
                    || text.startsWith("Please select")
                    || text.equalsIgnoreCase("Search")
                    || text.equalsIgnoreCase("Cancel")) {
                continue;
            }
            return node;
        }
        return null;
    }

    private Set<String> visibleTexts() {
        LinkedHashSet<String> texts = new LinkedHashSet<>();
        for (WebElement node : driver.findElements(By.className("android.widget.TextView"))) {
            String raw = node.getAttribute("text");
            if (raw != null && !raw.isBlank() && !"null".equalsIgnoreCase(raw)) {
                texts.add(raw.trim());
            }
        }
        return texts;
    }

    private WebElement smallestClickableWithText(String text) {
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(text));
        WebElement best = null;
        int bestArea = Integer.MAX_VALUE;
        for (WebElement row : rows) {
            org.openqa.selenium.Rectangle box = row.getRect();
            int area = box.width * box.height;
            if (area > 0 && area < bestArea) {
                bestArea = area;
                best = row;
            }
        }
        return best;
    }

    private WebElement clickableAncestorOrSelf(WebElement node) {
        List<WebElement> ancestors = node.findElements(
                By.xpath("./ancestor-or-self::android.view.View[@clickable='true']"));
        if (ancestors.isEmpty()) {
            // Dump Assign/Change: the TextView itself is clickable.
            if (Boolean.parseBoolean(node.getAttribute("clickable"))) {
                return node;
            }
            return node;
        }
        WebElement best = ancestors.get(0);
        int bestArea = Integer.MAX_VALUE;
        for (WebElement row : ancestors) {
            org.openqa.selenium.Rectangle box = row.getRect();
            int area = box.width * box.height;
            if (area > 0 && area < bestArea) {
                bestArea = area;
                best = row;
            }
        }
        return best;
    }

    private void clickGestureOn(WebElement element) {
        org.openqa.selenium.Rectangle box = element.getRect();
        clickGestureAt(box.x + box.width / 2, box.y + box.height / 2);
    }

    private void clickGestureAt(int x, int y) {
        driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    private static boolean isChromeCopy(String text) {
        return TAB_UPCOMING.equals(text) || TAB_ACTIVE.equals(text) || TAB_COMPLETED.equals(text)
                || TITLE_UPCOMING.equals(text) || TITLE_ACTIVE.equals(text)
                || TITLE_COMPLETED.equals(text)
                || "Help".equals(text) || "Booking for".equals(text)
                || "Get Direction".equals(text) || "View More Details".equals(text)
                || "View Less Details".equals(text) || "Assign".equals(text)
                || "Change".equals(text) || "Quick Booking".equals(text)
                || EMPTY_ACTIVE.equals(text)
                || "Assign machine".equals(text) || "Select an operator".equals(text)
                || "Confirm".equals(text) || "Skip".equals(text)
                || "Accept".equals(text) || "Decline".equals(text);
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
