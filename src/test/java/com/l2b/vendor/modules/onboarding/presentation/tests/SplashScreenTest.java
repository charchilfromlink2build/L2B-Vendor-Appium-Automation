package com.l2b.vendor.modules.onboarding.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverFactory;
import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.ui.BaseTest;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.onboarding.domain.OnboardingEnvironment;
import com.l2b.vendor.modules.onboarding.presentation.pages.LanguagePage;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.modules.onboarding.presentation.pages.NotificationPermissionDialog;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.lang.reflect.Method;
import java.time.Duration;
import org.openqa.selenium.ScreenOrientation;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

/**
 * Splash / first-launch permission gate for the Vendor QA APK ({@code com.l2b.app.qa}).
 * Covers the system notification dialog and the splash-to-language handoff: Allow, Deny,
 * Back-dismiss, 8-second timeout, offline launch, force-stop, and rotation. One Appium session per method.
 * Known open splash bugs (do not merge): {@code BUGS_FOUND} #1 offline launch has no
 * error/retry; #19 cold-start logo paints twice (system splash exit + Compose splash).
 */
@Epic("Vendor app")
@Feature("Splash screen")
public class SplashScreenTest extends BaseTest {

    /** Language must appear within this window after the permission dialog is handled. */
    private static final Duration SPLASH_TIMEOUT = Duration.ofSeconds(8);

    @BeforeSuite(alwaysRun = true)
    public void prepareOnboardingEnvironment() {
        OnboardingEnvironment.prepareSuite();
    }

    /** Clear app data so every case starts at the real first-launch splash / permission prompt. */
    @Override
    protected boolean noReset() {
        return false;
    }

    /** Isolated sessions so Allow / Deny / offline radio state cannot leak between cases. */
    @Override
    protected boolean newSessionPerMethod() {
        return true;
    }

    /** Leave the system dialog on screen — these cases are specifically about that prompt. */
    @Override
    protected boolean autoGrantPermissions() {
        return false;
    }

    /** Turn Wi-Fi and mobile data off before the offline-launch case creates its session. */
    @Override
    protected void beforeCreateDriver(Method method) {
        if ("noNetworkAtLaunchThenRecover".equals(method.getName())) {
            Adb.disableRadios();
        }
    }

    /** Always restore radios after a method so a failed offline case cannot leave the emulator dark. */
    @AfterMethod(alwaysRun = true)
    public void restoreRadios() {
        try {
            Adb.enableRadios();
        } catch (RuntimeException ignored) {
            // radio restore must not hide the test failure
        }
    }

    /**
     * Simulates a first-time user tapping Allow on the system notification prompt.
     * Expected: the dialog closes and the language chooser becomes the first in-app screen.
     * This is the happy-path gate — without it, later first-launch modules never start.
     */
    @Test(description = "Notification permission ALLOW")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Cold launch shows the system notification dialog. ALLOW → app proceeds to language.")
    public void notificationPermissionAllow() {
        NotificationPermissionDialog dialog = new NotificationPermissionDialog();
        dialog.waitUntilLoaded();
        assertThat(dialog.isDisplayedNow())
                .as("Notification dialog: 'Allow L2B Vendor to send you notifications?'")
                .isTrue();
        dialog.tapAllow();
        LanguagePage language = new LanguagePage();
        language.waitUntilVisible(SPLASH_TIMEOUT);
        assertThat(language.isDisplayedNow()).as("Language screen after Allow").isTrue();
        language.attachScreenshot("splash-allow-language");
    }

    /**
     * Simulates a user tapping Don't allow on the notification prompt.
     * Expected: the app does not crash and still reaches the language screen.
     * Notifications are optional for first-launch; denying them must not block onboarding.
     */
    @Test(description = "Notification permission DENY")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Don't allow → app does not crash; first-launch continues to language.")
    public void notificationPermissionDeny() {
        NotificationPermissionDialog dialog = new NotificationPermissionDialog();
        dialog.waitUntilLoaded();
        dialog.tapDeny();
        LanguagePage language = new LanguagePage();
        language.waitUntilVisible(SPLASH_TIMEOUT);
        assertThat(language.isDisplayedNow()).as("Language screen after Don't allow").isTrue();
        language.attachScreenshot("splash-deny-language");
    }

    /**
     * Simulates dismissing the permission dialog with the device Back key (neither Allow nor Deny).
     * Expected: GrantPermissionsActivity is gone and language still appears; the app must not hang.
     * Users often dismiss system prompts with Back; that must not trap them on the splash gate.
     */
    @Test(description = "Notification permission dismissed with Back")
    @Severity(SeverityLevel.NORMAL)
    @Description("Back without Allow/Deny → dialog gone, app does not hang on GrantPermissionsActivity.")
    public void notificationPermissionDismissed() {
        NotificationPermissionDialog dialog = new NotificationPermissionDialog();
        dialog.waitUntilLoaded();
        dialog.dismissWithBack();
        LanguagePage language = new LanguagePage();
        language.waitUntilVisible(SPLASH_TIMEOUT);
        assertThat(language.isDisplayedNow())
                .as("Language screen after Back on permission dialog")
                .isTrue();
        assertThat(dialog.isDisplayedNow()).as("Permission dialog should be gone").isFalse();
        language.attachScreenshot("splash-dismiss-back-language");
    }

    /**
     * Simulates Allow and then waits for the first in-app screen.
     * Expected: language appears within 8 seconds (splash-specific timeout, not a generic wait).
     * A hung splash after permission grant is a blocker for every later first-launch flow.
     */
    @Test(description = "Splash proceeds within 8s")
    @Severity(SeverityLevel.CRITICAL)
    @Description("After Allow, language must appear within 8s. Timeout uses a splash-specific message, not a generic Wait timeout.")
    public void splashProceedsWithinTimeout() {
        NotificationPermissionDialog dialog = new NotificationPermissionDialog();
        dialog.waitUntilLoaded();
        dialog.tapAllow();
        new LanguagePage().waitUntilVisible(SPLASH_TIMEOUT);
    }

    /**
     * Simulates a cold launch with Wi-Fi and mobile data already off, then restoring radios and tapping Allow.
     * Expected (ideal): a clear offline or retry message. Expected (this assertion): no crash; the permission
     * dialog still appears; after radios return, Allow still reaches language.
     * NOTE: this asserts actual observed behavior. Missing offline UX is logged as BUGS_FOUND.docx #1 —
     * the app never shows an error/retry, only the same notification prompt.
     */
    @Test(description = "No network at launch")
    @Severity(SeverityLevel.NORMAL)
    @Description("Wifi+data off before launch: no crash. Permission or language still appears. Restore radios → Allow still reaches language.")
    public void noNetworkAtLaunchThenRecover() {
        NotificationPermissionDialog dialog = new NotificationPermissionDialog();
        dialog.waitUntilLoaded();
        assertThat(dialog.isDisplayedNow())
                .as("Offline cold launch still shows notification dialog (not a crash / blank spinner-only screen)")
                .isTrue();
        dialog.attachScreenshot("splash-offline-permission");
        Adb.enableRadios();
        dialog.tapAllow();
        LanguagePage language = new LanguagePage();
        language.waitUntilVisible(SPLASH_TIMEOUT);
        assertThat(language.isDisplayedNow()).as("Language after radios restored + Allow").isTrue();
    }

    /**
     * Intended to simulate splash when the QA backend is down, without taking qa.waardian.com offline for others.
     * Expected (when enabled): a controlled error rather than an infinite splash.
     * Ignored until a local stub exists — enabling this would interfere with the shared QA environment.
     */
    @Ignore("No isolated stub for QA.waardian.com; enabling this would interfere with the shared QA env.")
    @Test(description = "Backend unreachable at splash")
    @Severity(SeverityLevel.NORMAL)
    @Description("Placeholder: simulate backend down without touching shared QA. Not run until a local stub exists.")
    public void backendUnreachableAtSplash() {
        throw new UnsupportedOperationException("No safe backend-down injection yet");
    }

    /**
     * Simulates Home (background) then activateApp, then a force-stop and cold relaunch during the permission gate.
     * Expected: after background/foreground the dialog or language is still on screen; after force-stop the
     * notification dialog appears again (clean first launch). Users kill and reopen apps during splash all the time.
     */
    @Test(description = "Force-stop mid-splash then relaunch")
    @Severity(SeverityLevel.NORMAL)
    @Description("Background/foreground the permission dialog, then force-stop and relaunch: clean GrantPermissions UI.")
    public void forceStopThenRelaunchAndBackground() {
        NotificationPermissionDialog dialog = new NotificationPermissionDialog();
        dialog.waitUntilLoaded();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        // runAppInBackground() cannot be used here: first UI is GrantPermissionsActivity
        // (permissioncontroller), and Appium then fails to restart that system activity.
        android.pressKey(new KeyEvent(AndroidKey.HOME));
        android.activateApp(Config.get("app.package"));
        LanguagePage language = new LanguagePage();
        Waits.until(android,
                d -> (dialog.isDisplayedNow() || language.isDisplayedNow()) ? Boolean.TRUE : null,
                "After HOME + activateApp, neither permission dialog nor language appeared",
                Duration.ofSeconds(10));
        assertThat(dialog.isDisplayedNow() || language.isDisplayedNow())
                .as("After background/foreground, dialog or language is still on screen")
                .isTrue();

        Adb.forceStop(Config.get("app.package"));
        DriverManager.quit();
        DriverManager.set(DriverFactory.create(false, false));
        NotificationPermissionDialog again = new NotificationPermissionDialog();
        again.waitUntilLoaded();
        assertThat(again.isDisplayedNow())
                .as("Relaunch after force-stop shows notification dialog again (clean start)")
                .isTrue();
        again.attachScreenshot("splash-after-force-stop");
    }

    /**
     * Simulates rotating the device while the notification dialog is on screen.
     * Expected: no crash; if the app is portrait-locked, it stays portrait (landscape layout is not asserted).
     * Rotation during a system overlay is a common crash site on first launch.
     */
    @Test(description = "Rotation during splash")
    @Severity(SeverityLevel.MINOR)
    @Description("If portrait-locked, stay portrait and do not crash. Landscape layout is not asserted when locked.")
    public void rotationDuringSplash() {
        NotificationPermissionDialog dialog = new NotificationPermissionDialog();
        dialog.waitUntilLoaded();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        ScreenOrientation before = android.getOrientation();
        try {
            android.rotate(ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            assertThat(dialog.isDisplayedNow()).as("Dialog still up after rotate threw").isTrue();
            return;
        }
        ScreenOrientation after = android.getOrientation();
        assertThat(dialog.isDisplayedNow() || new LanguagePage().isDisplayedNow())
                .as("No crash after rotation attempt")
                .isTrue();
        if (after == ScreenOrientation.PORTRAIT && before == ScreenOrientation.PORTRAIT) {
            dialog.attachScreenshot("splash-portrait-locked");
            return;
        }
        android.rotate(ScreenOrientation.PORTRAIT);
        assertThat(dialog.isDisplayedNow() || new LanguagePage().isDisplayedNow()).isTrue();
    }
}
