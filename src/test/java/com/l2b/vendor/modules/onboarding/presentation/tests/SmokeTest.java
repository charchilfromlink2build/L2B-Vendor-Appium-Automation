package com.l2b.vendor.modules.onboarding.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.api.HttpClient;
import com.l2b.vendor.core.ui.BaseTest;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.onboarding.domain.OnboardingEnvironment;
import com.l2b.vendor.modules.onboarding.presentation.pages.LanguagePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OnboardingCarouselPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.SignUpPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * First-launch flow, one Allure/TestNG test per screen. Shared Appium session.
 * OTP screen is opened; valid OTP is not submitted.
 */
@Epic("Vendor app")
public class SmokeTest extends BaseTest {

    @BeforeSuite(alwaysRun = true)
    public void prepareOnboardingEnvironment() {
        OnboardingEnvironment.prepareSuite();
    }

    @Override
    protected boolean noReset() {
        return OnboardingEnvironment.noReset();
    }

    @Override
    protected boolean newSessionPerMethod() {
        return OnboardingEnvironment.newSessionPerMethod();
    }

    @Test(description = "Splash screen")
    @Feature("01 Splash screen")
    @Story("Cold launch reaches first stable UI")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Splash is transient. First stable UI is language (after notification prompt). "
            + "Screenshot is taken once language is visible. QA backend ping is included here.")
    public void splashScreen() {
        LanguagePage language = new LanguagePage();
        language.waitUntilLoaded();
        language.attachScreenshot("01 Splash screen — first stable UI (language)");
        assertThat(language.isDisplayedNow())
                .as("Splash must finish on the language screen")
                .isTrue();
        Response backend = new HttpClient().getRoot();
        assertThat(backend.statusCode())
                .as("QA backend https://qa.waardian.com must respond")
                .isEqualTo(200);
    }

    @Test(description = "Language screen")
    @Feature("02 Language screen")
    @Story("All language options and Get started")
    @Severity(SeverityLevel.CRITICAL)
    public void languageScreen() {
        LanguagePage language = new LanguagePage();
        language.waitUntilLoaded();
        assertThat(language.isPartnerLogoVisible()).as("Partner logo").isTrue();
        assertThat(language.isDisplayedNow()).as("Title 'Welcome to L2B'").isTrue();
        assertThat(language.isSubtitleVisible()).as("Language subtitle").isTrue();
        assertThat(language.isChooseLanguageVisible()).as("'Choose your language'").isTrue();
        assertThat(language.isEnglishVisible()).as("English").isTrue();
        assertThat(language.isDefaultBadgeVisible()).as("Default badge on English").isTrue();
        assertThat(language.isHindiVisible()).as("हिंदी").isTrue();
        assertThat(language.isTeluguVisible()).as("తెలుగు").isTrue();
        assertThat(language.isKannadaVisible()).as("ಕನ್ನಡ").isTrue();
        assertThat(language.isGetStartedVisible()).as("Get started CTA").isTrue();
        language.attachScreenshot("02 Language screen");
        language.selectEnglish();
        language.tapGetStarted();
    }

    @Test(description = "Onboarding slide 1")
    @Feature("03 Onboarding slide 1")
    @Story("Grow Your Machine — Skip + Next")
    @Severity(SeverityLevel.CRITICAL)
    public void onboardingSlideOne() {
        OnboardingCarouselPage carousel = new OnboardingCarouselPage();
        carousel.waitUntilSlideOne();
        assertThat(carousel.isOnSlideOne()).as("Slide 1 headline").isTrue();
        assertThat(carousel.isSlideOneBodyVisible()).as("Slide 1 body").isTrue();
        assertThat(carousel.isSkipVisible()).as("Skip").isTrue();
        assertThat(carousel.isNextVisible()).as("Next").isTrue();
        assertThat(carousel.isCustomerPromptVisible()).as("'Are you Customer?'").isTrue();
        carousel.attachScreen("03 Onboarding slide 1");
        carousel.tapNext();
    }

    @Test(description = "Onboarding slide 2")
    @Feature("04 Onboarding slide 2")
    @Story("Manage Everything — Get started")
    @Severity(SeverityLevel.CRITICAL)
    public void onboardingSlideTwo() {
        OnboardingCarouselPage carousel = new OnboardingCarouselPage();
        carousel.waitUntilSlideTwo();
        assertThat(carousel.isOnSlideTwo()).as("Slide 2 headline").isTrue();
        assertThat(carousel.isSlideTwoBodyVisible()).as("Slide 2 body").isTrue();
        assertThat(carousel.isGetStartedVisible()).as("Get started on slide 2").isTrue();
        assertThat(carousel.isCustomerPromptVisible()).as("'Are you Customer?' still visible").isTrue();
        carousel.attachScreen("04 Onboarding slide 2");
        carousel.tapGetStarted();
    }

    @Test(description = "Sign up screen")
    @Feature("05 Sign up screen")
    @Story("Phone, +91, terms, Get OTP visible (not submitted yet)")
    @Severity(SeverityLevel.CRITICAL)
    public void signUpScreen() {
        SignUpPage signUp = new SignUpPage();
        signUp.waitUntilLoaded();
        assertThat(signUp.isDisplayedNow()).as("Sign up title").isTrue();
        assertThat(signUp.isSubtitleVisible()).as("Sign up subtitle").isTrue();
        assertThat(signUp.isMobileNumberLabelVisible()).as("'Mobile Number'").isTrue();
        assertThat(signUp.isCountryCodeVisible())
                .as("'+91' country code must be visible on Sign up screen")
                .isTrue();
        assertThat(signUp.isPhonePlaceholderVisible()).as("'Enter mobile number'").isTrue();
        assertThat(signUp.isTermsVisible()).as("Terms checkbox copy").isTrue();
        assertThat(signUp.isGetOtpVisible())
                .as("'Get OTP' CTA must be visible on Sign up screen")
                .isTrue();
        signUp.attachScreenshot("05 Sign up screen");
    }

    @Test(description = "OTP screen")
    @Feature("06 OTP screen")
    @Story("Get OTP opens verify screen — do not submit valid OTP")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Enters the QA rental-company phone, accepts terms, taps Get OTP. "
            + "Asserts OTP UI only. Does not enter or verify OTP.")
    public void otpScreen() {
        SignUpPage signUp = new SignUpPage();
        signUp.waitUntilLoaded();
        signUp.acceptTerms();
        signUp.enterPhone(Config.get("user.rental.company.phone"));
        signUp.tapGetOtp();

        OtpPage otp = new OtpPage();
        otp.waitUntilLoaded();
        assertThat(otp.isDisplayedNow()).as("'Verify your OTP'").isTrue();
        assertThat(otp.isSubtitleVisible()).as("OTP sent-to subtitle").isTrue();
        assertThat(otp.isEnterOtpLabelVisible()).as("'Enter your OTP'").isTrue();
        assertThat(otp.isEditNumberVisible()).as("'Edit number'").isTrue();
        assertThat(otp.isResendVisible()).as("OTP resend countdown").isTrue();
        assertThat(otp.isContinueVisible()).as("Continue").isTrue();
        otp.attachScreenshot("06 OTP screen");
    }
}
