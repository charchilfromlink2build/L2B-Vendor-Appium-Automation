package com.l2b.vendor.modules.calendar.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.calendar.presentation.pages.CalendarPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Calendar landing — Schedule chrome on {@code 9000000001}. Dump
 * {@code /tmp/l2b-calendar-0001-20260928}. Includes publish-quality layout checks.
 */
@Epic("Vendor app")
@Feature("Calendar landing — rental 9000000001")
public class CalendarLandingTest extends CalendarBaseTest {

    @Test(priority = 1, description = "CA-L1: bottom Calendar → Schedule")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Tap Calendar tab. Expect classify=calendar, Schedule title, bottom tabs.")
    public void bottomTabOpensSchedule() {
        CalendarPage cal = reachCalendarViaTab();
        cal.attachScreenshot("calendar-ca-l1");
        assertThat(classifyRentalNow()).isEqualTo("calendar");
        assertThat(cal.isDisplayedNow()).isTrue();
        assertThat(cal.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 2, description = "CA-L2: Schedule title + year + Back")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 01: Schedule + 2026 + content-desc Back.")
    public void titleYearBackChrome() {
        CalendarPage cal = reachCalendarViaTab();
        Allure.parameter("year", cal.yearText());
        cal.attachScreenshot("calendar-ca-l2");
        assertThat(cal.isTitleVisible()).isTrue();
        assertThat(cal.isYearVisible()).isTrue();
        assertThat(cal.headerTitleYearAligned())
                .as("Schedule title and year must share header row (padding/align)")
                .isTrue();
    }

    @Test(priority = 3, description = "CA-L3: month name + Previous/Next month")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 01: September between Previous month and Next month chevrons.")
    public void monthNavChrome() {
        CalendarPage cal = reachCalendarViaTab();
        Allure.parameter("month", cal.monthName());
        cal.attachScreenshot("calendar-ca-l3");
        assertThat(cal.monthName()).isNotBlank();
        assertThat(cal.isPreviousMonthVisible()).isTrue();
        assertThat(cal.isNextMonthVisible()).isTrue();
        assertThat(cal.monthBetweenNavChevrons())
                .as("Month label must sit between prev/next (layout)")
                .isTrue();
    }

    @Test(priority = 4, description = "CA-L4: weekday row S M T W F")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump weekday letters present (duplicate T/S may collapse in a11y).")
    public void weekdayRowPresent() {
        CalendarPage cal = reachCalendarViaTab();
        assertThat(cal.hasWeekdayRow()).isTrue();
    }

    @Test(priority = 5, description = "CA-L5: month grid has day cells")
    @Severity(SeverityLevel.CRITICAL)
    @Description("September dump shows days 1–30 in grid band.")
    public void monthGridHasDays() {
        CalendarPage cal = reachCalendarViaTab();
        int n = cal.gridDayCount();
        Allure.parameter("gridDays", String.valueOf(n));
        cal.attachScreenshot("calendar-ca-l5");
        assertThat(n).as("Expect most days of month in grid").isGreaterThanOrEqualTo(28);
    }

    @Test(priority = 6, description = "CA-L6: selected day heading")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump: '28 September, Monday' style heading under grid.")
    public void selectedDayHeading() {
        CalendarPage cal = reachCalendarViaTab();
        String heading = cal.dayHeadingText();
        Allure.parameter("heading", heading);
        assertThat(cal.isDayHeadingVisible()).isTrue();
        assertThat(heading).isNotBlank();
        assertThat(cal.dayHeadingLeftInsetOk())
                .as("Day heading left inset should be 24–72px for publish padding")
                .isTrue();
    }

    @Test(priority = 7, description = "CA-L7: empty day copy on day 21")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 02: 21 September → No bookings on this day.")
    public void emptyDayCopy() {
        CalendarPage cal = reachCalendarViaTab();
        cal.tapDay("21");
        sleepBrief();
        Allure.parameter("heading", cal.dayHeadingText());
        cal.attachScreenshot("calendar-ca-l7-empty");
        assertThat(cal.dayHeadingText()).contains("21");
        assertThat(cal.isEmptyDayVisible()).isTrue();
    }

    @Test(priority = 8, description = "CA-L8: booked day agenda chrome (day 25)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 03: 25 Sep shows Started/Completed/Start Task + machine names.")
    public void bookedDayAgendaChrome() {
        CalendarPage cal = reachCalendarViaTab();
        cal.tapDay("25");
        sleepBrief();
        Allure.parameter("heading", cal.dayHeadingText());
        Allure.parameter("started", String.valueOf(cal.hasStartedStatus()));
        Allure.parameter("completed", String.valueOf(cal.hasCompletedStatus()));
        Allure.parameter("startTask", String.valueOf(cal.hasStartTask()));
        cal.attachScreenshot("calendar-ca-l8-booked");
        assertThat(cal.dayHeadingText()).contains("25");
        assertThat(cal.isEmptyDayVisible()).isFalse();
        assertThat(cal.hasStartedStatus() || cal.hasCompletedStatus() || cal.hasStartTask())
                .as("Booked day must show timeline statuses or Start Task")
                .isTrue();
        assertThat(cal.hasMachineOnAgenda("Tata Ace") || cal.hasMachineOnAgenda("Excavator"))
                .as("Expect machine card on 25 Sep dump")
                .isTrue();
    }

    @Test(priority = 9, description = "CA-L9: bottom tabs stay + even spacing")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Calendar·Home·Earning·Fleet stay; centers roughly evenly spaced.")
    public void bottomTabsLayout() {
        CalendarPage cal = reachCalendarViaTab();
        assertThat(cal.isRentalBottomTabsVisible()).isTrue();
        assertThat(cal.bottomTabsEvenSpacing())
                .as("Bottom tab centers should be evenly spaced (publish layout)")
                .isTrue();
    }

    @Test(priority = 10, description = "CA-L10: today agenda or empty without crash")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap day 28 (dump today). Expect heading + Start Task or empty — stay Schedule.")
    public void todayDayLoads() {
        CalendarPage cal = reachCalendarViaTab();
        cal.tapDay("28");
        sleepBrief();
        Allure.parameter("heading", cal.dayHeadingText());
        cal.attachScreenshot("calendar-ca-l10-today");
        assertThat(cal.isDisplayedNow()).isTrue();
        assertThat(cal.dayHeadingText()).contains("28");
        assertThat(classifyRentalNow()).isEqualTo("calendar");
    }

    private void sleepBrief() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
