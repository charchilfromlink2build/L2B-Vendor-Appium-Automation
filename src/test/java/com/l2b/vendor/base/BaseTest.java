package com.l2b.vendor.base;

import com.l2b.vendor.utils.AllureEnvironment;
import com.l2b.vendor.utils.DriverFactory;
import com.l2b.vendor.utils.DriverManager;
import com.l2b.vendor.utils.EnvironmentCheck;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

/**
 * Session lifecycle. Default {@link #noReset()} is true; smoke overrides it.
 */
public abstract class BaseTest {

    private static final Logger LOG = LogManager.getLogger(BaseTest.class);
    private static final AtomicBoolean ENVIRONMENT_CHECKED = new AtomicBoolean(false);
    private static final AtomicBoolean ALLURE_ENV_WRITTEN = new AtomicBoolean(false);

    protected boolean noReset() {
        return true;
    }

    /** False = one Appium session for every @Test in the class / suite (page-wise first launch). */
    protected boolean newSessionPerMethod() {
        return true;
    }

    @BeforeSuite(alwaysRun = true)
    public void verifyEnvironment() {
        if (ENVIRONMENT_CHECKED.compareAndSet(false, true)) {
            EnvironmentCheck.verify();
        }
    }

    @BeforeSuite(alwaysRun = true)
    public void writeAllureEnvironment() {
        if (ALLURE_ENV_WRITTEN.compareAndSet(false, true)) {
            AllureEnvironment.write();
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        if (!newSessionPerMethod()) {
            if (!DriverManager.hasDriver()) {
                DriverManager.set(DriverFactory.create(noReset()));
            }
            return;
        }
        DriverManager.set(DriverFactory.create(noReset()));
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
}
