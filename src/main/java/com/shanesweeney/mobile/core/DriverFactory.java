package com.shanesweeney.mobile.core;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import io.appium.java_client.remote.options.BaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

/**
 * Builds the session for {@link Config#PLATFORM} on {@link Config#TARGET}. Only the device-specific
 * capabilities differ by target; the app, timeouts and test code are the same everywhere.
 */
public final class DriverFactory {
    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);
    private static final Duration DEVICE_BOOT_TIMEOUT = Duration.ofMinutes(3);
    // Ends the session if the client stops sending commands, e.g. a paused debugger or a killed JVM.
    private static final Duration NEW_COMMAND_TIMEOUT = Duration.ofMinutes(5);

    private DriverFactory() {}

    public static AppiumDriver create(DevicePool.Device device) {
        log.info("Starting {} session on {} ({})", Config.PLATFORM, device.name(), Config.TARGET);
        URL server = Config.TARGET == Config.Target.BROWSERSTACK ? Config.BROWSERSTACK_URL : AppiumServer.ensureRunning();
        // No implicit wait: BasePage uses explicit/fluent waits, and mixing the two compounds timeouts.
        AppiumDriver driver = switch (Config.PLATFORM) {
            case ANDROID -> new AndroidDriver(server, androidOptions(device));
            case IOS -> new IOSDriver(server, iosOptions(device));
        };
        log.info("Session {} started on {}", driver.getSessionId(), device.name());
        return driver;
    }

    private static UiAutomator2Options androidOptions(DevicePool.Device device) {
        UiAutomator2Options options = new UiAutomator2Options()
                .setNewCommandTimeout(NEW_COMMAND_TIMEOUT)
                // Runtime permission dialogs would otherwise block the first screen at random.
                .setAutoGrantPermissions(true)
                .setApp(AppUnderTest.app())
                .setAppPackage(AppUnderTest.ANDROID_PACKAGE)
                .setAppActivity(".MainActivity");
        switch (Config.TARGET) {
            case EMULATOR -> options
                    .setDeviceName(device.name())
                    // UiAutomator2 boots this AVD if no matching emulator is running.
                    .setAvd(device.name())
                    .setAvdLaunchTimeout(DEVICE_BOOT_TIMEOUT)
                    .setAvdReadyTimeout(DEVICE_BOOT_TIMEOUT)
                    .setSystemPort(device.systemPort());
            case DEVICE -> options
                    .setUdid(device.name())
                    .setSystemPort(device.systemPort());
            case BROWSERSTACK -> addBrowserStackOptions(options, device);
        }
        return options;
    }

    private static XCUITestOptions iosOptions(DevicePool.Device device) {
        XCUITestOptions options = new XCUITestOptions()
                .setNewCommandTimeout(NEW_COMMAND_TIMEOUT)
                .setApp(AppUnderTest.app())
                .setBundleId(AppUnderTest.IOS_BUNDLE_ID);
        switch (Config.TARGET) {
            case EMULATOR -> {
                // XCUITest boots the simulator matching deviceName (+ platformVersion) if it is shut down.
                options.setDeviceName(device.name())
                        .setSimulatorStartupTimeout(DEVICE_BOOT_TIMEOUT)
                        .setWdaLocalPort(device.wdaLocalPort());
                if (Config.PLATFORM_VERSION != null) {
                    options.setPlatformVersion(Config.PLATFORM_VERSION);
                }
            }
            case DEVICE -> {
                // A real iPhone needs a signed .ipa (-Dapp) and WebDriverAgent signed with your team.
                options.setUdid(device.name())
                        .setWdaLocalPort(device.wdaLocalPort());
                if (Config.XCODE_ORG_ID != null) {
                    // java-client has no typed setters for these WebDriverAgent signing capabilities.
                    options.amend("appium:xcodeOrgId", Config.XCODE_ORG_ID)
                            .amend("appium:xcodeSigningId", "Apple Development");
                }
            }
            case BROWSERSTACK -> addBrowserStackOptions(options, device);
        }
        return options;
    }

    /** BrowserStack picks the device from bstack:options; the app must be uploaded first (-Dapp=bs://...). */
    private static void addBrowserStackOptions(BaseOptions<?> options, DevicePool.Device device) {
        if (Config.BROWSERSTACK_USERNAME == null || Config.BROWSERSTACK_ACCESS_KEY == null) {
            throw new IllegalStateException("Set BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY to run on BrowserStack");
        }
        String[] nameAndVersion = device.name().split(":", 2);
        if (nameAndVersion.length != 2) {
            throw new IllegalArgumentException("BrowserStack devices are \"Device name:OS version\", got: " + device.name());
        }
        options.setCapability("bstack:options", Map.of(
                "userName", Config.BROWSERSTACK_USERNAME,
                "accessKey", Config.BROWSERSTACK_ACCESS_KEY,
                "deviceName", nameAndVersion[0],
                "osVersion", nameAndVersion[1],
                "projectName", "mobile-automation-framework",
                "buildName", Config.BUILD_NAME,
                "deviceLogs", true));
    }
}
