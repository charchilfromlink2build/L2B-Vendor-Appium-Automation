package com.l2b.vendor.modules.faq.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * FAQ from Profile drawer. Dump {@code /tmp/l2b-faq-0001-20261003}.
 * Live 3 Oct: error empty state &quot;Something went wrong. Please try again.&quot; + Retry
 * (BUGS_FOUND #48). Expand/Collapse when list loads — never Log Out Confirm / Accept / Decline.
 */
public class FaqPage extends SplashScreen {

    public static final String Q_REVIEW = "Why is my account still under review?";
    public static final String Q_PAID = "When do I get paid for a completed job?";
    public static final String Q_START = "How do I start a booked job?";
    public static final String Q_CANCEL = "What happens if a customer cancels?";
    public static final String Q_HUMAN = "How do I reach a human?";
    public static final String Q_HUMAN_ALT = "How do I talk to a human?";
    public static final String ERR_MSG = "Something went wrong. Please try again.";

    @Override
    @Step("Wait for FAQ")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "FAQ screen did not appear", Duration.ofSeconds(20));
    }

    @Step("Check FAQ identity")
    public boolean isDisplayedNow() {
        return isFaqTitleVisible()
                || isErrorVisible()
                || isPresent(ComposeLocators.textView(Q_START))
                || isPresent(ComposeLocators.textView(Q_CANCEL))
                || isPresent(ComposeLocators.textView(Q_REVIEW));
    }

    @Step("Check FAQs title")
    public boolean isFaqTitleVisible() {
        return isPresent(ComposeLocators.textView("FAQs"))
                || isPresent(ComposeLocators.textView("FAQ"));
    }

    @Step("Check FAQ load error empty state")
    public boolean isErrorVisible() {
        return isPresent(ComposeLocators.textView(ERR_MSG))
                || isPresent(ComposeLocators.textViewContains("Something went wrong"));
    }

    @Step("Check Retry CTA")
    public boolean isRetryVisible() {
        return isPresent(ComposeLocators.textView("Retry"));
    }

    @Step("Full FAQ list chrome (questions present)")
    public boolean isFullListVisible() {
        return isQuestionVisible(Q_REVIEW)
                || isQuestionVisible(Q_START)
                || isQuestionVisible(Q_CANCEL)
                || isQuestionVisible(Q_PAID);
    }

    @Step("Tap Retry")
    public void tapRetry() {
        WebElement el = driver.findElement(ComposeLocators.textView("Retry"));
        tap(el);
    }

    @Step("Check question visible")
    public boolean isQuestionVisible(String question) {
        return isPresent(ComposeLocators.textView(question));
    }

    @Step("Check Expand controls")
    public boolean hasExpandControl() {
        return !driver.findElements(By.xpath("//*[@content-desc='Expand']")).isEmpty();
    }

    @Step("Check Collapse controls")
    public boolean hasCollapseControl() {
        return !driver.findElements(By.xpath("//*[@content-desc='Collapse']")).isEmpty();
    }

    @Step("Check Back")
    public boolean isBackVisible() {
        return isPresent(By.xpath("//*[@content-desc='Back']"));
    }

    @Step("Header Back left-aligned")
    public boolean headerBackAligned() {
        List<WebElement> backs = driver.findElements(By.xpath("//*[@content-desc='Back']"));
        if (backs.isEmpty()) {
            return false;
        }
        Rectangle r = backs.get(0).getRect();
        return r.x < 120 && r.y < 220;
    }

    @Step("Tap Back")
    public void tapBack() {
        tap(driver.findElement(By.xpath("//*[@content-desc='Back']")));
    }

    @Step("Device Back")
    public void pressDeviceBack() {
        driver.navigate().back();
    }

    @Step("Tap question row")
    public void tapQuestion(String question) {
        tap(driver.findElement(ComposeLocators.textView(question)));
    }

    @Step("Tap first Expand")
    public void tapFirstExpand() {
        List<WebElement> els = driver.findElements(By.xpath("//*[@content-desc='Expand']"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Expand not visible");
        }
        Rectangle r = els.get(0).getRect();
        driver.executeScript("mobile: clickGesture",
                java.util.Map.of("x", r.x + Math.max(1, r.width / 2),
                        "y", r.y + Math.max(1, r.height / 2)));
    }

    @Step("Tap first Collapse")
    public void tapFirstCollapse() {
        List<WebElement> els = driver.findElements(By.xpath("//*[@content-desc='Collapse']"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Collapse not visible");
        }
        Rectangle r = els.get(0).getRect();
        driver.executeScript("mobile: clickGesture",
                java.util.Map.of("x", r.x + Math.max(1, r.width / 2),
                        "y", r.y + Math.max(1, r.height / 2)));
    }

    @Step("Swipe FAQ list up")
    public void swipeListUp() {
        driver.executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 1400, "width", 600, "height", 700,
                        "direction", "up", "percent", 0.7));
    }

    @Step("Swipe FAQ list down")
    public void swipeListDown() {
        driver.executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 800, "width", 600, "height", 700,
                        "direction", "down", "percent", 0.7));
    }
}
