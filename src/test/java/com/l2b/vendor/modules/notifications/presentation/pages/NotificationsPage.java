package com.l2b.vendor.modules.notifications.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Notifications (Home bell). Live dump {@code /tmp/l2b-notifications-0001-20260928}
 * on {@code 9000000001}. Stack screen — bottom tabs gone. Never Accept / Decline.
 */
public class NotificationsPage extends SplashScreen {

    @Override
    @Step("Wait for Notification screen")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Notification screen did not appear", Duration.ofSeconds(12));
    }

    @Step("Check Notification identity")
    public boolean isDisplayedNow() {
        return isTitleVisible() && (isUnreadTabVisible() || isAllTabVisible());
    }

    @Step("Check title Notification")
    public boolean isTitleVisible() {
        return isPresent(ComposeLocators.textView("Notification"));
    }

    @Step("Check Unread tab")
    public boolean isUnreadTabVisible() {
        return isPresent(ComposeLocators.textView("Unread"));
    }

    @Step("Check All tab")
    public boolean isAllTabVisible() {
        return isPresent(ComposeLocators.textView("All"));
    }

    @Step("Check header Back")
    public boolean isBackVisible() {
        return isPresent(By.xpath("//*[@content-desc='Back']"));
    }

    @Step("Check Unread empty copy")
    public boolean isUnreadEmptyVisible() {
        return isPresent(ComposeLocators.textView("You're all caught up."));
    }

    @Step("Check View all notifications CTA")
    public boolean isViewAllNotificationsVisible() {
        return isPresent(ComposeLocators.textView("View all notifications"));
    }

    @Step("Check Rentals category on All")
    public boolean isRentalsCategoryVisible() {
        return isPresent(ComposeLocators.textView("Rentals"));
    }

    @Step("Check See all on category")
    public boolean isSeeAllVisible() {
        return isPresent(ComposeLocators.textView("See all"));
    }

    @Step("Check booking-request row title")
    public boolean hasNewBookingRequest() {
        return isPresent(ComposeLocators.textView("New booking request"));
    }

    @Step("Check View Details on a row")
    public boolean isViewDetailsVisible() {
        return isPresent(ComposeLocators.textView("View Details"));
    }

    @Step("Check relative time (ago)")
    public boolean hasRelativeTime() {
        return visibleTexts().stream().anyMatch(t -> t.contains("ago"));
    }

    @Step("Check Mark all read (category list)")
    public boolean isMarkAllReadVisible() {
        return isPresent(ComposeLocators.textView("Mark all read"));
    }

    @Step("Check category list (Rentals See-all)")
    public boolean isCategoryListDisplayed() {
        return isMarkAllReadVisible()
                && (isRentalsCategoryVisible() || isViewDetailsVisible());
    }

    @Step("Bottom rental tabs should be gone on Notification stack")
    public boolean bottomTabsAbsent() {
        return !isPresent(By.xpath("//*[@content-desc='Calendar']"))
                && !isPresent(By.xpath("//*[@content-desc='Earning']"))
                && !isPresent(By.xpath("//*[@content-desc='Fleet']"));
    }

    @Step("Title and Back share header band")
    public boolean headerTitleBackAligned() {
        Rectangle title = firstBounds(ComposeLocators.textView("Notification"));
        List<WebElement> backs = driver.findElements(By.xpath("//*[@content-desc='Back']"));
        if (title == null || backs.isEmpty()) {
            return false;
        }
        Rectangle back = backs.get(0).getRect();
        return Math.abs((title.y + title.height / 2) - (back.y + back.height / 2)) <= 24
                && back.x < title.x;
    }

    @Step("Unread and All tabs roughly side-by-side")
    public boolean unreadAllTabsLayoutOk() {
        Rectangle unread = firstBounds(ComposeLocators.textView("Unread"));
        Rectangle all = firstBounds(ComposeLocators.textView("All"));
        if (unread == null || all == null) {
            return false;
        }
        return Math.abs(unread.y - all.y) <= 8
                && unread.x < all.x
                && Math.abs(unread.width - all.width) < 40;
    }

    @Step("Title left inset publish band (after Back)")
    public boolean titleLeftInsetOk() {
        Rectangle title = firstBounds(ComposeLocators.textView("Notification"));
        if (title == null) {
            return true;
        }
        // Dump: title x=126 with Back at x=64
        return title.x >= 80 && title.x <= 180;
    }

    @Step("Tap Unread tab")
    public void tapUnread() {
        clickText("Unread");
    }

    @Step("Tap All tab")
    public void tapAll() {
        clickText("All");
    }

    @Step("Tap View all notifications")
    public void tapViewAllNotifications() {
        clickText("View all notifications");
    }

    @Step("Tap See all")
    public void tapSeeAll() {
        clickText("See all");
    }

    @Step("Tap first View Details")
    public void tapViewDetails() {
        List<WebElement> els = driver.findElements(ComposeLocators.textView("View Details"));
        if (els.isEmpty()) {
            throw new IllegalStateException("View Details not visible");
        }
        clickGestureOn(els.get(0));
    }

    @Step("Tap Mark all read")
    public void tapMarkAllRead() {
        clickText("Mark all read");
    }

    @Step("Tap header Back")
    public void tapHeaderBack() {
        if (isPresent(By.xpath("//*[@content-desc='Back']"))) {
            driver.findElement(By.xpath("//*[@content-desc='Back']")).click();
            return;
        }
        driver.navigate().back();
    }

    @Step("Close Quick Booking from View Details if Close present")
    public void tapCloseIfPresent() {
        List<WebElement> els = driver.findElements(By.xpath("//*[@content-desc='Close']"));
        if (!els.isEmpty()) {
            els.get(0).click();
        }
    }

    @Step("Swipe notification list up")
    public void swipeListUp() {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1700));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(350),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 900));
        swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(swipe));
    }

    @Step("Visible TextView texts")
    public Set<String> visibleTexts() {
        Set<String> out = new LinkedHashSet<>();
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el).trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private void clickText(String text) {
        List<WebElement> els = driver.findElements(ComposeLocators.textView(text));
        if (els.isEmpty()) {
            throw new IllegalStateException("Text not found: " + text);
        }
        clickGestureOn(els.get(0));
    }

    private Rectangle firstBounds(By by) {
        List<WebElement> els = driver.findElements(by);
        if (els.isEmpty()) {
            return null;
        }
        return els.get(0).getRect();
    }

    private void clickGestureOn(WebElement el) {
        Rectangle r = el.getRect();
        int x = r.x + Math.max(1, r.width / 2);
        int y = r.y + Math.max(1, r.height / 2);
        driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    private static String safeText(WebElement el) {
        try {
            String t = el.getAttribute("text");
            return t == null || "null".equalsIgnoreCase(t) ? "" : t;
        } catch (RuntimeException e) {
            return "";
        }
    }
}
