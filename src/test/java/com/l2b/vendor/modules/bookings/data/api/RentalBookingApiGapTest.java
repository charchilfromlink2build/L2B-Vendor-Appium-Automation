package com.l2b.vendor.modules.bookings.data.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.onboarding.data.api.AuthApi;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
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

        // Sanity: the three tab buckets are the only expected homes for live rows.
        for (String status : distinctStatuses) {
            String tab = tabFor(status);
            assertThat(tab)
                    .as("status " + status)
                    .isIn("Upcoming", "Active", "Completed", "Hidden");
        }
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
        String phone = Config.get("user.rental.company.phone");
        auth.sendOtp(phone);
        Response verify = auth.verifyOtp(phone, otp());
        assertThat(verify.statusCode()).as("vendor login").isEqualTo(200);
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
