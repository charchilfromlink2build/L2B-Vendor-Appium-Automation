package com.l2b.vendor.modules.help.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.help.presentation.pages.HelpSupportPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import com.l2b.vendor.modules.settings.presentation.tests.ProfileDrawerBaseTest;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

/**
 * Shared 9000000001 path onto Help &amp; Support via Profile drawer.
 * Never Chat now (after concern) / Submit ticket / Accept / Decline / Log Out Confirm.
 */
public abstract class HelpBaseTest extends ProfileDrawerBaseTest {

    @Step("Reach Help & Support via Profile → Help")
    protected HelpSupportPage reachHelp() {
        ProfileDrawerPage drawer = reachProfileDrawer();
        drawer.tapRow("Help");
        HelpSupportPage page = new HelpSupportPage();
        page.waitUntilLoaded();
        Allure.parameter("entry", "drawer-Help");
        Allure.parameter("after", classifyRentalNow());
        return page;
    }

    @Step("Dismiss toward Help landing (Back only — never Submit / Chat now / dialer Call)")
    protected HelpSupportPage dismissToHelp(HelpSupportPage page) {
        for (int i = 0; i < 10; i++) {
            if (page.isHelpTitleVisible() && page.isLiveChatVisible() && page.isRaiseTicketVisible()
                    && page.isTicketHistoryVisible() && !page.isChatNowVisible()
                    && !page.isSubmitVisible() && !page.isTicketDetailVisible()) {
                return page;
            }
            String named = classifyRentalNow();
            if ("launcher".equals(named)) {
                return page;
            }
            // Dialer / external
            if (!vendorPackage().contains("l2b")) {
                DriverManager.get().navigate().back();
                sleepQuiet(700);
                continue;
            }
            if (page.isYourBookingsSheetVisible() || page.isConcernSheetVisible()
                    || page.isTicketDetailVisible() || page.isLiveChatFormVisible()
                    || page.isRaiseTicketFormVisible() || page.isTicketHistoryScreenVisible()) {
                if (page.isBackVisible()) {
                    page.tapHeaderBack();
                } else {
                    DriverManager.get().navigate().back();
                }
                sleepQuiet(700);
                continue;
            }
            if ("home-drawer".equals(named) || profileDrawerNow()) {
                new ProfileDrawerPage().tapRow("Help");
                sleepQuiet(1000);
                if (page.isDisplayedNow()) {
                    return page;
                }
            }
            if ("home".equals(named) || "extend-time".equals(named)) {
                try {
                    new HomePage().tapDesc("Profile");
                    sleepQuiet(900);
                    new ProfileDrawerPage().tapRow("Help");
                    sleepQuiet(1000);
                    if (page.isDisplayedNow()) {
                        return page;
                    }
                } catch (RuntimeException ignored) {
                }
            }
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        if (!page.isDisplayedNow()) {
            return reachHelp();
        }
        return page;
    }

    @Step("Ensure Help landing (not Live chat / Raise Ticket / history)")
    protected HelpSupportPage ensureHelpLanding(HelpSupportPage page) {
        if (page.isHelpTitleVisible() && page.isLiveChatVisible() && page.isRaiseTicketVisible()
                && page.isTicketHistoryVisible()) {
            return page;
        }
        return dismissToHelp(page);
    }
}
