package com.l2b.vendor.pages;

import com.l2b.vendor.utils.ComposeLocators;
import com.l2b.vendor.utils.Waits;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * OTP verify screen. Copy from live vendor dump 2026-09-11 ({@code Verify your OTP}).
 * Does not submit a valid OTP — that is a later page.
 */
public class OtpPage extends BaseScreen {

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Verify your OTP']")
    private WebElement screenTitle;

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true'][.//android.widget.TextView[@text='Continue']]")
    private WebElement continueButton;

    @Override
    @Step("Wait for OTP screen")
    public void waitUntilLoaded() {
        Waits.visible(driver, screenTitle);
    }

    @Step("Check 'Verify your OTP' is visible")
    public boolean isDisplayedNow() {
        return isPresent(ComposeLocators.textView("Verify your OTP"));
    }

    @Step("Check OTP subtitle is visible")
    public boolean isSubtitleVisible() {
        return isPresent(ComposeLocators.textViewContains("Please enter the OTP sent to"));
    }

    @Step("Check 'Enter your OTP' is visible")
    public boolean isEnterOtpLabelVisible() {
        return isPresent(ComposeLocators.textView("Enter your OTP"));
    }

    @Step("Check 'Edit number' is visible")
    public boolean isEditNumberVisible() {
        return isPresent(ComposeLocators.textView("Edit number"));
    }

    @Step("Check resend countdown is visible")
    public boolean isResendVisible() {
        return isPresent(ComposeLocators.textViewContains("OTP Resend"));
    }

    @Step("Check Continue is visible")
    public boolean isContinueVisible() {
        return isPresent(ComposeLocators.textView("Continue"));
    }

    @Step("Attach OTP screen screenshot")
    public void attachScreen() {
        attachScreenshot("OTP screen");
    }
}
