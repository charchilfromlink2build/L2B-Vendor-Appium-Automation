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
 * System notification grant dialog (GrantPermissionsActivity) shown on cold launch of {@code com.l2b.app.qa}.
 * Locators are Android permission-controller resource ids from the 2026-09-15 dump — stable vs app copy,
 * but they change if the OEM permission UI is restyled.
 */
public class NotificationPermissionDialog extends SplashScreen {

    private static final String CONTROLLER = "com.android.permissioncontroller:id/";
    /** Prompt message node ("Allow L2B Vendor to send you notifications?"). */
    private static final By MESSAGE = By.id(CONTROLLER + "permission_message");
    /** Allow button on the system notification dialog. */
    private static final By ALLOW = By.id(CONTROLLER + "permission_allow_button");
    /** Don't allow button on the system notification dialog. */
    private static final By DENY = By.id(CONTROLLER + "permission_deny_button");

    /** Wait until message, Allow, and Don't allow are all present (up to 10 seconds). */
    @Override
    @Step("Wait for notification permission dialog")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Notification permission dialog (Allow / Don't allow) did not appear",
                Duration.ofSeconds(10));
    }

    /** True when the permission message and both Allow / Don't allow buttons are present. */
    @Step("Check notification permission dialog is visible")
    public boolean isDisplayedNow() {
        return isPresent(MESSAGE) && isPresent(ALLOW) && isPresent(DENY);
    }

    /** Tap Allow on the system notification dialog. */
    @Step("Tap Allow on notification permission")
    public void tapAllow() {
        Waits.clickable(driver, driver.findElement(ALLOW)).click();
    }

    /** Tap Don't allow on the system notification dialog. */
    @Step("Tap Don't allow on notification permission")
    public void tapDeny() {
        Waits.clickable(driver, driver.findElement(DENY)).click();
    }

    /** Dismiss the dialog with the device Back key (neither Allow nor Deny). */
    @Step("Dismiss notification permission with Back (no Allow/Deny)")
    public void dismissWithBack() {
        ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.BACK));
    }
}
