package com.l2b.vendor.modules.bookings.presentation.tests.rentalbooking;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.bookings.presentation.pages.RentalBookingsPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.time.Duration;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * RB-I: taps on the Rental Bookings list that must not change booking state.
 * Sheets opened here (Assign, Change) are inspected and cancelled — the commit
 * taps live in RB-S. Isolated {@code bookings/booking-rental-interact-i*.xml}.
 */
@Epic("Vendor app")
@Feature("Rental Booking interact — rental vendor 9000000001")
public class RentalBookingInteractTest extends RentalBookingBaseTest {

    @Test(priority = 1, description = "RB-I1: Upcoming tab selects the upcoming list")
    @Severity(SeverityLevel.CRITICAL)
    @Description("From Active, tap Upcoming. Header returns to Upcoming Booking and upcoming "
            + "cards render. Record the landing name.")
    public void upcomingTab() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        assertThat(bookings.headerTitleNow())
                .as("See all lands on Upcoming by default")
                .isEqualTo(RentalBookingsPage.TITLE_UPCOMING);

        bookings.tapTab(RentalBookingsPage.TAB_ACTIVE);
        assertThat(bookings.headerTitleNow())
                .as("Active tab must switch header before returning to Upcoming")
                .isEqualTo(RentalBookingsPage.TITLE_ACTIVE);

        bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);
        String header = bookings.headerTitleNow();
        int cards = bookings.cardCountNow();
        Allure.parameter("headerTitle", header);
        Allure.parameter("cardCount", String.valueOf(cards));
        Allure.parameter("activeEmptyStillVisible",
                String.valueOf(bookings.isActiveEmptyStateVisible()));
        bookings.attachScreenshot("rb-i1-upcoming-tab");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(header)
                .as("Upcoming tab must restore Upcoming Booking header")
                .isEqualTo(RentalBookingsPage.TITLE_UPCOMING);
        assertThat(bookings.isActiveEmptyStateVisible())
                .as("Active empty copy must not linger under Upcoming")
                .isFalse();
        assertThat(cards)
                .as("Upcoming list must render cards (amount rows) after returning from Active")
                .isGreaterThan(0);
    }

    @Test(priority = 2, description = "RB-I2: Active tab swaps list and header")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap Active. Header becomes Active Order and the body is active cards or the "
            + "empty state. Upcoming cards must not linger underneath.")
    public void activeTab() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        int upcomingCards = bookings.cardCountNow();
        assertThat(bookings.headerTitleNow())
                .as("Start on Upcoming")
                .isEqualTo(RentalBookingsPage.TITLE_UPCOMING);

        bookings.tapTab(RentalBookingsPage.TAB_ACTIVE);
        String header = bookings.headerTitleNow();
        boolean empty = bookings.isActiveEmptyStateVisible();
        int cards = bookings.cardCountNow();
        Allure.parameter("headerTitle", header);
        Allure.parameter("activeEmpty", String.valueOf(empty));
        Allure.parameter("cardCount", String.valueOf(cards));
        Allure.parameter("upcomingCardsBefore", String.valueOf(upcomingCards));
        bookings.attachScreenshot("rb-i2-active-tab");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(header)
                .as("Active tab must show Active Order header")
                .isEqualTo(RentalBookingsPage.TITLE_ACTIVE);
        assertThat(empty || cards > 0)
                .as("Active body must be empty-state copy or active cards — not a blank hang")
                .isTrue();
        if (empty) {
            assertThat(cards)
                    .as("Active empty state must not keep Upcoming amount rows underneath")
                    .isEqualTo(0);
        }
    }

    @Test(priority = 3, description = "RB-I3: Completed tab swaps list and header")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap Completed. Header becomes Completed Order and completed summary cards "
            + "render. No Assign / Change controls on this tab.")
    public void completedTab() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();

        bookings.tapTab(RentalBookingsPage.TAB_COMPLETED);
        String header = bookings.headerTitleNow();
        // Completed cards are summary-only: bare ₹ figures (not Amount · rows).
        java.util.List<String> rupees = bookings.rupeeFiguresNow();
        int assign = bookings.assignCount();
        int change = bookings.changeCount();
        Allure.parameter("headerTitle", header);
        Allure.parameter("rupeeFigures", rupees.toString());
        Allure.parameter("assignCount", String.valueOf(assign));
        Allure.parameter("changeCount", String.valueOf(change));
        bookings.attachScreenshot("rb-i3-completed-tab");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(header)
                .as("Completed tab must show Completed Order header")
                .isEqualTo(RentalBookingsPage.TITLE_COMPLETED);
        assertThat(rupees)
                .as("Completed summary cards must render bare ₹ amounts")
                .isNotEmpty();
        assertThat(assign)
                .as("Assign must not appear on Completed")
                .isEqualTo(0);
        assertThat(change)
                .as("Change must not appear on Completed")
                .isEqualTo(0);
    }

    @Test(priority = 4,
            description = "RB-I4: rapid tab cycling settles on one selected tab")
    @Severity(SeverityLevel.NORMAL)
    @Description("Upcoming → Active → Completed → Upcoming with no wait between taps. Exactly "
            + "one header is visible at the end and the list matches it. Mixed content or a "
            + "stuck spinner is a new Bookings bug.")
    public void rapidTabCycling() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();

        bookings.tapTab(RentalBookingsPage.TAB_ACTIVE);
        bookings.tapTab(RentalBookingsPage.TAB_COMPLETED);
        bookings.tapTab(RentalBookingsPage.TAB_UPCOMING);

        String header = bookings.headerTitleNow();
        int cards = bookings.cardCountNow();
        boolean activeEmpty = bookings.isActiveEmptyStateVisible();
        boolean upcomingTitle = RentalBookingsPage.TITLE_UPCOMING.equals(header);
        boolean activeTitle = RentalBookingsPage.TITLE_ACTIVE.equals(header);
        boolean completedTitle = RentalBookingsPage.TITLE_COMPLETED.equals(header);
        int titleHits = (upcomingTitle ? 1 : 0) + (activeTitle ? 1 : 0) + (completedTitle ? 1 : 0);

        Allure.parameter("headerTitle", header);
        Allure.parameter("titleHits", String.valueOf(titleHits));
        Allure.parameter("cardCount", String.valueOf(cards));
        Allure.parameter("activeEmptyVisible", String.valueOf(activeEmpty));
        bookings.attachScreenshot("rb-i4-rapid-tab-cycle");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(titleHits)
                .as("Exactly one bookings header must be visible after rapid cycling")
                .isEqualTo(1);
        assertThat(header)
                .as("Final tab tap was Upcoming — header must settle on Upcoming Booking")
                .isEqualTo(RentalBookingsPage.TITLE_UPCOMING);
        assertThat(activeEmpty)
                .as("Active empty copy must not linger after settling on Upcoming")
                .isFalse();
        assertThat(cards)
                .as("Upcoming list must match the settled Upcoming header")
                .isGreaterThan(0);
    }

    @Test(priority = 5,
            description = "RB-I5: View More Details expands only its own card")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap View More Details on card 1. Extra detail appears for that card while "
            + "card 2 keeps its original rows. Dump 16 shows the expanded state inline.")
    public void viewMoreDetailsExpandsOneCard() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        int cards = bookings.cardCountNow();
        int viewMoreBefore = bookings.viewMoreDetailsCount();
        if (cards < 2 || viewMoreBefore < 2) {
            throw new SkipException("Need ≥2 Upcoming cards with View More Details for RB-I5");
        }

        bookings.tapViewMoreDetailsAt(0);
        int viewLess = bookings.viewLessDetailsCount();
        int viewMoreAfter = bookings.viewMoreDetailsCount();
        int otherCtas = bookings.changeCount() + bookings.assignCount();
        Allure.parameter("cards", String.valueOf(cards));
        Allure.parameter("viewMoreBefore", String.valueOf(viewMoreBefore));
        Allure.parameter("viewLessAfter", String.valueOf(viewLess));
        Allure.parameter("viewMoreAfter", String.valueOf(viewMoreAfter));
        Allure.parameter("card2AssignOrChange", String.valueOf(otherCtas));
        bookings.attachScreenshot("rb-i5-view-more-one-card");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(viewLess)
                .as("Only the tapped card must flip to View Less Details")
                .isEqualTo(1);
        assertThat(otherCtas)
                .as("Card 2 must still show Assign/Change (collapsed chrome), not a second View Less")
                .isGreaterThanOrEqualTo(1);
    }

    @Test(priority = 6,
            description = "RB-I6: expanded card collapses again")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap the toggle a second time (View Less Details if the label flips). The card "
            + "returns to its collapsed row set. A toggle that only expands is a new bug.")
    public void viewMoreDetailsCollapses() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        int viewMoreBefore = bookings.viewMoreDetailsCount();
        if (viewMoreBefore < 1) {
            throw new SkipException("Need ≥1 Upcoming card with View More Details for RB-I6");
        }

        bookings.tapViewMoreDetailsAt(0);
        assertThat(bookings.viewLessDetailsCount())
                .as("Expand must flip the tapped row to View Less Details")
                .isEqualTo(1);

        bookings.tapFirstViewLessDetails();
        int viewLessAfter = bookings.viewLessDetailsCount();
        int viewMoreAfter = bookings.viewMoreDetailsCount();
        Allure.parameter("viewMoreBefore", String.valueOf(viewMoreBefore));
        Allure.parameter("viewLessAfterCollapse", String.valueOf(viewLessAfter));
        Allure.parameter("viewMoreAfterCollapse", String.valueOf(viewMoreAfter));
        bookings.attachScreenshot("rb-i6-view-less-collapse");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(viewLessAfter)
                .as("View Less must be gone after collapse")
                .isEqualTo(0);
        assertThat(viewMoreAfter)
                .as("Collapsed card must restore View More Details")
                .isEqualTo(viewMoreBefore);
    }

    @Test(priority = 7,
            description = "RB-I7: Get Direction opens navigation and Back returns to Bookings")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap Get Direction. Record the destination package (maps intent or in-app map). "
            + "Back must return to the Bookings list on the same tab, not to Home or the "
            + "launcher.")
    public void getDirectionAndBack() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        if (bookings.getDirectionCount() < 1) {
            throw new SkipException("No Get Direction on Upcoming cards for RB-I7");
        }

        String pkgBefore = vendorPackage();
        String headerBefore = bookings.headerTitleNow();
        bookings.tapFirstGetDirection();
        Waits.until(DriverManager.get(),
                d -> {
                    String pkg = vendorPackage();
                    return (pkg != null && !pkg.isBlank()) ? Boolean.TRUE : null;
                },
                "After Get Direction, no current package",
                Duration.ofSeconds(8));

        // Emulator may show the Maps location permission sheet first.
        boolean permission = dismissMapsLocationPermissionIfPresent();
        Allure.parameter("mapsPermissionHandled", String.valueOf(permission));

        String pkgAfter = vendorPackage();
        String landing = pkgAfter == null ? "unknown"
                : pkgAfter.contains("maps") ? "maps"
                : pkgAfter.equals(Config.get("app.package"))
                        ? (bookings.isDisplayedNow() ? "bookings-in-app" : "vendor-other")
                        : "other-package:" + pkgAfter;
        Allure.parameter("packageBefore", String.valueOf(pkgBefore));
        Allure.parameter("packageAfter", String.valueOf(pkgAfter));
        Allure.parameter("getDirectionLanding", landing);
        bookings.attachScreenshot("rb-i7-get-direction-" + landing.replace(':', '-'));

        assertThat(pkgAfter).as("Get Direction must not crash (package blank)").isNotBlank();
        assertThat(landing).as("Get Direction landing must be named").isNotEqualTo("unknown");

        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        if (!Config.get("app.package").equals(pkgAfter) || !bookings.isDisplayedNow()) {
            android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.BACK));
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Second Back if still on Maps / permission chrome.
            if (!bookings.isDisplayedNow()
                    && (vendorPackage() == null || vendorPackage().contains("maps")
                    || !Config.get("app.package").equals(vendorPackage()))) {
                android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                        io.appium.java_client.android.nativekey.AndroidKey.BACK));
                try {
                    Thread.sleep(1200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (!Config.get("app.package").equals(vendorPackage())) {
                android.activateApp(Config.get("app.package"));
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        HomePage home = new HomePage();
        QuickBookingPage qb = new QuickBookingPage();
        String returnSurface = bookings.isDisplayedNow() ? "bookings"
                : qb.isDisplayedNow() ? "quick-booking"
                : home.isDisplayedNow() ? "home"
                : "other";
        Allure.parameter("returnSurface", returnSurface);
        Allure.parameter("headerAfter", bookings.headerTitleNow());
        bookings.attachScreenshot("rb-i7-after-back");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(returnSurface)
                .as("Device Back after Get Direction must restore Bookings — Home/QB/launcher is a defect")
                .isEqualTo("bookings");
        assertThat(bookings.headerTitleNow())
                .as("Upcoming tab after return")
                .isEqualTo(RentalBookingsPage.TITLE_UPCOMING);
    }

    /** Maps location permission sheet on emulator — tap While using the app when present. */
    private boolean dismissMapsLocationPermissionIfPresent() {
        try {
            java.util.List<org.openqa.selenium.WebElement> rows =
                    DriverManager.get().findElements(org.openqa.selenium.By.xpath(
                            "//*[contains(@text,'While using the app') or contains(@text,\"While using the app\")]"));
            if (rows.isEmpty()) {
                rows = DriverManager.get().findElements(org.openqa.selenium.By.xpath(
                        "//*[contains(@text,'Only this time')]"));
            }
            if (rows.isEmpty()) {
                return false;
            }
            rows.get(0).click();
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test(priority = 8, description = "RB-I8: tapping the map tile is safe")
    @Severity(SeverityLevel.MINOR)
    @Description("Tap the Google Map tile itself. Either it opens the same navigation target as "
            + "Get Direction or it is inert. It must not open an unrelated screen.")
    public void tapMapTile() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        if (bookings.mapCount() < 1) {
            throw new SkipException("No Google Map tile on Upcoming cards for RB-I8");
        }

        String pkgBefore = vendorPackage();
        bookings.tapFirstMapTile();
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String pkgAfter = vendorPackage();
        String landing = pkgAfter == null ? "unknown"
                : pkgAfter.contains("maps") ? "maps"
                : pkgAfter.equals(Config.get("app.package"))
                        ? (bookings.isDisplayedNow() ? "inert-or-in-app" : "vendor-other")
                        : "other-package:" + pkgAfter;
        Allure.parameter("packageBefore", String.valueOf(pkgBefore));
        Allure.parameter("packageAfter", String.valueOf(pkgAfter));
        Allure.parameter("mapTileLanding", landing);
        bookings.attachScreenshot("rb-i8-map-tile-" + landing.replace(':', '-'));

        assertThat(pkgAfter).as("Map tile must not crash (package blank)").isNotBlank();
        assertThat(landing)
                .as("Map tile must be maps, inert on Bookings, or in-app — not an unrelated package")
                .satisfiesAnyOf(
                        s -> assertThat(s).isEqualTo("maps"),
                        s -> assertThat(s).isEqualTo("inert-or-in-app"));

        if (!Config.get("app.package").equals(pkgAfter)) {
            ((io.appium.java_client.android.AndroidDriver) DriverManager.get())
                    .activateApp(Config.get("app.package"));
        }
    }

    @Test(priority = 9, description = "RB-I9: header Back returns to Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap the Back affordance. Home renders with greeting / Current Earning and the "
            + "rental tab bar. Dump 15-after-bookings-back confirms Home is the destination.")
    public void headerBackReturnsHome() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        assertThat(bookings.isBackVisible()).as("Back affordance present").isTrue();

        bookings.tapBack();
        HomePage home = new HomePage();
        Waits.until(DriverManager.get(),
                d -> home.isDisplayedNow() ? Boolean.TRUE : null,
                "Header Back from Bookings did not land on Home",
                Duration.ofSeconds(12));

        boolean greeting = home.isGreetingVisible();
        boolean earning = home.isCurrentEarningVisible();
        boolean tabs = home.isRentalBottomTabsVisible();
        Allure.parameter("homeGreeting", String.valueOf(greeting));
        Allure.parameter("homeCurrentEarning", String.valueOf(earning));
        Allure.parameter("rentalBottomTabs", String.valueOf(tabs));
        Allure.parameter("stillBookings", String.valueOf(bookings.isDisplayedNow()));
        home.attachScreenshot("rb-i9-header-back-home");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(bookings.isDisplayedNow())
                .as("Bookings chrome must be gone after header Back")
                .isFalse();
        assertThat(home.isDisplayedNow())
                .as("Header Back must land on Rental Home")
                .isTrue();
        assertThat(greeting || earning)
                .as("Home greeting or Current Earning must render")
                .isTrue();
    }

    @Test(priority = 10,
            description = "RB-I10: device Back matches header Back and does not exit the app")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Press device Back on the Bookings list. Expect Home, same as RB-I9. Landing on "
            + "the Android launcher is a new Bookings bug — do not fold into #13 (Quick Booking) "
            + "or #25 (Rental Home).")
    public void deviceBackFromBookings() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                io.appium.java_client.android.nativekey.AndroidKey.BACK));

        HomePage home = new HomePage();
        Waits.until(DriverManager.get(),
                d -> (home.isDisplayedNow()
                        || !Config.get("app.package").equals(vendorPackage()))
                        ? Boolean.TRUE : null,
                "After device Back from Bookings, no destination",
                Duration.ofSeconds(12));

        String pkg = vendorPackage();
        String landing = pkg == null ? "unknown"
                : pkg.contains("launcher") || pkg.contains("nexuslauncher") ? "launcher"
                : pkg.equals(Config.get("app.package"))
                        ? (home.isDisplayedNow() ? "home"
                                : bookings.isDisplayedNow() ? "bookings" : "vendor-other")
                        : "other:" + pkg;
        Allure.parameter("packageAfter", String.valueOf(pkg));
        Allure.parameter("landing", landing);
        home.attachScreenshot("rb-i10-device-back-" + landing.replace(':', '-'));

        assertThat(landing)
                .as("Device Back from Bookings must land on Home — not launcher "
                        + "(new Bookings bug, not #13/#25)")
                .isEqualTo("home");
        assertThat(home.isDisplayedNow()).as("Home after device Back").isTrue();
    }

    @Test(priority = 11, description = "RB-I11: Help opens support and returns")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap Help in the Bookings header. Record the destination, then Back to the "
            + "Bookings list with the same tab still selected.")
    public void helpFromBookings() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        assertThat(bookings.isHelpVisible()).as("Help present on Bookings").isTrue();
        String headerBefore = bookings.headerTitleNow();

        bookings.tapHelp();
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean leftBookings = !bookings.isDisplayedNow();
        String pkg = vendorPackage();
        Allure.parameter("headerBefore", headerBefore);
        Allure.parameter("leftBookings", String.valueOf(leftBookings));
        Allure.parameter("packageAfterHelp", String.valueOf(pkg));
        bookings.attachScreenshot("rb-i11-help-destination");

        assertThat(pkg).isEqualTo(Config.get("app.package"));
        assertThat(leftBookings)
                .as("Help must navigate away from the Bookings list")
                .isTrue();

        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                io.appium.java_client.android.nativekey.AndroidKey.BACK));
        Waits.until(DriverManager.get(),
                d -> bookings.isDisplayedNow() ? Boolean.TRUE : null,
                "Back from Help did not restore Bookings",
                Duration.ofSeconds(12));

        String headerAfter = bookings.headerTitleNow();
        Allure.parameter("headerAfter", headerAfter);
        bookings.attachScreenshot("rb-i11-back-to-bookings");

        assertThat(bookings.isDisplayedNow()).as("Bookings restored after Help Back").isTrue();
        assertThat(headerAfter)
                .as("Same Upcoming tab after Help return")
                .isEqualTo(RentalBookingsPage.TITLE_UPCOMING);
    }

    @Test(priority = 12, description = "RB-I12: tapping a Completed card")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap a completed summary card. Record whether a detail screen opens or the card "
            + "is inert, and confirm no Assign / Change / cancel action is reachable from there.")
    public void tapCompletedCard() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        bookings.tapTab(RentalBookingsPage.TAB_COMPLETED);
        java.util.List<String> rupees = bookings.rupeeFiguresNow();
        if (rupees.isEmpty()) {
            throw new SkipException("No Completed cards for RB-I12");
        }

        // Tap the first bare ₹ figure via page helper (Completed cards have no Amount · rows).
        java.util.List<String> figures = bookings.rupeeFiguresNow();
        String target = figures.get(0);
        org.openqa.selenium.WebElement amount =
                DriverManager.get().findElements(
                        org.openqa.selenium.By.xpath(
                                "//android.widget.TextView[@text='" + target.replace("'", "") + "']"))
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new SkipException("No bare ₹ node to tap on Completed"));
        String beforeHeader = bookings.headerTitleNow();
        amount.click();
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean stillCompleted = RentalBookingsPage.TITLE_COMPLETED.equals(bookings.headerTitleNow());
        boolean stillBookings = bookings.isDisplayedNow();
        int assign = bookings.assignCount();
        int change = bookings.changeCount();
        Allure.parameter("headerBefore", beforeHeader);
        Allure.parameter("headerAfter", bookings.headerTitleNow());
        Allure.parameter("stillBookings", String.valueOf(stillBookings));
        Allure.parameter("stillCompletedTab", String.valueOf(stillCompleted));
        Allure.parameter("assignCount", String.valueOf(assign));
        Allure.parameter("changeCount", String.valueOf(change));
        String behavior = !stillBookings ? "opened-elsewhere"
                : stillCompleted ? "inert-or-inline" : "tab-changed";
        Allure.parameter("tapBehavior", behavior);
        bookings.attachScreenshot("rb-i12-completed-card-tap");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(assign).as("No Assign after Completed card tap").isEqualTo(0);
        assertThat(change).as("No Change after Completed card tap").isEqualTo(0);
        if (!stillBookings) {
            // Detail screen — Back must not expose Assign/Change; then return.
            io.appium.java_client.android.AndroidDriver android =
                    (io.appium.java_client.android.AndroidDriver) DriverManager.get();
            android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.BACK));
            Waits.until(DriverManager.get(),
                    d -> bookings.isDisplayedNow() ? Boolean.TRUE : null,
                    "Back from Completed detail did not restore Bookings",
                    Duration.ofSeconds(10));
        }
    }

    @Test(priority = 13,
            description = "RB-I13: Change opens the operator sheet and cancels cleanly")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap Change on an assigned card. Inspect the operator list (names, availability, "
            + "disabled rows). Dismiss without selecting. The card must still show the original "
            + "operator. Committing a change is RB-S11.")
    public void changeOperatorSheetCancel() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        if (bookings.changeCount() < 1) {
            throw new SkipException("No Change CTA on Upcoming — need an assigned-operator card");
        }
        java.util.List<String> operatorsBefore = bookings.assignedOperatorRowsNow();
        assertThat(operatorsBefore).as("Need a visible Operator : row before Change").isNotEmpty();
        String original = operatorsBefore.get(0);

        bookings.tapFirstChange();
        assertThat(bookings.isAssignMachineVisible())
                .as("Change must open Assign machine sheet family")
                .isTrue();
        Allure.parameter("originalOperator", original);
        Allure.parameter("selectOperatorVisible",
                String.valueOf(bookings.isSelectOperatorVisible()));
        Allure.parameter("skipVisible", String.valueOf(bookings.isAssignSkipVisible()));
        bookings.attachScreenshot("rb-i13-change-sheet");

        if (bookings.isAssignSkipVisible()) {
            bookings.tapAssignSkip();
        } else {
            io.appium.java_client.android.AndroidDriver android =
                    (io.appium.java_client.android.AndroidDriver) DriverManager.get();
            android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.BACK));
        }
        Waits.until(DriverManager.get(),
                d -> !bookings.isAssignMachineVisible() ? Boolean.TRUE : null,
                "Change sheet did not dismiss",
                Duration.ofSeconds(10));

        java.util.List<String> operatorsAfter = bookings.assignedOperatorRowsNow();
        Allure.parameter("operatorsAfter", operatorsAfter.toString());
        bookings.attachScreenshot("rb-i13-after-cancel");

        assertThat(bookings.isDisplayedNow()).as("Still on Bookings after cancel").isTrue();
        assertThat(operatorsAfter)
                .as("Original operator must remain after Change cancel")
                .contains(original);
    }

    @Test(priority = 14,
            description = "RB-I14: Assign opens the assignment sheet and cancels cleanly")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap Assign on an 'Operator Not Assigned' card. Inspect machine / operator "
            + "pickers and the Confirm enablement (compare with #16 on the Accept path). Dismiss "
            + "without confirming; the card stays unassigned. Committing is RB-S10.")
    public void assignOperatorSheetCancel() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        if (bookings.assignCount() < 1) {
            throw new SkipException("No Assign CTA on Upcoming — need Operator Not Assigned card");
        }
        int unassignedBefore = bookings.operatorUnassignedCount();

        bookings.tapFirstAssign();
        assertThat(bookings.isAssignMachineVisible())
                .as("Assign must open Assign machine sheet")
                .isTrue();
        boolean confirmEnabled = bookings.isAssignConfirmEnabled();
        Allure.parameter("confirmEnabled", String.valueOf(confirmEnabled));
        Allure.parameter("selectOperatorVisible",
                String.valueOf(bookings.isSelectOperatorVisible()));
        Allure.parameter("skipVisible", String.valueOf(bookings.isAssignSkipVisible()));
        bookings.attachScreenshot("rb-i14-assign-sheet");

        if (bookings.isAssignSkipVisible()) {
            bookings.tapAssignSkip();
        } else {
            io.appium.java_client.android.AndroidDriver android =
                    (io.appium.java_client.android.AndroidDriver) DriverManager.get();
            android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.BACK));
        }
        Waits.until(DriverManager.get(),
                d -> !bookings.isAssignMachineVisible() ? Boolean.TRUE : null,
                "Assign sheet did not dismiss",
                Duration.ofSeconds(10));

        int unassignedAfter = bookings.operatorUnassignedCount();
        Allure.parameter("unassignedBefore", String.valueOf(unassignedBefore));
        Allure.parameter("unassignedAfter", String.valueOf(unassignedAfter));
        bookings.attachScreenshot("rb-i14-after-cancel");

        assertThat(bookings.isDisplayedNow()).as("Still on Bookings after Assign cancel").isTrue();
        assertThat(unassignedAfter)
                .as("Card must stay Operator Not Assigned after Assign cancel")
                .isGreaterThanOrEqualTo(unassignedBefore);
    }

    @Test(priority = 15,
            description = "RB-I15: last card in a long list is reachable and tappable")
    @Severity(SeverityLevel.NORMAL)
    @Description("Scroll to the final card and tap its View More Details. Verifies off-screen "
            + "element handling and that the list does not recycle the tap onto card 1.")
    public void lastCardInteraction() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        int viewMoreStart = bookings.viewMoreDetailsCount();
        if (viewMoreStart < 1) {
            throw new SkipException("No View More Details on Upcoming for RB-I15");
        }

        // Scroll until View More count stops growing / list end.
        int stable = 0;
        int lastCount = viewMoreStart;
        for (int i = 0; i < 12; i++) {
            bookings.swipeListUp();
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            int now = bookings.viewMoreDetailsCount();
            if (now <= lastCount) {
                stable++;
            } else {
                stable = 0;
                lastCount = now;
            }
            if (stable >= 2) {
                break;
            }
        }

        int viewMoreAtEnd = bookings.viewMoreDetailsCount();
        Allure.parameter("viewMoreStart", String.valueOf(viewMoreStart));
        Allure.parameter("viewMoreAtEnd", String.valueOf(viewMoreAtEnd));
        if (viewMoreAtEnd < 1) {
            throw new SkipException("No View More visible after scrolling to list end");
        }

        // Tap the last visible View More — should expand exactly one card.
        bookings.tapViewMoreDetailsAt(viewMoreAtEnd - 1);
        int viewLess = bookings.viewLessDetailsCount();
        Allure.parameter("viewLessAfterLastTap", String.valueOf(viewLess));
        bookings.attachScreenshot("rb-i15-last-card-expand");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(viewLess)
                .as("Last-card View More must expand exactly one card (not recycle onto card 1 only)")
                .isEqualTo(1);
    }

    @Test(priority = 16,
            description = "RB-I16: entry path independence — queue first, then Bookings")
    @Severity(SeverityLevel.NORMAL)
    @Description("Home → Booking Orders See all → Quick Booking → Close → Home → Upcoming See "
            + "all. The Bookings list must render the same content as a direct entry, with no "
            + "leftover Quick Booking chrome.")
    public void entryPathIndependence() {
        HomePage home = reachRentalHomeResilient();
        home.tapNthSeeAll(2); // Booking Orders See all
        QuickBookingPage qb = new QuickBookingPage();
        RentalBookingsPage bookings = new RentalBookingsPage();
        Waits.until(DriverManager.get(),
                d -> (qb.isDisplayedNow() || bookings.isDisplayedNow()) ? Boolean.TRUE : null,
                "Booking Orders See all opened neither Quick Booking nor Bookings",
                Duration.ofSeconds(12));

        String firstLanding = qb.isDisplayedNow() ? "quick-booking"
                : bookings.isDisplayedNow() ? "bookings-list" : "unknown";
        Allure.parameter("bookingOrdersSeeAllLanding", firstLanding);
        if (qb.isDisplayedNow()) {
            qb.tapClose();
        } else {
            bookings.tapBack();
        }
        Waits.until(DriverManager.get(),
                d -> home.isDisplayedNow() ? Boolean.TRUE : null,
                "Return from Booking Orders path did not land on Home",
                Duration.ofSeconds(12));

        home.tapNthSeeAll(1); // Upcoming → Bookings
        bookings.waitUntilLoaded();

        boolean closeLeft = qb.isCloseVisible();
        String header = bookings.headerTitleNow();
        int cards = bookings.cardCountNow();
        Allure.parameter("headerTitle", header);
        Allure.parameter("cardCount", String.valueOf(cards));
        Allure.parameter("qbCloseVisible", String.valueOf(closeLeft));
        bookings.attachScreenshot("rb-i16-entry-path");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(bookings.isDisplayedNow()).as("Bookings after queue/list → Home → See all").isTrue();
        assertThat(closeLeft)
                .as("No leftover Quick Booking Close chrome on Bookings")
                .isFalse();
        assertThat(header).isEqualTo(RentalBookingsPage.TITLE_UPCOMING);
        assertThat(cards).as("Upcoming cards after alternate entry").isGreaterThan(0);
    }
}
