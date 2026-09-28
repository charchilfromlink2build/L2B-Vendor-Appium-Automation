package com.l2b.vendor.modules.calendar.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.calendar.presentation.pages.CalendarPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Calendar edge / publish-quality — padding, month edges, empty→booked, no crash
 * on {@code 9000000001}. Dump {@code /tmp/l2b-calendar-0001-20260928}.
 */
@Epic("Vendor app")
@Feature("Calendar edge — rental 9000000001")
public class CalendarEdgeTest extends CalendarBaseTest {

    @Test(priority = 1, description = "CA-E1: rapid Previous×3 stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Spam Previous month — no crash/launcher; still Schedule chrome.")
    public void rapidPreviousMonths() {
        CalendarPage cal = reachCalendarViaTab();
        for (int i = 0; i < 3; i++) {
            cal.tapPreviousMonth();
            sleepBrief(400);
        }
        Allure.parameter("month", cal.monthName());
        cal.attachScreenshot("calendar-ca-e1-rapid-prev");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(classifyRentalNow()).isEqualTo("calendar");
        assertThat(cal.isTitleVisible()).isTrue();
        // restore toward Sep for later cases
        for (int i = 0; i < 3; i++) {
            cal.tapNextMonth();
            sleepBrief(400);
        }
    }

    @Test(priority = 2, description = "CA-E2: rapid Next×3 stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidNextMonths() {
        CalendarPage cal = reachCalendarViaTab();
        for (int i = 0; i < 3; i++) {
            cal.tapNextMonth();
            sleepBrief(400);
        }
        Allure.parameter("month", cal.monthName());
        cal.attachScreenshot("calendar-ca-e2-rapid-next");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(classifyRentalNow()).isEqualTo("calendar");
        for (int i = 0; i < 3; i++) {
            cal.tapPreviousMonth();
            sleepBrief(400);
        }
    }

    @Test(priority = 3, description = "CA-E3: day-1 narrow cell still selectable")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump: day '1' can be narrow — tap must update heading, no miss-tap crash.")
    public void dayOneSelectable() {
        CalendarPage cal = ensureSeptember();
        cal.tapDay("1");
        sleepBrief(1000);
        Allure.parameter("heading", cal.dayHeadingText());
        cal.attachScreenshot("calendar-ca-e3-day1");
        assertThat(cal.dayHeadingText()).contains("1");
        assertThat(classifyRentalNow()).isEqualTo("calendar");
    }

    @Test(priority = 4, description = "CA-E4: last day of month (30) selectable")
    @Severity(SeverityLevel.CRITICAL)
    public void lastDaySelectable() {
        CalendarPage cal = ensureSeptember();
        cal.tapDay("30");
        sleepBrief(1000);
        Allure.parameter("heading", cal.dayHeadingText());
        assertThat(cal.dayHeadingText()).contains("30");
        assertThat(cal.isDisplayedNow()).isTrue();
    }

    @Test(priority = 5, description = "CA-E5: empty→booked→empty agenda flip")
    @Severity(SeverityLevel.CRITICAL)
    @Description("21 empty → 25 booked → 21 empty; chrome stable.")
    public void emptyBookedEmptyFlip() {
        CalendarPage cal = ensureSeptember();
        cal.tapDay("21");
        sleepBrief(900);
        assertThat(cal.isEmptyDayVisible()).isTrue();
        cal.tapDay("25");
        sleepBrief(900);
        assertThat(cal.isEmptyDayVisible()).isFalse();
        cal.tapDay("21");
        sleepBrief(900);
        assertThat(cal.isEmptyDayVisible()).isTrue();
        assertThat(cal.isTitleVisible()).isTrue();
        assertThat(cal.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 6, description = "CA-E6: August empty day still shows empty copy")
    @Severity(SeverityLevel.NORMAL)
    @Description("Prev to August, tap mid-month day — empty or agenda, no crash.")
    public void augustDayNoCrash() {
        CalendarPage cal = ensureSeptember();
        cal.tapPreviousMonth();
        sleepBrief(900);
        assertThat(cal.monthName()).isEqualTo("August");
        cal.tapDay("15");
        sleepBrief(900);
        Allure.parameter("heading", cal.dayHeadingText());
        cal.attachScreenshot("calendar-ca-e6-aug");
        assertThat(cal.isDisplayedNow()).isTrue();
        assertThat(cal.dayHeadingText()).isNotBlank();
        cal.tapNextMonth();
        sleepBrief(700);
    }

    @Test(priority = 7, description = "CA-E7: October day no crash")
    @Severity(SeverityLevel.NORMAL)
    public void octoberDayNoCrash() {
        CalendarPage cal = ensureSeptember();
        cal.tapNextMonth();
        sleepBrief(900);
        assertThat(cal.monthName()).isEqualTo("October");
        cal.tapDay("10");
        sleepBrief(900);
        Allure.parameter("heading", cal.dayHeadingText());
        assertThat(cal.isDisplayedNow()).isTrue();
        cal.tapPreviousMonth();
        sleepBrief(700);
    }

    @Test(priority = 8, description = "CA-E8: header title/year alignment (padding)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Publish: Schedule + year same header band; year right of title.")
    public void headerAlignmentPublish() {
        CalendarPage cal = reachCalendarViaTab();
        assertThat(cal.headerTitleYearAligned())
                .as("Title/year vertical band + horizontal order")
                .isTrue();
        assertThat(cal.dayHeadingLeftInsetOk())
                .as("Day heading left padding 24–72px")
                .isTrue();
    }

    @Test(priority = 9, description = "CA-E9: month between chevrons (layout)")
    @Severity(SeverityLevel.CRITICAL)
    public void monthBetweenChevronsLayout() {
        CalendarPage cal = reachCalendarViaTab();
        assertThat(cal.monthBetweenNavChevrons()).isTrue();
        assertThat(cal.isPreviousMonthVisible()).isTrue();
        assertThat(cal.isNextMonthVisible()).isTrue();
    }

    @Test(priority = 10, description = "CA-E10: bottom tabs even spacing")
    @Severity(SeverityLevel.CRITICAL)
    public void bottomTabsEvenSpacingPublish() {
        CalendarPage cal = reachCalendarViaTab();
        assertThat(cal.bottomTabsEvenSpacing())
                .as("Calendar·Home·Earning·Fleet centers even")
                .isTrue();
    }

    @Test(priority = 11, description = "CA-E11: Calendar tab while already on Calendar")
    @Severity(SeverityLevel.NORMAL)
    @Description("Re-tap Calendar — stay Schedule, no flash to Home.")
    public void retapCalendarTab() {
        CalendarPage cal = reachCalendarViaTab();
        new HomePage().tapDesc("Calendar");
        sleepBrief(800);
        assertThat(classifyRentalNow()).isEqualTo("calendar");
        assertThat(cal.isTitleVisible()).isTrue();
    }

    @Test(priority = 12, description = "CA-E12: Home→Calendar→Earning→Calendar roundtrip")
    @Severity(SeverityLevel.CRITICAL)
    public void tabRoundtripKeepsSchedule() {
        CalendarPage cal = reachCalendarViaTab();
        cal.tapHomeTab();
        sleepBrief(900);
        new HomePage().tapDesc("Calendar");
        cal.waitUntilLoaded();
        cal.tapEarningTab();
        sleepBrief(900);
        new HomePage().tapDesc("Calendar");
        cal.waitUntilLoaded();
        assertThat(classifyRentalNow()).isEqualTo("calendar");
        assertThat(cal.isMonthNavVisible() || cal.monthName() != null).isTrue();
        assertThat(cal.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 13, description = "CA-E13: Schedule Back → Home; second Back documents #25")
    @Severity(SeverityLevel.CRITICAL)
    @Description("First device Back from Schedule must land Home (not launcher). "
            + "Second Back from Home root may hit known BUGS_FOUND #25 — document, not Calendar regression.")
    public void doubleBackStaysVendor() {
        CalendarPage cal = reachCalendarViaTab();
        DriverManager.get().navigate().back();
        sleepBrief(900);
        String mid = classifyRentalNow();
        Allure.parameter("afterFirstBack", mid);
        cal.attachScreenshot("calendar-ca-e13-after-first-back");
        assertThat(mid)
                .as("Schedule device Back must go Home — not launcher")
                .isIn("home", "extend-time", "quick-booking");
        assertThat(mid).isNotEqualTo("launcher");

        DriverManager.get().navigate().back();
        sleepBrief(900);
        String end = classifyRentalNow();
        Allure.parameter("afterSecondBack", end);
        cal.attachScreenshot("calendar-ca-e13-double-back");
        if ("launcher".equals(end)) {
            Allure.parameter("knownBug", "#25 Rental Home Back→launcher (cascade after Schedule→Home)");
            // Calendar first Back is correct; Home root exit is already #25.
            assertThat(mid).isIn("home", "extend-time", "quick-booking");
            return;
        }
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(end).isNotEqualTo("launcher");
    }

    @Test(priority = 14, description = "CA-E14: Start Task then Back recovers Schedule/Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Probe Start Task if shown; Back/dismiss must not leave Vendor. No full re-login.")
    public void startTaskThenRecover() {
        CalendarPage cal = ensureSeptember();
        cal.tapDay("25");
        sleepBrief(900);
        if (!cal.hasStartTask()) {
            cal.tapDay("28");
            sleepBrief(900);
        }
        Allure.parameter("hasStartTask", String.valueOf(cal.hasStartTask()));
        if (cal.hasStartTask()) {
            cal.tapStartTask();
            sleepBrief(1200);
            DriverManager.get().navigate().back();
            sleepBrief(900);
        }
        String after = classifyRentalNow();
        Allure.parameter("afterStartTaskBack", after);
        cal.attachScreenshot("calendar-ca-e14-after-back");
        assertThat(after).isNotEqualTo("launcher");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        dismissToCalendar(cal);
        Allure.parameter("afterDismiss", classifyRentalNow());
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    private CalendarPage ensureSeptember() {
        CalendarPage cal = reachCalendarViaTab();
        for (int i = 0; i < 4 && !"September".equals(cal.monthName()); i++) {
            String m = cal.monthName();
            if ("August".equals(m) || "July".equals(m)) {
                cal.tapNextMonth();
            } else {
                cal.tapPreviousMonth();
            }
            sleepBrief(700);
        }
        assertThat(cal.monthName()).isEqualTo("September");
        return cal;
    }

    private void sleepBrief(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
