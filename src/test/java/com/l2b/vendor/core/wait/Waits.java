package com.l2b.vendor.core.wait;

import com.l2b.vendor.environment.Config;
import java.time.Duration;
import java.util.function.Function;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Port of Customer {@code Waits}: the only place waits are written. No {@code Thread.sleep}.
 */
public final class Waits {

    private Waits() {
    }

    private static Duration defaultTimeout() {
        return Config.getSeconds("timeout.explicit", 20);
    }

    public static WebElement visible(WebDriver driver, WebElement element) {
        return visible(driver, element, defaultTimeout());
    }

    public static WebElement visible(WebDriver driver, WebElement element, Duration timeout) {
        return new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOf(element));
    }

    public static WebElement clickable(WebDriver driver, WebElement element) {
        return new WebDriverWait(driver, defaultTimeout())
                .until(ExpectedConditions.elementToBeClickable(element));
    }

    public static <T> T until(WebDriver driver, Function<WebDriver, T> condition, String description) {
        return until(driver, condition, description, defaultTimeout());
    }

    public static <T> T until(WebDriver driver, Function<WebDriver, T> condition,
                              String description, Duration timeout) {
        return new FluentWait<>(driver)
                .withTimeout(timeout)
                .pollingEvery(Duration.ofMillis(250))
                .ignoring(NoSuchElementException.class, StaleElementReferenceException.class)
                .withMessage(description)
                .until(condition);
    }
}
