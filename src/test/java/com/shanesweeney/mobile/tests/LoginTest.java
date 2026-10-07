package com.shanesweeney.mobile.tests;

import com.shanesweeney.mobile.pages.AlertDialog;
import com.shanesweeney.mobile.pages.LoginScreen;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// One app session shared across ordered steps, matching the TypeScript suite.
@Tag("smoke")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoginTest extends BaseTest {
    private LoginScreen loginScreen;

    // JUnit runs the superclass @BeforeAll first, so the driver exists here.
    @BeforeAll
    void setUpPages() {
        loginScreen = new LoginScreen(driver);
    }

    @Test
    @Order(1)
    @DisplayName("opens the login screen")
    void opensLoginScreen() {
        assertTrue(loginScreen.open().isOpen());
    }

    // Runs once per row in data/users.csv; the page clears the fields before typing.
    // The name shows only the email, keeping passwords out of reports.
    @ParameterizedTest(name = "logs in as {0}")
    @CsvFileSource(resources = "/data/users.csv", numLinesToSkip = 1)
    @Order(2)
    @DisplayName("logs in with valid credentials")
    void logsInWithValidCredentials(String email, String password) {
        AlertDialog alert = loginScreen.login(email, password);
        assertAll(
                () -> assertEquals("Success", alert.title()),
                () -> assertEquals("You are logged in!", alert.message()));
        alert.dismiss();
    }
}
