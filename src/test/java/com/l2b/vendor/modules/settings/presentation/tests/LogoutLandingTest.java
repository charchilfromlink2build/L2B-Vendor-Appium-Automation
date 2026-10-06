package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Log Out landing / dialog chrome — Confirm path covered in Interact/Edge.
 *
 * <p><b>Happy Path (LO-L1–L8)</b>
 */
@Epic("Vendor app")
@Feature("Log Out landing — rental 9000000001")
public class LogoutLandingTest extends LogoutBaseTest {

    @Test(priority = 1, description = "LO-L1: Log Out CTA opens dialog")
    @Severity(SeverityLevel.BLOCKER)
    public void logOutOpensDialog() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(800);
        page.attachScreenshot("lo-l1");
        assertThat(page.isLogoutDialogVisible()).isTrue();
        page.tapLogoutCancel();
    }

    @Test(priority = 2, description = "LO-L2: dialog title Log Out?")
    @Severity(SeverityLevel.CRITICAL)
    public void dialogTitle() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(700);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("Log Out?")).as("title").isTrue();
        softly.assertThat(page.isLogoutDialogBodyVisible()).as("body").isTrue();
        softly.assertAll();
        page.tapLogoutCancel();
    }

    @Test(priority = 3, description = "LO-L3: Cancel + Log out CTAs")
    @Severity(SeverityLevel.CRITICAL)
    public void cancelAndConfirmVisible() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(700);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isLogoutCancelVisible()).as("Cancel").isTrue();
        softly.assertThat(page.isLogoutConfirmVisible()).as("Log out confirm").isTrue();
        softly.assertAll();
        page.tapLogoutCancel();
    }

    @Test(priority = 4, description = "LO-L4: Cancel keeps session logged in")
    @Severity(SeverityLevel.CRITICAL)
    public void cancelKeepsSession() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(700);
        page.tapLogoutCancel();
        sleepQuiet(700);
        assertThat(page.isDisplayedNow() || looksStillLoggedIn()).isTrue();
        assertThat(looksLoggedOutUi()).isFalse();
    }

    @Test(priority = 5, description = "LO-L5: Confirm clears dialog")
    @Severity(SeverityLevel.BLOCKER)
    @Description("After Confirm, dialog must dismiss.")
    public void confirmClearsDialog() {
        confirmLogoutFromDrawer();
        ProfileDrawerPage page = new ProfileDrawerPage();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isLogoutDialogVisible()).as("dialog gone").isFalse();
        softly.assertAll();
        attachSessionParams("afterConfirm");
        page.attachScreenshot("lo-l5");
        if (looksLoggedOutUi()) {
            restoreSession0001();
        }
    }

    @Test(priority = 6, description = "LO-L6: Confirm must leave logged-in Home")
    @Severity(SeverityLevel.BLOCKER)
    @Description("BUG candidate: session remains after Confirm Log out.")
    public void confirmMustNotStayHome() {
        confirmLogoutFromDrawer();
        attachSessionParams("loL6");
        new ProfileDrawerPage().attachScreenshot("lo-l6");
        boolean stillIn = looksStillLoggedIn();
        boolean outUi = looksLoggedOutUi();
        Allure.parameter("bugSessionSticky", String.valueOf(stillIn && !outUi));
        // Soft product expectation: must reach auth UI. Failure = bug filed.
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(stillIn)
                .as("must NOT remain logged-in Home/drawer after Confirm")
                .isFalse();
        softly.assertThat(outUi)
                .as("must show logged-out auth UI after Confirm")
                .isTrue();
        try {
            softly.assertAll();
        } finally {
            if (looksLoggedOutUi() || !looksStillLoggedIn()) {
                try {
                    restoreSession0001();
                } catch (RuntimeException e) {
                    Allure.parameter("restoreFail", e.getClass().getSimpleName());
                }
            }
        }
    }

    @Test(priority = 7, description = "LO-L7: Cancel then Confirm sequence")
    @Severity(SeverityLevel.NORMAL)
    public void cancelThenConfirm() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(600);
        page.tapLogoutCancel();
        sleepQuiet(600);
        page.tapLogOut();
        sleepQuiet(700);
        page.tapLogoutConfirm();
        sleepQuiet(2000);
        attachSessionParams("loL7");
        Allure.parameter("bugSessionSticky", String.valueOf(looksStillLoggedIn()));
        if (looksLoggedOutUi()) {
            restoreSession0001();
        } else if (looksStillLoggedIn()) {
            // leave for edge suite / restore if stuck logged in for next tests
            Allure.parameter("stayedLoggedIn", "true");
        }
    }

    @Test(priority = 8, description = "LO-L8: package stays Vendor through Confirm")
    @Severity(SeverityLevel.NORMAL)
    public void packageThroughConfirm() {
        confirmLogoutFromDrawer();
        assertThat(vendorPackage()).contains("l2b");
        if (looksLoggedOutUi()) {
            restoreSession0001();
        }
    }
}
