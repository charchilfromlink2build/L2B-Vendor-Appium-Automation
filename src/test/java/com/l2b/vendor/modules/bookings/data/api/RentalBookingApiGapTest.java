package com.l2b.vendor.modules.bookings.data.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.calendar.data.api.CalendarApi;
import com.l2b.vendor.modules.earning.data.api.EarningApi;
import com.l2b.vendor.modules.home.data.api.HomeApi;
import com.l2b.vendor.modules.onboarding.data.api.AuthApi;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.Test;

/**
 * Genuine RB-A gaps not already covered by {@link RentalBookingLifecycleApiTest}.
 * Pure API — no Appium. Isolated {@code bookings/booking-rental-api-a*.xml}.
 *
 * <p>Covered elsewhere (do not duplicate): A1/A5/A6/A8/A9/A11-missing by lifecycle.
 */
@Epic("Vendor app")
@Feature("Rental Booking API gaps — 9000000001")
public class RentalBookingApiGapTest {

    /** Statuses that must land on the Upcoming tab. */
    private static final Set<String> UPCOMING =
            Set.of("pending", "confirmed", "operator_assigned");
    /**
     * Statuses counted by {@code dashboard.stats.upcoming} (Home Upcoming Booking tile).
     * Pending stays in the Quick Booking / accept queue and is excluded from the Home count.
     */
    private static final Set<String> DASHBOARD_UPCOMING =
            Set.of("confirmed", "operator_assigned");
    /** Statuses that must land on the Active tab. */
    private static final Set<String> ACTIVE = Set.of("in_progress");
    /** Statuses that must land on the Completed tab. */
    private static final Set<String> COMPLETED = Set.of("completed");
    /**
     * Terminal / queue-only statuses that must not appear on Upcoming / Active / Completed.
     * Decline removes the row from the vendor list; if any of these appear they still need a
     * known bucket (not "silent disappear").
     */
    private static final Set<String> HIDDEN =
            Set.of("declined", "cancelled", "expired", "rejected");

    private final AuthApi auth = new AuthApi();
    private final BookingsApi bookings = new BookingsApi();
    private final HomeApi homeApi = new HomeApi();
    private final CalendarApi calendarApi = new CalendarApi();
    private final EarningApi earningApi = new EarningApi();

    @Test(priority = 2, description = "RB-A2: API statuses map onto the three app tabs")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Group GET /api/v1/rentals/vendor/bookings by status. Every status must land in "
            + "exactly one of Upcoming / Active / Completed / Hidden. An unmapped status that "
            + "would silently disappear from the UI is a new Bookings bug.")
    public void statusToTabMapping() {
        String token = vendorToken();
        Response response = bookings.list(token);
        record("vendor bookings list", response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        List<Map<String, Object>> rows = response.jsonPath().getList("data");
        assertThat(rows).as("vendor must see at least one booking").isNotEmpty();

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        Map<String, Integer> byTab = new LinkedHashMap<>();
        List<String> unmapped = new ArrayList<>();
        Set<String> distinctStatuses = new LinkedHashSet<>();

        for (Map<String, Object> row : rows) {
            String status = String.valueOf(row.get("status"));
            distinctStatuses.add(status);
            byStatus.merge(status, 1, Integer::sum);
            String tab = tabFor(status);
            if ("UNMAPPED".equals(tab)) {
                unmapped.add(status + ":" + row.get("id"));
            }
            byTab.merge(tab, 1, Integer::sum);
        }

        Allure.parameter("rowCount", String.valueOf(rows.size()));
        Allure.parameter("distinctStatuses", distinctStatuses.toString());
        Allure.parameter("byStatus", byStatus.toString());
        Allure.parameter("byTab", byTab.toString());
        Allure.parameter("unmapped", unmapped.toString());

        assertThat(unmapped)
                .as("Every API status must map to Upcoming / Active / Completed / Hidden — "
                        + "an unmapped status is a new Bookings bug (would vanish from the UI)")
                .isEmpty();

        for (String status : distinctStatuses) {
            assertThat(tabFor(status))
                    .as("status " + status)
                    .isIn("Upcoming", "Active", "Completed", "Hidden");
        }
    }

    @Test(priority = 3, description = "RB-A3: list counts agree with dashboard (and Home tile)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Same-token snapshot: list rows vs /api/v1/rentals/vendor/dashboard stats. "
            + "Dashboard upcoming/completed are period-scoped (scheduled_start in period). "
            + "Home Upcoming Booking tile reads the same dashboard.upcoming value (RB-L14). "
            + "A period-filtered list count that disagrees with dashboard is a data bug. "
            + "/stats returning empty is recorded; UI LazyColumn undercount stays a separate "
            + "watch vs RB-L14.")
    public void countsAgreeAcrossSurfaces() {
        String token = vendorToken();

        Response list = bookings.list(token);
        record("vendor bookings list", list);
        assertThat(list.statusCode()).isEqualTo(200);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");
        assertThat(rows).isNotEmpty();

        Response dash = homeApi.rentalDashboard(token);
        record("rental dashboard", dash);
        assertThat(dash.statusCode()).isEqualTo(200);

        Response statsEndpoint = homeApi.rentalStats(token);
        record("rental stats endpoint", statsEndpoint);
        assertThat(statsEndpoint.statusCode()).isEqualTo(200);
        // /stats returns the stats object at the root (no data envelope), unlike dashboard.
        int upcomingStats = intFrom(statsEndpoint.jsonPath().get("upcoming.value"));
        int completedStats = intFrom(statsEndpoint.jsonPath().get("completed.value"));
        Allure.parameter("statsEndpointStatus", String.valueOf(statsEndpoint.statusCode()));
        Allure.parameter("upcomingStats", String.valueOf(upcomingStats));
        Allure.parameter("completedStats", String.valueOf(completedStats));

        Instant periodStart = Instant.parse(dash.jsonPath().getString("data.stats.period_start"));
        Instant periodEnd = Instant.parse(dash.jsonPath().getString("data.stats.period_end"));
        int upcomingDashboard = intFrom(dash.jsonPath().get("data.stats.upcoming.value"));
        int completedDashboard = intFrom(dash.jsonPath().get("data.stats.completed.value"));

        int upcomingRaw = 0;
        int upcomingInPeriod = 0;
        int dashboardUpcomingInPeriod = 0;
        int pendingInPeriod = 0;
        int completedRaw = 0;
        int completedInPeriod = 0;
        int activeRaw = 0;
        for (Map<String, Object> row : rows) {
            String status = String.valueOf(row.get("status"));
            Instant start = parseInstant(row.get("scheduled_start"));
            boolean inPeriod = start != null
                    && !start.isBefore(periodStart)
                    && !start.isAfter(periodEnd);
            if (UPCOMING.contains(status)) {
                upcomingRaw++;
                if (inPeriod) {
                    upcomingInPeriod++;
                }
            }
            if (DASHBOARD_UPCOMING.contains(status) && inPeriod) {
                dashboardUpcomingInPeriod++;
            }
            if ("pending".equals(status) && inPeriod) {
                pendingInPeriod++;
            }
            if (ACTIVE.contains(status)) {
                activeRaw++;
            } else if (COMPLETED.contains(status)) {
                completedRaw++;
                if (inPeriod) {
                    completedInPeriod++;
                }
            }
        }

        Allure.parameter("periodStart", periodStart.toString());
        Allure.parameter("periodEnd", periodEnd.toString());
        Allure.parameter("listTotal", String.valueOf(rows.size()));
        Allure.parameter("upcomingRaw(list)", String.valueOf(upcomingRaw));
        Allure.parameter("upcomingInPeriod(all incl pending)", String.valueOf(upcomingInPeriod));
        Allure.parameter("pendingInPeriod(excluded from Home)", String.valueOf(pendingInPeriod));
        Allure.parameter("dashboardUpcomingInPeriod(list)", String.valueOf(dashboardUpcomingInPeriod));
        Allure.parameter("upcomingDashboard", String.valueOf(upcomingDashboard));
        Allure.parameter("completedRaw(list)", String.valueOf(completedRaw));
        Allure.parameter("completedInPeriod(list)", String.valueOf(completedInPeriod));
        Allure.parameter("completedDashboard", String.valueOf(completedDashboard));
        Allure.parameter("activeRaw(list)", String.valueOf(activeRaw));
        Allure.parameter("homeTileSource",
                "dashboard.stats.upcoming = confirmed|operator_assigned with scheduled_start in period "
                        + "(pending excluded — still in accept queue)");
        Allure.parameter("upcomingAgrees",
                String.valueOf(dashboardUpcomingInPeriod == upcomingDashboard));
        Allure.parameter("completedAgrees",
                String.valueOf(completedInPeriod == completedDashboard));
        if (upcomingRaw != upcomingDashboard) {
            Allure.parameter("noteRawVsPeriod",
                    "Raw upcoming-tab statuses on list=" + upcomingRaw
                            + " != dashboard=" + upcomingDashboard
                            + " — expected: dashboard is period-scoped and excludes pending.");
        }

        assertThat(upcomingDashboard)
                .as("dashboard.stats.upcoming.value must be readable")
                .isGreaterThanOrEqualTo(0);
        assertThat(dashboardUpcomingInPeriod)
                .as("List count of confirmed|operator_assigned with scheduled_start in dashboard "
                        + "period must equal dashboard.stats.upcoming.value (Home tile). "
                        + "Pending is excluded from the Home count. Mismatch is a data defect.")
                .isEqualTo(upcomingDashboard);
        assertThat(completedInPeriod)
                .as("List count of Completed statuses with scheduled_start in dashboard period "
                        + "must equal dashboard.stats.completed.value")
                .isEqualTo(completedDashboard);
        assertThat(upcomingStats)
                .as("/stats upcoming.value must match dashboard.stats.upcoming.value")
                .isEqualTo(upcomingDashboard);
        assertThat(completedStats)
                .as("/stats completed.value must match dashboard.stats.completed.value")
                .isEqualTo(completedDashboard);
    }

    @Test(priority = 12, description = "RB-A12: another vendor's booking id is not reachable")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Take a booking id owned by 9000000001 and call detail / accept / decline / assign "
            + "with a foreign vendor token (9000000017). Expect 403 or 404 on every call — never "
            + "200. Any success is a cross-tenant authorisation defect.")
    public void otherVendorBookingIsForbidden() {
        String ownerToken = vendorToken(Config.get("user.rental.company.phone"));
        Response list = bookings.list(ownerToken);
        assertThat(list.statusCode()).isEqualTo(200);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");
        assertThat(rows).as("owner must have at least one booking id to probe").isNotEmpty();
        String foreignId = String.valueOf(rows.get(0).get("id"));
        Allure.parameter("ownerPhone", Config.get("user.rental.company.phone"));
        Allure.parameter("foreignBookingId", foreignId);
        Allure.parameter("foreignBookingNumber", String.valueOf(rows.get(0).get("booking_number")));

        String foreignToken = vendorToken(Config.get("user.material.phone"));
        Allure.parameter("attackerPhone", Config.get("user.material.phone"));

        Response detail = bookings.detail(foreignToken, foreignId);
        record("foreign detail", detail);
        Response accept = bookings.accept(foreignToken, foreignId);
        record("foreign accept", accept);
        Response decline = bookings.decline(foreignToken, foreignId, "Machine not available");
        record("foreign decline", decline);
        Response assign = bookings.assign(
                foreignToken,
                foreignId,
                "00000000-0000-0000-0000-000000000001",
                "00000000-0000-0000-0000-000000000002",
                true);
        record("foreign assign", assign);

        Allure.parameter("detailStatus", String.valueOf(detail.statusCode()));
        Allure.parameter("acceptStatus", String.valueOf(accept.statusCode()));
        Allure.parameter("declineStatus", String.valueOf(decline.statusCode()));
        Allure.parameter("assignStatus", String.valueOf(assign.statusCode()));

        assertThat(detail.statusCode())
                .as("Foreign detail must be 403 or 404 — never expose another vendor's booking")
                .isIn(403, 404);
        assertThat(accept.statusCode())
                .as("Foreign accept must be 403 or 404")
                .isIn(403, 404);
        assertThat(decline.statusCode())
                .as("Foreign decline must be 403 or 404")
                .isIn(403, 404);
        assertThat(assign.statusCode())
                .as("Foreign assign must be 403 or 404")
                .isIn(403, 404);
    }

    @Test(priority = 13, description = "RB-A13: invoice is available for a completed booking")
    @Severity(SeverityLevel.NORMAL)
    @Description("GET /api/v1/rentals/bookings/{id}/invoice for a completed booking returns 200 and "
            + "gross_amount matches the booking total_amount. A non-completed booking must not "
            + "500 — current QA behaviour (200 draft) is recorded.")
    public void invoiceForCompletedBooking() {
        String token = vendorToken();
        Response list = bookings.list(token);
        assertThat(list.statusCode()).isEqualTo(200);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");

        Map<String, Object> completed = null;
        Map<String, Object> incomplete = null;
        for (Map<String, Object> row : rows) {
            String status = String.valueOf(row.get("status"));
            if (completed == null && "completed".equals(status)) {
                completed = row;
            }
            if (incomplete == null && !"completed".equals(status)) {
                incomplete = row;
            }
        }
        assertThat(completed).as("Need at least one completed booking for invoice").isNotNull();

        String completedId = String.valueOf(completed.get("id"));
        Number listAmount = (Number) completed.get("total_amount");
        Response invoice = bookings.invoice(token, completedId);
        record("completed invoice", invoice);
        Allure.parameter("completedBooking", String.valueOf(completed.get("booking_number")));
        Allure.parameter("listTotalAmount", String.valueOf(listAmount));
        Allure.parameter("invoiceStatus", String.valueOf(invoice.statusCode()));

        assertThat(invoice.statusCode()).isEqualTo(200);
        assertThat(invoice.jsonPath().getBoolean("success")).isTrue();
        Number gross = (Number) invoice.jsonPath().get("data.gross_amount");
        Allure.parameter("invoiceGrossAmount", String.valueOf(gross));
        assertThat(gross).as("invoice gross_amount").isNotNull();
        assertThat(gross.doubleValue())
                .as("Invoice gross_amount must match the completed booking total_amount")
                .isEqualTo(listAmount.doubleValue());

        if (incomplete != null) {
            String incompleteId = String.valueOf(incomplete.get("id"));
            Response draft = bookings.invoice(token, incompleteId);
            record("non-completed invoice", draft);
            Allure.parameter("incompleteBooking", String.valueOf(incomplete.get("booking_number")));
            Allure.parameter("incompleteStatus", String.valueOf(incomplete.get("status")));
            Allure.parameter("incompleteInvoiceHttp", String.valueOf(draft.statusCode()));
            assertThat(draft.statusCode())
                    .as("Non-completed invoice must not 500 — refuse or return a draft")
                    .isNotEqualTo(500);
        }
    }

    @Test(priority = 14, description = "RB-A14: accepted booking propagates to schedule and earnings")
    @Severity(SeverityLevel.CRITICAL)
    @Description("For an accepted (operator_assigned) booking, GET schedule?year&month marks the "
            + "scheduled day as upcoming, and wallet earning-summary + dashboard earnings return "
            + "usable projected figures (read-only cross-module consistency).")
    public void acceptedBookingPropagates() {
        String token = vendorToken();
        Response list = bookings.list(token);
        assertThat(list.statusCode()).isEqualTo(200);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");

        Map<String, Object> accepted = null;
        for (Map<String, Object> row : rows) {
            if ("operator_assigned".equals(String.valueOf(row.get("status")))
                    && "direct".equals(String.valueOf(row.get("booking_type")))) {
                accepted = row;
                break;
            }
        }
        assertThat(accepted)
                .as("Need an operator_assigned direct booking to check schedule propagation")
                .isNotNull();

        Instant start = parseInstant(accepted.get("scheduled_start"));
        assertThat(start).as("scheduled_start").isNotNull();
        int year = start.atZone(java.time.ZoneOffset.UTC).getYear();
        int month = start.atZone(java.time.ZoneOffset.UTC).getMonthValue();
        int day = start.atZone(java.time.ZoneOffset.UTC).getDayOfMonth();
        String dayKey = String.valueOf(day);

        Allure.parameter("bookingNumber", String.valueOf(accepted.get("booking_number")));
        Allure.parameter("scheduledStart", start.toString());
        Allure.parameter("scheduleYearMonthDay", year + "-" + month + "-" + day);

        Response schedule = calendarApi.schedule(token, year, month);
        record("vendor schedule", schedule);
        assertThat(schedule.statusCode()).isEqualTo(200);
        assertThat(schedule.jsonPath().getBoolean("success")).isTrue();
        List<String> markers = schedule.jsonPath().getList("day_markers.'" + dayKey + "'");
        if (markers == null) {
            markers = schedule.jsonPath().getList("day_markers." + dayKey);
        }
        Allure.parameter("dayMarkers", String.valueOf(markers));
        assertThat(markers)
                .as("Schedule day_markers for the booking's scheduled day must include an "
                        + "upcoming (or in_progress) marker")
                .isNotNull()
                .isNotEmpty();
        assertThat(markers)
                .as("Day marker must signal an upcoming/active slot")
                .anyMatch(m -> "upcoming".equalsIgnoreCase(m)
                        || "in_progress".equalsIgnoreCase(m)
                        || "active".equalsIgnoreCase(m));

        Response summary = earningApi.earningSummary(token);
        record("earning summary", summary);
        assertThat(summary.statusCode()).isEqualTo(200);
        Number salesTotal = (Number) summary.jsonPath().get("sales.total");
        Number available = (Number) summary.jsonPath().get("available_balance");
        Allure.parameter("earningSalesTotal", String.valueOf(salesTotal));
        Allure.parameter("earningAvailableBalance", String.valueOf(available));
        assertThat(salesTotal).as("earning-summary sales.total").isNotNull();
        assertThat(available).as("earning-summary available_balance").isNotNull();

        Response dash = homeApi.rentalDashboard(token);
        record("rental dashboard earnings", dash);
        assertThat(dash.statusCode()).isEqualTo(200);
        Number projected = (Number) dash.jsonPath().get("data.stats.earnings.value");
        Allure.parameter("dashboardEarningsProjected", String.valueOf(projected));
        assertThat(projected)
                .as("dashboard.stats.earnings.value (Home Earning Projected) must be present")
                .isNotNull();
        assertThat(projected.doubleValue()).isGreaterThanOrEqualTo(0);
    }

    /**
     * RB-A10 — accept after the QB Timer window. Seed: any pending booking whose
     * {@code created_at} is older than 30 minutes (QB Timer shows ~30:00). Expected: Accept
     * refused (4xx) and/or status already {@code expired}. A 200 confirm is BUGS_FOUND #28.
     */
    @Test(priority = 10, description = "RB-A10: accept after Timer window must be refused")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Find a pending vendor booking older than 30 minutes (QB Timer window). POST "
            + "accept must not return 200 confirmed — expect 4xx / expired. A successful accept "
            + "after the window is BUGS_FOUND #28.")
    public void acceptExpiredBookingRefused() {
        String token = vendorToken();
        Response list = bookings.list(token);
        assertThat(list.statusCode()).isEqualTo(200);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");

        Instant cutoff = Instant.now().minusSeconds(30 * 60L);
        Map<String, Object> aged = null;
        for (Map<String, Object> row : rows) {
            if (!"pending".equals(String.valueOf(row.get("status")))) {
                continue;
            }
            Instant created = parseInstant(row.get("created_at"));
            if (created != null && created.isBefore(cutoff)) {
                aged = row;
                break;
            }
        }
        if (aged == null) {
            throw new org.testng.SkipException(
                    "No pending booking older than 30 minutes — seed E0B0FF and wait for Timer");
        }

        String id = String.valueOf(aged.get("id"));
        String number = String.valueOf(aged.get("booking_number"));
        Instant created = parseInstant(aged.get("created_at"));
        long ageMin = (Instant.now().getEpochSecond() - created.getEpochSecond()) / 60L;
        Allure.parameter("bookingId", id);
        Allure.parameter("bookingNumber", number);
        Allure.parameter("createdAt", String.valueOf(created));
        Allure.parameter("ageMinutes", String.valueOf(ageMin));

        Response before = bookings.detail(token, id);
        record("before accept", before);
        String statusBefore = before.jsonPath().getString("data.status");
        Allure.parameter("statusBefore", statusBefore);

        if ("expired".equals(statusBefore)) {
            Response acceptExpired = bookings.accept(token, id);
            record("accept on expired", acceptExpired);
            assertThat(acceptExpired.statusCode())
                    .as("Accept on status=expired must be refused (BUGS_FOUND #28 if 200)")
                    .isIn(400, 403, 404, 409, 422);
            return;
        }

        assertThat(statusBefore).as("seed must still be pending to probe Timer gate").isEqualTo("pending");
        Response accept = bookings.accept(token, id);
        record("accept after timer window", accept);
        Allure.parameter("acceptStatus", String.valueOf(accept.statusCode()));
        String statusAfter = accept.jsonPath().getString("data.status");
        Allure.parameter("statusAfter", String.valueOf(statusAfter));

        assertThat(accept.statusCode())
                .as("BUGS_FOUND #28: Accept after QB Timer (~30m) must be refused — not 200 confirmed")
                .isIn(400, 403, 404, 409, 422);
        assertThat(statusAfter)
                .as("Must not confirm a Timer-elapsed pending booking")
                .isNotEqualTo("confirmed");
    }

    private static Instant parseInstant(Object raw) {
        if (raw == null) {
            return null;
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        return Instant.parse(text);
    }

    private static int intFrom(Object raw) {
        if (raw == null) {
            return -1;
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return -1;
        }
        return (int) Double.parseDouble(text);
    }

    private static String tabFor(String status) {
        if (UPCOMING.contains(status)) {
            return "Upcoming";
        }
        if (ACTIVE.contains(status)) {
            return "Active";
        }
        if (COMPLETED.contains(status)) {
            return "Completed";
        }
        if (HIDDEN.contains(status)) {
            return "Hidden";
        }
        return "UNMAPPED";
    }

    private String vendorToken() {
        return vendorToken(Config.get("user.rental.company.phone"));
    }

    private String vendorToken(String phone) {
        auth.sendOtp(phone);
        Response verify = auth.verifyOtp(phone, otp());
        assertThat(verify.statusCode()).as("login " + phone).isEqualTo(200);
        String token = verify.jsonPath().getString("access_token");
        assertThat(token).isNotBlank();
        return token;
    }

    private static String otp() {
        String fromProp = System.getProperty("qa.otp", System.getenv("L2B_QA_OTP"));
        return fromProp == null || fromProp.isBlank() ? "1234" : fromProp;
    }

    private static void record(String label, Response response) {
        Allure.parameter(label + ".status", String.valueOf(response.statusCode()));
        Allure.addAttachment(label + " body", "application/json", response.asString(), ".json");
    }
}
