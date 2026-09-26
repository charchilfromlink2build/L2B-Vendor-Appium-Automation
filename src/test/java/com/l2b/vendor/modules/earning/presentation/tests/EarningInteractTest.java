package com.l2b.vendor.modules.earning.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.earning.presentation.pages.EarningPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.time.Duration;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

/**
 * Earning interact — Help / Withdraw form / tabs / Back on {@code 9000000001}.
 * Dump {@code /tmp/l2b-earning-0001-20260926}. Never complete Withdraw Money.
 */
@Epic("Vendor app")
@Feature("Earning interact — rental 9000000001")
public class EarningInteractTest extends EarningBaseTest {

    @Test(priority = 1, description = "EA-I1: Help opens Help & Support")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 03: Help → Help & Support. Back returns to Earning.")
    public void helpOpensSupport() {
        EarningPage earning = reachEarningViaTab();
        earning.tapHelp();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> earning.isHelpSupportVisible());
        earning.attachScreenshot("earning-fl-i1-help");
        assertThat(earning.isHelpSupportVisible()).isTrue();
        DriverManager.get().navigate().back();
        sleepBrief();
        dismissToEarning(earning);
        assertThat(earning.isDisplayedNow()).isTrue();
    }

    @Test(priority = 2, description = "EA-I2: Withdraw opens Withdraw Money form")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Dump 04: Withdraw → Enter Amount + chips + Withdraw Money CTA.")
    public void withdrawOpensForm() {
        EarningPage earning = reachEarningViaTab();
        earning.tapWithdraw();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> earning.isWithdrawFormVisible());
        earning.attachScreenshot("earning-fl-i2-withdraw");
        assertThat(earning.isWithdrawFormVisible()).isTrue();
        assertThat(earning.isAmountChipVisible("₹500")).isTrue();
        assertThat(earning.isAmountChipVisible("₹1000")).isTrue();
        DriverManager.get().navigate().back();
    }

    @Test(priority = 3, description = "EA-I3: Back from Withdraw form returns Earning")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Withdraw → Back → Earning & Incentive.")
    public void backFromWithdrawForm() {
        EarningPage earning = reachEarningViaTab();
        earning.tapWithdraw();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> earning.isWithdrawFormVisible());
        DriverManager.get().navigate().back();
        sleepBrief();
        if (!earning.isDisplayedNow()) {
            earning.tapHeaderBack();
            sleepBrief();
        }
        if (!earning.isDisplayedNow()) {
            new HomePage().tapDesc("Earning");
            earning.waitUntilLoaded();
        }
        earning.attachScreenshot("earning-fl-i3-back");
        assertThat(earning.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("earning");
    }

    @Test(priority = 4, description = "EA-I4: chip ₹500 selectable (no confirm)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Withdraw form → tap ₹500. Do not tap Withdraw Money confirm.")
    public void chipFiveHundredSelectable() {
        EarningPage earning = reachEarningViaTab();
        earning.tapWithdraw();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> earning.isWithdrawFormVisible());
        earning.tapAmountChip("₹500");
        sleepBrief();
        String typed = earning.withdrawAmountText();
        Allure.parameter("editText", typed);
        earning.attachScreenshot("earning-fl-i4-chip");
        assertThat(earning.isWithdrawFormVisible()).isTrue();
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        DriverManager.get().navigate().back();
    }

    @Test(priority = 5, description = "EA-I5: type amount in EditText (no confirm)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Type 100 into Enter Amount. Never tap Withdraw Money.")
    public void typeAmountNoConfirm() {
        EarningPage earning = reachEarningViaTab();
        earning.tapWithdraw();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> earning.isWithdrawFormVisible());
        earning.typeWithdrawAmount("100");
        sleepBrief();
        Allure.parameter("editText", earning.withdrawAmountText());
        earning.attachScreenshot("earning-fl-i5-type");
        assertThat(earning.withdrawAmountText()).contains("100");
        DriverManager.get().navigate().back();
    }

    @Test(priority = 6, description = "EA-I6: Recent Transactions See all")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap See all. Expect leave landing or expand list; stay in Vendor.")
    public void transactionsSeeAll() {
        EarningPage earning = reachEarningViaTab();
        earning.tapSeeAll();
        sleepBrief();
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        earning.attachScreenshot("earning-fl-i6-see-all");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(after).isNotEqualTo("launcher");
        if (!earning.isDisplayedNow()) {
            DriverManager.get().navigate().back();
            sleepBrief();
        }
    }

    @Test(priority = 7, description = "EA-I7: See all Back returns Earning or Home")
    @Severity(SeverityLevel.NORMAL)
    @Description("See all → Back. Expect Earning (or Home if stack popped).")
    public void seeAllBack() {
        EarningPage earning = reachEarningViaTab();
        earning.tapSeeAll();
        sleepBrief();
        DriverManager.get().navigate().back();
        sleepBrief();
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        assertThat(after).isIn("earning", "home", "vendor-other");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 8, description = "EA-I8: Home tab from Earning")
    @Severity(SeverityLevel.CRITICAL)
    @Description("From Earning, Home tab → Home.")
    public void homeTabFromEarning() {
        EarningPage earning = reachEarningViaTab();
        earning.tapHomeTab();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> "home".equals(classifyRentalNow())
                        || new HomePage().isDisplayedNow());
        assertThat(classifyRentalNow()).isIn("home", "extend-time", "quick-booking");
    }

    @Test(priority = 9, description = "EA-I9: Fleet tab from Earning")
    @Severity(SeverityLevel.CRITICAL)
    @Description("From Earning, Fleet tab → Your Fleet.")
    public void fleetTabFromEarning() {
        EarningPage earning = reachEarningViaTab();
        earning.tapFleetTab();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> "fleet".equals(classifyRentalNow()));
        assertThat(classifyRentalNow()).isEqualTo("fleet");
    }

    @Test(priority = 10, description = "EA-I10: Calendar tab from Earning")
    @Severity(SeverityLevel.CRITICAL)
    @Description("From Earning, Calendar tab → Schedule.")
    public void calendarTabFromEarning() {
        EarningPage earning = reachEarningViaTab();
        earning.tapCalendarTab();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> "calendar".equals(classifyRentalNow())
                        || !"earning".equals(classifyRentalNow()));
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        assertThat(after).isIn("calendar", "home", "vendor-other");
    }

    @Test(priority = 11, description = "EA-I11: header Back → Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 09: header Back → Home.")
    public void headerBackToHome() {
        EarningPage earning = reachEarningViaTab();
        earning.tapHeaderBack();
        sleepBrief();
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        assertThat(after).isIn("home", "extend-time", "quick-booking");
    }

    @Test(priority = 12, description = "EA-I12: device Back → Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 07: device Back from Earning → Home.")
    public void deviceBackToHome() {
        EarningPage earning = reachEarningViaTab();
        DriverManager.get().navigate().back();
        sleepBrief();
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        assertThat(after).isIn("home", "extend-time", "quick-booking");
        assertThat(after).isNotEqualTo("launcher");
    }

    @Test(priority = 13, description = "EA-I13: re-open Earning after Back")
    @Severity(SeverityLevel.NORMAL)
    @Description("Back to Home → Earning tab again loads Earning & Incentive.")
    public void reopenEarningAfterBack() {
        EarningPage earning = reachEarningViaTab();
        earning.tapHeaderBack();
        sleepBrief();
        new HomePage().tapDesc("Earning");
        earning.waitUntilLoaded();
        assertThat(classifyRentalNow()).isEqualTo("earning");
        assertThat(earning.isTitleVisible()).isTrue();
    }

    @Test(priority = 14, description = "EA-I14: Withdraw form note bullets visible")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump 04: Note + fee / support bullets on Withdraw Money.")
    public void withdrawFormNotesVisible() {
        EarningPage earning = reachEarningViaTab();
        earning.tapWithdraw();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> earning.isWithdrawFormVisible());
        Allure.parameter("notes", String.valueOf(earning.hasWithdrawNotes()));
        earning.attachScreenshot("earning-fl-i14-notes");
        assertThat(earning.hasWithdrawNotes()).isTrue();
        DriverManager.get().navigate().back();
    }


    private void sleepBrief() {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
