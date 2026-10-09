package com.shanesweeney.mobile.core;

import io.appium.java_client.AppiumClientConfig;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.options.BaseOptions;
import io.appium.java_client.remote.options.SupportsAppOption;
import io.appium.java_client.remote.options.SupportsClearSystemFilesOption;
import io.appium.java_client.remote.options.SupportsEnforceAppInstallOption;
import io.appium.java_client.remote.options.SupportsIsHeadlessOption;
import io.appium.java_client.remote.options.SupportsLanguageOption;
import io.appium.java_client.remote.options.SupportsLocaleOption;
import io.appium.java_client.remote.options.SupportsOrientationOption;
import io.appium.java_client.remote.options.SupportsSkipLogCaptureOption;
import org.openqa.selenium.ScreenOrientation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

/**
 * Builds the session for {@link Config#PLATFORM} on {@link Config#TARGET}. Every capability is chained
 * here: {@link #commonOptions} for both platforms, then one chain per platform, then the few that
 * depend on the target. A null value (an unset {@link Config} entry) is left out of the request.
 */
public final class DriverFactory {
    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);
    private static final Duration DEVICE_BOOT_TIMEOUT = Duration.ofMinutes(3);
    // Ends the session if the client stops sending commands, e.g. a paused debugger or a killed JVM.
    private static final Duration NEW_COMMAND_TIMEOUT = Duration.ofMinutes(5);
    // Above the drivers' defaults (20-90s): cold CI emulators and simulators are slow to install and start.
    private static final Duration INSTALL_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration AUTOMATION_SERVER_TIMEOUT = Duration.ofMinutes(2);
    private static final Duration ADB_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration APP_LAUNCH_TIMEOUT = Duration.ofSeconds(30);

    private DriverFactory() {}

    public static AppiumDriver create(DevicePool.Device device) {
        log.info("Starting {} session on {} ({})", Config.PLATFORM, device.name(), Config.TARGET);
        URL server = Config.TARGET == Config.Target.BROWSERSTACK
                ? Config.BROWSERSTACK_URL
                : AppiumServer.ensureRunning(device.appiumUrl());
        AppiumClientConfig client = AppiumClientConfig.defaultConfig()
                .baseUrl(server)
                .withFilter(new HttpLogFilter(server.getHost() + ":" + server.getPort()));
        // No implicit wait: BasePage uses explicit/fluent waits, and mixing the two compounds timeouts.
        AppiumDriver driver = switch (Config.PLATFORM) {
            case ANDROID -> new AndroidDriver(client, androidOptions(device));
            case IOS -> new IOSDriver(client, iosOptions(device));
        };
        log.info("Session {} started on {} via {}", driver.getSessionId(), device.name(), server);
        return driver;
    }

    /** Capabilities with the same meaning on UiAutomator2 and XCUITest. */
    private static <T extends BaseOptions<T> & SupportsAppOption<T> & SupportsLanguageOption<T>
            & SupportsLocaleOption<T> & SupportsOrientationOption<T> & SupportsIsHeadlessOption<T>
            & SupportsEnforceAppInstallOption<T> & SupportsClearSystemFilesOption<T>
            & SupportsSkipLogCaptureOption<T>> T commonOptions(T options) {
        return options
                // Device
                .setPlatformVersion(Config.PLATFORM_VERSION)
                .setLanguage(Config.LANGUAGE)
                .setLocale(Config.LOCALE)
                .setOrientation(ScreenOrientation.PORTRAIT)
                // Only applies when Appium boots the emulator or simulator itself.
                .setIsHeadless(Config.HEADLESS)
                // App
                .setApp(AppUnderTest.app())
                .setNoReset(Config.NO_RESET)
                .setFullReset(Config.FULL_RESET)
                // Skip reinstalling when the same or a newer build is already on the device.
                .setEnforceAppInstall(false)
                // Session
                .setNewCommandTimeout(NEW_COMMAND_TIMEOUT)
                // FailureArtifacts reads logcat/syslog, so log capture must stay on.
                .setSkipLogCapture(false)
                // FailureArtifacts already saves the page source of a failed test.
                .setPrintPageSourceOnFindFailure(false)
                .setEventTimings(false)
                // Deletes the temporary files Appium created for the session when it ends.
                .setClearSystemFiles(true);
    }

    private static UiAutomator2Options androidOptions(DevicePool.Device device) {
        UiAutomator2Options options = commonOptions(new UiAutomator2Options())
                .setPlatformName("Android")
                .setAutomationName(AutomationName.ANDROID_UIAUTOMATOR2)
                // App
                .setAppPackage(AppUnderTest.ANDROID_PACKAGE)
                .setAppActivity(".MainActivity")
                .setAppWaitDuration(APP_LAUNCH_TIMEOUT)
                .setAndroidInstallTimeout(INSTALL_TIMEOUT)
                // Runtime permission dialogs would otherwise block the first screen at random.
                .setAutoGrantPermissions(true)
                // Device
                .setDisableWindowAnimation(true)
                .setIgnoreHiddenApiPolicyError(true)
                // So a failed test's logcat only covers its own session.
                .setClearDeviceLogsOnStart(true)
                // Driver
                .setAdbExecTimeout(ADB_TIMEOUT)
                .setUiautomator2ServerInstallTimeout(AUTOMATION_SERVER_TIMEOUT)
                .setUiautomator2ServerLaunchTimeout(AUTOMATION_SERVER_TIMEOUT);
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
        XCUITestOptions options = commonOptions(new XCUITestOptions())
                .setPlatformName("iOS")
                .setAutomationName(AutomationName.IOS_XCUI_TEST)
                // App
                .setBundleId(AppUnderTest.IOS_BUNDLE_ID)
                .setForceAppLaunch(true)
                .setShouldTerminateApp(true)
                // Tests assert on the app's own alerts, so Appium must leave them alone.
                .setAutoAcceptAlerts(false)
                .setAutoDismissAlerts(false)
                // Device: use the on-screen keyboard like a real user, not the Mac's.
                .setConnectHardwareKeyboard(false)
                .setReduceMotion(true)
                .setMaxTypingFrequency(60)
                .setWaitForQuiescence(true)
                // WebDriverAgent
                .setWdaLaunchTimeout(AUTOMATION_SERVER_TIMEOUT)
                .setWdaConnectionTimeout(AUTOMATION_SERVER_TIMEOUT)
                .setWdaStartupRetries(2);
        switch (Config.TARGET) {
            case EMULATOR -> options
                    // XCUITest boots the simulator matching deviceName (+ platformVersion) if it is shut down.
                    .setDeviceName(device.name())
                    .setSimulatorStartupTimeout(DEVICE_BOOT_TIMEOUT)
                    .setWdaLocalPort(device.wdaLocalPort());
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
                "projectName", "walmartautomationmobile",
                "buildName", Config.BUILD_NAME,
                "deviceLogs", true));
    }
}
