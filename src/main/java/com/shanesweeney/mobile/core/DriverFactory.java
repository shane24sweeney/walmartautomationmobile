package com.shanesweeney.mobile.core;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.nio.file.Path;
import java.time.Duration;

public final class DriverFactory {
    // Native app builds fetched by scripts/download-apps.sh. Appium installs them on session start.
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path ANDROID_APP = ROOT.resolve("apps/android.apk");
    private static final Path IOS_APP = ROOT.resolve("apps/ios.app");

    private static final Duration DEVICE_BOOT_TIMEOUT = Duration.ofMinutes(3);
    // Ends the session if the client stops sending commands, e.g. a paused debugger or a killed JVM.
    private static final Duration NEW_COMMAND_TIMEOUT = Duration.ofMinutes(5);

    private DriverFactory() {}

    public static AppiumDriver create(DevicePool.Device device) {
        // No implicit wait: BasePage uses explicit/fluent waits, and mixing the two compounds timeouts.
        return switch (Config.PLATFORM) {
            case ANDROID -> new AndroidDriver(AppiumServer.ensureRunning(), androidOptions(device));
            case IOS -> new IOSDriver(AppiumServer.ensureRunning(), iosOptions(device));
        };
    }

    private static UiAutomator2Options androidOptions(DevicePool.Device device) {
        return new UiAutomator2Options()
                .setDeviceName(device.name())
                // UiAutomator2 boots this AVD if no matching emulator is running.
                .setAvd(device.name())
                .setAvdLaunchTimeout(DEVICE_BOOT_TIMEOUT)
                .setAvdReadyTimeout(DEVICE_BOOT_TIMEOUT)
                .setSystemPort(device.systemPort())
                .setNewCommandTimeout(NEW_COMMAND_TIMEOUT)
                // Runtime permission dialogs would otherwise block the first screen at random.
                .setAutoGrantPermissions(true)
                .setApp(ANDROID_APP.toString())
                .setAppPackage("com.wdiodemoapp")
                .setAppActivity(".MainActivity");
    }

    private static XCUITestOptions iosOptions(DevicePool.Device device) {
        // XCUITest boots the simulator matching deviceName (+ platformVersion) if it is shut down.
        XCUITestOptions options = new XCUITestOptions()
                .setDeviceName(device.name())
                .setSimulatorStartupTimeout(DEVICE_BOOT_TIMEOUT)
                .setWdaLocalPort(device.wdaLocalPort())
                .setNewCommandTimeout(NEW_COMMAND_TIMEOUT)
                .setApp(IOS_APP.toString())
                .setBundleId("org.wdiodemoapp");
        if (Config.PLATFORM_VERSION != null) {
            options.setPlatformVersion(Config.PLATFORM_VERSION);
        }
        return options;
    }
}
