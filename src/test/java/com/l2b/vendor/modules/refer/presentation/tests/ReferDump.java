package com.l2b.vendor.modules.refer.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.refer.presentation.pages.ReferPage;
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

/**
 * Dump-1: Refer & Earn from Profile drawer on {@code 9000000001}.
 * Never Accept / Decline / Log Out Confirm. Refer now → dismiss share only.
 */
@Epic("Vendor app")
@Feature("Refer dump — rental vendor 9000000001")
public class ReferDump extends ReferBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-refer-0001-20261003");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
    }

    @Test(description = "Dump-1: Refer & Earn chrome + Copy / Refer now probes")
    @Description("9000000001. Profile → Refer & Earn. Dump landing, scroll, Copy code, "
            + "Refer now (share dismiss). Back only.")
    public void dumpRefer0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = ensureRentalHomeWarm();
        writeDump("00-home", classifyRentalNow());

        home.tapDesc("Profile");
        sleepQuiet(1000);
        writeDump("01-drawer", classifyRentalNow());

        new ProfileDrawerPage().tapRow("Refer & Earn");
        sleepQuiet(1200);
        writeDump("02-refer", classifyRentalNow());

        ReferPage page = new ReferPage();
        if (page.isRetryVisible()) {
            page.tapRetry();
            sleepQuiet(2000);
            writeDump("02b-after-retry", classifyRentalNow());
        }

        swipeUp();
        sleepQuiet(600);
        writeDump("03-refer-scrolled", classifyRentalNow());

        swipeDown();
        sleepQuiet(500);

        if (page.isCopyCodeVisible()) {
            page.tapCopyCode();
            sleepQuiet(900);
            writeDump("04-after-copy", classifyRentalNow());
        }

        if (page.isReferNowVisible()) {
            page.tapReferNow();
            sleepQuiet(1500);
            writeDump("05-refer-now", classifyRentalNow());
            DriverManager.get().navigate().back();
            sleepQuiet(900);
        }
        writeDump("06-after-refer-now-back", classifyRentalNow());

        if (page.isBackVisible()) {
            page.tapBack();
            sleepQuiet(900);
        }
        writeDump("07-after-back", classifyRentalNow());

        Allure.parameter("dumpDir", DUMP_DIR.toString());
        Allure.parameter("unavailable", String.valueOf(page.isUnavailableVisible()));
    }

    private void writeDump(String name, String named) throws IOException {
        String src = DriverManager.get().getPageSource();
        Files.writeString(DUMP_DIR.resolve(name + ".xml"), src, StandardCharsets.UTF_8);
        byte[] png = ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES);
        Files.write(DUMP_DIR.resolve(name + ".png"), png);
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

    private void swipeUp() {
        DriverManager.get().executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 1400, "width", 600, "height", 700,
                        "direction", "up", "percent", 0.75));
    }

    private void swipeDown() {
        DriverManager.get().executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 800, "width", 600, "height", 700,
                        "direction", "down", "percent", 0.75));
    }
}
