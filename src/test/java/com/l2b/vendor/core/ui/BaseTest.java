package com.l2b.vendor.core.ui;

import com.l2b.vendor.core.driver.DriverFactory;
import com.l2b.vendor.core.driver.DriverManager;
import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;

/**
 * Appium session only. Environment (JDK, device, Allure, noReset) lives in
 * each feature's {@code domain/*Environment}.
 */
public abstract class BaseTest {

    private static final Logger LOG = LogManager.getLogger(BaseTest.class);

    /** Feature module supplies this from its domain environment. */
    protected abstract boolean noReset();

    /** Feature module supplies this from its domain environment. */
    protected abstract boolean newSessionPerMethod();

    /** Smoke keeps auto-grant. Splash permission cases must return false. */
    protected boolean autoGrantPermissions() {
        return true;
    }

    /** Runs before the Appium session is created. Splash offline case uses this. */
    protected void beforeCreateDriver(Method method) {
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        beforeCreateDriver(method);
        if (!newSessionPerMethod()) {
            if (DriverManager.hasDriver() && !sessionAlive()) {
                quitDriver();
            }
            if (!DriverManager.hasDriver()) {
                DriverManager.set(DriverFactory.create(noReset(), autoGrantPermissions()));
            }
            return;
        }
        // Quit a leftover shared session (e.g. SmokeTest) before this class's first method.
        if (DriverManager.hasDriver()) {
            quitDriver();
        }
        DriverManager.set(DriverFactory.create(noReset(), autoGrantPermissions()));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (!newSessionPerMethod()) {
            return;
        }
        quitDriver();
    }

    @AfterSuite(alwaysRun = true)
    public void quitSharedSession() {
        quitDriver();
    }

    private void quitDriver() {
        try {
            DriverManager.quit();
        } catch (RuntimeException e) {
            LOG.warn("Error while quitting session: {}", e.getMessage());
        } finally {
            ThreadContext.remove("device");
        }
    }

    /** True only if the session can still talk to UiAutomator2. */
    private static boolean sessionAlive() {
        if (!DriverManager.hasDriver()) {
            return false;
        }
        try {
            DriverManager.get().getPageSource();
            return true;
        } catch (RuntimeException e) {
            LOG.warn("Stale Appium session, will recreate: {}", e.getMessage());
            return false;
        }
    }
}
