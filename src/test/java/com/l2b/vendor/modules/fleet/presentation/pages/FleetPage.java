package com.l2b.vendor.modules.fleet.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import org.openqa.selenium.By;

/**
 * Rental Fleet — {@code Your Fleet} / {@code Add Machine}. Live dump 21 Sep / 25 Sep
 * on {@code 9000000001}. Bottom tabs stay (Calendar · Home · Earning · Fleet).
 * Same surface as Home Active Fleet See all and drawer Your machines.
 */
public class FleetPage extends SplashScreen {

    @Override
    @Step("Wait for Fleet")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Fleet screen did not appear", Duration.ofSeconds(12));
    }

    @Step("Check Fleet identity (Your Fleet or Add Machine)")
    public boolean isDisplayedNow() {
        return isPresent(ComposeLocators.textView("Your Fleet"))
                || isPresent(ComposeLocators.textView("Add Machine"))
                || isPresent(ComposeLocators.textView("No Machines Yet"));
    }

    @Step("Check Add Machine is visible")
    public boolean isAddMachineVisible() {
        return isPresent(ComposeLocators.textView("Add Machine"))
                || isPresent(By.xpath("//*[@content-desc='Add Machine']"));
    }

    @Step("Check empty fleet copy")
    public boolean isEmptyStateVisible() {
        return isPresent(ComposeLocators.textView("No Machines Yet"))
                || isPresent(ComposeLocators.textView("No Machines"));
    }

    @Step("Check Status Active label on list")
    public boolean hasActiveStatus() {
        return isPresent(ComposeLocators.textView("Active"))
                || isPresent(ComposeLocators.textView("Status Active"));
    }

    @Step("Check Change operator affordance")
    public boolean hasChangeOperator() {
        return isPresent(ComposeLocators.textView("Change operator"))
                || isPresent(ComposeLocators.textView("Change Operator"))
                || isPresent(By.xpath("//*[contains(@text,'Change') and contains(@text,'perator')]"));
    }

    @Step("Check rental bottom tabs still visible on Fleet")
    public boolean areBottomTabsVisible() {
        return isPresent(By.xpath("//*[@content-desc='Calendar']"))
                && isPresent(By.xpath("//*[@content-desc='Home']"))
                && isPresent(By.xpath("//*[@content-desc='Earning']"))
                && isPresent(By.xpath("//*[@content-desc='Fleet']"));
    }

    @Step("Count machine plate-like TextViews (KA/MH prefix heuristic)")
    public int visiblePlateCount() {
        return driver.findElements(By.xpath(
                "//android.widget.TextView[starts-with(@text,'KA') or starts-with(@text,'MH') "
                        + "or starts-with(@text,'KA ') or starts-with(@text,'MH ')]"
        )).size();
    }
}
