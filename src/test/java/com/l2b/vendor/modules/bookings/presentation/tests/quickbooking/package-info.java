/**
 * Quick Booking tests split by QA persona so each account's edges run in isolation.
 *
 * <ul>
 *   <li>{@code RentalCompanyQuickBookingTest} — 9000000001 pending rental queue</li>
 *   <li>{@code RentalIndividualQuickBookingTest} — 9000000003 zero rental bookings</li>
 *   <li>{@code MaterialVendorQuickBookingTest} — 9000000017 pending material orders</li>
 *   <li>{@code OperatorQuickBookingTest} — 9000000002 operator post-OTP landing</li>
 * </ul>
 *
 * <p>Isolated XML under {@code src/test/resources/quick-booking/}. Not in default
 * {@code testng.xml} until review. Never tap Accept or Decline.
 */
package com.l2b.vendor.modules.bookings.presentation.tests.quickbooking;
