package com.l2b.vendor.utils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * Same resolution order as L2B Customer: {@code -D} &gt; {@code L2B_*} env &gt; file.
 */
public final class Config {

    private static final String DEFAULTS = "config.properties";
    private static final String ENV_PREFIX = "L2B_";
    private static final Properties PROPS = load();

    private Config() {
    }

    private static Properties load() {
        Properties p = new Properties();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(DEFAULTS)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + DEFAULTS);
            }
            p.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + DEFAULTS, e);
        }
        return p;
    }

    private static String toEnvKey(String key) {
        return ENV_PREFIX + key.replace('.', '_').replace('-', '_').toUpperCase();
    }

    private static String resolve(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        String env = System.getenv(toEnvKey(key));
        if (env != null && !env.isBlank()) {
            return env;
        }
        return PROPS.getProperty(key);
    }

    public static String get(String key) {
        String value = resolve(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("No value for config key '" + key + "'");
        }
        return value.trim();
    }

    public static String get(String key, String fallback) {
        String value = resolve(key);
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    public static boolean getBoolean(String key, boolean fallback) {
        return Boolean.parseBoolean(get(key, String.valueOf(fallback)));
    }

    public static int getInt(String key, int fallback) {
        String raw = get(key, String.valueOf(fallback));
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Config key '" + key + "' must be an integer: " + raw, e);
        }
    }

    public static Duration getSeconds(String key, int fallbackSeconds) {
        return Duration.ofSeconds(getInt(key, fallbackSeconds));
    }
}
