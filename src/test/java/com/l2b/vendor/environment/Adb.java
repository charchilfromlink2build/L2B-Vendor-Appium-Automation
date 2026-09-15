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
