package com.l2b.vendor.environment;

import com.l2b.vendor.environment.Config;
import com.l2b.vendor.core.driver.DriverManager;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestListener;
import org.testng.ITestResult;

/** Failure screenshot + hierarchy dump — same idea as Customer {@code TestListener}. */
public class TestListener implements ITestListener {

    private static final Logger LOG = LogManager.getLogger(TestListener.class);

    @Override
    public void onTestStart(ITestResult result) {
        LOG.info("START {}", name(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("PASS  {} ({} ms)", name(result), duration(result));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOG.error("FAIL  {} ({} ms)", name(result), duration(result), result.getThrowable());
        if (Config.getBoolean("screenshot.on.failure", true)) {
            captureScreenshot(name(result));
        }
        capturePageSource(name(result));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.warn("SKIP  {}", name(result));
    }

    private void captureScreenshot(String testName) {
        if (!DriverManager.hasDriver()) {
            return;
        }
        try {
            byte[] png = ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(testName + " (failure)", new ByteArrayInputStream(png));
            Path dir = Path.of(Config.get("screenshot.dir", "screenshots"));
            Files.createDirectories(dir);
            Files.write(dir.resolve(testName.replaceAll("[^a-zA-Z0-9._-]", "_") + ".png"), png);
        } catch (Exception e) {
            LOG.warn("Could not capture screenshot: {}", e.getMessage());
        }
    }

    private void capturePageSource(String testName) {
        if (!DriverManager.hasDriver()) {
            return;
        }
        try {
            Allure.addAttachment(testName + " (hierarchy)", "application/xml",
                    DriverManager.get().getPageSource(), ".xml");
        } catch (Exception e) {
            LOG.debug("Could not capture page source: {}", e.getMessage());
        }
    }

    private static String name(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
    }

    private static long duration(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }
}
