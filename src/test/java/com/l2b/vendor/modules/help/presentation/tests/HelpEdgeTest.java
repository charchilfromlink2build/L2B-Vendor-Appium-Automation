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
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Help &amp; Support edge — Never Submit / Chat now after concern / Accept / Decline /
 * Log Out Confirm.
 *
 * <p><b>Critical Edge Cases (HS-E1–E14)</b>
 * <ol>
 *   <li>E1 Rapid Live chat open/Back stays Vendor</li>
 *   <li>E2 Rapid Raise Ticket open/Back stays Vendor</li>
 *   <li>E3 Rapid Ticket history open/Back stays Vendor</li>
 *   <li>E4 Layout publish checks (soft)</li>
 *   <li>E5 Chat now stays disabled empty concern</li>
 *   <li>E6 Submit stays disabled empty form</li>
 *   <li>E7 Triple probe Live/Raise/History then Help</li>
 *   <li>E8 Reopen after drawer dismiss</li>
 *   <li>E9 Package Vendor through full probe chain</li>
 *   <li>E10 Device Back from Live chat safe</li>
 *   <li>E11 Device Back from Raise Ticket safe</li>
 *   <li>E12 No Accept/Decline from Help path</li>
 *   <li>E13 Idempotent header Back on Help landing</li>
 *   <li>E14 History chrome survives reopen</li>
 *   <li>E15 Chat now disabled after concern-only</li>
 *   <li>E16 Submit disabled after concern+description without Order ID</li>
 *   <li>E17 Concern options soft count</li>
 *   <li>E18 Ticket detail open/close twice</li>
 *   <li>E19 Dialer package then recover Vendor</li>
 *   <li>E20 No Accept/Decline on ticket detail</li>
 *   <li>E21 Device Back from Your Bookings sheet</li>
 *   <li>E22 Rapid Call Support open/recover</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Help & Support edge — rental 9000000001")
public class HelpEdgeTest extends HelpBaseTest {

    @Test(priority = 1, description = "HS-E1: rapid Live chat open/Back stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidLiveChatBackStaysVendor() {
        HelpSupportPage page = reachHelp();
        for (int i = 0; i < 3; i++) {
            page.tapLiveChat();
            sleepQuiet(500);
            page.tapHeaderBack();
            sleepQuiet(500);
            page = ensureHelpLanding(page);
        }
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("help");
    }

    @Test(priority = 2, description = "HS-E2: rapid Raise Ticket open/Back stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidRaiseTicketBackStaysVendor() {
        HelpSupportPage page = reachHelp();
        for (int i = 0; i < 3; i++) {
            page.tapRaiseTicket();
            sleepQuiet(500);
            page.tapHeaderBack();
            sleepQuiet(500);
            page = ensureHelpLanding(page);
        }
        assertThat(page.isRaiseTicketVisible()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 3, description = "HS-E3: rapid Ticket history open/Back stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidHistoryBackStaysVendor() {
        HelpSupportPage page = reachHelp();
        for (int i = 0; i < 3; i++) {
            page.tapTicketHistory();
            sleepQuiet(500);
            page.tapHeaderBack();
            sleepQuiet(500);
            page = ensureHelpLanding(page);
        }
        assertThat(page.isTicketHistoryVisible()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 4, description = "HS-E4: layout publish checks")
    @Severity(SeverityLevel.CRITICAL)
    public void layoutPublishChecks() {
        HelpSupportPage page = reachHelp();
        assertThat(page.isDisplayedNow()).as("hard: Help open").isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.headerBackAligned()).as("Back | title").isTrue();
        softly.assertThat(page.titleLeftInsetOk()).as("title left inset").isTrue();
        softly.assertThat(page.callSupportLayoutOk()).as("Call Support").isTrue();
        softly.assertThat(page.raiseHistoryRowLayoutOk()).as("Raise | history").isTrue();
        softly.assertThat(page.hoursAboveLiveChatOk()).as("hours above Live chat").isTrue();
        softly.assertAll();
    }

    @Test(priority = 5, description = "HS-E5: Chat now disabled without concern")
    @Severity(SeverityLevel.CRITICAL)
    public void chatNowDisabledEmpty() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(900);
        assertThat(page.isChatNowVisible()).isTrue();
        assertThat(page.isChatNowDisabled()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(800);
    }

    @Test(priority = 6, description = "HS-E6: Submit disabled empty form")
    @Severity(SeverityLevel.CRITICAL)
    public void submitDisabledEmpty() {
        HelpSupportPage page = reachHelp();
        page.tapRaiseTicket();
        sleepQuiet(900);
        assertThat(page.isSubmitVisible()).isTrue();
        assertThat(page.isSubmitDisabled()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(800);
    }

    @Test(priority = 7, description = "HS-E7: triple probe Live/Raise/History then Help")
    @Severity(SeverityLevel.CRITICAL)
    public void tripleProbeThenHelp() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(700);
        page.tapHeaderBack();
        sleepQuiet(600);
        page = ensureHelpLanding(page);
        page.tapRaiseTicket();
        sleepQuiet(700);
        page.tapHeaderBack();
        sleepQuiet(600);
        page = ensureHelpLanding(page);
        page.tapTicketHistory();
        sleepQuiet(700);
        page.tapHeaderBack();
        sleepQuiet(700);
        page = ensureHelpLanding(page);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isWorkingHoursVisible()).isTrue();
    }

    @Test(priority = 8, description = "HS-E8: reopen Help after drawer dismiss+Profile")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterDrawerDismiss() {
        HelpSupportPage page = reachHelp();
        page.tapHeaderBack();
        sleepQuiet(700);
        new ProfileDrawerPage().tapCloseNavigationMenu();
        sleepQuiet(800);
        // Stay on session — do not full re-login
        new com.l2b.vendor.modules.home.presentation.pages.HomePage().tapDesc("Profile");
        sleepQuiet(900);
        new ProfileDrawerPage().tapRow("Help");
        sleepQuiet(1100);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("help");
    }

    @Test(priority = 9, description = "HS-E9: package Vendor through full probe chain")
    @Severity(SeverityLevel.BLOCKER)
    public void packageVendorThroughProbes() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(600);
        page.tapHeaderBack();
        sleepQuiet(500);
        page = ensureHelpLanding(page);
        page.tapRaiseTicket();
        sleepQuiet(600);
        page.tapHeaderBack();
        sleepQuiet(500);
        page = ensureHelpLanding(page);
        page.tapTicketHistory();
        sleepQuiet(600);
        page.tapHeaderBack();
        sleepQuiet(500);
        assertThat(vendorPackage()).contains("l2b");
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
    }

    @Test(priority = 10, description = "HS-E10: device Back from Live chat safe")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBackFromLiveChat() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(800);
        page.pressDeviceBack();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(vendorPackage()).contains("l2b");
        assertThat(launcherNow()).isFalse();
    }

    @Test(priority = 11, description = "HS-E11: device Back from Raise Ticket safe")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBackFromRaiseTicket() {
        HelpSupportPage page = reachHelp();
        page.tapRaiseTicket();
        sleepQuiet(800);
        page.pressDeviceBack();
        sleepQuiet(900);
        assertThat(vendorPackage()).contains("l2b");
        assertThat(launcherNow()).isFalse();
    }

    @Test(priority = 12, description = "HS-E12: no Accept/Decline from Help path")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Help path must never surface Accept/Decline booking chrome.")
    public void noAcceptDeclineOnHelpPath() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(600);
        page.tapHeaderBack();
        sleepQuiet(500);
        page = ensureHelpLanding(page);
        page.tapRaiseTicket();
        sleepQuiet(600);
        page.tapHeaderBack();
        sleepQuiet(500);
        page = ensureHelpLanding(page);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.visibleTexts().stream().noneMatch(t -> t.equals("Accept")))
                .as("no Accept").isTrue();
        softly.assertThat(page.visibleTexts().stream().noneMatch(t -> t.equals("Decline")))
                .as("no Decline").isTrue();
        softly.assertAll();
    }

    @Test(priority = 13, description = "HS-E13: idempotent header Back on Help landing")
    @Severity(SeverityLevel.NORMAL)
    public void idempotentHeaderBackOnLanding() {
        HelpSupportPage page = reachHelp();
        page.tapHeaderBack();
        sleepQuiet(800);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
        // Second Back should not invent Accept/Decline; stay Vendor
        page.pressDeviceBack();
        sleepQuiet(800);
        assertThat(vendorPackage()).contains("l2b");
        assertThat(launcherNow()).isFalse();
    }

    @Test(priority = 14, description = "HS-E14: history chrome survives reopen")
    @Severity(SeverityLevel.CRITICAL)
    public void historyChromeSurvivesReopen() {
        HelpSupportPage page = reachHelp();
        page.tapTicketHistory();
        sleepQuiet(900);
        assertThat(page.isTicketHistoryScreenVisible()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(800);
        page = ensureHelpLanding(page);
        page.tapTicketHistory();
        sleepQuiet(900);
        assertThat(page.isTicketHistoryScreenVisible()).isTrue();
        assertThat(page.isRecentTicketsVisible() || page.hasTicketIdVisible()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(700);
    }

    @Test(priority = 15, description = "HS-E15: Chat now disabled after concern-only")
    @Severity(SeverityLevel.CRITICAL)
    public void chatNowDisabledAfterConcernOnly() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(800);
        page.tapTicketConcern();
        sleepQuiet(500);
        page.selectPaymentBillingConcern();
        sleepQuiet(800);
        assertThat(page.isOrderIdFieldVisible()).isTrue();
        assertThat(page.isChatNowDisabled()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(700);
    }

    @Test(priority = 16, description = "HS-E16: Submit disabled without Order ID")
    @Severity(SeverityLevel.CRITICAL)
    public void submitDisabledWithoutOrderId() {
        HelpSupportPage page = reachHelp();
        page.tapRaiseTicket();
        sleepQuiet(800);
        page.tapTicketConcern();
        sleepQuiet(500);
        page.selectPaymentBillingConcern();
        sleepQuiet(700);
        page.typeDescription("probe only");
        sleepQuiet(400);
        assertThat(page.isSubmitDisabled()).isTrue();
        page.clearDescription();
        sleepQuiet(300);
        assertThat(page.isSubmitDisabled()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(700);
    }

    @Test(priority = 17, description = "HS-E17: concern options soft count")
    @Severity(SeverityLevel.NORMAL)
    public void concernOptionsSoftCount() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(800);
        page.tapTicketConcern();
        sleepQuiet(800);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.allConcernOptionsVisible()).as(">=10 concerns").isTrue();
        softly.assertThat(page.visibleTexts()).as("Other").anyMatch(t -> t.equals("Other"));
        softly.assertThat(page.visibleTexts()).as("Rental Machine")
                .anyMatch(t -> t.contains("Rental Machine"));
        softly.assertAll();
        page.pressDeviceBack();
        sleepQuiet(600);
        ensureHelpLanding(page);
    }

    @Test(priority = 18, description = "HS-E18: ticket detail open/close twice")
    @Severity(SeverityLevel.CRITICAL)
    public void ticketDetailOpenCloseTwice() {
        HelpSupportPage page = reachHelp();
        page.tapTicketHistory();
        sleepQuiet(800);
        for (int i = 0; i < 2; i++) {
            page.tapFirstTicket();
            sleepQuiet(900);
            assertThat(page.isTicketDetailVisible()).isTrue();
            page.tapHeaderBack();
            sleepQuiet(800);
            assertThat(page.isTicketHistoryScreenVisible()).isTrue();
        }
        page.tapHeaderBack();
        sleepQuiet(700);
        ensureHelpLanding(page);
    }

    @Test(priority = 19, description = "HS-E19: dialer package then recover Vendor")
    @Severity(SeverityLevel.BLOCKER)
    public void dialerThenRecoverVendor() {
        HelpSupportPage page = reachHelp();
        page.tapCallSupport();
        sleepQuiet(1400);
        assertThat(page.isSupportDialerOpen()).isTrue();
        for (int i = 0; i < 5; i++) {
            page.pressDeviceBack();
            sleepQuiet(700);
            if (vendorPackage().contains("l2b")) {
                break;
            }
        }
        assertThat(vendorPackage()).contains("l2b");
        assertThat(launcherNow()).isFalse();
    }

    @Test(priority = 20, description = "HS-E20: no Accept/Decline on ticket detail")
    @Severity(SeverityLevel.CRITICAL)
    public void noAcceptDeclineOnTicketDetail() {
        HelpSupportPage page = reachHelp();
        page.tapTicketHistory();
        sleepQuiet(800);
        page.tapFirstTicket();
        sleepQuiet(900);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.visibleTexts().stream().noneMatch(t -> t.equals("Accept")))
                .as("no Accept").isTrue();
        softly.assertThat(page.visibleTexts().stream().noneMatch(t -> t.equals("Decline")))
                .as("no Decline").isTrue();
        softly.assertAll();
        page.tapHeaderBack();
        sleepQuiet(700);
        ensureHelpLanding(page);
    }

    @Test(priority = 21, description = "HS-E21: device Back from Your Bookings sheet")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBackFromBookingsSheet() {
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
        assertThat(vendorPackage()).contains("l2b");
        assertThat(launcherNow()).isFalse();
        assertThat(page.isLiveChatFormVisible() || page.isDisplayedNow()).isTrue();
        ensureHelpLanding(page);
    }

    @Test(priority = 22, description = "HS-E22: rapid Call Support open/recover")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidCallSupportRecover() {
        HelpSupportPage page = reachHelp();
        for (int i = 0; i < 2; i++) {
            if (!page.isCallSupportVisible()) {
                page = ensureHelpLanding(page);
            }
            page.tapCallSupport();
            sleepQuiet(1200);
            for (int b = 0; b < 4; b++) {
                if (vendorPackage().contains("l2b") && page.isCallSupportVisible()) {
                    break;
                }
                page.pressDeviceBack();
                sleepQuiet(600);
            }
        }
        assertThat(vendorPackage()).contains("l2b");
        assertThat(launcherNow()).isFalse();
    }
}
