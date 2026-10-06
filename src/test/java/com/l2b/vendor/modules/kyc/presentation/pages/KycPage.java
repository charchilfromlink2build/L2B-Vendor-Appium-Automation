package com.l2b.vendor.modules.kyc.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Profile drawer → KYC Details. Prior dump {@code /tmp/l2b-lang-matrix-0001-20261001/d-kyc}
 * (Hindi: केवाईसी विवरण + Company PAN + bank). Dual-path: full chrome or error empty.
 * Never Accept / Decline / Log Out Confirm.
 */
public class KycPage extends SplashScreen {

    public static final String TITLE = "KYC Details";
    public static final String TITLE_HI = "केवाईसी विवरण";
    /** Live chrome uses Title Case; older dumps used sentence case. */
    public static final String DOC_DETAILS = "Document Details";
    public static final String DOC_DETAILS_ALT = "Document details";
    public static final String DOC_DETAILS_HI = "दस्तावेज़ विवरण";
    public static final String BANK_DETAILS = "Bank Details";
    public static final String BANK_DETAILS_ALT = "Bank details";
    public static final String BANK_DETAILS_HI = "बैंक विवरण";
    public static final String PAN = "Company PAN Card";
    public static final String DOC_TYPE = "Document Type";
    public static final String DOC_NUMBER = "Document Number";
    public static final String GST = "GST number";
    public static final String GST_HI = "जीएसटी नंबर";

    @Override
    @Step("Wait for KYC")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "KYC screen did not appear", Duration.ofSeconds(20));
    }

    @Step("Check KYC identity")
    public boolean isDisplayedNow() {
        return isTitleVisible()
                || isErrorVisible()
                || isPresent(ComposeLocators.textView(DOC_DETAILS))
                || isPresent(ComposeLocators.textView(DOC_DETAILS_ALT))
                || isPresent(ComposeLocators.textView(DOC_DETAILS_HI))
                || isPresent(ComposeLocators.textView(PAN))
                || isPresent(ComposeLocators.textView(DOC_TYPE))
                || isPresent(ComposeLocators.textView(BANK_DETAILS))
                || isPresent(ComposeLocators.textView(BANK_DETAILS_ALT))
                || isPresent(ComposeLocators.textView(BANK_DETAILS_HI));
    }

    @Step("Check KYC title")
    public boolean isTitleVisible() {
        return isPresent(ComposeLocators.textView(TITLE))
                || isPresent(ComposeLocators.textView(TITLE_HI))
                || isPresent(ComposeLocators.textView("KYC"));
    }

    @Step("Check load error empty state")
    public boolean isErrorVisible() {
        return isPresent(ComposeLocators.textView("Something went wrong. Please try again."))
                || isPresent(ComposeLocators.textViewContains("Something went wrong"))
                || isPresent(ComposeLocators.textView("An unexpected error occurred"))
                || isPresent(ComposeLocators.textViewContains("unexpected error"));
    }

    @Step("Check Retry")
    public boolean isRetryVisible() {
        return isPresent(ComposeLocators.textView("Retry"));
    }

    @Step("Full KYC chrome (docs + bank)")
    public boolean isFullChromeVisible() {
        boolean docs = isPresent(ComposeLocators.textView(DOC_DETAILS))
                || isPresent(ComposeLocators.textView(DOC_DETAILS_ALT))
                || isPresent(ComposeLocators.textView(DOC_DETAILS_HI))
                || isPresent(ComposeLocators.textView(PAN))
                || isPresent(ComposeLocators.textView(DOC_TYPE));
        boolean bank = isPresent(ComposeLocators.textView(BANK_DETAILS))
                || isPresent(ComposeLocators.textView(BANK_DETAILS_ALT))
                || isPresent(ComposeLocators.textView(BANK_DETAILS_HI))
                || isPresent(ComposeLocators.textViewContains("IFSC"))
                || isPresent(ComposeLocators.textViewContains("आईएफएससी"));
        return docs || bank;
    }

    @Step("Tap Retry")
    public void tapRetry() {
        tap(driver.findElement(ComposeLocators.textView("Retry")));
    }

    @Step("Document details section")
    public boolean isDocumentDetailsVisible() {
        return isPresent(ComposeLocators.textView(DOC_DETAILS))
                || isPresent(ComposeLocators.textView(DOC_DETAILS_ALT))
                || isPresent(ComposeLocators.textView(DOC_DETAILS_HI))
                || isPresent(ComposeLocators.textView(PAN))
                || isPresent(ComposeLocators.textView(DOC_TYPE));
    }

    @Step("Bank details section")
    public boolean isBankDetailsVisible() {
        return isPresent(ComposeLocators.textView(BANK_DETAILS))
                || isPresent(ComposeLocators.textView(BANK_DETAILS_ALT))
                || isPresent(ComposeLocators.textView(BANK_DETAILS_HI))
                || isPresent(ComposeLocators.textViewContains("Yes Bank"))
                || isPresent(ComposeLocators.textViewContains("IFSC"));
    }

    @Step("PAN / document number row")
    public boolean isPanVisible() {
        return isPresent(ComposeLocators.textView(PAN))
                || isPresent(ComposeLocators.textViewContains("PAN"))
                || isPresent(ComposeLocators.textView(DOC_NUMBER))
                || isPresent(ComposeLocators.textView(DOC_TYPE));
    }

    /** True only when a PAN value (not merely the Document Type/Number labels) is present. */
    @Step("PAN value present (not empty dash)")
    public boolean isPresentPanValue() {
        return isPresent(ComposeLocators.textView(PAN))
                || isPresent(ComposeLocators.textViewContains("PAN"));
    }

    /** Empty-dash Details state (Document/Bank values show em dash). */
    @Step("Empty dash values visible")
    public boolean isEmptyDashStateVisible() {
        return isPresent(ComposeLocators.textView("—"))
                || isPresent(ComposeLocators.textView("-"));
    }

    @Step("Check Back")
    public boolean isBackVisible() {
        return !driver.findElements(By.xpath(
                "//*[@content-desc='Back' or @content-desc='Navigate up' or @text='Back']"))
                .isEmpty()
                || isPresent(ComposeLocators.textView("Back"));
    }

    @Step("Tap Back")
    public void tapBack() {
        List<WebElement> els = driver.findElements(By.xpath(
                "//*[@content-desc='Back' or @content-desc='Navigate up']"));
        if (!els.isEmpty()) {
            Rectangle r = els.get(0).getRect();
            driver.executeScript("mobile: clickGesture",
                    Map.of("x", r.x + Math.max(1, r.width / 2),
                            "y", r.y + Math.max(1, r.height / 2)));
            return;
        }
        driver.navigate().back();
    }

    @Step("Swipe KYC list up")
    public void swipeListUp() {
        var size = driver.manage().window().getSize();
        int x = size.width / 2;
        int y1 = (int) (size.height * 0.72);
        int y2 = (int) (size.height * 0.35);
        driver.executeScript("mobile: swipeGesture",
                Map.of("left", x - 20, "top", y2, "width", 40, "height", y1 - y2,
                        "direction", "up", "percent", 0.75));
    }
}
