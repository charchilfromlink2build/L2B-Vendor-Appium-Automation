package com.l2b.vendor.modules.quickbooking.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

/**
 * Post-OTP Quick Booking queue for rental owner {@code 9000000001}.
 * Identity locators from live dump 18 Sep 2026
 * ({@code /tmp/l2b-qb-discovery/rental-company-0001/window.xml}):
 * title {@code Quick Booking} plus Close ({@code content-desc='Close'}).
 * View More Details is a clickable TextView. Accept / Decline stay on the
 * clickable outer View — never tap them from landing tests.
 */
public class QuickBookingLandingPage extends SplashScreen {

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true'][.//*[@content-desc='Close']]")
    private WebElement closeButton;

    private boolean lastMenuOptionEnabled = true;

    @Override
    @Step("Wait for Quick Booking landing")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Quick Booking landing did not appear after OTP", Duration.ofSeconds(20));
    }

    @Step("Check Quick Booking title and Close are visible")
    public boolean isDisplayedNow() {
        return isPresent(ComposeLocators.textView("Quick Booking"))
                && isPresent(By.xpath("//*[@content-desc='Close']"));
    }

    @Step("Check Close control is visible")
    public boolean isCloseVisible() {
        return isPresent(By.xpath("//*[@content-desc='Close']"));
    }

    @Step("Tap Close (clickable outer View, not the inner icon)")
    public void tapClose() {
        tap(closeButton);
    }

    @Step("Check Accept is visible on a booking card")
    public boolean isAcceptVisible() {
        return isPresent(ComposeLocators.textView("Accept"));
    }

    @Step("Count Accept actions (clickable outer Views)")
    public int acceptCount() {
        return driver.findElements(ComposeLocators.clickableWithText("Accept")).size();
    }

    @Step("Count Decline actions (clickable outer Views)")
    public int declineCount() {
        return driver.findElements(ComposeLocators.clickableWithText("Decline")).size();
    }

    @Step("Count View More Details rows")
    public int viewMoreDetailsCount() {
        return driver.findElements(ComposeLocators.textView("View More Details")).size();
    }

    @Step("Count View Less Details rows")
    public int viewLessDetailsCount() {
        return driver.findElements(ComposeLocators.textView("View Less Details")).size();
    }

    @Step("Check View More Details is visible")
    public boolean isViewMoreVisible() {
        return isPresent(ComposeLocators.textView("View More Details"));
    }

    @Step("Check View Less Details is visible")
    public boolean isViewLessVisible() {
        return isPresent(ComposeLocators.textView("View Less Details"));
    }

    @Step("Tap the first View More Details")
    public void tapFirstViewMore() {
        List<WebElement> rows = driver.findElements(ComposeLocators.textView("View More Details"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("View More Details not on screen — refusing tap");
        }
        tap(rows.get(0));
    }

    @Step("Tap the first View Less Details")
    public void tapFirstViewLess() {
        List<WebElement> rows = driver.findElements(ComposeLocators.textView("View Less Details"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("View Less Details not on screen — refusing tap");
        }
        tap(rows.get(0));
    }

    @Step("Check visible text contains fragment")
    public boolean hasText(String fragment) {
        return isPresent(ComposeLocators.textViewContains(fragment));
    }

    @Step("Check rental Timer label is visible")
    public boolean isTimerVisible() {
        return isPresent(ComposeLocators.textView("Timer"));
    }

    @Step("Check Get Direction is visible")
    public boolean isGetDirectionVisible() {
        return isPresent(ComposeLocators.textView("Get Direction"));
    }

    @Step("Read Get Direction enabled on the clickable outer View")
    public boolean isGetDirectionEnabled() {
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Get Direction"));
        if (rows.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(rows.get(0).getAttribute("enabled"));
    }

    @Step("Tap first Get Direction (clickable outer View)")
    public void tapFirstGetDirection() {
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Get Direction"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Get Direction not on screen — refusing tap");
        }
        tap(rows.get(0));
    }

    @Step("Check Booking for is visible")
    public boolean isBookingForVisible() {
        return isPresent(ComposeLocators.textView("Booking for"));
    }

    @Step("Check material Order Placed chrome")
    public boolean isOrderPlacedVisible() {
        return isPresent(ComposeLocators.textViewContains("Order Placed"));
    }

    @Step("Check Booking Declined success dialog")
    public boolean isBookingDeclinedVisible() {
        return isPresent(ComposeLocators.textView("Booking Declined"));
    }

    @Step("Tap OK on Booking Declined")
    public void tapBookingDeclinedOk() {
        if (!isBookingDeclinedVisible()) {
            throw new IllegalStateException("Booking Declined not on screen — refusing OK");
        }
        WebElement ok = smallestClickableWithText("OK");
        if (ok == null) {
            List<WebElement> labels = driver.findElements(ComposeLocators.textView("OK"));
            if (labels.isEmpty()) {
                throw new IllegalStateException("OK not on Booking Declined");
            }
            clickGestureOn(labels.get(0));
        } else {
            clickGestureOn(ok);
        }
    }

    @Step("Check Decline Booking? reason dialog")
    public boolean isDeclineBookingDialogVisible() {
        return isPresent(ComposeLocators.textView("Decline Booking?"));
    }

    @Step("Check Assign machine sheet")
    public boolean isAssignMachineVisible() {
        return isPresent(ComposeLocators.textView("Assign machine"));
    }

    @Step("Check Assign machine Skip is visible")
    public boolean isAssignSkipVisible() {
        return isPresent(ComposeLocators.textView("Skip"));
    }

    @Step("Check Select an operator field is on Assign machine")
    public boolean isSelectOperatorVisible() {
        return isPresent(ComposeLocators.textView("Select an operator"));
    }

    @Step("Check Select a reason field is on Decline Booking?")
    public boolean isSelectReasonVisible() {
        return isPresent(ComposeLocators.textView("Select a reason"));
    }

    /**
     * Opens {@code Select an operator} and taps the first new menu row.
     * Returns the chosen operator label. Fails if the list is empty.
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
     * Opens {@code Select a reason} and taps the first new menu row.
     * Returns the chosen reason. Fails if the list is empty.
     */
    @Step("Select first decline reason from dropdown")
    public String selectFirstDeclineReason() {
        if (!isDeclineBookingDialogVisible()) {
            throw new IllegalStateException("Decline Booking? dialog not on screen");
        }
        String chosen = pickFirstDropdownOption("Select a reason", "Reason dropdown showed no options");
        dismissDropdownOverlay();
        return chosen;
    }

    @Step("Read whether the last dropdown row was enabled")
    public boolean lastMenuOptionEnabled() {
        return lastMenuOptionEnabled;
    }

    @Step("Tap Assign machine Confirm via clickGesture (Compose enabled lives on outer View)")
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

    @Step("Tap Decline on the reason dialog via clickGesture")
    public void tapDeclineOnReasonDialog() {
        dismissDropdownOverlay();
        if (!isDeclineBookingDialogVisible()) {
            throw new IllegalStateException("Decline Booking? dialog not on screen — refusing dialog Decline");
        }
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Decline"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Decline not on reason dialog");
        }
        clickGestureOn(rows.get(rows.size() - 1));
    }

    @Step("Tap first Quick Booking Decline (outer View)")
    public void tapFirstDecline() {
        if (isExtendTimeDialogVisible() || isDeclineBookingDialogVisible()) {
            throw new IllegalStateException("A dialog is showing — refusing card Decline tap");
        }
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Decline"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Decline not on Quick Booking — refusing tap");
        }
        tap(rows.get(0));
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

    /**
     * Operator/reason menus are a separate UiAutomator window. While open, Assign machine /
     * Decline Booking? are not in the tree. Tap left of the popup — do not press Back
     * (Back from Quick Booking goes to the launcher, bug #13).
     */
    @Step("Dismiss dropdown overlay without leaving Quick Booking")
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

    private void clickGestureOn(WebElement element) {
        org.openqa.selenium.Rectangle box = element.getRect();
        clickGestureAt(box.x + box.width / 2, box.y + box.height / 2);
    }

    private void clickGestureAt(int x, int y) {
        driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    private WebElement clickableAncestorOrSelf(WebElement node) {
        List<WebElement> ancestors = node.findElements(
                By.xpath("./ancestor-or-self::android.view.View[@clickable='true']"));
        if (ancestors.isEmpty()) {
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

    @Step("Read Assign Confirm enabled on clickable outer View")
    public boolean isAssignConfirmEnabled() {
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Confirm"));
        for (WebElement row : rows) {
            if (Boolean.parseBoolean(row.getAttribute("enabled"))) {
                return true;
            }
        }
        return false;
    }

    @Step("Check Request to extend time dialog")
    public boolean isExtendTimeDialogVisible() {
        return isPresent(ComposeLocators.textView("Request to extend time"));
    }

    @Step("Read Accept enabled on the clickable outer View")
    public boolean isAcceptEnabled() {
        return outerEnabled("Accept");
    }

    @Step("Read Decline enabled on the clickable outer View")
    public boolean isDeclineEnabled() {
        return outerEnabled("Decline");
    }

    /**
     * One booking only. Taps the first Quick Booking card Accept (outer View).
     * Refuses the Home extend-time dialog Accept.
     */
    @Step("Tap first Quick Booking Accept (outer View)")
    public void tapFirstAccept() {
        if (isExtendTimeDialogVisible()) {
            throw new IllegalStateException("Extend-time dialog is showing — refusing Accept tap");
        }
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Accept"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Accept not on Quick Booking — refusing tap");
        }
        tap(rows.get(0));
    }

    /**
     * One booking only. Taps the first Quick Booking card Decline (outer View).
     * Refuses the Home extend-time dialog Decline.
     */
    @Step("Tap second Quick Booking Decline if two cards are visible, else first")
    public void tapSecondDeclineOrFirst() {
        if (isExtendTimeDialogVisible()) {
            throw new IllegalStateException("Extend-time dialog is showing — refusing Decline tap");
        }
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Decline"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Decline not on Quick Booking — refusing tap");
        }
        tap(rows.size() >= 2 ? rows.get(1) : rows.get(0));
    }

    private boolean outerEnabled(String label) {
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(label));
        if (rows.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(rows.get(0).getAttribute("enabled"));
    }

    @Step("Read first Amount · Online Mode line")
    public String firstAmountLine() {
        return firstText(ComposeLocators.textViewContains("Amount · Online Mode"));
    }

    @Step("Read all Amount · Online Mode lines")
    public List<String> amountLines() {
        List<String> lines = new ArrayList<>();
        for (WebElement row : driver.findElements(ComposeLocators.textViewContains("Amount · Online Mode"))) {
            String raw = row.getAttribute("text");
            if (raw != null && !"null".equalsIgnoreCase(raw)) {
                lines.add(raw);
            }
        }
        return lines;
    }

    @Step("Read first Timer value")
    public String firstTimerValue() {
        return firstText(By.xpath(
                "//android.widget.TextView[@text='Timer']/following-sibling::android.widget.TextView"));
    }

    /**
     * Dump 18 Sep: date/duration sits immediately under Amount · Online Mode,
     * e.g. {@code 18 Sep 1:44 PM - 19 Sep 2026 1:44 PM (1 day)}.
     */
    @Step("Read first booking start/end/duration line")
    public String firstDateRangeLine() {
        return firstText(By.xpath(
                "//android.widget.TextView[contains(@text,'Amount · Online Mode')]"
                        + "/following-sibling::android.widget.TextView[1]"));
    }

    @Step("Tap first Quick Booking Accept via clickGesture N times")
    public void tapFirstAcceptRapidly(int times) {
        if (isExtendTimeDialogVisible()) {
            throw new IllegalStateException("Extend-time dialog is showing — refusing Accept tap");
        }
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Accept"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Accept not on Quick Booking — refusing tap");
        }
        org.openqa.selenium.Rectangle box = rows.get(0).getRect();
        int x = box.x + box.width / 2;
        int y = box.y + box.height / 2;
        for (int i = 0; i < times; i++) {
            clickGestureAt(x, y);
        }
    }

    @Step("Tap first Quick Booking Decline via clickGesture N times")
    public void tapFirstDeclineRapidly(int times) {
        if (isExtendTimeDialogVisible() || isDeclineBookingDialogVisible()) {
            throw new IllegalStateException("A dialog is showing — refusing Decline tap");
        }
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Decline"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Decline not on Quick Booking — refusing tap");
        }
        org.openqa.selenium.Rectangle box = rows.get(0).getRect();
        int x = box.x + box.width / 2;
        int y = box.y + box.height / 2;
        for (int i = 0; i < times; i++) {
            clickGestureAt(x, y);
        }
    }

    @Step("Tap Assign machine Skip")
    public void tapAssignSkip() {
        if (!isAssignMachineVisible()) {
            throw new IllegalStateException("Assign machine not on screen — refusing Skip");
        }
        List<WebElement> rows = driver.findElements(ComposeLocators.textView("Skip"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Skip not on Assign machine");
        }
        clickGestureOn(rows.get(0));
    }

    @Step("Check Close sheet scrim")
    public boolean isCloseSheetVisible() {
        return isPresent(By.xpath("//*[@content-desc='Close sheet']"));
    }

    @Step("Tap Close sheet scrim")
    public void tapCloseSheet() {
        List<WebElement> rows = driver.findElements(By.xpath("//*[@content-desc='Close sheet']"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Close sheet not on screen");
        }
        clickGestureOn(rows.get(0));
    }

    @Step("Check timer value is 00:00")
    public boolean isFirstTimerExpired() {
        String value = firstTimerValue();
        return "0:00".equals(value) || "00:00".equals(value);
    }

    @Step("Check a loading/progress marker")
    public boolean isLoadingVisible() {
        return isPresent(By.className("android.widget.ProgressBar"))
                || isPresent(ComposeLocators.textViewContains("Loading"))
                || isPresent(ComposeLocators.textViewContains("Please wait"));
    }

    @Step("Check Booking for has a non-blank site")
    public boolean isBookingForBlank() {
        List<WebElement> rows = driver.findElements(ComposeLocators.textView("Booking for"));
        if (rows.isEmpty()) {
            return true;
        }
        return !isPresent(ComposeLocators.textViewContains("hebbal"))
                && !isPresent(ComposeLocators.textViewContains("Bengaluru"))
                && !isPresent(ComposeLocators.textViewContains("Banglore"));
    }

    @Step("Tap Cancel on Decline Booking? dialog")
    public void tapDeclineDialogCancel() {
        if (!isDeclineBookingDialogVisible()) {
            throw new IllegalStateException("Decline Booking? not on screen — refusing Cancel");
        }
        WebElement cancel = smallestClickableWithText("Cancel");
        if (cancel == null) {
            List<WebElement> labels = driver.findElements(ComposeLocators.textView("Cancel"));
            if (labels.isEmpty()) {
                throw new IllegalStateException("Cancel not on Decline dialog");
            }
            clickGestureOn(labels.get(0));
        } else {
            clickGestureOn(cancel);
        }
    }

    @Step("Swipe the booking list up to reveal further cards")
    public void swipeListUp() {
        Dimension size = driver.manage().window().getSize();
        int left = (int) (size.width * 0.10);
        int top = (int) (size.height * 0.35);
        int width = (int) (size.width * 0.80);
        int height = (int) (size.height * 0.40);
        driver.executeScript("mobile: swipeGesture", Map.of(
                "left", left,
                "top", top,
                "width", width,
                "height", height,
                "direction", "up",
                "percent", 0.80));
    }

    private String firstText(By locator) {
        List<WebElement> rows = driver.findElements(locator);
        if (rows.isEmpty()) {
            return "";
        }
        String raw = rows.get(0).getAttribute("text");
        return raw == null || "null".equalsIgnoreCase(raw) ? "" : raw;
    }
}
