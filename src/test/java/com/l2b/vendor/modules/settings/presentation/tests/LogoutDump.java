package com.l2b.vendor.modules.settings.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.annotations.Test;

@Epic("Vendor app")
@Feature("Log Out Confirm dump — rental 9000000001")
public class LogoutDump extends LogoutBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-logout-0001-20261005");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,200})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
    }

    @Test(description = "Dump-1: Log Out Confirm + immediate + force-stop relaunch session")
    @Description("9000000001. Confirm Log out. Probe if prior session remains (user-reported).")
    public void dumpLogoutConfirm0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = ensureRentalHomeWarm();
        writeDump("00-home", classifyRentalNow());
        home.tapDesc("Profile");
        sleepQuiet(1000);
        writeDump("01-drawer", classifyRentalNow());

        ProfileDrawerPage page = new ProfileDrawerPage();
        page.tapLogOut();
        sleepQuiet(900);
        writeDump("02-dialog", classifyRentalNow());

        page.tapLogoutConfirm();
        sleepQuiet(2500);
        writeDump("03-after-confirm", classifyRentalNow());
        Allure.parameter("afterConfirmLoggedIn", String.valueOf(looksStillLoggedIn()));
        Allure.parameter("afterConfirmLoggedOutUi", String.valueOf(looksLoggedOutUi()));

        // User report: after Confirm, device Back returns to prior active Home session.
        DriverManager.get().navigate().back();
        sleepQuiet(1500);
        writeDump("03b-after-confirm-back", classifyRentalNow());
        boolean stickyAfterBack = looksStillLoggedIn();
        Allure.parameter("afterConfirmBackLoggedIn", String.valueOf(stickyAfterBack));
        Allure.parameter("afterConfirmBackLoggedOutUi", String.valueOf(looksLoggedOutUi()));
        Allure.parameter("bugBackSticky", stickyAfterBack ? "53" : "none");

        // If Back restored Home, leave dump evidence and do not force-stop yet.
        if (!stickyAfterBack) {
            forceStopRelaunch();
            writeDump("04-after-force-stop-relaunch", classifyRentalNow());
            Allure.parameter("relaunchLoggedIn", String.valueOf(looksStillLoggedIn()));
            Allure.parameter("relaunchLoggedOutUi", String.valueOf(looksLoggedOutUi()));
        } else {
            writeDump("04-skipped-force-stop-sticky-after-back", classifyRentalNow());
        }

        // Restore for later suites (must not fail the dump evidence)
        try {
            if (looksLoggedOutUi() || !looksStillLoggedIn()) {
                restoreSession0001();
                writeDump("05-restored", classifyRentalNow());
            }
        } catch (RuntimeException e) {
            Allure.parameter("restoreFail", e.getClass().getSimpleName() + ": " + e.getMessage());
            writeDump("05-restore-failed", classifyRentalNow());
        }
        Allure.parameter("dumpDir", DUMP_DIR.toString());
        Allure.parameter("sessionClearedOnConfirm",
                String.valueOf(!looksStillLoggedIn() || looksLoggedOutUi()));
        Allure.parameter("sessionStickyOnBackAfterConfirm", String.valueOf(stickyAfterBack));
    }

    private void writeDump(String name, String named) throws IOException {
        String src = DriverManager.get().getPageSource();
        Files.writeString(DUMP_DIR.resolve(name + ".xml"), src, StandardCharsets.UTF_8);
        Files.write(DUMP_DIR.resolve(name + ".png"),
                ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
        Set<String> texts = new LinkedHashSet<>();
        Matcher tm = TEXT.matcher(src);
        while (tm.find()) {
            if (!tm.group(1).isBlank()) {
                texts.add(tm.group(1));
            }
        }
        Set<String> descs = new LinkedHashSet<>();
        Matcher dm = DESC.matcher(src);
        while (dm.find()) {
            if (!dm.group(1).isBlank()) {
                descs.add(dm.group(1));
            }
        }
        String summary = "named=" + named
                + "\nloggedIn=" + looksStillLoggedIn()
                + "\nloggedOutUi=" + looksLoggedOutUi()
                + "\ntexts=" + texts
                + "\ndescs=" + descs + "\n";
        Files.writeString(DUMP_DIR.resolve(name + ".txt"), summary, StandardCharsets.UTF_8);
        Allure.addAttachment(name + "-summary", "text/plain", summary);
        Allure.parameter(name + "Pkg",
                ((AndroidDriver) DriverManager.get()).getCurrentPackage());
    }
}
