package com.l2b.vendor.modules.fleet.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;
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
 * Dump-0: Rental Fleet on {@code 9000000001}. Isolated suite. Reach usable Home,
 * then open Fleet via bottom tab, Active Fleet See all, and drawer Your machines.
 * Do not tap Add Machine submit, Change operator confirm, Accept, Decline, or Log Out.
 */
@Epic("Vendor app")
@Feature("Fleet dump — rental vendor 9000000001")
public class FleetDump extends RentalHomeBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-fleet-0001-20260925");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Dump-0: Fleet chrome via tab + See all + drawer Your machines")
    @Description("9000000001. Close QB once. Dump Fleet from bottom tab, Active Fleet See all, "
            + "and Profile → Your machines. Read-only — no Add Machine submit / operator confirm.")
    public void dumpFleet0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        writeDump("00-home", classifyRentalNow());

        home.tapDesc("Fleet");
        sleepQuiet(800);
        String viaTab = classifyRentalNow();
        Allure.parameter("viaTab", viaTab);
        writeDump("01-fleet-tab", viaTab);

        // Return Home then Active Fleet See all (index 0)
        if (!"home".equals(classifyRentalNow())) {
            home.tapDesc("Home");
            sleepQuiet(600);
        }
        home = new HomePage();
        home.tapNthSeeAll(0);
        sleepQuiet(800);
        String viaSeeAll = classifyRentalNow();
        Allure.parameter("viaSeeAll", viaSeeAll);
        writeDump("02-active-fleet-see-all", viaSeeAll);

        // Drawer → Your machines
        if (!"home".equals(classifyRentalNow())) {
            try {
                home.tapDesc("Home");
            } catch (RuntimeException ignored) {
                DriverManager.get().navigate().back();
            }
            sleepQuiet(600);
        }
        home = new HomePage();
        home.tapDesc("Profile");
        sleepQuiet(500);
        writeDump("03-drawer", classifyRentalNow());
        try {
            home.tapText("Your machines");
        } catch (RuntimeException e) {
            Allure.parameter("yourMachinesTap", e.getClass().getSimpleName());
            try {
                home.tapText("Your Machines");
            } catch (RuntimeException e2) {
                Allure.parameter("yourMachinesTap2", e2.getClass().getSimpleName());
            }
        }
        sleepQuiet(800);
        String viaDrawer = classifyRentalNow();
        Allure.parameter("viaDrawer", viaDrawer);
        writeDump("04-drawer-your-machines", viaDrawer);

        Allure.addAttachment("dump-dir", "text/plain", DUMP_DIR.toString());
    }

    private void writeDump(String name, String classified) {
        Path dir = DUMP_DIR.resolve(name);
        try {
            Files.createDirectories(dir);
            String xml = safeSource();
            Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("texts.txt"), extract(xml), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("classified.txt"), classified, StandardCharsets.UTF_8);
            byte[] png = ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES);
            Files.write(dir.resolve("screen.png"), png);
            Allure.addAttachment(name + "-screen", "image/png",
                    new java.io.ByteArrayInputStream(png), ".png");
            Allure.addAttachment(name + "-texts", "text/plain", extract(xml));
        } catch (IOException e) {
            throw new IllegalStateException("Fleet dump write failed: " + name, e);
        }
    }

    private static String extract(String xml) {
        Set<String> out = new LinkedHashSet<>();
        Matcher t = TEXT.matcher(xml);
        while (t.find()) {
            out.add("text=" + t.group(1));
        }
        Matcher d = DESC.matcher(xml);
        while (d.find()) {
            out.add("desc=" + d.group(1));
        }
        return String.join("\n", out);
    }

    private String safeSource() {
        try {
            return ((AndroidDriver) DriverManager.get()).getPageSource();
        } catch (Exception e) {
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
