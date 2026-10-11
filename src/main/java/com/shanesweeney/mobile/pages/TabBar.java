package com.shanesweeney.mobile.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

/** The bottom navigation shown on every screen; each method returns the screen it opens. */
public class TabBar extends BasePage {
    // A cold launch (fresh install, or several emulators sharing a CPU) can hold the splash screen
    // well past the normal wait.
    private static final Duration APP_LAUNCH_TIMEOUT = Duration.ofSeconds(60);
    private static final By WEBVIEW = AppiumBy.accessibilityId("Webview");
    private static final By LOGIN = AppiumBy.accessibilityId("Login");
    private static final By FORMS = AppiumBy.accessibilityId("Forms");
    private static final By SWIPE = AppiumBy.accessibilityId("Swipe");
    private static final By DRAG = AppiumBy.accessibilityId("Drag");

    public TabBar(AppiumDriver driver) {
        super(driver);
    }

    /** Waits for the app to finish launching, so the first tap of a test doesn't race the splash screen. */
    public TabBar waitUntilShown() {
        waitUntil("waiting for the app to launch and show the tab bar", APP_LAUNCH_TIMEOUT,
                ExpectedConditions.visibilityOfElementLocated(LOGIN));
        return this;
    }

    public WebviewScreen openWebview() {
        tap(WEBVIEW);
        return new WebviewScreen(driver);
    }

    public LoginScreen openLogin() {
        tap(LOGIN);
        return new LoginScreen(driver);
    }

    public FormsScreen openForms() {
        tap(FORMS);
        return new FormsScreen(driver);
    }

    public SwipeScreen openSwipe() {
        tap(SWIPE);
        return new SwipeScreen(driver);
    }

    public DragScreen openDrag() {
        tap(DRAG);
        return new DragScreen(driver);
    }
}
