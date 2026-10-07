package com.shanesweeney.mobile.core;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Every run setting in one place. Each is read from {@code -Dkey}, then the {@code ENV_VAR},
 * then the default, so CI and local runs configure the framework the same way.
 */
public final class Config {
    public enum Platform { ANDROID, IOS }

    public static final Platform PLATFORM = "ios".equalsIgnoreCase(get("platform", "PLATFORM", "android"))
            ? Platform.IOS
            : Platform.ANDROID;
    public static final URL APPIUM_URL = url(get("appiumUrl", "APPIUM_URL", "http://127.0.0.1:4723"));
    /** AVD names on Android, simulator names on iOS; one per parallel thread. */
    public static final List<String> DEVICES = list(get("devices", "DEVICES", PLATFORM == Platform.IOS
            ? get("deviceName", "DEVICE_NAME", "iPhone 17")
            : get("avd", "AVD", "Pixel_8_API_35")));
    /** Null lets XCUITest pick the newest installed simulator runtime. */
    public static final String PLATFORM_VERSION = get("platformVersion", "PLATFORM_VERSION", null);
    public static final Duration WAIT_TIMEOUT = Duration.ofSeconds(Long.parseLong(get("waitTimeout", "WAIT_TIMEOUT", "10")));

    private Config() {}

    private static String get(String property, String env, String fallback) {
        return System.getProperty(property, System.getenv().getOrDefault(env, fallback));
    }

    private static List<String> list(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static URL url(String value) {
        try {
            return URI.create(value).toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Appium URL: " + value, e);
        }
    }
}
