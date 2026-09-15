package com.l2b.vendor.modules.onboarding.presentation.pages;

import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.qameta.allure.Step;
import java.time.Duration;
import org.openqa.selenium.By;

/**
 * System notification grant dialog. Locators from live dump 2026-09-15 on
 * {@code com.l2b.app.qa} cold launch (GrantPermissionsActivity):
 * message {@code Allow L2B Vendor to send you notifications?},
 * Allow {@code permission_allow_button}, Don’t allow {@code permission_deny_button}.
 */
public class NotificationPermissionDialog extends SplashScreen {

    private static final String CONTROLLER = "com.android.permissioncontroller:id/";
    private static final By MESSAGE = By.id(CONTROLLER + "permission_message");
    private static final By ALLOW = By.id(CONTROLLER + "permission_allow_button");
    private static final By DENY = By.id(CONTROLLER + "permission_deny_button");

    @Override
    @Step("Wait for notification permission dialog")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Notification permission dialog (Allow / Don't allow) did not appear",
                Duration.ofSeconds(10));
    }

    @Step("Check notification permission dialog is visible")
    public boolean isDisplayedNow() {
        return isPresent(MESSAGE) && isPresent(ALLOW) && isPresent(DENY);
    }

    @Step("Tap Allow on notification permission")
    public void tapAllow() {
        Waits.clickable(driver, driver.findElement(ALLOW)).click();
    }

    @Step("Tap Don't allow on notification permission")
    public void tapDeny() {
        Waits.clickable(driver, driver.findElement(DENY)).click();
    }

    @Step("Dismiss notification permission with Back (no Allow/Deny)")
    public void dismissWithBack() {
        ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.BACK));
    }
}
