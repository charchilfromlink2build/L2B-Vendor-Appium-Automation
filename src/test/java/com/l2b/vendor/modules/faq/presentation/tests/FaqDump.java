package com.l2b.vendor.modules.faq.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.modules.faq.presentation.pages.FaqPage;
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
@Feature("FAQ dump — rental 9000000001")
public class FaqDump extends FaqBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-faq-0001-20261003");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,200})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
    }

    @Test(description = "Dump-1: FAQ list / error + Retry / expand probes")
    @Description("9000000001. Profile → FAQ. Expand/Collapse or Retry only.")
    public void dumpFaq0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = ensureRentalHomeWarm();
        writeDump("00-home", classifyRentalNow());
        home.tapDesc("Profile");
        sleepQuiet(1000);
        writeDump("01-drawer", classifyRentalNow());
        new ProfileDrawerPage().tapRow("FAQ");
        sleepQuiet(1200);
        writeDump("02-faq", classifyRentalNow());

        FaqPage page = new FaqPage();
        Allure.parameter("errorState", String.valueOf(page.isErrorVisible()));
        Allure.parameter("fullList", String.valueOf(page.isFullListVisible()));

        if (page.isRetryVisible() && page.isErrorVisible()) {
            page.tapRetry();
            sleepQuiet(2000);
            writeDump("03-after-retry", classifyRentalNow());
        }

        if (page.isQuestionVisible(FaqPage.Q_REVIEW)) {
            page.tapQuestion(FaqPage.Q_REVIEW);
            sleepQuiet(800);
            writeDump("04-expanded-review", classifyRentalNow());
        } else if (page.hasExpandControl()) {
            page.tapFirstExpand();
            sleepQuiet(800);
            writeDump("04-expanded-first", classifyRentalNow());
        }

        if (page.hasCollapseControl()) {
            page.tapFirstCollapse();
            sleepQuiet(700);
            writeDump("05-collapsed", classifyRentalNow());
        }

        page.swipeListUp();
        sleepQuiet(600);
        writeDump("06-scrolled", classifyRentalNow());

        if (page.isQuestionVisible(FaqPage.Q_START)) {
            page.tapQuestion(FaqPage.Q_START);
            sleepQuiet(800);
            writeDump("07-expanded-start", classifyRentalNow());
        }

        if (page.isBackVisible()) {
            page.tapBack();
            sleepQuiet(900);
        }
        writeDump("08-after-back", classifyRentalNow());
        Allure.parameter("dumpDir", DUMP_DIR.toString());
        Allure.parameter("unavailable", String.valueOf(page.isErrorVisible()));
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
