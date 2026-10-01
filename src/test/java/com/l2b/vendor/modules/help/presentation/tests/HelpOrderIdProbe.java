package com.l2b.vendor.modules.help.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.help.presentation.pages.HelpSupportPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/** Order ID sheet probe. Never Chat now. */
public class HelpOrderIdProbe extends RentalHomeBaseTest {

    private static final Path DIR = Path.of("/tmp/l2b-help-orderid-0001-20261001");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,200})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test
    public void probeOrderId() throws Exception {
        Files.createDirectories(DIR);
        reachUsableRentalHomeOrFailExtendTime();
        new HomePage().tapDesc("Profile");
        sleepQuiet(900);
        new ProfileDrawerPage().tapRow("Help");
        sleepQuiet(1100);
        HelpSupportPage help = new HelpSupportPage();
        help.tapLiveChat();
        sleepQuiet(900);
        help.tapTicketConcern();
        sleepQuiet(800);
        clickContains("Payment");
        sleepQuiet(900);
        dump("01-after-concern");
        clickExact("Order ID");
        sleepQuiet(1100);
        dump("02-order-sheet");
        for (WebElement el : DriverManager.get().findElements(By.className("android.widget.TextView"))) {
            String t = attr(el);
            if (t.startsWith("L2B-")) {
                clickGesture(el);
                Allure.parameter("orderPicked", t);
                break;
            }
        }
        sleepQuiet(900);
        dump("03-after-order");
        var parents = DriverManager.get().findElements(By.xpath(
                "//android.view.View[@clickable='true'][.//android.widget.TextView[@text='Chat now']]"));
        String en = parents.isEmpty() ? "missing" : parents.get(0).getAttribute("enabled");
        Files.writeString(DIR.resolve("03-after-order/chat-enabled.txt"),
                "enabled=" + en, StandardCharsets.UTF_8);
        DriverManager.get().navigate().back();
        sleepQuiet(700);
    }

    private void dump(String name) throws Exception {
        Path d = DIR.resolve(name);
        Files.createDirectories(d);
        String xml = ((AndroidDriver) DriverManager.get()).getPageSource();
        Files.writeString(d.resolve("window.xml"), xml, StandardCharsets.UTF_8);
        Set<String> out = new LinkedHashSet<>();
        Matcher m = TEXT.matcher(xml);
        while (m.find()) {
            out.add(m.group(1));
        }
        Files.writeString(d.resolve("texts.txt"), String.join("\n", out), StandardCharsets.UTF_8);
        Files.write(d.resolve("screen.png"),
                ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
    }

    private void clickExact(String text) {
        List<WebElement> els = DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[@text='" + text + "']"));
        if (!els.isEmpty()) {
            clickGesture(els.get(0));
        }
    }

    private void clickContains(String fragment) {
        List<WebElement> els = DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[contains(@text,'" + fragment + "')]"));
        if (!els.isEmpty()) {
            clickGesture(els.get(0));
        }
    }

    private void clickGesture(WebElement el) {
        Rectangle r = el.getRect();
        DriverManager.get().executeScript("mobile: clickGesture",
                Map.of("x", r.x + Math.max(1, r.width / 2), "y", r.y + Math.max(1, r.height / 2)));
    }

    private static String attr(WebElement el) {
        try {
            String t = el.getAttribute("text");
            return t == null ? "" : t.trim();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
