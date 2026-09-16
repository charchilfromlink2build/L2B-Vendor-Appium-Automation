package com.l2b.vendor.core.ui;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import io.qameta.allure.Allure;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

/**
 * Shared page helper for Vendor screens: Appium driver, taps, presence checks, Allure screenshots.
 * Subclasses hold locators and actions only — assertions stay in the test classes.
 */
public abstract class SplashScreen {

    protected final AppiumDriver driver;
    protected final Logger log = LogManager.getLogger(getClass());

    /** Binds this page to the current Appium session and initializes {@code @AndroidFindBy} fields. */
    protected SplashScreen() {
        this.driver = DriverManager.get();
        PageFactory.initElements(
                new AppiumFieldDecorator(driver, Duration.ofSeconds(1)),
                this);
    }

    /** Block until this screen's primary marker is visible. */
    public abstract void waitUntilLoaded();

    /** Wait until the element is clickable, then tap it. */
    protected void tap(WebElement element) {
        Waits.clickable(driver, element).click();
    }

    /** True if the element becomes visible within 3 seconds; false on timeout (does not throw). */
    protected boolean isDisplayed(WebElement element) {
        try {
            return Waits.visible(driver, element, Duration.ofSeconds(3)).isDisplayed();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Instant presence check (~100ms). Does not use the page-factory decorator timeout. */
    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    /** Attach a PNG screenshot to the Allure step with the given name. */
    public void attachScreenshot(String name) {
        byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), "png");
    }

    /** System notification prompt on API 33+ after a data-clear. Taps Allow if the dialog is present. */
    protected void dismissNotificationPromptIfPresent() {
        try {
            By allow = By.id("com.android.permissioncontroller:id/permission_allow_button");
            if (!driver.findElements(allow).isEmpty()) {
                driver.findElement(allow).click();
                log.info("System permission dialog was present — dismissed it");
            }
        } catch (RuntimeException e) {
            log.debug("No notification prompt: {}", e.getMessage());
        }
    }
}
