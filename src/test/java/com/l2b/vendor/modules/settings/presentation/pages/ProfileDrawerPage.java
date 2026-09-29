package com.l2b.vendor.modules.settings.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Profile drawer chrome (Home → Profile). Live dump
 * {@code /tmp/l2b-profile-drawer-0001-20260929} on {@code 9000000001}.
 * Left nav sheet. Never tap Confirm / Log out on the logout dialog.
 */
public class ProfileDrawerPage extends SplashScreen {

    public static final String[] MENU_ROWS = {
            "Account",
            "KYC",
            "Your machines",
            "Manage team",
            "Help",
            "Language",
            "Refer & Earn",
            "FAQ",
            "Terms & Services",
            "Policies",
            "Settings"
    };

    @Override
    @Step("Wait for Profile drawer")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Profile drawer did not appear", Duration.ofSeconds(12));
    }

    @Step("Check Profile drawer identity (Account + Log Out)")
    public boolean isDisplayedNow() {
        return isAccountVisible() && isLogOutVisible();
    }

    @Step("Check Account row")
    public boolean isAccountVisible() {
        return isPresent(ComposeLocators.textView("Account"));
    }

    @Step("Check Log Out CTA")
    public boolean isLogOutVisible() {
        return isPresent(ComposeLocators.textView("Log Out"))
                || isPresent(ComposeLocators.textView("Logout"));
    }

    @Step("Check vendor display name in header")
    public boolean isVendorNameVisible() {
        return isPresent(ComposeLocators.textView("Kasim Pathan"))
                || visibleTexts().stream().anyMatch(t -> t.length() > 2 && !MENU_ROWS_SET.contains(t)
                && !t.contains("Company") && !t.equals("Log Out") && !t.equals("Account")
                && Character.isUpperCase(t.charAt(0)) && t.contains(" "));
    }

    @Step("Check Company Id label")
    public boolean isCompanyIdLabelVisible() {
        return isPresent(ComposeLocators.textViewContains("Company Id"))
                || isPresent(ComposeLocators.textViewContains("Company ID"));
    }

    @Step("Check L2B company code")
    public boolean isCompanyCodeVisible() {
        return visibleTexts().stream().anyMatch(t -> t.startsWith("L2B-"));
    }

    @Step("Check Close navigation menu (right scrim)")
    public boolean isCloseNavigationMenuVisible() {
        return isPresent(By.xpath("//*[@content-desc='Close navigation menu']"));
    }

    @Step("Check Profile avatar still in chrome")
    public boolean isProfileDescVisible() {
        return isPresent(By.xpath("//*[@content-desc='Profile']"));
    }

    @Step("All menu rows present")
    public boolean allMenuRowsVisible() {
        for (String row : MENU_ROWS) {
            if (!isRowVisible(row)) {
                return false;
            }
        }
        return true;
    }

    @Step("Check menu row")
    public boolean isRowVisible(String row) {
        return isPresent(ComposeLocators.textView(row))
                || isPresent(ComposeLocators.textViewContains(row.replace(" & ", " ")));
    }

    @Step("Logout confirm dialog visible")
    public boolean isLogoutDialogVisible() {
        return isPresent(ComposeLocators.textView("Log Out?"))
                || (isPresent(ComposeLocators.textView("Cancel"))
                && isPresent(ComposeLocators.textView("Log out")));
    }

    @Step("Logout dialog body copy")
    public boolean isLogoutDialogBodyVisible() {
        return isPresent(ComposeLocators.textViewContains("Are you sure you want to log out"));
    }

    @Step("Cancel on logout dialog")
    public boolean isLogoutCancelVisible() {
        return isPresent(ComposeLocators.textView("Cancel"));
    }

    @Step("Confirm Log out on dialog (do not tap)")
    public boolean isLogoutConfirmVisible() {
        return isPresent(ComposeLocators.textView("Log out"));
    }

    @Step("Menu rows stacked top→bottom with equal gaps")
    public boolean menuRowsLayoutOk() {
        Rectangle prev = null;
        for (String row : MENU_ROWS) {
            Rectangle r = firstBounds(ComposeLocators.textView(row));
            if (r == null) {
                return false;
            }
            if (prev != null) {
                if (r.y <= prev.y) {
                    return false;
                }
                // Dump: ~145px row pitch (566-421 etc.)
                int gap = r.y - (prev.y + prev.height);
                if (gap < 40 || gap > 160) {
                    return false;
                }
                if (Math.abs(r.x - prev.x) > 8) {
                    return false;
                }
            }
            prev = r;
        }
        return true;
    }

    @Step("Header name above Company Id / code")
    public boolean headerLayoutOk() {
        Rectangle name = firstBounds(ComposeLocators.textView("Kasim Pathan"));
        Rectangle code = firstBoundsContains("L2B-");
        if (name == null || code == null) {
            return isVendorNameVisible() && isCompanyCodeVisible();
        }
        return name.y < code.y && name.x < 600;
    }

    @Step("Log Out below Settings with bottom inset")
    public boolean logOutBelowSettingsOk() {
        Rectangle settings = firstBounds(ComposeLocators.textView("Settings"));
        Rectangle logout = firstBounds(ComposeLocators.textView("Log Out"));
        if (settings == null || logout == null) {
            return false;
        }
        // Dump: Settings y=1871, Log Out y=2183
        return logout.y > settings.y + settings.height + 80
                && logout.y + logout.height < 2400;
    }

    @Step("Row left inset publish band")
    public boolean rowLeftInsetOk() {
        Rectangle account = firstBounds(ComposeLocators.textView("Account"));
        if (account == null) {
            return true;
        }
        // Dump: Account x=179
        return account.x >= 120 && account.x <= 260;
    }

    @Step("Scrim Close strip on right edge")
    public boolean scrimRightEdgeOk() {
        List<WebElement> els = driver.findElements(
                By.xpath("//*[@content-desc='Close navigation menu']"));
        if (els.isEmpty()) {
            return false;
        }
        Rectangle r = els.get(0).getRect();
        // Dump: [918,0][1080,2400]
        return r.x >= 800 && r.width >= 80;
    }

    @Step("Tap menu row by exact label")
    public void tapRow(String row) {
        clickText(row);
    }

    @Step("Tap Log Out (opens dialog — Cancel only afterward)")
    public void tapLogOut() {
        clickText("Log Out");
    }

    @Step("Tap Cancel on logout dialog")
    public void tapLogoutCancel() {
        clickText("Cancel");
    }

    @Step("Tap Close navigation menu scrim")
    public void tapCloseNavigationMenu() {
        List<WebElement> els = driver.findElements(
                By.xpath("//*[@content-desc='Close navigation menu']"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Close navigation menu not present");
        }
        // Prefer clickGesture mid-strip (right edge)
        Rectangle r = els.get(0).getRect();
        int x = r.x + Math.max(1, r.width / 2);
        int y = Math.min(1200, r.y + Math.max(1, r.height / 2));
        driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    @Step("Tap right scrim coordinates")
    public void tapOutsideScrim() {
        driver.executeScript("mobile: clickGesture", Map.of("x", 999, "y", 1200));
    }

    @Step("Device Back")
    public void pressDeviceBack() {
        driver.navigate().back();
    }

    @Step("Visible TextView texts")
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

    private static final Set<String> MENU_ROWS_SET = Set.of(MENU_ROWS);

    private void clickText(String text) {
        List<WebElement> els = driver.findElements(ComposeLocators.textView(text));
        if (els.isEmpty()) {
            els = driver.findElements(ComposeLocators.textViewContains(text));
        }
        if (els.isEmpty()) {
            throw new IllegalStateException("Text not found: " + text);
        }
        clickGestureOn(els.get(0));
    }

    private Rectangle firstBounds(By by) {
        List<WebElement> els = driver.findElements(by);
        if (els.isEmpty()) {
            return null;
        }
        return els.get(0).getRect();
    }

    private Rectangle firstBoundsContains(String fragment) {
        for (WebElement el : driver.findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el);
            if (t.contains(fragment)) {
                return el.getRect();
            }
        }
        return null;
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
