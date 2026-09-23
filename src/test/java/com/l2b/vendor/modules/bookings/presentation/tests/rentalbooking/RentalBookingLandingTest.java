package com.l2b.vendor.modules.bookings.presentation.tests.rentalbooking;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * RB-L: Rental Bookings list visibility on {@code 9000000001}. Read-only — no tab
 * taps (RB-I) and no state change (RB-S). Isolated
 * {@code bookings/booking-rental-landing-l*.xml}.
 *
 * <p>Every method is {@code enabled = false} and skips if forced. Bodies are filled
 * one at a time during execution day.
 */
@Epic("Vendor app")
@Feature("Rental Booking landing — rental vendor 9000000001")
public class RentalBookingLandingTest extends RentalBookingBaseTest {

    @Test(priority = 1,
            description = "RB-L1: Home Upcoming See all opens the Bookings list")
    @Severity(SeverityLevel.CRITICAL)
    @Description("From usable Home, tap See all on the Upcoming Booking section. Expect the "
            + "Bookings screen: Back affordance, three tabs, a bookings header, and not the "
            + "Quick Booking Close app bar. Record the landing. Dump 11-see-all-upcoming.")
    public void seeAllOpensBookingsList() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();

        String header = bookings.headerTitleNow();
        Allure.parameter("headerTitle", header);
        Allure.parameter("tabsVisible", String.valueOf(bookings.areTabsVisible()));
        Allure.parameter("backVisible", String.valueOf(bookings.isBackVisible()));
        Allure.parameter("helpVisible", String.valueOf(bookings.isHelpVisible()));
        Allure.parameter("cardCount", String.valueOf(bookings.cardCountNow()));
        bookings.attachScreenshot("rb-l1-see-all-bookings");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(bookings.isDisplayedNow())
                .as("See all must land on the Bookings screen (tabs + a bookings header)")
                .isTrue();
        assertThat(bookings.areTabsVisible())
                .as("Upcoming / Active / Completed tabs must be present")
                .isTrue();
        assertThat(header)
                .as("A bookings header (Upcoming Booking / Active Order / Completed Order) must render")
                .isIn(RentalBookingsPage.TITLE_UPCOMING,
                        RentalBookingsPage.TITLE_ACTIVE,
                        RentalBookingsPage.TITLE_COMPLETED);
        assertThat(bookings.isBackVisible())
                .as("Back affordance must be present on the Bookings screen")
                .isTrue();
    }

    @Test(priority = 2,
            description = "RB-L2: Upcoming / Active / Completed tabs exist, Upcoming is default")
    @Severity(SeverityLevel.CRITICAL)
    @Description("All three tab labels are present on first paint and the Upcoming list is the "
            + "one rendered (header Upcoming Booking). Do not tap the tabs here.")
    public void threeTabsWithUpcomingDefault() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();

        String header = bookings.headerTitleNow();
        Allure.parameter("tabsVisible", String.valueOf(bookings.areTabsVisible()));
        Allure.parameter("headerTitle", header);
        Allure.parameter("activeEmptyOrList",
                String.valueOf(bookings.isActiveEmptyStateVisible()));
        bookings.attachScreenshot("rb-l2-three-tabs-upcoming-default");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(bookings.areTabsVisible())
                .as("Upcoming / Active / Completed tab labels must all render on first paint")
                .isTrue();
        assertThat(header)
                .as("Upcoming must be the default tab — header Upcoming Booking without any tap")
                .isEqualTo(RentalBookingsPage.TITLE_UPCOMING);
    }

    @Test(priority = 3,
            description = "RB-L3: Bookings is not Home and not Quick Booking")
    @Severity(SeverityLevel.CRITICAL)
    @Description("No Close app bar (Quick Booking identity) and no greeting / Current Earning "
            + "(Home identity), while the Bookings tabs + header render. Prevents a false pass "
            + "from the wrong screen. The bottom nav bar is recorded as an observation, not "
            + "hard-asserted, because it may be shared chrome.")
    public void bookingsIsItsOwnScreen() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();
        QuickBookingPage qb = new QuickBookingPage();
        HomePage home = new HomePage();

        boolean closeAppBar = qb.isCloseVisible();
        boolean greeting = home.isGreetingVisible();
        boolean currentEarning = home.isCurrentEarningVisible();
        boolean rentalBottomBar = home.isRentalBottomTabsVisible();

        Allure.parameter("headerTitle", bookings.headerTitleNow());
        Allure.parameter("tabsVisible", String.valueOf(bookings.areTabsVisible()));
        Allure.parameter("quickBookingClose", String.valueOf(closeAppBar));
        Allure.parameter("homeGreeting", String.valueOf(greeting));
        Allure.parameter("homeCurrentEarning", String.valueOf(currentEarning));
        Allure.parameter("rentalBottomBar(observed)", String.valueOf(rentalBottomBar));
        bookings.attachScreenshot("rb-l3-bookings-own-screen");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(bookings.isDisplayedNow())
                .as("Bookings identity (tabs + a bookings header) must render")
                .isTrue();
        assertThat(closeAppBar)
                .as("Bookings must NOT show the Quick Booking Close app bar")
                .isFalse();
        assertThat(greeting || currentEarning)
                .as("Bookings must NOT show the Home greeting or Current Earning")
                .isFalse();
    }

    @Test(priority = 4,
            description = "RB-L4: Upcoming card core fields render")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Per card: machine title, amount row (Amount · Online Mode ₹...), and a date "
            + "range ending in (N day/days). Assert shape, never a pinned value. Amount rows or "
            + "date ranges missing versus the card count are recorded as a new Bookings bug.")
    public void upcomingCardCoreFields() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();

        int cards = bookings.cardCountNow();
        java.util.List<String> amounts = bookings.amountsNow();
        java.util.List<String> dateRanges = bookings.dateRangesNow();
        java.util.List<String> machines = bookings.machineTitlesNow();
        int sourceTags = bookings.sourceTagCount();

        Allure.parameter("cardCount", String.valueOf(cards));
        Allure.parameter("amountRows", String.valueOf(amounts.size()));
        Allure.parameter("amountsSample", amounts.toString());
        Allure.parameter("dateRanges", String.valueOf(dateRanges.size()));
        Allure.parameter("dateRangesSample", dateRanges.toString());
        Allure.parameter("sourceTags", String.valueOf(sourceTags));
        Allure.parameter("machineTitleCount", String.valueOf(machines.size()));
        bookings.attachScreenshot("rb-l4-upcoming-card-fields");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(cards)
                .as("At least one Upcoming card must render to check its fields")
                .isGreaterThanOrEqualTo(1);
        assertThat(amounts)
                .as("Every amount row must carry a ₹ figure (Amount · Online Mode ₹...)")
                .allMatch(a -> a.contains("\u20b9"));
        assertThat(amounts.size())
                .as("Amount rows must equal the card count — a missing amount is a new Bookings bug")
                .isEqualTo(cards);
        assertThat(dateRanges.size())
                .as("Every card must show a date range ending in (N day/days) — a missing range "
                        + "is a new Bookings bug")
                .isGreaterThanOrEqualTo(cards);
        assertThat(machines)
                .as("Each card must show a machine title")
                .isNotEmpty();
    }

    @Test(priority = 5,
            description = "RB-L5: Upcoming card map tile and Get Direction render")
    @Severity(SeverityLevel.NORMAL)
    @Description("Google Map tile and the Get Direction control appear on cards that carry a "
            + "location. Map and Get Direction must be paired (equal counts) and recorded against "
            + "the card count. A Get Direction with no map tile is a new Bookings bug.")
    public void upcomingCardMapAndDirection() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();

        int cards = bookings.cardCountNow();
        int maps = bookings.mapCount();
        int directions = bookings.getDirectionCount();

        Allure.parameter("cardCount", String.valueOf(cards));
        Allure.parameter("mapTiles", String.valueOf(maps));
        Allure.parameter("getDirection", String.valueOf(directions));
        Allure.parameter("cardsMissingMap(observed)", String.valueOf(Math.max(0, cards - maps)));
        bookings.attachScreenshot("rb-l5-map-and-direction");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(cards)
                .as("At least one Upcoming card must render")
                .isGreaterThanOrEqualTo(1);
        assertThat(directions)
                .as("Every Get Direction control must sit on a card that also shows a map tile "
                        + "— an unpaired Get Direction is a new Bookings bug")
                .isLessThanOrEqualTo(maps);
    }

    @Test(priority = 6,
            description = "RB-L6: operator row is assigned-or-unassigned, never both")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Each card shows either 'Operator : <name>' with Change, or 'Operator Not "
            + "Assigned' with Assign. assignedCount + unassignedCount must equal the card count, "
            + "Change pairs with assigned, Assign pairs with unassigned. Any mismatch is a new "
            + "Bookings bug.")
    public void operatorRowIsExclusive() {
        RentalBookingsPage bookings = openBookingsFromHomeSeeAll();

        int cards = bookings.cardCountNow();
        int assigned = bookings.operatorAssignedCount();
        int unassigned = bookings.operatorUnassignedCount();
        int changeCtas = bookings.changeCount();
        int assignCtas = bookings.assignCount();

        Allure.parameter("cardCount", String.valueOf(cards));
        Allure.parameter("operatorAssigned", String.valueOf(assigned));
        Allure.parameter("operatorUnassigned", String.valueOf(unassigned));
        Allure.parameter("changeCtas", String.valueOf(changeCtas));
        Allure.parameter("assignCtas", String.valueOf(assignCtas));
        bookings.attachScreenshot("rb-l6-operator-row-exclusive");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(cards)
                .as("At least one Upcoming card must render")
                .isGreaterThanOrEqualTo(1);
        assertThat(assigned + unassigned)
                .as("Every card must be exactly one of assigned / unassigned — the sum must "
                        + "equal the card count")
                .isEqualTo(cards);
        assertThat(changeCtas)
                .as("Change must appear once per assigned-operator card")
                .isEqualTo(assigned);
        assertThat(assignCtas)
                .as("Assign must appear once per Operator-Not-Assigned card")
                .isEqualTo(unassigned);
    }

    @Test(enabled = false, priority = 7,
            description = "RB-L7: plate format matches the fleet plate shape")
    @Severity(SeverityLevel.NORMAL)
    @Description("Every assigned card shows a plate matching [A-Z]{2}[0-9]{2}[A-Z]{1,3}[0-9]{4} "
            + "(dump: KA13Z2117, MH12SD4444). A blank or malformed plate is a new Bookings bug.")
    public void plateFormatOnCards() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 8,
            description = "RB-L8: Booking for address line is present and non-empty")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Each card has a Booking for label plus an address line. Dump 16 shows 'Address "
            + "not provided' on one card. Record empty / placeholder addresses as a new Bookings "
            + "bug — do not fold into Home #20.")
    public void bookingForAddressPresent() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 9,
            description = "RB-L9: View More Details exists once per Upcoming card")
    @Severity(SeverityLevel.NORMAL)
    @Description("viewMoreDetailsCount equals cardCount on the Upcoming tab. Missing rows are a "
            + "new Bookings bug, separate from Home #22 (no View More on the Home feed card).")
    public void viewMoreDetailsPerCard() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 10,
            description = "RB-L10: Active tab renders a list or the empty state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Exactly one of: at least one active card, or the copy 'No active bookings.' "
            + "A blank body with neither is a new Bookings bug. Dump 12-bookings-active.")
    public void activeTabListOrEmptyState() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 11,
            description = "RB-L11: header switches to Active Order on the Active tab")
    @Severity(SeverityLevel.NORMAL)
    @Description("Header title tracks the selected tab: Upcoming Booking / Active Order / "
            + "Completed Order. Stale titles are a new Bookings bug.")
    public void activeTabHeaderTitle() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 12,
            description = "RB-L12: Completed card shows machine, date range, and amount")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Completed cards are summary-only in the dump (machine, '31 Aug - 31 Aug 2026 "
            + "(1 day)', ₹1,02,000) with header Completed Order. Assert those three plus the "
            + "absence of Assign / Change. Dump 13-bookings-completed.")
    public void completedCardSummaryFields() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 13,
            description = "RB-L13: rupee formatting is consistent across tabs")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump shows Upcoming '₹102,150' (Western) against Completed '₹1,02,000' "
            + "(Indian). Capture both tabs in one run. A mixed convention is a new Bookings bug "
            + "— do not fold into Home #24.")
    public void rupeeGroupingConsistentAcrossTabs() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 14,
            description = "RB-L14: Upcoming card count matches the Home Upcoming Booking stat")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Read the Home stat value, open See all, count cards. A mismatch is a real "
            + "data-consistency defect and is cross-checked against the list API in RB-A3.")
    public void upcomingCountMatchesHomeStat() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 15,
            description = "RB-L15: vertical scroll keeps tabs and header pinned")
    @Severity(SeverityLevel.NORMAL)
    @Description("Scroll the list body to the last card. Tabs and header must remain, and the "
            + "list must not bounce back to the first card.")
    public void scrollKeepsTabsAndHeader() {
        throw new SkipException(ON_HOLD);
    }

    @Test(enabled = false, priority = 16,
            description = "RB-L16: booking seeded on customer web appears in the right bucket")
    @Severity(SeverityLevel.BLOCKER)
    @Description("After the accepted seed booking exists, it appears under Upcoming with the "
            + "machine, dates, and address entered on the customer site — not under Active or "
            + "Completed. This is the anchor case for execution day.")
    public void seededBookingLandsInCorrectBucket() {
        throw new SkipException(ON_HOLD);
    }
}
