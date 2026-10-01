package com.l2b.vendor.modules.help.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.help.presentation.pages.HelpSupportPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Help &amp; Support landing — Profile drawer → Help on {@code 9000000001}.
 * Dump {@code /tmp/l2b-help-0001-20261001}. Never Submit / Chat now after concern.
 *
 * <p><b>Happy Path (HS-L1–L13)</b>
 * <ol>
 *   <li>L1 Drawer Help → Help &amp; Support</li>
 *   <li>L2 Title + Back + Call Support</li>
 *   <li>L3 Intro + Working Hours 8:00 AM – 8:00 PM</li>
 *   <li>L4 Live chat CTA</li>
 *   <li>L5 Raise Ticket card + Raise Ticket + Ticket history</li>
 *   <li>L6 Header layout (Back | title | Call Support)</li>
 *   <li>L7 Raise Ticket | Ticket history same row</li>
 *   <li>L8 Hours above Live chat</li>
 *   <li>L9 Stay Vendor package</li>
 *   <li>L10 Live chat opens Submit a query + Chat now disabled</li>
 *   <li>L11 Raise Ticket opens form + Submit disabled</li>
 *   <li>L12 Ticket history Recent tickets + TKT-</li>
 *   <li>L13 Soft layout publish batch</li>
 *   <li>L14 Eleven Ticket Concern options</li>
 *   <li>L15 Concern pick → Order ID appears; Chat now still disabled</li>
 *   <li>L16 Order ID → Your Bookings sheet (Completed/Active/Upcoming)</li>
 *   <li>L17 Ticket Details chrome from history</li>
 *   <li>L18 Call Support dialer +91 1800 00 0000</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Help & Support landing — rental 9000000001")
public class HelpLandingTest extends HelpBaseTest {

    @Test(priority = 1, description = "HS-L1: drawer Help → Help & Support")
    @Severity(SeverityLevel.BLOCKER)
    public void helpOpensFromDrawer() {
        HelpSupportPage page = reachHelp();
        page.attachScreenshot("hs-l1");
        assertThat(classifyRentalNow()).isEqualTo("help");
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 2, description = "HS-L2: title + Back + Call Support")
    @Severity(SeverityLevel.CRITICAL)
    public void titleBackCallSupportChrome() {
        HelpSupportPage page = reachHelp();
        assertThat(page.isHelpTitleVisible()).isTrue();
        assertThat(page.isBackVisible()).isTrue();
        assertThat(page.isCallSupportVisible()).isTrue();
        assertThat(page.headerBackAligned()).isTrue();
    }

    @Test(priority = 3, description = "HS-L3: intro + Working Hours 8:00 AM – 8:00 PM")
    @Severity(SeverityLevel.CRITICAL)
    public void introAndWorkingHours() {
        HelpSupportPage page = reachHelp();
        assertThat(page.isDisplayedNow()).as("hard: Help open").isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isIntroCopyVisible()).as("intro copy").isTrue();
        softly.assertThat(page.isWorkingHoursVisible()).as("Working Hours 8–8").isTrue();
        softly.assertAll();
    }

    @Test(priority = 4, description = "HS-L4: Live chat CTA")
    @Severity(SeverityLevel.CRITICAL)
    public void liveChatCtaVisible() {
        HelpSupportPage page = reachHelp();
        assertThat(page.isLiveChatVisible()).isTrue();
    }

    @Test(priority = 5, description = "HS-L5: Raise Ticket card + CTAs")
    @Severity(SeverityLevel.CRITICAL)
    public void raiseTicketCardAndHistory() {
        HelpSupportPage page = reachHelp();
        assertThat(page.isDisplayedNow()).as("hard: Help open").isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isRaiseTicketCardHeadingVisible()).as("card heading").isTrue();
        softly.assertThat(page.isRaiseTicketCardBodyVisible()).as("card body").isTrue();
        softly.assertThat(page.isRaiseTicketVisible()).as("Raise Ticket").isTrue();
        softly.assertThat(page.isTicketHistoryVisible()).as("Ticket history").isTrue();
        softly.assertAll();
    }

    @Test(priority = 6, description = "HS-L6: Call Support right of title")
    @Severity(SeverityLevel.NORMAL)
    public void callSupportLayout() {
        HelpSupportPage page = reachHelp();
        assertThat(page.callSupportLayoutOk()).isTrue();
        assertThat(page.titleLeftInsetOk()).isTrue();
    }

    @Test(priority = 7, description = "HS-L7: Raise Ticket | Ticket history same row")
    @Severity(SeverityLevel.NORMAL)
    public void raiseHistorySameRow() {
        HelpSupportPage page = reachHelp();
        assertThat(page.raiseHistoryRowLayoutOk()).isTrue();
    }

    @Test(priority = 8, description = "HS-L8: Working Hours above Live chat")
    @Severity(SeverityLevel.NORMAL)
    public void hoursAboveLiveChat() {
        HelpSupportPage page = reachHelp();
        assertThat(page.hoursAboveLiveChatOk()).isTrue();
    }

    @Test(priority = 9, description = "HS-L9: stay Vendor package")
    @Severity(SeverityLevel.BLOCKER)
    public void staysVendorPackage() {
        HelpSupportPage page = reachHelp();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 10, description = "HS-L10: Live chat form + Chat now disabled")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Open Live chat chrome only — never Chat now after selecting concern.")
    public void liveChatFormChrome() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(1100);
        page.attachScreenshot("hs-l10-live-chat");
        assertThat(page.isLiveChatFormVisible()).isTrue();
        assertThat(page.isTicketConcernVisible()).isTrue();
        assertThat(page.isChatNowDisabled()).as("Chat now disabled without concern").isTrue();
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
    }

    @Test(priority = 11, description = "HS-L11: Raise Ticket form + Submit disabled")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Open Raise Ticket form only — never Submit.")
    public void raiseTicketFormChrome() {
        HelpSupportPage page = reachHelp();
        page.tapRaiseTicket();
        sleepQuiet(1100);
        page.attachScreenshot("hs-l11-raise");
        assertThat(page.isRaiseTicketFormVisible()).isTrue();
        assertThat(page.isTicketConcernVisible()).isTrue();
        assertThat(page.isDescriptionEditTextVisible()).isTrue();
        assertThat(page.isSubmitDisabled()).as("Submit disabled empty form").isTrue();
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
    }

    @Test(priority = 12, description = "HS-L12: Ticket history Recent tickets + TKT-")
    @Severity(SeverityLevel.CRITICAL)
    public void ticketHistoryChrome() {
        HelpSupportPage page = reachHelp();
        page.tapTicketHistory();
        sleepQuiet(1100);
        page.attachScreenshot("hs-l12-history");
        assertThat(page.isTicketHistoryScreenVisible()).isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isRecentTicketsVisible()).as("Recent tickets").isTrue();
        softly.assertThat(page.hasTicketIdVisible()).as("TKT- id").isTrue();
        softly.assertThat(page.isPendingStatusVisible()).as("Pending").isTrue();
        softly.assertThat(page.isHistoryRowLabelsVisible()).as("Date/Order/Topic").isTrue();
        softly.assertAll();
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(ensureHelpLanding(page).isDisplayedNow()).isTrue();
    }

    @Test(priority = 13, description = "HS-L13: soft layout publish batch")
    @Severity(SeverityLevel.NORMAL)
    public void softLayoutPublishBatch() {
        HelpSupportPage page = reachHelp();
        assertThat(page.isDisplayedNow()).as("hard: Help open").isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.headerBackAligned()).as("Back | title").isTrue();
        softly.assertThat(page.titleLeftInsetOk()).as("title inset").isTrue();
        softly.assertThat(page.callSupportLayoutOk()).as("Call Support").isTrue();
        softly.assertThat(page.raiseHistoryRowLayoutOk()).as("Raise | history row").isTrue();
        softly.assertThat(page.hoursAboveLiveChatOk()).as("hours above Live chat").isTrue();
        softly.assertAll();
    }

    @Test(priority = 14, description = "HS-L14: eleven Ticket Concern options")
    @Severity(SeverityLevel.CRITICAL)
    public void elevenConcernOptions() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(900);
        page.tapTicketConcern();
        sleepQuiet(900);
        page.attachScreenshot("hs-l14-concerns");
        assertThat(page.isConcernSheetVisible()).isTrue();
        assertThat(page.allConcernOptionsVisible()).as("11 concern options").isTrue();
        page.pressDeviceBack();
        sleepQuiet(700);
        ensureHelpLanding(page);
    }

    @Test(priority = 15, description = "HS-L15: concern → Order ID; Chat now still disabled")
    @Severity(SeverityLevel.CRITICAL)
    @Description("API/UI: Order ID required after concern. Never Chat now.")
    public void concernRevealsOrderIdChatStillDisabled() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(900);
        page.tapTicketConcern();
        sleepQuiet(700);
        page.selectPaymentBillingConcern();
        sleepQuiet(900);
        assertThat(page.isOrderIdFieldVisible()).as("Order ID after concern").isTrue();
        assertThat(page.isChatNowDisabled()).as("Chat now needs Order ID too").isTrue();
        page.tapHeaderBack();
        sleepQuiet(800);
        ensureHelpLanding(page);
    }

    @Test(priority = 16, description = "HS-L16: Order ID → Your Bookings sheet tabs")
    @Severity(SeverityLevel.CRITICAL)
    public void orderIdOpensYourBookingsSheet() {
        HelpSupportPage page = reachHelp();
        page.tapLiveChat();
        sleepQuiet(800);
        page.tapTicketConcern();
        sleepQuiet(600);
        page.selectPaymentBillingConcern();
        sleepQuiet(800);
        page.tapOrderIdField();
        sleepQuiet(1000);
        page.attachScreenshot("hs-l16-bookings");
        assertThat(page.isYourBookingsSheetVisible()).isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.visibleTexts()).as("Completed tab").contains("Completed");
        softly.assertThat(page.visibleTexts()).as("Active tab").contains("Active");
        softly.assertThat(page.visibleTexts()).as("Upcoming tab").contains("Upcoming");
        softly.assertAll();
        page.pressDeviceBack();
        sleepQuiet(700);
        ensureHelpLanding(page);
    }

    @Test(priority = 17, description = "HS-L17: Ticket Details chrome from history")
    @Severity(SeverityLevel.CRITICAL)
    public void ticketDetailsChrome() {
        HelpSupportPage page = reachHelp();
        page.tapTicketHistory();
        sleepQuiet(1000);
        page.tapFirstTicket();
        sleepQuiet(1100);
        page.attachScreenshot("hs-l17-detail");
        assertThat(page.isTicketDetailVisible()).isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isPendingStatusVisible()).as("Pending").isTrue();
        softly.assertThat(page.isNoAgentRepliesVisible()).as("timeline/empty replies").isTrue();
        softly.assertThat(page.visibleTexts().stream().anyMatch(t -> t.contains("Topic of Concern")
                || t.contains("Payment"))).as("topic").isTrue();
        softly.assertAll();
        page.tapHeaderBack();
        sleepQuiet(800);
        ensureHelpLanding(page);
    }

    @Test(priority = 18, description = "HS-L18: Call Support dialer +91 1800 00 0000")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Never tap dialer Call.")
    public void callSupportOpensDialerWithNumber() {
        HelpSupportPage page = reachHelp();
        page.tapCallSupport();
        sleepQuiet(1500);
        page.attachScreenshot("hs-l18-dialer");
        assertThat(page.isSupportDialerOpen()).as("dialer or support number").isTrue();
        // recover — never Call
        for (int i = 0; i < 4; i++) {
            if (vendorPackage().contains("l2b") && page.isDisplayedNow()) {
                break;
            }
            page.pressDeviceBack();
            sleepQuiet(700);
        }
        assertThat(vendorPackage()).contains("l2b");
    }
}
