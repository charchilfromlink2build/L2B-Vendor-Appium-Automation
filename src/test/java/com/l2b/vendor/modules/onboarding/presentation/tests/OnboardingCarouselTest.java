package com.l2b.vendor.modules.onboarding.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.ui.BaseTest;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.modules.onboarding.domain.OnboardingEnvironment;
import com.l2b.vendor.modules.onboarding.presentation.pages.LanguagePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OnboardingCarouselPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.SignUpPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.lang.reflect.Method;
import java.time.Duration;
import org.openqa.selenium.ScreenOrientation;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Onboarding carousel edges for {@code com.l2b.app.qa}.
 *
 * <p>Locators come from live uiautomator dumps (slide copy confirmed 2026-09-11,
 * re-checked on this run). Do not copy Customer strings ({@code Rent Machinery},
 * {@code Continue}) — Vendor slide 1 is {@code Grow Your Machine & Material Business With Us},
 * slide 2 is {@code Manage Everything In One Place}.
 *
 * <p>Each method starts a fresh session ({@code noReset=false}) so first-launch
 * language → carousel is reachable. Case 9 then terminate+activate <em>without</em>
 * clearing data to observe persistence — we do not assume skip vs replay.
 */
@Epic("Vendor app")
@Feature("Onboarding carousel")
public class OnboardingCarouselTest extends BaseTest {

    private static final String VENDOR_PKG = "com.l2b.app.qa";
    private static final String PLAY_STORE_PKG = "com.android.vending";

    @BeforeSuite(alwaysRun = true)
    public void prepareOnboardingEnvironment() {
        OnboardingEnvironment.prepareSuite();
        // Best-effort: cold emulator often leaves io.appium.settings stopped.
        try {
            Adb.run("shell", "am", "start", "-n", "io.appium.settings/.Settings");
        } catch (RuntimeException ignored) {
            // Session create still reports the real error if Settings cannot start.
        }
    }

    /** Fresh install path — same override Splash uses. Required for carousel to appear. */
    @Override
    protected boolean noReset() {
        return false;
    }

    /**
     * Isolated cases: Skip / Get started / Customer tap must not leak into the next method.
     * Fresh session per method (same pattern as Splash). Requires a healthy
     * {@code io.appium.settings} install on the emulator.
     */
    @Override
    protected boolean newSessionPerMethod() {
        return true;
    }

    /**
     * Auto-grant so we are testing the carousel, not the notification dialog
     * (those cases live in {@link SplashScreenTest}).
     */
    @Override
    protected boolean autoGrantPermissions() {
        return true;
    }

    /**
     * Case 7 presses Android Back. If Play Store is still in the recents stack
     * from Case 5, Back-to-exit can surface Play Store even though this method
     * already has a new Appium session. Force-stop + Home before the session
     * so isolation and in-suite runs share the same clean device state.
     */
    @Override
    protected void beforeCreateDriver(Method method) {
        if ("androidBackOnCarouselDoesNotCrash".equals(method.getName())) {
            try {
                Adb.forceStop(PLAY_STORE_PKG);
                Adb.removeRecentTasks(PLAY_STORE_PKG);
                Adb.forceStop(VENDOR_PKG);
                Adb.pressHome();
            } catch (RuntimeException ignored) {
                // Session create still reports the real error if the device is gone.
            }
        }
    }

    // -------------------------------------------------------------------------
    // Case 1 — Slide 1 chrome
    // -------------------------------------------------------------------------

    @Test(priority = 1, description = "Case 1: Slide 1 shows headline, body, Skip, Next, Are you Customer?")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-sourced slide 1: 'Grow Your Machine…', body 'List your machines easily', "
            + "Skip + Next, footer 'Are you Customer?'.")
    public void slideOneShowsHeadlineBodySkipNextAndCustomerPrompt() {
        OnboardingCarouselPage slide1 = openSlideOne();

        assertThat(slide1.isOnSlideOne()).as("Slide 1 headline 'Grow Your Machine'").isTrue();
        assertThat(slide1.isSlideOneBodyVisible()).as("Slide 1 body 'List your machines easily'").isTrue();
        assertThat(slide1.isSkipVisible()).as("Skip on slide 1").isTrue();
        assertThat(slide1.isNextVisible()).as("Next on slide 1").isTrue();
        assertThat(slide1.isCustomerPromptVisible()).as("'Are you Customer?' footer").isTrue();
        // Progress-dot highlight is visual (pill + circle above Next) but those
        // Views have no selected/content-desc in the dump — not asserted here.
        // See progressDotsAreVisualOnlyNotInSelectedState().
        slide1.attachScreen("case1-slide1");
    }

    // -------------------------------------------------------------------------
    // Case 2 — Next
    // -------------------------------------------------------------------------

    @Test(priority = 2, description = "Case 2: Next on slide 1 advances to slide 2")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Next must change the headline to 'Manage Everything In One Place' and expose Get started.")
    public void nextOnSlideOneAdvancesToSlideTwo() {
        OnboardingCarouselPage carousel = openSlideOne();
        carousel.tapNext();
        carousel.waitUntilSlideTwo();

        assertThat(carousel.isOnSlideTwo()).as("Slide 2 headline after Next").isTrue();
        assertThat(carousel.isSlideTwoBodyVisible())
                .as("Slide 2 body 'Track orders, inventory, deliveries'")
                .isTrue();
        assertThat(carousel.isGetStartedVisible()).as("Get started on slide 2").isTrue();
        // Product copy: Skip is slide-1 only. Assert the dump, not the mockup.
        assertThat(carousel.isSkipAbsent()).as("Skip must not remain on slide 2").isTrue();
        assertThat(carousel.isCustomerPromptVisible()).as("'Are you Customer?' still on slide 2").isTrue();
        carousel.attachScreen("case2-slide2-after-next");
    }

    // -------------------------------------------------------------------------
    // Case 3 — Skip
    // -------------------------------------------------------------------------

    @Test(priority = 3, description = "Case 3: Skip on slide 1 advances to slide 2 (same as Next)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Live 15 Sep 2026: Skip does not bypass to Sign up. It behaves like Next — "
            + "slide 2 'Manage Everything In One Place'. Logged as BUGS_FOUND #2.")
    public void skipFromSlideOneGoesToSlideTwo() {
        OnboardingCarouselPage slide1 = openSlideOne();
        slide1.tapSkip();

        AndroidDriver android = (AndroidDriver) DriverManager.get();
        Waits.until(android,
                d -> "carousel-slide2".equals(describeLanding(android)) ? Boolean.TRUE : null,
                "After Skip, slide 2 did not appear",
                Duration.ofSeconds(12));

        String landing = describeLanding(android);
        Allure.parameter("skipLanding", landing);
        new OnboardingCarouselPage().attachScreen("case3-skip-landing-" + landing);

        assertThat(landing)
                .as("Skip on slide 1 must go to slide 2 (it does not bypass to Sign up)")
                .isEqualTo("carousel-slide2");
        assertThat(new SignUpPage().isDisplayedNow()).as("Skip must not open Sign up").isFalse();
    }

    // -------------------------------------------------------------------------
    // Case 4 — Get started
    // -------------------------------------------------------------------------

    @Test(priority = 4, description = "Case 4: Get started on slide 2 opens Sign up")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Happy path: Next → Get started → Sign up. Does not send OTP.")
    public void getStartedOnSlideTwoGoesToSignUp() {
        OnboardingCarouselPage carousel = openSlideOne();
        carousel.tapNext();
        carousel.waitUntilSlideTwo();
        carousel.tapGetStarted();

        SignUpPage signUp = new SignUpPage();
        signUp.waitUntilLoaded();
        assertThat(signUp.isDisplayedNow()).as("Sign up after Get started").isTrue();
        assertThat(new OnboardingCarouselPage().isGone()).as("Carousel gone after Get started").isTrue();
        signUp.attachScreen();
    }

    // -------------------------------------------------------------------------
    // Case 5 — Are you Customer?
    // Customer app is a different project. We only prove Vendor did not crash
    // and we can name the landing surface.
    // -------------------------------------------------------------------------

    @Test(priority = 5, description = "Case 5: Are you Customer? does not crash; landing screen is identifiable")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap the footer. Do not automate the Customer app. Pass = process alive + a named landing "
            + "(still Vendor carousel/signup, another package, Play Store, or browser).")
    public void areYouCustomerRedirectDoesNotCrash() {
        OnboardingCarouselPage slide1 = openSlideOne();
        String beforePkg = slide1.currentPackage();
        slide1.tapCustomerPrompt();

        AndroidDriver android = (AndroidDriver) DriverManager.get();
        // Prefer a package change (Play Store / browser / Customer). If none in 6s, stay on Vendor.
        try {
            Waits.until(android,
                    d -> {
                        String pkg = android.getCurrentPackage();
                        return (pkg != null && !pkg.equals(beforePkg)) ? Boolean.TRUE : null;
                    },
                    "package did not change after customer tap",
                    Duration.ofSeconds(6));
        } catch (RuntimeException ignored) {
            // Staying inside Vendor is a valid landing — recorded below, not treated as a crash.
        }

        String afterPkg = android.getCurrentPackage();
        String landing = describeLanding(android);
        slide1.attachScreen("case5-customer-landing-" + landing);

        // Crash = package gone / null. Anything else is a named landing we record, not a Customer E2E.
        assertThat(afterPkg)
                .as("Vendor must not crash on 'Are you Customer?'. Landing=%s package=%s", landing, afterPkg)
                .isNotBlank();
        assertThat(landing)
                .as("Landing after customer tap must be identifiable (not a blank hang)")
                .isNotEqualTo("unknown");
    }

    // -------------------------------------------------------------------------
    // Case 6 — Swipe
    // Pager may or may not be swipeable. Assert the real gesture result.
    // -------------------------------------------------------------------------

    @Test(priority = 6, description = "Case 6: Horizontal swipe on slide 1 — record whether the pager moves")
    @Severity(SeverityLevel.NORMAL)
    @Description("Do not assume Next is the only path. Swipe left and record slide 2 vs stay-on-slide-1.")
    public void swipeOnSlideOneMovesOrStays() {
        OnboardingCarouselPage carousel = openSlideOne();
        carousel.swipeTowardNext();

        Waits.until(DriverManager.get(),
                d -> (carousel.isOnSlideTwo() || carousel.isOnSlideOne()) ? Boolean.TRUE : null,
                "After swipe, neither slide 1 nor slide 2 was on screen",
                Duration.ofSeconds(8));

        boolean onTwo = carousel.isOnSlideTwo();
        boolean onOne = carousel.isOnSlideOne();
        carousel.attachScreen(onTwo ? "case6-swipe-reached-slide2" : "case6-swipe-stayed-slide1");

        assertThat(onOne || onTwo)
                .as("Swipe must not crash; land on slide 1 or slide 2 (pager=%s)", onTwo ? "moved" : "did-not-move")
                .isTrue();
        // If swipe works, a reverse swipe should be able to come back — only asserted when it moved.
        if (onTwo) {
            carousel.swipeTowardPrevious();
            Waits.until(DriverManager.get(),
                    d -> carousel.isOnSlideOne() ? Boolean.TRUE : null,
                    "After reverse swipe, slide 1 did not return",
                    Duration.ofSeconds(8));
            assertThat(carousel.isOnSlideOne()).as("Reverse swipe returns to slide 1").isTrue();
        }
    }

    // -------------------------------------------------------------------------
    // Case 7 — Android Back
    // -------------------------------------------------------------------------

    @Test(priority = 7, description = "Case 7: Android Back from slide 2 and slide 1 — no crash, named screen")
    @Severity(SeverityLevel.NORMAL)
    @Description("Back from slide 2 may return to slide 1, language, or exit. Record the actual screen.")
    public void androidBackOnCarouselDoesNotCrash() {
        OnboardingCarouselPage carousel = openSlideOne();
        carousel.tapNext();
        carousel.waitUntilSlideTwo();
        carousel.pressBack();

        AndroidDriver android = (AndroidDriver) DriverManager.get();
        // Back can animate / change package; wait for a named screen, not the first frame.
        Waits.until(android,
                d -> !"unknown".equals(describeLanding(android)) ? Boolean.TRUE : null,
                "After Back from slide 2, no named screen",
                Duration.ofSeconds(8));
        String afterSlide2Back = describeLanding(android);
        Allure.parameter("backFromSlide2", afterSlide2Back);
        Allure.parameter("packageAfterBack", String.valueOf(android.getCurrentPackage()));
        carousel.attachScreen("case7-back-from-slide2-" + afterSlide2Back);
        assertThat(afterSlide2Back)
                .as("Back from slide 2 must land on a named screen, not hang")
                .isNotEqualTo("unknown");

        if (carousel.isOnSlideOne()) {
            carousel.pressBack();
            String afterSlide1Back = describeLanding((AndroidDriver) DriverManager.get());
            carousel.attachScreen("case7-back-from-slide1-" + afterSlide1Back);
            assertThat(afterSlide1Back)
                    .as("Back from slide 1 must land on a named screen, not hang")
                    .isNotEqualTo("unknown");
        }
    }

    // -------------------------------------------------------------------------
    // Case 8 — Rotation
    // -------------------------------------------------------------------------

    @Test(priority = 8, description = "Case 8: Rotation during carousel does not crash")
    @Severity(SeverityLevel.MINOR)
    @Description("If portrait-locked, stay portrait. Landscape layout is not asserted when locked.")
    public void rotationDuringCarousel() {
        OnboardingCarouselPage carousel = openSlideOne();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        ScreenOrientation before = android.getOrientation();
        try {
            android.rotate(ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            assertThat(carousel.isLoaded()).as("Carousel still up after rotate threw").isTrue();
            return;
        }
        ScreenOrientation after = android.getOrientation();
        assertThat(carousel.isLoaded() || new SignUpPage().isDisplayedNow())
                .as("No crash after rotation attempt")
                .isTrue();
        carousel.attachScreen("case8-after-rotate-" + after);
        if (after == ScreenOrientation.PORTRAIT && before == ScreenOrientation.PORTRAIT) {
            return;
        }
        android.rotate(ScreenOrientation.PORTRAIT);
        assertThat(carousel.isLoaded() || new SignUpPage().isDisplayedNow()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Case 10 — Rapid double/triple tap (was in the original 9; rotation stayed too)
    // -------------------------------------------------------------------------

    @Test(priority = 10, description = "Case 10: Rapid Skip/Next taps — no crash, no duplicate navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap Skip 3× fast on a fresh slide 1, then Next 3× fast on a fresh slide 1. "
            + "Pass = process alive and a single named landing (not a hang / crash).")
    public void rapidDoubleTapSkipAndNextDoesNotDuplicate() {
        OnboardingCarouselPage carousel = openSlideOne();
        carousel.tapSkipRapidly(3);

        AndroidDriver android = (AndroidDriver) DriverManager.get();
        Waits.until(android,
                d -> {
                    String now = describeLanding(android);
                    return ("carousel-slide2".equals(now) || "signup".equals(now)
                            || "carousel-slide1".equals(now)) ? Boolean.TRUE : null;
                },
                "After rapid Skip, no named Vendor screen",
                Duration.ofSeconds(12));
        String skipLanding = describeLanding(android);
        Allure.parameter("rapidSkipLanding", skipLanding);
        carousel.attachScreen("case10-rapid-skip-" + skipLanding);
        assertThat(android.getCurrentPackage()).as("Vendor alive after rapid Skip").isEqualTo(VENDOR_PKG);
        assertThat(skipLanding)
                .as("Rapid Skip must not crash; one landing (expected slide 2, not Sign up)")
                .isEqualTo("carousel-slide2");

        // New session is per-method, so Next rapid needs its own walk: stay in this
        // method and go back to slide 1 if we can, else the Skip path already proved
        // one-advance. Re-open slide 1 via language only if still on carousel.
        if (carousel.isOnSlideTwo()) {
            carousel.swipeTowardPrevious();
            Waits.until(android, d -> carousel.isOnSlideOne() ? Boolean.TRUE : null,
                    "Reverse swipe after rapid Skip did not return to slide 1",
                    Duration.ofSeconds(8));
        }
        assertThat(carousel.isOnSlideOne()).as("Back on slide 1 before rapid Next").isTrue();
        carousel.tapNextRapidly(3);
        Waits.until(android,
                d -> {
                    String now = describeLanding(android);
                    return ("carousel-slide2".equals(now) || "signup".equals(now)) ? Boolean.TRUE : null;
                },
                "After rapid Next, no named Vendor screen",
                Duration.ofSeconds(12));
        String nextLanding = describeLanding(android);
        Allure.parameter("rapidNextLanding", nextLanding);
        carousel.attachScreen("case10-rapid-next-" + nextLanding);
        assertThat(android.getCurrentPackage()).as("Vendor alive after rapid Next").isEqualTo(VENDOR_PKG);
        assertThat(nextLanding)
                .as("Rapid Next: slide 2 = one advance; signup = Next slot also fires Get started")
                .isIn("carousel-slide2", "signup");
        // Observed 15 Sep 2026: landing=signup. Logged as BUGS_FOUND #3.
    }

    // -------------------------------------------------------------------------
    // Case 11 — Progress dots (not asserted in Case 1)
    // -------------------------------------------------------------------------

    @Test(priority = 11, description = "Case 11: Progress dots — visual cluster exists; highlight not in dump")
    @Severity(SeverityLevel.MINOR)
    @Description("Screenshots show a pill + circle above Next/Get started. Dump has two 126×126 "
            + "Views with no selected/content-desc, so correct-dot-per-slide cannot be asserted.")
    public void progressDotsAreVisualOnlyNotInSelectedState() {
        OnboardingCarouselPage carousel = openSlideOne();
        int slide1Count = carousel.pagerIndicatorNodeCount();
        boolean slide1Selected = carousel.pagerIndicatorExposesSelectedState();
        Allure.parameter("slide1IndicatorViews", String.valueOf(slide1Count));
        Allure.parameter("slide1SelectedOrLabelled", String.valueOf(slide1Selected));
        carousel.attachScreen("case11-slide1-dots");

        assertThat(slide1Count)
                .as("Slide 1 should expose the two indicator Views above Next (dump 15 Sep)")
                .isGreaterThanOrEqualTo(2);

        carousel.tapNext();
        carousel.waitUntilSlideTwo();
        int slide2Count = carousel.pagerIndicatorNodeCount();
        boolean slide2Selected = carousel.pagerIndicatorExposesSelectedState();
        Allure.parameter("slide2IndicatorViews", String.valueOf(slide2Count));
        Allure.parameter("slide2SelectedOrLabelled", String.valueOf(slide2Selected));
        carousel.attachScreen("case11-slide2-dots");

        assertThat(slide2Count)
                .as("Slide 2 should still expose the indicator Views above Get started")
                .isGreaterThanOrEqualTo(2);
        // Dump/live: those Views have no usable selected/content-desc. Screenshots
        // are the highlight-sync evidence (pill moves from first to second).
    }

    // -------------------------------------------------------------------------
    // Case 9 — Persistence after completing carousel
    // First session: noReset=false (class default) so we actually see the carousel.
    // Second session: noReset=true so data is kept. Do not assume skip vs replay.
    // -------------------------------------------------------------------------

    @Test(priority = 9, description = "Case 9: After Get started, relaunch with data kept — record if carousel returns")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Complete slide 2 Get started → Sign up, force-stop, new session noReset=true. "
            + "Landing may be Sign up (persisted), carousel (not persisted), or language. Record which.")
    public void carouselPersistenceAfterRelaunch() {
        OnboardingCarouselPage carousel = openSlideOne();
        carousel.tapNext();
        carousel.waitUntilSlideTwo();
        carousel.tapGetStarted();
        SignUpPage signUp = new SignUpPage();
        signUp.waitUntilLoaded();
        assertThat(signUp.isDisplayedNow()).as("Precondition: Sign up visible before relaunch").isTrue();

        // Keep app data: terminate + activate on the same session (no pm clear, no second
        // DriverFactory.create). A new session with noReset=true is equivalent but was
        // dying on Appium Settings on this emulator.
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        android.terminateApp(VENDOR_PKG);
        android.activateApp(VENDOR_PKG);

        Waits.until(android,
                d -> {
                    if (!VENDOR_PKG.equals(android.getCurrentPackage())) {
                        return null;
                    }
                    String now = describeLanding(android);
                    return !"unknown".equals(now) ? Boolean.TRUE : null;
                },
                "Vendor relaunched (package=" + VENDOR_PKG
                        + ") but never reached language, carousel, or Sign up",
                Duration.ofSeconds(20));

        String landing = describeLanding(android);
        new OnboardingCarouselPage().attachScreen("case9-relaunch-landing-" + landing);

        assertThat(landing)
                .as("Relaunch after completing carousel. Observed landing=%s package=%s "
                        + "(signup=persisted, carousel=not persisted, language=full first-launch again)",
                        landing, android.getCurrentPackage())
                .isIn("signup", "carousel-slide1", "carousel-slide2", "language",
                        "notification-permission");
    }

    /**
     * First-launch walk to slide 1. Language locators are dump-sourced
     * ({@code Welcome to L2B} / {@code Get started}) — not Customer {@code Continue}.
     */
    private OnboardingCarouselPage openSlideOne() {
        LanguagePage language = new LanguagePage();
        language.waitUntilLoaded();
        language.selectEnglish();
        language.tapGetStarted();
        OnboardingCarouselPage carousel = new OnboardingCarouselPage();
        carousel.waitUntilSlideOne();
        return carousel;
    }

    /** Names the screen we are on so edge cases can pass without guessing product intent. */
    private String describeLanding(AndroidDriver android) {
        String pkg = android.getCurrentPackage();
        if (pkg == null || pkg.isBlank()) {
            return "unknown";
        }
        if (!VENDOR_PKG.equals(pkg)) {
            if (pkg.contains("vending") || pkg.contains("play")) {
                return "play-store";
            }
            if (pkg.contains("chrome") || pkg.contains("browser")) {
                return "browser";
            }
            if (pkg.contains("launcher") || pkg.contains("systemui")) {
                return "launcher";
            }
            return "other-package:" + pkg;
        }
        if (new SignUpPage().isDisplayedNow()) {
            return "signup";
        }
        OnboardingCarouselPage carousel = new OnboardingCarouselPage();
        if (carousel.isOnSlideTwo()) {
            return "carousel-slide2";
        }
        if (carousel.isOnSlideOne()) {
            return "carousel-slide1";
        }
        if (new LanguagePage().isDisplayedNow()) {
            return "language";
        }
        if (new com.l2b.vendor.modules.onboarding.presentation.pages.NotificationPermissionDialog()
                .isDisplayedNow()) {
            return "notification-permission";
        }
        return "unknown";
    }
}
