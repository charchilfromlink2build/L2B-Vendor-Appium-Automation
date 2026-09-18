package com.l2b.vendor.modules.quickbooking.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.time.Duration;
import org.testng.annotations.Test;

/**
 * Decline path for rental owner {@code 9000000001} (OTP 1234).
 * Isolated {@code src/test/resources/quickbooking/decline.xml}.
 * This pass uses only one rental card for Decline. Do not tap Accept here.
 * Do not tap the Home {@code Request to extend time} dialog.
 */
@Epic("Vendor app")
@Feature("Quick Booking decline — rental owner 9000000001")
public class QuickBookingDeclineTest extends QuickBookingBaseTest {

    @Test(priority = 1, description = "Decline-1: Select a reason then confirm Decline")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap one Quick Booking Decline. Decline Booking? must open. Open Select a reason, "
            + "pick the first reason, then tap dialog Decline. Dialog-open alone is not a pass. "
            + "Do not tap Accept. Do not tap extend-time Decline.")
    public void declineOneRentalCard() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        HomePage home = new HomePage();
        int before = page.declineCount();
        String amount = page.firstAmountLine();
        assertThat(before).as("Need a pending card").isGreaterThanOrEqualTo(1);
        assertThat(page.isDeclineEnabled()).as("Decline outer View enabled").isTrue();
        assertThat(page.isExtendTimeDialogVisible()).as("Must not tap extend-time dialog").isFalse();

        Allure.parameter("declineCountBefore", String.valueOf(before));
        Allure.parameter("firstCardAmount", amount);
        Allure.parameter("declineTarget", before >= 2 ? "second-card" : "first-card");
        page.attachScreenshot("decline-1-before");
        page.tapSecondDeclineOrFirst();

        Waits.until(DriverManager.get(),
                d -> page.isDeclineBookingDialogVisible() ? Boolean.TRUE : null,
                "After Decline, Decline Booking? did not open",
                Duration.ofSeconds(15));

        Allure.parameter("declineDialog", String.valueOf(page.isDeclineBookingDialogVisible()));
        Allure.parameter("selectReason", String.valueOf(page.isSelectReasonVisible()));
        page.attachScreenshot("decline-1-reason-dialog");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isExtendTimeDialogVisible()).as("Must not be extend-time dialog").isFalse();
        assertThat(page.isDeclineBookingDialogVisible()).as("Decline opens Decline Booking?").isTrue();
        assertThat(page.isSelectReasonVisible()).as("Dialog has Select a reason").isTrue();

        String reason = page.selectFirstDeclineReason();
        Allure.parameter("declineReason", reason);
        Allure.parameter("reasonRowEnabled", String.valueOf(page.lastMenuOptionEnabled()));
        page.attachScreenshot("decline-1-reason-selected");
        assertThat(reason).as("A decline reason was chosen from the dropdown").isNotBlank();

        page.tapDeclineOnReasonDialog();
        Waits.until(DriverManager.get(),
                d -> (page.isBookingDeclinedVisible()
                        || (!page.isDeclineBookingDialogVisible()
                        && (page.isDisplayedNow() || home.isDisplayedNow()))) ? Boolean.TRUE : null,
                "After reason + Decline, Decline Booking? stayed open — Decline did not complete",
                Duration.ofSeconds(15));

        Allure.parameter("bookingDeclined", String.valueOf(page.isBookingDeclinedVisible()));
        page.attachScreenshot("decline-1-success");
        boolean declinedAck = page.isBookingDeclinedVisible();
        if (declinedAck) {
            page.tapBookingDeclinedOk();
            Waits.until(DriverManager.get(),
                    d -> (!page.isBookingDeclinedVisible()
                            && (page.isDisplayedNow() || home.isDisplayedNow())) ? Boolean.TRUE : null,
                    "After OK, Booking Declined stayed open",
                    Duration.ofSeconds(12));
        }

        int after = page.declineCount();
        Allure.parameter("declineDialogAfter", String.valueOf(page.isDeclineBookingDialogVisible()));
        Allure.parameter("declineCountAfter", String.valueOf(after));
        Allure.parameter("stillOnQuickBooking", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("homeVisible", String.valueOf(home.isDisplayedNow()));
        page.attachScreenshot("decline-1-after");

        assertThat(page.isDeclineBookingDialogVisible()).as("Decline Booking? must close").isFalse();
        assertThat(page.isBookingDeclinedVisible()).as("Booking Declined must close after OK").isFalse();
        assertThat(page.isExtendTimeDialogVisible()).as("Must not be extend-time dialog").isFalse();
        assertThat(declinedAck || after < before)
                .as("Decline must finish (success OK or a card left the queue)")
                .isTrue();
        if (page.isDisplayedNow()) {
            assertThat(after).as("Only one Decline — queue must not be empty").isGreaterThanOrEqualTo(1);
        }
    }

    @Test(priority = 2, description = "Decline-2: Decline is enabled on an eligible rental card")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #7. First Decline outer View must be enabled. Do not tap Accept. "
            + "Do not tap extend-time Decline.")
    public void declineButtonIsEnabledOnEligibleCard() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        Allure.parameter("declineCount", String.valueOf(page.declineCount()));
        Allure.parameter("declineEnabled", String.valueOf(page.isDeclineEnabled()));
        page.attachScreenshot("decline-2-enabled");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isDeclineEnabled()).as("Decline clickable outer View enabled").isTrue();
        assertThat(page.isExtendTimeDialogVisible()).isFalse();
    }

    @Test(priority = 3, description = "Decline-3: Rapid double-tap Decline opens one reason dialog")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #10. Two clickGestures on first Decline. One Decline Booking? dialog, no crash. "
            + "Cancel the dialog. Do not tap Accept.")
    public void rapidDoubleTapDeclineOpensOneDialog() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        page.tapFirstDeclineRapidly(2);
        Waits.until(DriverManager.get(),
                d -> page.isDeclineBookingDialogVisible() ? Boolean.TRUE : null,
                "Rapid Decline did not open Decline Booking?",
                Duration.ofSeconds(12));
        Allure.parameter("declineDialog", String.valueOf(page.isDeclineBookingDialogVisible()));
        page.attachScreenshot("decline-3-rapid");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isDeclineBookingDialogVisible()).as("One Decline Booking? after rapid tap").isTrue();
        page.tapDeclineDialogCancel();
        Waits.until(DriverManager.get(),
                d -> !page.isDeclineBookingDialogVisible() && page.isDisplayedNow() ? Boolean.TRUE : null,
                "Cancel did not close Decline Booking?",
                Duration.ofSeconds(8));
        assertThat(page.isDeclineBookingDialogVisible()).isFalse();
        assertThat(page.isDisplayedNow()).as("Queue remains after Cancel").isTrue();
    }

    @Test(priority = 4, description = "Decline-4: Network drop during Decline shows feedback, no crash")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #13. Open Decline Booking?, pick a reason, disable radios, tap dialog Decline. "
            + "Record offline copy vs silent no-op. Radios restored in AfterMethod. Do not tap Accept.")
    public void networkDropDuringDeclineShowsFeedback() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        int before = page.declineCount();
        page.tapFirstDecline();
        Waits.until(DriverManager.get(),
                d -> page.isDeclineBookingDialogVisible() ? Boolean.TRUE : null,
                "Decline Booking? did not open before offline Decline",
                Duration.ofSeconds(12));
        String reason = page.selectFirstDeclineReason();
        Adb.disableRadios();
        page.tapDeclineOnReasonDialog();

        Waits.until(DriverManager.get(),
                d -> (page.isBookingDeclinedVisible() || page.isDeclineBookingDialogVisible()
                        || page.isDisplayedNow() || hasOfflineCopy(d.getPageSource()))
                        ? Boolean.TRUE : null,
                "After offline dialog Decline, no named landing",
                Duration.ofSeconds(12));

        boolean success = page.isBookingDeclinedVisible();
        boolean dialog = page.isDeclineBookingDialogVisible();
        boolean offlineCopy = hasOfflineCopy(DriverManager.get().getPageSource());
        Allure.parameter("declineReason", reason);
        Allure.parameter("bookingDeclined", String.valueOf(success));
        Allure.parameter("dialogStillOpen", String.valueOf(dialog));
        Allure.parameter("offlineOrRetryCopy", String.valueOf(offlineCopy));
        Allure.parameter("offlineUx", success ? "declined-anyway"
                : offlineCopy ? "error-or-retry-copy"
                : dialog ? "stayed-on-dialog-no-error-copy"
                : "left-dialog-no-error-copy");
        page.attachScreenshot("decline-4-offline");

        assertThat(vendorPackage()).as("Offline Decline must not crash Vendor")
                .isEqualTo(Config.get("app.package"));
        if (success) {
            page.tapBookingDeclinedOk();
        }
        if (page.isDisplayedNow()) {
            assertThat(page.declineCount()).as("Queue still has cards").isGreaterThanOrEqualTo(1);
            Allure.parameter("declineCountAfter", String.valueOf(page.declineCount()));
            Allure.parameter("declineCountBefore", String.valueOf(before));
        }
    }

    @Test(priority = 5, description = "Decline-5: Remaining cards stay after cancelling Decline")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #20 related. Open Decline Booking? on first card, Cancel. First amount and "
            + "queue count must stay. Do not tap Accept.")
    public void cancelDeclineLeavesQueueUnchanged() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        int before = page.declineCount();
        String amount = page.firstAmountLine();
        page.tapFirstDecline();
        Waits.until(DriverManager.get(),
                d -> page.isDeclineBookingDialogVisible() ? Boolean.TRUE : null,
                "Decline Booking? did not open",
                Duration.ofSeconds(12));
        page.tapDeclineDialogCancel();
        Waits.until(DriverManager.get(),
                d -> !page.isDeclineBookingDialogVisible() && page.isDisplayedNow() ? Boolean.TRUE : null,
                "Cancel did not return to queue",
                Duration.ofSeconds(8));

        Allure.parameter("declineCountBefore", String.valueOf(before));
        Allure.parameter("declineCountAfter", String.valueOf(page.declineCount()));
        Allure.parameter("amountBefore", amount);
        Allure.parameter("amountAfter", page.firstAmountLine());
        page.attachScreenshot("decline-5-cancel");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isDeclineBookingDialogVisible()).isFalse();
        assertThat(page.declineCount()).as("Cancel must not consume a card").isEqualTo(before);
        assertThat(page.firstAmountLine()).as("First card amount unchanged").isEqualTo(amount);
    }

    @Test(priority = 6, description = "Decline-6: Decline while timer is still running opens the reason dialog")
    @Severity(SeverityLevel.NORMAL)
    @Description("List #18 related. If timer is not 00:00, Decline must still open Decline Booking?. "
            + "Cancel — do not consume. True post-expiry Decline needs a 00:00 seed.")
    public void declineWhileTimerRunningOpensReasonDialog() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        String timer = page.firstTimerValue();
        boolean expired = page.isFirstTimerExpired();
        Allure.parameter("timer", timer);
        Allure.parameter("timerExpired", String.valueOf(expired));
        assertThat(page.isDisplayedNow()).isTrue();
        page.tapFirstDecline();
        Waits.until(DriverManager.get(),
                d -> page.isDeclineBookingDialogVisible() ? Boolean.TRUE : null,
                "Decline with live/expired timer did not open Decline Booking?",
                Duration.ofSeconds(12));
        page.attachScreenshot("decline-6-timer");
        assertThat(page.isDeclineBookingDialogVisible()).isTrue();
        page.tapDeclineDialogCancel();
        Waits.until(DriverManager.get(),
                d -> !page.isDeclineBookingDialogVisible() && page.isDisplayedNow() ? Boolean.TRUE : null,
                "Cancel did not return to queue",
                Duration.ofSeconds(8));
        assertThat(page.isDisplayedNow()).isTrue();
    }
}
