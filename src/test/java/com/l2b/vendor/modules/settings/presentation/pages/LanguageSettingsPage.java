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
 * In-app Language settings (Profile drawer → Language). Live dumps
 * {@code /tmp/l2b-lang-settings-0001-20261001}, {@code /tmp/l2b-lang-matrix-0001-20261001}.
 * Save is allowed here (apply locale). Always restore English after reflection probes.
 * Never Accept / Decline / Log Out Confirm.
 */
public class LanguageSettingsPage extends SplashScreen {

    public static final String[] LANGUAGE_LABELS = {"English", "हिंदी", "తెలుగు", "ಕನ್ನಡ"};

    @Override
    @Step("Wait for Language settings")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Language settings did not appear", Duration.ofSeconds(12));
    }

    @Step("Check Language settings identity")
    public boolean isDisplayedNow() {
        return (isChooseLanguageVisible() || isAllLanguageVisible())
                && (isEnglishVisible() || isHindiVisible())
                && isSaveVisible();
    }

    @Step("Choose the language / भाषा चुनें title")
    public boolean isChooseLanguageVisible() {
        return isPresent(ComposeLocators.textView("Choose the language"))
                || isPresent(ComposeLocators.textView("भाषा चुनें"))
                || isPresent(ComposeLocators.textViewContains("Choose the language"))
                || isPresent(ComposeLocators.textViewContains("भाषा"));
    }

    @Step("All language / सभी भाषाएं")
    public boolean isAllLanguageVisible() {
        return isPresent(ComposeLocators.textView("All language"))
                || isPresent(ComposeLocators.textView("सभी भाषाएं"))
                || isPresent(ComposeLocators.textViewContains("All language"))
                || isPresent(ComposeLocators.textViewContains("सभी भाषा"));
    }

    @Step("English option")
    public boolean isEnglishVisible() {
        return isPresent(ComposeLocators.textView("English"));
    }

    @Step("Hindi option हिंदी")
    public boolean isHindiVisible() {
        return isPresent(ComposeLocators.textView("हिंदी"));
    }

    @Step("Telugu option")
    public boolean isTeluguVisible() {
        return isPresent(ComposeLocators.textView("తెలుగు"));
    }

    @Step("Kannada option")
    public boolean isKannadaVisible() {
        return isPresent(ComposeLocators.textView("ಕನ್ನಡ"));
    }

    @Step("Default badge")
    public boolean isDefaultVisible() {
        return isPresent(ComposeLocators.textView("Default"))
                || isPresent(ComposeLocators.textView("डिफ़ॉल्ट"));
    }

    @Step("Save / सेव करें")
    public boolean isSaveVisible() {
        return isPresent(ComposeLocators.textView("Save"))
                || isPresent(ComposeLocators.textView("सेव करें"));
    }

    @Step("Back")
    public boolean isBackVisible() {
        return isPresent(By.xpath("//*[@content-desc='Back']"));
    }

    @Step("All four language rows visible")
    public boolean allLanguageRowsVisible() {
        return isEnglishVisible() && isHindiVisible() && isTeluguVisible() && isKannadaVisible();
    }

    @Step("Header Back left of title")
    public boolean headerBackAligned() {
        Rectangle title = firstBounds(ComposeLocators.textView("Choose the language"));
        if (title == null) {
            title = firstBounds(ComposeLocators.textView("भाषा चुनें"));
        }
        List<WebElement> backs = driver.findElements(By.xpath("//*[@content-desc='Back']"));
        if (title == null || backs.isEmpty()) {
            return false;
        }
        Rectangle back = backs.get(0).getRect();
        return Math.abs((title.y + title.height / 2) - (back.y + back.height / 2)) <= 40
                && back.x < title.x;
    }

    @Step("Save near bottom")
    public boolean saveNearBottomOk() {
        Rectangle save = firstBounds(ComposeLocators.textView("Save"));
        if (save == null) {
            save = firstBounds(ComposeLocators.textView("सेव करें"));
        }
        return save != null && save.y > 1800;
    }

    @Step("Select English")
    public void selectEnglish() {
        clickText("English");
    }

    @Step("Select Hindi")
    public void selectHindi() {
        clickText("हिंदी");
    }

    @Step("Select Telugu")
    public void selectTelugu() {
        clickText("తెలుగు");
    }

    @Step("Select Kannada")
    public void selectKannada() {
        clickText("ಕನ್ನಡ");
    }

    @Step("Tap Save (apply language)")
    public void tapSave() {
        if (isPresent(ComposeLocators.textView("Save"))) {
            clickText("Save");
            return;
        }
        if (isPresent(ComposeLocators.textView("सेव करें"))) {
            clickText("सेव करें");
            return;
        }
        throw new IllegalStateException("Save not visible");
    }

    @Step("Tap header Back")
    public void tapHeaderBack() {
        List<WebElement> els = driver.findElements(By.xpath("//*[@content-desc='Back']"));
        if (els.isEmpty()) {
            throw new IllegalStateException("Back not visible");
        }
        try {
            clickGestureOn(els.get(0).findElement(By.xpath("./..")));
        } catch (RuntimeException e) {
            clickGestureOn(els.get(0));
        }
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

    private void clickGestureOn(WebElement el) {
        Rectangle r = el.getRect();
        driver.executeScript("mobile: clickGesture",
                Map.of("x", r.x + Math.max(1, r.width / 2), "y", r.y + Math.max(1, r.height / 2)));
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
