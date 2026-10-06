package com.l2b.vendor.modules.refer.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Refer & Earn from Profile drawer. Live dump
 * {@code /tmp/l2b-refer-0001-20261003} on {@code 9000000001}.
 * Never Confirm Log Out / Accept / Decline.
 */
public class ReferPage extends SplashScreen {

    @Override
    @Step("Wait for Refer & Earn")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Refer & Earn screen did not appear", Duration.ofSeconds(20));
    }

    @Step("Check Refer & Earn identity")
    public boolean isDisplayedNow() {
        return isReferTitleVisible()
                || isReferNowVisible()
                || isReferralCodeLabelVisible()
                || isHeroEarnVisible()
                || isUnavailableVisible();
    }

    @Step("Check referrals unavailable empty state")
    public boolean isUnavailableVisible() {
        return isPresent(ComposeLocators.textViewContains("Referrals are not available"));
    }

    @Step("Check Retry CTA")
    public boolean isRetryVisible() {
        return isPresent(ComposeLocators.textView("Retry"));
    }

    @Step("Full referral chrome (code + how-to)")
    public boolean isFullChromeVisible() {
        return isReferralCodeLabelVisible() && isStepShareVisible();
    }

    @Step("Tap Retry")
    public void tapRetry() {
        WebElement el = driver.findElement(ComposeLocators.textView("Retry"));
        tap(el);
    }

    @Step("Check Refer & Earn title")
    public boolean isReferTitleVisible() {
        return isPresent(ComposeLocators.textView("Refer & Earn"))
                || isPresent(ComposeLocators.textView("Refer and Earn"));
    }

    @Step("Check hero earn line")
    public boolean isHeroEarnVisible() {
        return isPresent(ComposeLocators.textViewContains("Refer a partner"))
                || isPresent(ComposeLocators.textViewContains("earn INR"));
    }

    @Step("Check Your referral code label")
    public boolean isReferralCodeLabelVisible() {
        return isPresent(ComposeLocators.textView("Your referral code"));
    }

    @Step("Check referral code value present (non-empty)")
    public boolean isReferralCodeValueVisible() {
        // Seeded rental vendor uses HRHND8; accept any short alphanumeric code TextView.
        if (isPresent(ComposeLocators.textView("HRHND8"))) {
            return true;
        }
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = el.getText();
            if (t != null && t.matches("[A-Z0-9]{5,12}")) {
                return true;
            }
        }
        return false;
    }

    @Step("Check How to Refer heading")
    public boolean isHowToHeadingVisible() {
        return isPresent(ComposeLocators.textViewContains("How to Refer"))
                || isPresent(ComposeLocators.textViewContains("Refer & Earn reward"));
    }

    @Step("Check three-step intro")
    public boolean isThreeStepsIntroVisible() {
        return isPresent(ComposeLocators.textViewContains("three easy steps"));
    }

    @Step("Check step Share your code")
    public boolean isStepShareVisible() {
        return isPresent(ComposeLocators.textView("Share your code"));
    }

    @Step("Check step They sign up")
    public boolean isStepSignUpVisible() {
        return isPresent(ComposeLocators.textView("They sign up"));
    }

    @Step("Check step You both earn")
    public boolean isStepEarnVisible() {
        return isPresent(ComposeLocators.textView("You both earn"));
    }

    @Step("Check Refer now CTA")
    public boolean isReferNowVisible() {
        return isPresent(ComposeLocators.textView("Refer now"));
    }

    @Step("Check Copy referral code")
    public boolean isCopyCodeVisible() {
        return isPresent(By.xpath("//*[@content-desc='Copy referral code']"));
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

    @Step("Refer now near bottom")
    public boolean referNowNearBottom() {
        List<WebElement> els = driver.findElements(ComposeLocators.textView("Refer now"));
        if (els.isEmpty()) {
            return false;
        }
        Rectangle r = els.get(0).getRect();
        return r.y > 1600;
    }

    @Step("Tap Back")
    public void tapBack() {
        WebElement back = driver.findElement(By.xpath("//*[@content-desc='Back']"));
        tap(back);
    }

    @Step("Device Back")
    public void pressDeviceBack() {
        driver.navigate().back();
    }

    @Step("Tap Copy referral code")
    public void tapCopyCode() {
        List<WebElement> els = driver.findElements(By.xpath("//*[@content-desc='Copy referral code']"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Copy referral code not visible");
        }
        Rectangle r = els.get(0).getRect();
        driver.executeScript("mobile: clickGesture",
                java.util.Map.of(
                        "x", r.x + Math.max(1, r.width / 2),
                        "y", r.y + Math.max(1, r.height / 2)));
    }

    @Step("Tap Refer now (share sheet — dismiss only)")
    public void tapReferNow() {
        WebElement el = driver.findElement(ComposeLocators.textView("Refer now"));
        tap(el);
    }

    @Step("Visible unique texts")
    public Set<String> visibleTexts() {
        Set<String> out = new LinkedHashSet<>();
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = el.getText();
            if (t != null && !t.isBlank()) {
                out.add(t.trim());
            }
        }
        return out;
    }
}
