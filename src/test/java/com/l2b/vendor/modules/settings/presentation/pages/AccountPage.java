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
 * Account / Profile Info from Profile drawer. Live dump
 * {@code /tmp/l2b-account-0001-20260929} on {@code 9000000001}.
 * Never tap Save Changes — Cancel / Back only.
 */
public class AccountPage extends SplashScreen {

    @Override
    @Step("Wait for Account / Profile Info")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Account screen did not appear", Duration.ofSeconds(12));
    }

    @Step("Check Account identity")
    public boolean isDisplayedNow() {
        return isProfileInfoVisible()
                && (isFullNameLabelVisible() || isMobileLabelVisible());
    }

    @Step("Check Profile Info heading")
    public boolean isProfileInfoVisible() {
        return isPresent(ComposeLocators.textView("Profile Info"));
    }

    @Step("Check Account header title")
    public boolean isAccountTitleVisible() {
        return isPresent(ComposeLocators.textView("Account"));
    }

    @Step("Check Back")
    public boolean isBackVisible() {
        return isPresent(By.xpath("//*[@content-desc='Back']"));
    }

    @Step("Check Edit affordance")
    public boolean isEditVisible() {
        return isPresent(ComposeLocators.textView("Edit"));
    }

    @Step("Check Update affordance (Company Info)")
    public boolean isUpdateVisible() {
        return isPresent(ComposeLocators.textView("Update"));
    }

    @Step("Check Full Name label")
    public boolean isFullNameLabelVisible() {
        return isPresent(ComposeLocators.textView("Full Name"));
    }

    @Step("Check Mobile Number label")
    public boolean isMobileLabelVisible() {
        return isPresent(ComposeLocators.textView("Mobile Number"));
    }

    @Step("Check Company Info heading")
    public boolean isCompanyInfoVisible() {
        return isPresent(ComposeLocators.textView("Company Info"));
    }

    @Step("Check Company Name label")
    public boolean isCompanyNameLabelVisible() {
        return isPresent(ComposeLocators.textView("Company Name"));
    }

    @Step("Check Email Address label")
    public boolean isEmailLabelVisible() {
        return isPresent(ComposeLocators.textView("Email Address"));
    }

    @Step("Check Profile photo")
    public boolean isProfilePhotoVisible() {
        return isPresent(By.xpath("//*[@content-desc='Profile photo']"));
    }

    @Step("Check phone value 9000000001")
    public boolean isPhoneValueVisible() {
        return hasEditText("9000000001")
                || isPresent(ComposeLocators.textView("9000000001"));
    }

    @Step("Check vendor display name")
    public boolean isVendorNameVisible() {
        if (hasEditText("Kasim Pathan") || hasEditTextContains("Kasim")) {
            return true;
        }
        if (isPresent(ComposeLocators.textView("Kasim Pathan"))
                || isPresent(ComposeLocators.textViewContains("Kasim"))) {
            return true;
        }
        return visibleTexts().stream().anyMatch(t -> t.contains("Kasim") || t.contains("Pathan"));
    }

    @Step("Check email value contains @")
    public boolean isEmailValueVisible() {
        return visibleEditTexts().stream().anyMatch(t -> t.contains("@"))
                || visibleTexts().stream().anyMatch(t -> t.contains("@"));
    }

    @Step("Check company name value present")
    public boolean isCompanyNameValueVisible() {
        return hasEditText("kasim")
                || visibleEditTexts().stream().anyMatch(t -> t.equalsIgnoreCase("kasim")
                || (t.length() > 1 && !t.contains("@") && !t.matches("\\d+")));
    }

    @Step("Edit mode: Cancel + Save Changes")
    public boolean isEditModeVisible() {
        return isPresent(ComposeLocators.textView("Cancel"))
                && isPresent(ComposeLocators.textView("Save Changes"));
    }

    @Step("Company Update mode: Cancel + Save Changes")
    public boolean isCompanyUpdateModeVisible() {
        return isPresent(ComposeLocators.textView("Cancel"))
                && isPresent(ComposeLocators.textView("Save Changes"))
                && !isPresent(ComposeLocators.textView("Update"));
    }

    @Step("Photo preview Close")
    public boolean isPhotoCloseVisible() {
        return isPresent(By.xpath("//*[@content-desc='Close']"));
    }

    @Step("Header Back left of Account title")
    public boolean headerBackAligned() {
        Rectangle title = firstBounds(ComposeLocators.textView("Account"));
        List<WebElement> backs = driver.findElements(By.xpath("//*[@content-desc='Back']"));
        if (title == null || backs.isEmpty()) {
            return false;
        }
        Rectangle back = backs.get(0).getRect();
        return Math.abs((title.y + title.height / 2) - (back.y + back.height / 2)) <= 40
                && back.x < title.x;
    }

    @Step("Title left inset publish band")
    public boolean titleLeftInsetOk() {
        Rectangle title = firstBounds(ComposeLocators.textView("Account"));
        if (title == null) {
            return true;
        }
        // Dump: Account x=126
        return title.x >= 80 && title.x <= 200;
    }

    @Step("Profile Info and Edit share band")
    public boolean profileInfoEditLayoutOk() {
        Rectangle info = firstBounds(ComposeLocators.textView("Profile Info"));
        Rectangle edit = firstBounds(ComposeLocators.textView("Edit"));
        if (info == null || edit == null) {
            return false;
        }
        return Math.abs(info.y - edit.y) <= 40 && info.x < edit.x;
    }

    @Step("Company Info and Update share band")
    public boolean companyInfoUpdateLayoutOk() {
        Rectangle info = firstBounds(ComposeLocators.textView("Company Info"));
        Rectangle update = firstBounds(ComposeLocators.textView("Update"));
        if (info == null || update == null) {
            return false;
        }
        return Math.abs(info.y - update.y) <= 40 && info.x < update.x;
    }

    @Step("Field labels stacked Full Name → Mobile → Company → Email")
    public boolean fieldLabelsLayoutOk() {
        Rectangle full = firstBounds(ComposeLocators.textView("Full Name"));
        Rectangle mobile = firstBounds(ComposeLocators.textView("Mobile Number"));
        Rectangle company = firstBounds(ComposeLocators.textView("Company Name"));
        Rectangle email = firstBounds(ComposeLocators.textView("Email Address"));
        if (full == null || mobile == null || company == null || email == null) {
            return false;
        }
        return full.y < mobile.y && mobile.y < company.y && company.y < email.y
                && Math.abs(full.x - mobile.x) <= 8;
    }

    @Step("Tap Edit (opens Cancel + Save Changes — never Save)")
    public void tapEdit() {
        clickText("Edit");
    }

    @Step("Tap Update (company edit — never Save)")
    public void tapUpdate() {
        clickText("Update");
    }

    @Step("Tap Cancel (edit / update mode)")
    public void tapCancel() {
        clickText("Cancel");
    }

    @Step("Tap Profile photo")
    public void tapProfilePhoto() {
        List<WebElement> els = driver.findElements(By.xpath("//*[@content-desc='Profile photo']"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Profile photo not visible");
        }
        clickGestureOn(els.get(0));
    }

    @Step("Tap photo Close")
    public void tapPhotoClose() {
        List<WebElement> els = driver.findElements(By.xpath("//*[@content-desc='Close']"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Close not visible on photo");
        }
        clickGestureOn(els.get(0));
    }

    @Step("Tap header Back")
    public void tapHeaderBack() {
        List<WebElement> els = driver.findElements(By.xpath("//*[@content-desc='Back']"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Back not visible");
        }
        clickGestureOn(els.get(0));
    }

    @Step("Device Back")
    public void pressDeviceBack() {
        driver.navigate().back();
    }

    @Step("Visible texts")
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

    @Step("Visible EditText values")
    public Set<String> visibleEditTexts() {
        Set<String> out = new LinkedHashSet<>();
        for (WebElement el : driver.findElements(By.className("android.widget.EditText"))) {
            String t = safeText(el).trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private boolean hasEditText(String exact) {
        return !driver.findElements(
                By.xpath("//android.widget.EditText[@text=" + xpathLit(exact) + "]")).isEmpty();
    }

    private boolean hasEditTextContains(String fragment) {
        return !driver.findElements(
                By.xpath("//android.widget.EditText[contains(@text," + xpathLit(fragment) + ")]"))
                .isEmpty();
    }

    private static String xpathLit(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        return "\"" + value + "\"";
    }

    private void clickText(String text) {
        List<WebElement> els = driver.findElements(ComposeLocators.textView(text));
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
