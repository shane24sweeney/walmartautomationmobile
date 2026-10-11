package com.shanesweeney.mobile.pages;

import com.shanesweeney.mobile.core.Biometrics;
import com.shanesweeney.mobile.core.Config;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * The system Face ID / fingerprint prompt the app raises from its biometric login button. Scans are
 * simulated, so the device must have biometrics enrolled ({@link Biometrics#enrol}).
 */
public class BiometricPrompt extends BasePage {
    // Android's prompt is system UI the session can see; iOS draws Face ID outside the app's element tree.
    private static final By ANDROID_PROMPT = By.id("com.android.systemui:id/biometric_icon");
    // The simulated Face ID check takes a few seconds before the app reacts (about 5s seen on iOS),
    // so a non-match is only confirmed after waiting comfortably longer than that.
    private static final Duration NO_LOGIN_WAIT = Duration.ofSeconds(10);
    private static final Logger log = LoggerFactory.getLogger(BiometricPrompt.class);

    public BiometricPrompt(AppiumDriver driver) {
        super(driver);
    }

    /**
     * A scan sent before the prompt is ready is ignored. Android's prompt can be waited for, but iOS
     * shows nothing to wait for, so if the app hasn't reacted within the normal wait, scan once more.
     * Only once: a second scan the app also accepts would raise a second alert.
     */
    public AlertDialog scanMatching() {
        waitUntilShown();
        AlertDialog alert = new AlertDialog(driver);
        Biometrics.scan(driver, true);
        if (!alert.isShownWithin(Config.WAIT_TIMEOUT)) {
            log.warn("No response to the biometric scan; scanning again");
            Biometrics.scan(driver, true);
        }
        return alert;
    }

    /** Scans an unknown face or finger; the prompt stays up and the app shows no login alert. */
    public BiometricPrompt scanNonMatching() {
        waitUntilShown();
        Biometrics.scan(driver, false);
        return this;
    }

    public boolean loggedIn() {
        return new AlertDialog(driver).isShownWithin(NO_LOGIN_WAIT);
    }

    private void waitUntilShown() {
        if (Config.PLATFORM == Config.Platform.ANDROID) {
            waitForVisible(ANDROID_PROMPT);
        }
    }
}
