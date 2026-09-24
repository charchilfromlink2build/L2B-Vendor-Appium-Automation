package com.l2b.vendor.modules.bookings.presentation.tests.rentalbooking;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.data.api.BookingsApi;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.bookings.presentation.pages.RentalBookingsPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.data.api.AuthApi;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * RB-E: negative, spam, interruption, and bad-data edges for Rental Booking.
 * Duplicate-action cases run against seed bookings only, one booking per case, so
 * a double-commit defect cannot be blamed on shared state. Isolated
 * {@code bookings/booking-rental-edge-e*.xml}.
 */
@Epic("Vendor app")
@Feature("Rental Booking edges — rental vendor 9000000001")
public class RentalBookingEdgeTest extends RentalBookingBaseTest {

    private final AuthApi auth = new AuthApi();
    private final BookingsApi bookingsApi = new BookingsApi();

    @Test(priority = 1, description = "RB-E1: double-tap Accept commits once")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Two fast taps on Accept for one seed booking. Expect a single accept: one "
            + "Assign sheet, one booking in Upcoming, one accept call server-side. Two bookings "
            + "or a duplicated charge is a high-severity new Bookings bug.")
    public void doubleTapAcceptCommitsOnce() {
        Map<String, Object> seed = readSeed("/tmp/l2b-e1-seed.json", "E1");
        String bookingId = String.valueOf(seed.get("booking_id"));
        String bookingNumber = String.valueOf(seed.get("booking_number"));
        String siteNeedle = "RB-E1";
        Allure.parameter("bookingNumber", bookingNumber);
        Allure.parameter("bookingId", bookingId);

        String token = vendorToken();
        Response before = bookingsApi.detail(token, bookingId);
        assertThat(before.statusCode()).isEqualTo(200);
        assertThat(before.jsonPath().getString("data.status")).isEqualTo("pending");

        QuickBookingPage qb = reachQuickBookingQueue();
        assertThat(qb.isDisplayedNow()).as("Quick Booking queue").isTrue();
        assertThat(DriverManager.get().getPageSource().contains(siteNeedle)
                || DriverManager.get().getPageSource().contains(bookingNumber)
                || qb.hasText("Excavator"))
                .as("E1 rental seed must be on the Quick Booking queue (not only material)")
                .isTrue();

        qb.tapAcceptNearTextRapidly(siteNeedle, 2);

        RentalBookingsPage sheet = new RentalBookingsPage();
        Waits.until(DriverManager.get(),
                d -> sheet.isAssignMachineVisible() ? Boolean.TRUE : null,
                "Double Accept did not open Assign machine sheet",
                Duration.ofSeconds(15));
        sheet.attachScreenshot("rb-e1-after-double-accept");
        Allure.parameter("assignSheetAfterDoubleAccept", "true");

        // Dismiss the single Assign sheet via Skip (confirmed, no operator).
        boolean skipped = false;
        if (sheet.isAssignSkipVisible()) {
            sheet.tapAssignSkip();
            try {
                Waits.until(DriverManager.get(),
                        d -> !sheet.isAssignMachineVisible() ? Boolean.TRUE : null,
                        "Skip did not close Assign sheet after double Accept",
                        Duration.ofSeconds(8));
                skipped = true;
            } catch (org.openqa.selenium.TimeoutException skipStuck) {
                Allure.parameter("skipStuckAfterDoubleAccept", "true");
                sheet.attachScreenshot("rb-e1-skip-stuck");
                // Retry once with a second Skip tap — if still stuck, product defect.
                if (sheet.isAssignSkipVisible()) {
                    sheet.tapAssignSkip();
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                skipped = !sheet.isAssignMachineVisible();
            }
        }
        Allure.parameter("assignSkipped", String.valueOf(skipped));

        Response after = bookingsApi.detail(token, bookingId);
        String statusAfter = after.jsonPath().getString("data.status");
        Allure.parameter("apiStatusAfter", statusAfter);

        Response list = bookingsApi.list(token);
        assertThat(list.statusCode()).isEqualTo(200);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");
        long matching = rows.stream()
                .filter(r -> bookingId.equals(String.valueOf(r.get("id")))
                        || bookingNumber.equals(String.valueOf(r.get("booking_number"))))
                .count();
        Allure.parameter("listMatchesForSeed", String.valueOf(matching));
        assertThat(matching)
                .as("Double Accept must not create a duplicate list row for the seed")
                .isEqualTo(1);

        if (!skipped && sheet.isAssignMachineVisible()) {
            // Distinct defect: after double Accept, Assign opens but Skip cannot dismiss.
            assertThat(false)
                    .as("NEW BUG: After double-tap Accept, Assign machine stayed open and Skip "
                            + "did not dismiss — user is stuck on Assign (seed still "
                            + statusAfter + "). Not the same as #16 busy Confirm.")
                    .isTrue();
        }

        assertThat(statusAfter)
                .as("Seed must be accepted exactly once (confirmed or later)")
                .isIn("confirmed", "operator_assigned");
        assertThat(sheet.isAssignMachineVisible())
                .as("Only one Assign sheet from the double Accept, then dismissed")
                .isFalse();
    }

    @Test(priority = 2, description = "RB-E2: double-tap Decline commits once")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Two fast taps on Decline. One reason dialog, one decline call, no crash on the "
            + "second tap after the card is already gone.")
    public void doubleTapDeclineCommitsOnce() {
        Map<String, Object> seed = readSeed("/tmp/l2b-e2-seed.json", "E2");
        String bookingId = String.valueOf(seed.get("booking_id"));
        String bookingNumber = String.valueOf(seed.get("booking_number"));
        String siteNeedle = "RB-E2";
        Allure.parameter("bookingNumber", bookingNumber);
        Allure.parameter("bookingId", bookingId);

        String token = vendorToken();
        Response before = bookingsApi.detail(token, bookingId);
        assertThat(before.statusCode()).isEqualTo(200);
        assertThat(before.jsonPath().getString("data.status")).isEqualTo("pending");

        QuickBookingPage qb = reachQuickBookingQueue();
        assertThat(qb.isDisplayedNow()).as("Quick Booking queue").isTrue();
        assertThat(DriverManager.get().getPageSource().contains(siteNeedle)
                || DriverManager.get().getPageSource().contains(bookingNumber))
                .as("E2 rental seed must be on the Quick Booking queue")
                .isTrue();

        qb.tapDeclineNearTextRapidly(siteNeedle, 2);

        RentalBookingsPage bookings = new RentalBookingsPage();
        Waits.until(DriverManager.get(),
                d -> bookings.isDeclineBookingDialogVisible() ? Boolean.TRUE : null,
                "Double Decline did not open Decline Booking? dialog",
                Duration.ofSeconds(12));
        bookings.attachScreenshot("rb-e2-decline-dialog");

        // Exactly one reason dialog — pick reason + confirm once.
        String reason = bookings.selectFirstDeclineReason();
        Allure.parameter("declineReason", reason);
        assertThat(reason).as("A decline reason must be selectable").isNotBlank();
        assertThat(bookings.isConfirmDeclineEnabled())
                .as("Dialog Decline must enable after reason")
                .isTrue();
        bookings.tapConfirmDecline();

        try {
            Waits.until(DriverManager.get(),
                    d -> !bookings.isDeclineBookingDialogVisible()
                            || DriverManager.get().getPageSource().contains("Booking Declined")
                            ? Boolean.TRUE : null,
                    "Decline dialog did not close after confirm",
                    Duration.ofSeconds(12));
        } catch (org.openqa.selenium.TimeoutException stuck) {
            Allure.parameter("declineDialogStuck", "true");
            bookings.attachScreenshot("rb-e2-decline-stuck");
            assertThat(false)
                    .as("NEW BUG: After double-tap Decline + reason, Decline Booking? dialog "
                            + "did not dismiss — user stuck on decline sheet")
                    .isTrue();
        }
        if (DriverManager.get().getPageSource().contains("Booking Declined")) {
            java.util.List<org.openqa.selenium.WebElement> oks =
                    DriverManager.get().findElements(org.openqa.selenium.By.xpath(
                            "//android.widget.TextView[@text='OK']"));
            if (!oks.isEmpty()) {
                oks.get(0).click();
            }
        }

        // Queue must not keep the declined seed; no second dialog / crash.
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean stillOnQueue = qb.isDisplayedNow()
                && (qb.hasText(bookingNumber) || qb.hasText(siteNeedle)
                || DriverManager.get().getPageSource().contains(bookingNumber));
        Allure.parameter("stillOnQueue", String.valueOf(stillOnQueue));
        assertThat(stillOnQueue)
                .as("Declined seed must leave the Quick Booking queue after one Decline")
                .isFalse();

        Response after = bookingsApi.detail(token, bookingId);
        int afterCode = after.statusCode();
        String statusAfter = afterCode == 200 ? after.jsonPath().getString("data.status") : null;
        Allure.parameter("apiDetailStatusCode", String.valueOf(afterCode));
        Allure.parameter("apiStatusAfter", String.valueOf(statusAfter));

        Response list = bookingsApi.list(token);
        long matching = 0;
        if (list.statusCode() == 200) {
            List<Map<String, Object>> rows = list.jsonPath().getList("data");
            matching = rows.stream()
                    .filter(r -> bookingId.equals(String.valueOf(r.get("id")))
                            || bookingNumber.equals(String.valueOf(r.get("booking_number"))))
                    .count();
        }
        Allure.parameter("listMatchesForSeed", String.valueOf(matching));
        assertThat(matching)
                .as("Double Decline must not leave duplicate vendor-list rows")
                .isLessThanOrEqualTo(1);

        assertThat(afterCode == 404 || matching == 0
                || "declined".equals(statusAfter)
                || "rejected".equals(statusAfter)
                || "cancelled".equals(statusAfter))
                .as("After one Decline confirm, booking must leave vendor list or be declined "
                        + "(detail=" + afterCode + " status=" + statusAfter + ")")
                .isTrue();

        assertThat(bookings.isDeclineBookingDialogVisible())
                .as("No second Decline dialog after confirm")
                .isFalse();
        bookings.attachScreenshot("rb-e2-after-decline");
    }

    @Test(priority = 3,
            description = "RB-E3: Accept and Decline in the same instant resolve to one outcome")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Tap Decline then Accept on the same card with no wait. The booking ends in "
            + "exactly one terminal state and the UI agrees with the backend. A booking that is "
            + "both accepted and declined is a data-integrity defect.")
    public void acceptAndDeclineRace() {
        Map<String, Object> seed = readSeed("/tmp/l2b-e3-seed.json", "E3");
        String bookingId = String.valueOf(seed.get("booking_id"));
        String bookingNumber = String.valueOf(seed.get("booking_number"));
        String siteNeedle = "RB-E3";
        Allure.parameter("bookingNumber", bookingNumber);
        Allure.parameter("bookingId", bookingId);

        String token = vendorToken();
        Response before = bookingsApi.detail(token, bookingId);
        assertThat(before.statusCode()).isEqualTo(200);
        assertThat(before.jsonPath().getString("data.status")).isEqualTo("pending");

        QuickBookingPage qb = reachQuickBookingQueue();
        assertThat(DriverManager.get().getPageSource().contains(siteNeedle)
                || DriverManager.get().getPageSource().contains(bookingNumber))
                .as("E3 seed must be on Quick Booking")
                .isTrue();

        qb.raceDeclineThenAcceptNearText(siteNeedle);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        RentalBookingsPage bookings = new RentalBookingsPage();
        boolean declineDialog = bookings.isDeclineBookingDialogVisible();
        boolean assignSheet = bookings.isAssignMachineVisible();
        Allure.parameter("declineDialogVisible", String.valueOf(declineDialog));
        Allure.parameter("assignSheetVisible", String.valueOf(assignSheet));
        bookings.attachScreenshot("rb-e3-after-race");

        // Resolve whichever single UI path won — do not drive both.
        if (declineDialog && assignSheet) {
            assertThat(false)
                    .as("NEW BUG: Decline dialog and Assign sheet both visible after Accept/Decline race")
                    .isTrue();
        }

        if (declineDialog) {
            String reason = bookings.selectFirstDeclineReason();
            Allure.parameter("raceWinner", "decline-dialog");
            Allure.parameter("declineReason", reason);
            assertThat(bookings.isConfirmDeclineEnabled()).isTrue();
            bookings.tapConfirmDecline();
            Waits.until(DriverManager.get(),
                    d -> !bookings.isDeclineBookingDialogVisible()
                            || DriverManager.get().getPageSource().contains("Booking Declined")
                            ? Boolean.TRUE : null,
                    "Decline path after race did not finish",
                    Duration.ofSeconds(15));
            if (DriverManager.get().getPageSource().contains("Booking Declined")) {
                java.util.List<org.openqa.selenium.WebElement> oks =
                        DriverManager.get().findElements(org.openqa.selenium.By.xpath(
                                "//android.widget.TextView[@text='OK']"));
                if (!oks.isEmpty()) {
                    oks.get(0).click();
                }
            }
        } else if (assignSheet) {
            Allure.parameter("raceWinner", "assign-sheet");
            if (bookings.isAssignSkipVisible()) {
                bookings.tapAssignSkip();
                try {
                    Waits.until(DriverManager.get(),
                            d -> !bookings.isAssignMachineVisible() ? Boolean.TRUE : null,
                            "Skip after race Accept did not close Assign",
                            Duration.ofSeconds(10));
                } catch (org.openqa.selenium.TimeoutException e) {
                    Allure.parameter("skipStuckAfterRace", "true");
                    bookings.attachScreenshot("rb-e3-skip-stuck");
                }
            }
        } else {
            Allure.parameter("raceWinner", "neither-sheet");
        }

        Response after = bookingsApi.detail(token, bookingId);
        int afterCode = after.statusCode();
        String statusAfter = afterCode == 200 ? after.jsonPath().getString("data.status") : null;
        Allure.parameter("apiDetailStatusCode", String.valueOf(afterCode));
        Allure.parameter("apiStatusAfter", String.valueOf(statusAfter));

        Response list = bookingsApi.list(token);
        long matching = 0;
        if (list.statusCode() == 200) {
            List<Map<String, Object>> rows = list.jsonPath().getList("data");
            matching = rows.stream()
                    .filter(r -> bookingId.equals(String.valueOf(r.get("id")))
                            || bookingNumber.equals(String.valueOf(r.get("booking_number"))))
                    .count();
        }
        Allure.parameter("listMatchesForSeed", String.valueOf(matching));
        assertThat(matching)
                .as("Race must not duplicate the seed on the vendor list")
                .isLessThanOrEqualTo(1);

        boolean declined = afterCode == 404 || matching == 0
                || "declined".equals(statusAfter)
                || "rejected".equals(statusAfter)
                || "cancelled".equals(statusAfter);
        boolean accepted = "confirmed".equals(statusAfter)
                || "operator_assigned".equals(statusAfter);
        boolean stillPending = "pending".equals(statusAfter);

        Allure.parameter("terminalDeclined", String.valueOf(declined));
        Allure.parameter("terminalAccepted", String.valueOf(accepted));
        Allure.parameter("stillPending", String.valueOf(stillPending));

        assertThat(declined && accepted)
                .as("NEW BUG: booking appears both accepted and declined after race")
                .isFalse();
        assertThat(bookings.isDeclineBookingDialogVisible() && bookings.isAssignMachineVisible())
                .as("NEW BUG: UI shows both Decline dialog and Assign sheet after race")
                .isFalse();

        // Accept won the race but Assign Skip left the user stuck with pending — #29.
        if (stillPending && bookings.isAssignMachineVisible()) {
            assertThat(false)
                    .as("BUGS_FOUND #29 re-observed on RB-E3: Accept won Decline/Accept race, "
                            + "Assign machine stayed open, Skip did not dismiss, status remained "
                            + "pending — user cannot finish Accept")
                    .isTrue();
        }

        if (stillPending && !bookings.isDeclineBookingDialogVisible()
                && !bookings.isAssignMachineVisible()) {
            Allure.parameter("raceOutcome", "noop-still-pending");
            return;
        }

        assertThat(declined || accepted)
                .as("Race must resolve to exactly one terminal state (accepted XOR declined). "
                        + "status=" + statusAfter)
                .isTrue();
    }

    @Test(priority = 4,
            description = "RB-E4: Accept at the moment the timer hits 00:00")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Let a seed request run to the last second and tap Accept as it reaches 00:00. "
            + "Either the accept wins cleanly or the app shows an expired message. A silent "
            + "success on an expired booking is a defect.")
    public void acceptAtTimerZero() {
        Path blockNote = Path.of("/tmp/l2b-e4-blocked.json");
        if (Files.isRegularFile(blockNote)) {
            try {
                Allure.addAttachment("e4-blocked", "application/json",
                        Files.readString(blockNote), ".json");
            } catch (Exception e) {
                Allure.parameter("e4BlockedNote", e.getMessage());
            }
        }
        throw new SkipException(
                "BLOCKED RB-E4: need a pending rental Quick Booking (L2B-RNT-*) on 9000000001 "
                        + "with QB Timer at/near 00:00 in one unbroken session (no relaunch — "
                        + "timer restarts ~29:5x per #28). Same seed gate as RB-S15. Do not "
                        + "fake or shorten the countdown.");
    }

    @Test(priority = 5, description = "RB-E5: Accept while offline")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Disable radios, tap Accept. Expect an error or retry, not a local-only "
            + "success. Restore network: the booking must still be pending, and must not "
            + "auto-accept from a queued request.")
    public void acceptOffline() throws Exception {
        Map<String, Object> seed = readSeed("/tmp/l2b-e5-seed.json", "E5");
        String bookingId = String.valueOf(seed.get("booking_id"));
        String bookingNumber = String.valueOf(seed.get("booking_number"));
        String siteNeedle = "RB-E5";
        Allure.parameter("bookingNumber", bookingNumber);
        Allure.parameter("bookingId", bookingId);

        String token = vendorToken();
        assertThat(bookingsApi.detail(token, bookingId).jsonPath().getString("data.status"))
                .isEqualTo("pending");

        QuickBookingPage qb = reachQuickBookingQueue();
        assertThat(DriverManager.get().getPageSource().contains(siteNeedle)
                || DriverManager.get().getPageSource().contains(bookingNumber))
                .as("E5 seed on Quick Booking")
                .isTrue();

        setAirplaneMode(true);
        Allure.parameter("airplaneMode", "on");
        try {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            qb.tapAcceptNearText(siteNeedle);
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            RentalBookingsPage bookings = new RentalBookingsPage();
            boolean assignOpened = bookings.isAssignMachineVisible();
            String src = DriverManager.get().getPageSource();
            boolean errorChrome = src.contains("No internet") || src.contains("offline")
                    || src.contains("Offline") || src.contains("Network")
                    || src.contains("Something went wrong") || src.contains("Try again")
                    || src.contains("Unable to") || src.contains("connection");
            Allure.parameter("assignOpenedOffline", String.valueOf(assignOpened));
            Allure.parameter("errorChromeOffline", String.valueOf(errorChrome));
            bookings.attachScreenshot("rb-e5-offline-after-accept");

            if (assignOpened && bookings.isAssignSkipVisible()) {
                io.appium.java_client.android.AndroidDriver android =
                        (io.appium.java_client.android.AndroidDriver) DriverManager.get();
                android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                        io.appium.java_client.android.nativekey.AndroidKey.BACK));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Restore happens in finally — then assert.
            Allure.parameter("offlineOpenedAssign", String.valueOf(assignOpened));
            if (assignOpened) {
                assertThat(false)
                        .as("NEW BUG: Accept while offline opened Assign machine — local-only "
                                + "success (user thinks booking is accepted)")
                        .isTrue();
            }
        } finally {
            setAirplaneMode(false);
            Allure.parameter("airplaneModeRestored", "true");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Response after = bookingsApi.detail(token, bookingId);
        String statusAfter = after.jsonPath().getString("data.status");
        Allure.parameter("apiStatusAfterRestore", statusAfter);
        assertThat(statusAfter)
                .as("After offline Accept + network restore, booking must still be pending "
                        + "(must not auto-accept from a queued request)")
                .isEqualTo("pending");
    }

    private void setAirplaneMode(boolean on) throws Exception {
        String value = on ? "1" : "0";
        Process p1 = new ProcessBuilder("adb", "shell", "settings", "put", "global",
                "airplane_mode_on", value).inheritIO().start();
        p1.waitFor();
        Process p2 = new ProcessBuilder("adb", "shell", "am", "broadcast", "-a",
                "android.intent.action.AIRPLANE_MODE", "--ez", "state",
                String.valueOf(on)).inheritIO().start();
        p2.waitFor();
    }

    @Test(priority = 6, description = "RB-E6: Decline while offline")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Same as RB-E5 for Decline, including the reason dialog. No phantom decline "
            + "after the network returns.")
    public void declineOffline() throws Exception {
        Map<String, Object> seed = readSeed("/tmp/l2b-e6-seed.json", "E6");
        String bookingId = String.valueOf(seed.get("booking_id"));
        String bookingNumber = String.valueOf(seed.get("booking_number"));
        String siteNeedle = "RB-E6";
        Allure.parameter("bookingNumber", bookingNumber);
        Allure.parameter("bookingId", bookingId);

        String token = vendorToken();
        assertThat(bookingsApi.detail(token, bookingId).jsonPath().getString("data.status"))
                .isEqualTo("pending");

        QuickBookingPage qb = reachQuickBookingQueue();
        assertThat(DriverManager.get().getPageSource().contains(siteNeedle)
                || DriverManager.get().getPageSource().contains(bookingNumber))
                .as("E6 seed on Quick Booking")
                .isTrue();

        setAirplaneMode(true);
        Allure.parameter("airplaneMode", "on");
        boolean declineDialogOpened = false;
        boolean confirmAttempted = false;
        try {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            qb.tapDeclineNearText(siteNeedle);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            RentalBookingsPage bookings = new RentalBookingsPage();
            declineDialogOpened = bookings.isDeclineBookingDialogVisible();
            Allure.parameter("declineDialogOffline", String.valueOf(declineDialogOpened));
            bookings.attachScreenshot("rb-e6-offline-after-decline");

            if (declineDialogOpened) {
                // If dialog opens offline, completing it must not phantom-decline on restore.
                try {
                    String reason = bookings.selectFirstDeclineReason();
                    Allure.parameter("declineReasonOffline", reason);
                    if (bookings.isConfirmDeclineEnabled()) {
                        confirmAttempted = true;
                        bookings.tapConfirmDecline();
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (Exception pickErr) {
                    Allure.parameter("offlineDeclinePickError", pickErr.getMessage());
                }
                bookings.attachScreenshot("rb-e6-offline-after-confirm-attempt");
            }
        } finally {
            setAirplaneMode(false);
            Allure.parameter("airplaneModeRestored", "true");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Response after = bookingsApi.detail(token, bookingId);
        int afterCode = after.statusCode();
        String statusAfter = afterCode == 200 ? after.jsonPath().getString("data.status") : null;
        Allure.parameter("apiDetailStatusCode", String.valueOf(afterCode));
        Allure.parameter("apiStatusAfterRestore", String.valueOf(statusAfter));
        Allure.parameter("confirmAttemptedOffline", String.valueOf(confirmAttempted));

        boolean phantomDeclined = afterCode == 404
                || "declined".equals(statusAfter)
                || "rejected".equals(statusAfter)
                || "cancelled".equals(statusAfter);
        if (phantomDeclined) {
            assertThat(false)
                    .as("NEW BUG: Decline while offline produced a phantom decline after "
                            + "network restore (detail=" + afterCode + " status=" + statusAfter + ")")
                    .isTrue();
        }
        assertThat(statusAfter)
                .as("After offline Decline + network restore, booking must still be pending")
                .isEqualTo("pending");
    }

    @Test(priority = 7,
            description = "RB-E7: Accept a booking already resolved elsewhere")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Hold a stale card on screen, resolve the same booking from the customer web or "
            + "an API call, then tap Accept. Expect a clear conflict message and a list refresh, "
            + "not a crash or a second acceptance (pairs with RB-A6).")
    public void acceptAlreadyResolvedElsewhere() {
        Map<String, Object> seed = readSeed("/tmp/l2b-e7-seed.json", "E7");
        String bookingId = String.valueOf(seed.get("booking_id"));
        String bookingNumber = String.valueOf(seed.get("booking_number"));
        String siteNeedle = "RB-E7";
        Allure.parameter("bookingNumber", bookingNumber);
        Allure.parameter("bookingId", bookingId);

        String token = vendorToken();
        assertThat(bookingsApi.detail(token, bookingId).jsonPath().getString("data.status"))
                .isEqualTo("pending");

        QuickBookingPage qb = reachQuickBookingQueue();
        assertThat(DriverManager.get().getPageSource().contains(siteNeedle)
                || DriverManager.get().getPageSource().contains(bookingNumber))
                .as("E7 seed must be visible (stale card held)")
                .isTrue();
        qb.attachScreenshot("rb-e7-stale-card-before-resolve");

        // Resolve elsewhere via vendor API Accept while UI still shows the pending card.
        Response remoteAccept = bookingsApi.accept(token, bookingId);
        Allure.parameter("remoteAcceptStatus", String.valueOf(remoteAccept.statusCode()));
        assertThat(remoteAccept.statusCode()).isEqualTo(200);
        String remoteStatus = remoteAccept.jsonPath().getString("data.status");
        Allure.parameter("remoteStatus", remoteStatus);
        assertThat(remoteStatus).isIn("confirmed", "operator_assigned");

        // Stale UI Accept — must not crash or create a second booking.
        qb.tapAcceptNearText(siteNeedle);
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        RentalBookingsPage bookings = new RentalBookingsPage();
        boolean assignOpened = bookings.isAssignMachineVisible();
        String src = DriverManager.get().getPageSource();
        boolean conflictChrome = src.contains("already") || src.contains("Accepted")
                || src.contains("not available") || src.contains("expired")
                || src.contains("Unable") || src.contains("Something went wrong")
                || src.contains("Try again") || src.contains("conflict")
                || src.contains("updated") || src.contains("refresh");
        Allure.parameter("assignOpenedOnStaleAccept", String.valueOf(assignOpened));
        Allure.parameter("conflictChrome", String.valueOf(conflictChrome));
        bookings.attachScreenshot("rb-e7-after-stale-accept");

        Response after = bookingsApi.detail(token, bookingId);
        String statusAfter = after.jsonPath().getString("data.status");
        Allure.parameter("apiStatusAfterStaleAccept", statusAfter);
        assertThat(statusAfter)
                .as("Seed must remain a single accepted booking")
                .isIn("confirmed", "operator_assigned");

        Response list = bookingsApi.list(token);
        long matching = list.jsonPath().getList("data").stream()
                .map(o -> (Map<String, Object>) o)
                .filter(r -> bookingId.equals(String.valueOf(r.get("id")))
                        || bookingNumber.equals(String.valueOf(r.get("booking_number"))))
                .count();
        Allure.parameter("listMatchesForSeed", String.valueOf(matching));
        assertThat(matching)
                .as("Stale Accept must not duplicate the booking on the vendor list")
                .isEqualTo(1);

        // If Assign opens on an already-confirmed booking, user is misled (blocker).
        if (assignOpened) {
            assertThat(false)
                    .as("NEW BUG: Stale Accept on an already-accepted booking opened Assign "
                            + "machine — looks like a second Accept; expected conflict/refresh")
                    .isTrue();
        }

        // No crash: still in vendor package.
        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("App must not crash after stale Accept")
                .isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 8,
            description = "RB-E8: Accept a booking the customer cancelled mid-view")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Open the card, cancel from customer side, then Accept. Expect conflict / gone "
            + "state, not a successful accept of a cancelled booking.")
    public void acceptCustomerCancelledMidView() throws Exception {
        Map<String, Object> seed = readSeed("/tmp/l2b-e8-seed.json", "E8");
        String bookingId = String.valueOf(seed.get("booking_id"));
        String bookingNumber = String.valueOf(seed.get("booking_number"));
        String siteNeedle = "RB-E8";
        Allure.parameter("bookingNumber", bookingNumber);
        Allure.parameter("bookingId", bookingId);

        String token = vendorToken();
        assertThat(bookingsApi.detail(token, bookingId).jsonPath().getString("data.status"))
                .isEqualTo("pending");

        QuickBookingPage qb = reachQuickBookingQueue();
        assertThat(DriverManager.get().getPageSource().contains(siteNeedle)
                || DriverManager.get().getPageSource().contains(bookingNumber))
                .as("E8 seed visible before customer cancel")
                .isTrue();
        qb.attachScreenshot("rb-e8-before-cancel");

        String customerToken = Files.readString(Path.of("/tmp/l2b-customer-token.txt")).trim();
        if (customerToken.isBlank()) {
            throw new SkipException("Missing /tmp/l2b-customer-token.txt for customer cancel");
        }
        Response cancel = io.restassured.RestAssured.given()
                .baseUri(Config.get("api.base.url"))
                .header("Authorization", "Bearer " + customerToken)
                .contentType("application/json")
                .body(Map.of("reason", "RB-E8 mid-view cancel"))
                .post("/api/v1/customer/orders/" + bookingId + "/cancel");
        Allure.parameter("customerCancelStatus", String.valueOf(cancel.statusCode()));
        assertThat(cancel.statusCode())
                .as("Customer cancel must succeed for E8 seed")
                .isIn(200, 201, 204);

        qb.tapAcceptNearText(siteNeedle);
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        RentalBookingsPage bookings = new RentalBookingsPage();
        boolean assignOpened = bookings.isAssignMachineVisible();
        String src = DriverManager.get().getPageSource();
        boolean conflictChrome = src.contains("cancel") || src.contains("Cancel")
                || src.contains("not available") || src.contains("Unable")
                || src.contains("Something went wrong") || src.contains("Try again")
                || src.contains("gone") || src.contains("removed");
        Allure.parameter("assignOpenedAfterCancel", String.valueOf(assignOpened));
        Allure.parameter("conflictChrome", String.valueOf(conflictChrome));
        bookings.attachScreenshot("rb-e8-after-accept-cancelled");

        Response after = bookingsApi.detail(token, bookingId);
        int afterCode = after.statusCode();
        String statusAfter = afterCode == 200 ? after.jsonPath().getString("data.status") : null;
        Allure.parameter("apiDetailStatusCode", String.valueOf(afterCode));
        Allure.parameter("apiStatusAfter", String.valueOf(statusAfter));

        boolean acceptedAnyway = "confirmed".equals(statusAfter)
                || "operator_assigned".equals(statusAfter);
        if (acceptedAnyway || assignOpened) {
            assertThat(false)
                    .as("NEW BUG: Accept after customer cancel "
                            + (assignOpened ? "opened Assign " : "")
                            + (acceptedAnyway ? "and/or confirmed the cancelled booking " : "")
                            + "(detail=" + afterCode + " status=" + statusAfter + ")")
                    .isTrue();
        }

        assertThat(acceptedAnyway)
                .as("Cancelled booking must not become confirmed via stale Accept")
                .isFalse();
        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("App must not crash after Accept on cancelled booking")
                .isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 9,
            description = "RB-E9: Cancelled booking disappears on refresh")
    @Severity(SeverityLevel.NORMAL)
    @Description("After customer cancel, pull-to-refresh / relaunch removes the card from QB.")
    public void cancelledBookingDisappearsOnRefresh() {
        Map<String, Object> seed = readSeed("/tmp/l2b-e9-seed.json", "E9");
        String bookingId = String.valueOf(seed.get("booking_id"));
        String bookingNumber = String.valueOf(seed.get("booking_number"));
        String siteNeedle = "RB-E9";
        Allure.parameter("bookingNumber", bookingNumber);
        Allure.parameter("bookingId", bookingId);

        String token = vendorToken();
        Response detail = bookingsApi.detail(token, bookingId);
        Allure.parameter("apiDetailStatusCode", String.valueOf(detail.statusCode()));
        // Cancelled seed should be gone or non-pending for this vendor.
        assertThat(detail.statusCode() == 404
                || !"pending".equals(detail.jsonPath().getString("data.status")))
                .as("E9 seed must already be cancelled/absent before refresh check")
                .isTrue();

        // Fresh session — cancelled card must not appear on Quick Booking / Home feed.
        try {
            QuickBookingPage qb = reachQuickBookingQueue();
            boolean visible = qb.hasText(siteNeedle) || qb.hasText(bookingNumber)
                    || DriverManager.get().getPageSource().contains(bookingNumber);
            Allure.parameter("visibleOnQbAfterRelaunch", String.valueOf(visible));
            qb.attachScreenshot("rb-e9-qb-after-relaunch");
            assertThat(visible)
                    .as("Cancelled " + bookingNumber + " must not appear on Quick Booking after relaunch")
                    .isFalse();
        } catch (AssertionError noQueue) {
            // No pending queue at all is also success for E9 (cancelled removed).
            Allure.parameter("qbQueueAbsent", "true");
            HomePage home = reachRentalHomeResilient();
            boolean onHome = home.isDisplayedNow()
                    && (DriverManager.get().getPageSource().contains(siteNeedle)
                    || DriverManager.get().getPageSource().contains(bookingNumber));
            Allure.parameter("visibleOnHomeFeed", String.valueOf(onHome));
            home.attachScreenshot("rb-e9-home-after-relaunch");
            assertThat(onHome)
                    .as("Cancelled seed must not linger on Home Booking Orders feed")
                    .isFalse();
        }
    }

    @Test(priority = 10,
            description = "RB-E10: Tab switching while the list is loading")
    @Severity(SeverityLevel.NORMAL)
    @Description("Spam Upcoming/Active/Completed while the list loads. No crash; final tab "
            + "matches content.")
    public void tabSwitchWhileLoading() {
        final RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        assertThat(bookings.areTabsVisible()).as("Bookings tabs visible").isTrue();
        bookings.attachScreenshot("rb-e10-before-spam");

        String[] sequence = {
                RentalBookingsPage.TAB_ACTIVE,
                RentalBookingsPage.TAB_COMPLETED,
                RentalBookingsPage.TAB_UPCOMING,
                RentalBookingsPage.TAB_COMPLETED,
                RentalBookingsPage.TAB_ACTIVE,
                RentalBookingsPage.TAB_UPCOMING,
                RentalBookingsPage.TAB_ACTIVE
        };
        for (String tab : sequence) {
            bookings.tapTab(tab);
            // No intentional wait — spam while list may still be loading.
        }

        String finalTab = RentalBookingsPage.TAB_ACTIVE;
        bookings.tapTab(finalTab);
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String header = bookings.headerTitleNow();
        Allure.parameter("headerAfterSpam", header);
        boolean stillBookings = bookings.isDisplayedNow() && bookings.areTabsVisible();
        Allure.parameter("stillOnBookings", String.valueOf(stillBookings));
        bookings.attachScreenshot("rb-e10-after-spam");

        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("App must not crash during tab spam")
                .isEqualTo(Config.get("app.package"));
        assertThat(stillBookings)
                .as("Must remain on Bookings after rapid tab switches")
                .isTrue();
        assertThat(header)
                .as("Final tab content/header must match Active after last tap")
                .containsIgnoringCase("Active");

        // Cross-check: Active header should not still show Upcoming/Completed title.
        assertThat(header.toLowerCase())
                .as("Header must not stay on wrong tab after settling on Active")
                .doesNotContain("upcoming")
                .doesNotContain("completed");
    }

    @Test(priority = 11,
            description = "RB-E11: Bookings list while offline shows error, not empty")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Airplane mode on Bookings: error chrome, not a false empty state.")
    public void bookingsListOfflineShowsError() throws Exception {
        final RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean hadContentBefore = DriverManager.get().getPageSource().contains("Excavator")
                || DriverManager.get().getPageSource().contains("₹")
                || DriverManager.get().getPageSource().contains("Amount");
        Allure.parameter("hadContentBeforeOffline", String.valueOf(hadContentBefore));
        bookings.attachScreenshot("rb-e11-before-offline");

        setAirplaneMode(true);
        try {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Force a reload by switching tabs offline.
            bookings.tapTab(RentalBookingsPage.TAB_COMPLETED);
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            String src = DriverManager.get().getPageSource();
            boolean errorChrome = src.contains("No internet") || src.contains("offline")
                    || src.contains("Offline") || src.contains("Network")
                    || src.contains("Something went wrong") || src.contains("Try again")
                    || src.contains("Unable to") || src.contains("connection")
                    || src.contains("Check your") || src.contains("Retry");
            boolean emptyState = bookings.isActiveEmptyStateVisible()
                    || src.contains("No upcoming") || src.contains("Nothing here")
                    || src.contains("No bookings") || src.contains("No Upcoming");
            boolean stillLooksLoaded = src.contains("Excavator") || src.contains("Amount ·")
                    || src.contains("Operator") || src.contains("₹");
            Allure.parameter("errorChromeOffline", String.valueOf(errorChrome));
            Allure.parameter("emptyStateOffline", String.valueOf(emptyState));
            Allure.parameter("staleContentOffline", String.valueOf(stillLooksLoaded));
            bookings.attachScreenshot("rb-e11-offline");

            assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                    .as("App must not crash offline on Bookings")
                    .isEqualTo(Config.get("app.package"));

            // Critical: false empty while offline misleads the vendor.
            if (emptyState && !errorChrome && hadContentBefore) {
                assertThat(false)
                        .as("NEW BUG: Bookings offline shows empty state without error chrome "
                                + "— vendor may think they have no bookings")
                        .isTrue();
            }
            if (!errorChrome && !stillLooksLoaded && emptyState) {
                assertThat(false)
                        .as("NEW BUG: Offline Bookings looks empty with no network error")
                        .isTrue();
            }
            // Prefer error chrome. Cached content without error is weaker but not empty-lie.
            if (!errorChrome && !stillLooksLoaded) {
                assertThat(false)
                        .as("NEW BUG: Offline Bookings has neither error chrome nor cached list")
                        .isTrue();
            }
            Allure.parameter("offlineUx",
                    errorChrome ? "error" : stillLooksLoaded ? "stale-cache" : "unknown");
        } finally {
            setAirplaneMode(false);
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test(priority = 12,
            description = "RB-E12: Airplane toggle mid-list self-heals")
    @Severity(SeverityLevel.NORMAL)
    @Description("Toggle airplane while on Bookings; list recovers without stuck spinner.")
    public void airplaneToggleMidListSelfHeals() throws Exception {
        final RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bookings.attachScreenshot("rb-e12-before-toggle");

        setAirplaneMode(true);
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bookings.tapTab(RentalBookingsPage.TAB_COMPLETED);
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bookings.attachScreenshot("rb-e12-while-offline");

        setAirplaneMode(false);
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Nudge refresh after restore.
        bookings.tapTab(RentalBookingsPage.TAB_ACTIVE);
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String src = DriverManager.get().getPageSource();
        boolean stuckSpinner = src.contains("Loading") && !src.contains("Excavator")
                && !src.contains("₹") && !src.contains("No upcoming");
        boolean recovered = bookings.isDisplayedNow() && bookings.areTabsVisible()
                && (src.contains("Excavator") || src.contains("₹") || src.contains("Upcoming")
                || src.contains("No upcoming") || src.contains("Amount"));
        Allure.parameter("stuckSpinner", String.valueOf(stuckSpinner));
        Allure.parameter("recoveredAfterRestore", String.valueOf(recovered));
        bookings.attachScreenshot("rb-e12-after-restore");

        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("App must not crash after airplane toggle")
                .isEqualTo(Config.get("app.package"));
        if (stuckSpinner) {
            assertThat(false)
                    .as("NEW BUG: Bookings stuck on loading spinner after airplane restore")
                    .isTrue();
        }
        assertThat(recovered)
                .as("Bookings must self-heal after airplane toggle (list or empty chrome)")
                .isTrue();
    }

    @Test(priority = 13,
            description = "RB-E13: Background and foreground during an Accept")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Background mid-Accept; foreground must not double-commit.")
    public void backgroundDuringAccept() throws Exception {
        Map<String, Object> seed = readSeed("/tmp/l2b-e13-seed.json", "E13");
        String bookingId = String.valueOf(seed.get("booking_id"));
        String bookingNumber = String.valueOf(seed.get("booking_number"));
        String siteNeedle = "RB-E13";
        Allure.parameter("bookingNumber", bookingNumber);
        Allure.parameter("bookingId", bookingId);

        String token = vendorToken();
        assertThat(bookingsApi.detail(token, bookingId).jsonPath().getString("data.status"))
                .isEqualTo("pending");

        QuickBookingPage qb = reachQuickBookingQueue();
        assertThat(DriverManager.get().getPageSource().contains(siteNeedle)
                || DriverManager.get().getPageSource().contains(bookingNumber))
                .as("E13 seed on queue")
                .isTrue();

        qb.tapAcceptNearText(siteNeedle);
        // Immediately background before Assign settles.
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        android.runAppInBackground(Duration.ofSeconds(3));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        RentalBookingsPage bookings = new RentalBookingsPage();
        boolean assignVisible = bookings.isAssignMachineVisible();
        Allure.parameter("assignAfterForeground", String.valueOf(assignVisible));
        bookings.attachScreenshot("rb-e13-after-foreground");

        Response after = bookingsApi.detail(token, bookingId);
        String statusAfter = after.jsonPath().getString("data.status");
        Allure.parameter("apiStatusAfter", statusAfter);

        Response list = bookingsApi.list(token);
        long matching = list.jsonPath().getList("data").stream()
                .map(o -> (Map<?, ?>) o)
                .filter(r -> bookingId.equals(String.valueOf(r.get("id")))
                        || bookingNumber.equals(String.valueOf(r.get("booking_number"))))
                .count();
        Allure.parameter("listMatches", String.valueOf(matching));
        assertThat(matching)
                .as("Background mid-Accept must not duplicate the booking")
                .isEqualTo(1);

        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("App must not crash after background mid-Accept")
                .isEqualTo(Config.get("app.package"));

        // If Assign is up and Skip works, dismiss once — do not Confirm twice.
        if (assignVisible && bookings.isAssignSkipVisible()) {
            bookings.tapAssignSkip();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (bookings.isAssignMachineVisible()) {
                Allure.parameter("skipStuckE13", "true");
                // #29 class on background path
                assertThat(false)
                        .as("BUGS_FOUND #29 re-observed on RB-E13: Assign stayed open after "
                                + "background/foreground Accept (Skip stuck, status="
                                + statusAfter + ")")
                        .isTrue();
            }
        }

        Response finalDetail = bookingsApi.detail(token, bookingId);
        String finalStatus = finalDetail.jsonPath().getString("data.status");
        Allure.parameter("apiStatusFinal", finalStatus);
        assertThat(list.jsonPath().getList("data").stream()
                .map(o -> (Map<?, ?>) o)
                .filter(r -> bookingId.equals(String.valueOf(r.get("id")))).count())
                .as("Still exactly one row after foreground")
                .isEqualTo(1);
    }

    @Test(priority = 14,
            description = "RB-E14: Force-stop and relaunch from the list")
    @Severity(SeverityLevel.NORMAL)
    @Description("Force-stop on Bookings; relaunch lands safely without corrupt state.")
    public void forceStopRelaunchFromList() {
        final RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bookings.attachScreenshot("rb-e14-before-force-stop");

        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.terminateApp(pkg);
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        android.activateApp(pkg);

        final HomePage home = new HomePage();
        final QuickBookingPage qb = new QuickBookingPage();
        Waits.until(DriverManager.get(),
                d -> (home.isDisplayedNow() || qb.isDisplayedNow()) ? Boolean.TRUE : null,
                "After force-stop relaunch, neither Home nor QB appeared",
                Duration.ofSeconds(25));
        Allure.parameter("postRelaunch",
                home.isDisplayedNow() ? "home" : qb.isDisplayedNow() ? "qb" : "other");
        home.attachScreenshot("rb-e14-after-relaunch");

        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("Must be back in vendor package after force-stop")
                .isEqualTo(pkg);
        assertThat(home.isDisplayedNow() || qb.isDisplayedNow())
                .as("Relaunch must land on Home or Quick Booking — not a blank/corrupt screen")
                .isTrue();

        if (qb.isDisplayedNow()) {
            qb.tapClose();
            Waits.until(DriverManager.get(),
                    d -> home.isDisplayedNow() ? Boolean.TRUE : null,
                    "Close after relaunch did not reach Home",
                    Duration.ofSeconds(12));
        }
        assertThat(home.isDisplayedNow()).as("Usable Home after force-stop path").isTrue();
    }

    @Test(priority = 15, description = "RB-E15: Rotation on the Bookings list")
    @Severity(SeverityLevel.NORMAL)
    @Description("Rotate on Upcoming/Active/Completed; chrome remains usable.")
    public void rotationOnBookingsList() {
        final RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String headerBefore = bookings.headerTitleNow();
        Allure.parameter("headerPortrait", headerBefore);
        bookings.attachScreenshot("rb-e15-portrait");

        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        android.rotate(org.openqa.selenium.ScreenOrientation.LANDSCAPE);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String srcLand = DriverManager.get().getPageSource();
        boolean tabsLand = bookings.areTabsVisible();
        boolean drawerLand = srcLand.contains("Log Out") || srcLand.contains("Account")
                || srcLand.contains("KYC");
        String headerLand = bookings.headerTitleNow();
        Allure.parameter("tabsLandscape", String.valueOf(tabsLand));
        Allure.parameter("drawerLandscape", String.valueOf(drawerLand));
        Allure.parameter("headerLandscape", headerLand);
        bookings.attachScreenshot("rb-e15-landscape");

        android.rotate(org.openqa.selenium.ScreenOrientation.PORTRAIT);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean tabsPort = bookings.areTabsVisible();
        boolean drawerPort = DriverManager.get().getPageSource().contains("Log Out");
        String headerPort = bookings.headerTitleNow();
        Allure.parameter("tabsPortraitRestore", String.valueOf(tabsPort));
        Allure.parameter("drawerPortrait", String.valueOf(drawerPort));
        Allure.parameter("headerPortraitRestore", headerPort);
        bookings.attachScreenshot("rb-e15-portrait-restore");

        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("App must not crash on Bookings rotation")
                .isEqualTo(Config.get("app.package"));

        if (drawerLand && !tabsLand) {
            assertThat(false)
                    .as("NEW BUG: Rotating Bookings to landscape opens account drawer and "
                            + "hides tabs/list — vendor cannot use Bookings in landscape")
                    .isTrue();
        }
        if (drawerPort && !tabsPort) {
            assertThat(false)
                    .as("NEW BUG: After landscape→portrait, Bookings still shows account drawer "
                            + "instead of list tabs")
                    .isTrue();
        }
        assertThat(tabsPort)
                .as("Bookings tabs must be usable after portrait restore")
                .isTrue();
        assertThat(bookings.isDisplayedNow())
                .as("Must remain on Bookings after rotation")
                .isTrue();
    }

    @Test(priority = 16,
            description = "RB-E16: Long machine name and long address stay in the card")
    @Severity(SeverityLevel.NORMAL)
    @Description("Long strings do not overflow CTAs or crash the list.")
    public void longNamesStayInCard() {
        Map<String, Object> seed = readSeed("/tmp/l2b-e16-seed.json", "E16");
        String bookingId = String.valueOf(seed.get("booking_id"));
        String bookingNumber = String.valueOf(seed.get("booking_number"));
        String siteNeedle = "RB-E16";
        String machineName = String.valueOf(seed.getOrDefault("machine_name", "Excavator"));
        String fullAddress = String.valueOf(seed.getOrDefault("site_address", ""));
        Allure.parameter("bookingNumber", bookingNumber);
        Allure.parameter("bookingId", bookingId);
        Allure.parameter("machineName", machineName);
        Allure.parameter("fullAddressLength", String.valueOf(fullAddress.length()));

        String token = vendorToken();
        Response before = bookingsApi.detail(token, bookingId);
        assertThat(before.statusCode()).isEqualTo(200);
        assertThat(before.jsonPath().getString("data.status")).isEqualTo("pending");

        QuickBookingPage qb = reachQuickBookingQueue();
        assertThat(qb.isDisplayedNow()).as("Quick Booking queue").isTrue();

        // Scroll until the long-address seed card is on screen (do not Accept).
        org.openqa.selenium.WebElement label = null;
        for (int i = 0; i < 8; i++) {
            java.util.List<org.openqa.selenium.WebElement> hits =
                    DriverManager.get().findElements(
                            com.l2b.vendor.core.locators.ComposeLocators.textViewContains(siteNeedle));
            if (!hits.isEmpty()) {
                label = hits.get(0);
                break;
            }
            org.openqa.selenium.Dimension size = DriverManager.get().manage().window().getSize();
            DriverManager.get().executeScript("mobile: swipeGesture", java.util.Map.of(
                    "left", (int) (size.width * 0.2),
                    "top", (int) (size.height * 0.55),
                    "width", (int) (size.width * 0.6),
                    "height", (int) (size.height * 0.25),
                    "direction", "up",
                    "percent", 0.75));
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        assertThat(label)
                .as("E16 long-address seed must appear on Quick Booking")
                .isNotNull();

        String shownAddress = label.getText() == null ? "" : label.getText().trim();
        Allure.parameter("shownAddressLength", String.valueOf(shownAddress.length()));
        Allure.parameter("shownAddressPreview",
                shownAddress.length() > 120 ? shownAddress.substring(0, 120) + "…" : shownAddress);
        qb.attachScreenshot("rb-e16-qb-long-card");

        // Machine title near the seed card.
        boolean machineVisible = DriverManager.get().getPageSource().contains(machineName)
                || DriverManager.get().getPageSource().contains("Excavator");
        Allure.parameter("machineVisible", String.valueOf(machineVisible));
        assertThat(machineVisible)
                .as("Machine title must remain readable on the long-address card")
                .isTrue();

        // Bounds: address / machine must not cover Accept or Decline CTAs.
        org.openqa.selenium.Rectangle addrBox = label.getRect();
        org.openqa.selenium.WebElement accept =
                nearestQbClickable("Accept", addrBox);
        org.openqa.selenium.WebElement decline =
                nearestQbClickable("Decline", addrBox);
        org.openqa.selenium.Rectangle aBox = accept.getRect();
        org.openqa.selenium.Rectangle dBox = decline.getRect();
        Allure.parameter("addressBounds",
                addrBox.x + "," + addrBox.y + "," + addrBox.width + "," + addrBox.height);
        Allure.parameter("acceptBounds",
                aBox.x + "," + aBox.y + "," + aBox.width + "," + aBox.height);
        Allure.parameter("declineBounds",
                dBox.x + "," + dBox.y + "," + dBox.width + "," + dBox.height);

        boolean acceptOverlap = rectsOverlap(addrBox, aBox);
        boolean declineOverlap = rectsOverlap(addrBox, dBox);
        Allure.parameter("addressOverlapsAccept", String.valueOf(acceptOverlap));
        Allure.parameter("addressOverlapsDecline", String.valueOf(declineOverlap));

        boolean acceptEnabled = accept.isEnabled() && aBox.height > 20 && aBox.width > 40;
        boolean declineEnabled = decline.isEnabled() && dBox.height > 20 && dBox.width > 40;
        Allure.parameter("acceptUsable", String.valueOf(acceptEnabled));
        Allure.parameter("declineUsable", String.valueOf(declineEnabled));

        // Truncation is OK; blank / missing prefix is not.
        assertThat(shownAddress)
                .as("Address line under Booking for must not be blank for long seed")
                .isNotBlank();
        assertThat(shownAddress)
                .as("Visible address must keep the RB-E16 seed prefix")
                .contains(siteNeedle);

        // Extremely short clipped line that loses all site detail is a product layout bug
        // when the API stored a long address (vendor cannot identify the site).
        if (fullAddress.length() > 80 && shownAddress.length() < 12) {
            assertThat(false)
                    .as("NEW BUG: Long site address collapsed to nearly empty card text "
                            + "(shownLen=" + shownAddress.length() + ", apiLen="
                            + fullAddress.length() + ")")
                    .isTrue();
        }

        if (acceptOverlap || declineOverlap) {
            assertThat(false)
                    .as("NEW BUG: Long site address overlaps Accept/Decline CTAs on Quick "
                            + "Booking card — vendor cannot safely act (acceptOverlap="
                            + acceptOverlap + ", declineOverlap=" + declineOverlap + ")")
                    .isTrue();
        }
        if (!acceptEnabled || !declineEnabled) {
            assertThat(false)
                    .as("NEW BUG: Long address card leaves Accept/Decline unusable "
                            + "(accept=" + acceptEnabled + ", decline=" + declineEnabled + ")")
                    .isTrue();
        }

        // Screen width: text that draws past the card/CTA column is overflow.
        int screenW = DriverManager.get().manage().window().getSize().width;
        boolean addressPastScreen = addrBox.x + addrBox.width > screenW + 8;
        Allure.parameter("addressPastScreen", String.valueOf(addressPastScreen));
        if (addressPastScreen) {
            assertThat(false)
                    .as("NEW BUG: Long address text overflows past the screen edge "
                            + "(x+w=" + (addrBox.x + addrBox.width) + ", screenW=" + screenW + ")")
                    .isTrue();
        }

        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("App must not crash rendering long address/machine on QB card")
                .isEqualTo(Config.get("app.package"));
        assertThat(qb.isAcceptVisible() && qb.isDeclineVisible())
                .as("Accept and Decline must stay visible on the long-text card")
                .isTrue();

        // Leave seed pending — no Accept (layout-only case).
        Response after = bookingsApi.detail(token, bookingId);
        assertThat(after.jsonPath().getString("data.status"))
                .as("E16 is layout-only — seed must remain pending")
                .isEqualTo("pending");
    }

    private static boolean rectsOverlap(org.openqa.selenium.Rectangle a,
            org.openqa.selenium.Rectangle b) {
        return a.x < b.x + b.width && a.x + a.width > b.x
                && a.y < b.y + b.height && a.y + a.height > b.y;
    }

    private org.openqa.selenium.WebElement nearestQbClickable(String label,
            org.openqa.selenium.Rectangle near) {
        java.util.List<org.openqa.selenium.WebElement> rows = DriverManager.get().findElements(
                com.l2b.vendor.core.locators.ComposeLocators.clickableWithText(label));
        assertThat(rows).as(label + " on Quick Booking").isNotEmpty();
        org.openqa.selenium.WebElement best = null;
        int bestDist = Integer.MAX_VALUE;
        for (org.openqa.selenium.WebElement row : rows) {
            org.openqa.selenium.Rectangle box = row.getRect();
            int dist = Math.abs(box.getY() - near.getY());
            if (dist < bestDist) {
                bestDist = dist;
                best = row;
            }
        }
        assertThat(best).as(label + " near long card").isNotNull();
        assertThat(bestDist)
                .as(label + " must be on the same card as the long address")
                .isLessThan(900);
        return best;
    }

    @Test(priority = 17,
            description = "RB-E17: Missing or placeholder field values")
    @Severity(SeverityLevel.NORMAL)
    @Description("Placeholder / missing fields render safely without blanking the card.")
    public void missingPlaceholderFieldsSafe() {
        String token = vendorToken();
        Response list = bookingsApi.list(token);
        assertThat(list.statusCode()).isEqualTo(200);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");
        java.util.List<String> apiEmptyAddress = new java.util.ArrayList<>();
        java.util.List<String> apiMissingSku = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            String bn = String.valueOf(row.get("booking_number"));
            Object site = row.get("site_address");
            if (site == null) {
                site = row.get("delivery_address");
            }
            String siteStr = site == null ? "" : String.valueOf(site).trim();
            if (siteStr.isEmpty() || siteStr.equalsIgnoreCase("null")
                    || siteStr.equalsIgnoreCase("Address not provided")
                    || siteStr.equals("-") || siteStr.equalsIgnoreCase("N/A")) {
                apiEmptyAddress.add(bn + ":" + row.get("status"));
            }
            Object sku = row.get("sku_name");
            if (sku == null || String.valueOf(sku).trim().isEmpty()) {
                apiMissingSku.add(bn);
            }
        }
        Allure.parameter("apiEmptyAddressBookings", apiEmptyAddress.toString());
        Allure.parameter("apiMissingSkuBookings", apiMissingSku.toString());

        final RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        java.util.List<String> uiPlaceholders = new java.util.ArrayList<>();
        java.util.List<String> uiBlanks = new java.util.ArrayList<>();
        java.util.List<String> uiBadMachines = new java.util.ArrayList<>();

        String[] tabs = {
                RentalBookingsPage.TAB_UPCOMING,
                RentalBookingsPage.TAB_ACTIVE,
                RentalBookingsPage.TAB_COMPLETED
        };
        for (String tab : tabs) {
            bookings.tapTab(tab);
            try {
                Thread.sleep(900);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Scroll a few times so off-screen cards are sampled.
            for (int swipe = 0; swipe < 3; swipe++) {
                java.util.List<String> addresses = bookings.bookingForAddressesNow();
                for (String a : addresses) {
                    if (a == null || a.trim().isEmpty()) {
                        uiBlanks.add(tab + ":blank");
                    } else if (a.equalsIgnoreCase("Address not provided")
                            || a.equalsIgnoreCase("N/A")
                            || a.equals("-")
                            || a.equalsIgnoreCase("null")
                            || a.equalsIgnoreCase("Not available")) {
                        uiPlaceholders.add(tab + ":" + a);
                    }
                }
                java.util.List<String> machines = bookings.machineTitlesNow();
                for (String m : machines) {
                    if (m == null) {
                        continue;
                    }
                    String t = m.trim();
                    if (t.equalsIgnoreCase("N/A") || t.equals("-")
                            || t.equalsIgnoreCase("Unknown")
                            || t.equalsIgnoreCase("null")
                            || t.equalsIgnoreCase("Machine name not provided")) {
                        uiBadMachines.add(tab + ":" + t);
                    }
                }
                String src = DriverManager.get().getPageSource();
                if (src.contains("Address not provided") || src.contains("address not provided")) {
                    uiPlaceholders.add(tab + ":Address not provided(source)");
                }
                if (src.contains("null") && src.toLowerCase().contains("booking for")) {
                    // Soft signal only — Compose sometimes exposes literal null.
                    Allure.parameter("nullTokenNearBookingFor_" + tab,
                            String.valueOf(src.contains("null")));
                }
                org.openqa.selenium.Dimension size =
                        DriverManager.get().manage().window().getSize();
                DriverManager.get().executeScript("mobile: swipeGesture", java.util.Map.of(
                        "left", (int) (size.width * 0.2),
                        "top", (int) (size.height * 0.45),
                        "width", (int) (size.width * 0.6),
                        "height", (int) (size.height * 0.35),
                        "direction", "up",
                        "percent", 0.7));
                try {
                    Thread.sleep(350);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            bookings.attachScreenshot("rb-e17-" + tab.toLowerCase());
        }

        Allure.parameter("uiBlankAddresses", uiBlanks.toString());
        Allure.parameter("uiPlaceholderAddresses", uiPlaceholders.toString());
        Allure.parameter("uiBadMachines", uiBadMachines.toString());

        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("App must not crash while scanning placeholder fields")
                .isEqualTo(Config.get("app.package"));

        if (!uiBlanks.isEmpty()) {
            assertThat(false)
                    .as("NEW BUG: Bookings card leaves a blank line under Booking for "
                            + uiBlanks)
                    .isTrue();
        }
        if (!uiPlaceholders.isEmpty()) {
            assertThat(false)
                    .as("NEW BUG: Bookings shows placeholder/missing site address on card "
                            + uiPlaceholders
                            + " (API empty-address rows=" + apiEmptyAddress + "). "
                            + "Vendor cannot identify the job site. Not folded into Home #20.")
                    .isTrue();
        }
        if (!uiBadMachines.isEmpty()) {
            assertThat(false)
                    .as("NEW BUG: Bookings shows placeholder/missing machine title "
                            + uiBadMachines)
                    .isTrue();
        }

        // API has empty site but UI never surfaces it — still a data defect for Upcoming
        // cards that should show Booking for. Fail when those bookings are confirmed/
        // assigned (visible Upcoming) so the vendor is not flying blind.
        boolean upcomingEmptyApi = apiEmptyAddress.stream()
                .anyMatch(s -> s.contains("confirmed") || s.contains("operator_assigned")
                        || s.contains("pending"));
        if (upcomingEmptyApi && uiPlaceholders.isEmpty() && uiBlanks.isEmpty()) {
            // Cross-check detail for first empty-address booking — does UI hide bad data?
            String sampleBn = apiEmptyAddress.get(0).split(":")[0];
            Map<String, Object> sample = rows.stream()
                    .filter(r -> sampleBn.equals(String.valueOf(r.get("booking_number"))))
                    .findFirst()
                    .orElse(null);
            if (sample != null) {
                String id = String.valueOf(sample.get("id"));
                Response detail = bookingsApi.detail(token, id);
                String detailSite = detail.jsonPath().getString("data.site_address");
                if (detailSite == null) {
                    detailSite = detail.jsonPath().getString("data.delivery_address");
                }
                Allure.parameter("sampleEmptyDetailSite", String.valueOf(detailSite));
                bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                String src = DriverManager.get().getPageSource();
                boolean cardVisible = src.contains(sampleBn)
                        || src.contains(String.valueOf(sample.get("sku_name")));
                Allure.parameter("emptyAddressCardVisible", String.valueOf(cardVisible));
                if (cardVisible && (detailSite == null || detailSite.isBlank())) {
                    assertThat(false)
                            .as("NEW BUG: Upcoming/confirmed booking " + sampleBn
                                    + " has empty site_address in API and no safe placeholder "
                                    + "on the card — site identity missing for vendor")
                            .isTrue();
                }
            }
        }

        // Clean: no blanks, no placeholders, no bad machines.
        assertThat(uiBlanks).isEmpty();
        assertThat(uiPlaceholders).isEmpty();
        assertThat(uiBadMachines).isEmpty();
    }

    @Test(priority = 18,
            description = "RB-E18: All three tabs on a zero-booking account")
    @Severity(SeverityLevel.NORMAL)
    @Description("Empty-state copy on Upcoming/Active/Completed for a clean account.")
    public void zeroBookingAccountTabs() {
        String phone = Config.get("user.rental.individual.phone");
        Allure.parameter("phone", phone);
        assertThat(phone).as("Zero-booking control account").isEqualTo("9000000003");

        // API gate: must truly be empty — otherwise BLOCKED, do not fake PASS.
        String token;
        {
            auth.sendOtp(phone);
            Response verify = auth.verifyOtp(phone, "1234");
            assertThat(verify.statusCode()).isEqualTo(200);
            token = verify.jsonPath().getString("access_token");
            if (token == null || token.isBlank()) {
                token = verify.jsonPath().getString("data.access_token");
            }
        }
        Response list = bookingsApi.list(token);
        assertThat(list.statusCode()).isEqualTo(200);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");
        int apiCount = rows == null ? -1 : rows.size();
        Allure.parameter("apiBookingCount", String.valueOf(apiCount));
        if (apiCount != 0) {
            throw new SkipException(
                    "BLOCKED RB-E18: 9000000003 is not zero-booking (apiCount=" + apiCount
                            + "). Need a clean rental individual with 0 bookings.");
        }

        OtpPage otp = openOtp(phone);
        otp.focusOtpField();
        if (!otp.otpFieldText().isEmpty()) {
            otp.clearOtp();
        }
        otp.pressDigitKeys("1234");

        HomePage home = new HomePage();
        QuickBookingPage qb = new QuickBookingPage();
        Waits.until(DriverManager.get(),
                d -> (home.isDisplayedNow() || qb.isDisplayedNow()) ? Boolean.TRUE : null,
                "After OTP on 9000000003, neither Home nor Quick Booking",
                Duration.ofSeconds(20));
        Allure.parameter("postOtpLanding",
                qb.isDisplayedNow() ? "quick-booking" : home.isDisplayedNow() ? "home" : "other");
        if (qb.isDisplayedNow()) {
            assertThat(false)
                    .as("NEW BUG: Zero-booking account 9000000003 opened Quick Booking "
                            + "with an empty API list — false Accept queue")
                    .isTrue();
        }
        assertThat(home.isDisplayedNow()).as("Zero-booking must land on Home").isTrue();
        home.attachScreenshot("rb-e18-home");

        // Open Bookings via Upcoming See all (same index as 0001).
        home.tapNthSeeAll(1);
        RentalBookingsPage bookings = new RentalBookingsPage();
        try {
            Waits.until(DriverManager.get(),
                    d -> bookings.isDisplayedNow() ? Boolean.TRUE : null,
                    "See all did not open Bookings for zero-booking account",
                    Duration.ofSeconds(12));
        } catch (org.openqa.selenium.TimeoutException e) {
            home.attachScreenshot("rb-e18-see-all-failed");
            assertThat(false)
                    .as("NEW BUG: Zero-booking Home See all did not open Bookings — "
                            + "vendor cannot inspect empty Upcoming/Active/Completed")
                    .isTrue();
        }

        java.util.LinkedHashMap<String, String> emptyCopy = new java.util.LinkedHashMap<>();
        String[] tabs = {
                RentalBookingsPage.TAB_UPCOMING,
                RentalBookingsPage.TAB_ACTIVE,
                RentalBookingsPage.TAB_COMPLETED
        };
        for (String tab : tabs) {
            bookings.tapTab(tab);
            try {
                Thread.sleep(900);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            String src = DriverManager.get().getPageSource();
            int cards = bookings.cardCountNow();
            boolean emptyActive = bookings.isActiveEmptyStateVisible();
            boolean emptyUpcoming = src.contains("No upcoming") || src.contains("No Upcoming")
                    || src.contains("No bookings") || src.contains("nothing here")
                    || src.contains("Nothing here") || src.contains("No Upcoming Booking");
            boolean emptyCompleted = src.contains("No completed") || src.contains("No Completed")
                    || src.contains("No past") || src.contains("No Completed Order");
            boolean genericEmpty = src.contains("No active bookings.")
                    || src.contains("No bookings yet")
                    || src.contains("You have no")
                    || emptyActive;
            boolean hasEmpty = emptyActive || emptyUpcoming || emptyCompleted || genericEmpty
                    || (tab.equals(RentalBookingsPage.TAB_ACTIVE) && emptyActive);
            // Tab-specific: Active known copy; Upcoming/Completed may share pattern.
            if (tab.equals(RentalBookingsPage.TAB_ACTIVE)) {
                hasEmpty = emptyActive || src.contains("No active bookings");
            } else if (tab.equals(RentalBookingsPage.TAB_UPCOMING)) {
                hasEmpty = emptyUpcoming || genericEmpty || (cards == 0 && !src.contains("Amount ·")
                        && !src.contains("Excavator") && !src.contains("Operator"));
            } else {
                hasEmpty = emptyCompleted || genericEmpty || (cards == 0 && !src.contains("₹")
                        && !src.contains("Excavator"));
            }
            Allure.parameter("cards_" + tab, String.valueOf(cards));
            Allure.parameter("emptySignal_" + tab, String.valueOf(hasEmpty));
            emptyCopy.put(tab, hasEmpty ? "empty-ok" : "MISSING-EMPTY-OR-HAS-CARDS");
            bookings.attachScreenshot("rb-e18-" + tab.toLowerCase());

            assertThat(cards)
                    .as("Zero-booking " + tab + " must not show booking cards")
                    .isEqualTo(0);
            if (!hasEmpty) {
                assertThat(false)
                        .as("NEW BUG: Zero-booking " + tab
                                + " tab has 0 cards but no empty-state copy — blank list "
                                + "misleads the vendor")
                        .isTrue();
            }
        }
        Allure.parameter("emptyCopyByTab", emptyCopy.toString());
        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("App must not crash on zero-booking Bookings tabs")
                .isEqualTo(Config.get("app.package"));
        assertThat(bookings.areTabsVisible()).as("Tabs remain usable").isTrue();
    }

    @Test(priority = 19,
            description = "RB-E19: Expired session while on Bookings")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Invalidate session on Bookings; expect re-auth, not a silent broken list.")
    public void expiredSessionOnBookings() {
        final RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        boolean hadCards = bookings.cardCountNow() > 0
                || DriverManager.get().getPageSource().contains("Excavator")
                || DriverManager.get().getPageSource().contains("Amount");
        Allure.parameter("hadCardsBeforeInvalidate", String.valueOf(hadCards));
        bookings.attachScreenshot("rb-e19-before-invalidate");

        // Kill every vendor session server-side (including the app's refresh token).
        String token = vendorToken();
        Response logoutAll = io.restassured.RestAssured.given()
                .baseUri(Config.get("api.base.url"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .contentType("application/json")
                .body("{}")
                .post("/api/v1/auth/logout-all");
        Allure.parameter("logoutAllStatus", String.valueOf(logoutAll.statusCode()));
        assertThat(logoutAll.statusCode())
                .as("logout-all must succeed to invalidate the app session")
                .isEqualTo(200);

        // Prove API is 401 with the killed token.
        Response denied = bookingsApi.list(token);
        Allure.parameter("listAfterLogoutAll", String.valueOf(denied.statusCode()));
        assertThat(denied.statusCode())
                .as("Vendor list must reject the invalidated token")
                .isEqualTo(401);

        // Force the app to hit the network again.
        bookings.tapTab(RentalBookingsPage.TAB_COMPLETED);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Extra nudge: background/foreground.
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.runAppInBackground(Duration.ofSeconds(2));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String src = DriverManager.get().getPageSource();
        boolean onBookings = bookings.isDisplayedNow() && bookings.areTabsVisible();
        boolean reauth = src.contains("Sign up") || src.contains("Get OTP")
                || src.contains("Verify OTP") || src.contains("Welcome to L2B")
                || src.contains("Continue") && src.contains("mobile")
                || src.contains("Enter mobile") || src.contains("Log in")
                || src.contains("Login") || src.contains("Session expired")
                || src.contains("session has expired") || src.contains("Please login")
                || src.contains("Please log in") || src.contains("Sign in");
        boolean sessionError = src.contains("Session") || src.contains("unauthorized")
                || src.contains("Unauthorized") || src.contains("401")
                || src.contains("log in again") || src.contains("Login again");
        boolean falseEmpty = bookings.isActiveEmptyStateVisible()
                || src.contains("No upcoming") || src.contains("No bookings")
                || (bookings.cardCountNow() == 0 && onBookings && !reauth && !sessionError);
        boolean stillLooksAuthedWithData = onBookings && (src.contains("Excavator")
                || src.contains("Amount ·") || src.contains("Operator"));

        Allure.parameter("onBookingsAfter", String.valueOf(onBookings));
        Allure.parameter("reauthChrome", String.valueOf(reauth));
        Allure.parameter("sessionErrorChrome", String.valueOf(sessionError));
        Allure.parameter("falseEmptyAfterInvalidate", String.valueOf(falseEmpty));
        Allure.parameter("staleAuthedContent", String.valueOf(stillLooksAuthedWithData));
        Allure.parameter("packageAfter", String.valueOf(
                DriverManager.get().getCapabilities().getCapability("appPackage")));
        bookings.attachScreenshot("rb-e19-after-invalidate");

        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("App must not crash when session is revoked on Bookings")
                .isEqualTo(pkg);

        if (falseEmpty && hadCards && !reauth && !sessionError) {
            assertThat(false)
                    .as("NEW BUG: After logout-all, Bookings shows empty list without "
                            + "re-auth — vendor thinks they have no bookings (silent "
                            + "expired session)")
                    .isTrue();
        }
        if (stillLooksAuthedWithData && !reauth) {
            // Stale cached cards after server kill — soft fail if tab switch still
            // presents them as live without any session warning.
            assertThat(false)
                    .as("NEW BUG: After logout-all, Bookings still shows booking cards "
                            + "with no re-auth / session-expired chrome — stale session UX")
                    .isTrue();
        }
        if (!reauth && !sessionError && onBookings) {
            assertThat(false)
                    .as("NEW BUG: Session invalidated via logout-all while on Bookings, "
                            + "but UI stayed on Bookings with no re-auth and no session error")
                    .isTrue();
        }
        assertThat(reauth || sessionError)
                .as("Expired session on Bookings must force re-auth or show session error")
                .isTrue();
    }

    @Test(priority = 20,
            description = "RB-E20: Rapid Confirm taps on Assign produce one assignment")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Double/triple Confirm on Assign after operator pick — one assignment only.")
    public void rapidAssignConfirmOnce() {
        Map<String, Object> seed = readSeed("/tmp/l2b-e20-seed.json", "E20");
        String bookingId = String.valueOf(seed.get("booking_id"));
        String bookingNumber = String.valueOf(seed.get("booking_number"));
        String siteNeedle = "RB-E20";
        Allure.parameter("bookingNumber", bookingNumber);
        Allure.parameter("bookingId", bookingId);

        String token = vendorToken();
        Response before = bookingsApi.detail(token, bookingId);
        assertThat(before.statusCode()).isEqualTo(200);
        assertThat(before.jsonPath().getString("data.status")).isEqualTo("pending");

        QuickBookingPage qb = reachQuickBookingQueue();
        assertThat(qb.isDisplayedNow()).as("Quick Booking queue").isTrue();
        assertThat(DriverManager.get().getPageSource().contains(siteNeedle)
                || DriverManager.get().getPageSource().contains(bookingNumber))
                .as("E20 seed must be on Quick Booking")
                .isTrue();

        qb.tapAcceptNearText(siteNeedle);
        RentalBookingsPage sheet = new RentalBookingsPage();
        Waits.until(DriverManager.get(),
                d -> sheet.isAssignMachineVisible() ? Boolean.TRUE : null,
                "Accept did not open Assign machine for E20",
                Duration.ofSeconds(15));
        sheet.attachScreenshot("rb-e20-assign-open");

        String operator = sheet.selectAvailableOperatorContaining("Randanberno");
        Allure.parameter("chosenOperator", operator);
        assertThat(operator).as("Need an Available operator for rapid Confirm").isNotBlank();
        boolean confirmEnabled = sheet.isAssignConfirmEnabled();
        Allure.parameter("confirmEnabledAfterPick", String.valueOf(confirmEnabled));
        if (!confirmEnabled) {
            sheet.attachScreenshot("rb-e20-confirm-disabled");
            assertThat(confirmEnabled)
                    .as("BUGS_FOUND #16: Confirm disabled after Available operator pick "
                            + "— cannot execute rapid Confirm (busy-slot or broader)")
                    .isTrue();
            return;
        }

        // Triple rapid Confirm on the same enabled CTA.
        java.util.List<org.openqa.selenium.WebElement> confirms =
                DriverManager.get().findElements(
                        com.l2b.vendor.core.locators.ComposeLocators.clickableWithText("Confirm"));
        assertThat(confirms).as("Confirm outer View on Assign").isNotEmpty();
        org.openqa.selenium.Rectangle box = confirms.get(0).getRect();
        int x = box.x + box.width / 2;
        int y = box.y + box.height / 2;
        Allure.parameter("confirmTapXY", x + "," + y);
        for (int i = 0; i < 3; i++) {
            DriverManager.get().executeScript("mobile: clickGesture",
                    java.util.Map.of("x", x, "y", y));
        }
        sheet.attachScreenshot("rb-e20-after-triple-confirm");

        try {
            Waits.until(DriverManager.get(),
                    d -> !sheet.isAssignMachineVisible() ? Boolean.TRUE : null,
                    "Assign sheet did not close after rapid Confirm",
                    Duration.ofSeconds(15));
        } catch (org.openqa.selenium.TimeoutException stuck) {
            Allure.parameter("assignStuckAfterRapidConfirm", "true");
            sheet.attachScreenshot("rb-e20-assign-stuck");
            if (sheet.isAssignMachineVisible()) {
                assertThat(false)
                        .as("NEW BUG: After triple Confirm on Assign, sheet stayed open "
                                + "(possible #29-family commit failure)")
                        .isTrue();
            }
        }

        // Fresh token — prior vendorToken may still be valid.
        token = vendorToken();
        Response after = bookingsApi.detail(token, bookingId);
        String statusAfter = after.jsonPath().getString("data.status");
        Allure.parameter("apiStatusAfter", statusAfter);
        Allure.parameter("apiOperatorAfter",
                String.valueOf(after.jsonPath().getString("data.assigned_operator_name")));

        Response list = bookingsApi.list(token);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");
        long matching = rows.stream()
                .filter(r -> bookingId.equals(String.valueOf(r.get("id")))
                        || bookingNumber.equals(String.valueOf(r.get("booking_number"))))
                .count();
        Allure.parameter("listMatchesForSeed", String.valueOf(matching));
        assertThat(matching)
                .as("Rapid Confirm must not duplicate the booking row")
                .isEqualTo(1);

        assertThat(statusAfter)
                .as("Seed must be assigned/confirmed exactly once after rapid Confirm")
                .isIn("confirmed", "operator_assigned");
        assertThat(sheet.isAssignMachineVisible())
                .as("Assign sheet must dismiss after a single successful Confirm")
                .isFalse();
        assertThat(DriverManager.get().getCapabilities().getCapability("appPackage"))
                .as("App must not crash on rapid Assign Confirm")
                .isEqualTo(Config.get("app.package"));
    }

    private String vendorToken() {
        String phone = Config.get("user.rental.company.phone");
        auth.sendOtp(phone);
        Response verify = auth.verifyOtp(phone, "1234");
        assertThat(verify.statusCode()).isEqualTo(200);
        String token = verify.jsonPath().getString("access_token");
        assertThat(token).isNotBlank();
        return token;
    }

    private Map<String, Object> readSeed(String path, String label) {
        Path p = Path.of(path);
        if (!Files.isRegularFile(p)) {
            throw new SkipException("Missing " + path + " — place RB-" + label + " customer seed first");
        }
        try {
            String raw = Files.readString(p);
            io.restassured.path.json.JsonPath jp = new io.restassured.path.json.JsonPath(raw);
            Map<String, Object> map = jp.getMap("");
            if (map == null || map.get("booking_id") == null || map.get("booking_number") == null) {
                throw new SkipException(label + " seed file incomplete: " + raw);
            }
            return map;
        } catch (SkipException e) {
            throw e;
        } catch (Exception e) {
            throw new SkipException("Cannot read " + label + " seed: " + e.getMessage());
        }
    }
}
