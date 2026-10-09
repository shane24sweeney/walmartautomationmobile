package com.shanesweeney.mobile.core;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Every run setting in one place. Each is read from {@code -Dkey}, then the {@code ENV_VAR},
 * then the default, so CI and local runs configure the framework the same way.
 */
public final class Config {
    public enum Platform { ANDROID, IOS }

    /** Where sessions run: local emulators/simulators, real devices on this machine, or BrowserStack. */
    public enum Target { EMULATOR, DEVICE, BROWSERSTACK }

    public static final Platform PLATFORM = Platform.valueOf(get("platform", "PLATFORM", "android").toUpperCase(Locale.ROOT));
    public static final Target TARGET = Target.valueOf(get("target", "TARGET", "emulator").toUpperCase(Locale.ROOT));
    /**
     * Comma-separated Appium servers; device N uses server N modulo the list, so several servers can
     * share the load. Local ones are started if not running; remote servers or grids are used as is.
     */
    public static final List<URL> APPIUM_URLS = list(get("appiumUrl", "APPIUM_URL", "http://127.0.0.1:4723"))
            .stream().map(Config::url).toList();
    /**
     * One entry per parallel thread: AVD or simulator names for {@code emulator}, UDIDs for
     * {@code device}, and {@code "Device name:OS version"} for {@code browserstack}.
     */
    public static final List<String> DEVICES = list(get("devices", "DEVICES", PLATFORM == Platform.IOS
            ? get("deviceName", "DEVICE_NAME", "iPhone 17")
            : get("avd", "AVD", "Pixel_8_API_35")));
    /** OS version the device must run. Null accepts any, and lets XCUITest pick the newest simulator runtime. */
    public static final String PLATFORM_VERSION = get("platformVersion", "PLATFORM_VERSION", null);
    /** App build to install: a local path, a URL, or a BrowserStack {@code bs://} ID. Null uses apps/. */
    public static final String APP = get("app", "APP", null);
    /** Apple team ID for signing WebDriverAgent on a real iPhone; unused elsewhere. */
    public static final String XCODE_ORG_ID = get("xcodeOrgId", "XCODE_ORG_ID", null);
    public static final Duration WAIT_TIMEOUT = Duration.ofSeconds(Long.parseLong(get("waitTimeout", "WAIT_TIMEOUT", "10")));
    /** Keep app data between sessions instead of clearing it; cannot be combined with {@link #FULL_RESET}. */
    public static final boolean NO_RESET = Boolean.parseBoolean(get("noReset", "NO_RESET", "false"));
    /** Uninstall the app before and after each session, for a clean install every time. */
    public static final boolean FULL_RESET = Boolean.parseBoolean(get("fullReset", "FULL_RESET", "false"));
    /** Device language and region, e.g. {@code es} and {@code ES}. Null leaves the device's own setting. */
    public static final String LANGUAGE = get("language", "LANGUAGE", null);
    public static final String LOCALE = get("locale", "LOCALE", null);
    /** Boot emulators and simulators without a window, e.g. on CI. */
    public static final boolean HEADLESS = Boolean.parseBoolean(get("headless", "HEADLESS", "false"));

    // Cloud credentials come from the environment only, so they never appear on a command line.
    public static final String BROWSERSTACK_USERNAME = System.getenv("BROWSERSTACK_USERNAME");
    public static final String BROWSERSTACK_ACCESS_KEY = System.getenv("BROWSERSTACK_ACCESS_KEY");
    public static final URL BROWSERSTACK_URL = url("https://hub-cloud.browserstack.com/wd/hub");
    /** Groups sessions from one run under a single build in the BrowserStack dashboard. */
    public static final String BUILD_NAME = get("buildName", "BUILD_NAME", "local");

    static {
        if (NO_RESET && FULL_RESET) {
            throw new IllegalStateException("noReset and fullReset are mutually exclusive; set at most one");
        }
    }

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
            throw new IllegalArgumentException("Invalid URL: " + value, e);
        }
    }
}
