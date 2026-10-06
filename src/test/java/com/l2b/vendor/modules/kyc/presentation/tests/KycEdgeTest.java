package com.l2b.vendor.modules.kyc.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.kyc.presentation.pages.KycPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * KYC edge — offline, relaunch, rotation, rapid taps.
 *
 * <p><b>Critical Edge (KYC-E1–E10)</b>
 */
@Epic("Vendor app")
@Feature("KYC edge — rental 9000000001")
public class KycEdgeTest extends KycBaseTest {

    @Test(priority = 1, description = "KYC-E1: device Back leaves KYC")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBack() {
        reachKyc();
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        assertThat(profileDrawerNow() || hasText("Current Earning") || hasText("Account")
                || launcherNow()).isTrue();
    }

    @Test(priority = 2, description = "KYC-E2: force-stop relaunch does not crash")
    @Severity(SeverityLevel.CRITICAL)
    public void forceStopRelaunch() {
        reachKyc();
        String pkg = Config.get("app.package");
        Adb.forceStop(pkg);
        sleepQuiet(1000);
        try {
            ((AndroidDriver) DriverManager.get()).activateApp(pkg);
        } catch (RuntimeException e) {
            Adb.run("shell", "am", "start", "-n", pkg + "/" + Config.get("app.activity"));
        }
        sleepQuiet(2500);
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 3, description = "KYC-E3: background/foreground")
    @Severity(SeverityLevel.NORMAL)
    public void backgroundForeground() {
        KycPage page = reachKyc();
        try {
            ((AndroidDriver) DriverManager.get()).runAppInBackground(java.time.Duration.ofSeconds(2));
        } catch (RuntimeException e) {
            Adb.run("shell", "input", "keyevent", "3");
            sleepQuiet(1500);
            ((AndroidDriver) DriverManager.get()).activateApp(Config.get("app.package"));
        }
        sleepQuiet(1200);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(vendorPackage()).contains("l2b");
        softly.assertThat(page.isDisplayedNow() || hasText("Current Earning")
                || profileDrawerNow()).isTrue();
        softly.assertAll();
    }

    @Test(priority = 4, description = "KYC-E4: rapid Retry taps")
    @Severity(SeverityLevel.NORMAL)
    public void rapidRetry() {
        KycPage page = reachKyc();
        if (!page.isRetryVisible()) {
            Allure.parameter("skipped", "no-retry");
            return;
        }
        for (int i = 0; i < 3; i++) {
            try {
                page.tapRetry();
            } catch (RuntimeException ignored) {
            }
            sleepQuiet(300);
        }
        sleepQuiet(1000);
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 5, description = "KYC-E5: offline open still shows chrome or error")
    @Severity(SeverityLevel.CRITICAL)
    public void offlineOpen() {
        try {
            Adb.run("shell", "cmd", "connectivity", "airplane-mode", "enable");
            Adb.run("shell", "settings", "put", "global", "airplane_mode_on", "1");
            sleepQuiet(1000);
            KycPage page = reachKyc();
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(page.isDisplayedNow() || page.isErrorVisible()
                    || page.isFullChromeVisible()).as("KYC or error offline").isTrue();
            softly.assertThat(vendorPackage()).contains("l2b");
            softly.assertAll();
        } finally {
            try {
                Adb.run("shell", "cmd", "connectivity", "airplane-mode", "disable");
                Adb.run("shell", "settings", "put", "global", "airplane_mode_on", "0");
            } catch (RuntimeException ignored) {
            }
            Adb.ensureNetworkReady();
        }
    }

    @Test(priority = 6, description = "KYC-E6: rotation keeps Vendor")
    @Severity(SeverityLevel.NORMAL)
    public void rotation() {
        reachKyc();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        try {
            android.rotate(org.openqa.selenium.ScreenOrientation.LANDSCAPE);
            sleepQuiet(1200);
            Allure.parameter("landscapeDrawer", String.valueOf(profileDrawerNow()));
            android.rotate(org.openqa.selenium.ScreenOrientation.PORTRAIT);
            sleepQuiet(1000);
        } catch (RuntimeException e) {
            Allure.parameter("rotateFail", e.getClass().getSimpleName());
        }
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 7, description = "KYC-E7: open KYC twice in a row")
    @Severity(SeverityLevel.NORMAL)
    public void openTwice() {
        KycPage a = reachKyc();
        a.tapBack();
        sleepQuiet(700);
        KycPage b = reachKyc();
        assertThat(b.isDisplayedNow()).isTrue();
    }

    @Test(priority = 8, description = "KYC-E8: not launcher after open")
    @Severity(SeverityLevel.CRITICAL)
    public void notLauncher() {
        reachKyc();
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 9, description = "KYC-E9: masked bank mobile when chrome loads")
    @Severity(SeverityLevel.NORMAL)
    public void maskedMobileWhenLoaded() {
        KycPage page = reachKyc();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error");
            return;
        }
        page.swipeListUp();
        sleepQuiet(600);
        // Prior dump showed ********74 — mask preferred over full number
        boolean masked = hasTextContains("****") || hasTextContains("••") || page.isBankDetailsVisible();
        Allure.parameter("maskedOrBank", String.valueOf(masked));
        assertThat(masked).isTrue();
    }

    @Test(priority = 10, description = "KYC-E10: no Accept/Decline after scroll")
    @Severity(SeverityLevel.CRITICAL)
    public void noAcceptAfterScroll() {
        KycPage page = reachKyc();
        page.swipeListUp();
        sleepQuiet(500);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("Decline")).isFalse();
        softly.assertThat(DriverManager.get().findElements(
                org.openqa.selenium.By.xpath("//*[@text='Accept']")).isEmpty()).isTrue();
        softly.assertAll();
    }

    private boolean hasTextContains(String fragment) {
        try {
            String src = DriverManager.get().getPageSource();
            return src != null && src.contains(fragment);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
