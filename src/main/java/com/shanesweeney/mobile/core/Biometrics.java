package com.shanesweeney.mobile.core;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Face ID / fingerprint on a simulator or emulator: enrol, then send a matching or non-matching scan.
 * Real devices and BrowserStack can't fake a scan this way, so tests using this run on simulators and
 * emulators only.
 *
 * <p>iOS enrols with one Appium command. Android only allows a fingerprint behind a screen lock, so
 * {@link #enrol} sets a PIN and walks the Settings enrolment flow (English UI), and {@link #remove}
 * clears the PIN again, which also deletes the fingerprint.
 */
public final class Biometrics {
    private static final Logger log = LoggerFactory.getLogger(Biometrics.class);
    private static final String PIN = "1111";
    private static final int ENROLLED_FINGER = 1;
    private static final int UNKNOWN_FINGER = 2;
    private static final Duration SETTINGS_TIMEOUT = Duration.ofSeconds(30);
    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final By PIN_FIELD = By.id("com.android.settings:id/password_entry");
    private static final By AGREE = AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)I agree\")");
    private static final By MORE = AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)More\")");
    private static final By ADDED = AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)Fingerprint added\")");
    private static final By DONE = AppiumBy.androidUIAutomator("new UiSelector().textMatches(\"(?i)Done\")");

    private Biometrics() {}

    public static void enrol(AppiumDriver driver) {
        log.info("Enrolling a {} on the device", Config.PLATFORM == Config.Platform.IOS ? "face" : "fingerprint");
        if (Config.PLATFORM == Config.Platform.IOS) {
            driver.executeScript("mobile: enrollBiometric", Map.of("isEnabled", true));
            return;
        }
        String serial = serial(driver);
        // A PIN left by an interrupted run would make set-pin fail.
        adbIgnoringFailure(serial, "shell", "locksettings", "clear", "--old", PIN);
        adb(serial, "shell", "locksettings", "set-pin", PIN);
        adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP");
        AndroidDriver android = (AndroidDriver) driver;
        // Settings asks for the PIN, shows an intro (scroll with MORE, then I AGREE), then wants touches.
        // An app still finishing its launch can come to the front over Settings, so reopen it until it stays.
        until(driver, "the PIN prompt", d -> {
            if (!d.findElements(PIN_FIELD).isEmpty()) {
                return true;
            }
            if (!SETTINGS_PACKAGE.equals(android.getCurrentPackage())) {
                adb(serial, "shell", "am", "start", "-a", "android.settings.FINGERPRINT_ENROLL");
            }
            return false;
        });
        adb(serial, "shell", "input", "text", PIN);
        adb(serial, "shell", "input", "keyevent", "KEYCODE_ENTER");
        until(driver, "the fingerprint terms", d -> {
            List<WebElement> agree = d.findElements(AGREE);
            if (!agree.isEmpty()) {
                agree.get(0).click();
                return true;
            }
            d.findElements(MORE).forEach(WebElement::click);
            return false;
        });
        until(driver, "the fingerprint to be added", d -> {
            if (!d.findElements(ADDED).isEmpty()) {
                return true;
            }
            android.fingerPrint(ENROLLED_FINGER);
            return false;
        });
        driver.findElement(DONE).click();
    }

    /** Undoes {@link #enrol}, so the device is left as it was found. */
    public static void remove(AppiumDriver driver) {
        log.info("Removing the enrolled biometrics");
        if (Config.PLATFORM == Config.Platform.IOS) {
            driver.executeScript("mobile: enrollBiometric", Map.of("isEnabled", false));
        } else {
            adbIgnoringFailure(serial(driver), "shell", "locksettings", "clear", "--old", PIN);
        }
    }

    /** Answers the system's biometric prompt with the enrolled face or finger, or with an unknown one. */
    public static void scan(AppiumDriver driver, boolean match) {
        log.info("Sending a {} biometric scan", match ? "matching" : "non-matching");
        if (Config.PLATFORM == Config.Platform.IOS) {
            // Every current iPhone simulator has Face ID.
            driver.executeScript("mobile: sendBiometricMatch", Map.of("type", "faceId", "match", match));
        } else {
            ((AndroidDriver) driver).fingerPrint(match ? ENROLLED_FINGER : UNKNOWN_FINGER);
        }
    }

    private static void until(AppiumDriver driver, String what, Function<AppiumDriver, Boolean> step) {
        new FluentWait<>(driver)
                .withTimeout(SETTINGS_TIMEOUT)
                .pollingEvery(Duration.ofSeconds(1))
                .withMessage("waiting for " + what + " in the fingerprint enrolment")
                .until(step);
    }

    private static String serial(AppiumDriver driver) {
        return String.valueOf(driver.getCapabilities().getCapability("appium:deviceUDID"));
    }

    private static void adb(String serial, String... args) {
        if (run(serial, args) != 0) {
            throw new IllegalStateException("adb " + String.join(" ", args) + " failed on " + serial);
        }
    }

    private static void adbIgnoringFailure(String serial, String... args) {
        run(serial, args);
    }

    private static int run(String serial, String... args) {
        List<String> command = new ArrayList<>(List.of(adbPath(), "-s", serial));
        command.addAll(List.of(args));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exit = process.waitFor();
            log.debug("adb {} -> {} {}", String.join(" ", args), exit, output);
            return exit;
        } catch (IOException e) {
            throw new IllegalStateException("Could not run adb; set ANDROID_HOME or put adb on the PATH", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running adb", e);
        }
    }

    private static String adbPath() {
        for (String home : new String[] {System.getenv("ANDROID_HOME"), System.getenv("ANDROID_SDK_ROOT")}) {
            if (home != null && Files.isExecutable(Path.of(home, "platform-tools", "adb"))) {
                return Path.of(home, "platform-tools", "adb").toString();
            }
        }
        return "adb";
    }
}
