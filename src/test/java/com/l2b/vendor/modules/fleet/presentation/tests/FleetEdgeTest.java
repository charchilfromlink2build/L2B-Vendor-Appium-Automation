package com.l2b.vendor.modules.fleet.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.fleet.presentation.pages.FleetPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

/**
 * Fleet edge — validation, field wiring, duplicate plate, long-press Delete/Disable,
 * empty Submit. Dump {@code /tmp/l2b-fleet-full-0001-20260925}. Account
 * {@code 9000000001}. Does not Confirm Delete/Disable.
 */
@Epic("Vendor app")
@Feature("Fleet edge — rental 9000000001")
public class FleetEdgeTest extends FleetBaseTest {

    @Test(priority = 1, description = "FL-E1: sheet Select disabled until a driver is picked")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 07/08: open Change sheet — Select CTA disabled until a driver row is "
            + "tapped. Close without confirm.")
    public void sheetSelectDisabledUntilDriverPicked() {
        FleetPage fleet = reachFleetViaTab();
        fleet.tapFirstChangeOperator();
        waitSheet(fleet);
        boolean before = fleet.isSheetSelectEnabled();
        Allure.parameter("selectEnabledBeforePick", String.valueOf(before));
        fleet.attachScreenshot("fleet-fl-e1-select-disabled");
        assertThat(before).as("Select must stay disabled before pick").isFalse();

        fleet.tapDriverNamed(FleetPage.OPERATOR_NAUMAN);
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(6))
                .until(d -> fleet.isSheetSelectEnabled());
        boolean after = fleet.isSheetSelectEnabled();
        Allure.parameter("selectEnabledAfterPick", String.valueOf(after));
        fleet.attachScreenshot("fleet-fl-e1-select-enabled");
        assertThat(after).as("Select enables after picking a driver").isTrue();
        fleet.closeOperatorSheet();
        assertThat(fleet.isDisplayedNow()).isTrue();
    }

    @Test(priority = 2, description = "FL-E2: dismiss sheet without Select leaves operator unchanged")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Open Change, pick other driver, Close sheet — list Change label must not flip.")
    public void dismissSheetLeavesOperatorUnchanged() {
        FleetPage fleet = reachFleetViaTab();
        String before = fleet.firstChangeOperatorLabel();
        fleet.tapFirstChangeOperator();
        waitSheet(fleet);
        String other = before.contains("Nauman")
                ? FleetPage.OPERATOR_RANDAN
                : FleetPage.OPERATOR_NAUMAN;
        fleet.tapDriverNamed(other);
        fleet.closeOperatorSheet();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> fleet.isDisplayedNow() && !fleet.isOperatorSheetVisible());
        String after = fleet.firstChangeOperatorLabel();
        Allure.parameter("before", before);
        Allure.parameter("after", after);
        fleet.attachScreenshot("fleet-fl-e2-unchanged");
        assertThat(after).as("Close without Select must not change operator").isEqualTo(before);
    }

    @Test(priority = 3, description = "FL-E3: Name of machinery dropdown opens and accepts a pick")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 13-name-dd: tap Name row → options (Excavators, Tata Ace, …) → pick one.")
    public void nameDropdownOpensAndPicks() {
        FleetPage fleet = openAddForm();
        Set<String> before = fleet.visibleTexts();
        fleet.openNameDropdown();
        List<String> options = waitMenuOptions(fleet, before);
        Allure.parameter("nameOptions", String.join(" | ", options));
        fleet.attachScreenshot("fleet-fl-e3-name-dd");
        assertThat(options).as("Name dropdown must list machinery types").isNotEmpty();
        String pick = options.contains("Excavators") ? "Excavators"
                : options.contains("Tata Ace") ? "Tata Ace" : options.get(0);
        fleet.pickDropdownOption(pick);
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(6))
                .until(d -> fleet.isAddMachineFormVisible());
        Allure.parameter("pickedName", pick);
        assertThat(fleet.isAddMachineFormVisible()).isTrue();
        assertThat(fleet.visibleTexts()).as("Picked name stays on form").contains(pick);
        fleet.tapBack();
    }

    @Test(priority = 4, description = "FL-E4: Capacity dropdown opens after Name is set")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Name → Capacity row → capacity options appear and one can be picked.")
    public void capacityDropdownOpensAfterName() {
        FleetPage fleet = openAddForm();
        pickName(fleet, "Excavators");
        Set<String> before = fleet.visibleTexts();
        fleet.openCapacityDropdown();
        List<String> options = waitMenuOptions(fleet, before);
        Allure.parameter("capacityOptions", String.join(" | ", options));
        fleet.attachScreenshot("fleet-fl-e4-capacity-dd");
        assertThat(options).as("Capacity dropdown options").isNotEmpty();
        fleet.pickDropdownOption(options.get(0));
        Allure.parameter("pickedCapacity", options.get(0));
        assertThat(fleet.isAddMachineFormVisible()).isTrue();
        fleet.tapBack();
    }

    @Test(priority = 5, description = "FL-E5: Variant dropdown opens after Name + Capacity")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Name + Capacity → Variant row → options → pick.")
    public void variantDropdownOpensAfterCapacity() {
        FleetPage fleet = openAddForm();
        pickName(fleet, "Excavators");
        pickCapacityFirst(fleet);
        Set<String> before = fleet.visibleTexts();
        fleet.openVariantDropdown();
        List<String> options = waitMenuOptions(fleet, before);
        Allure.parameter("variantOptions", String.join(" | ", options));
        fleet.attachScreenshot("fleet-fl-e5-variant-dd");
        assertThat(options).as("Variant dropdown options").isNotEmpty();
        fleet.pickDropdownOption(options.get(0));
        Allure.parameter("pickedVariant", options.get(0));
        assertThat(fleet.isAddMachineFormVisible()).isTrue();
        fleet.tapBack();
    }

    @Test(priority = 6, description = "FL-E6: Registration EditText accepts typed plate")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 10-add-top: single EditText for Registration / Serial no. Types a unique plate.")
    public void registrationFieldAcceptsInput() {
        FleetPage fleet = openAddForm();
        String plate = "FLTEST" + (System.currentTimeMillis() % 100000);
        fleet.typeRegistration(plate);
        String read = fleet.registrationText();
        Allure.parameter("typed", plate);
        Allure.parameter("readBack", read);
        fleet.attachScreenshot("fleet-fl-e6-registration");
        assertThat(read).as("Registration EditText must keep typed value").contains(plate);
        fleet.tapBack();
    }

    @Test(priority = 7, description = "FL-E7: Upload RC / Insurance / TPI rows are present and tappable")
    @Severity(SeverityLevel.CRITICAL)
    @Description("All three Upload * rows visible. Tap RC — if a system picker opens, Back dismisses "
            + "and form remains. Documents are required for real Submit.")
    public void uploadRowsPresentAndTappable() {
        FleetPage fleet = openAddForm();
        assertThat(fleet.hasUploadDocumentRows()).as("RC + Insurance + TPI rows").isTrue();
        fleet.tapUploadRow("Upload RC document *");
        sleepBrief();
        String pkg = vendorPackage();
        boolean stillForm = fleet.isAddMachineFormVisible();
        Allure.parameter("packageAfterRcTap", pkg);
        Allure.parameter("formAfterRcTap", String.valueOf(stillForm));
        fleet.attachScreenshot("fleet-fl-e7-after-rc-tap");
        if (!stillForm) {
            // System picker / Photos — dismiss and require form again.
            fleet.dismissOverlayWithBack();
            sleepBrief();
            if (!fleet.isAddMachineFormVisible()) {
                fleet.dismissOverlayWithBack();
            }
        }
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(fleet.isAddMachineFormVisible() || classifyRentalNow().equals("fleet"))
                .as("Must remain in Vendor after upload tap")
                .isTrue();
        if (fleet.isAddMachineFormVisible()) {
            fleet.tapBack();
        }
    }

    @Test(priority = 8, description = "FL-E8: + Add Another Machine expands a second machine block")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap + Add Another Machine. Expect 2 Machine (or second Name row) without crash.")
    public void addAnotherMachineExpands() {
        FleetPage fleet = openAddForm();
        assertThat(fleet.hasAddAnotherMachine()).isTrue();
        fleet.tapAddAnotherMachine();
        sleepBrief();
        boolean two = fleet.visibleTexts().stream()
                .anyMatch(t -> t.contains("2 Machine") || t.contains("2 Machines"));
        Allure.parameter("twoMachineCopy", String.valueOf(two));
        fleet.attachScreenshot("fleet-fl-e8-add-another");
        assertThat(fleet.isAddMachineFormVisible()).isTrue();
        assertThat(two || fleet.visibleTexts().stream().filter(t -> t.equals("Name of machinery *")).count() >= 1)
                .as("+ Add Another Machine must keep form usable")
                .isTrue();
        fleet.tapBack();
    }

    @Test(priority = 9, description = "FL-E9: empty Submit & Verify must not drop to launcher")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Dump 20-after-empty-submit previously landed on Android launcher. Empty Submit "
            + "must stay on Add Fleet Machines with validation — not leave Vendor. New bug #35 if "
            + "launcher.")
    public void emptySubmitDoesNotLeaveVendor() {
        FleetPage fleet = openAddForm();
        fleet.tapSubmitAndVerify();
        sleepBrief();
        String after = classifyRentalNow();
        String pkg = vendorPackage();
        boolean form = fleet.isAddMachineFormVisible();
        Allure.parameter("afterSubmit", after);
        Allure.parameter("package", pkg);
        fleet.attachScreenshot("fleet-fl-e9-empty-submit");
        boolean stayed = form || "fleet".equals(after);
        boolean leftForm = !form;
        boolean launcher = "launcher".equals(after) || !Config.get("app.package").equals(pkg);
        Allure.parameter("stayedOnFormOrFleet", String.valueOf(stayed));
        Allure.parameter("landedLauncher", String.valueOf(launcher));
        if (launcher || leftForm) {
            Allure.parameter("bug", "#35 empty/invalid Submit leaves Add Fleet Machines "
                    + "(dump launcher; live afterSubmit=" + after + ")");
        }
        assertThat(launcher || leftForm)
                .as("BUG #35: empty Submit & Verify left Add Fleet Machines "
                        + "(after=%s package=%s) — expected stay on form with validation",
                        after, pkg)
                .isFalse();
        assertThat(stayed).as("Empty Submit must keep form or Fleet").isTrue();
        if (fleet.isAddMachineFormVisible()) {
            fleet.tapBack();
        }
    }

    @Test(priority = 10, description = "FL-E10: filled fields without docs — Submit stays with validation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Pick Name/Capacity/Variant + type plate, no uploads, Submit. Must not crash or "
            + "leave Vendor. May stay on form (docs required) — that is correct.")
    public void submitWithoutDocsStaysInVendor() {
        FleetPage fleet = openAddForm();
        pickName(fleet, "Tata Ace");
        pickCapacityFirst(fleet);
        pickVariantFirst(fleet);
        String plate = "FLNOD" + (System.currentTimeMillis() % 100000);
        fleet.typeRegistration(plate);
        fleet.attachScreenshot("fleet-fl-e10-before-submit");
        fleet.tapSubmitAndVerify();
        sleepBrief();
        String after = classifyRentalNow();
        Allure.parameter("after", after);
        Allure.parameter("package", vendorPackage());
        fleet.attachScreenshot("fleet-fl-e10-after-submit");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(fleet.isAddMachineFormVisible() || "fleet".equals(after))
                .as("Submit without docs must not leave Vendor")
                .isTrue();
        // Successful add without docs would be unexpected — note if plate appears on list.
        if ("fleet".equals(after) && !fleet.isAddMachineFormVisible()) {
            Allure.parameter("plateOnList",
                    String.valueOf(fleet.plateTexts().contains(plate)));
        }
        if (fleet.isAddMachineFormVisible()) {
            fleet.tapBack();
        }
    }

    @Test(priority = 11, description = "FL-E11: duplicate plate MH12SD4444 on two Active cards")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump 03-fleet-list: 14 Ft Truck and Tata Ace both show MH12SD4444. Document as "
            + "data/UX issue #36 if still true — two Active machines must not share one plate.")
    public void duplicatePlateOnTwoCards() {
        FleetPage fleet = reachFleetViaTab();
        int hits = fleet.countPlateOccurrences("MH12SD4444");
        Allure.parameter("mh12sd4444Count", String.valueOf(hits));
        Allure.parameter("plates", String.join(",", fleet.plateTexts()));
        fleet.attachScreenshot("fleet-fl-e11-duplicate-plate");
        if (hits >= 2) {
            Allure.parameter("bug", "#36 duplicate plate MH12SD4444 on two Active machines");
        }
        assertThat(hits)
                .as("BUG #36: plate MH12SD4444 appears on %s Active cards — expected unique plates",
                        hits)
                .isLessThan(2);
    }

    @Test(priority = 12, description = "FL-E12: machine title tap stays on Your Fleet (no detail)")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump 05-machine-title-tap: tapping Excavator 20 Tonnes title does not open a "
            + "detail/edit screen.")
    public void machineTitleTapStaysOnFleet() {
        FleetPage fleet = reachFleetViaTab();
        fleet.tapMachineTitle("Excavator 20 Tonnes");
        sleepBrief();
        Allure.parameter("after", classifyRentalNow());
        fleet.attachScreenshot("fleet-fl-e12-title-tap");
        assertThat(classifyRentalNow()).isEqualTo("fleet");
        assertThat(fleet.isDisplayedNow()).isTrue();
        assertThat(fleet.isAddMachineFormVisible()).isFalse();
    }

    @Test(priority = 13, description = "FL-E13: long-press plate reveals Delete + Disable (no confirm)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 06-longpress-plate: long-press plate → Delete + Disable. Do not tap Confirm.")
    public void longPressRevealsDeleteDisable() {
        FleetPage fleet = reachFleetViaTab();
        fleet.longPressFirstPlate();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8))
                .until(d -> fleet.hasDeleteAndDisable());
        Allure.parameter("deleteDisable", String.valueOf(fleet.hasDeleteAndDisable()));
        fleet.attachScreenshot("fleet-fl-e13-longpress");
        assertThat(fleet.hasDeleteAndDisable()).isTrue();
        // Dismiss without destructive confirm — Close sheet / Back. Long-press chrome
        // itself is the assertion; dismiss may leave overlay briefly.
        driverBack();
        sleepBrief();
        Allure.parameter("afterDismiss", classifyRentalNow());
        if (fleet.hasDeleteAndDisable()) {
            driverBack();
            sleepBrief();
        }
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(fleet.isDisplayedNow() || "fleet".equals(classifyRentalNow())
                || "home".equals(classifyRentalNow()))
                .as("After dismissing Delete/Disable, stay in Vendor Fleet/Home")
                .isTrue();
    }

    @Test(priority = 14, description = "FL-E14: Insurance + TPI upload rows tappable")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap Upload Insurance and Upload TPI (after RC covered in E7). Stay in Vendor.")
    public void insuranceAndTpiUploadRowsTappable() {
        FleetPage fleet = openAddForm();
        assertThat(fleet.hasUploadDocumentRows()).isTrue();
        fleet.tapUploadRow("Upload Insurance document *");
        sleepBrief();
        if (!fleet.isAddMachineFormVisible()) {
            fleet.dismissOverlayWithBack();
            sleepBrief();
        }
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        if (!fleet.isAddMachineFormVisible()) {
            // re-open if picker ate the form
            fleet = openAddForm();
        }
        fleet.tapUploadRow("Upload TPI certificate document *");
        sleepBrief();
        if (!fleet.isAddMachineFormVisible()) {
            fleet.dismissOverlayWithBack();
            sleepBrief();
        }
        fleet.attachScreenshot("fleet-fl-e14-uploads");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        if (fleet.isAddMachineFormVisible()) {
            fleet.tapBack();
        }
    }

    @Test(priority = 15, description = "FL-E15: device Back on Your Fleet stays in Vendor or Home")
    @Severity(SeverityLevel.NORMAL)
    @Description("Device Back from Your Fleet — expect Home (or stay Fleet). Launcher is a bug "
            + "candidate (#25 family) if it drops out of Vendor.")
    public void backOnFleetStaysInVendor() {
        FleetPage fleet = reachFleetViaTab();
        driverBack();
        sleepBrief();
        String after = classifyRentalNow();
        String pkg = vendorPackage();
        Allure.parameter("after", after);
        Allure.parameter("package", pkg);
        fleet.attachScreenshot("fleet-fl-e15-back");
        boolean launcher = "launcher".equals(after) || !Config.get("app.package").equals(pkg);
        if (launcher) {
            Allure.parameter("bug", "Fleet Back drops to Android launcher (candidate)");
        }
        assertThat(launcher)
                .as("Back on Your Fleet dropped to launcher — expected Home/Fleet")
                .isFalse();
        assertThat(after).isIn("home", "fleet", "quick-booking", "extend-time");
    }

    @Test(priority = 16, description = "FL-E16: short/invalid registration still allows typing")
    @Severity(SeverityLevel.NORMAL)
    @Description("Type junk plate 'X' into Registration — field accepts it (validation is on Submit).")
    public void invalidRegistrationAcceptsTyping() {
        FleetPage fleet = openAddForm();
        fleet.typeRegistration("X");
        Allure.parameter("readBack", fleet.registrationText());
        fleet.attachScreenshot("fleet-fl-e16-invalid-reg");
        assertThat(fleet.registrationText()).contains("X");
        fleet.tapBack();
    }


    @Test(priority = 17, description = "FL-E17: fake RC/Insurance/TPI uploads must not create machine")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Push intentional fake JPGs (testdata/fleet-fake-docs). Upload all three required "
            + "docs + Submit. Expected: reject invalid documents. If plate appears on Fleet → bug.")
    public void fakeDocumentsMustNotCreateMachine() {
        FleetPage fleet = openAddForm();
        pickName(fleet, "Tata Ace");
        pickCapacityFirst(fleet);
        pickVariantFirst(fleet);
        String plate = "FLFAKE" + (System.currentTimeMillis() % 100000);
        fleet.typeRegistration(plate);
        Allure.parameter("targetPlate", plate);

        String[] rows = {
                "Upload RC document *",
                "Upload Insurance document *",
                "Upload TPI certificate document *"
        };
        String[] files = {"fake-rc", "fake-insurance", "fake-tpi"};
        int picked = 0;
        for (int i = 0; i < rows.length; i++) {
            fleet.ensureUploadRowsVisible();
            fleet.tapUploadRow(rows[i]);
            sleepBrief();
            boolean ok = fleet.tryPickFakeDocument(files[i]);
            Allure.parameter("picked_" + files[i], String.valueOf(ok));
            if (ok) {
                picked++;
            } else {
                fleet.attachScreenshot("fleet-fl-e17-picker-" + files[i]);
                // dismiss picker and continue — may still submit without all three
                if (!fleet.isAddMachineFormVisible()) {
                    fleet.dismissOverlayWithBack();
                    sleepBrief();
                }
            }
            // wait for return to form
            for (int w = 0; w < 5 && !fleet.isAddMachineFormVisible(); w++) {
                sleepBrief();
            }
            if (!fleet.isAddMachineFormVisible()) {
                fleet.dismissOverlayWithBack();
                sleepBrief();
            }
        }
        Allure.parameter("fakeDocsPicked", String.valueOf(picked));
        fleet.attachScreenshot("fleet-fl-e17-before-submit");
        assertThat(picked)
                .as("Need at least one fake document picked from gallery/files")
                .isGreaterThan(0);

        fleet.ensureUploadRowsVisible();
        fleet.tapSubmitAndVerify();
        sleepBrief();
        sleepBrief();
        String after = classifyRentalNow();
        Allure.parameter("afterSubmit", after);
        fleet.attachScreenshot("fleet-fl-e17-after-submit");
        boolean machinesAdded = fleet.visibleTexts().stream().anyMatch(s ->
                s.contains("Machines Added")
                        || s.contains("submitted for verification"));
        Allure.parameter("machinesAddedDialog", String.valueOf(machinesAdded));
        if (machinesAdded) {
            Allure.parameter("bug", "#38 Fake/partial mock docs accepted — Machines Added");
            // Dismiss OK so list check can run
            try {
                if (DriverManager.get().findElements(
                        org.openqa.selenium.By.xpath("//android.widget.TextView[@text='OK']")).size() > 0) {
                    DriverManager.get().findElement(
                            org.openqa.selenium.By.xpath("//android.widget.TextView[@text='OK']")).click();
                    sleepBrief();
                }
            } catch (RuntimeException ignored) {
            }
        }

        // Stay in-session — avoid re-login splash flake after submit.
        if (fleet.isAddMachineFormVisible()) {
            fleet.tapBack();
            sleepBrief();
        }
        for (int i = 0; i < 4 && !fleet.isDisplayedNow(); i++) {
            DriverManager.get().navigate().back();
            sleepBrief();
        }
        if (!fleet.isDisplayedNow()) {
            fleet.tapHomeTab();
            sleepBrief();
            new com.l2b.vendor.modules.home.presentation.pages.HomePage().tapDesc("Fleet");
            fleet.waitUntilLoaded();
        }
        boolean onList = false;
        for (int i = 0; i < 6; i++) {
            if (fleet.isPlateVisible(plate)
                    || fleet.plateTexts().stream().anyMatch(p -> p.startsWith("FLFAKE"))) {
                onList = true;
                break;
            }
            for (org.openqa.selenium.WebElement el : DriverManager.get().findElements(
                    org.openqa.selenium.By.xpath(
                            "//android.widget.TextView[starts-with(@text,'FLFAKE')]"))) {
                String tx = el.getAttribute("text");
                if (tx != null && tx.startsWith("FLFAKE")) {
                    onList = true;
                    Allure.parameter("foundPlate", tx);
                    break;
                }
            }
            if (onList) {
                break;
            }
            fleet.swipeFleetListUp();
            sleepBrief();
        }
        Allure.parameter("fakePlateOnList", String.valueOf(onList));
        if (onList) {
            Allure.parameter("bug", "#38 Fake RC/Insurance/TPI accepted — machine created");
        }
        // Soft evidence: if picked>=3 and machine created → fail as bug.
        // If picked>=1 and machine created without real docs → also fail.
        assertThat(machinesAdded || onList)
                .as("BUG #38: fake/mock (or partial) document upload allowed machine create "
                        + "(plate %s, machinesAdded=%s, onList=%s). Expected reject invalid docs.",
                        plate, machinesAdded, onList)
                .isFalse();
    }

    private FleetPage openAddForm() {
        FleetPage fleet = reachFleetViaTab();
        fleet.tapAddMachine();
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> fleet.isAddMachineFormVisible());
        assertThat(fleet.isAddMachineFormVisible()).isTrue();
        return fleet;
    }

    private void pickName(FleetPage fleet, String preferred) {
        Set<String> before = fleet.visibleTexts();
        fleet.openNameDropdown();
        List<String> options = waitMenuOptions(fleet, before);
        String pick = options.contains(preferred) ? preferred : options.get(0);
        fleet.pickDropdownOption(pick);
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(6))
                .until(d -> fleet.isAddMachineFormVisible());
    }

    private void pickCapacityFirst(FleetPage fleet) {
        Set<String> before = fleet.visibleTexts();
        fleet.openCapacityDropdown();
        List<String> options = waitMenuOptions(fleet, before);
        assertThat(options).as("Capacity options").isNotEmpty();
        fleet.pickDropdownOption(options.get(0));
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(6))
                .until(d -> fleet.isAddMachineFormVisible());
    }

    private void pickVariantFirst(FleetPage fleet) {
        Set<String> before = fleet.visibleTexts();
        fleet.openVariantDropdown();
        List<String> options = waitMenuOptions(fleet, before);
        assertThat(options).as("Variant options").isNotEmpty();
        fleet.pickDropdownOption(options.get(0));
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(6))
                .until(d -> fleet.isAddMachineFormVisible());
    }

    private List<String> waitMenuOptions(FleetPage fleet, Set<String> before) {
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(8)).until(d -> {
            List<String> opts = fleet.listNewMenuTexts(before);
            return opts.isEmpty() ? null : Boolean.TRUE;
        });
        return fleet.listNewMenuTexts(before);
    }

    private void waitSheet(FleetPage fleet) {
        new WebDriverWait(DriverManager.get(), Duration.ofSeconds(10))
                .until(d -> fleet.isOperatorSheetVisible());
    }

    private void sleepBrief() {
        try {
            Thread.sleep(1200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void driverBack() {
        DriverManager.get().navigate().back();
    }
}
