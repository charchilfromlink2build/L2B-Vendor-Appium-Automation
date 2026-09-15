package com.l2b.vendor.core.driver;

import com.l2b.vendor.environment.Config;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 * Builds Appium sessions. Port of Customer {@code DriverFactory} (local only).
 */
public final class DriverFactory {

    private static final Logger LOG = LogManager.getLogger(DriverFactory.class);

    private DriverFactory() {
    }

    public static AndroidDriver create(boolean noReset) {
        return create(noReset, true);
    }

    public static AndroidDriver create(boolean noReset, boolean autoGrantPermissions) {
        cleanupStaleInstrumentation();
        UiAutomator2Options options = localOptions(noReset, autoGrantPermissions);
        URL server = serverUrl();
        String device = options.getDeviceName().orElse("unknown-device");
        ThreadContext.put("device", device);
        LOG.info("Creating Android session on {} via {} noReset={} autoGrant={}",
                device, server, noReset, autoGrantPermissions);
        return new AndroidDriver(server, options);
    }

    private static UiAutomator2Options localOptions(boolean noReset, boolean autoGrantPermissions) {
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName(Config.get("platform.name", "Android"))
                .setAutomationName(Config.get("automation.name", "UiAutomator2"))
                .setDeviceName(Config.get("device.name"))
                .setNewCommandTimeout(Config.getSeconds("appium.new.command.timeout", 180))
                .setAdbExecTimeout(Duration.ofSeconds(60))
                .setAutoGrantPermissions(autoGrantPermissions);

        options.setCapability("platformVersion", Config.get("platform.version", ""));
        String udid = Config.get("device.udid", "");
        if (!udid.isBlank()) {
            options.setUdid(udid);
        }
        options.setCapability("systemPort", Config.getInt("appium.system.port", 8200));
        options.setCapability("uiautomator2ServerInstallTimeout", 120_000);
        options.setCapability("uiautomator2ServerLaunchTimeout", 60_000);
        options.setCapability("disableWindowAnimation", true);

        String appPath = Config.get("app.path", "");
        if (!appPath.isBlank()) {
            Path resolved = Path.of(appPath).toAbsolutePath();
            if (Files.notExists(resolved)) {
                throw new IllegalStateException("app.path missing: " + resolved);
            }
            options.setApp(resolved.toString());
        } else {
            options.setAppPackage(Config.get("app.package"));
            options.setAppActivity(Config.get("app.activity"));
        }

        options.setNoReset(noReset);
        options.setFullReset(Config.getBoolean("app.full.reset", false));
        return options;
    }

    private static URL serverUrl() {
        String raw = Config.get("appium.url", "http://127.0.0.1:4723/");
        try {
            return new URI(raw).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalStateException("Invalid Appium server URL: " + raw, e);
        }
    }

    private static void cleanupStaleInstrumentation() {
        int systemPort = Config.getInt("appium.system.port", 8200);
        try {
            adb("shell", "am", "force-stop", "io.appium.uiautomator2.server");
            adb("shell", "am", "force-stop", "io.appium.uiautomator2.server.test");
        } catch (Exception e) {
            LOG.debug("Could not clean stale UiAutomator2: {}", e.getMessage());
        }
        try {
            adb("forward", "--remove", "tcp:" + systemPort);
        } catch (Exception e) {
            LOG.debug("No adb forward on systemPort {}: {}", systemPort, e.getMessage());
        }
    }

    private static void adb(String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(adbBinary());
        String udid = Config.get("device.udid", "");
        if (!udid.isBlank()) {
            command.add("-s");
            command.add(udid);
        }
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        process.waitFor();
    }

    public static String adbBinary() {
        String home = System.getenv("ANDROID_HOME");
        if (home == null || home.isBlank()) {
            home = System.getenv("ANDROID_SDK_ROOT");
        }
        if (home != null && !home.isBlank()) {
            return Path.of(home, "platform-tools", "adb").toString();
        }
        return "adb";
    }
}
