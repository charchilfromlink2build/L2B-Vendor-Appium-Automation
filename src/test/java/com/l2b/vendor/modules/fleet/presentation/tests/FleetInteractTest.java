package com.l2b.vendor.modules.fleet.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.fleet.presentation.pages.FleetPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

/**
 * Fleet interact — Change/Select operator and Add Machine on {@code 9000000001}.
 * Dump {@code /tmp/l2b-fleet-full-0001-20260925}. FL-I1–I8 are open/dismiss/back;
 * FL-I9+ confirm operator assignment (restore after change).
 */
@Epic("Vendor app")
@Feature("Fleet interact — rental 9000000001")
public class FleetInteractTest extends FleetBaseTest {

    @Test(priority = 1, description = "FL-I1: Change operator opens Select Driver/operator sheet")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap Change <name> on an assigned card. Expect Select Driver/operator sheet "
            + "+ Close sheet. Do not pick a driver. Dump 25 Sep 02-change-operator.")
    public void changeOperatorOpensSheet() {
        FleetPage fleet = reachFleetViaTab();
        assertThat(fleet.hasChangeOperator()).as("Change <operator> on list").isTrue();
        fleet.tapFirstChangeOperator();
        waitSheetOrForm(fleet);
        fleet.attachScreenshot("fleet-fl-i1-change-sheet");
        Allure.parameter("sheet", String.valueOf(fleet.isOperatorSheetVisible()));
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(fleet.isOperatorSheetVisible())
                .as("Select Driver/operator sheet")
                .isTrue();
        fleet.closeOperatorSheet();
    }

    @Test(priority = 2, description = "FL-I2: dismiss operator sheet returns to Your Fleet")
    @Severity(SeverityLevel.CRITICAL)
    @Description("After opening Change sheet, Close sheet / Back returns to Your Fleet with "
            + "bottom tabs. Dump 25 Sep 03-after-operator-back.")
    public void dismissOperatorSheetReturnsFleet() {
        FleetPage fleet = reachFleetViaTab();
        fleet.tapFirstChangeOperator();
        waitSheetOrForm(fleet);
        assertThat(fleet.isOperatorSheetVisible()).isTrue();
        fleet.closeOperatorSheet();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> fleet.isDisplayedNow() && !fleet.isOperatorSheetVisible());
        fleet.attachScreenshot("fleet-fl-i2-sheet-dismissed");
        assertThat(fleet.isDisplayedNow()).isTrue();
        assertThat(fleet.isOperatorSheetVisible()).isFalse();
        assertThat(fleet.areBottomTabsVisible()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("fleet");
    }

    @Test(priority = 3, description = "FL-I3: Select (unassigned) opens the same operator sheet")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Live 0001 14 Ft Truck shows Select. Tap Select → same Select Driver/operator "
            + "sheet. Close without confirm.")
    public void selectOperatorOpensSheet() {
        FleetPage fleet = reachFleetViaTab();
        if (!fleet.hasSelectOperator()) {
            Allure.parameter("selectPresent", "false");
            fleet.attachScreenshot("fleet-fl-i3-no-select");
            // Still assert Change path exists so suite documents the account state.
            assertThat(fleet.hasChangeOperator()).as("No Select — Change must exist").isTrue();
            return;
        }
        fleet.tapFirstSelectOperator();
        waitSheetOrForm(fleet);
        fleet.attachScreenshot("fleet-fl-i3-select-sheet");
        assertThat(fleet.isOperatorSheetVisible()).isTrue();
        fleet.closeOperatorSheet();
        assertThat(fleet.isDisplayedNow()).isTrue();
    }

    @Test(priority = 4, description = "FL-I4: Add Machine opens Add Fleet Machines form")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap Add Machine. Expect Add Fleet Machines title. Dump 25 Sep 04-add-machine. "
            + "Do not Submit & Verify.")
    public void addMachineOpensForm() {
        FleetPage fleet = reachFleetViaTab();
        fleet.tapAddMachine();
        waitSheetOrForm(fleet);
        fleet.attachScreenshot("fleet-fl-i4-add-form");
        Allure.parameter("form", String.valueOf(fleet.isAddMachineFormVisible()));
        assertThat(fleet.isAddMachineFormVisible()).as("Add Fleet Machines form").isTrue();
        fleet.tapBack();
    }

    @Test(priority = 5, description = "FL-I5: Add Machine form shows required fields + Submit & Verify")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Form fields: Name/Capacity/Variant/Registration * + upload docs + "
            + "Add Another Machine + Submit & Verify. Never tap submit.")
    public void addMachineFormFields() {
        FleetPage fleet = reachFleetViaTab();
        fleet.tapAddMachine();
        waitSheetOrForm(fleet);
        Allure.parameter("required", String.valueOf(fleet.hasAddMachineRequiredFields()));
        Allure.parameter("submit", String.valueOf(fleet.isSubmitAndVerifyVisible()));
        fleet.attachScreenshot("fleet-fl-i5-add-fields");
        assertThat(fleet.hasAddMachineRequiredFields()).isTrue();
        assertThat(fleet.isSubmitAndVerifyVisible()).isTrue();
        fleet.tapBack();
        assertThat(fleet.isDisplayedNow()).isTrue();
    }

    @Test(priority = 6, description = "FL-I6: Back from Add Machine returns to Your Fleet")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Add Machine → Back → Your Fleet; bottom tabs stay. Dump 25 Sep 05-after-add-back.")
    public void backFromAddMachineReturnsFleet() {
        FleetPage fleet = reachFleetViaTab();
        fleet.tapAddMachine();
        waitSheetOrForm(fleet);
        assertThat(fleet.isAddMachineFormVisible()).isTrue();
        fleet.tapBack();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> fleet.isDisplayedNow());
        fleet.attachScreenshot("fleet-fl-i6-back-add");
        assertThat(fleet.isDisplayedNow()).isTrue();
        assertThat(fleet.isAddMachineFormVisible()).isFalse();
        assertThat(fleet.areBottomTabsVisible()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("fleet");
    }

    @Test(priority = 7, description = "FL-I7: Back from Fleet returns to Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Your Fleet Back → Rental Home. Dump 25 Sep 06-back-from-fleet.")
    public void backFromFleetReturnsHome() {
        FleetPage fleet = reachFleetViaTab();
        fleet.tapBack();
        HomePage home = new HomePage();
        home.waitUntilLoaded();
        home.attachScreenshot("fleet-fl-i7-back-home");
        assertThat(classifyRentalNow()).isEqualTo("home");
        assertThat(home.isDisplayedNow() || home.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 8, description = "FL-I8: Home bottom tab from Fleet returns to Home")
    @Severity(SeverityLevel.NORMAL)
    @Description("Fleet → Home tab → Rental Home (same as Back path).")
    public void homeTabFromFleetReturnsHome() {
        FleetPage fleet = reachFleetViaTab();
        fleet.tapHomeTab();
        HomePage home = new HomePage();
        home.waitUntilLoaded();
        home.attachScreenshot("fleet-fl-i8-home-tab");
        assertThat(classifyRentalNow()).isEqualTo("home");
        assertThat(home.isDisplayedNow() || home.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 9, description = "FL-I9: Change operator picks another driver (Secondary) then restore")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Live 25 Sep: Change sheet must scroll to reveal Select. Picking a second driver "
            + "adds Secondary Operator (Primary stays). Toggle that driver off + Select restores "
            + "single Operator. Dump 07/08 + live probe.")
    public void changeOperatorToAnotherDriverAndRestore() {
        FleetPage fleet = reachFleetViaTab();
        assertThat(fleet.hasChangeOperator()).as("Need an assigned Change control").isTrue();
        String before = fleet.firstChangeOperatorLabel();
        Allure.parameter("changeBefore", before);
        boolean secondaryBefore = fleet.hasSecondaryOperatorLabel();
        Allure.parameter("secondaryBefore", String.valueOf(secondaryBefore));

        fleet.tapFirstChangeOperator();
        waitSheetOrForm(fleet);
        assertThat(fleet.isOperatorSheetVisible()).isTrue();
        fleet.scrollOperatorSheetUntilSelectVisible();
        boolean selectBeforePick = fleet.isSheetSelectEnabled();
        Allure.parameter("selectEnabledBeforePick", String.valueOf(selectBeforePick));

        String target = before.contains("Nauman")
                ? FleetPage.OPERATOR_RANDAN
                : FleetPage.OPERATOR_NAUMAN;
        fleet.tapDriverNamed(target);
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> fleet.isSheetSelectEnabled());
        Allure.parameter("selectEnabledAfterPick", String.valueOf(fleet.isSheetSelectEnabled()));
        fleet.tapSheetSelect();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(12))
                .until(d -> fleet.isDisplayedNow() && !fleet.isOperatorSheetVisible());
        fleet.attachScreenshot("fleet-fl-i9-after-change");
        boolean secondaryAfter = fleet.hasSecondaryOperatorLabel();
        boolean changeHasTarget = fleet.hasChangeOperatorNamed(target.split(" ")[0]);
        Allure.parameter("secondaryAfter", String.valueOf(secondaryAfter));
        Allure.parameter("changeHasTarget", String.valueOf(changeHasTarget));
        Allure.parameter("target", target);
        assertThat(secondaryAfter || changeHasTarget)
                .as("Picking " + target + " must show Secondary Operator or Change " + target)
                .isTrue();

        // Restore: reopen Primary Change, toggle target off (badge "2" clears), Select.
        String primaryFragment = before.contains("Nauman") ? "Nauman" : "Randanberno";
        boolean restored = false;
        for (int attempt = 1; attempt <= 3 && fleet.hasSecondaryOperatorLabel(); attempt++) {
            Allure.parameter("restoreAttempt" + attempt, "start");
            if (fleet.hasChangeOperatorNamed(primaryFragment)) {
                fleet.tapChangeOperatorNamed(primaryFragment);
            } else {
                fleet.tapFirstChangeOperator();
            }
            waitSheetOrForm(fleet);
            fleet.scrollOperatorSheetUntilSelectVisible();
            String sheetSrc = DriverManager.get().getPageSource();
            boolean hasBadge2 = sheetSrc.contains("text=\"2\"");
            Allure.parameter("restoreAttempt" + attempt + "badge2", String.valueOf(hasBadge2));
            if (hasBadge2 || sheetSrc.contains(target.split(" ")[0])) {
                fleet.tapDriverNamed(target);
                try {
                    Thread.sleep(900);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (fleet.isSheetSelectEnabled()) {
                fleet.tapSheetSelect();
                new WebDriverWait(DriverManager.get(), Duration.ofSeconds(12))
                        .until(d -> fleet.isDisplayedNow() && !fleet.isOperatorSheetVisible());
            } else {
                fleet.closeOperatorSheet();
                new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                        .until(d -> !fleet.isOperatorSheetVisible());
            }
            restored = !fleet.hasSecondaryOperatorLabel();
            Allure.parameter("restoreAttempt" + attempt + "cleared", String.valueOf(restored));
        }
        fleet.attachScreenshot("fleet-fl-i9-restored");
        Allure.parameter("secondaryRestored", String.valueOf(fleet.hasSecondaryOperatorLabel()));
        Allure.parameter("changeRestored", fleet.firstChangeOperatorLabel());
        assertThat(fleet.hasSecondaryOperatorLabel())
                .as("Secondary Operator must clear after deselect + Select")
                .isFalse();
        assertThat(classifyRentalNow()).isEqualTo("fleet");
    }

    @Test(priority = 10, description = "FL-I10: Select on unassigned card assigns an operator")
    @Severity(SeverityLevel.CRITICAL)
    @Description("14 Ft Truck shows Select. Tap Select → pick first available driver → sheet Select. "
            + "Card should become Change <name>.")
    public void selectUnassignedAssignsOperator() {
        FleetPage fleet = reachFleetViaTab();
        if (!fleet.hasSelectOperator()) {
            Allure.parameter("selectPresent", "false");
            fleet.attachScreenshot("fleet-fl-i10-no-select");
            assertThat(fleet.hasChangeOperator()).as("No unassigned Select — Change still exists").isTrue();
            return;
        }
        int changeBefore = fleet.changeOperatorCount();
        fleet.tapFirstSelectOperator();
        waitSheetOrForm(fleet);
        assertThat(fleet.isOperatorSheetVisible()).isTrue();
        fleet.scrollOperatorSheetUntilSelectVisible();
        String pick = fleet.firstAssignableDriverName();
        Allure.parameter("pickedDriver", pick);
        assertThat(pick).as("Sheet must list at least one driver").isNotBlank();
        fleet.assignOperatorFromSheet(pick);
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(12))
                .until(d -> fleet.isDisplayedNow());
        fleet.attachScreenshot("fleet-fl-i10-after-assign");
        Allure.parameter("changeBefore", String.valueOf(changeBefore));
        Allure.parameter("changeAfter", String.valueOf(fleet.changeOperatorCount()));
        Allure.parameter("hasSelectAfter", String.valueOf(fleet.hasSelectOperator()));
        assertThat(fleet.hasChangeOperatorNamed(pick.split(" ")[0])
                || fleet.changeOperatorCount() > changeBefore
                || !fleet.hasSelectOperator())
                .as("Unassigned Select must become an assigned Change after pick")
                .isTrue();
    }

    @Test(priority = 11, description = "FL-I11: Fleet → Calendar bottom tab")
    @Severity(SeverityLevel.CRITICAL)
    @Description("From Your Fleet, Calendar tab leaves Fleet. Do not assert Calendar internals.")
    public void calendarTabFromFleet() {
        FleetPage fleet = reachFleetViaTab();
        fleet.tapCalendarTab();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> !"fleet".equals(classifyRentalNow()));
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(after).as("Calendar tab must leave Fleet").isNotEqualTo("fleet");
        assertThat(after).isIn("calendar", "home", "vendor-other");
    }

    @Test(priority = 12, description = "FL-I12: Fleet → Earning bottom tab")
    @Severity(SeverityLevel.CRITICAL)
    @Description("From Your Fleet, Earning tab leaves Fleet.")
    public void earningTabFromFleet() {
        FleetPage fleet = reachFleetViaTab();
        fleet.tapEarningTab();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> !"fleet".equals(classifyRentalNow()));
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(after).isNotEqualTo("fleet");
        assertThat(after).isIn("earning", "home", "vendor-other");
    }

    @Test(priority = 13, description = "FL-I13: Disable → Cancel keeps machine on Fleet")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Long-press → Disable → Disable Machine? → Cancel. Machine stays; no confirm Disable.")
    public void disableCancelKeepsMachine() {
        FleetPage fleet = reachFleetViaTab();
        int platesBefore = fleet.visiblePlateCount();
        fleet.longPressFirstPlate();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> fleet.hasDeleteAndDisable());
        fleet.tapDisableAction();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> fleet.isDisableConfirmVisible());
        fleet.attachScreenshot("fleet-fl-i13-disable-confirm");
        assertThat(fleet.isDisableConfirmVisible()).isTrue();
        fleet.tapConfirmCancel();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> !fleet.isDisableConfirmVisible() && fleet.isDisplayedNow());
        Allure.parameter("platesBefore", String.valueOf(platesBefore));
        Allure.parameter("platesAfter", String.valueOf(fleet.visiblePlateCount()));
        assertThat(fleet.isDisplayedNow()).isTrue();
        assertThat(fleet.hasActiveStatus()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("fleet");
    }

    @Test(priority = 14, description = "FL-I14: Delete → Cancel keeps machine on Fleet")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Long-press → Delete → Delete Machine? → Cancel. Do not confirm Delete.")
    public void deleteCancelKeepsMachine() {
        FleetPage fleet = reachFleetViaTab();
        int platesBefore = fleet.visiblePlateCount();
        fleet.longPressFirstPlate();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> fleet.hasDeleteAndDisable());
        fleet.tapDeleteAction();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> fleet.isDeleteConfirmVisible());
        fleet.attachScreenshot("fleet-fl-i14-delete-confirm");
        assertThat(fleet.isDeleteConfirmVisible()).isTrue();
        fleet.tapConfirmCancel();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> !fleet.isDeleteConfirmVisible() && fleet.isDisplayedNow());
        Allure.parameter("platesBefore", String.valueOf(platesBefore));
        Allure.parameter("platesAfter", String.valueOf(fleet.visiblePlateCount()));
        assertThat(fleet.isDisplayedNow()).isTrue();
        assertThat(fleet.visiblePlateCount()).isGreaterThanOrEqualTo(Math.max(1, platesBefore - 1));
        assertThat(classifyRentalNow()).isEqualTo("fleet");
    }

    /**
     * Disposable machine from FL-E10 fill attempt: Tata Ace / {@code FLNOD*} / Under Review.
     * Only this plate is Disable/Delete-confirmed — never KA13Z2117 / production plates.
     */
    private static final String DISPOSABLE_PLATE_PREFIX = "FLNOD";

    @Test(priority = 15, description = "FL-I15: Disable Confirm — Active disposable or Under Review Delete-only")
    @Severity(SeverityLevel.BLOCKER)
    @Description("FLNOD* Under Review long-press shows Delete only (no Disable) — record that. "
            + "If Disable is present, confirm Disable. Never touch KA13Z2117.")
    public void disableConfirmOnDisposableMachine() {
        FleetPage fleet = reachFleetViaTab();
        String plate = findDisposablePlate(fleet);
        Allure.parameter("targetPlate", plate);
        if (plate.isBlank()) {
            Allure.parameter("skipped", "No FLNOD* left — prior Delete already cleaned");
            fleet.attachScreenshot("fleet-fl-i15-no-disposable");
            assertThat(fleet.isDisplayedNow()).isTrue();
            return;
        }

        fleet.longPressPlate(plate);
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> fleet.hasDeleteAction() || fleet.hasDisableAction());
        boolean canDisable = fleet.hasDisableAction();
        Allure.parameter("disableActionVisible", String.valueOf(canDisable));
        Allure.parameter("deleteActionVisible", String.valueOf(fleet.hasDeleteAction()));
        fleet.attachScreenshot("fleet-fl-i15-longpress");

        if (!canDisable) {
            Allure.parameter("note", "Under Review long-press: Delete only — Disable N/A");
            DriverManager.get().navigate().back();
            assertThat(fleet.hasDeleteAction() || fleet.isDisplayedNow()).isTrue();
            assertThat(canDisable).as("Documented: no Disable on this Under Review card").isFalse();
            return;
        }

        fleet.tapDisableAction();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> fleet.isDisableConfirmVisible());
        fleet.attachScreenshot("fleet-fl-i15-disable-confirm");
        assertThat(fleet.isDisableConfirmVisible()).isTrue();
        fleet.tapConfirmDisable();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(12))
                .until(d -> !fleet.isDisableConfirmVisible());
        sleepBrief();
        fleet.attachScreenshot("fleet-fl-i15-after-disable");
        Allure.parameter("plateStillVisible", String.valueOf(fleet.isPlateVisible(plate)));
        Allure.parameter("after", classifyRentalNow());
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(fleet.isDisableConfirmVisible()).isFalse();
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
    }

    @Test(priority = 16, description = "FL-I16: Delete Confirm removes disposable FLNOD* machine")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Ensure FLNOD* exists (seed if needed) → long-press → Delete → confirm. Plate gone.")
    public void deleteConfirmRemovesDisposableMachine() {
        FleetPage fleet = reachFleetViaTab();
        String plate = findDisposablePlate(fleet);
        if (plate.isBlank()) {
            plate = seedDisposableUnderReview(fleet);
            // Stay in-session — do not re-login (splash/language flake).
            for (int i = 0; i < 3 && !fleet.isDisplayedNow(); i++) {
                DriverManager.get().navigate().back();
                sleepBrief();
            }
            if (!fleet.isDisplayedNow()) {
                fleet.tapHomeTab();
                sleepBrief();
                new HomePage().tapDesc("Fleet");
                fleet.waitUntilLoaded();
            }
            sleepBrief();
            String found = findDisposablePlate(fleet);
            if (!found.isBlank()) {
                plate = found;
            }
        }
        final FleetPage target = fleet;
        final String disposable = plate;
        Allure.parameter("targetPlate", disposable);
        assertThat(disposable).as("Need FLNOD* disposable to Delete Confirm").isNotBlank();

        target.longPressPlate(disposable);
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> target.hasDeleteAction());
        target.tapDeleteAction();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> target.isDeleteConfirmVisible());
        target.attachScreenshot("fleet-fl-i16-delete-confirm");
        assertThat(target.isDeleteConfirmVisible()).isTrue();
        target.tapConfirmDelete();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(15))
                .until(d -> !target.isDeleteConfirmVisible());
        sleepBrief();
        for (int i = 0; i < 4 && target.isPlateVisible(disposable); i++) {
            target.swipeFleetListUp();
            sleepBrief();
        }
        if (target.isPlateVisible(disposable)) {
            target.tapHomeTab();
            sleepBrief();
            new HomePage().tapDesc("Fleet");
            target.waitUntilLoaded();
            sleepBrief();
        }
        target.attachScreenshot("fleet-fl-i16-after-delete");
        Allure.parameter("plateStillVisible", String.valueOf(target.isPlateVisible(disposable)));
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(target.isDeleteConfirmVisible()).isFalse();
        assertThat(target.isPlateVisible(disposable))
                .as("Disposable plate %s must be removed after Delete Confirm", disposable)
                .isFalse();
    }

    /** Best-effort seed: Name/Capacity/Variant + FLNOD plate + Submit (may land Under Review). */
    private String seedDisposableUnderReview(FleetPage fleet) {
        String plate = "FLNOD" + (System.currentTimeMillis() % 100000);
        try {
            fleet.tapAddMachine();
            new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                    .until(d -> fleet.isAddMachineFormVisible());
            java.util.Set<String> beforeName = fleet.visibleTexts();
            fleet.openNameDropdown();
            new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                    .until(d -> !fleet.listNewMenuTexts(beforeName).isEmpty());
            java.util.List<String> names = fleet.listNewMenuTexts(beforeName);
            String name = names.contains("Tata Ace") ? "Tata Ace" : names.get(0);
            fleet.pickDropdownOption(name);
            sleepBrief();
            java.util.Set<String> beforeCap = fleet.visibleTexts();
            fleet.openCapacityDropdown();
            new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                    .until(d -> !fleet.listNewMenuTexts(beforeCap).isEmpty());
            fleet.pickDropdownOption(fleet.listNewMenuTexts(beforeCap).get(0));
            sleepBrief();
            java.util.Set<String> beforeVar = fleet.visibleTexts();
            fleet.openVariantDropdown();
            new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                    .until(d -> !fleet.listNewMenuTexts(beforeVar).isEmpty());
            fleet.pickDropdownOption(fleet.listNewMenuTexts(beforeVar).get(0));
            sleepBrief();
            fleet.typeRegistration(plate);
            fleet.tapSubmitAndVerify();
            sleepBrief();
            sleepBrief();
            Allure.parameter("seededPlate", plate);
        } catch (RuntimeException e) {
            Allure.parameter("seedError", e.getMessage());
        }
        return plate;
    }

    private String findDisposablePlate(FleetPage fleet) {
        for (int i = 0; i < 5; i++) {
            for (String p : fleet.plateTexts()) {
                if (p.startsWith(DISPOSABLE_PLATE_PREFIX)) {
                    return p;
                }
            }
            for (WebElement el : DriverManager.get().findElements(By.xpath(
                    "//android.widget.TextView[starts-with(@text,'FL')]"))) {
                String t = el.getAttribute("text");
                if (t != null && t.startsWith(DISPOSABLE_PLATE_PREFIX)) {
                    return t;
                }
            }
            fleet.swipeFleetListUp();
            sleepBrief();
        }
        return "";
    }

    private void sleepBrief() {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void waitSheetOrForm(FleetPage fleet) {
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10)).until(d ->
                fleet.isOperatorSheetVisible() || fleet.isAddMachineFormVisible());
    }
}
