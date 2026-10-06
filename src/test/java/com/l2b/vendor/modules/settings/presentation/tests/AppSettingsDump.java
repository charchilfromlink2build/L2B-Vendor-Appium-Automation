package com.l2b.vendor.modules.settings.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.AppSettingsPage;
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
@Feature("App Settings dump — rental 9000000001")
public class AppSettingsDump extends AppSettingsBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-app-settings-0001-20261005");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,200})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
    }

    @Test(description = "Dump-1: Settings permissions + toggle probe")
    @Description("9000000001. Profile → Settings. Toggle once then restore. Never Log Out Confirm.")
    public void dumpAppSettings0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = ensureRentalHomeWarm();
        writeDump("00-home", classifyRentalNow());
        home.tapDesc("Profile");
        sleepQuiet(1000);
        writeDump("01-drawer", classifyRentalNow());
        new ProfileDrawerPage().tapRow("Settings");
        sleepQuiet(1200);
        writeDump("02-settings", classifyRentalNow());

        AppSettingsPage page = new AppSettingsPage();
        Allure.parameter("errorState", String.valueOf(page.isErrorVisible()));
        Allure.parameter("fullChrome", String.valueOf(page.isFullChromeVisible()));
        Allure.parameter("switchCount", String.valueOf(page.switchCount()));

        if (page.isRetryVisible() && page.isErrorVisible()) {
            page.tapRetry();
            sleepQuiet(2000);
            writeDump("03-after-retry", classifyRentalNow());
        }

        if (page.switchCount() > 0) {
            Boolean before = page.firstSwitchChecked();
            Allure.parameter("firstSwitchBefore", String.valueOf(before));
            page.tapFirstSwitch();
            sleepQuiet(900);
            writeDump("04-after-toggle", classifyRentalNow());
            page.tapFirstSwitch();
            sleepQuiet(900);
            writeDump("05-restored", classifyRentalNow());
        }

        page.swipeListUp();
        sleepQuiet(700);
        writeDump("06-scrolled", classifyRentalNow());

        if (page.isBackVisible()) {
            page.tapBack();
            sleepQuiet(900);
        }
        writeDump("07-after-back", classifyRentalNow());
        Allure.parameter("dumpDir", DUMP_DIR.toString());
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
        String summary = "named=" + named + "\ntexts=" + texts + "\ndescs=" + descs + "\n";
        Files.writeString(DUMP_DIR.resolve(name + ".txt"), summary, StandardCharsets.UTF_8);
        Allure.addAttachment(name + "-summary", "text/plain", summary);
        Allure.parameter(name + "Pkg",
                ((AndroidDriver) DriverManager.get()).getCurrentPackage());
    }
}
