package com.shanesweeney.mobile.tests;

import com.shanesweeney.mobile.pages.AlertDialog;
import com.shanesweeney.mobile.pages.LoginScreen;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("smoke")
class LoginTest extends BaseTest {
    private LoginScreen loginScreen;

    // JUnit runs the superclass @BeforeAll first, so the driver exists here.
    @BeforeAll
    void setUpPages() {
        loginScreen = new LoginScreen(driver);
    }

    @Test
    @DisplayName("opens the login screen")
    void opensLoginScreen() {
        assertTrue(loginScreen.open().isOpen());
    }

    // Runs once per row in data/users.csv, each on a freshly restarted app.
    // The name shows only the email, keeping passwords out of reports.
    @ParameterizedTest(name = "logs in as {0}")
    @CsvFileSource(resources = "/data/users.csv", numLinesToSkip = 1)
    @DisplayName("logs in with valid credentials")
    void logsInWithValidCredentials(String email, String password) {
        AlertDialog alert = loginScreen.open().login(email, password);
        assertAll(
                () -> assertEquals("Success", alert.title()),
                () -> assertEquals("You are logged in!", alert.message()));
        alert.dismiss();
    }
}
