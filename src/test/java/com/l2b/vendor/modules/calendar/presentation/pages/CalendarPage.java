package com.l2b.vendor.modules.calendar.presentation.pages;

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
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Rental Schedule / Calendar. Live dump {@code /tmp/l2b-calendar-0001-20260928}
 * on {@code 9000000001}. Bottom tabs stay (Calendar · Home · Earning · Fleet).
 */
public class CalendarPage extends SplashScreen {

    @Override
    @Step("Wait for Schedule / Calendar")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Schedule screen did not appear", Duration.ofSeconds(12));
    }

    @Step("Check Schedule identity")
    public boolean isDisplayedNow() {
        return isTitleVisible()
                || (isPresent(ComposeLocators.textView("September"))
                || isPresent(ComposeLocators.textView("August"))
                || isPresent(ComposeLocators.textView("October")))
                && isPresent(By.xpath("//*[@content-desc='Previous month']"));
    }

    @Step("Check Schedule title")
    public boolean isTitleVisible() {
        return isPresent(ComposeLocators.textView("Schedule"));
    }

    @Step("Check year visible (e.g. 2026)")
    public boolean isYearVisible() {
        return isPresent(ComposeLocators.textView("2026"))
                || visibleTexts().stream().anyMatch(t -> t.matches("20\\d{2}"));
    }

    @Step("Read year text")
    public String yearText() {
        for (String t : visibleTexts()) {
            if (t.matches("20\\d{2}")) {
                return t;
            }
        }
        return "";
    }

    @Step("Read month name")
    public String monthName() {
        for (String m : List.of("January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December")) {
            if (isPresent(ComposeLocators.textView(m))) {
                return m;
            }
        }
        return "";
    }

    @Step("Check Previous month affordance")
    public boolean isPreviousMonthVisible() {
        return isPresent(By.xpath("//*[@content-desc='Previous month']"));
    }

    @Step("Check Next month affordance")
    public boolean isNextMonthVisible() {
        return isPresent(By.xpath("//*[@content-desc='Next month']"));
    }

    @Step("Check month nav row (prev + next)")
    public boolean isMonthNavVisible() {
        return isPreviousMonthVisible() && isNextMonthVisible();
    }

    @Step("Check weekday row letters present")
    public boolean hasWeekdayRow() {
        // Dump unique texts: S M T W F (duplicate T/S collapse in tree)
        return isPresent(ComposeLocators.textView("S"))
                && isPresent(ComposeLocators.textView("M"))
                && isPresent(ComposeLocators.textView("T"))
                && isPresent(ComposeLocators.textView("W"))
                && isPresent(ComposeLocators.textView("F"));
    }

    @Step("Check empty-day copy")
    public boolean isEmptyDayVisible() {
        return isPresent(ComposeLocators.textView("No bookings on this day."));
    }

    @Step("Check selected day heading contains month name")
    public boolean isDayHeadingVisible() {
        return visibleTexts().stream().anyMatch(t ->
                t.matches("\\d{1,2} \\w+, \\w+") || t.contains(" September,")
                        || t.contains(" August,") || t.contains(" October,"));
    }

    @Step("Read selected day heading")
    public String dayHeadingText() {
        for (String t : visibleTexts()) {
            if (t.matches("\\d{1,2} \\w+, \\w+")) {
                return t;
            }
        }
        return "";
    }

    @Step("Check agenda status Started")
    public boolean hasStartedStatus() {
        return isPresent(ComposeLocators.textView("Started"));
    }

    @Step("Check agenda status Completed")
    public boolean hasCompletedStatus() {
        return isPresent(ComposeLocators.textView("Completed"));
    }

    @Step("Check Start Task CTA")
    public boolean hasStartTask() {
        return isPresent(ComposeLocators.textView("Start Task"));
    }

    @Step("Check machine name on agenda")
    public boolean hasMachineOnAgenda(String name) {
        return isPresent(ComposeLocators.textView(name))
                || isPresent(ComposeLocators.textViewContains(name));
    }

    @Step("Check rental bottom tabs")
    public boolean isRentalBottomTabsVisible() {
        return isPresent(By.xpath("//*[@content-desc='Calendar']"))
                && isPresent(By.xpath("//*[@content-desc='Home']"))
                && isPresent(By.xpath("//*[@content-desc='Earning']"))
                && isPresent(By.xpath("//*[@content-desc='Fleet']"));
    }

    @Step("Count day-number cells in month grid (1..31 in upper band)")
    public int gridDayCount() {
        Set<String> days = new LinkedHashSet<>();
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el).trim();
            if (!t.matches("\\d{1,2}")) {
                continue;
            }
            int n = Integer.parseInt(t);
            if (n < 1 || n > 31) {
                continue;
            }
            Rectangle r = el.getRect();
            if (r.y >= 400 && r.y <= 1100 && r.width < 80) {
                days.add(t);
            }
        }
        return days.size();
    }

    @Step("Header Schedule and year share same vertical band")
    public boolean headerTitleYearAligned() {
        Rectangle title = firstBounds(ComposeLocators.textView("Schedule"));
        Rectangle year = null;
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            if (safeText(el).matches("20\\d{2}")) {
                year = el.getRect();
                break;
            }
        }
        if (title == null || year == null) {
            return false;
        }
        return Math.abs(title.y - year.y) <= 8;
    }

    @Step("Month name sits between Previous and Next month controls")
    public boolean monthBetweenNavChevrons() {
        if (!isPreviousMonthVisible() || !isNextMonthVisible()) {
            return false;
        }
        Rectangle prev = driver.findElement(By.xpath("//*[@content-desc='Previous month']")).getRect();
        Rectangle next = driver.findElement(By.xpath("//*[@content-desc='Next month']")).getRect();
        String month = monthName();
        if (month.isEmpty()) {
            return false;
        }
        Rectangle mid = firstBounds(ComposeLocators.textView(month));
        if (mid == null) {
            return false;
        }
        return prev.x < mid.x && mid.x < next.x
                && Math.abs(prev.y - mid.y) <= 40
                && Math.abs(next.y - mid.y) <= 40;
    }

    @Step("Bottom tab labels roughly evenly spaced")
    public boolean bottomTabsEvenSpacing() {
        int[] xs = new int[4];
        String[] tabs = {"Calendar", "Home", "Earning", "Fleet"};
        for (int i = 0; i < 4; i++) {
            List<WebElement> els = driver.findElements(By.xpath("//*[@content-desc='" + tabs[i] + "']"));
            if (els.isEmpty()) {
                return false;
            }
            xs[i] = els.get(0).getRect().x + els.get(0).getRect().width / 2;
        }
        int d1 = xs[1] - xs[0];
        int d2 = xs[2] - xs[1];
        int d3 = xs[3] - xs[2];
        return Math.abs(d1 - d2) < 80 && Math.abs(d2 - d3) < 80 && d1 > 100;
    }

    @Step("Day heading left inset is within publish padding band (24–72px)")
    public boolean dayHeadingLeftInsetOk() {
        String heading = dayHeadingText();
        if (heading.isEmpty()) {
            return true;
        }
        Rectangle r = firstBounds(ComposeLocators.textView(heading));
        if (r == null) {
            return true;
        }
        return r.x >= 24 && r.x <= 72;
    }

    @Step("Tap Previous month")
    public void tapPreviousMonth() {
        driver.findElement(By.xpath("//*[@content-desc='Previous month']")).click();
    }

    @Step("Tap Next month")
    public void tapNextMonth() {
        driver.findElement(By.xpath("//*[@content-desc='Next month']")).click();
    }

    @Step("Tap day number in month grid")
    public void tapDay(String day) {
        List<WebElement> days = driver.findElements(ComposeLocators.textView(day));
        if (days.isEmpty()) {
            throw new IllegalStateException("Day not found: " + day);
        }
        WebElement best = days.get(0);
        int bestY = Integer.MAX_VALUE;
        for (WebElement el : days) {
            Rectangle r = el.getRect();
            if (r.y < 1200 && r.y < bestY && r.width < 100) {
                bestY = r.y;
                best = el;
            }
        }
        clickGestureOn(best);
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

    @Step("Tap Earning bottom tab")
    public void tapEarningTab() {
        driver.findElement(By.xpath("//*[@content-desc='Earning']")).click();
    }

    @Step("Tap Fleet bottom tab")
    public void tapFleetTab() {
        driver.findElement(By.xpath("//*[@content-desc='Fleet']")).click();
    }

    @Step("Tap Calendar bottom tab")
    public void tapCalendarTab() {
        driver.findElement(By.xpath("//*[@content-desc='Calendar']")).click();
    }

    @Step("Tap Start Task (probe — may stay on Schedule)")
    public void tapStartTask() {
        List<WebElement> labels = driver.findElements(ComposeLocators.textView("Start Task"));
        if (labels.isEmpty()) {
            throw new IllegalStateException("Start Task not visible");
        }
        clickGestureOn(labels.get(0));
    }

    @Step("Swipe agenda up")
    public void swipeAgendaUp() {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1700));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(350),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1000));
        swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(swipe));
    }

    @Step("Visible TextView texts")
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
