package com.l2b.vendor.modules.quickbooking.presentation.tests;

import com.l2b.vendor.core.ui.BaseTest;
import com.l2b.vendor.modules.quickbooking.domain.QuickBookingEnvironment;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.BeforeSuite;

/**
 * Quick Booking details (card → View More Details). Isolated
 * {@code src/test/resources/quickbooking/details.xml}. Not in default
 * {@code testng.xml} until review. No cases yet.
 */
@Epic("Vendor app")
@Feature("Quick Booking details")
public class QuickBookingDetailsTest extends BaseTest {

    @BeforeSuite(alwaysRun = true)
    public void prepareQuickBookingEnvironment() {
        QuickBookingEnvironment.prepareSuite();
    }

    @Override
    protected boolean noReset() {
        return QuickBookingEnvironment.noReset();
    }

    @Override
    protected boolean newSessionPerMethod() {
        return QuickBookingEnvironment.newSessionPerMethod();
    }
}
