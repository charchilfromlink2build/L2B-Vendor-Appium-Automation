package com.l2b.vendor.modules.settings.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.AccountPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

/**
 * Shared 9000000001 path onto Account via Profile drawer. Never Save Changes /
 * Accept / Decline / Log Out Confirm.
 */
public abstract class AccountBaseTest extends ProfileDrawerBaseTest {

    @Step("Reach Account via Profile → Account")
    protected AccountPage reachAccount() {
        ProfileDrawerPage drawer = reachProfileDrawer();
        drawer.tapRow("Account");
        AccountPage page = new AccountPage();
        page.waitUntilLoaded();
        Allure.parameter("entry", "drawer-Account");
        Allure.parameter("after", classifyRentalNow());
        return page;
    }

    @Step("Dismiss toward Account (Cancel edits, never Save)")
    protected AccountPage dismissToAccount(AccountPage page) {
        for (int i = 0; i < 8; i++) {
            if (page.isDisplayedNow() && !page.isEditModeVisible()) {
                return page;
            }
            String named = classifyRentalNow();
            if ("launcher".equals(named)) {
                return page;
            }
            if (page.isEditModeVisible() || page.isCompanyUpdateModeVisible()) {
                page.tapCancel();
                sleepQuiet(700);
                continue;
            }
            if (page.isPhotoCloseVisible()) {
                page.tapPhotoClose();
                sleepQuiet(700);
                continue;
            }
            if ("account".equals(named) || page.isDisplayedNow()) {
                return page;
            }
            if ("home-drawer".equals(named) || profileDrawerNow()) {
                new ProfileDrawerPage().tapRow("Account");
                sleepQuiet(1000);
                if (page.isDisplayedNow()) {
                    return page;
                }
            }
            if ("home".equals(named) || "extend-time".equals(named)) {
                try {
                    new HomePage().tapDesc("Profile");
                    sleepQuiet(900);
                    new ProfileDrawerPage().tapRow("Account");
                    sleepQuiet(1000);
                    if (page.isDisplayedNow()) {
                        return page;
                    }
                } catch (RuntimeException ignored) {
                }
            }
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        if (!page.isDisplayedNow()) {
            return reachAccount();
        }
        return page;
    }

    @Step("Ensure Account read mode (Cancel if editing)")
    protected AccountPage ensureAccountReadMode(AccountPage page) {
        if (page.isEditModeVisible() || page.isCompanyUpdateModeVisible()) {
            page.tapCancel();
            sleepQuiet(700);
        }
        if (page.isPhotoCloseVisible()) {
            page.tapPhotoClose();
            sleepQuiet(700);
        }
        if (!page.isDisplayedNow()) {
            return dismissToAccount(page);
        }
        return page;
    }
}
