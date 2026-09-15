package com.l2b.vendor.environment;

import com.l2b.vendor.environment.Config;
import com.l2b.vendor.core.driver.DriverManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Capabilities;

/**
 * Writes Allure {@code environment.properties} and {@code categories.json} into
 * the results directory. Never throws to the test run.
 */
public final class AllureEnvironment {

    private static final Logger LOG = LogManager.getLogger(AllureEnvironment.class);

    private AllureEnvironment() {
    }

    public static void write() {
        Path resultsDir = Path.of(System.getProperty("allure.results.directory", "target/allure-results"));
        try {
            Files.createDirectories(resultsDir);
            writeEnvironment(resultsDir.resolve("environment.properties"));
            writeCategories(resultsDir.resolve("categories.json"));
        } catch (IOException e) {
            LOG.warn("Could not write Allure environment metadata: {}", e.getMessage());
        }
    }

    private static void writeEnvironment(Path dest) throws IOException {
        Properties props = new Properties();
        props.setProperty("Platform", Config.get("platform.name", "Android"));
        props.setProperty("Platform.Version", Config.get("platform.version", "16"));
        props.setProperty("Device", Config.get("device.name", "Medium_Phone_API_36.1"));
        props.setProperty("App.Package", Config.get("app.package", "com.l2b.app.qa"));
        props.setProperty("API.Base.URL", Config.get("api.base.url", "https://qa.waardian.com"));
        props.setProperty("Java.Version", String.valueOf(System.getProperty("java.version", "")));
        props.setProperty("Execution.Start", Instant.now().toString());
        appiumVersionFromSession().ifPresent(v -> props.setProperty("Appium.Version", v));
        try (OutputStream out = Files.newOutputStream(dest)) {
            props.store(out, "Allure environment — written at runtime");
        }
    }

    private static java.util.Optional<String> appiumVersionFromSession() {
        if (!DriverManager.hasDriver()) {
            return java.util.Optional.empty();
        }
        try {
            Capabilities caps = DriverManager.get().getCapabilities();
            Object version = caps.getCapability("appiumVersion");
            if (version == null) {
                version = caps.getCapability("appium:appiumVersion");
            }
            if (version == null) {
                return java.util.Optional.empty();
            }
            String raw = String.valueOf(version).trim();
            if (raw.isEmpty() || "unknown".equalsIgnoreCase(raw) || "null".equalsIgnoreCase(raw)) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(raw);
        } catch (RuntimeException e) {
            LOG.warn("Could not read Appium version from session: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }

    private static void writeCategories(Path dest) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream("categories.json")) {
            if (in == null) {
                LOG.warn("Classpath resource categories.json is missing");
                return;
            }
            Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
