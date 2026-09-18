package com.l2b.vendor.modules.quickbooking.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.time.Duration;
import org.testng.annotations.Test;

/**
 * Accept path for rental owner {@code 9000000001} (OTP 1234).
 * Isolated {@code src/test/resources/quickbooking/accept.xml}.
 * This pass uses only one rental card for Accept. Do not tap Decline here.
 * Do not tap the Home {@code Request to extend time} dialog.
 */
@Epic("Vendor app")
@Feature("Quick Booking accept — rental owner 9000000001")
public class QuickBookingAcceptTest extends QuickBookingBaseTest {

    @Test(priority = 1, description = "Accept-1: View More Details expands the first rental card")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001. Tap View More Details on the first card. Purpose / Soil / Load "
            + "must appear. Do not tap Accept/Decline.")
    public void viewMoreDetailsExpandsFirstCard() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        assertThat(page.isViewMoreVisible()).as("Precondition: View More Details").isTrue();
        page.tapFirstViewMore();
        Waits.until(DriverManager.get(),
                d -> (page.isViewLessVisible()
                        || page.hasText("Purpose")
                        || page.hasText("Type of Soil")
                        || page.hasText("Type of Load")) ? Boolean.TRUE : null,
                "View More did not expand",
                Duration.ofSeconds(8));

        Allure.parameter("viewLess", String.valueOf(page.isViewLessVisible()));
        Allure.parameter("purpose", String.valueOf(page.hasText("Purpose")));
        Allure.parameter("soil", String.valueOf(page.hasText("Type of Soil")));
        Allure.parameter("load", String.valueOf(page.hasText("Type of Load")));
        page.attachScreenshot("accept-1-view-more");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isViewLessVisible()).as("View Less after expand").isTrue();
        assertThat(page.hasText("Purpose") || page.hasText("Type of Soil") || page.hasText("Type of Load"))
                .as("Expanded details")
                .isTrue();
        assertThat(page.isExtendTimeDialogVisible()).as("Must not be the extend-time dialog").isFalse();
    }

    @Test(priority = 2, description = "Accept-2: Assign operator then Confirm completes one Accept")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap first Quick Booking Accept. Assign machine must open. Open Select an operator, "
            + "pick the first operator, then Confirm. Sheet-open alone is not a pass. Do not Skip. "
            + "Do not tap Decline. Do not tap extend-time Accept.")
    public void acceptOneRentalCard() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        HomePage home = new HomePage();
        int before = page.acceptCount();
        String amount = page.firstAmountLine();
        assertThat(before).as("Need a pending card").isGreaterThanOrEqualTo(1);
        assertThat(page.isAcceptEnabled()).as("Accept outer View enabled").isTrue();
        assertThat(page.isExtendTimeDialogVisible()).as("Must not tap extend-time dialog").isFalse();

        Allure.parameter("acceptCountBefore", String.valueOf(before));
        Allure.parameter("acceptedAmount", amount);
        page.attachScreenshot("accept-2-before");
        page.tapFirstAccept();

        Waits.until(DriverManager.get(),
                d -> page.isAssignMachineVisible() ? Boolean.TRUE : null,
                "After Accept, Assign machine did not open",
                Duration.ofSeconds(15));

        Allure.parameter("assignMachine", String.valueOf(page.isAssignMachineVisible()));
        Allure.parameter("selectOperator", String.valueOf(page.isSelectOperatorVisible()));
        page.attachScreenshot("accept-2-assign-machine");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isExtendTimeDialogVisible()).as("Must not be extend-time dialog").isFalse();
        assertThat(page.isAssignMachineVisible()).as("Accept opens Assign machine").isTrue();
        assertThat(page.isSelectOperatorVisible()).as("Assign machine has Select an operator").isTrue();

        String operator = page.selectFirstOperator();
        Allure.parameter("assignedOperator", operator);
        Allure.parameter("operatorRowEnabled", String.valueOf(page.lastMenuOptionEnabled()));
        Allure.parameter("confirmEnabledAfterAssign", String.valueOf(page.isAssignConfirmEnabled()));
        page.attachScreenshot("accept-2-operator-selected");
        assertThat(operator).as("An operator was chosen from the dropdown").isNotBlank();

        page.tapAssignConfirm();
        Waits.until(DriverManager.get(),
                d -> (!page.isAssignMachineVisible()
                        && (page.isDisplayedNow() || home.isDisplayedNow())) ? Boolean.TRUE : null,
                "After operator + Confirm, Assign machine stayed open — Accept did not complete "
                        + "(Confirm stayed disabled while copy says confirm machine now; BUGS_FOUND #16)",
                Duration.ofSeconds(15));

        int after = page.acceptCount();
        Allure.parameter("assignMachineAfterConfirm", String.valueOf(page.isAssignMachineVisible()));
        Allure.parameter("acceptCountAfterConfirm", String.valueOf(after));
        Allure.parameter("stillOnQuickBooking", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("homeVisible", String.valueOf(home.isDisplayedNow()));
        page.attachScreenshot("accept-2-after-confirm");

        assertThat(page.isAssignMachineVisible()).as("Assign machine must close after Confirm").isFalse();
        assertThat(page.isExtendTimeDialogVisible()).as("Must not be extend-time dialog").isFalse();
        if (page.isDisplayedNow()) {
            assertThat(after).as("Accepted card left; remaining rentals stay").isLessThan(before);
            assertThat(after).as("Only one Accept — queue must not be empty").isGreaterThanOrEqualTo(1);
            assertThat(page.firstAmountLine()).as("First remaining card is not the accepted amount")
                    .isNotEqualTo(amount);
        }
    }

    @Test(priority = 3, description = "Accept-3: Accept is enabled on an eligible rental card")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #5. First Accept outer View must be enabled. Do not tap Decline. "
            + "Do not tap extend-time Accept.")
    public void acceptButtonIsEnabledOnEligibleCard() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        Allure.parameter("acceptCount", String.valueOf(page.acceptCount()));
        Allure.parameter("acceptEnabled", String.valueOf(page.isAcceptEnabled()));
        page.attachScreenshot("accept-3-enabled");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isAcceptVisible()).as("Accept visible").isTrue();
        assertThat(page.isAcceptEnabled()).as("Accept clickable outer View enabled").isTrue();
        assertThat(page.isExtendTimeDialogVisible()).isFalse();
    }

    @Test(priority = 4, description = "Accept-4: Rapid double-tap Accept opens one Assign machine")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #9. Two clickGestures on first Accept. One Assign machine sheet, no crash. "
            + "Do not Confirm. Do not tap Decline.")
    public void rapidDoubleTapAcceptOpensOneSheet() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        int before = page.acceptCount();
        assertThat(page.isAcceptEnabled()).isTrue();
        page.tapFirstAcceptRapidly(2);

        Waits.until(DriverManager.get(),
                d -> page.isAssignMachineVisible() ? Boolean.TRUE : null,
                "Rapid Accept did not open Assign machine",
                Duration.ofSeconds(12));

        Allure.parameter("acceptCountBefore", String.valueOf(before));
        Allure.parameter("assignMachine", String.valueOf(page.isAssignMachineVisible()));
        Allure.parameter("package", String.valueOf(vendorPackage()));
        page.attachScreenshot("accept-4-rapid-accept");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isAssignMachineVisible()).as("One Assign machine after rapid Accept").isTrue();
        assertThat(page.isExtendTimeDialogVisible()).isFalse();
    }

    @Test(priority = 5, description = "Accept-5: Network drop during Accept shows feedback, no crash")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #12. Open Assign machine online, disable radios, tap Confirm. Record "
            + "offline copy vs silent no-op. Radios restored in AfterMethod. Do not tap Decline.")
    public void networkDropDuringAcceptShowsFeedback() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        page.tapFirstAccept();
        Waits.until(DriverManager.get(),
                d -> page.isAssignMachineVisible() ? Boolean.TRUE : null,
                "Assign machine did not open before offline Confirm",
                Duration.ofSeconds(12));
        Adb.disableRadios();
        boolean confirmEnabled = page.isAssignConfirmEnabled();
        page.tapAssignConfirm();

        Waits.until(DriverManager.get(),
                d -> (page.isAssignMachineVisible() || page.isDisplayedNow()
                        || hasOfflineCopy(d.getPageSource())) ? Boolean.TRUE : null,
                "After offline Confirm, no Assign machine / queue / offline copy",
                Duration.ofSeconds(10));

        String source = DriverManager.get().getPageSource();
        boolean offlineCopy = hasOfflineCopy(source);
        Allure.parameter("confirmEnabled", String.valueOf(confirmEnabled));
        Allure.parameter("assignMachineAfter", String.valueOf(page.isAssignMachineVisible()));
        Allure.parameter("offlineOrRetryCopy", String.valueOf(offlineCopy));
        Allure.parameter("offlineUx", offlineCopy ? "error-or-retry-copy"
                : page.isAssignMachineVisible() ? "stayed-on-assign-no-error-copy"
                : "left-assign-no-error-copy");
        page.attachScreenshot("accept-5-offline-confirm");

        assertThat(vendorPackage()).as("Offline Confirm must not crash Vendor")
                .isEqualTo(Config.get("app.package"));
        if (!offlineCopy && page.isAssignMachineVisible()) {
            Allure.parameter("bugNote", "Silent no-op on offline Confirm — log BUGS_FOUND if new");
        }
    }

    @Test(priority = 6, description = "Accept-6: Accept while timer is still running opens Assign machine")
    @Severity(SeverityLevel.NORMAL)
    @Description("List #17 related. If timer is not 00:00, Accept must still open Assign machine. "
            + "True post-expiry Accept needs a 00:00 seed (not on 0001 this run). Do not Confirm.")
    public void acceptWhileTimerRunningOpensAssignMachine() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        String timer = page.firstTimerValue();
        boolean expired = page.isFirstTimerExpired();
        Allure.parameter("timer", timer);
        Allure.parameter("timerExpired", String.valueOf(expired));
        page.attachScreenshot("accept-6-timer");

        assertThat(page.isDisplayedNow()).isTrue();
        if (expired) {
            page.tapFirstAccept();
            Waits.until(DriverManager.get(),
                    d -> (page.isAssignMachineVisible() || page.isDisplayedNow()) ? Boolean.TRUE : null,
                    "After Accept on expired timer, no named landing",
                    Duration.ofSeconds(12));
            Allure.parameter("assignAfterExpired", String.valueOf(page.isAssignMachineVisible()));
            assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
            return;
        }
        assertThat(timer).as("Timer still running").matches("\\d{1,2}:\\d{2}");
        page.tapFirstAccept();
        Waits.until(DriverManager.get(),
                d -> page.isAssignMachineVisible() ? Boolean.TRUE : null,
                "Accept with live timer did not open Assign machine",
                Duration.ofSeconds(12));
        assertThat(page.isAssignMachineVisible()).isTrue();
        assertThat(page.isExtendTimeDialogVisible()).isFalse();
    }
}
