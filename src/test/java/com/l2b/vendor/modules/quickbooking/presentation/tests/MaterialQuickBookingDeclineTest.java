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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.testng.annotations.Test;

/**
 * Material vendor {@code 9000000017} Quick Booking Decline. Consumes the remaining
 * card (10 Sep P-Sand). Isolated {@code quickbooking/material-decline.xml}.
 * Do not assume Rental's reason list — record the options actually shown.
 * Do not log in as 0001 / 0002 / 0003. Do not tap Accept here.
 */
@Epic("Vendor app")
@Feature("Quick Booking decline — material vendor 9000000017")
public class MaterialQuickBookingDeclineTest extends QuickBookingBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-qb-material-0017-20260919");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");

    @Test(priority = 1, description = "Material-Decline-1: Decline remaining material card and record the real reason UI")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017. Tap first Decline (remaining 10 Sep card). Dump immediately. "
            + "If a reason sheet opens, list every option then complete Decline. Do not tap Accept.")
    public void declineRemainingMaterialCard() {
        QuickBookingPage page = loginToMaterialLanding();
        QuickBookingLandingPage landing = new QuickBookingLandingPage();
        HomePage home = new HomePage();

        int before = landing.declineCount();
        boolean pickup10 = page.hasText("10 Sep 2026");
        boolean pickup11 = page.hasText("11 Sep 2026");
        assertThat(materialPhone()).isEqualTo("9000000017");
        assertThat(before).as("Need the remaining material card").isGreaterThanOrEqualTo(1);
        assertThat(landing.isDeclineEnabled()).as("Decline outer View enabled").isTrue();
        assertThat(landing.isExtendTimeDialogVisible()).as("Must not tap extend-time dialog").isFalse();

        Allure.parameter("phone", materialPhone());
        Allure.parameter("declineCountBefore", String.valueOf(before));
        Allure.parameter("pickup10SepBefore", String.valueOf(pickup10));
        Allure.parameter("pickup11SepBefore", String.valueOf(pickup11));
        writeDump("07-decline-before", landing, home, List.of());
        page.attachScreenshot("material-decline-1-before");

        landing.tapFirstDecline();

        try {
            Waits.until(DriverManager.get(),
                    d -> namedAfterDecline(landing, home, before) != null ? Boolean.TRUE : null,
                    "After Decline, no named next screen yet",
                    Duration.ofSeconds(15));
        } catch (TimeoutException wait) {
            Allure.parameter("declineWait", wait.getMessage());
        }

        String outcome = namedAfterDecline(landing, home, before);
        if (outcome == null) {
            outcome = "unknown";
        }
        boolean dialog = landing.isDeclineBookingDialogVisible();
        boolean selectReason = landing.isSelectReasonVisible();
        Allure.parameter("declineOutcome", outcome);
        Allure.parameter("declineDialog", String.valueOf(dialog));
        Allure.parameter("selectReason", String.valueOf(selectReason));
        Allure.parameter("visibleAfterDeclineTap", String.join(" | ", visibleTexts()));
        writeDump("08-decline-after-tap", landing, home, List.of());
        page.attachScreenshot("material-decline-1-after-tap");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(landing.isExtendTimeDialogVisible()).isFalse();

        List<String> reasons = new ArrayList<>();
        String chosen = "";
        if (dialog && selectReason) {
            Allure.parameter("confirmDeclineVisible", String.valueOf(landing.isConfirmDeclineVisible()));
            Allure.parameter("confirmDeclineEnabledBeforeReason",
                    String.valueOf(landing.isConfirmDeclineEnabled()));
            reasons = landing.listDeclineReasonOptions();
            Allure.parameter("declineReasonsShown", String.join(" || ", reasons));
            writeDump("09-decline-reason-options", landing, home, reasons);
            page.attachScreenshot("material-decline-1-reason-options");
            assertThat(reasons).as("Material reason list must not be empty").isNotEmpty();

            chosen = reasons.get(0);
            landing.tapDeclineReasonOption(chosen);
            try {
                Waits.until(DriverManager.get(),
                        d -> landing.isConfirmDeclineEnabled() ? Boolean.TRUE : null,
                        "Confirm Decline stayed disabled after reason",
                        Duration.ofSeconds(6));
            } catch (TimeoutException ignored) {
                // recorded below
            }
            Allure.parameter("declineReasonChosen", chosen);
            Allure.parameter("reasonRowEnabled", String.valueOf(landing.lastMenuOptionEnabled()));
            Allure.parameter("confirmDeclineEnabledAfterReason",
                    String.valueOf(landing.isConfirmDeclineEnabled()));
            writeDump("10-decline-reason-selected", landing, home, reasons);
            page.attachScreenshot("material-decline-1-reason-selected");
            assertThat(chosen).as("A decline reason was chosen").isNotBlank();
            if (!landing.isConfirmDeclineEnabled()) {
                Allure.parameter("bugNote",
                        "Material Confirm Decline stayed disabled after a reason — new page-specific bug");
                assertThat(landing.isConfirmDeclineEnabled())
                        .as("Confirm Decline stayed disabled after selecting a reason — log a new #17+ row")
                        .isTrue();
            }

            landing.tapConfirmDecline();
            Waits.until(DriverManager.get(),
                    d -> (landing.isRequestDeclinedVisible()
                            || landing.isBookingDeclinedVisible()
                            || (!landing.isDeclineBookingDialogVisible()
                            && (landing.isDisplayedNow() || home.isDisplayedNow()))) ? Boolean.TRUE : null,
                    "After reason + Confirm Decline, no Request Declined / queue / Home",
                    Duration.ofSeconds(15));
        } else {
            Allure.parameter("declineFlow", outcome);
        }

        boolean requestDeclined = landing.isRequestDeclinedVisible();
        boolean bookingDeclined = landing.isBookingDeclinedVisible();
        Allure.parameter("requestDeclined", String.valueOf(requestDeclined));
        Allure.parameter("bookingDeclined", String.valueOf(bookingDeclined));
        writeDump("11-decline-success", landing, home, reasons);
        page.attachScreenshot("material-decline-1-success");
        if (requestDeclined) {
            landing.tapRequestDeclinedOk();
            Waits.until(DriverManager.get(),
                    d -> (!landing.isRequestDeclinedVisible()
                            && (landing.isDisplayedNow() || home.isDisplayedNow())) ? Boolean.TRUE : null,
                    "After Ok, Request Declined stayed open",
                    Duration.ofSeconds(12));
        } else if (bookingDeclined) {
            landing.tapBookingDeclinedOk();
            Waits.until(DriverManager.get(),
                    d -> (!landing.isBookingDeclinedVisible()
                            && (landing.isDisplayedNow() || home.isDisplayedNow())) ? Boolean.TRUE : null,
                    "After OK, Booking Declined stayed open",
                    Duration.ofSeconds(12));
        }

        int after = landing.declineCount();
        boolean stillQb = landing.isDisplayedNow();
        boolean homeNow = home.isDisplayedNow();
        Allure.parameter("declineDialogAfter", String.valueOf(landing.isDeclineBookingDialogVisible()));
        Allure.parameter("declineCountAfter", String.valueOf(after));
        Allure.parameter("stillOnQuickBooking", String.valueOf(stillQb));
        Allure.parameter("homeVisible", String.valueOf(homeNow));
        Allure.parameter("pickup10SepAfter", String.valueOf(page.hasText("10 Sep 2026")));
        Allure.parameter("visibleAfterComplete", String.join(" | ", visibleTexts()));
        writeDump("12-decline-complete", landing, home, reasons);
        page.attachScreenshot("material-decline-1-complete");

        assertThat(landing.isDeclineBookingDialogVisible()).as("Decline Booking? must close").isFalse();
        assertThat(landing.isRequestDeclinedVisible()).as("Request Declined must close after Ok").isFalse();
        assertThat(landing.isBookingDeclinedVisible()).as("Booking Declined must close after OK").isFalse();
        assertThat(landing.isExtendTimeDialogVisible()).isFalse();
        assertThat(requestDeclined || bookingDeclined || after < before || homeNow)
                .as("Decline must finish (success Ok, card left, or Home)")
                .isTrue();
        if (stillQb && page.hasText("10 Sep 2026")) {
            Allure.parameter("bugNote",
                    "Request Declined Ok returned to the same 10 Sep card — Material Decline did not consume it");
        }
    }

    private String namedAfterDecline(QuickBookingLandingPage landing, HomePage home, int declineBefore) {
        if (landing.isDeclineBookingDialogVisible()) {
            return "decline-reason-dialog";
        }
        if (landing.isRequestDeclinedVisible()) {
            return "request-declined";
        }
        if (landing.isBookingDeclinedVisible()) {
            return "booking-declined";
        }
        if (home.isDisplayedNow()) {
            return "home";
        }
        if (landing.isDisplayedNow() && landing.declineCount() < declineBefore) {
            return "one-step-queue-updated";
        }
        if (landing.isDisplayedNow()) {
            return "still-on-queue";
        }
        return null;
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

    private void writeDump(String tag, QuickBookingLandingPage landing, HomePage home, List<String> reasons) {
        String xml = DriverManager.get().getPageSource();
        Path dir = DUMP_DIR.resolve(tag);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("landing.txt"),
                    "phone=9000000017\noutcome=" + namedAfterDecline(landing, home, Integer.MAX_VALUE)
                            + "\npackage=" + vendorPackage()
                            + "\ndeclineDialog=" + landing.isDeclineBookingDialogVisible()
                            + "\nselectReason=" + landing.isSelectReasonVisible()
                            + "\nbookingDeclined=" + landing.isBookingDeclinedVisible()
                            + "\nquickBooking=" + landing.isDisplayedNow()
                            + "\nhome=" + home.isDisplayedNow()
                            + "\ndeclineCount=" + landing.declineCount()
                            + "\nreasons=" + String.join(" || ", reasons)
                            + "\ntexts=\n" + String.join("\n", visibleTexts()) + "\n",
                    StandardCharsets.UTF_8);
            Files.write(dir.resolve("screen.png"),
                    ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write material decline dump " + tag, e);
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
                d -> page.isAcceptVisible() || page.isOrderPlacedVisible() || new HomePage().isDisplayedNow()
                        ? Boolean.TRUE : null,
                "Material landing did not load after OTP",
                Duration.ofSeconds(15));
        return page;
    }
}
