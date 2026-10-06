package com.l2b.vendor.modules.kyc.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.kyc.presentation.pages.KycPage;
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
@Feature("KYC dump — rental 9000000001")
public class KycDump extends KycBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-kyc-0001-20261006");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,200})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
    }

    @Test(description = "Dump-1: Profile → KYC Details + scroll/retry")
    @Description("9000000001. Profile → KYC. Scroll/Retry only — never Accept/Decline/Log Out Confirm.")
    public void dumpKyc0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = ensureRentalHomeWarm();
        writeDump("00-home", classifyRentalNow());
        home.tapDesc("Profile");
        sleepQuiet(1000);
        writeDump("01-drawer", classifyRentalNow());
        new ProfileDrawerPage().tapRow("KYC");
        sleepQuiet(1200);
        writeDump("02-kyc", classifyRentalNow());

        KycPage page = new KycPage();
        Allure.parameter("errorState", String.valueOf(page.isErrorVisible()));
        Allure.parameter("fullChrome", String.valueOf(page.isFullChromeVisible()));
        Allure.parameter("docs", String.valueOf(page.isDocumentDetailsVisible()));
        Allure.parameter("bank", String.valueOf(page.isBankDetailsVisible()));
        Allure.parameter("emptyDash", String.valueOf(page.isEmptyDashStateVisible()));
        Allure.parameter("panValue", String.valueOf(page.isPresentPanValue()));

        if (page.isRetryVisible() && page.isErrorVisible()) {
            page.tapRetry();
            sleepQuiet(2000);
            writeDump("03-after-retry", classifyRentalNow());
        }

        page.swipeListUp();
        sleepQuiet(700);
        writeDump("04-scrolled", classifyRentalNow());

        if (page.isBackVisible()) {
            page.tapBack();
            sleepQuiet(900);
        }
        writeDump("05-after-back", classifyRentalNow());
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
