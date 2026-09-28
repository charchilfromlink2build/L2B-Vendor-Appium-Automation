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
import java.time.Duration;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

/**
 * Calendar interact — month/day nav, tabs, Back on {@code 9000000001}.
 * Dump {@code /tmp/l2b-calendar-0001-20260928}. No Accept/Decline/Logout.
 */
@Epic("Vendor app")
@Feature("Calendar interact — rental 9000000001")
public class CalendarInteractTest extends CalendarBaseTest {

    @Test(priority = 1, description = "CA-I1: Previous month → August")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Dump 06: Previous month changes September → August.")
    public void previousMonth() {
        CalendarPage cal = reachCalendarViaTab();
        // Ensure September first
        for (int i = 0; i < 3 && !"September".equals(cal.monthName()); i++) {
            if ("August".equals(cal.monthName())) {
                cal.tapNextMonth();
            } else if ("October".equals(cal.monthName())) {
                cal.tapPreviousMonth();
            }
            sleepBrief();
        }
        cal.tapPreviousMonth();
        sleepBrief();
        Allure.parameter("month", cal.monthName());
        cal.attachScreenshot("calendar-ca-i1-prev");
        assertThat(cal.monthName()).isEqualTo("August");
        assertThat(classifyRentalNow()).isEqualTo("calendar");
    }

    @Test(priority = 2, description = "CA-I2: Next month → October")
    @Severity(SeverityLevel.BLOCKER)
    @Description("From September, Next month → October (dump 08).")
    public void nextMonth() {
        CalendarPage cal = ensureSeptember();
        cal.tapNextMonth();
        sleepBrief();
        Allure.parameter("month", cal.monthName());
        cal.attachScreenshot("calendar-ca-i2-next");
        assertThat(cal.monthName()).isEqualTo("October");
    }

    @Test(priority = 3, description = "CA-I3: Next then Previous restores September")
    @Severity(SeverityLevel.CRITICAL)
    @Description("September → Next → Previous → September.")
    public void nextThenPreviousRestores() {
        CalendarPage cal = ensureSeptember();
        cal.tapNextMonth();
        sleepBrief();
        cal.tapPreviousMonth();
        sleepBrief();
        assertThat(cal.monthName()).isEqualTo("September");
    }

    @Test(priority = 4, description = "CA-I4: select empty day 21")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap 21 → heading + No bookings.")
    public void selectEmptyDay() {
        CalendarPage cal = ensureSeptember();
        cal.tapDay("21");
        sleepBrief();
        assertThat(cal.dayHeadingText()).contains("21");
        assertThat(cal.isEmptyDayVisible()).isTrue();
    }

    @Test(priority = 5, description = "CA-I5: select booked day 25")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap 25 → agenda statuses/machines, not empty copy.")
    public void selectBookedDay() {
        CalendarPage cal = ensureSeptember();
        cal.tapDay("25");
        sleepBrief();
        assertThat(cal.dayHeadingText()).contains("25");
        assertThat(cal.isEmptyDayVisible()).isFalse();
        assertThat(cal.hasStartedStatus() || cal.hasStartTask()).isTrue();
    }

    @Test(priority = 6, description = "CA-I6: switch empty → booked day")
    @Severity(SeverityLevel.CRITICAL)
    @Description("21 (empty) then 25 (booked) updates agenda.")
    public void switchEmptyToBooked() {
        CalendarPage cal = ensureSeptember();
        cal.tapDay("21");
        sleepBrief();
        assertThat(cal.isEmptyDayVisible()).isTrue();
        cal.tapDay("25");
        sleepBrief();
        assertThat(cal.isEmptyDayVisible()).isFalse();
        assertThat(cal.dayHeadingText()).contains("25");
    }

    @Test(priority = 7, description = "CA-I7: Start Task probe stays in Vendor")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 09: tap Start Task if present — stay Vendor, no launcher. No job complete.")
    public void startTaskProbe() {
        CalendarPage cal = ensureSeptember();
        cal.tapDay("28");
        sleepBrief();
        if (!cal.hasStartTask()) {
            cal.tapDay("25");
            sleepBrief();
        }
        Allure.parameter("hasStartTask", String.valueOf(cal.hasStartTask()));
        if (cal.hasStartTask()) {
            cal.tapStartTask();
            sleepBrief();
        }
        cal.attachScreenshot("calendar-ca-i7-start-task");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
        dismissToCalendar(cal);
        assertThat(cal.isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
    }

    @Test(priority = 8, description = "CA-I8: Home tab from Calendar")
    @Severity(SeverityLevel.CRITICAL)
    public void homeTabFromCalendar() {
        CalendarPage cal = reachCalendarViaTab();
        cal.tapHomeTab();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> "home".equals(classifyRentalNow()) || new HomePage().isDisplayedNow());
        assertThat(classifyRentalNow()).isIn("home", "extend-time", "quick-booking");
    }

    @Test(priority = 9, description = "CA-I9: Earning tab from Calendar")
    @Severity(SeverityLevel.CRITICAL)
    public void earningTabFromCalendar() {
        CalendarPage cal = reachCalendarViaTab();
        cal.tapEarningTab();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> "earning".equals(classifyRentalNow()));
        assertThat(classifyRentalNow()).isEqualTo("earning");
    }

    @Test(priority = 10, description = "CA-I10: Fleet tab from Calendar")
    @Severity(SeverityLevel.CRITICAL)
    public void fleetTabFromCalendar() {
        CalendarPage cal = reachCalendarViaTab();
        cal.tapFleetTab();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> "fleet".equals(classifyRentalNow()));
        assertThat(classifyRentalNow()).isEqualTo("fleet");
    }

    @Test(priority = 11, description = "CA-I11: header Back → Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 15: header Back → Home.")
    public void headerBackToHome() {
        CalendarPage cal = reachCalendarViaTab();
        cal.tapHeaderBack();
        sleepBrief();
        assertThat(classifyRentalNow()).isIn("home", "extend-time", "quick-booking");
    }

    @Test(priority = 12, description = "CA-I12: device Back → Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 14: device Back → Home, not launcher.")
    public void deviceBackToHome() {
        CalendarPage cal = reachCalendarViaTab();
        DriverManager.get().navigate().back();
        sleepBrief();
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        assertThat(after).isNotEqualTo("launcher");
        assertThat(after).isIn("home", "extend-time", "quick-booking");
    }

    @Test(priority = 13, description = "CA-I13: re-open Calendar after Back")
    @Severity(SeverityLevel.NORMAL)
    public void reopenAfterBack() {
        CalendarPage cal = reachCalendarViaTab();
        cal.tapHeaderBack();
        sleepBrief();
        new HomePage().tapDesc("Calendar");
        cal.waitUntilLoaded();
        assertThat(classifyRentalNow()).isEqualTo("calendar");
        assertThat(cal.isTitleVisible()).isTrue();
    }

    @Test(priority = 14, description = "CA-I14: agenda swipe keeps Schedule chrome")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump 05: swipe agenda — Schedule + tabs stay.")
    public void agendaSwipeKeepsChrome() {
        CalendarPage cal = ensureSeptember();
        cal.tapDay("25");
        sleepBrief();
        cal.swipeAgendaUp();
        sleepBrief();
        cal.attachScreenshot("calendar-ca-i14-swipe");
        assertThat(cal.isTitleVisible()).isTrue();
        assertThat(cal.isRentalBottomTabsVisible()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("calendar");
    }

    private CalendarPage ensureSeptember() {
        CalendarPage cal = reachCalendarViaTab();
        for (int i = 0; i < 4 && !"September".equals(cal.monthName()); i++) {
            String m = cal.monthName();
            if ("August".equals(m)) {
                cal.tapNextMonth();
            } else if ("October".equals(m)) {
                cal.tapPreviousMonth();
            } else if ("July".equals(m)) {
                cal.tapNextMonth();
            } else {
                cal.tapPreviousMonth();
            }
            sleepBrief();
        }
        assertThat(cal.monthName()).isEqualTo("September");
        return cal;
    }

    private void sleepBrief() {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
