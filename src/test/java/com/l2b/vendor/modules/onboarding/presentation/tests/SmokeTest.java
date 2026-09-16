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
 * First-launch happy-path smoke for {@code com.l2b.app.qa}: one TestNG method per screen, shared session.
 * Walks splash → language → onboarding slides 1–2 → Sign up chrome → OTP UI. Does not submit a valid OTP
 * and does not replace the dedicated Splash / Language / carousel edge suites.
 */
@Epic("Vendor app")
public class SmokeTest extends BaseTest {

    @BeforeSuite(alwaysRun = true)
    public void prepareOnboardingEnvironment() {
        OnboardingEnvironment.prepareSuite();
    }

    /** Smoke uses the onboarding environment defaults (typically noReset=false, shared session). */
    @Override
    protected boolean noReset() {
        return OnboardingEnvironment.noReset();
    }

    @Override
    protected boolean newSessionPerMethod() {
        return OnboardingEnvironment.newSessionPerMethod();
    }

    /**
     * Simulates a cold launch until the first stable in-app screen.
     * Expected: language is visible (splash is transient) and qa.waardian.com returns HTTP 200.
     * Proves splash completed and the QA backend is reachable before the rest of this shared-session walk.
     */
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

    /**
     * Simulates the language chooser on the continuing first-launch session.
     * Expected: logo, English title, subtitle, all four languages, Default badge, and Get started are visible;
     * then English is selected and Get started is tapped.
     * This is chrome-only coverage; LanguageScreenTest owns single-select and locale-downstream edges.
     */
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

    /**
     * Simulates arriving on onboarding slide 1 after English Get started.
     * Expected: dump-sourced headline, body, Skip, Next, and "Are you Customer?" are visible; then Next is tapped.
     * Confirms the English carousel actually opened after language, before slide-2 assertions.
     */
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

    /**
     * Simulates slide 2 after Next from slide 1.
     * Expected: "Manage Everything…" headline, body, Get started, and the customer footer; then Get started is tapped.
     * Completes the carousel happy path into Sign up without exercising Skip-equals-Next or rapid-tap bugs.
     */
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

    /**
     * Simulates landing on Sign up after carousel Get started.
     * Expected: title, subtitle, mobile label, +91, placeholder, terms copy, and Get OTP are visible.
     * Visibility only — this smoke does not type a phone number or send OTP (Sign up edges are a later module).
     */
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

    /**
     * Simulates accepting terms, entering the QA rental-company phone, and tapping Get OTP.
     * Expected: the verify-OTP chrome appears (title, subtitle, OTP field label, Edit number, resend, Continue).
     * Does not enter or submit a valid OTP — smoke stops at the OTP screen.
     */
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
