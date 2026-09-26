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
 * Earning edge — validation / consistency / stress on {@code 9000000001}.
 * Dump {@code /tmp/l2b-earning-0001-20260926}. Never complete Withdraw Money.
 */
@Epic("Vendor app")
@Feature("Earning edge — rental 9000000001")
public class EarningEdgeTest extends EarningBaseTest {

    @Test(priority = 1, description = "EA-E1: empty Withdraw Money CTA stays in Vendor")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Open Withdraw, leave amount empty, tap Withdraw Money CTA. Expect stay in "
            + "Vendor (validation or form) — never launcher. Do not complete a real withdraw.")
    public void emptyWithdrawCtaStaysInVendor() {
        EarningPage earning = reachEarningViaTab();
        earning.tapWithdraw();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> earning.isWithdrawFormVisible());
        earning.tapWithdrawMoneyCta();
        sleepBrief();
        String after = classifyRentalNow();
        String pkg = vendorPackage();
        Allure.parameter("after", after);
        Allure.parameter("package", pkg);
        earning.attachScreenshot("earning-fl-e1-empty-cta");
        assertThat(pkg).isEqualTo(Config.get("app.package"));
        assertThat(after).isNotEqualTo("launcher");
        assertThat(earning.isWithdrawFormVisible() || earning.isDisplayedNow()
                || "vendor-other".equals(after) || "earning".equals(after))
                .as("Empty CTA must not leave Vendor")
                .isTrue();
        dismissToEarning(earning);
    }

    @Test(priority = 2, description = "EA-E2: type 0 amount (no confirm)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Type 0 into amount. Field accepts typing; do not confirm withdraw.")
    public void typeZeroAmount() {
        EarningPage earning = openWithdrawForm();
        earning.typeWithdrawAmount("0");
        sleepBrief();
        Allure.parameter("editText", earning.withdrawAmountText());
        earning.attachScreenshot("earning-fl-e2-zero");
        assertThat(earning.withdrawAmountText()).contains("0");
        assertThat(earning.isWithdrawFormVisible()).isTrue();
        DriverManager.get().navigate().back();
    }

    @Test(priority = 3, description = "EA-E3: type amount larger than wallet (no confirm)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Type 99999999. Stay on form — never confirm. Record edit text.")
    public void typeHugeAmountNoConfirm() {
        EarningPage earning = openWithdrawForm();
        earning.typeWithdrawAmount("99999999");
        sleepBrief();
        Allure.parameter("editText", earning.withdrawAmountText());
        earning.attachScreenshot("earning-fl-e3-huge");
        assertThat(earning.isWithdrawFormVisible()).isTrue();
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        DriverManager.get().navigate().back();
    }

    @Test(priority = 4, description = "EA-E4: double-tap Withdraw opens one form")
    @Severity(SeverityLevel.NORMAL)
    @Description("Double-tap Withdraw quickly. Expect one Withdraw Money form, no crash.")
    public void doubleTapWithdraw() {
        EarningPage earning = reachEarningViaTab();
        earning.tapWithdraw();
        try {
            earning.tapWithdraw();
        } catch (RuntimeException ignored) {
            // second tap may miss if form already open
        }
        sleepBrief();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> earning.isWithdrawFormVisible());
        earning.attachScreenshot("earning-fl-e4-double");
        assertThat(earning.isWithdrawFormVisible()).isTrue();
        DriverManager.get().navigate().back();
    }

    @Test(priority = 5, description = "EA-E5: rapid Help open/close")
    @Severity(SeverityLevel.NORMAL)
    @Description("Help → Back → Help again. Stay in Vendor; end on Earning or Help.")
    public void rapidHelpOpenClose() {
        EarningPage earning = reachEarningViaTab();
        earning.tapHelp();
        sleepBrief();
        DriverManager.get().navigate().back();
        sleepBrief();
        dismissToEarning(earning);
        assertThat(earning.isDisplayedNow()).isTrue();
        earning.tapHelp();
        sleepBrief();
        Allure.parameter("after", classifyRentalNow());
        earning.attachScreenshot("earning-fl-e5-help");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(earning.isHelpSupportVisible() || earning.isDisplayedNow()).isTrue();
        DriverManager.get().navigate().back();
        dismissToEarning(earning);
    }

    @Test(priority = 6, description = "EA-E6: Wallet Indian grouping vs Home Western")
    @Severity(SeverityLevel.NORMAL)
    @Description("Home Current Earning uses Western thousands (₹ 152,453); Earning Wallet "
            + "uses Indian (₹ 1,52,452) on dump. Record both — candidate #24 family.")
    public void walletGroupingVsHome() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        String homeAmt = home.currentEarningAmountNow();
        home.tapDesc("Earning");
        EarningPage earning = new EarningPage();
        earning.waitUntilLoaded();
        String wallet = earning.walletBalanceAmount();
        Allure.parameter("homeCurrentEarning", homeAmt);
        Allure.parameter("walletBalance", wallet);
        boolean homeWestern = homeAmt.matches(".*\\d{1,3}(,\\d{3})+.*")
                && !homeAmt.matches(".*\\d{1,2},\\d{2},\\d{3}.*");
        boolean walletIndian = wallet.matches(".*\\d{1,2},\\d{2},\\d{3}.*");
        Allure.parameter("homeLooksWestern", String.valueOf(homeWestern));
        Allure.parameter("walletLooksIndian", String.valueOf(walletIndian));
        earning.attachScreenshot("earning-fl-e6-grouping");
        assertThat(homeAmt).contains("₹");
        assertThat(wallet).contains("₹");
        if (homeWestern && walletIndian) {
            Allure.parameter("bug", "Earning Wallet Indian grouping vs Home Western "
                    + "(#24 family) — inconsistent rupee format across surfaces");
        }
        // Soft documentation assert: both surfaces must show a rupee; inconsistency logged.
        assertThat(earning.isDisplayedNow()).isTrue();
    }

    @Test(priority = 7, description = "EA-E7: progress fraction matches percent")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 01: 6/10 → 60%. Assert round(100*n/d) == percent when parseable.")
    public void progressFractionMatchesPercent() {
        EarningPage earning = reachEarningViaTab();
        String frac = earning.progressFractionText();
        String pct = earning.progressPercentText();
        Allure.parameter("fraction", frac);
        Allure.parameter("percent", pct);
        assertThat(frac).matches("\\d+/\\d+");
        assertThat(pct).matches("\\d+%");
        String[] parts = frac.split("/");
        int n = Integer.parseInt(parts[0]);
        int d = Integer.parseInt(parts[1]);
        int shown = Integer.parseInt(pct.replace("%", ""));
        int expected = d == 0 ? 0 : (int) Math.round(100.0 * n / d);
        Allure.parameter("expectedPct", String.valueOf(expected));
        assertThat(shown)
                .as("Progress % should match fraction %s", frac)
                .isEqualTo(expected);
    }

    @Test(priority = 8, description = "EA-E8: scroll reveals Credited and/or Debited")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump 02: after swipe, Debited and Credited labels appear.")
    public void scrollRevealsCreditDebit() {
        EarningPage earning = reachEarningViaTab();
        boolean debited = earning.hasDebitedLabel();
        boolean credited = earning.hasCreditedLabel();
        for (int i = 0; i < 3 && !(debited && credited); i++) {
            earning.swipeListUp();
            sleepBrief();
            debited = debited || earning.hasDebitedLabel();
            credited = credited || earning.hasCreditedLabel();
        }
        Allure.parameter("debited", String.valueOf(debited));
        Allure.parameter("credited", String.valueOf(credited));
        earning.attachScreenshot("earning-fl-e8-scroll");
        assertThat(debited || credited || earning.visibleTransactionIdCount() > 0)
                .as("Expect Debited/Credited or txn ids after scroll")
                .isTrue();
    }

    @Test(priority = 9, description = "EA-E9: all amount chips present on Withdraw")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 04: ₹500 ₹1000 ₹2000 ₹5000 chips.")
    public void allAmountChipsPresent() {
        EarningPage earning = openWithdrawForm();
        for (String chip : new String[] {"₹500", "₹1000", "₹2000", "₹5000"}) {
            assertThat(earning.isAmountChipVisible(chip)).as(chip).isTrue();
        }
        earning.attachScreenshot("earning-fl-e9-chips");
        DriverManager.get().navigate().back();
    }

    @Test(priority = 10, description = "EA-E10: chip then type overwrites or keeps form usable")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap ₹1000 then type 250. Form stays; no confirm.")
    public void chipThenType() {
        EarningPage earning = openWithdrawForm();
        earning.tapAmountChip("₹1000");
        sleepBrief();
        earning.typeWithdrawAmount("250");
        sleepBrief();
        Allure.parameter("editText", earning.withdrawAmountText());
        assertThat(earning.isWithdrawFormVisible()).isTrue();
        assertThat(earning.withdrawAmountText()).contains("250");
        DriverManager.get().navigate().back();
    }

    @Test(priority = 11, description = "EA-E11: Withdraw form Help stays in Vendor")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump 04 has Help on Withdraw Money. Tap Help — stay in Vendor.")
    public void withdrawFormHelp() {
        EarningPage earning = openWithdrawForm();
        if (earning.isHelpVisible()) {
            earning.tapHelp();
            sleepBrief();
        }
        Allure.parameter("after", classifyRentalNow());
        earning.attachScreenshot("earning-fl-e11-form-help");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
        dismissToEarning(earning);
    }

    @Test(priority = 12, description = "EA-E12: device Back on Withdraw form → Earning")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Withdraw form → device Back → Earning (not launcher).")
    public void backOnWithdrawFormNotLauncher() {
        EarningPage earning = openWithdrawForm();
        DriverManager.get().navigate().back();
        sleepBrief();
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        assertThat(after).isNotEqualTo("launcher");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(after).isIn("earning", "home", "vendor-other");
    }

    private EarningPage openWithdrawForm() {
        EarningPage earning = reachEarningViaTab();
        earning.tapWithdraw();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> earning.isWithdrawFormVisible());
        assertThat(earning.isWithdrawFormVisible()).isTrue();
        return earning;
    }

    private void sleepBrief() {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
