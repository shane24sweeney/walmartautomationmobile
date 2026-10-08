package com.shanesweeney.mobile.tests;

import com.shanesweeney.mobile.pages.AlertDialog;
import com.shanesweeney.mobile.pages.LoginScreen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("regression")
class LoginTest extends BaseTest {

    @Test
    @Tag("smoke")
    @DisplayName("opens the login screen")
    void opensLoginScreen() {
        assertTrue(tabBar.openLogin().isOpen());
    }

    // Runs once per row in data/users.csv, each on a freshly restarted app.
    // The name shows only the email, keeping passwords out of reports.
    @ParameterizedTest(name = "logs in as {0}")
    @CsvFileSource(resources = "/data/users.csv", numLinesToSkip = 1)
    @Tag("smoke")
    @DisplayName("logs in with valid credentials")
    void logsInWithValidCredentials(String email, String password) {
        AlertDialog alert = tabBar.openLogin().login(email, password);
        assertAll(
                () -> assertEquals("Success", alert.title()),
                () -> assertEquals("You are logged in!", alert.message()));
        alert.dismiss();
    }

    @ParameterizedTest(name = "rejects {0} with \"{2}\"")
    @CsvFileSource(resources = "/data/invalid-logins.csv", numLinesToSkip = 1)
    @DisplayName("rejects invalid credentials")
    void rejectsInvalidCredentials(String email, String password, String expectedError) {
        LoginScreen login = tabBar.openLogin().loginWithInvalidCredentials(email, password);
        assertTrue(login.showsError(expectedError), "Expected validation error: " + expectedError);
    }

    @Test
    @DisplayName("signs up a new account")
    void signsUp() {
        AlertDialog alert = tabBar.openLogin().signUp("new.user@example.com", "Password1", "Password1");
        assertAll(
                () -> assertEquals("Signed Up!", alert.title()),
                () -> assertEquals("You successfully signed up!", alert.message()));
        alert.dismiss();
    }
}
