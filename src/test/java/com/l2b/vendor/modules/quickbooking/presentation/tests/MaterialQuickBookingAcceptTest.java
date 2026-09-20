package com.l2b.vendor.modules.quickbooking.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.bookings.presentation.tests.quickbooking.QuickBookingBaseTest;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.testng.annotations.Test;

/**
 * Material vendor {@code 9000000017} Quick Booking Accept. Consumes the first
 * card (11 Sep AAC Block). Isolated {@code quickbooking/material-accept.xml}.
 * Do not assume Assign machine/operator — record the real next screen.
 * Do not log in as 0001 / 0002 / 0003. Do not tap Decline here.
 */
@Epic("Vendor app")
@Feature("Quick Booking accept — material vendor 9000000017")
public class MaterialQuickBookingAcceptTest extends QuickBookingBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-qb-material-0017-20260919");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");

    @Test(priority = 1, description = "Material-Accept-1: Accept first material card and record the real next step")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017. Tap first Accept (11 Sep AAC). Dump immediately. Complete only if "
            + "an enabled Confirm/OK exists. Do not assume Assign machine. Do not tap Decline.")
    public void acceptFirstMaterialCard() {
        QuickBookingPage page = loginToMaterialLanding();
        QuickBookingLandingPage landing = new QuickBookingLandingPage();
        HomePage home = new HomePage();

        int before = landing.acceptCount();
        boolean pickup11 = page.hasText("11 Sep 2026");
        boolean pickup10 = page.hasText("10 Sep 2026");
        assertThat(materialPhone()).isEqualTo("9000000017");
        assertThat(before).as("Need a pending material card").isGreaterThanOrEqualTo(1);
        assertThat(landing.isAcceptEnabled()).as("Accept outer View enabled").isTrue();
        assertThat(landing.isExtendTimeDialogVisible()).as("Must not tap extend-time dialog").isFalse();

        Allure.parameter("phone", materialPhone());
        Allure.parameter("acceptCountBefore", String.valueOf(before));
        Allure.parameter("pickup11SepBefore", String.valueOf(pickup11));
        Allure.parameter("pickup10SepBefore", String.valueOf(pickup10));
        writeDump("04-accept-before", landing, home);
        page.attachScreenshot("material-accept-1-before");

        landing.tapFirstAccept();

        try {
            Waits.until(DriverManager.get(),
                    d -> namedAfterAccept(landing, home, before) != null ? Boolean.TRUE : null,
                    "After Accept, no named next screen yet",
                    Duration.ofSeconds(15));
        } catch (TimeoutException wait) {
            Allure.parameter("acceptWait", wait.getMessage());
        }

        String outcome = namedAfterAccept(landing, home, before);
        if (outcome == null) {
            outcome = "unknown";
        }
        boolean assign = landing.isAssignMachineVisible();
        boolean selectOperator = landing.isSelectOperatorVisible();
        boolean confirmEnabled = landing.isAssignConfirmEnabled();
        boolean skip = landing.isAssignSkipVisible();
        int afterImmediate = landing.acceptCount();
        Allure.parameter("acceptOutcome", outcome);
        Allure.parameter("assignMachine", String.valueOf(assign));
        Allure.parameter("selectOperator", String.valueOf(selectOperator));
        Allure.parameter("assignSkip", String.valueOf(skip));
        Allure.parameter("confirmEnabled", String.valueOf(confirmEnabled));
        Allure.parameter("acceptCountImmediate", String.valueOf(afterImmediate));
        Allure.parameter("visibleAfterAccept", String.join(" | ", visibleTexts()));
        writeDump("05-accept-after-tap", landing, home);
        page.attachScreenshot("material-accept-1-after-tap");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(landing.isExtendTimeDialogVisible()).as("Must not be extend-time dialog").isFalse();

        if (assign) {
            Allure.parameter("acceptFlow", "assign-step");
            if (!confirmEnabled) {
                Allure.parameter("bugNote",
                        "Material Assign Confirm disabled after Accept — new page-specific bug, not #16");
                writeDump("05b-accept-confirm-disabled", landing, home);
                page.attachScreenshot("material-accept-1-confirm-disabled");
                assertThat(confirmEnabled)
                        .as("Material Confirm stayed disabled — log a new #17+ row, do not merge into rental #16")
                        .isTrue();
            }
            landing.tapAssignConfirm();
            Waits.until(DriverManager.get(),
                    d -> (!landing.isAssignMachineVisible()
                            && (landing.isDisplayedNow() || home.isDisplayedNow())) ? Boolean.TRUE : null,
                    "After Confirm, Assign step stayed open — Accept did not complete",
                    Duration.ofSeconds(15));
        } else {
            Allure.parameter("acceptFlow", outcome);
        }

        int after = landing.acceptCount();
        boolean stillQb = landing.isDisplayedNow();
        boolean homeNow = home.isDisplayedNow();
        boolean pickup11After = page.hasText("11 Sep 2026");
        boolean pickup10After = page.hasText("10 Sep 2026");
        Allure.parameter("acceptCountAfter", String.valueOf(after));
        Allure.parameter("stillOnQuickBooking", String.valueOf(stillQb));
        Allure.parameter("homeVisible", String.valueOf(homeNow));
        Allure.parameter("assignMachineAfter", String.valueOf(landing.isAssignMachineVisible()));
        Allure.parameter("pickup11SepAfter", String.valueOf(pickup11After));
        Allure.parameter("pickup10SepAfter", String.valueOf(pickup10After));
        writeDump("06-accept-complete", landing, home);
        page.attachScreenshot("material-accept-1-complete");

        assertThat(landing.isAssignMachineVisible()).as("Assign step must not stay open").isFalse();
        assertThat(landing.isExtendTimeDialogVisible()).isFalse();
        assertThat(after < before || homeNow || hasAcceptSuccessChrome())
                .as("Accept must finish (card left queue, Home, or success chrome)")
                .isTrue();
        if (stillQb) {
            assertThat(after).as("Only one Accept — remaining card must stay").isGreaterThanOrEqualTo(1);
        }
    }

    private String namedAfterAccept(QuickBookingLandingPage landing, HomePage home, int acceptBefore) {
        if (landing.isAssignMachineVisible()) {
            return "assign-machine";
        }
        if (landing.isSelectOperatorVisible()) {
            return "select-operator";
        }
        if (hasText("Assign") && (hasText("Confirm") || hasText("operator") || hasText("machine"))) {
            return "assign-like-sheet";
        }
        if (hasAcceptSuccessChrome()) {
            return "success-chrome";
        }
        if (home.isDisplayedNow()) {
            return "home";
        }
        if (landing.isDisplayedNow() && landing.acceptCount() < acceptBefore) {
            return "one-step-queue-updated";
        }
        if (landing.isDisplayedNow()) {
            return "still-on-queue";
        }
        return null;
    }

    private boolean hasAcceptSuccessChrome() {
        return hasText("Booking Accepted")
                || hasText("Order Accepted")
                || hasText("Accepted Successfully")
                || hasText("Order Confirmed");
    }

    private boolean hasText(String fragment) {
        return DriverManager.get().getPageSource().contains(fragment);
    }

    private Set<String> visibleTexts() {
        Matcher m = TEXT.matcher(DriverManager.get().getPageSource());
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) {
            String t = m.group(1);
            if (!t.startsWith("http") && !t.contains("com.android")) {
                out.add(t);
            }
        }
        return out;
    }

    private void writeDump(String tag, QuickBookingLandingPage landing, HomePage home) {
        String xml = DriverManager.get().getPageSource();
        Path dir = DUMP_DIR.resolve(tag);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("landing.txt"),
                    "phone=9000000017\noutcome=" + namedAfterAccept(landing, home, Integer.MAX_VALUE)
                            + "\npackage=" + vendorPackage()
                            + "\nassignMachine=" + landing.isAssignMachineVisible()
                            + "\nselectOperator=" + landing.isSelectOperatorVisible()
                            + "\nconfirmEnabled=" + landing.isAssignConfirmEnabled()
                            + "\nquickBooking=" + landing.isDisplayedNow()
                            + "\nhome=" + home.isDisplayedNow()
                            + "\nacceptCount=" + landing.acceptCount()
                            + "\ntexts=\n" + String.join("\n", visibleTexts()) + "\n",
                    StandardCharsets.UTF_8);
            Files.write(dir.resolve("screen.png"),
                    ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write material accept dump " + tag, e);
        }
    }

    private QuickBookingPage loginToMaterialLanding() {
        OtpPage otp = openOtp(materialPhone());
        otp.focusOtpField();
        if (!otp.otpFieldText().isEmpty()) {
            otp.clearOtp();
        }
        otp.pressDigitKeys("1234");
        QuickBookingPage page = new QuickBookingPage();
        try {
            Waits.until(DriverManager.get(),
                    d -> page.isDisplayedNow() ? Boolean.TRUE : null,
                    "Quick Booking title did not appear after OTP",
                    Duration.ofSeconds(8));
        } catch (TimeoutException first) {
            if (otp.isDisplayedNow() && otp.otpFieldText().isEmpty()) {
                otp.pasteOtp("1234");
            }
            page.waitUntilLoaded();
        }
        Waits.until(DriverManager.get(),
                d -> page.isAcceptVisible() || page.isOrderPlacedVisible() ? Boolean.TRUE : null,
                "Material cards did not load after Quick Booking title",
                Duration.ofSeconds(15));
        return page;
    }
}
