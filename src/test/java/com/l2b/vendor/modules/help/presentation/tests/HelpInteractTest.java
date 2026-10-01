package com.l2b.vendor.modules.help.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.help.presentation.pages.HelpSupportPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Help &amp; Support interact — Happy Path actions. Never Submit / Chat now after concern /
 * Accept / Decline / Log Out Confirm.
 *
 * <p><b>Happy Path actions (HS-I1–I12)</b>
 * <ol>
 *   <li>I1 Live chat → Back → Help</li>
 *   <li>I2 Raise Ticket → Back → Help</li>
 *   <li>I3 Ticket history → Back → Help</li>
 *   <li>I4 Header Back → drawer</li>
 *   <li>I5 Device Back → drawer</li>
 *   <li>I6 Reopen Help after Back</li>
 *   <li>I7 Ticket Concern on Live chat → Back (no Chat now)</li>
 *   <li>I8 Ticket Concern on Raise Ticket → Back (no Submit)</li>
 *   <li>I9 Live chat then Raise Ticket chain</li>
 *   <li>I10 History then Live chat chain</li>
 *   <li>I11 Call Support visibility then stay/recover Vendor</li>
 *   <li>I12 Chrome intact after history Back</li>
 *   <li>I13 Concern → Order ID → Back (no Chat now)</li>
 *   <li>I14 Your Bookings sheet → dismiss → Live chat form</li>
 *   <li>I15 Select booking → Chat now enables (never tap)</li>
 *   <li>I16 Raise: concern+desc+booking → Submit enables (never Submit)</li>
 *   <li>I17 Ticket detail → Back → history</li>
 *   <li>I18 Call Support → number → recover Help</li>
 *   <li>I19 Bookings Active/Upcoming tabs switch</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Help & Support interact — rental 9000000001")
public class HelpInteractTest extends HelpBaseTest {

    @Test(priority = 1, description = "HS-I1: Live chat → Back → Help")
    @Severity(SeverityLevel.BLOCKER)
    public void liveChatBackToHelp() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(1000);
        assertThat(page.isLiveChatFormVisible()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("help");
    }

    @Test(priority = 2, description = "HS-I2: Raise Ticket → Back → Help")
    @Severity(SeverityLevel.BLOCKER)
    public void raiseTicketBackToHelp() {
        HelpSupportPage page = reachHelp();
        page.tapRaiseTicket();
        sleepQuiet(1000);
        assertThat(page.isRaiseTicketFormVisible()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
    }

    @Test(priority = 3, description = "HS-I3: Ticket history → Back → Help")
    @Severity(SeverityLevel.BLOCKER)
    public void ticketHistoryBackToHelp() {
        HelpSupportPage page = reachHelp();
        page.tapTicketHistory();
        sleepQuiet(1000);
        assertThat(page.isTicketHistoryScreenVisible()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
    }

    @Test(priority = 4, description = "HS-I4: header Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void headerBackToDrawer() {
        HelpSupportPage page = reachHelp();
        page.tapHeaderBack();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
        assertThat(profileDrawerNow()).isTrue();
    }

    @Test(priority = 5, description = "HS-I5: device Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBackToDrawer() {
        HelpSupportPage page = reachHelp();
        page.pressDeviceBack();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 6, description = "HS-I6: reopen Help after Back")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterBack() {
        HelpSupportPage page = reachHelp();
        page.tapHeaderBack();
        sleepQuiet(800);
        new ProfileDrawerPage().tapRow("Help");
        sleepQuiet(1000);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("help");
    }

    @Test(priority = 7, description = "HS-I7: Ticket Concern on Live chat → Back (no Chat now)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Open concern dropdown only — never Chat now.")
    public void liveChatConcernThenBack() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(900);
        page.tapTicketConcern();
        sleepQuiet(800);
        page.attachScreenshot("hs-i7-concern");
        // Dismiss sheet/list without selecting if possible; device Back is safest
        page.pressDeviceBack();
        sleepQuiet(700);
        if (page.isLiveChatFormVisible()) {
            page.tapHeaderBack();
            sleepQuiet(800);
        }
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 8, description = "HS-I8: Ticket Concern on Raise Ticket → Back (no Submit)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Open concern dropdown only — never Submit.")
    public void raiseTicketConcernThenBack() {
        HelpSupportPage page = reachHelp();
        page.tapRaiseTicket();
        sleepQuiet(900);
        page.tapTicketConcern();
        sleepQuiet(800);
        page.pressDeviceBack();
        sleepQuiet(700);
        if (page.isRaiseTicketFormVisible()) {
            page.tapHeaderBack();
            sleepQuiet(800);
        }
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
    }

    @Test(priority = 9, description = "HS-I9: Live chat then Raise Ticket chain")
    @Severity(SeverityLevel.CRITICAL)
    public void liveChatThenRaiseTicketChain() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(800);
        page.tapHeaderBack();
        sleepQuiet(800);
        page = ensureHelpLanding(page);
        page.tapRaiseTicket();
        sleepQuiet(900);
        assertThat(page.isRaiseTicketFormVisible()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(800);
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
    }

    @Test(priority = 10, description = "HS-I10: history then Live chat chain")
    @Severity(SeverityLevel.CRITICAL)
    public void historyThenLiveChatChain() {
        HelpSupportPage page = reachHelp();
        page.tapTicketHistory();
        sleepQuiet(800);
        page.tapHeaderBack();
        sleepQuiet(800);
        page = ensureHelpLanding(page);
        page.tapLiveChat();
        sleepQuiet(900);
        assertThat(page.isLiveChatFormVisible()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(800);
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
    }

    @Test(priority = 11, description = "HS-I11: Call Support visible; recover if dialer")
    @Severity(SeverityLevel.NORMAL)
    public void callSupportVisibleOrRecover() {
        HelpSupportPage page = reachHelp();
        assertThat(page.isCallSupportVisible()).isTrue();
        page.tapCallSupport();
        sleepQuiet(1200);
        page.attachScreenshot("hs-i11-call");
        // Dialer or sheet — Back until Help or drawer
        for (int i = 0; i < 4; i++) {
            if (page.isDisplayedNow() && page.isLiveChatVisible()) {
                break;
            }
            page.pressDeviceBack();
            sleepQuiet(700);
        }
        assertThat(vendorPackage()).contains("l2b");
        Allure.parameter("after", classifyRentalNow());
    }

    @Test(priority = 12, description = "HS-I12: chrome intact after history Back")
    @Severity(SeverityLevel.CRITICAL)
    public void chromeIntactAfterHistory() {
        HelpSupportPage page = reachHelp();
        page.tapTicketHistory();
        sleepQuiet(900);
        page.tapHeaderBack();
        sleepQuiet(900);
        page = ensureHelpLanding(page);
        assertThat(page.isLiveChatVisible()).isTrue();
        assertThat(page.isRaiseTicketVisible()).isTrue();
        assertThat(page.isTicketHistoryVisible()).isTrue();
        assertThat(page.isWorkingHoursVisible()).isTrue();
        assertThat(page.isCallSupportVisible()).isTrue();
    }

    @Test(priority = 13, description = "HS-I13: concern → Order ID → Back (no Chat now)")
    @Severity(SeverityLevel.CRITICAL)
    public void concernThenOrderIdThenBack() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(800);
        page.tapTicketConcern();
        sleepQuiet(600);
        page.selectPaymentBillingConcern();
        sleepQuiet(800);
        assertThat(page.isOrderIdFieldVisible()).isTrue();
        assertThat(page.isChatNowDisabled()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(800);
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
    }

    @Test(priority = 14, description = "HS-I14: Your Bookings sheet dismiss → Live chat form")
    @Severity(SeverityLevel.CRITICAL)
    public void bookingsSheetDismissKeepsLiveChat() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(800);
        page.tapTicketConcern();
        sleepQuiet(500);
        page.selectPaymentBillingConcern();
        sleepQuiet(700);
        page.tapOrderIdField();
        sleepQuiet(900);
        assertThat(page.isYourBookingsSheetVisible()).isTrue();
        page.pressDeviceBack();
        sleepQuiet(800);
        assertThat(page.isLiveChatFormVisible()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(800);
        ensureHelpLanding(page);
    }

    @Test(priority = 15, description = "HS-I15: select booking → Chat now enables (never tap)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Functional enablement after Order ID. Never Chat now (external).")
    public void selectBookingEnablesChatNow() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(800);
        page.tapTicketConcern();
        sleepQuiet(500);
        page.selectPaymentBillingConcern();
        sleepQuiet(700);
        page.tapOrderIdField();
        sleepQuiet(1000);
        page.selectFirstBookingCard();
        sleepQuiet(1100);
        page.attachScreenshot("hs-i15-chat-enabled");
        assertThat(page.isChatNowEnabled()).as("Chat now enabled after booking").isTrue();
        // NEVER Chat now
        page.tapHeaderBack();
        sleepQuiet(800);
        ensureHelpLanding(page);
    }

    @Test(priority = 16, description = "HS-I16: Raise concern+desc+booking → Submit enables (never Submit)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Full form enablement. Never Submit ticket.")
    public void raiseFullFormEnablesSubmit() {
        HelpSupportPage page = reachHelp();
        page.tapRaiseTicket();
        sleepQuiet(900);
        page.tapTicketConcern();
        sleepQuiet(500);
        page.selectPaymentBillingConcern();
        sleepQuiet(700);
        page.typeDescription("Automation probe — do not submit");
        sleepQuiet(400);
        assertThat(page.isSubmitDisabled()).as("still disabled without Order ID").isTrue();
        page.tapOrderIdField();
        sleepQuiet(1000);
        page.selectFirstBookingCard();
        sleepQuiet(1100);
        page.attachScreenshot("hs-i16-submit-enabled");
        assertThat(page.isSubmitEnabled()).as("Submit enabled full form").isTrue();
        // NEVER Submit
        page.tapHeaderBack();
        sleepQuiet(800);
        ensureHelpLanding(page);
    }

    @Test(priority = 17, description = "HS-I17: ticket detail → Back → history")
    @Severity(SeverityLevel.CRITICAL)
    public void ticketDetailBackToHistory() {
        HelpSupportPage page = reachHelp();
        page.tapTicketHistory();
        sleepQuiet(900);
        page.tapFirstTicket();
        sleepQuiet(1000);
        assertThat(page.isTicketDetailVisible()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(page.isTicketHistoryScreenVisible()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(800);
        ensureHelpLanding(page);
    }

    @Test(priority = 18, description = "HS-I18: Call Support → number → recover Help")
    @Severity(SeverityLevel.CRITICAL)
    public void callSupportRecoverToHelp() {
        HelpSupportPage page = reachHelp();
        page.tapCallSupport();
        sleepQuiet(1400);
        assertThat(page.isSupportDialerOpen()).isTrue();
        for (int i = 0; i < 5; i++) {
            if (vendorPackage().contains("l2b") && page.isHelpTitleVisible()) {
                break;
            }
            page.pressDeviceBack();
            sleepQuiet(700);
        }
        assertThat(vendorPackage()).contains("l2b");
        if (!page.isDisplayedNow()) {
            page = ensureHelpLanding(page);
        }
        assertThat(page.isCallSupportVisible() || page.isLiveChatVisible()).isTrue();
    }

    @Test(priority = 19, description = "HS-I19: Bookings Active/Upcoming tabs switch")
    @Severity(SeverityLevel.NORMAL)
    public void bookingsTabsSwitch() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(800);
        page.tapTicketConcern();
        sleepQuiet(500);
        page.selectPaymentBillingConcern();
        sleepQuiet(700);
        page.tapOrderIdField();
        sleepQuiet(900);
        assertThat(page.isYourBookingsSheetVisible()).isTrue();
        page.tapBookingsTab("Active");
        sleepQuiet(700);
        page.attachScreenshot("hs-i19-active");
        page.tapBookingsTab("Upcoming");
        sleepQuiet(700);
        page.tapBookingsTab("Completed");
        sleepQuiet(600);
        assertThat(page.isYourBookingsSheetVisible()).isTrue();
        page.pressDeviceBack();
        sleepQuiet(700);
        ensureHelpLanding(page);
    }
}
