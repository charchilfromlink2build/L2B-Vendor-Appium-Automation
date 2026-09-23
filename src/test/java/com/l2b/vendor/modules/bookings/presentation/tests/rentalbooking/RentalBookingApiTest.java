package com.l2b.vendor.modules.bookings.presentation.tests.rentalbooking;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * RB-A: API and backend consistency for Rental Booking, using
 * {@link com.l2b.vendor.modules.bookings.data.api.BookingsApi} against
 * {@code https://qa.waardian.com}.
 *
 * <p>Read endpoints may run on their own. Write endpoints (accept, decline,
 * assign) are called only to verify what the app just did, or against a seed
 * booking reserved for that case — never to drain the 0001 queue.
 *
 * <p>Isolated {@code bookings/booking-rental-api-a*.xml}.
 */
@Epic("Vendor app")
@Feature("Rental Booking API — rental vendor 9000000001")
public class RentalBookingApiTest extends RentalBookingBaseTest {

    @Test(enabled = false, priority = 1,
            description = "RB-A1: GET vendor bookings returns 200 with a usable shape")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /api/v1/rentals/vendor/bookings with a valid token. 200, parseable body, "
            + "and every item carries id, status, machine, operator, amount, start, end, and "
            + "address. Missing fields explain the blank UI rows in RB-E17.")
    public void listReturnsUsableShape() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 2,
            description = "RB-A2: API statuses map onto the three app tabs — LIVE in ApiGapTest")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Implemented in RentalBookingApiGapTest.statusToTabMapping (pure API). Suite "
            + "booking-rental-api-a2.xml points there. Scaffold method kept disabled to avoid "
            + "Appium session overhead.")
    public void statusToTabMapping() {
        throw new SkipException("RB-A2 lives in RentalBookingApiGapTest — use booking-rental-api-a2.xml");
    }

    @Test(enabled = false, priority = 3,
            description = "RB-A3: list counts agree with dashboard — LIVE in ApiGapTest")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Implemented in RentalBookingApiGapTest.countsAgreeAcrossSurfaces. Settles "
            + "RB-L14: Home tile = dashboard.stats.upcoming; period-filtered list must match.")
    public void countsAgreeAcrossSurfaces() {
        throw new SkipException("RB-A3 lives in RentalBookingApiGapTest — use booking-rental-api-a3.xml");
    }

    @Test(enabled = false, priority = 4,
            description = "RB-A4: booking detail matches the card")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /api/v1/rentals/bookings/{booking_id} for a card on screen. Amount, dates, "
            + "machine, plate, operator, and address match the rendered card exactly, including "
            + "the rupee value before formatting.")
    public void detailMatchesCard() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 5,
            description = "RB-A5: accept transitions the booking server-side")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Around the RB-S4 accept, capture the booking before and after. Status leaves "
            + "pending, the assigned machine and operator are stored, and the change is visible "
            + "to a fresh list call.")
    public void acceptTransitionsStatus() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 6,
            description = "RB-A6: second accept on the same booking is rejected or idempotent")
    @Severity(SeverityLevel.BLOCKER)
    @Description("POST accept twice for one booking id. The second call must not create a "
            + "second booking or a second charge; expect a conflict status or an idempotent "
            + "repeat of the first result. Backs the UI case RB-E1.")
    public void secondAcceptIsSafe() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 7,
            description = "RB-A7: decline persists the reason")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Around RB-S8, confirm the decline call succeeds, the stored reason matches the "
            + "option chosen in the app, and the booking no longer appears for this vendor.")
    public void declinePersistsReason() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 8,
            description = "RB-A8: assign persists machine and operator")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Around RB-S10 / RB-S11, confirm the assign call stores the machine and "
            + "operator, the detail endpoint reflects it, and a re-assignment replaces rather "
            + "than appends.")
    public void assignPersistsMachineAndOperator() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 9,
            description = "RB-A9: accept without an assignment")
    @Severity(SeverityLevel.NORMAL)
    @Description("Call accept without a prior assign. Document whether the server allows an "
            + "unassigned acceptance — the Upcoming list does show 'Operator Not Assigned' "
            + "cards, so the two behaviours must be consistent.")
    public void acceptWithoutAssign() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 10,
            description = "RB-A10: accept on an expired booking is refused")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Take the booking expired in RB-S15 and call accept. The server must refuse it. "
            + "An accepted expired booking is a high-severity data defect.")
    public void acceptExpiredBookingRefused() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 11,
            description = "RB-A11: booking endpoints reject a missing or expired token")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Call list, detail, accept, decline, and assign with no token and with an "
            + "expired one. Each must return 401 rather than data or a 500. Pairs with the UI "
            + "case RB-E19.")
    public void endpointsRejectBadToken() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 12,
            description = "RB-A12: another vendor's booking id is not reachable — LIVE in ApiGapTest")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Implemented in RentalBookingApiGapTest.otherVendorBookingIsForbidden.")
    public void otherVendorBookingIsForbidden() {
        throw new SkipException("RB-A12 lives in RentalBookingApiGapTest — use booking-rental-api-a12.xml");
    }

    @Test(enabled = false, priority = 13,
            description = "RB-A13: invoice for completed booking — LIVE in ApiGapTest")
    @Severity(SeverityLevel.NORMAL)
    @Description("Implemented in RentalBookingApiGapTest.invoiceForCompletedBooking.")
    public void invoiceForCompletedBooking() {
        throw new SkipException("RB-A13 lives in RentalBookingApiGapTest — use booking-rental-api-a13.xml");
    }

    @Test(enabled = false, priority = 14,
            description = "RB-A14: accepted booking propagates to schedule and earnings")
    @Severity(SeverityLevel.CRITICAL)
    @Description("After acceptance, /api/v1/rentals/vendor/schedule shows the slot and the "
            + "wallet earning summary reflects the projected amount, matching Calendar and "
            + "Earning in the app. Read-only cross-module consistency.")
    public void acceptedBookingPropagates() {
        throw new SkipException(ON_HOLD);
    }
}
