package com.l2b.vendor.modules.bookings.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.qameta.allure.Step;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Post-OTP Quick Booking queue. Locators from live dumps 18 Sep 2026
 * ({@code /tmp/l2b-qb-discovery/rental-company-0001/window.xml} and
 * {@code material-0017/window.xml}).
 *
 * <p>App bar title {@code Quick Booking}. Close is the clickable outer View wrapping
 * {@code content-desc='Close'} (inner icon View is not clickable). Accept / Decline
 * enabled lives on the clickable outer View; inner TextView/Button stay
 * {@code enabled=true} decoys — never tap the inner nodes. Do not tap Accept or Decline
 * in discovery/queue-preserving cases.
 */
public class QuickBookingPage extends SplashScreen {

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true'][.//*[@content-desc='Close']]")
    private WebElement closeButton;

    @Override
    @Step("Wait for Quick Booking")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Quick Booking screen did not appear after OTP", Duration.ofSeconds(20));
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

    @Step("Press device Back")
    public void pressBack() {
        ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.BACK));
    }

    @Step("Count Accept actions (clickable outer Views)")
    public int acceptCount() {
        return driver.findElements(ComposeLocators.clickableWithText("Accept")).size();
    }

    @Step("Count Decline actions (clickable outer Views)")
    public int declineCount() {
        return driver.findElements(ComposeLocators.clickableWithText("Decline")).size();
    }

    @Step("Check Accept is visible")
    public boolean isAcceptVisible() {
        return isPresent(ComposeLocators.textView("Accept"));
    }

    @Step("Tap first Accept (clickable outer View)")
    public void tapFirstAccept() {
        java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Accept"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Accept not on Quick Booking — refusing tap");
        }
        tap(rows.get(0));
    }

    @Step("Check Decline is visible")
    public boolean isDeclineVisible() {
        return isPresent(ComposeLocators.textView("Decline"));
    }

    @Step("Check View More Details is visible")
    public boolean isViewMoreDetailsVisible() {
        return isPresent(ComposeLocators.textView("View More Details"));
    }

    @Step("Check Booking for is visible")
    public boolean isBookingForVisible() {
        return isPresent(ComposeLocators.textView("Booking for"));
    }

    @Step("Check rental amount chrome (Amount · …)")
    public boolean isRentalAmountChromeVisible() {
        return isPresent(ComposeLocators.textViewContains("Amount \u00b7"));
    }

    @Step("Read first Amount · line (any payment mode)")
    public String firstAmountLine() {
        java.util.List<WebElement> rows = driver.findElements(
                ComposeLocators.textViewContains("Amount \u00b7"));
        if (rows.isEmpty()) {
            return "";
        }
        String raw = rows.get(0).getAttribute("text");
        return raw == null || "null".equalsIgnoreCase(raw) ? "" : raw;
    }

    @Step("Check rental Timer label is visible")
    public boolean isTimerVisible() {
        return isPresent(ComposeLocators.textView("Timer"));
    }

    @Step("Check Get Direction is visible")
    public boolean isGetDirectionVisible() {
        return isPresent(ComposeLocators.textView("Get Direction"));
    }

    @Step("Check material Order Placed chrome")
    public boolean isOrderPlacedVisible() {
        return isPresent(ComposeLocators.textViewContains("Order Placed"));
    }

    @Step("Check material Pickup scheduled chrome")
    public boolean isPickupScheduledVisible() {
        return isPresent(ComposeLocators.textViewContains("Pickup scheduled"));
    }

    @Step("Check Fast Delivery is visible")
    public boolean isFastDeliveryVisible() {
        return isPresent(ComposeLocators.textView("Fast Delivery"));
    }

    @Step("Check visible text contains fragment")
    public boolean hasText(String fragment) {
        return isPresent(ComposeLocators.textViewContains(fragment));
    }

    /**
     * Dump 18 Sep: Accept/Decline enabled lives on the clickable outer View.
     * Inner TextView/Button stay enabled=true decoys.
     */
    @Step("Read Accept enabled on the clickable outer View")
    public boolean isAcceptEnabled() {
        return outerEnabled("Accept");
    }

    @Step("Read Decline enabled on the clickable outer View")
    public boolean isDeclineEnabled() {
        return outerEnabled("Decline");
    }

    @Step("Read inner Accept TextView enabled (decoy)")
    public boolean isAcceptInnerTextEnabled() {
        return innerEnabled(ComposeLocators.textView("Accept"));
    }

    @Step("Read inner Accept Button clickable (decoy)")
    public boolean isAcceptInnerButtonClickable() {
        java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Accept"));
        if (rows.isEmpty()) {
            return false;
        }
        java.util.List<WebElement> buttons = rows.get(0).findElements(By.className("android.widget.Button"));
        if (buttons.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(buttons.get(0).getAttribute("clickable"));
    }

    @Step("Count View More Details rows")
    public int viewMoreDetailsCount() {
        return driver.findElements(ComposeLocators.textView("View More Details")).size();
    }

    @Step("Read first Timer value")
    public String firstTimerValue() {
        java.util.List<WebElement> labels = driver.findElements(ComposeLocators.textView("Timer"));
        if (labels.isEmpty()) {
            return "";
        }
        java.util.List<WebElement> values = driver.findElements(
                By.xpath("//android.widget.TextView[@text='Timer']/following-sibling::android.widget.TextView"));
        if (values.isEmpty()) {
            return "";
        }
        String raw = values.get(0).getAttribute("text");
        return raw == null || "null".equalsIgnoreCase(raw) ? "" : raw;
    }

    /**
     * Rapid tap Close on the clickable outer View. Does not tap Accept/Decline.
     */
    @Step("Tap Close rapidly {times} times")
    public void tapCloseRapidly(int times) {
        java.util.List<WebElement> rows = driver.findElements(
                By.xpath("//android.view.View[@clickable='true'][.//*[@content-desc='Close']]"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Close not on screen — refusing rapid tap");
        }
        org.openqa.selenium.Rectangle box = rows.get(0).getRect();
        int x = box.x + box.width / 2;
        int y = box.y + box.height / 2;
        for (int i = 0; i < times; i++) {
            driver.executeScript("mobile: clickGesture", java.util.Map.of("x", x, "y", y));
        }
    }

    private boolean outerEnabled(String label) {
        java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(label));
        if (rows.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(rows.get(0).getAttribute("enabled"));
    }

    private boolean innerEnabled(By locator) {
        java.util.List<WebElement> nodes = driver.findElements(locator);
        if (nodes.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(nodes.get(0).getAttribute("enabled"));
    }
}
