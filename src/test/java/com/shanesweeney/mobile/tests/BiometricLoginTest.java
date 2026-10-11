package com.shanesweeney.mobile.tests;

import com.shanesweeney.mobile.core.Biometrics;
import com.shanesweeney.mobile.core.Config;
import com.shanesweeney.mobile.pages.AlertDialog;
import com.shanesweeney.mobile.pages.LoginScreen;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Face ID (iOS) and fingerprint (Android) login. The class enrols a face or finger on the device
 * before its tests and removes it afterwards, leaving the device as it was.
 */
@Tag("regression")
@EnabledIf(value = "onSimulatorOrEmulator",
        disabledReason = "Biometric scans can only be simulated on simulators and emulators")
class BiometricLoginTest extends BaseTest {

    static boolean onSimulatorOrEmulator() {
        return Config.TARGET == Config.Target.EMULATOR;
    }

    // Runs after BaseTest has started the session, and before each test restarts the app.
    @BeforeAll
    void enrolBiometrics() {
        // Let the app finish launching first, so it doesn't come to the front over the Settings enrolment.
        tabBar.waitUntilShown();
        Biometrics.enrol(driver);
    }

    // Runs before BaseTest ends the session.
    @AfterAll
    void removeBiometrics() {
        Biometrics.remove(driver);
    }

    @Test
    @Tag("smoke")
    @DisplayName("logs in with a matching face or fingerprint")
    void logsInWithMatchingBiometrics() {
        LoginScreen login = tabBar.openLogin();
        assertTrue(login.hasBiometricLogin(), "biometric login button shown once enrolled");
        AlertDialog alert = login.loginWithBiometrics().scanMatching();
        assertAll(
                () -> assertEquals("Success", alert.title()),
                () -> assertEquals(Config.PLATFORM == Config.Platform.IOS
                        ? "You are logged in through FaceID!"
                        : "You are logged in through Fingerprint!", alert.message()));
        alert.dismiss();
    }

    @Test
    @DisplayName("rejects a face or fingerprint that doesn't match")
    void rejectsNonMatchingBiometrics() {
        assertFalse(tabBar.openLogin().loginWithBiometrics().scanNonMatching().loggedIn());
    }
}
