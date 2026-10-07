package com.shanesweeney.mobile.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

/**
 * Actions a user can take on the login screen. Actions return the page the app lands on,
 * so tests read as a chain of user steps.
 */
public class LoginScreen extends BasePage {
    private static final By TAB = AppiumBy.accessibilityId("Login");
    private static final By SCREEN = AppiumBy.accessibilityId("Login-screen");
    private static final By EMAIL_INPUT = AppiumBy.accessibilityId("input-email");
    private static final By PASSWORD_INPUT = AppiumBy.accessibilityId("input-password");
    private static final By LOGIN_BUTTON = AppiumBy.accessibilityId("button-LOGIN");

    public LoginScreen(AppiumDriver driver) {
        super(driver);
    }

    public LoginScreen open() {
        tap(TAB);
        return this;
    }

    public boolean isOpen() {
        return isDisplayed(SCREEN);
    }

    public AlertDialog login(String email, String password) {
        type(EMAIL_INPUT, email);
        type(PASSWORD_INPUT, password);
        tap(LOGIN_BUTTON);
        return new AlertDialog(driver);
    }
}
