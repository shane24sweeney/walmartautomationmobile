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
    /** A local server is started if none is running; point this at a remote server or grid to use its devices. */
    public static final URL APPIUM_URL = url(get("appiumUrl", "APPIUM_URL", "http://127.0.0.1:4723"));
    /**
     * One entry per parallel thread: AVD or simulator names for {@code emulator}, UDIDs for
     * {@code device}, and {@code "Device name:OS version"} for {@code browserstack}.
     */
    public static final List<String> DEVICES = list(get("devices", "DEVICES", PLATFORM == Platform.IOS
            ? get("deviceName", "DEVICE_NAME", "iPhone 17")
            : get("avd", "AVD", "Pixel_8_API_35")));
    /** Null lets XCUITest pick the newest installed simulator runtime. */
    public static final String PLATFORM_VERSION = get("platformVersion", "PLATFORM_VERSION", null);
    /** App build to install: a local path, a URL, or a BrowserStack {@code bs://} ID. Null uses apps/. */
    public static final String APP = get("app", "APP", null);
    /** Apple team ID for signing WebDriverAgent on a real iPhone; unused elsewhere. */
    public static final String XCODE_ORG_ID = get("xcodeOrgId", "XCODE_ORG_ID", null);
    public static final Duration WAIT_TIMEOUT = Duration.ofSeconds(Long.parseLong(get("waitTimeout", "WAIT_TIMEOUT", "10")));

    // Cloud credentials come from the environment only, so they never appear on a command line.
    public static final String BROWSERSTACK_USERNAME = System.getenv("BROWSERSTACK_USERNAME");
    public static final String BROWSERSTACK_ACCESS_KEY = System.getenv("BROWSERSTACK_ACCESS_KEY");
    public static final URL BROWSERSTACK_URL = url("https://hub-cloud.browserstack.com/wd/hub");
    /** Groups sessions from one run under a single build in the BrowserStack dashboard. */
    public static final String BUILD_NAME = get("buildName", "BUILD_NAME", "local");

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
