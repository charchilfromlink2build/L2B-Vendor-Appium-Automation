package com.l2b.vendor.modules.bookings.presentation.tests.rentalbooking;

import com.l2b.vendor.modules.bookings.presentation.pages.RentalBookingsPage;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;

/**
 * Shared entry for Rental Booking cases on {@code 9000000001}.
 *
 * <p>Inherits the proven Rental Home path: OTP {@code 1234} → Quick Booking →
 * one {@code Close} → usable Home (fails as BUGS_FOUND #15 if the extend-time
 * dialog owns the window). Bookings is then reached from Home → Upcoming
 * Booking → {@code See all}.
 *
 * <p><b>On hold.</b> The entry helper throws until the live rental booking seed
 * exists, so nothing in this package can start a session or touch the queue by
 * accident. Standing rules that still apply when it is enabled:
 * <ul>
 *   <li>Accept / Decline / Assign only on bookings seeded from the customer web
 *       for this exercise — never on the pre-existing 0001 queue.</li>
 *   <li>Never tap Log Out.</li>
 *   <li>A new Bookings defect gets a new sequential number. Never folded into
 *       #13–#16 (Quick Booking) or #17–#26 (Home).</li>
 * </ul>
 */
public abstract class RentalBookingBaseTest extends RentalHomeBaseTest {

    protected static final String ON_HOLD =
            "Rental Booking automation is on hold until the live customer-web booking seed. "
                    + "Case is specified only — no Appium steps run.";

    /**
     * Home → Upcoming Booking {@code See all} → Bookings list.
     *
     * @return the Bookings page once implemented
     * @throws UnsupportedOperationException while the module is on hold
     */
    protected RentalBookingsPage openBookingsFromHomeSeeAll() {
        throw new UnsupportedOperationException(ON_HOLD);
    }

    /**
     * Home → Booking Orders {@code See all} → Quick Booking queue, the surface an
     * incoming rental request lands on.
     *
     * @throws UnsupportedOperationException while the module is on hold
     */
    protected void openIncomingQueueFromHomeSeeAll() {
        throw new UnsupportedOperationException(ON_HOLD);
    }
}
