package com.l2b.vendor.modules.quickbooking.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.bookings.presentation.tests.quickbooking.QuickBookingBaseTest;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;
import java.time.Duration;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Material vendor {@code 9000000017} Quick Booking landing. Dump 19 Sep 2026
 * ({@code /tmp/l2b-qb-material-0017-20260919}). Isolated
 * {@code quickbooking/material-landing.xml}. Do not tap Accept or Decline.
 * Do not log in as 0001 / 0002 / 0003.
 */
@Epic("Vendor app")
@Feature("Quick Booking landing — material vendor 9000000017")
public class MaterialQuickBookingLandingTest extends QuickBookingBaseTest {

    @Test(priority = 1, description = "Material-Landing-1: pending_vendor_acceptance opens Quick Booking")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017 + 1234. Pending material orders must open Quick Booking "
            + "(title + Close), not Home. Do not tap Accept/Decline.")
    public void pendingLandsOnQuickBooking() {
        QuickBookingPage page = loginToMaterialLanding();
        HomePage home = new HomePage();

        Allure.parameter("phone", materialPhone());
        Allure.parameter("package", String.valueOf(vendorPackage()));
        Allure.parameter("quickBookingVisible", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("homeVisible", String.valueOf(home.isDisplayedNow()));
        page.attachScreenshot("material-landing-1-after-login");

        assertThat(materialPhone()).isEqualTo("9000000017");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isDisplayedNow()).as("Quick Booking title + Close after login").isTrue();
        assertThat(page.isCloseVisible()).as("Close").isTrue();
        assertThat(page.isAcceptVisible()).as("At least one pending card (Accept)").isTrue();
        assertThat(home.isDisplayedNow()).as("Must not skip queue to Home").isFalse();
    }

    @Test(priority = 2, description = "Material-Landing-2: two pending material cards")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017. First screen must show two pending cards (Accept >= 2, "
            + "matching Decline, both pickup dates). Do not tap Accept/Decline.")
    public void twoPendingCardsVisible() {
        QuickBookingPage page = loginToMaterialLanding();

        int accept = page.acceptCount();
        int decline = page.declineCount();
        int viewMore = page.viewMoreDetailsCount();
        Allure.parameter("acceptCount", String.valueOf(accept));
        Allure.parameter("declineCount", String.valueOf(decline));
        Allure.parameter("viewMoreCount", String.valueOf(viewMore));
        Allure.parameter("pickup11Sep", String.valueOf(page.hasText("11 Sep 2026")));
        Allure.parameter("pickup10Sep", String.valueOf(page.hasText("10 Sep 2026")));
        page.attachScreenshot("material-landing-2-two-cards");

        assertThat(page.isDisplayedNow()).as("Still on Quick Booking").isTrue();
        assertThat(accept).as("Two pending material cards").isGreaterThanOrEqualTo(2);
        assertThat(decline).as("Each card has Decline").isEqualTo(accept);
        assertThat(viewMore).as("Each card has View More Details").isGreaterThanOrEqualTo(2);
        assertThat(page.hasText("11 Sep 2026")).as("MAT-20260909-2556 pickup").isTrue();
        assertThat(page.hasText("10 Sep 2026")).as("MAT-20260908-2000 pickup").isTrue();
    }

    @Test(priority = 3, description = "Material-Landing-3: card chrome is material, not rental")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Collapsed card: Order Placed, Fast Delivery, Pickup scheduled, Booking for. "
            + "Must not show rental Timer, Amount · Online Mode, or Get Direction. "
            + "Do not tap Accept/Decline.")
    public void cardShowsMaterialChromeNotRental() {
        QuickBookingPage page = loginToMaterialLanding();

        Allure.parameter("orderPlaced", String.valueOf(page.isOrderPlacedVisible()));
        Allure.parameter("fastDelivery", String.valueOf(page.isFastDeliveryVisible()));
        Allure.parameter("pickup", String.valueOf(page.isPickupScheduledVisible()));
        Allure.parameter("bookingFor", String.valueOf(page.isBookingForVisible()));
        Allure.parameter("rentalAmount", String.valueOf(page.isRentalAmountChromeVisible()));
        Allure.parameter("timer", String.valueOf(page.isTimerVisible()));
        Allure.parameter("getDirection", String.valueOf(page.isGetDirectionVisible()));
        page.attachScreenshot("material-landing-3-chrome");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isOrderPlacedVisible()).as("Order Placed").isTrue();
        assertThat(page.isFastDeliveryVisible()).as("Fast Delivery").isTrue();
        assertThat(page.isPickupScheduledVisible()).as("Pickup scheduled").isTrue();
        assertThat(page.isBookingForVisible()).as("Booking for").isTrue();
        assertThat(page.isRentalAmountChromeVisible()).as("Must not show Amount · Online Mode").isFalse();
        assertThat(page.isTimerVisible()).as("Must not show rental Timer").isFalse();
        assertThat(page.isGetDirectionVisible()).as("Must not show Get Direction").isFalse();
    }

    @Test(priority = 4, description = "Material-Landing-4: View More expands bill summary")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap View More on first card. Must show item + Bill Summary. "
            + "Do not tap Accept/Decline.")
    public void viewMoreExpandsBillSummary() {
        QuickBookingPage page = loginToMaterialLanding();
        QuickBookingLandingPage landing = new QuickBookingLandingPage();
        landing.tapFirstViewMore();
        Waits.until(DriverManager.get(),
                d -> landing.isViewLessVisible() || page.hasText("Bill Summary") ? Boolean.TRUE : null,
                "View More did not expand the first material card",
                Duration.ofSeconds(8));

        Allure.parameter("viewLess", String.valueOf(landing.isViewLessVisible()));
        Allure.parameter("billSummary", String.valueOf(page.hasText("Bill Summary")));
        Allure.parameter("aacBlock", String.valueOf(page.hasText("AAC Block")));
        Allure.parameter("totalBill", String.valueOf(page.hasText("₹ 6,590")));
        Allure.parameter("assignMachine", String.valueOf(page.hasText("Assign machine")));
        page.attachScreenshot("material-landing-4-view-more");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(landing.isViewLessVisible()).as("View Less after expand").isTrue();
        assertThat(page.hasText("Bill Summary")).as("Bill Summary").isTrue();
        assertThat(page.hasText("AAC Block")).as("First item AAC Block - Grade 1").isTrue();
        assertThat(page.hasText("Total Bill")).as("Total Bill").isTrue();
        assertThat(page.hasText("Assign machine")).as("Assign machine must not appear on expand").isFalse();
    }

    @Test(priority = 5, description = "Material-Landing-5: View Less collapses the first card")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Expand first card, tap View Less. Details must collapse. Do not tap Accept/Decline.")
    public void viewLessCollapsesFirstCard() {
        QuickBookingPage page = loginToMaterialLanding();
        QuickBookingLandingPage landing = new QuickBookingLandingPage();
        landing.tapFirstViewMore();
        Waits.until(DriverManager.get(),
                d -> landing.isViewLessVisible() ? Boolean.TRUE : null,
                "View Less did not appear after View More",
                Duration.ofSeconds(8));
        landing.tapFirstViewLess();
        Waits.until(DriverManager.get(),
                d -> landing.isViewMoreVisible() && !landing.isViewLessVisible() ? Boolean.TRUE : null,
                "View More did not return after View Less",
                Duration.ofSeconds(8));

        Allure.parameter("viewMoreAfter", String.valueOf(landing.isViewMoreVisible()));
        Allure.parameter("viewLessAfter", String.valueOf(landing.isViewLessVisible()));
        Allure.parameter("billAfter", String.valueOf(page.hasText("Bill Summary")));
        page.attachScreenshot("material-landing-5-view-less");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(landing.isViewMoreVisible()).as("View More back after collapse").isTrue();
        assertThat(landing.isViewLessVisible()).as("View Less gone after collapse").isFalse();
    }

    @Test(priority = 6, description = "Material-Landing-6: expand does not change the other card")
    @Severity(SeverityLevel.NORMAL)
    @Description("Expand first card. Second card stays collapsed (10 Sep + View More). "
            + "Do not tap Accept/Decline.")
    public void expandDoesNotChangeOtherCard() {
        QuickBookingPage page = loginToMaterialLanding();
        QuickBookingLandingPage landing = new QuickBookingLandingPage();
        landing.tapFirstViewMore();
        Waits.until(DriverManager.get(),
                d -> landing.isViewLessVisible() ? Boolean.TRUE : null,
                "View Less did not appear after View More",
                Duration.ofSeconds(8));

        int viewLess = landing.viewLessDetailsCount();
        int viewMore = landing.viewMoreDetailsCount();
        Allure.parameter("viewLessCount", String.valueOf(viewLess));
        Allure.parameter("viewMoreCount", String.valueOf(viewMore));
        Allure.parameter("secondPickup", String.valueOf(page.hasText("10 Sep 2026")));
        page.attachScreenshot("material-landing-6-other-card");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(viewLess).as("Only the tapped card shows View Less").isEqualTo(1);
        assertThat(viewMore).as("Second card still View More").isGreaterThanOrEqualTo(1);
        assertThat(page.hasText("10 Sep 2026")).as("Second card still on screen").isTrue();
    }

    /**
     * Title+Close can appear while copy is still {@code Loading your orders…}.
     * Wait for a card before asserting chrome. Do not tap Accept/Decline.
     */
    private QuickBookingPage loginToMaterialLanding() {
        QuickBookingPage page = loginToQuickBooking(materialPhone());
        Waits.until(DriverManager.get(),
                d -> page.isAcceptVisible() || page.isOrderPlacedVisible() ? Boolean.TRUE : null,
                "Material cards did not load after Quick Booking title",
                Duration.ofSeconds(15));
        return page;
    }
}
