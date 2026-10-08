package com.shanesweeney.mobile.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

/**
 * Actions a user can take on the login / sign-up screen. Actions return the page the app lands on,
 * so tests read as a chain of user steps.
 */
public class LoginScreen extends BasePage {
    private static final By SCREEN = AppiumBy.accessibilityId("Login-screen");
    private static final By SIGN_UP_TAB = AppiumBy.accessibilityId("button-sign-up-container");
    private static final By EMAIL_INPUT = AppiumBy.accessibilityId("input-email");
    private static final By PASSWORD_INPUT = AppiumBy.accessibilityId("input-password");
    private static final By CONFIRM_PASSWORD_INPUT = AppiumBy.accessibilityId("input-repeat-password");
    private static final By LOGIN_BUTTON = AppiumBy.accessibilityId("button-LOGIN");
    private static final By SIGN_UP_BUTTON = AppiumBy.accessibilityId("button-SIGN UP");

    public LoginScreen(AppiumDriver driver) {
        super(driver);
    }

    public boolean isOpen() {
        return isDisplayed(SCREEN);
    }

    public AlertDialog login(String email, String password) {
        submitLogin(email, password);
        return new AlertDialog(driver);
    }

    /** Submits credentials the form rejects; the app stays on this screen and shows validation errors. */
    public LoginScreen loginWithInvalidCredentials(String email, String password) {
        submitLogin(email, password);
        return this;
    }

    public boolean showsError(String message) {
        return isDisplayed(byText(message));
    }

    public AlertDialog signUp(String email, String password, String confirmPassword) {
        tap(SIGN_UP_TAB);
        waitForVisible(CONFIRM_PASSWORD_INPUT);
        type(EMAIL_INPUT, email);
        type(PASSWORD_INPUT, password);
        type(CONFIRM_PASSWORD_INPUT, confirmPassword);
        scrollTo(SIGN_UP_BUTTON);
        tap(SIGN_UP_BUTTON);
        return new AlertDialog(driver);
    }

    private void submitLogin(String email, String password) {
        type(EMAIL_INPUT, email);
        type(PASSWORD_INPUT, password);
        scrollTo(LOGIN_BUTTON);
        tap(LOGIN_BUTTON);
    }
}
