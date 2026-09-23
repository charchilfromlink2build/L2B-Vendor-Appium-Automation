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
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Executable API lifecycle + edges for rental accept → assign → Start OTP → End OTP.
 *
 * <p>No Appium. Isolated {@code bookings/rental-booking-lifecycle-api.xml}. Proven live on 23 Sep
 * 2026 against {@code L2B-RNT-2026-1837E2} (customer web seed → vendor 9000000001 → operator
 * 9000000002). UI Assign Confirm remains blocked by BUGS_FOUND #16 when all operators are busy;
 * API assign with {@code allow_operator_overlap=true} completes the same path.
 */
@Epic("Vendor app")
@Feature("Rental Booking lifecycle API — 9000000001 / 9000000002")
public class RentalBookingLifecycleApiTest {

    private static final double SITE_LAT = 13.0358;
    private static final double SITE_LNG = 77.5970;

    private final AuthApi auth = new AuthApi();
    private final BookingsApi bookings = new BookingsApi();
    private final OperatorBookingsApi operatorBookings = new OperatorBookingsApi();

    @Test(priority = 1, description = "RB-API-1: vendor bookings list is 200 with usable rows")
    @Severity(SeverityLevel.CRITICAL)
    @Description("GET /api/v1/rentals/vendor/bookings. 200, success, each row has id + status.")
    public void listReturnsUsableShape() {
        String token = vendorToken();
        Response response = bookings.list(token);
        record("vendor bookings list", response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        List<Map<String, Object>> rows = response.jsonPath().getList("data");
        assertThat(rows).as("vendor must see at least one booking row").isNotEmpty();
        for (Map<String, Object> row : rows) {
            assertThat(row.get("id")).as("id").isNotNull();
            assertThat(row.get("status")).as("status").isNotNull();
        }
    }

    @Test(priority = 2, description = "RB-API-2: bookings endpoints reject missing token")
    @Severity(SeverityLevel.CRITICAL)
    @Description("list / detail / accept without Authorization must be 401, never 200 with data.")
    public void endpointsRejectMissingToken() {
        Response list = bookings.listUnauthenticated();
        record("list no token", list);
        assertThat(list.statusCode()).as("list").isEqualTo(401);

        Response detail = bookings.detailUnauthenticated("00000000-0000-0000-0000-000000000001");
        record("detail no token", detail);
        assertThat(detail.statusCode()).as("detail").isEqualTo(401);

        Response accept = bookings.acceptUnauthenticated("00000000-0000-0000-0000-000000000001");
        record("accept no token", accept);
        assertThat(accept.statusCode()).as("accept").isEqualTo(401);
    }

    @Test(priority = 3, description = "RB-API-3: accept without assign → confirmed; second accept safe")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Accept a pending vendor booking. Status becomes confirmed with no operator. "
            + "A second accept must not 500 and must not invent a second booking.")
    public void acceptWithoutAssignThenSecondAcceptSafe() {
        String token = vendorToken();
        String bookingId = firstPendingId(token);
        if (bookingId == null) {
            throw new SkipException("No pending booking on 9000000001 — seed from customer web first");
        }

        Response before = bookings.detail(token, bookingId);
        record("before accept", before);
        assertThat(before.jsonPath().getString("data.status")).isEqualTo("pending");

        Response accept = bookings.accept(token, bookingId);
        record("accept", accept);
        assertThat(accept.statusCode()).isEqualTo(200);
        assertThat(accept.jsonPath().getString("data.status")).isEqualTo("confirmed");
        assertThat(accept.jsonPath().getString("data.assigned_operator_id")).isNull();

        Response second = bookings.accept(token, bookingId);
        record("second accept", second);
        assertThat(second.statusCode())
                .as("second accept must not 500")
                .isNotEqualTo(500);
        assertThat(second.statusCode())
                .as("second accept is refused or idempotent confirmed")
                .isIn(200, 400, 409);
        if (second.statusCode() == 200) {
            assertThat(second.jsonPath().getString("data.status")).isEqualTo("confirmed");
        }
    }

    @Test(priority = 4, description = "RB-API-4: busy operator needs overlap flag; assign persists")
    @Severity(SeverityLevel.CRITICAL)
    @Description("When assignment-candidates reports all_operators_busy, assign without overlap "
            + "fails; with allow_operator_overlap=true status becomes operator_assigned.")
    public void assignRequiresOverlapWhenBusy() {
        String token = vendorToken();
        // Prefer Excavator 20 — Nauman is eligible there. Tata Ace / other SKUs return
        // "operator is not eligible" even with allow_operator_overlap.
        String bookingId = firstConfirmedWithoutOperatorForSku(token, "excavator-20");
        if (bookingId == null) {
            bookingId = firstPendingIdForSku(token, "excavator-20");
            if (bookingId == null) {
                throw new SkipException("Need a pending/confirmed Excavator 20 booking to assign");
            }
            Response accept = bookings.accept(token, bookingId);
            record("accept for assign", accept);
            assertThat(accept.statusCode()).isEqualTo(200);
        }

        Response candidates = bookings.assignmentCandidates(token, bookingId);
        record("candidates", candidates);
        assertThat(candidates.statusCode()).isEqualTo(200);

        String machineId = firstSelectableMachineId(candidates);
        String operatorId = candidates.jsonPath().getString("data.operators[0].id");
        Boolean allBusy = candidates.jsonPath().getBoolean("data.all_operators_busy");
        Allure.parameter("machineId", machineId);
        Allure.parameter("operatorId", operatorId);
        Allure.parameter("allOperatorsBusy", String.valueOf(allBusy));
        assertThat(machineId).as("selectable machine").isNotBlank();
        assertThat(operatorId).as("candidate operator").isNotBlank();

        if (Boolean.TRUE.equals(allBusy)) {
            Response blocked = bookings.assign(token, bookingId, machineId, operatorId, false);
            record("assign without overlap", blocked);
            assertThat(blocked.statusCode())
                    .as("busy operator without overlap must not silently succeed")
                    .isNotEqualTo(200);
        }

        Response assigned =
                bookings.assign(token, bookingId, machineId, operatorId, true);
        record("assign with overlap", assigned);
        assertThat(assigned.statusCode()).isEqualTo(200);
        assertThat(assigned.jsonPath().getString("data.status")).isEqualTo("operator_assigned");
        assertThat(assigned.jsonPath().getString("data.assigned_operator_id")).isEqualTo(operatorId);
        assertThat(assigned.jsonPath().getString("data.equipment_id")).isEqualTo(machineId);
    }

    @Test(priority = 5, description = "RB-API-5: wrong Start OTP is refused")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Operator POST start with otp=0000 on an operator_assigned booking must be 4xx.")
    public void wrongStartOtpRefused() {
        String vendor = vendorToken();
        String bookingId = firstStatusId(vendor, "operator_assigned");
        if (bookingId == null) {
            throw new SkipException("No operator_assigned booking for wrong-OTP probe");
        }
        String opToken = operatorToken();
        Response wrong = operatorBookings.start(opToken, bookingId, "0000", SITE_LAT, SITE_LNG);
        record("start wrong otp", wrong);
        assertThat(wrong.statusCode()).isBetween(400, 499);
        assertThat(wrong.statusCode()).isNotEqualTo(500);

        Response still = bookings.detail(vendor, bookingId);
        record("status after wrong otp", still);
        assertThat(still.jsonPath().getString("data.status")).isEqualTo("operator_assigned");
    }

    @Test(priority = 6, description = "RB-API-6: Start OTP then End OTP completes the job")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Requires -Dl2b.startOtp and -Dl2b.endOtp (customer console values) plus "
            + "-Dl2b.bookingId of an operator_assigned booking owned by 9000000002. "
            + "Proven manually on L2B-RNT-2026-1837E2 (9477 → 5400 → completed).")
    public void startThenEndCompletes() {
        String bookingId = System.getProperty("l2b.bookingId", System.getenv("L2B_BOOKING_ID"));
        String startOtp = System.getProperty("l2b.startOtp", System.getenv("L2B_START_OTP"));
        String endOtp = System.getProperty("l2b.endOtp", System.getenv("L2B_END_OTP"));
        if (isBlank(bookingId) || isBlank(startOtp) || isBlank(endOtp)) {
            throw new SkipException(
                    "Pass -Dl2b.bookingId -Dl2b.startOtp -Dl2b.endOtp from customer console. "
                            + "Live proof already on L2B-RNT-2026-1837E2.");
        }

        String vendor = vendorToken();
        Response before = bookings.detail(vendor, bookingId);
        record("before start/end", before);
        String status = before.jsonPath().getString("data.status");
        if ("completed".equals(status)) {
            throw new SkipException("Booking " + bookingId + " already completed — seed a fresh operator_assigned id");
        }
        assertThat(status).isEqualTo("operator_assigned");

        String opToken = operatorToken();

        Response start = operatorBookings.start(opToken, bookingId, startOtp, SITE_LAT, SITE_LNG);
        record("start otp", start);
        assertThat(start.statusCode()).isEqualTo(200);
        assertThat(start.jsonPath().getString("data.status")).isEqualTo("in_progress");

        Response mid = bookings.detail(vendor, bookingId);
        record("in_progress detail", mid);
        assertThat(mid.jsonPath().getString("data.status")).isEqualTo("in_progress");
        assertThat(mid.jsonPath().getBoolean("data.available_actions.can_verify_end_otp")).isTrue();

        Response end = operatorBookings.end(opToken, bookingId, endOtp, SITE_LAT, SITE_LNG);
        record("end otp", end);
        assertThat(end.statusCode()).isEqualTo(200);
        assertThat(end.jsonPath().getString("data.status")).isEqualTo("completed");

        Response done = bookings.detail(vendor, bookingId);
        record("completed detail", done);
        assertThat(done.jsonPath().getString("data.status")).isEqualTo("completed");
        assertThat(done.jsonPath().getString("data.actual_start")).isNotBlank();
        assertThat(done.jsonPath().getString("data.actual_end")).isNotBlank();
    }

    @Test(priority = 7, description = "RB-API-7: decline with reason removes booking from vendor list")
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST decline with reason=Machine not available. Detail becomes unreachable "
            + "(404) for this vendor and the id leaves the vendor list.")
    public void declineRemovesFromVendorList() {
        String token = vendorToken();
        String bookingId = firstPendingId(token);
        if (bookingId == null) {
            throw new SkipException("No pending booking left to decline — seed one first");
        }
        Allure.parameter("declineBookingId", bookingId);

        Response decline = bookings.decline(token, bookingId, "Machine not available");
        record("decline", decline);
        assertThat(decline.statusCode()).isEqualTo(200);

        Response detail = bookings.detail(token, bookingId);
        record("detail after decline", detail);
        assertThat(detail.statusCode()).as("declined booking hidden from vendor").isEqualTo(404);

        Response list = bookings.list(token);
        List<String> ids = list.jsonPath().getList("data.id");
        assertThat(ids).doesNotContain(bookingId);
    }

    @Test(priority = 8, description = "RB-API-8: completed booking 1837E2 stays completed")
    @Severity(SeverityLevel.NORMAL)
    @Description("Regression lock on the live full-flow seed L2B-RNT-2026-1837E2.")
    public void completedFullFlowSeedRemainsCompleted() {
        String token = vendorToken();
        Response list = bookings.list(token);
        String bookingId = null;
        List<Map<String, Object>> rows = list.jsonPath().getList("data");
        for (Map<String, Object> row : rows) {
            Object number = row.get("booking_number");
            if (number != null && number.toString().contains("1837E2")) {
                bookingId = String.valueOf(row.get("id"));
                break;
            }
        }
        if (bookingId == null) {
            // list rows may omit booking_number — scan by known completed ids via detail filter
            for (Map<String, Object> row : rows) {
                if ("completed".equals(String.valueOf(row.get("status")))) {
                    String id = String.valueOf(row.get("id"));
                    Response d = bookings.detail(token, id);
                    if (d.statusCode() == 200
                            && "L2B-RNT-2026-1837E2"
                                    .equals(d.jsonPath().getString("data.booking_number"))) {
                        bookingId = id;
                        break;
                    }
                }
            }
        }
        if (bookingId == null) {
            throw new SkipException("1837E2 not on vendor list — already purged or different env");
        }
        Response detail = bookings.detail(token, bookingId);
        record("1837E2 detail", detail);
        assertThat(detail.jsonPath().getString("data.status")).isEqualTo("completed");
        assertThat(detail.jsonPath().getString("data.booking_number")).isEqualTo("L2B-RNT-2026-1837E2");
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

    private String operatorToken() {
        String phone = Config.get("user.operator.phone");
        auth.sendOtp(phone);
        Response verify = auth.verifyOtp(phone, otp());
        assertThat(verify.statusCode()).as("operator login").isEqualTo(200);
        String token = verify.jsonPath().getString("access_token");
        assertThat(token).isNotBlank();
        return token;
    }

    private static String otp() {
        String fromProp = System.getProperty("qa.otp", System.getenv("L2B_QA_OTP"));
        return isBlank(fromProp) ? "1234" : fromProp;
    }

    private String firstPendingId(String token) {
        return firstStatusId(token, "pending");
    }

    private String firstPendingIdForSku(String token, String skuFragment) {
        return firstStatusIdForSku(token, "pending", skuFragment);
    }

    private String firstStatusId(String token, String status) {
        return firstStatusIdForSku(token, status, null);
    }

    private String firstStatusIdForSku(String token, String status, String skuFragment) {
        Response list = bookings.list(token);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");
        if (rows == null) {
            return null;
        }
        for (Map<String, Object> row : rows) {
            if (!status.equals(String.valueOf(row.get("status")))) {
                continue;
            }
            String id = String.valueOf(row.get("id"));
            if (skuFragment == null) {
                return id;
            }
            Response detail = bookings.detail(token, id);
            if (detail.statusCode() != 200) {
                continue;
            }
            String slug = detail.jsonPath().getString("data.sku_slug");
            String name = detail.jsonPath().getString("data.sku_name");
            String blob = ((slug == null ? "" : slug) + " " + (name == null ? "" : name)).toLowerCase();
            if (blob.contains(skuFragment.toLowerCase())) {
                return id;
            }
        }
        return null;
    }

    private String firstConfirmedWithoutOperatorForSku(String token, String skuFragment) {
        Response list = bookings.list(token);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");
        if (rows == null) {
            return null;
        }
        for (Map<String, Object> row : rows) {
            if (!"confirmed".equals(String.valueOf(row.get("status")))) {
                continue;
            }
            String id = String.valueOf(row.get("id"));
            Response detail = bookings.detail(token, id);
            if (detail.statusCode() != 200) {
                continue;
            }
            JsonPath path = detail.jsonPath();
            if (path.getString("data.assigned_operator_id") != null) {
                continue;
            }
            String slug = path.getString("data.sku_slug");
            String name = path.getString("data.sku_name");
            String blob = ((slug == null ? "" : slug) + " " + (name == null ? "" : name)).toLowerCase();
            if (blob.contains(skuFragment.toLowerCase())) {
                return id;
            }
        }
        return null;
    }

    private static String firstSelectableMachineId(Response candidates) {
        List<Map<String, Object>> machines = candidates.jsonPath().getList("data.machines");
        if (machines == null) {
            return null;
        }
        for (Map<String, Object> machine : machines) {
            Object selectable = machine.get("is_selectable");
            if (selectable == null || Boolean.TRUE.equals(selectable)) {
                Object id = machine.get("id");
                if (id != null) {
                    return String.valueOf(id);
                }
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void record(String label, Response response) {
        Allure.parameter(label + ".status", String.valueOf(response.statusCode()));
        Allure.addAttachment(label + " body", "application/json", response.asString(), ".json");
    }
}
