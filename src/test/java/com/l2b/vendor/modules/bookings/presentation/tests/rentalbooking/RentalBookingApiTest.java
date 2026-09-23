package com.l2b.vendor.modules.bookings.presentation.tests.rentalbooking;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.data.api.BookingsApi;
import com.l2b.vendor.modules.bookings.presentation.pages.RentalBookingsPage;
import com.l2b.vendor.modules.onboarding.data.api.AuthApi;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * RB-A: API and backend consistency for Rental Booking. Pure-API gaps live in
 * {@code RentalBookingApiGapTest}; this class holds Appium+API cases.
 *
 * <p>Isolated {@code bookings/booking-rental-api-a*.xml}.
 */
@Epic("Vendor app")
@Feature("Rental Booking API — rental vendor 9000000001")
public class RentalBookingApiTest extends RentalBookingBaseTest {

    private static final Pattern RUPEE_DIGITS = Pattern.compile("([0-9][0-9,]*)");

    private final AuthApi auth = new AuthApi();
    private final BookingsApi bookingsApi = new BookingsApi();

    @Test(enabled = false, priority = 1,
            description = "RB-A1: list shape — covered by lifecycle")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Covered by RentalBookingLifecycleApiTest.listReturnsUsableShape (RB-API-1).")
    public void listReturnsUsableShape() {
        throw new SkipException("RB-A1 covered by RentalBookingLifecycleApiTest");
    }

    @Test(enabled = false, priority = 2,
            description = "RB-A2 — LIVE in ApiGapTest")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Implemented in RentalBookingApiGapTest.statusToTabMapping.")
    public void statusToTabMapping() {
        throw new SkipException("RB-A2 lives in RentalBookingApiGapTest");
    }

    @Test(enabled = false, priority = 3,
            description = "RB-A3 — LIVE in ApiGapTest")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Implemented in RentalBookingApiGapTest.countsAgreeAcrossSurfaces.")
    public void countsAgreeAcrossSurfaces() {
        throw new SkipException("RB-A3 lives in RentalBookingApiGapTest");
    }

    @Test(priority = 4, description = "RB-A4: booking detail matches the card")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Open Upcoming, pick a card with a fleet plate, GET detail for that booking id "
            + "(matched via machine_registration_number). Machine, plate, operator, amount "
            + "(numeric), and address must agree with the rendered card. Rupee grouping may "
            + "differ (#27) — compare digits only.")
    public void detailMatchesCard() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        // Cards can paint a beat after the tabs/header — wait for at least one amount row.
        com.l2b.vendor.core.wait.Waits.until(
                com.l2b.vendor.core.driver.DriverManager.get(),
                d -> bookings.cardCountNow() >= 1 ? Boolean.TRUE : null,
                "Upcoming cards (amount rows) did not appear after See all",
                java.time.Duration.ofSeconds(12));
        assertThat(bookings.cardCountNow()).as("Need at least one Upcoming card").isGreaterThanOrEqualTo(1);

        String plate = firstPlate(bookings);
        if (plate.isEmpty()) {
            bookings.distinctCardCountByScrolling(6);
            plate = firstPlate(bookings);
        }
        assertThat(plate)
                .as("Need a card with a fleet plate to correlate UI ↔ API (assigned machine)")
                .isNotBlank();

        List<String> amounts = bookings.amountsNow();
        List<String> machines = bookings.machineTitlesNow();
        List<String> addresses = bookings.bookingForAddressesNow();
        List<String> operatorRows = bookings.assignedOperatorRowsNow();
        List<String> dateRanges = bookings.dateRangesNow();
        bookings.attachScreenshot("rb-a4-ui-card");

        Allure.parameter("uiPlate", plate);
        Allure.parameter("uiAmounts", amounts.toString());
        Allure.parameter("uiMachines", machines.toString());
        Allure.parameter("uiAddresses", addresses.toString());
        Allure.parameter("uiOperators", operatorRows.toString());
        Allure.parameter("uiDateRanges", dateRanges.toString());

        String token = vendorToken();
        Response list = bookingsApi.list(token);
        assertThat(list.statusCode()).isEqualTo(200);
        List<Map<String, Object>> rows = list.jsonPath().getList("data");
        java.util.Set<Long> uiAmountDigits = new java.util.LinkedHashSet<>();
        for (String amt : amounts) {
            long d = digitsFromRupee(amt);
            if (d > 0) {
                uiAmountDigits.add(d);
            }
        }

        // Same plate can appear on multiple bookings — correlate with a UI amount on screen.
        Map<String, Object> match = null;
        for (Map<String, Object> row : rows) {
            if (!plate.equals(String.valueOf(row.get("machine_registration_number")))) {
                continue;
            }
            Object rawAmt = row.get("total_amount");
            if (!(rawAmt instanceof Number)) {
                continue;
            }
            long rupees = Math.round(((Number) rawAmt).doubleValue());
            if (uiAmountDigits.contains(rupees)) {
                match = row;
                break;
            }
        }
        if (match == null) {
            for (Map<String, Object> row : rows) {
                if (plate.equals(String.valueOf(row.get("machine_registration_number")))) {
                    match = row;
                    Allure.parameter("matchFallback", "plate-only (no amount overlap with viewport)");
                    break;
                }
            }
        }
        assertThat(match)
                .as("List must contain a booking with plate " + plate
                        + " whose amount appears on the Upcoming viewport")
                .isNotNull();

        String bookingId = String.valueOf(match.get("id"));
        Allure.parameter("bookingId", bookingId);
        Allure.parameter("bookingNumber", String.valueOf(match.get("booking_number")));

        Response detail = bookingsApi.detail(token, bookingId);
        Allure.parameter("detail.status", String.valueOf(detail.statusCode()));
        Allure.addAttachment("detail body", "application/json", detail.asString(), ".json");
        assertThat(detail.statusCode()).isEqualTo(200);

        String apiMachine = detail.jsonPath().getString("data.sku_name");
        String apiPlate = detail.jsonPath().getString("data.machine_registration_number");
        String apiOperator = trimOrEmpty(detail.jsonPath().getString("data.assigned_operator_name"));
        String apiAddress = firstNonBlank(
                detail.jsonPath().getString("data.site_address"),
                detail.jsonPath().getString("data.delivery_address"),
                detail.jsonPath().getString("data.pickup_address"));
        Number apiAmount = detail.jsonPath().get("data.total_amount");
        String apiStart = detail.jsonPath().getString("data.scheduled_start");

        Allure.parameter("apiMachine", apiMachine);
        Allure.parameter("apiPlate", apiPlate);
        Allure.parameter("apiOperator", apiOperator);
        Allure.parameter("apiAddress", apiAddress);
        Allure.parameter("apiAmount", String.valueOf(apiAmount));
        Allure.parameter("apiStart", apiStart);

        assertThat(apiPlate).as("detail plate").isEqualTo(plate);
        assertThat(machines)
                .as("UI must show the API machine title")
                .anyMatch(m -> m.equals(apiMachine) || m.contains(apiMachine) || apiMachine.contains(m));

        if (!apiOperator.isEmpty()) {
            assertThat(operatorRows)
                    .as("UI must show Operator : <name> matching detail")
                    .anyMatch(row -> row.contains(apiOperator));
        }

        long apiRupees = Math.round(apiAmount.doubleValue());
        boolean amountMatched = uiAmountDigits.contains(apiRupees);
        Allure.parameter("amountMatched", String.valueOf(amountMatched));
        assertThat(amountMatched)
                .as("UI amount digits must equal API total_amount (" + apiRupees
                        + ") — grouping may differ (#27)")
                .isTrue();

        if (!apiAddress.isEmpty()
                && !"Address not provided".equalsIgnoreCase(apiAddress)
                && !"null".equalsIgnoreCase(apiAddress)) {
            String addrNorm = apiAddress.toLowerCase().replaceAll("\\s+", " ").trim();
            assertThat(addresses)
                    .as("UI Booking-for address must reflect API address")
                    .anyMatch(a -> {
                        String ui = a.toLowerCase().replaceAll("\\s+", " ").trim();
                        return ui.contains(addrNorm) || addrNorm.contains(ui)
                                || overlapToken(ui, addrNorm);
                    });
        } else {
            Allure.parameter("addressNote", "API address blank/null — UI address recorded only");
        }

        String year = apiStart != null && apiStart.length() >= 4 ? apiStart.substring(0, 4) : "";
        if (!year.isEmpty()) {
            assertThat(dateRanges)
                    .as("UI date range must include the scheduled year from detail")
                    .anyMatch(dr -> dr.contains(year));
        }
    }

    private static String firstPlate(RentalBookingsPage bookings) {
        List<String> plates = bookings.platesNow();
        return plates.isEmpty() ? "" : plates.get(0);
    }

    private static long digitsFromRupee(String raw) {
        Matcher m = RUPEE_DIGITS.matcher(raw);
        if (!m.find()) {
            return -1;
        }
        return Long.parseLong(m.group(1).replace(",", ""));
    }

    private static boolean overlapToken(String a, String b) {
        for (String tok : a.split("[\\s,]+")) {
            if (tok.length() >= 4 && b.contains(tok)) {
                return true;
            }
        }
        return false;
    }

    private static String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String vendorToken() {
        String phone = Config.get("user.rental.company.phone");
        auth.sendOtp(phone);
        Response verify = auth.verifyOtp(phone, "1234");
        assertThat(verify.statusCode()).isEqualTo(200);
        return verify.jsonPath().getString("access_token");
    }

    @Test(enabled = false, priority = 5,
            description = "RB-A5 — covered by lifecycle")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Covered by RentalBookingLifecycleApiTest.")
    public void acceptTransitionsStatus() {
        throw new SkipException("RB-A5 covered by RentalBookingLifecycleApiTest");
    }

    @Test(enabled = false, priority = 6,
            description = "RB-A6 — covered by lifecycle")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Covered by RentalBookingLifecycleApiTest.")
    public void secondAcceptIsSafe() {
        throw new SkipException("RB-A6 covered by RentalBookingLifecycleApiTest");
    }

    @Test(enabled = false, priority = 7,
            description = "RB-A7 — partial in lifecycle")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Lifecycle covers removal; reason-store strengthen deferred.")
    public void declinePersistsReason() {
        throw new SkipException("RB-A7 partial in lifecycle");
    }

    @Test(enabled = false, priority = 8,
            description = "RB-A8 — covered by lifecycle")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Covered by RentalBookingLifecycleApiTest.")
    public void assignPersistsMachineAndOperator() {
        throw new SkipException("RB-A8 covered by RentalBookingLifecycleApiTest");
    }

    @Test(enabled = false, priority = 9,
            description = "RB-A9 — covered by lifecycle")
    @Severity(SeverityLevel.NORMAL)
    @Description("Covered by RentalBookingLifecycleApiTest.")
    public void acceptWithoutAssign() {
        throw new SkipException("RB-A9 covered by RentalBookingLifecycleApiTest");
    }

    @Test(enabled = false, priority = 10,
            description = "RB-A10 — LIVE in ApiGapTest")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Implemented in RentalBookingApiGapTest.acceptExpiredBookingRefused (#28).")
    public void acceptExpiredBookingRefused() {
        throw new SkipException("RB-A10 lives in RentalBookingApiGapTest");
    }

    @Test(enabled = false, priority = 11,
            description = "RB-A11 — missing token covered by lifecycle")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Missing-token covered by lifecycle; expired-token deferred.")
    public void endpointsRejectBadToken() {
        throw new SkipException("RB-A11 missing-token covered by lifecycle");
    }

    @Test(enabled = false, priority = 12,
            description = "RB-A12 — LIVE in ApiGapTest")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Implemented in RentalBookingApiGapTest.")
    public void otherVendorBookingIsForbidden() {
        throw new SkipException("RB-A12 lives in RentalBookingApiGapTest");
    }

    @Test(enabled = false, priority = 13,
            description = "RB-A13 — LIVE in ApiGapTest")
    @Severity(SeverityLevel.NORMAL)
    @Description("Implemented in RentalBookingApiGapTest.")
    public void invoiceForCompletedBooking() {
        throw new SkipException("RB-A13 lives in RentalBookingApiGapTest");
    }

    @Test(enabled = false, priority = 14,
            description = "RB-A14 — LIVE in ApiGapTest")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Implemented in RentalBookingApiGapTest.")
    public void acceptedBookingPropagates() {
        throw new SkipException("RB-A14 lives in RentalBookingApiGapTest");
    }
}
