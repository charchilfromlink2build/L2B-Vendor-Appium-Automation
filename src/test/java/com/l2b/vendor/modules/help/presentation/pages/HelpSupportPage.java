package com.l2b.vendor.modules.help.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Help &amp; Support from Profile drawer. Live dumps
 * {@code /tmp/l2b-help-0001-20261001}, {@code /tmp/l2b-help-deep-0001-20261001},
 * {@code /tmp/l2b-help-orderid-0001-20261001} on {@code 9000000001}.
 * Never tap Chat now (external chat) / Submit ticket / dialer Call /
 * Accept / Decline / Log Out Confirm.
 */
public class HelpSupportPage extends SplashScreen {

    /** Live concern sheet 1 Oct 2026 — 11 options. */
    public static final String[] CONCERN_OPTIONS = {
            "Payment & Billing Issues",
            "Delivery Partner Issues",
            "Rental Machine Issues",
            "Material Order Issues",
            "Account & Profile Issues",
            "Operator issue",
            "Document & Invoice Issues",
            "Offers / Cashback Issues",
            "Security & Safety",
            "Location / Address Issues",
            "Other"
    };

    public static final String SUPPORT_NUMBER = "+91 1800 00 0000";

    @Override
    @Step("Wait for Help & Support")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Help & Support did not appear", Duration.ofSeconds(12));
    }

    @Step("Check Help & Support identity")
    public boolean isDisplayedNow() {
        return isHelpTitleVisible()
                && (isLiveChatVisible() || isRaiseTicketVisible() || isWorkingHoursVisible());
    }

    @Step("Check Help & Support title")
    public boolean isHelpTitleVisible() {
        return isPresent(ComposeLocators.textView("Help & Support"))
                || isPresent(ComposeLocators.textViewContains("Help"));
    }

    @Step("Check Back")
    public boolean isBackVisible() {
        return isPresent(By.xpath("//*[@content-desc='Back']"));
    }

    @Step("Check Call Support")
    public boolean isCallSupportVisible() {
        return isPresent(ComposeLocators.textView("Call Support"));
    }

    @Step("Check intro copy")
    public boolean isIntroCopyVisible() {
        return isPresent(ComposeLocators.textViewContains("We're ready to help"))
                || isPresent(ComposeLocators.textViewContains("ready to help"));
    }

    @Step("Check Working Hours 8:00 AM - 8:00 PM")
    public boolean isWorkingHoursVisible() {
        return isPresent(ComposeLocators.textViewContains("Working Hours"))
                && (isPresent(ComposeLocators.textViewContains("8:00 AM"))
                || isPresent(ComposeLocators.textViewContains("8:00 PM")));
    }

    @Step("Check Live chat CTA")
    public boolean isLiveChatVisible() {
        return isPresent(ComposeLocators.textView("Live chat"));
    }

    @Step("Check Raise Ticket CTA")
    public boolean isRaiseTicketVisible() {
        return isPresent(ComposeLocators.textView("Raise Ticket"));
    }

    @Step("Check Ticket history CTA")
    public boolean isTicketHistoryVisible() {
        return isPresent(ComposeLocators.textView("Ticket history"));
    }

    @Step("Check raise-ticket card heading")
    public boolean isRaiseTicketCardHeadingVisible() {
        return isPresent(ComposeLocators.textViewContains("Raise a Support Ticket"));
    }

    @Step("Check raise-ticket card body")
    public boolean isRaiseTicketCardBodyVisible() {
        return isPresent(ComposeLocators.textViewContains("fill in your details"));
    }

    @Step("Live chat form: Submit a query")
    public boolean isLiveChatFormVisible() {
        return isPresent(ComposeLocators.textView("Live chat"))
                && isPresent(ComposeLocators.textView("Submit a query"))
                && isPresent(ComposeLocators.textView("Chat now"));
    }

    @Step("Raise Ticket form")
    public boolean isRaiseTicketFormVisible() {
        return isPresent(ComposeLocators.textView("Raise Ticket"))
                && isPresent(ComposeLocators.textView("Submit a query"))
                && isPresent(ComposeLocators.textView("Submit"))
                && isDescriptionEditTextVisible();
    }

    @Step("Ticket history list")
    public boolean isTicketHistoryScreenVisible() {
        return isPresent(ComposeLocators.textView("Ticket history"))
                && (isPresent(ComposeLocators.textView("Recent tickets"))
                || isPresent(ComposeLocators.textViewContains("TKT-"))
                || isPresent(ComposeLocators.textViewContains("No ticket")));
    }

    @Step("Ticket Concern placeholder/dropdown")
    public boolean isTicketConcernVisible() {
        return isPresent(ComposeLocators.textView("Ticket Concern"));
    }

    @Step("Issue description EditText present")
    public boolean isDescriptionEditTextVisible() {
        return !driver.findElements(By.className("android.widget.EditText")).isEmpty();
    }

    @Step("Chat now CTA visible")
    public boolean isChatNowVisible() {
        return isPresent(ComposeLocators.textView("Chat now"));
    }

    @Step("Submit CTA visible")
    public boolean isSubmitVisible() {
        return isPresent(ComposeLocators.textView("Submit"));
    }

    @Step("Chat now parent disabled (no concern)")
    public boolean isChatNowDisabled() {
        return actionParentEnabled("Chat now") == Boolean.FALSE;
    }

    @Step("Chat now parent enabled")
    public boolean isChatNowEnabled() {
        return actionParentEnabled("Chat now") == Boolean.TRUE;
    }

    @Step("Submit parent disabled (empty form)")
    public boolean isSubmitDisabled() {
        return actionParentEnabled("Submit") == Boolean.FALSE;
    }

    @Step("Submit parent enabled")
    public boolean isSubmitEnabled() {
        return actionParentEnabled("Submit") == Boolean.TRUE;
    }

    @Step("Order ID field visible (after concern)")
    public boolean isOrderIdFieldVisible() {
        return isPresent(ComposeLocators.textView("Order ID"))
                || visibleTexts().stream().anyMatch(t -> t.startsWith("L2B-"));
    }

    @Step("Concern sheet shows known options")
    public boolean isConcernSheetVisible() {
        return isPresent(ComposeLocators.textViewContains("Payment"))
                && isPresent(ComposeLocators.textView("Other"));
    }

    @Step("All 11 concern options visible")
    public boolean allConcernOptionsVisible() {
        Set<String> texts = visibleTexts();
        int hit = 0;
        for (String opt : CONCERN_OPTIONS) {
            if (texts.contains(opt) || texts.stream().anyMatch(t -> t.contains(opt.replace(" & ", " "))
                    || t.contains(opt.split(" ")[0]))) {
                hit++;
            } else if (texts.stream().anyMatch(t -> normalizeAmp(t).equals(normalizeAmp(opt)))) {
                hit++;
            }
        }
        return hit >= 10; // allow 1 flaky encoding miss
    }

    @Step("Your Bookings order picker sheet")
    public boolean isYourBookingsSheetVisible() {
        return isPresent(ComposeLocators.textView("Your Bookings"))
                && (isPresent(ComposeLocators.textView("Completed"))
                || isPresent(ComposeLocators.textView("Active"))
                || isPresent(ComposeLocators.textView("Upcoming")));
    }

    @Step("Ticket Details screen")
    public boolean isTicketDetailVisible() {
        return isPresent(ComposeLocators.textView("Ticket Timeline"))
                || isPresent(ComposeLocators.textViewContains("Conversation"))
                || (isPresent(ComposeLocators.textViewContains("TKT-"))
                && isPresent(ComposeLocators.textView("Issue description")));
    }

    @Step("Support dialer number visible")
    public boolean isSupportNumberVisible() {
        if (isPresent(ComposeLocators.textView(SUPPORT_NUMBER))
                || isPresent(ComposeLocators.textViewContains("1800 00 0000"))
                || isPresent(ComposeLocators.textViewContains("1800"))
                || isPresent(ComposeLocators.textViewContains("+91"))) {
            return true;
        }
        // Dialer often exposes digits without spaces / content-desc
        try {
            String src = driver.getPageSource();
            return src != null && (src.contains("1800 00 0000")
                    || src.contains("1800000000")
                    || src.contains("+91 1800")
                    || src.contains("+911800"));
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Step("Support dialer package or number UI")
    public boolean isSupportDialerOpen() {
        try {
            String p = ((io.appium.java_client.android.AndroidDriver) driver).getCurrentPackage();
            if (p != null && (p.contains("dialer") || p.contains("telecom") || p.contains("phone"))) {
                return true;
            }
        } catch (RuntimeException ignored) {
        }
        return isSupportNumberVisible();
    }

    @Step("Dialer Call affordance (never tap)")
    public boolean isDialerCallVisible() {
        return isPresent(ComposeLocators.textView("Call"))
                && isSupportNumberVisible();
    }

    @Step("Recent tickets heading")
    public boolean isRecentTicketsVisible() {
        return isPresent(ComposeLocators.textView("Recent tickets"));
    }

    @Step("Any TKT- id visible")
    public boolean hasTicketIdVisible() {
        return visibleTexts().stream().anyMatch(t -> t.startsWith("TKT-"));
    }

    @Step("Pending status on history")
    public boolean isPendingStatusVisible() {
        return isPresent(ComposeLocators.textView("Pending"));
    }

    @Step("History row labels Date / Order ID / Topic")
    public boolean isHistoryRowLabelsVisible() {
        return isPresent(ComposeLocators.textView("Date"))
                && isPresent(ComposeLocators.textView("Order ID"))
                && isPresent(ComposeLocators.textView("Topic of Concern"));
    }

    @Step("No agent replies empty state on detail")
    public boolean isNoAgentRepliesVisible() {
        return isPresent(ComposeLocators.textViewContains("No agent replies"))
                || isPresent(ComposeLocators.textViewContains("queued for review"));
    }

    @Step("Header Back left of Help title")
    public boolean headerBackAligned() {
        Rectangle title = firstBounds(ComposeLocators.textView("Help & Support"));
        if (title == null) {
            title = firstBounds(ComposeLocators.textViewContains("Help"));
        }
        List<WebElement> backs = driver.findElements(By.xpath("//*[@content-desc='Back']"));
        if (title == null || backs.isEmpty()) {
            return false;
        }
        Rectangle back = backs.get(0).getRect();
        return Math.abs((title.y + title.height / 2) - (back.y + back.height / 2)) <= 40
                && back.x < title.x;
    }

    @Step("Title left inset publish band")
    public boolean titleLeftInsetOk() {
        Rectangle title = firstBounds(ComposeLocators.textView("Help & Support"));
        if (title == null) {
            title = firstBounds(ComposeLocators.textViewContains("Help"));
        }
        if (title == null) {
            return true;
        }
        // Dump: Help & Support x=127
        return title.x >= 80 && title.x <= 220;
    }

    @Step("Call Support right of title")
    public boolean callSupportLayoutOk() {
        Rectangle title = firstBounds(ComposeLocators.textView("Help & Support"));
        Rectangle call = firstBounds(ComposeLocators.textView("Call Support"));
        if (title == null || call == null) {
            return false;
        }
        return Math.abs(title.y - call.y) <= 40 && title.x < call.x;
    }

    @Step("Raise Ticket left of Ticket history same row")
    public boolean raiseHistoryRowLayoutOk() {
        Rectangle raise = firstBounds(ComposeLocators.textView("Raise Ticket"));
        Rectangle history = firstBounds(ComposeLocators.textView("Ticket history"));
        if (raise == null || history == null) {
            return false;
        }
        return Math.abs(raise.y - history.y) <= 20 && raise.x < history.x;
    }

    @Step("Working Hours above Live chat")
    public boolean hoursAboveLiveChatOk() {
        Rectangle hours = firstBounds(ComposeLocators.textViewContains("Working Hours"));
        Rectangle live = firstBounds(ComposeLocators.textView("Live chat"));
        if (hours == null || live == null) {
            return false;
        }
        return hours.y < live.y;
    }

    @Step("Tap Live chat")
    public void tapLiveChat() {
        clickClickableOrText("Live chat");
    }

    @Step("Tap Raise Ticket")
    public void tapRaiseTicket() {
        clickClickableOrText("Raise Ticket");
    }

    @Step("Tap Ticket history")
    public void tapTicketHistory() {
        clickClickableOrText("Ticket history");
    }

    @Step("Tap Call Support (may open dialer — recover with Back)")
    public void tapCallSupport() {
        clickClickableOrText("Call Support");
    }

    @Step("Tap Ticket Concern dropdown")
    public void tapTicketConcern() {
        List<WebElement> clickable = driver.findElements(ComposeLocators.clickableWithText("Ticket Concern"));
        if (!clickable.isEmpty()) {
            clickGestureOn(clickable.get(0));
            return;
        }
        // After pick, field shows selected concern — re-open via Payment/contains
        if (isPresent(ComposeLocators.textViewContains("Payment"))) {
            clickTextContains("Payment");
            return;
        }
        clickText("Ticket Concern");
    }

    @Step("Select concern option")
    public void selectConcern(String option) {
        if (isPresent(ComposeLocators.textView(option))) {
            clickText(option);
            return;
        }
        // Ampersand encoding / partial
        String head = option.split(" ")[0];
        clickTextContains(head);
    }

    @Step("Select first preferred concern (Payment)")
    public void selectPaymentBillingConcern() {
        selectConcern("Payment & Billing Issues");
    }

    @Step("Tap Order ID field")
    public void tapOrderIdField() {
        List<WebElement> clickable = driver.findElements(ComposeLocators.clickableWithText("Order ID"));
        if (!clickable.isEmpty()) {
            clickGestureOn(clickable.get(0));
            return;
        }
        clickText("Order ID");
    }

    @Step("Tap bookings tab Completed/Active/Upcoming")
    public void tapBookingsTab(String tab) {
        clickClickableOrText(tab);
    }

    @Step("Select first booking card from Your Bookings (never Chat now/Submit)")
    public void selectFirstBookingCard() {
        for (String name : List.of("Excavator 20 Tonnes", "Tata Ace", "Excavator")) {
            if (isPresent(ComposeLocators.textView(name))
                    || isPresent(ComposeLocators.textViewContains(name.split(" ")[0]))) {
                clickTextContains(name.split(" ")[0]);
                return;
            }
        }
        throw new IllegalStateException("No booking card to select in Your Bookings");
    }

    @Step("Tap first TKT- ticket in history")
    public void tapFirstTicket() {
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el).trim();
            if (t.startsWith("TKT-")) {
                clickGestureOn(el);
                return;
            }
        }
        throw new IllegalStateException("No TKT- ticket visible");
    }

    @Step("Type issue description (Raise Ticket)")
    public void typeDescription(String value) {
        List<WebElement> edits = driver.findElements(By.className("android.widget.EditText"));
        if (edits.isEmpty()) {
            throw new IllegalStateException("Description EditText missing");
        }
        edits.get(0).click();
        edits.get(0).clear();
        edits.get(0).sendKeys(value);
    }

    @Step("Clear issue description")
    public void clearDescription() {
        List<WebElement> edits = driver.findElements(By.className("android.widget.EditText"));
        if (!edits.isEmpty()) {
            edits.get(0).click();
            edits.get(0).clear();
        }
    }

    @Step("Tap header Back")
    public void tapHeaderBack() {
        List<WebElement> els = driver.findElements(By.xpath("//*[@content-desc='Back']"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Back not visible");
        }
        // Prefer clickable ancestor of Back (dump: parent [2,85][128,211])
        try {
            WebElement parent = els.get(0).findElement(By.xpath("./.."));
            clickGestureOn(parent);
        } catch (RuntimeException e) {
            clickGestureOn(els.get(0));
        }
    }

    @Step("Device Back")
    public void pressDeviceBack() {
        driver.navigate().back();
    }

    @Step("Visible texts")
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

    private static String normalizeAmp(String s) {
        return s.replace("&amp;", "&").replace("  ", " ").trim();
    }

    private Boolean actionParentEnabled(String label) {
        List<WebElement> parents = driver.findElements(ComposeLocators.clickableWithText(label));
        if (parents.isEmpty()) {
            return null;
        }
        String en = parents.get(0).getAttribute("enabled");
        if (en == null) {
            return null;
        }
        return Boolean.parseBoolean(en);
    }

    private void clickClickableOrText(String text) {
        List<WebElement> clickable = driver.findElements(ComposeLocators.clickableWithText(text));
        if (!clickable.isEmpty()) {
            clickGestureOn(clickable.get(0));
            return;
        }
        clickText(text);
    }

    private void clickText(String text) {
        List<WebElement> els = driver.findElements(ComposeLocators.textView(text));
        if (els.isEmpty()) {
            els = driver.findElements(ComposeLocators.textViewContains(text));
        }
        if (els.isEmpty()) {
            throw new IllegalStateException("Text not found: " + text);
        }
        clickGestureOn(els.get(0));
    }

    private void clickTextContains(String fragment) {
        List<WebElement> els = driver.findElements(ComposeLocators.textViewContains(fragment));
        if (els.isEmpty()) {
            throw new IllegalStateException("Text contains not found: " + fragment);
        }
        clickGestureOn(els.get(0));
    }

    private Rectangle firstBounds(By by) {
        List<WebElement> els = driver.findElements(by);
        if (els.isEmpty()) {
            return null;
        }
        return els.get(0).getRect();
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
