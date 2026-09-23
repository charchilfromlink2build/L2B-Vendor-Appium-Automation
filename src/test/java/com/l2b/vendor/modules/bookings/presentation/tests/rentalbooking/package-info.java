/**
 * Rental Booking cases for {@code 9000000001}, prepared 22 Sep 2026 and held for
 * execution against real bookings placed from the customer web.
 *
 * <p>Naming follows Rental Home: one page object, one base test, then landing /
 * interact / edge classes, plus lifecycle and API classes this module needs.
 *
 * <table>
 *   <caption>Case groups</caption>
 *   <tr><th>Prefix</th><th>Class</th><th>Count</th><th>Scope</th></tr>
 *   <tr><td>RB-L</td><td>{@code RentalBookingLandingTest}</td><td>16</td>
 *       <td>List, tabs, card fields, empty states — read-only</td></tr>
 *   <tr><td>RB-I</td><td>{@code RentalBookingInteractTest}</td><td>16</td>
 *       <td>Taps and navigation that must not change booking state</td></tr>
 *   <tr><td>RB-S</td><td>{@code RentalBookingLifecycleTest}</td><td>16</td>
 *       <td>Request to completion, the only state-changing group</td></tr>
 *   <tr><td>RB-E</td><td>{@code RentalBookingEdgeTest}</td><td>20</td>
 *       <td>Spam, race, offline, interruption, bad data, session</td></tr>
 *   <tr><td>RB-A</td><td>{@code RentalBookingApiTest}</td><td>14</td>
 *       <td>Endpoint contract and backend state consistency</td></tr>
 * </table>
 *
 * <p>82 cases total. Full specification, the user / app / backend activity map,
 * and the execution protocol live in {@code docs/rental-booking-test-plan.md}.
 *
 * <p><b>Status:</b> Step 1 page helpers are live on {@code RentalBookingsPage}
 * (Accept / Decline / Assign / Change / Confirm + Assign-machine sheet). All 82
 * test methods remain {@code enabled = false} and throw
 * {@link org.testng.SkipException} if forced — Step 2 Landing has not started.
 * Isolated suites under {@code src/test/resources/bookings/} report zero tests
 * until a case is enabled on execution day.
 *
 * <p>Running an on-hold suite reports {@code Tests run: 0}, but the Allure TestNG
 * listener still schedules a status-less result per method in the class. Those
 * entries surface as {@code unknown} in a generated report and must be dropped
 * from {@code target/allure-results} before publishing, until the cases are live.
 *
 * <p>Rules carried over from Quick Booking and Rental Home: never tap Accept,
 * Decline, or Assign on the pre-existing 0001 queue; never tap Log Out; a new
 * Bookings defect always gets a new sequential number and is never merged into
 * #13–#16 (Quick Booking) or #17–#26 (Home).
 */
package com.l2b.vendor.modules.bookings.presentation.tests.rentalbooking;
