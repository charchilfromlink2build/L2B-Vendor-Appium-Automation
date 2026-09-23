package com.l2b.vendor.modules.bookings.presentation.tests.rentalbooking;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.data.api.BookingsApi;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * RB-S: the rental booking lifecycle end to end, from a request seeded on the
 * customer web to a completed booking. These are the only Rental Booking cases
 * allowed to change state, and only against seed bookings created for this
 * exercise — never the pre-existing 0001 queue.
 *
 * <p>Each case pairs a UI assertion with the backend state it implies; the API
 * half is cross-referenced in RB-A. Isolated
 * {@code bookings/booking-rental-lifecycle-s*.xml}.
 */
@Epic("Vendor app")
@Feature("Rental Booking lifecycle — rental vendor 9000000001")
public class RentalBookingLifecycleTest extends RentalBookingBaseTest {

    private static final String S1_BOOKING_NUMBER = "L2B-RNT-2026-6419DF";
    private static final String S1_BOOKING_ID = "75b35da9-a2bc-4b1a-9b06-5e7cefbd9f02";
    private static final String S1_SITE = "RB-S1 Test Site";

    private final AuthApi auth = new AuthApi();
    private final BookingsApi bookingsApi = new BookingsApi();

    @Test(priority = 1,
            description = "RB-S1: a booking placed on customer web reaches the vendor app")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Place a rental booking for a 0001 machine on the customer site. With the app "
            + "already open on Home, record how it arrives: push notification, Booking Orders "
            + "feed, Quick Booking intercept on next launch, or only after refresh. Capture the "
            + "delay. Cross-check RB-A1 list status = pending.")
    public void seededBookingReachesVendor() {
        // API half first — seed must already be pending on this vendor (customer API place).
        String token = vendorToken();
        Response list = bookingsApi.list(token);
        assertThat(list.statusCode()).isEqualTo(200);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");
        Map<String, Object> seed = rows.stream()
                .filter(r -> S1_BOOKING_NUMBER.equals(String.valueOf(r.get("booking_number"))))
                .findFirst()
                .orElse(null);
        if (seed == null) {
            throw new SkipException("Seed " + S1_BOOKING_NUMBER + " not on vendor list — re-place");
        }
        Allure.parameter("bookingNumber", S1_BOOKING_NUMBER);
        Allure.parameter("bookingId", String.valueOf(seed.get("id")));
        Allure.parameter("apiStatus", String.valueOf(seed.get("status")));
        assertThat(seed.get("status"))
                .as("RB-A1 cross-check: seed must be pending on vendor list")
                .isEqualTo("pending");

        long t0 = System.currentTimeMillis();
        QuickBookingPage qb = reachQuickBookingQueue();
        long delayMs = System.currentTimeMillis() - t0;

        boolean excavator = qb.hasText("Excavator 20 Tonnes");
        boolean site = qb.hasText(S1_SITE) || qb.hasText("RB-S1");
        boolean timer = qb.isTimerVisible();
        String timerVal = qb.firstTimerValue();
        boolean accept = qb.isAcceptVisible();
        boolean decline = qb.isDeclineVisible();
        String amount = qb.firstAmountLine();
        String arrival = qb.isDisplayedNow() ? "quick-booking-intercept" : "unknown";

        Allure.parameter("arrivalPath", arrival);
        Allure.parameter("loginToQueueMs", String.valueOf(delayMs));
        Allure.parameter("excavatorVisible", String.valueOf(excavator));
        Allure.parameter("siteVisible", String.valueOf(site));
        Allure.parameter("timerVisible", String.valueOf(timer));
        Allure.parameter("timerValue", timerVal);
        Allure.parameter("acceptVisible", String.valueOf(accept));
        Allure.parameter("declineVisible", String.valueOf(decline));
        Allure.parameter("amountLine", amount);
        qb.attachScreenshot("rb-s1-seed-on-queue");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(arrival)
                .as("Pending customer seed must intercept post-OTP as Quick Booking")
                .isEqualTo("quick-booking-intercept");
        assertThat(excavator)
                .as("Seed Excavator 20 Tonnes must render on the queue card")
                .isTrue();
        assertThat(timer && accept && decline)
                .as("Incoming card must show Timer + Accept + Decline")
                .isTrue();
        // Do not Accept/Decline here — RB-S2/S3.
    }

    @Test(priority = 2,
            description = "RB-S2: incoming request shows countdown plus Decline and Accept")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The incoming card shows Timer starting at 30:00, Decline, and Accept, and the "
            + "amount / dates / address match what the customer entered. Read only — the action "
            + "taps are RB-S3 and RB-S8.")
    public void incomingRequestShowsTimerAndActions() {
        QuickBookingPage qb = reachQuickBookingQueue();

        boolean timer = qb.isTimerVisible();
        String timerVal = qb.firstTimerValue();
        boolean accept = qb.isAcceptVisible();
        boolean decline = qb.isDeclineVisible();
        boolean acceptEnabled = qb.isAcceptEnabled();
        boolean declineEnabled = qb.isDeclineEnabled();
        String amount = qb.firstAmountLine();
        boolean excavator = qb.hasText("Excavator 20 Tonnes");
        boolean site = qb.hasText(S1_SITE) || qb.hasText("RB-S1") || qb.hasText("Bengaluru");
        boolean dateRange = qb.hasText("day") || qb.hasText("Day") || qb.hasText("Sep");

        Allure.parameter("timerVisible", String.valueOf(timer));
        Allure.parameter("timerValue", timerVal);
        Allure.parameter("acceptVisible", String.valueOf(accept));
        Allure.parameter("declineVisible", String.valueOf(decline));
        Allure.parameter("acceptEnabled", String.valueOf(acceptEnabled));
        Allure.parameter("declineEnabled", String.valueOf(declineEnabled));
        Allure.parameter("amountLine", amount);
        Allure.parameter("excavatorVisible", String.valueOf(excavator));
        Allure.parameter("siteOrCityVisible", String.valueOf(site));
        Allure.parameter("dateChromeVisible", String.valueOf(dateRange));
        qb.attachScreenshot("rb-s2-incoming-card");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(timer).as("Timer label must show on incoming card").isTrue();
        assertThat(timerVal)
                .as("Timer must show mm:ss countdown (near 30:00 on a fresh seed)")
                .matches("\\d{1,2}:\\d{2}");
        assertThat(accept && decline)
                .as("Decline and Accept must both render")
                .isTrue();
        assertThat(acceptEnabled && declineEnabled)
                .as("Accept and Decline outer Views must be enabled")
                .isTrue();
        assertThat(excavator).as("Seed machine Excavator 20 Tonnes").isTrue();
        assertThat(amount)
                .as("Amount line must carry ₹ and payment mode")
                .contains("₹")
                .contains("Amount");
        // Read only — do not tap Accept/Decline (RB-S3 / RB-S8).
    }

    @Test(priority = 3,
            description = "RB-S3: Accept opens Assign machine")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Tap Accept once on the seed booking. Expect the Assign machine sheet with a "
            + "machine preselected and Select an operator. Record Confirm enablement — BUGS_FOUND "
            + "#16 is the known blocker when all operators are busy; if it reproduces here it is "
            + "referenced, not renumbered.")
    public void acceptOpensAssignMachine() {
        QuickBookingPage qb = reachQuickBookingQueue();
        assertThat(qb.hasText("Excavator 20 Tonnes"))
                .as("Seed Excavator must still be on the queue before Accept")
                .isTrue();

        com.l2b.vendor.modules.bookings.presentation.pages.RentalBookingsPage bookings =
                new com.l2b.vendor.modules.bookings.presentation.pages.RentalBookingsPage();
        qb.tapFirstAccept();
        Waits.until(DriverManager.get(),
                d -> bookings.isAssignMachineVisible() ? Boolean.TRUE : null,
                "Accept did not open Assign machine / Assign Operator sheet",
                Duration.ofSeconds(12));

        boolean selectOp = bookings.isSelectOperatorVisible();
        boolean skip = bookings.isAssignSkipVisible();
        boolean confirmEnabled = bookings.isAssignConfirmEnabled();
        Allure.parameter("assignSheetVisible", "true");
        Allure.parameter("selectOperatorVisible", String.valueOf(selectOp));
        Allure.parameter("skipVisible", String.valueOf(skip));
        Allure.parameter("confirmEnabled", String.valueOf(confirmEnabled));
        Allure.parameter("bug16BusySlot", String.valueOf(!confirmEnabled));
        bookings.attachScreenshot("rb-s3-assign-after-accept");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(bookings.isAssignMachineVisible())
                .as("Accept must open Assign machine / Assign Operator")
                .isTrue();
        // Confirm may be disabled when all operators busy (#16) — record, do not renumber.
        // Do not Confirm here — RB-S4 commits.
        if (skip) {
            bookings.tapAssignSkip();
        } else {
            ((io.appium.java_client.android.AndroidDriver) DriverManager.get())
                    .pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                            io.appium.java_client.android.nativekey.AndroidKey.BACK));
        }
    }

    @Test(priority = 4,
            description = "RB-S4: machine plus operator selection enables Confirm and commits")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Select machine and a free operator, then Confirm. The sheet closes and the "
            + "app reports success. Backend: booking status leaves pending and carries the "
            + "chosen machine and operator (RB-A5, RB-A8).")
    public void assignConfirmCommitsAcceptance() {
        QuickBookingPage qb = reachQuickBookingQueue();
        com.l2b.vendor.modules.bookings.presentation.pages.RentalBookingsPage bookings =
                new com.l2b.vendor.modules.bookings.presentation.pages.RentalBookingsPage();

        qb.tapFirstAccept();
        Waits.until(DriverManager.get(),
                d -> bookings.isAssignMachineVisible() ? Boolean.TRUE : null,
                "Accept did not open Assign sheet",
                Duration.ofSeconds(12));

        boolean confirmBefore = bookings.isAssignConfirmEnabled();
        String pageBefore = DriverManager.get().getPageSource();
        boolean busyBanner = pageBefore.contains("All operators are busy")
                || pageBefore.contains("operators are busy for this slot");
        Allure.parameter("confirmBeforePick", String.valueOf(confirmBefore));
        Allure.parameter("busyBanner", String.valueOf(busyBanner));

        String operator = bookings.selectAvailableOperator();
        boolean rowEnabled = bookings.lastMenuOptionEnabled();
        boolean confirmAfter = bookings.isAssignConfirmEnabled();
        Allure.parameter("chosenOperator", operator);
        Allure.parameter("operatorRowEnabled", String.valueOf(rowEnabled));
        Allure.parameter("confirmAfterPick", String.valueOf(confirmAfter));
        bookings.attachScreenshot("rb-s4-after-operator-pick");

        assertThat(operator).as("An operator must be chosen").isNotBlank();
        if (!confirmAfter) {
            Allure.parameter("bug", "16");
            bookings.attachScreenshot("rb-s4-confirm-disabled-bug16");
            assertThat(confirmAfter)
                    .as("BUGS_FOUND #16: Confirm stays disabled after operator pick "
                            + "(busy-slot or broader). Recorded, not renumbered.")
                    .isTrue();
            return;
        }

        bookings.tapAssignConfirm();
        Waits.until(DriverManager.get(),
                d -> !bookings.isAssignMachineVisible() ? Boolean.TRUE : null,
                "Assign sheet did not close after Confirm",
                Duration.ofSeconds(15));

        String token = vendorToken();
        Response detail = bookingsApi.detail(token, S1_BOOKING_ID);
        Allure.parameter("apiStatusAfter", detail.jsonPath().getString("data.status"));
        Allure.parameter("apiOperatorAfter",
                String.valueOf(detail.jsonPath().getString("data.assigned_operator_name")));
        bookings.attachScreenshot("rb-s4-after-confirm");

        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(detail.jsonPath().getString("data.status"))
                .as("After Confirm, seed must leave pending (confirmed or operator_assigned)")
                .isIn("confirmed", "operator_assigned");
    }

    @Test(priority = 5,
            description = "RB-S5: accepted booking leaves the incoming queue")
    @Severity(SeverityLevel.CRITICAL)
    @Description("After Accept, the card is gone from Booking Orders and from the Quick Booking "
            + "queue, and does not return after relaunch. A ghost card that survives restart is "
            + "a new Bookings bug.")
    public void acceptedBookingLeavesQueue() {
        // Seed 6419DF is operator_assigned after RB-S4 — relaunch must not show it on QB.
        String token = vendorToken();
        Response detail = bookingsApi.detail(token, S1_BOOKING_ID);
        String apiStatus = detail.jsonPath().getString("data.status");
        Allure.parameter("apiStatus", apiStatus);
        assertThat(apiStatus)
                .as("RB-S5 expects seed already accepted (run after RB-S4)")
                .isIn("confirmed", "operator_assigned");

        OtpPage otp = openOtp(rentalCompanyPhone());
        otp.focusOtpField();
        if (!otp.otpFieldText().isEmpty()) {
            otp.clearOtp();
        }
        otp.pressDigitKeys("1234");

        QuickBookingPage qb = new QuickBookingPage();
        HomePage home = new HomePage();
        com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage landing =
                new com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage();
        Waits.until(DriverManager.get(),
                d -> (qb.isDisplayedNow() || home.isDisplayedNow()
                        || landing.isExtendTimeDialogVisible()) ? Boolean.TRUE : null,
                "Post-OTP landing missing after accept",
                Duration.ofSeconds(20));

        String landingName = qb.isDisplayedNow() ? "quick-booking"
                : home.isDisplayedNow() ? "home" : "extend-time";
        boolean seedOnQueue = false;
        if (qb.isDisplayedNow()) {
            String src = DriverManager.get().getPageSource();
            seedOnQueue = src.contains(S1_BOOKING_NUMBER)
                    || src.contains("6419DF")
                    || (src.contains(S1_SITE) && src.contains("Excavator 20 Tonnes"));
            Allure.parameter("qbAcceptCount", String.valueOf(qb.acceptCount()));
            qb.attachScreenshot("rb-s5-queue-after-accept");
        } else {
            home.attachScreenshot("rb-s5-home-no-queue");
        }
        Allure.parameter("postOtpLanding", landingName);
        Allure.parameter("seedStillOnQueue", String.valueOf(seedOnQueue));

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(seedOnQueue)
                .as("Accepted seed must not remain on Quick Booking after relaunch")
                .isFalse();
    }

    @Test(priority = 6,
            description = "RB-S6: accepted booking appears under Bookings → Upcoming")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Open Bookings → Upcoming. The seed booking is listed with the assigned "
            + "operator ('Operator : <name>' plus Change), the assigned plate, the customer "
            + "address, and the same amount and dates as the request.")
    public void acceptedBookingAppearsInUpcoming() {
        com.l2b.vendor.modules.bookings.presentation.pages.RentalBookingsPage bookings =
                openBookingsFromHomeSeeAll();

        boolean excavator = DriverManager.get().getPageSource().contains("Excavator 20 Tonnes");
        boolean operator = bookings.assignedOperatorRowsNow().stream()
                .anyMatch(r -> r.contains("Randanberno") || r.contains("Operator :"));
        boolean plate = bookings.collectPlatesByScrolling(6).contains("KA13Z2117")
                || DriverManager.get().getPageSource().contains("KA13Z2117");
        boolean site = DriverManager.get().getPageSource().contains(S1_SITE)
                || DriverManager.get().getPageSource().contains("RB-S1");
        boolean change = bookings.changeCount() > 0;
        int cards = bookings.cardCountNow();

        Allure.parameter("header", bookings.headerTitleNow());
        Allure.parameter("cardCount", String.valueOf(cards));
        Allure.parameter("excavatorVisible", String.valueOf(excavator));
        Allure.parameter("operatorRowVisible", String.valueOf(operator));
        Allure.parameter("plateVisible", String.valueOf(plate));
        Allure.parameter("siteVisible", String.valueOf(site));
        Allure.parameter("changeCtaVisible", String.valueOf(change));
        bookings.attachScreenshot("rb-s6-upcoming-after-accept");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(bookings.headerTitleNow()).isEqualTo(
                com.l2b.vendor.modules.bookings.presentation.pages.RentalBookingsPage.TITLE_UPCOMING);
        assertThat(excavator).as("Accepted Excavator seed on Upcoming").isTrue();
        assertThat(operator).as("Operator : row after assign").isTrue();
        assertThat(plate || change)
                .as("Assigned plate or Change CTA must show on Upcoming")
                .isTrue();
    }

    @Test(enabled = false, priority = 7,
            description = "RB-S7: Home stats move after acceptance")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Capture Upcoming Booking count and Earning Projected before and after Accept. "
            + "Both must move consistently with the accepted amount, and match the dashboard API "
            + "(RB-A3). A stale Home until relaunch is a new Bookings/Home sync bug.")
    public void homeStatsReflectAcceptance() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 8,
            description = "RB-S8: Decline with a reason removes the request")
    @Severity(SeverityLevel.CRITICAL)
    @Description("On a second seed booking, tap Decline, pick a reason, confirm. The request "
            + "disappears from the queue and the customer side reflects the decline. Backend: "
            + "decline reason persisted (RB-A7).")
    public void declineWithReasonRemovesRequest() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 9,
            description = "RB-S9: declined booking is in no vendor bucket")
    @Severity(SeverityLevel.CRITICAL)
    @Description("After the decline, the booking is absent from Upcoming, Active, and Completed, "
            + "and absent from the list API for this vendor.")
    public void declinedBookingNotInAnyBucket() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 10,
            description = "RB-S10: Assign on an unassigned upcoming booking flips the row")
    @Severity(SeverityLevel.CRITICAL)
    @Description("On a card showing 'Operator Not Assigned', complete an assignment. The row "
            + "becomes 'Operator : <name>' with Change without leaving the list, and survives "
            + "a relaunch.")
    public void assignOnUpcomingFlipsRow() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 11,
            description = "RB-S11: Change operator propagates to calendar and team")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Change the operator on an assigned Upcoming card and Confirm. Calendar and "
            + "team views for the new window show the new operator; the old operator is freed.")
    public void changeOperatorPropagates() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 12,
            description = "RB-S12: Upcoming → Active at start time")
    @Severity(SeverityLevel.BLOCKER)
    @Description("When scheduled_start arrives (or is advanced in QA), the booking moves from "
            + "Upcoming to Active with Start OTP chrome. Backend status in_progress (RB-A).")
    public void upcomingBecomesActiveAtStart() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 13,
            description = "RB-S13: Active → Completed at completion")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Complete End OTP. Booking leaves Active and appears under Completed with the "
            + "summary card (machine, dates, bare ₹). Backend status completed.")
    public void activeBecomesCompleted() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 14,
            description = "RB-S14: Completed booking is immutable")
    @Severity(SeverityLevel.CRITICAL)
    @Description("From Completed, Assign / Change / cancel are unreachable. API mutations on the "
            + "completed id are refused.")
    public void completedBookingImmutable() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 15,
            description = "RB-S15: untouched request expires at 00:00 and is not booked")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Leave a seed request untouched until the Timer hits 00:00. The booking is "
            + "expired / unfulfilled rather than accepted.")
    public void expiredRequestIsNotBooked() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 16,
            description = "RB-S16: extend-time decision updates the booking end date")
    @Severity(SeverityLevel.CRITICAL)
    @Description("When a Request to extend time dialog is present for a seed booking in the "
            + "vendor app, the 'Request to extend time' dialog (BUGS_FOUND #15 path) shows the "
            + "new end; Accept/Decline of the extension updates scheduled_end.")
    public void extendTimeUpdatesEndDate() {
        throw new SkipException(ON_HOLD);
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
}
