package com.shanesweeney.mobile.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

/** The bottom navigation shown on every screen; each method returns the screen it opens. */
public class TabBar extends BasePage {
    private static final By LOGIN = AppiumBy.accessibilityId("Login");
    private static final By FORMS = AppiumBy.accessibilityId("Forms");
    private static final By SWIPE = AppiumBy.accessibilityId("Swipe");
    private static final By DRAG = AppiumBy.accessibilityId("Drag");

    public TabBar(AppiumDriver driver) {
        super(driver);
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
