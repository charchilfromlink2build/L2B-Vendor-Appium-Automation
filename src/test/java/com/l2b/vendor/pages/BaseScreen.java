package com.l2b.vendor.pages;

import com.l2b.vendor.utils.DriverManager;
import com.l2b.vendor.utils.Waits;
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

/** Port of Customer {@code BaseScreen}: locators + taps, no assertions. */
public abstract class BaseScreen {

    protected final AppiumDriver driver;
    protected final Logger log = LogManager.getLogger(getClass());

    protected BaseScreen() {
        this.driver = DriverManager.get();
        PageFactory.initElements(
                new AppiumFieldDecorator(driver, Duration.ofSeconds(1)),
                this);
    }

    public abstract void waitUntilLoaded();

    protected void tap(WebElement element) {
        Waits.clickable(driver, element).click();
    }

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

    public void attachScreenshot(String name) {
        byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), "png");
    }

    /** System notification prompt on API 33+ after a data-clear. */
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
