package com.l2b.vendor.environment;

import com.l2b.vendor.core.driver.DriverFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Thin adb wrapper for splash edge cases. Restores nothing by itself. */
public final class Adb {

    private Adb() {
    }

    public static void disableRadios() {
        run("shell", "svc", "wifi", "disable");
        run("shell", "svc", "data", "disable");
    }

    public static void enableRadios() {
        run("shell", "svc", "wifi", "enable");
        run("shell", "svc", "data", "enable");
    }

    /**
     * Process note (not a product bug): switching radios back on is not enough.
     * Offline cases left the emulator with IP ping but no hostname DNS, so the
     * next suite could not leave Language. Confirm a real name resolves before
     * the following test runs.
     */
    public static void ensureNetworkReady() {
        enableRadios();
        String host = Config.get("api.base.url", "https://qa.waardian.com")
                .replaceFirst("^https?://", "")
                .replaceFirst("/.*$", "");
        if (host.isBlank()) {
            host = "qa.waardian.com";
        }
        long deadline = System.currentTimeMillis() + 30_000;
        String last = "";
        while (System.currentTimeMillis() < deadline) {
            try {
                last = runAndRead("shell", "ping", "-c", "1", "-W", "4", host);
                if (hostnameResolved(last, host)) {
                    return;
                }
            } catch (RuntimeException e) {
                last = e.getMessage() == null ? "" : e.getMessage();
                if (hostnameResolved(last, host)) {
                    return;
                }
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for hostname DNS", e);
            }
        }
        throw new IllegalStateException(
                "Network not ready: radios on but hostname " + host + " did not resolve. last=" + last);
    }

    /** True when ping printed an IPv4/IPv6 for the name. ICMP loss is allowed. */
    static boolean hostnameResolved(String pingOutput, String host) {
        if (pingOutput == null || pingOutput.contains("unknown host") || pingOutput.contains("Name or service not known")) {
            return false;
        }
        return pingOutput.contains("PING " + host) || pingOutput.contains("bytes from");
    }

    public static void forceStop(String packageName) {
        run("shell", "am", "force-stop", packageName);
    }

    /** Home so a leftover Play Store / browser task is not the Back target. */
    public static void pressHome() {
        run("shell", "input", "keyevent", "KEYCODE_HOME");
    }

    /**
     * Drop this package from the recents stack. Force-stop alone leaves the
     * task; Android Back from Vendor can then surface Play Store from Case 5.
     */
    public static void removeRecentTasks(String packageName) {
        String dump = runAndRead("shell", "dumpsys", "activity", "recents");
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("Task\\{[^\\n]*?#(\\d+)[^\\n]*?" + java.util.regex.Pattern.quote(packageName))
                .matcher(dump);
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        for (String id : ids) {
            try {
                run("shell", "am", "task", "remove", id);
            } catch (RuntimeException ignored) {
                // Task may already be gone.
            }
        }
    }

    public static void run(String... args) {
        runAndRead(args);
    }

    public static String runAndRead(String... args) {
        List<String> command = new ArrayList<>();
        command.add(DriverFactory.adbBinary());
        String udid = Config.get("device.udid", "");
        if (!udid.isBlank()) {
            command.add("-s");
            command.add(udid);
        }
        command.addAll(List.of(args));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = process.waitFor();
            if (code != 0) {
                throw new IllegalStateException("adb failed (" + code + "): " + String.join(" ", command)
                        + "\n" + output);
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("adb failed: " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("adb interrupted", e);
        }
    }
}
