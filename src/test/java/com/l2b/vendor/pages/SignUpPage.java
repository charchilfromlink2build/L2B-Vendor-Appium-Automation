package com.l2b.vendor.pages;

import com.l2b.vendor.utils.ComposeLocators;
import com.l2b.vendor.utils.Waits;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * Phone Sign up screen. Copy from live vendor dump 2026-09-11: {@code Get OTP}.
 * Locator matches the clickable outer View that owns enabled state, not an inner decoy Button.
 */
public class SignUpPage extends BaseScreen {

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Sign up']")
    private WebElement screenTitle;

    @AndroidFindBy(xpath = "//android.widget.EditText")
    private WebElement phoneNumberField;

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true'][.//android.widget.TextView[@text='Get OTP']]")
    private WebElement getOtpButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='+91']")
    private WebElement countryCode;

    @AndroidFindBy(className = "android.widget.CheckBox")
    private WebElement termsCheckbox;

    @Override
    @Step("Wait for Sign up screen")
    public void waitUntilLoaded() {
        Waits.visible(driver, screenTitle);
        Waits.visible(driver, phoneNumberField);
    }

    @Step("Check Sign up screen is visible")
    public boolean isDisplayedNow() {
        return isPresent(ComposeLocators.textView("Sign up"));
    }

    @Step("Check Sign up subtitle is visible")
    public boolean isSubtitleVisible() {
        return isPresent(ComposeLocators.textViewContains("Start your journey as a machine owner"));
    }

    @Step("Check 'Mobile Number' label is visible")
    public boolean isMobileNumberLabelVisible() {
        return isPresent(ComposeLocators.textView("Mobile Number"));
    }

    @Step("Check phone placeholder is visible")
    public boolean isPhonePlaceholderVisible() {
        return isPresent(ComposeLocators.textView("Enter mobile number"));
    }

    @Step("Check terms checkbox copy is visible")
    public boolean isTermsVisible() {
        return isPresent(ComposeLocators.textViewContains("I agree to the terms of service"));
    }

    @Step("Check 'Get OTP' is visible")
    public boolean isGetOtpVisible() {
        return isPresent(ComposeLocators.clickableWithText("Get OTP"));
    }

    @Step("Check '+91' is visible")
    public boolean isCountryCodeVisible() {
        return isPresent(ComposeLocators.textView("+91"));
    }

    @Step("Enter mobile number")
    public void enterPhone(String phone) {
        Waits.visible(driver, phoneNumberField).click();
        phoneNumberField.clear();
        phoneNumberField.sendKeys(phone);
    }

    @Step("Accept terms and privacy checkbox")
    public void acceptTerms() {
        tap(termsCheckbox);
    }

    @Step("Tap Get OTP")
    public void tapGetOtp() {
        tap(getOtpButton);
    }

    @Step("Attach Sign up screen screenshot")
    public void attachScreen() {
        attachScreenshot("Sign up screen");
    }
}
