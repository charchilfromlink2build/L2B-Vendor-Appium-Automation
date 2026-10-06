package com.l2b.vendor.modules.team.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Manage Team from Profile drawer. Live dump
 * {@code /tmp/l2b-team-0001-20261002} on {@code 9000000001}.
 * Never tap destructive assign/remove confirms in dump/landing.
 */
public class TeamPage extends SplashScreen {

    @Override
    @Step("Wait for Manage Team")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Manage Team screen did not appear", Duration.ofSeconds(20));
    }

    @Step("Check Manage Team identity")
    public boolean isDisplayedNow() {
        return isManageTeamTitleVisible()
                && (isYourTeamMemberVisible() || isAddMemberVisible() || hasActiveMember());
    }

    @Step("Check Manage Team title")
    public boolean isManageTeamTitleVisible() {
        return isPresent(ComposeLocators.textView("Manage Team"));
    }

    @Step("Check Your team member heading")
    public boolean isYourTeamMemberVisible() {
        return isPresent(ComposeLocators.textView("Your team member"))
                || isPresent(ComposeLocators.textView("Your team members"));
    }

    @Step("Check Back")
    public boolean isBackVisible() {
        return isPresent(By.xpath("//*[@content-desc='Back']"));
    }

    @Step("Check Add Member CTA")
    public boolean isAddMemberVisible() {
        return isPresent(ComposeLocators.textView("Add Member"));
    }

    @Step("Check Active status chip")
    public boolean isActiveVisible() {
        return isPresent(ComposeLocators.textView("Active"));
    }

    @Step("Check Change Assignment")
    public boolean isChangeAssignmentVisible() {
        return isPresent(ComposeLocators.textView("Change Assignment"));
    }

    @Step("Check Machine column label")
    public boolean isMachineLabelVisible() {
        return isPresent(ComposeLocators.textView("Machine"));
    }

    @Step("Check Capacity column label")
    public boolean isCapacityLabelVisible() {
        return isPresent(ComposeLocators.textView("Capacity"));
    }

    @Step("Check Skills section")
    public boolean isSkillsVisible() {
        return isPresent(ComposeLocators.textView("Skills"));
    }

    @Step("Check Experience line")
    public boolean isExperienceVisible() {
        return isPresent(ComposeLocators.textViewContains("Experience:"));
    }

    @Step("Check Primary marker")
    public boolean isPrimaryVisible() {
        return isPresent(ComposeLocators.textViewContains("Primary"));
    }

    @Step("Check known member Nauman")
    public boolean isNaumanVisible() {
        return isPresent(ComposeLocators.textViewContains("Nauman"));
    }

    @Step("Check known member phone 9000000002")
    public boolean isNaumanPhoneVisible() {
        return isPresent(ComposeLocators.textView("9000000002"));
    }

    @Step("Check known member Randanberno")
    public boolean isRandanbernoVisible() {
        return isPresent(ComposeLocators.textViewContains("Randanberno"));
    }

    @Step("True when any Active member chip is on screen")
    public boolean hasActiveMember() {
        return isActiveVisible();
    }

    @Step("Check Change Assignment machine sheet")
    public boolean isChangeAssignmentSheetVisible() {
        // Prefer title text — Close sheet content-desc can hang UiAutomator when absent.
        return isPresent(ComposeLocators.textView("Select machine for operator/driver"));
    }

    @Step("Check Close sheet control")
    public boolean isCloseSheetVisible() {
        try {
            return !driver.findElements(By.xpath("//*[@content-desc='Close sheet']")).isEmpty();
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Step("Dismiss Change Assignment sheet if open (Close sheet preferred)")
    public void dismissSheetIfPresent() {
        try {
            if (!isChangeAssignmentSheetVisible()) {
                return;
            }
            tapCloseSheet();
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (isChangeAssignmentSheetVisible()) {
                tapCloseSheet();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (RuntimeException e) {
            try {
                pressDeviceBack();
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Step("Tap Back")
    public void tapBack() {
        WebElement back = driver.findElement(By.xpath("//*[@content-desc='Back']"));
        tap(back);
    }

    @Step("Device Back")
    public void pressDeviceBack() {
        driver.navigate().back();
    }

    @Step("Tap Close sheet (Change Assignment — never confirm)")
    public void tapCloseSheet() {
        By close = By.xpath("//*[@content-desc='Close sheet']");
        for (int i = 0; i < 6; i++) {
            List<WebElement> els = driver.findElements(close);
            if (!els.isEmpty()) {
                try {
                    Rectangle r = els.get(0).getRect();
                    int x = r.x + Math.max(1, r.width / 2);
                    int y = r.y + Math.min(Math.max(80, r.height / 3), Math.max(120, r.height - 40));
                    driver.executeScript("mobile: clickGesture",
                            java.util.Map.of("x", x, "y", y));
                    return;
                } catch (RuntimeException ignored) {
                    // retry
                }
            }
            // Dump 04: sheet title visible before Close sheet desc appears — tap upper scrim.
            if (isPresent(ComposeLocators.textView("Select machine for operator/driver"))) {
                driver.executeScript("mobile: clickGesture",
                        java.util.Map.of("x", 540, "y", 400));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (!isChangeAssignmentSheetVisible()) {
                    return;
                }
            }
            try {
                Thread.sleep(350);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        pressDeviceBack();
    }

    @Step("Tap Add Member")
    public void tapAddMember() {
        WebElement el = driver.findElement(ComposeLocators.textView("Add Member"));
        tap(el);
    }

    @Step("Tap first Change Assignment")
    public void tapChangeAssignment() {
        List<WebElement> els = driver.findElements(ComposeLocators.textView("Change Assignment"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Change Assignment not on screen");
        }
        tap(els.get(0));
        // Wait for sheet title (Close sheet desc may lag — dump 04 had empty descs).
        for (int i = 0; i < 12; i++) {
            if (isPresent(ComposeLocators.textView("Select machine for operator/driver"))) {
                return;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Step("Tap Change Assignment by index (0-based)")
    public void tapChangeAssignmentAt(int index) {
        List<WebElement> els = driver.findElements(ComposeLocators.textView("Change Assignment"));
        if (index < 0 || index >= els.size()) {
            throw new IllegalStateException("Change Assignment index " + index
                    + " out of " + els.size());
        }
        tap(els.get(index));
    }

    @Step("Header Back left-aligned")
    public boolean headerBackAligned() {
        List<WebElement> backs = driver.findElements(By.xpath("//*[@content-desc='Back']"));
        if (backs.isEmpty()) {
            return false;
        }
        Rectangle r = backs.get(0).getRect();
        return r.x < 120 && r.y < 220;
    }

    @Step("Add Member top-right in app bar (dump y≈109 x≈777)")
    public boolean addMemberTopRight() {
        List<WebElement> els = driver.findElements(ComposeLocators.textView("Add Member"));
        if (els.isEmpty()) {
            return false;
        }
        Rectangle r = els.get(0).getRect();
        return r.y < 220 && r.x > 600;
    }

    @Step("Swipe team list up")
    public void swipeListUp() {
        driver.executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 1400, "width", 600, "height", 700,
                        "direction", "up", "percent", 0.75));
    }

    @Step("Swipe team list down")
    public void swipeListDown() {
        driver.executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 800, "width", 600, "height", 700,
                        "direction", "down", "percent", 0.75));
    }

    @Step("Visible unique texts")
    public Set<String> visibleTexts() {
        Set<String> out = new LinkedHashSet<>();
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = el.getText();
            if (t != null && !t.isBlank()) {
                out.add(t.trim());
            }
        }
        return out;
    }
}
