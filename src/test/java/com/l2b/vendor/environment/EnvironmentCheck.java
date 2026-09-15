package com.l2b.vendor.environment;

import com.l2b.vendor.environment.Config;
import com.l2b.vendor.core.driver.DriverFactory;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Fail-fast suite pre-flight so missing JDK/device/app/Appium do not become
 * {@code SessionNotCreatedException}.
 */
public final class EnvironmentCheck {

    private EnvironmentCheck() {
    }

    public static void verify() {
        checkJava17();
        checkDeviceOnline();
        checkPackageInstalledIfNeeded();
        checkAppiumReachable();
    }

    private static void checkJava17() {
        String found = System.getProperty("java.version");
        if (majorVersion(found) != 17) {
            throw new IllegalStateException(
                    "Running on Java " + found + ", expected 17. Run: "
                            + "export JAVA_HOME=/opt/homebrew/opt/openjdk@17");
        }
    }

    private static int majorVersion(String javaVersion) {
        if (javaVersion == null || javaVersion.isBlank()) {
            return -1;
        }
        String normalized = javaVersion.startsWith("1.")
                ? javaVersion.substring(2)
                : javaVersion;
        String major = normalized.split("[^0-9]")[0];
        if (major.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(major);
    }

    private static void checkDeviceOnline() {
        String udid = Config.get("device.udid");
        String output = run(DriverFactory.adbBinary(), "devices");
        String state = deviceState(output, udid);
        if (state == null) {
            throw new IllegalStateException(
                    "Device " + udid + " not found in 'adb devices'. Is the emulator running?");
        }
        if ("unauthorized".equals(state)) {
            throw new IllegalStateException(
                    "Device " + udid + " is unauthorized. Run: "
                            + "adb kill-server && adb start-server, then accept the USB debugging "
                            + "dialog on the emulator.");
        }
        if ("offline".equals(state)) {
            throw new IllegalStateException(
                    "Device " + udid + " is offline. Wait for boot to finish, or cold boot with: "
                            + "emulator -avd " + Config.get("device.name") + " -no-snapshot-load");
        }
        if (!"device".equals(state)) {
            throw new IllegalStateException(
                    "Device " + udid + " is " + state + ", expected state device.");
        }
    }

    private static String deviceState(String adbDevicesOutput, String udid) {
        for (String line : adbDevicesOutput.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("List of devices")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length >= 2 && udid.equals(parts[0])) {
                return parts[1];
            }
        }
        return null;
    }

    private static void checkPackageInstalledIfNeeded() {
        if (!Config.get("app.path", "").isBlank()) {
            return;
        }
        String udid = Config.get("device.udid");
        String pkg = Config.get("app.package");
        String output = run(DriverFactory.adbBinary(), "-s", udid, "shell", "pm", "list", "packages", pkg);
        if (!output.contains("package:" + pkg)) {
            throw new IllegalStateException(
                    "Package " + pkg + " is not installed on " + udid
                            + ". Install the QA APK first (adb install -r <path-to-apk>).");
        }
    }

    private static void checkAppiumReachable() {
        String base = Config.get("appium.url", "http://127.0.0.1:4723/");
        String statusUrl = base.endsWith("/") ? base + "status" : base + "/status";
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(statusUrl))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(appiumUnreachable(statusUrl));
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(appiumUnreachable(statusUrl), e);
        }
    }

    private static String appiumUnreachable(String url) {
        return "Appium not reachable at " + url
                + ". Start it with: appium --address 127.0.0.1 --port 4723";
    }

    private static String run(String... command) {
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to run: " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running: " + String.join(" ", command), e);
        }
    }
}
