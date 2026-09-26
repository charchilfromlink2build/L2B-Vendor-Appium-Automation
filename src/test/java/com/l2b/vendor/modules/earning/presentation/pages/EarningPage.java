package com.l2b.vendor.modules.earning.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Rental Earning — {@code Earning & Incentive}. Live dump
 * {@code /tmp/l2b-earning-0001-20260926} on {@code 9000000001}.
 * Bottom tabs stay (Calendar · Home · Earning · Fleet). Withdraw Money
 * confirm is never completed in automation.
 */
public class EarningPage extends SplashScreen {

    private static final Pattern RUPEE = Pattern.compile("₹\\s*[\\d,]+");

    @Override
    @Step("Wait for Earning")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Earning screen did not appear", Duration.ofSeconds(12));
    }

    @Step("Check Earning identity (Earning & Incentive or Wallet Balance)")
    public boolean isDisplayedNow() {
        return isTitleVisible() || isWalletBalanceLabelVisible();
    }

    @Step("Check Earning & Incentive title")
    public boolean isTitleVisible() {
        return isPresent(ComposeLocators.textView("Earning & Incentive"));
    }

    @Step("Check Help visible")
    public boolean isHelpVisible() {
        return isPresent(ComposeLocators.textView("Help"));
    }

    @Step("Check Wallet Balance label")
    public boolean isWalletBalanceLabelVisible() {
        return isPresent(ComposeLocators.textView("Wallet Balance"));
    }

    @Step("Check Incentive label")
    public boolean isIncentiveLabelVisible() {
        return isPresent(ComposeLocators.textView("Incentive"));
    }

    @Step("Check Withdraw action visible")
    public boolean isWithdrawVisible() {
        return isPresent(ComposeLocators.textView("Withdraw"));
    }

    @Step("Check Order stat label")
    public boolean isOrderStatVisible() {
        return isPresent(ComposeLocators.textView("Order"));
    }

    @Step("Check Sale stat label")
    public boolean isSaleStatVisible() {
        return isPresent(ComposeLocators.textView("Sale"));
    }

    @Step("Check Recent Transactions heading")
    public boolean isRecentTransactionsVisible() {
        return isPresent(ComposeLocators.textView("Recent Transactions"));
    }

    @Step("Check See all near Recent Transactions")
    public boolean isSeeAllVisible() {
        return isPresent(ComposeLocators.textView("See all"));
    }

    @Step("Check bonus FIRST label")
    public boolean isFirstBonusLabelVisible() {
        return isPresent(ComposeLocators.textView("FIRST"));
    }

    @Step("Check progress fraction like 6/10")
    public boolean isProgressFractionVisible() {
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el);
            if (t.matches("\\d+/\\d+")) {
                return true;
            }
        }
        return false;
    }

    @Step("Read progress fraction text")
    public String progressFractionText() {
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el).trim();
            if (t.matches("\\d+/\\d+")) {
                return t;
            }
        }
        return "";
    }

    @Step("Read progress percent text")
    public String progressPercentText() {
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el).trim();
            if (t.matches("\\d+%")) {
                return t;
            }
        }
        return "";
    }

    @Step("Visible Wallet Balance rupee near label")
    public String walletBalanceAmount() {
        return nearestRupeeAfterLabel("Wallet Balance");
    }

    @Step("Visible Incentive rupee")
    public String incentiveAmount() {
        return nearestRupeeAfterLabel("Incentive");
    }

    @Step("Visible Sale rupee")
    public String saleAmount() {
        return nearestRupeeAfterLabel("Sale");
    }

    @Step("Check rental bottom tabs Calendar·Home·Earning·Fleet")
    public boolean isRentalBottomTabsVisible() {
        return isPresent(By.xpath("//*[@content-desc='Calendar']"))
                && isPresent(By.xpath("//*[@content-desc='Home']"))
                && isPresent(By.xpath("//*[@content-desc='Earning']"))
                && isPresent(By.xpath("//*[@content-desc='Fleet']"));
    }

    @Step("Check Debited label on list")
    public boolean hasDebitedLabel() {
        return isPresent(ComposeLocators.textView("Debited"));
    }

    @Step("Check Credited label on list")
    public boolean hasCreditedLabel() {
        return isPresent(ComposeLocators.textView("Credited"));
    }

    @Step("Count visible transaction-like ids (L2B- / MDL-)")
    public int visibleTransactionIdCount() {
        int n = 0;
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el);
            if (t.startsWith("L2B-") || t.startsWith("MDL-") || t.startsWith("Earnings")) {
                n++;
            }
        }
        return n;
    }

    @Step("Check Withdraw Money form")
    public boolean isWithdrawFormVisible() {
        return isPresent(ComposeLocators.textView("Withdraw Money"))
                && (isPresent(ComposeLocators.textView("Enter Amount"))
                || isPresent(ComposeLocators.textViewContains("Enter Amount")));
    }

    @Step("Check Help & Support surface")
    public boolean isHelpSupportVisible() {
        return isPresent(ComposeLocators.textView("Help & Support"))
                || isPresent(ComposeLocators.textViewContains("ready to help"));
    }

    @Step("Check amount chip visible")
    public boolean isAmountChipVisible(String chip) {
        return isPresent(ComposeLocators.textView(chip));
    }

    @Step("Check Withdraw form Note bullets")
    public boolean hasWithdrawNotes() {
        return isPresent(ComposeLocators.textView("Note"))
                || visibleTexts().stream().anyMatch(t -> t.contains("L2B does not charge"));
    }

    @Step("Tap Help")
    public void tapHelp() {
        tapTextOrParent("Help");
    }

    @Step("Tap Withdraw")
    public void tapWithdraw() {
        tapTextOrParent("Withdraw");
    }

    @Step("Tap See all")
    public void tapSeeAll() {
        tapTextOrParent("See all");
    }

    @Step("Tap header Back")
    public void tapHeaderBack() {
        if (isPresent(By.xpath("//*[@content-desc='Back']"))) {
            driver.findElement(By.xpath("//*[@content-desc='Back']")).click();
            return;
        }
        driver.navigate().back();
    }

    @Step("Tap Home bottom tab")
    public void tapHomeTab() {
        driver.findElement(By.xpath("//*[@content-desc='Home']")).click();
    }

    @Step("Tap Fleet bottom tab")
    public void tapFleetTab() {
        driver.findElement(By.xpath("//*[@content-desc='Fleet']")).click();
    }

    @Step("Tap Calendar bottom tab")
    public void tapCalendarTab() {
        driver.findElement(By.xpath("//*[@content-desc='Calendar']")).click();
    }

    @Step("Tap Earning bottom tab")
    public void tapEarningTab() {
        driver.findElement(By.xpath("//*[@content-desc='Earning']")).click();
    }

    @Step("Tap amount chip on Withdraw form")
    public void tapAmountChip(String chip) {
        tapTextOrParent(chip);
    }

    @Step("Type withdraw amount into EditText")
    public void typeWithdrawAmount(String amount) {
        List<WebElement> fields = driver.findElements(By.className("android.widget.EditText"));
        if (fields.isEmpty()) {
            throw new IllegalStateException("No EditText on Withdraw form");
        }
        WebElement field = fields.get(0);
        field.click();
        field.clear();
        field.sendKeys(amount);
    }

    @Step("Read withdraw EditText text")
    public String withdrawAmountText() {
        List<WebElement> fields = driver.findElements(By.className("android.widget.EditText"));
        if (fields.isEmpty()) {
            return "";
        }
        String t = fields.get(0).getAttribute("text");
        return t == null || "null".equalsIgnoreCase(t) ? "" : t;
    }

    @Step("Tap Withdraw Money CTA (probe only — may show validation)")
    public void tapWithdrawMoneyCta() {
        List<WebElement> labels = driver.findElements(ComposeLocators.textView("Withdraw Money"));
        if (labels.isEmpty()) {
            throw new IllegalStateException("Withdraw Money CTA not found");
        }
        // Prefer the bottom CTA (higher Y)
        WebElement best = labels.get(0);
        int bestY = -1;
        for (WebElement el : labels) {
            Rectangle r = el.getRect();
            if (r.y > bestY) {
                bestY = r.y;
                best = el;
            }
        }
        try {
            clickGestureOn(best.findElement(By.xpath("./..")));
        } catch (RuntimeException e) {
            clickGestureOn(best);
        }
    }

    @Step("Swipe Earning list up")
    public void swipeListUp() {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1600));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(350),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 700));
        swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(swipe));
    }

    @Step("Visible TextView texts snapshot")
    public Set<String> visibleTexts() {
        Set<String> out = new LinkedHashSet<>();
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el).trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    @Step("List visible rupee strings")
    public List<String> visibleRupeeAmounts() {
        List<String> out = new ArrayList<>();
        for (String t : visibleTexts()) {
            Matcher m = RUPEE.matcher(t);
            if (m.find()) {
                out.add(t.trim());
            }
        }
        return out;
    }

    private String nearestRupeeAfterLabel(String label) {
        List<WebElement> nodes = driver.findElements(By.className("android.widget.TextView"));
        int labelY = -1;
        int labelX = -1;
        for (WebElement el : nodes) {
            if (label.equals(safeText(el).trim())) {
                Rectangle r = el.getRect();
                labelY = r.y;
                labelX = r.x;
                break;
            }
        }
        String best = "";
        int bestScore = Integer.MAX_VALUE;
        for (WebElement el : nodes) {
            String t = safeText(el).trim();
            if (!RUPEE.matcher(t).find()) {
                continue;
            }
            Rectangle r = el.getRect();
            // Prefer rupee just below the label (Wallet Balance → big amount, not Incentive ₹ 0).
            int dy = r.y - labelY;
            if (labelY >= 0 && (dy < -20 || dy > 220)) {
                continue;
            }
            int dx = Math.abs(r.x - labelX);
            int score = (labelY < 0 ? 0 : Math.abs(dy) * 10 + dx);
            // Prefer larger amounts when scores tie (avoid Incentive ₹ 0).
            long digits = t.replaceAll("[^0-9]", "").isEmpty() ? 0
                    : Long.parseLong(t.replaceAll("[^0-9]", ""));
            int amountPenalty = digits == 0 ? 50_000 : 0;
            score += amountPenalty;
            if (score < bestScore) {
                bestScore = score;
                best = t;
            }
        }
        return best;
    }

    private void tapTextOrParent(String text) {
        List<WebElement> outers = driver.findElements(ComposeLocators.clickableWithText(text));
        if (!outers.isEmpty()) {
            clickGestureOn(outers.get(0));
            return;
        }
        List<WebElement> labels = driver.findElements(ComposeLocators.textView(text));
        if (labels.isEmpty()) {
            throw new IllegalStateException("Text not found: " + text);
        }
        try {
            clickGestureOn(labels.get(0).findElement(By.xpath("./..")));
        } catch (RuntimeException e) {
            clickGestureOn(labels.get(0));
        }
    }

    private void clickGestureOn(WebElement el) {
        Rectangle r = el.getRect();
        int x = r.x + Math.max(1, r.width / 2);
        int y = r.y + Math.max(1, r.height / 2);
        driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    private static String safeText(WebElement el) {
        try {
            String t = el.getAttribute("text");
            return t == null || "null".equalsIgnoreCase(t) ? "" : t;
        } catch (RuntimeException e) {
            return "";
        }
    }
}
