package com.l2b.vendor.modules.fleet.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Rental Fleet — {@code Your Fleet} / {@code Add Machine}. Live dump
 * {@code /tmp/l2b-fleet-full-0001-20260925} on {@code 9000000001}.
 * Bottom tabs stay (Calendar · Home · Earning · Fleet).
 * Supports Change/Select operator (including Select confirm) and Add Fleet
 * Machines form field interaction + Submit &amp; Verify for edge coverage.
 */
public class FleetPage extends SplashScreen {

    public static final String OPERATOR_SHEET_TITLE = "Select Driver/operator";
    public static final String OPERATOR_RANDAN = "Randanberno Ezung";
    public static final String OPERATOR_NAUMAN = "Nauman Majid Pathan";

    @Override
    @Step("Wait for Fleet")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Fleet screen did not appear", Duration.ofSeconds(12));
    }

    @Step("Check Fleet identity (Your Fleet or Add Machine)")
    public boolean isDisplayedNow() {
        return isYourFleetTitleVisible()
                || isAddMachineVisible()
                || isEmptyStateVisible();
    }

    @Step("Check Your Fleet title (dump 03-fleet-list)")
    public boolean isYourFleetTitleVisible() {
        return isPresent(ComposeLocators.textView("Your Fleet"));
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
        return isPresent(By.xpath("//android.widget.TextView[starts-with(@text,'Change ')]"));
    }

    @Step("Check Select (unassigned) operator affordance")
    public boolean hasSelectOperator() {
        // List-card Select only — not the sheet bottom CTA while sheet is open.
        if (isOperatorSheetVisible()) {
            return false;
        }
        return isPresent(ComposeLocators.textView("Select"));
    }

    @Step("Check Change label containing operator fragment")
    public boolean hasChangeOperatorNamed(String nameFragment) {
        String needle = nameFragment == null ? "" : nameFragment.trim();
        for (WebElement el : driver.findElements(
                By.xpath("//android.widget.TextView[starts-with(@text,'Change ')]"))) {
            String t = safeText(el);
            if (t.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @Step("Count Change <operator> labels")
    public int changeOperatorCount() {
        return driver.findElements(
                By.xpath("//android.widget.TextView[starts-with(@text,'Change ')]")).size();
    }

    @Step("Read first Change <operator> label")
    public String firstChangeOperatorLabel() {
        List<WebElement> els = driver.findElements(
                By.xpath("//android.widget.TextView[starts-with(@text,'Change ')]"));
        if (els.isEmpty()) {
            return "";
        }
        return safeText(els.get(0));
    }

    @Step("Check rental bottom tabs still visible on Fleet")
    public boolean areBottomTabsVisible() {
        return isPresent(By.xpath("//*[@content-desc='Calendar']"))
                && isPresent(By.xpath("//*[@content-desc='Home']"))
                && isPresent(By.xpath("//*[@content-desc='Earning']"))
                && isPresent(By.xpath("//*[@content-desc='Fleet']"));
    }

    @Step("Count machine plate-like TextViews (KA/MH/FL prefix heuristic)")
    public int visiblePlateCount() {
        return driver.findElements(By.xpath(
                "//android.widget.TextView[starts-with(@text,'KA') or starts-with(@text,'MH') "
                        + "or starts-with(@text,'FL')]"
        )).size();
    }

    @Step("Count distinct plate texts (KA/MH/FL)")
    public int distinctPlateCount() {
        return plateTexts().size();
    }

    @Step("List visible plate texts")
    public List<String> plateTexts() {
        Set<String> out = new LinkedHashSet<>();
        for (WebElement el : driver.findElements(By.xpath(
                "//android.widget.TextView[starts-with(@text,'KA') or starts-with(@text,'MH') "
                        + "or starts-with(@text,'FL')]"))) {
            String t = safeText(el).trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return new ArrayList<>(out);
    }

    @Step("Count cards sharing a plate text")
    public int countPlateOccurrences(String plate) {
        int n = 0;
        for (WebElement el : driver.findElements(ComposeLocators.textView(plate))) {
            if (safeText(el).equals(plate)) {
                n++;
            }
        }
        return n;
    }

    @Step("Tap first Change <operator> control")
    public void tapFirstChangeOperator() {
        List<WebElement> els = driver.findElements(By.xpath(
                "//android.widget.TextView[starts-with(@text,'Change ')]"));
        if (els.isEmpty()) {
            throw new IllegalStateException("No Change <operator> control on Fleet");
        }
        clickGestureOn(els.get(0));
    }

    @Step("Tap Change label containing operator fragment")
    public void tapChangeOperatorNamed(String nameFragment) {
        String needle = nameFragment == null ? "" : nameFragment.trim();
        for (WebElement el : driver.findElements(
                By.xpath("//android.widget.TextView[starts-with(@text,'Change ')]"))) {
            if (safeText(el).contains(needle)) {
                clickGestureOn(el);
                return;
            }
        }
        throw new IllegalStateException("No Change label containing: " + needle);
    }

    @Step("Tap first Select (unassigned) operator control on list")
    public void tapFirstSelectOperator() {
        if (isOperatorSheetVisible()) {
            throw new IllegalStateException("Operator sheet already open — refusing list Select");
        }
        List<WebElement> els = driver.findElements(ComposeLocators.textView("Select"));
        if (els.isEmpty()) {
            throw new IllegalStateException("No Select operator control on Fleet");
        }
        clickGestureOn(els.get(0));
    }

    @Step("Check Select Driver/operator sheet")
    public boolean isOperatorSheetVisible() {
        return isPresent(ComposeLocators.textView(OPERATOR_SHEET_TITLE))
                || isPresent(By.xpath("//*[@content-desc='Close sheet']"));
    }

    @Step("Check Primary Operator label")
    public boolean hasPrimaryOperatorLabel() {
        return isPresent(ComposeLocators.textView("Primary Operator"));
    }

    @Step("Check Secondary Operator label")
    public boolean hasSecondaryOperatorLabel() {
        return isPresent(ComposeLocators.textView("Secondary Operator"));
    }

    @Step("Scroll operator sheet until Select CTA is visible")
    public void scrollOperatorSheetUntilSelectVisible() {
        if (!isOperatorSheetVisible()) {
            throw new IllegalStateException("Operator sheet not open");
        }
        for (int i = 0; i < 6; i++) {
            if (sheetSelectLabel() != null) {
                return;
            }
            org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence swipe =
                    new org.openqa.selenium.interactions.Sequence(finger, 1);
            swipe.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1950));
            swipe.addAction(finger.createPointerDown(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            swipe.addAction(finger.createPointerMove(Duration.ofMillis(400),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1150));
            swipe.addAction(finger.createPointerUp(
                    org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
            driver.perform(java.util.List.of(swipe));
        }
        if (sheetSelectLabel() == null) {
            throw new IllegalStateException("Select CTA not visible after scrolling sheet");
        }
    }

    @Step("Read Select CTA enabled on sheet (outer clickable View)")
    public boolean isSheetSelectEnabled() {
        if (!isOperatorSheetVisible()) {
            return false;
        }
        scrollOperatorSheetUntilSelectVisible();
        WebElement label = sheetSelectLabel();
        if (label == null) {
            return false;
        }
        try {
            WebElement parent = label.findElement(By.xpath("./.."));
            return Boolean.parseBoolean(parent.getAttribute("enabled"));
        } catch (RuntimeException e) {
            List<WebElement> outers = driver.findElements(ComposeLocators.clickableWithText("Select"));
            for (WebElement outer : outers) {
                if (outer.getRect().y > 1800) {
                    return Boolean.parseBoolean(outer.getAttribute("enabled"));
                }
            }
            return false;
        }
    }

    /** Bottom-sheet Select CTA only — ignore list-card Select (unassigned machines). */
    private WebElement sheetSelectLabel() {
        if (!isOperatorSheetVisible()) {
            return null;
        }
        WebElement best = null;
        int bestY = -1;
        for (WebElement el : driver.findElements(ComposeLocators.textView("Select"))) {
            Rectangle r = el.getRect();
            // Sheet CTA is the lowest Select; list-card Select sits higher in the fleet list.
            if (r.y > bestY) {
                bestY = r.y;
                best = el;
            }
        }
        // Require bottom-ish placement so list Select is not chosen when sheet is closed.
        if (best != null && bestY >= 1600) {
            return best;
        }
        return null;
    }

    @Step("Check driver/operator name visible on sheet")
    public boolean isDriverNamedVisible(String nameFragment) {
        String needle = nameFragment == null ? "" : nameFragment.trim();
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            if (safeText(el).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @Step("Tap a driver/operator row on the sheet by name fragment")
    public void tapDriverNamed(String nameFragment) {
        String needle = nameFragment == null ? "" : nameFragment.trim();
        scrollOperatorSheetUntilSelectVisible();
        WebElement nameEl = null;
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el);
            if (t.contains(needle) && !t.startsWith("Change ") && !t.equals("Select")) {
                nameEl = el;
                break;
            }
        }
        if (nameEl == null) {
            throw new IllegalStateException("Driver not on sheet: " + needle);
        }
        Rectangle nameRect = nameEl.getRect();
        WebElement best = null;
        int bestArea = Integer.MAX_VALUE;
        for (WebElement row : driver.findElements(By.xpath("//android.view.View[@clickable='true']"))) {
            Rectangle r = row.getRect();
            if (r.width < 400 || r.height > 800 || r.height < 40) {
                continue;
            }
            boolean covers = r.y <= nameRect.y && (r.y + r.height) >= (nameRect.y + nameRect.height);
            if (!covers) {
                continue;
            }
            int area = r.width * r.height;
            if (area < bestArea) {
                bestArea = area;
                best = row;
            }
        }
        clickGestureOn(best != null ? best : nameEl);
    }

    @Step("Tap sheet Select CTA to confirm operator")
    public void tapSheetSelect() {
        scrollOperatorSheetUntilSelectVisible();
        WebElement label = sheetSelectLabel();
        if (label == null) {
            throw new IllegalStateException("Sheet Select CTA not found");
        }
        try {
            clickGestureOn(label.findElement(By.xpath("./..")));
        } catch (RuntimeException e) {
            clickGestureOn(label);
        }
    }

    @Step("First assignable driver name on open sheet")
    public String firstAssignableDriverName() {
        scrollOperatorSheetUntilSelectVisible();
        for (String known : new String[] {OPERATOR_RANDAN, OPERATOR_NAUMAN}) {
            if (isDriverNamedVisible(known)) {
                return known;
            }
        }
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el).trim();
            if (t.length() < 5 || t.length() > 40) {
                continue;
            }
            if (t.equals(OPERATOR_SHEET_TITLE) || t.equals("Select") || t.equals("Machines")
                    || t.equals("Capacity") || t.startsWith("Completed") || t.matches("\\d+")
                    || t.contains("Tonnes") || t.contains("Primary") || t.contains("Secondary")
                    || t.startsWith("Change ") || t.contains("Kg") || t.contains("HP")
                    || t.contains("ft") || t.contains("Tons")) {
                continue;
            }
            // Likely a person name (has a space).
            if (t.contains(" ")) {
                return t;
            }
        }
        return "";
    }

    @Step("Assign operator from open sheet (scroll → pick name → Select)")
    public void assignOperatorFromSheet(String nameFragment) {
        if (!isOperatorSheetVisible()) {
            throw new IllegalStateException("Operator sheet not open");
        }
        scrollOperatorSheetUntilSelectVisible();
        tapDriverNamed(nameFragment);
        Waits.until(driver, d -> isSheetSelectEnabled() ? Boolean.TRUE : null,
                "Sheet Select stayed disabled after picking " + nameFragment,
                Duration.ofSeconds(10));
        tapSheetSelect();
        Waits.until(driver, d -> !isOperatorSheetVisible() ? Boolean.TRUE : null,
                "Operator sheet stayed open after Select",
                Duration.ofSeconds(12));
    }

    @Step("Close operator sheet (do not confirm assignment)")
    public void closeOperatorSheet() {
        if (isPresent(By.xpath("//*[@content-desc='Close sheet']"))) {
            driver.findElement(By.xpath("//*[@content-desc='Close sheet']")).click();
            return;
        }
        driver.navigate().back();
    }

    @Step("Tap Add Machine")
    public void tapAddMachine() {
        if (isPresent(By.xpath("//*[@content-desc='Add Machine']"))) {
            driver.findElement(By.xpath("//*[@content-desc='Add Machine']")).click();
            return;
        }
        driver.findElement(ComposeLocators.textView("Add Machine")).click();
    }

    @Step("Check Add Fleet Machines form")
    public boolean isAddMachineFormVisible() {
        return isPresent(ComposeLocators.textView("Add Fleet Machines"))
                || isPresent(ComposeLocators.textView("Submit & Verify"))
                || isPresent(ComposeLocators.textView("Name of machinery *"));
    }

    @Step("Check Add Machine required field labels")
    public boolean hasAddMachineRequiredFields() {
        return isPresent(ComposeLocators.textView("Name of machinery *"))
                && isPresent(ComposeLocators.textView("Capacity of machinery *"))
                && isPresent(ComposeLocators.textView("Variant of machinery *"))
                && isPresent(ComposeLocators.textView("Registration / Serial no. *"));
    }

    @Step("Check upload document rows")
    public boolean hasUploadDocumentRows() {
        return isPresent(ComposeLocators.textView("Upload RC document *"))
                && isPresent(ComposeLocators.textView("Upload Insurance document *"))
                && isPresent(ComposeLocators.textView("Upload TPI certificate document *"));
    }

    @Step("Check + Add Another Machine")
    public boolean hasAddAnotherMachine() {
        return isPresent(ComposeLocators.textView("+ Add Another Machine"))
                || isPresent(ComposeLocators.textViewContains("Add Another Machine"));
    }

    @Step("Check Submit & Verify visible")
    public boolean isSubmitAndVerifyVisible() {
        return isPresent(ComposeLocators.textView("Submit & Verify"));
    }

    @Step("Open Name of machinery dropdown")
    public void openNameDropdown() {
        tapFieldRow("Name of machinery *");
    }

    @Step("Open Capacity of machinery dropdown")
    public void openCapacityDropdown() {
        tapFieldRow("Capacity of machinery *");
    }

    @Step("Open Variant of machinery dropdown")
    public void openVariantDropdown() {
        tapFieldRow("Variant of machinery *");
    }

    @Step("Pick dropdown option by exact text")
    public void pickDropdownOption(String option) {
        Waits.until(driver, d -> isPresent(ComposeLocators.textView(option)) ? Boolean.TRUE : null,
                "Dropdown option not shown: " + option, Duration.ofSeconds(8));
        clickGestureOn(driver.findElement(ComposeLocators.textView(option)));
    }

    @Step("List newly visible short menu texts after opening a dropdown")
    public List<String> listNewMenuTexts(Set<String> before) {
        List<String> options = new ArrayList<>();
        for (WebElement node : driver.findElements(By.className("android.widget.TextView"))) {
            String text = safeText(node).trim();
            if (text.isEmpty() || text.length() >= 80 || before.contains(text)) {
                continue;
            }
            if (text.endsWith(" *") || text.startsWith("Upload ") || text.startsWith("Submit")
                    || text.startsWith("Add Fleet") || text.startsWith("+ Add")
                    || text.equals("1 Machine") || text.startsWith("(")) {
                continue;
            }
            if (!options.contains(text)) {
                options.add(text);
            }
        }
        return options;
    }

    @Step("Visible TextView texts snapshot")
    public Set<String> visibleTexts() {
        Set<String> out = new LinkedHashSet<>();
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el).trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    @Step("Type Registration / Serial no.")
    public void typeRegistration(String value) {
        List<WebElement> fields = driver.findElements(By.className("android.widget.EditText"));
        if (fields.isEmpty()) {
            throw new IllegalStateException("No EditText on Add Machine form");
        }
        WebElement field = fields.get(0);
        field.click();
        field.clear();
        field.sendKeys(value);
    }

    @Step("Read Registration EditText text")
    public String registrationText() {
        List<WebElement> fields = driver.findElements(By.className("android.widget.EditText"));
        if (fields.isEmpty()) {
            return "";
        }
        String t = fields.get(0).getAttribute("text");
        return t == null || "null".equalsIgnoreCase(t) ? "" : t;
    }

    @Step("Tap Submit & Verify")
    public void tapSubmitAndVerify() {
        List<WebElement> labels = driver.findElements(ComposeLocators.textView("Submit & Verify"));
        if (labels.isEmpty()) {
            throw new IllegalStateException("Submit & Verify not on form");
        }
        List<WebElement> outers = driver.findElements(ComposeLocators.clickableWithText("Submit & Verify"));
        clickGestureOn(outers.isEmpty() ? labels.get(0) : outers.get(0));
    }

    @Step("Tap + Add Another Machine")
    public void tapAddAnotherMachine() {
        if (isPresent(ComposeLocators.textView("+ Add Another Machine"))) {
            clickGestureOn(driver.findElement(ComposeLocators.textView("+ Add Another Machine")));
            return;
        }
        clickGestureOn(driver.findElement(ComposeLocators.textViewContains("Add Another Machine")));
    }

    @Step("Tap upload row by label")
    public void tapUploadRow(String label) {
        tapFieldRow(label);
    }

    @Step("Long-press first plate to reveal Delete/Disable")
    public void longPressFirstPlate() {
        List<WebElement> plates = driver.findElements(By.xpath(
                "//android.widget.TextView[starts-with(@text,'KA') or starts-with(@text,'MH') "
                        + "or starts-with(@text,'FL')]"));
        if (plates.isEmpty()) {
            throw new IllegalStateException("No plate to long-press");
        }
        longPressElement(plates.get(0));
    }

    @Step("Long-press a specific plate text")
    public void longPressPlate(String plate) {
        ensurePlateVisible(plate);
        List<WebElement> plates = driver.findElements(ComposeLocators.textView(plate));
        if (plates.isEmpty()) {
            throw new IllegalStateException("Plate not on Fleet: " + plate);
        }
        longPressElement(plates.get(0));
    }

    @Step("Check plate text visible on Fleet")
    public boolean isPlateVisible(String plate) {
        return isPresent(ComposeLocators.textView(plate));
    }

    @Step("Scroll Fleet until plate is visible")
    public void ensurePlateVisible(String plate) {
        for (int i = 0; i < 6; i++) {
            if (isPlateVisible(plate)) {
                return;
            }
            swipeFleetListUp();
        }
        if (!isPlateVisible(plate)) {
            throw new IllegalStateException("Plate not found after scroll: " + plate);
        }
    }

    @Step("Tap Confirm Disable on Disable Machine? dialog")
    public void tapConfirmDisable() {
        if (!isDisableConfirmVisible()) {
            throw new IllegalStateException("Disable Machine? not open");
        }
        // Prefer bottom dialog Disable (not the long-press row if still in tree).
        WebElement best = null;
        int bestY = -1;
        for (WebElement el : driver.findElements(ComposeLocators.textView("Disable"))) {
            Rectangle r = el.getRect();
            if (r.y > bestY) {
                bestY = r.y;
                best = el;
            }
        }
        if (best == null) {
            throw new IllegalStateException("Disable confirm CTA not found");
        }
        try {
            clickGestureOn(best.findElement(By.xpath("./..")));
        } catch (RuntimeException e) {
            clickGestureOn(best);
        }
    }

    @Step("Tap Confirm Delete on Delete Machine? dialog")
    public void tapConfirmDelete() {
        if (!isDeleteConfirmVisible()) {
            throw new IllegalStateException("Delete Machine? not open");
        }
        WebElement best = null;
        int bestY = -1;
        for (WebElement el : driver.findElements(ComposeLocators.textView("Delete"))) {
            Rectangle r = el.getRect();
            if (r.y > bestY) {
                bestY = r.y;
                best = el;
            }
        }
        if (best == null) {
            throw new IllegalStateException("Delete confirm CTA not found");
        }
        try {
            clickGestureOn(best.findElement(By.xpath("./..")));
        } catch (RuntimeException e) {
            clickGestureOn(best);
        }
    }

    private void longPressElement(WebElement el) {
        Rectangle r = el.getRect();
        int x = r.x + r.width / 2;
        int y = r.y + r.height / 2;
        driver.executeScript("mobile: longClickGesture", Map.of(
                "x", x, "y", y, "duration", 1200));
    }

    @Step("Check Delete and/or Disable long-press actions")
    public boolean hasDeleteAndDisable() {
        return isPresent(ComposeLocators.textView("Delete"))
                && isPresent(ComposeLocators.textView("Disable"));
    }

    @Step("Check Delete action visible (Under Review may omit Disable)")
    public boolean hasDeleteAction() {
        return isPresent(ComposeLocators.textView("Delete"));
    }

    @Step("Check Disable action visible")
    public boolean hasDisableAction() {
        return isPresent(ComposeLocators.textView("Disable"));
    }

    @Step("Tap Delete on long-press actions")
    public void tapDeleteAction() {
        List<WebElement> els = driver.findElements(ComposeLocators.textView("Delete"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Delete action not visible");
        }
        clickGestureOn(els.get(0));
    }

    @Step("Tap Disable on long-press actions")
    public void tapDisableAction() {
        List<WebElement> els = driver.findElements(ComposeLocators.textView("Disable"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Disable action not visible");
        }
        clickGestureOn(els.get(0));
    }

    @Step("Check Disable Machine? confirm dialog")
    public boolean isDisableConfirmVisible() {
        return isPresent(ComposeLocators.textView("Disable Machine?"))
                || isPresent(ComposeLocators.textViewContains("disable this machine"));
    }

    @Step("Check Delete Machine? confirm dialog")
    public boolean isDeleteConfirmVisible() {
        return isPresent(ComposeLocators.textView("Delete Machine?"))
                || isPresent(ComposeLocators.textViewContains("delete this machine"));
    }

    @Step("Tap Cancel on Disable/Delete confirm")
    public void tapConfirmCancel() {
        List<WebElement> els = driver.findElements(ComposeLocators.textView("Cancel"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Cancel not on confirm dialog");
        }
        clickGestureOn(els.get(0));
    }

    @Step("Check Status Under Review on list")
    public boolean hasUnderReviewStatus() {
        return isPresent(ComposeLocators.textView("Under Review"));
    }

    @Step("Tap Calendar bottom tab")
    public void tapCalendarTab() {
        driver.findElement(By.xpath("//*[@content-desc='Calendar']")).click();
    }

    @Step("Tap Earning bottom tab")
    public void tapEarningTab() {
        driver.findElement(By.xpath("//*[@content-desc='Earning']")).click();
    }

    @Step("Swipe Fleet list upward")
    public void swipeFleetListUp() {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1600));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(350),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 700));
        swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(java.util.List.of(swipe));
    }

    @Step("Tap machine title text (expect no detail screen)")
    public void tapMachineTitle(String title) {
        List<WebElement> els = driver.findElements(ComposeLocators.textView(title));
        if (els.isEmpty()) {
            throw new IllegalStateException("Machine title not found: " + title);
        }
        clickGestureOn(els.get(0));
    }

    @Step("Back from Add Machine / Fleet stack")
    public void tapBack() {
        if (isPresent(By.xpath("//*[@content-desc='Back']"))) {
            driver.findElement(By.xpath("//*[@content-desc='Back']")).click();
            return;
        }
        driver.navigate().back();
    }

    @Step("Tap Home bottom tab")
    public void tapHomeTab() {
        driver.findElement(By.xpath("//*[@content-desc='Home']")).click();
    }


    @Step("Swipe Add Machine form up to reveal upload rows")
    public void swipeAddFormUp() {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1700));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(400),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 700));
        swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(java.util.List.of(swipe));
    }

    @Step("Ensure upload document rows visible")
    public void ensureUploadRowsVisible() {
        for (int i = 0; i < 5; i++) {
            if (hasUploadDocumentRows()) {
                return;
            }
            swipeAddFormUp();
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    @Step("Try pick a fake document from system picker / gallery")
    public boolean tryPickFakeDocument(String filenameHint) {
        // Chooser / bottom sheet labels
        String[] chooser = {
                "Gallery", "Photos", "Files", "Documents", "Download", "Downloads",
                "Choose from Gallery", "Browse", "Show roots", "Recent"
        };
        for (String label : chooser) {
            if (isPresent(ComposeLocators.textView(label))
                    || isPresent(By.xpath("//*[@content-desc='" + label + "']"))) {
                try {
                    if (isPresent(ComposeLocators.textView(label))) {
                        clickGestureOn(driver.findElement(ComposeLocators.textView(label)));
                    } else {
                        clickGestureOn(driver.findElement(By.xpath("//*[@content-desc='" + label + "']")));
                    }
                    try { Thread.sleep(900); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                } catch (RuntimeException ignored) {
                    // keep trying
                }
            }
        }
        // Filename match (Downloads / Files UI)
        java.util.List<WebElement> byName = driver.findElements(By.xpath(
                "//*[contains(@text,'" + filenameHint + "') or contains(@content-desc,'"
                        + filenameHint + "')]"));
        if (!byName.isEmpty()) {
            clickGestureOn(byName.get(0));
            try { Thread.sleep(1200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return true;
        }
        // Photo picker / gallery grid — tap a recent image (ImageView or content-desc Photo)
        java.util.List<WebElement> images = driver.findElements(By.className("android.widget.ImageView"));
        for (WebElement img : images) {
            try {
                org.openqa.selenium.Rectangle r = img.getRect();
                if (r.width < 80 || r.height < 80 || r.y < 200) {
                    continue;
                }
                clickGestureOn(img);
                try { Thread.sleep(1200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return true;
            } catch (RuntimeException ignored) {
            }
        }
        java.util.List<WebElement> photos = driver.findElements(By.xpath(
                "//*[contains(@content-desc,'Photo') or contains(@content-desc,'image')"
                        + " or contains(@content-desc,'fake')]"));
        if (!photos.isEmpty()) {
            clickGestureOn(photos.get(0));
            try { Thread.sleep(1200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            return true;
        }
        return false;
    }

    @Step("Check upload row still shows required asterisk (likely unfilled)")
    public boolean uploadRowStillRequired(String label) {
        return isPresent(ComposeLocators.textView(label));
    }

    @Step("Dismiss system picker / overlay with Back if still on Vendor")
    public void dismissOverlayWithBack() {
        driver.navigate().back();
    }

    private void tapFieldRow(String label) {
        List<WebElement> outers = driver.findElements(ComposeLocators.clickableWithText(label));
        if (!outers.isEmpty()) {
            clickGestureOn(outers.get(0));
            return;
        }
        List<WebElement> labels = driver.findElements(ComposeLocators.textView(label));
        if (labels.isEmpty()) {
            throw new IllegalStateException("Field label not found: " + label);
        }
        clickGestureOn(labels.get(0));
    }

    private void clickGestureOn(WebElement el) {
        Rectangle r = el.getRect();
        int x = r.x + Math.max(1, r.width / 2);
        int y = r.y + Math.max(1, r.height / 2);
        driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    private static String safeText(WebElement el) {
        try {
            String t = el.getAttribute("text");
            return t == null || "null".equalsIgnoreCase(t) ? "" : t;
        } catch (RuntimeException e) {
            return "";
        }
    }
}
