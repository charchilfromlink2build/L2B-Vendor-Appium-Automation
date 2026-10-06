package com.l2b.vendor.modules.settings.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Profile drawer → Settings (permissions). Dump {@code /tmp/l2b-app-settings-0001-20261005}.
 * Live 5 Oct: error empty state &quot;An unexpected error occurred&quot; + Retry (BUGS_FOUND #51).
 * Toggle probes only when list loads — restore when flipped. Never Log Out Confirm / Accept / Decline.
 */
public class AppSettingsPage extends SplashScreen {

    public static final String ROW_NOTIFICATION = "Notification";
    public static final String ROW_GPS = "GPS Location";
    public static final String ROW_CAMERA = "Camera Access";
    public static final String ROW_CONTACTS = "Contacts Access";
    public static final String ROW_GALLERY = "Gallery / Media";
    public static final String ROW_CALENDAR = "Calendar";
    public static final String ROW_BACKGROUND = "Background data usage";
    public static final String ROW_MIC = "Microphone";
    public static final String ERR_MSG = "An unexpected error occurred";

    @Override
    @Step("Wait for App Settings")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Settings screen did not appear", Duration.ofSeconds(20));
    }

    @Step("Check Settings identity")
    public boolean isDisplayedNow() {
        return isSettingsTitleVisible()
                || isErrorVisible()
                || isPresent(ComposeLocators.textView("Permissions"))
                || isPresent(ComposeLocators.textView(ROW_NOTIFICATION))
                || isPresent(ComposeLocators.textView(ROW_GPS));
    }

    @Step("Check Settings title")
    public boolean isSettingsTitleVisible() {
        return isPresent(ComposeLocators.textView("Settings"))
                || isPresent(ComposeLocators.textView("सेटिंग्स"));
    }

    @Step("Check load error empty state")
    public boolean isErrorVisible() {
        return isPresent(ComposeLocators.textView(ERR_MSG))
                || isPresent(ComposeLocators.textViewContains("unexpected error"))
                || isPresent(ComposeLocators.textViewContains("Something went wrong"));
    }

    @Step("Check Retry")
    public boolean isRetryVisible() {
        return isPresent(ComposeLocators.textView("Retry"));
    }

    @Step("Full permissions chrome")
    public boolean isFullChromeVisible() {
        return isPresent(ComposeLocators.textView("Permissions"))
                && isPresent(ComposeLocators.textView(ROW_NOTIFICATION));
    }

    @Step("Tap Retry")
    public void tapRetry() {
        tap(driver.findElement(ComposeLocators.textView("Retry")));
    }

    @Step("Check Permissions heading")
    public boolean isPermissionsHeadingVisible() {
        return isPresent(ComposeLocators.textView("Permissions"));
    }

    @Step("Check permission row")
    public boolean isRowVisible(String row) {
        return isPresent(ComposeLocators.textView(row));
    }

    @Step("Count Switch nodes")
    public int switchCount() {
        return driver.findElements(By.className("android.widget.Switch")).size();
    }

    @Step("First switch checked state")
    public Boolean firstSwitchChecked() {
        List<WebElement> sw = driver.findElements(By.className("android.widget.Switch"));
        if (sw.isEmpty()) {
            return null;
        }
        String c = sw.get(0).getAttribute("checked");
        return c != null && Boolean.parseBoolean(c);
    }

    @Step("Tap first Switch (toggle)")
    public void tapFirstSwitch() {
        List<WebElement> sw = driver.findElements(By.className("android.widget.Switch"));
        if (sw.isEmpty()) {
            throw new IllegalStateException("No Switch visible");
        }
        Rectangle r = sw.get(0).getRect();
        driver.executeScript("mobile: clickGesture",
                java.util.Map.of("x", r.x + Math.max(1, r.width / 2),
                        "y", r.y + Math.max(1, r.height / 2)));
    }

    @Step("Check Back")
    public boolean isBackVisible() {
        return isPresent(By.xpath("//*[@content-desc='Back']"));
    }

    @Step("Header Back left-aligned")
    public boolean headerBackAligned() {
        List<WebElement> backs = driver.findElements(By.xpath("//*[@content-desc='Back']"));
        if (backs.isEmpty()) {
            return false;
        }
        Rectangle r = backs.get(0).getRect();
        return r.x < 120 && r.y < 220;
    }

    @Step("Tap Back")
    public void tapBack() {
        tap(driver.findElement(By.xpath("//*[@content-desc='Back']")));
    }

    @Step("Device Back")
    public void pressDeviceBack() {
        driver.navigate().back();
    }

    @Step("Swipe settings list up")
    public void swipeListUp() {
        driver.executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 1400, "width", 600, "height", 700,
                        "direction", "up", "percent", 0.7));
    }

    @Step("Swipe settings list down")
    public void swipeListDown() {
        driver.executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 800, "width", 600, "height", 700,
                        "direction", "down", "percent", 0.7));
    }
}
