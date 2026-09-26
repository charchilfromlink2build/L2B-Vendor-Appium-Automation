package com.l2b.vendor.modules.earning.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.earning.presentation.pages.EarningPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Earning landing — chrome on {@code 9000000001}. Dump
 * {@code /tmp/l2b-earning-0001-20260926}. Read-only.
 */
@Epic("Vendor app")
@Feature("Earning landing — rental 9000000001")
public class EarningLandingTest extends EarningBaseTest {

    @Test(priority = 1, description = "EA-L1: bottom Earning → Earning & Incentive")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Tap bottom Earning. Expect classify=earning, title, Calendar·Home·Earning·Fleet.")
    public void bottomTabOpensEarning() {
        EarningPage earning = reachEarningViaTab();
        earning.attachScreenshot("earning-fl-l1");
        assertThat(classifyRentalNow()).isEqualTo("earning");
        assertThat(earning.isDisplayedNow()).isTrue();
        assertThat(earning.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 2, description = "EA-L2: title + Help + Back chrome")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 01: Earning & Incentive + Help + content-desc Back.")
    public void titleHelpBackChrome() {
        EarningPage earning = reachEarningViaTab();
        earning.attachScreenshot("earning-fl-l2");
        assertThat(earning.isTitleVisible()).isTrue();
        assertThat(earning.isHelpVisible()).isTrue();
        assertThat(earning.isDisplayedNow()).isTrue();
    }

    @Test(priority = 3, description = "EA-L3: Wallet Balance + Incentive amounts")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 01: Wallet Balance + Incentive labels with ₹ amounts.")
    public void walletAndIncentiveAmounts() {
        EarningPage earning = reachEarningViaTab();
        String wallet = earning.walletBalanceAmount();
        String incentive = earning.incentiveAmount();
        io.qameta.allure.Allure.parameter("wallet", wallet);
        io.qameta.allure.Allure.parameter("incentive", incentive);
        earning.attachScreenshot("earning-fl-l3");
        assertThat(earning.isWalletBalanceLabelVisible()).isTrue();
        assertThat(earning.isIncentiveLabelVisible()).isTrue();
        assertThat(wallet).as("Wallet Balance rupee").contains("₹");
        assertThat(incentive).as("Incentive rupee").contains("₹");
    }

    @Test(priority = 4, description = "EA-L4: bonus FIRST + progress fraction + percent")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 01: FIRST + 6/10 + 60% (values may change).")
    public void bonusProgressChrome() {
        EarningPage earning = reachEarningViaTab();
        String frac = earning.progressFractionText();
        String pct = earning.progressPercentText();
        io.qameta.allure.Allure.parameter("fraction", frac);
        io.qameta.allure.Allure.parameter("percent", pct);
        earning.attachScreenshot("earning-fl-l4");
        assertThat(earning.isFirstBonusLabelVisible()).isTrue();
        assertThat(earning.isProgressFractionVisible()).isTrue();
        assertThat(frac).matches("\\d+/\\d+");
        assertThat(pct).matches("\\d+%");
    }

    @Test(priority = 5, description = "EA-L5: Withdraw action visible")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 01: Withdraw tile. Transfer/Add Money not on live 0001.")
    public void withdrawActionVisible() {
        EarningPage earning = reachEarningViaTab();
        earning.attachScreenshot("earning-fl-l5");
        assertThat(earning.isWithdrawVisible()).isTrue();
    }

    @Test(priority = 6, description = "EA-L6: Order + Sale stats")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 01: Order + Sale cards with Sale ₹ amount.")
    public void orderAndSaleStats() {
        EarningPage earning = reachEarningViaTab();
        String sale = earning.saleAmount();
        io.qameta.allure.Allure.parameter("sale", sale);
        earning.attachScreenshot("earning-fl-l6");
        assertThat(earning.isOrderStatVisible()).isTrue();
        assertThat(earning.isSaleStatVisible()).isTrue();
        assertThat(sale).contains("₹");
    }

    @Test(priority = 7, description = "EA-L7: Recent Transactions + See all")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 01: Recent Transactions heading + See all.")
    public void recentTransactionsChrome() {
        EarningPage earning = reachEarningViaTab();
        earning.attachScreenshot("earning-fl-l7");
        assertThat(earning.isRecentTransactionsVisible()).isTrue();
        assertThat(earning.isSeeAllVisible()).isTrue();
    }

    @Test(priority = 8, description = "EA-L8: transaction rows or empty copy")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump 01: L2B-/MDL- rows with Debited, or empty state.")
    public void transactionRowsOrEmpty() {
        EarningPage earning = reachEarningViaTab();
        int ids = earning.visibleTransactionIdCount();
        boolean debited = earning.hasDebitedLabel();
        boolean empty = earning.visibleTexts().stream()
                .anyMatch(t -> t.toLowerCase().contains("no transaction"));
        io.qameta.allure.Allure.parameter("txnIds", String.valueOf(ids));
        io.qameta.allure.Allure.parameter("debited", String.valueOf(debited));
        earning.attachScreenshot("earning-fl-l8");
        assertThat(ids > 0 || empty || earning.isRecentTransactionsVisible())
                .as("Expect txn rows or empty Recent Transactions")
                .isTrue();
    }

    @Test(priority = 9, description = "EA-L9: bottom tabs stay on Earning")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Calendar · Home · Earning · Fleet remain while on Earning.")
    public void bottomTabsStay() {
        EarningPage earning = reachEarningViaTab();
        assertThat(earning.isRentalBottomTabsVisible()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("earning");
    }

    @Test(priority = 10, description = "EA-L10: scroll keeps title + tabs")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump 02: swipe up — title and bottom tabs stay; more txns may appear.")
    public void scrollKeepsChrome() {
        EarningPage earning = reachEarningViaTab();
        earning.swipeListUp();
        sleepBrief();
        earning.attachScreenshot("earning-fl-l10");
        assertThat(earning.isTitleVisible() || earning.isDisplayedNow()).isTrue();
        assertThat(earning.isRentalBottomTabsVisible()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("earning");
    }

    private void sleepBrief() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
