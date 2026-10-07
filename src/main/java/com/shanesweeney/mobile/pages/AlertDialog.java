package com.shanesweeney.mobile.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

/** The native OS alert, reusable by any screen that raises one. Each platform exposes it differently. */
public class AlertDialog extends BasePage {
    private static final By TITLE = byPlatform(By.id("com.wdiodemoapp:id/alert_title"), AppiumBy.accessibilityId("Success"));
    private static final By MESSAGE = byPlatform(By.id("android:id/message"), AppiumBy.accessibilityId("You are logged in!"));
    private static final By OK = byPlatform(By.id("android:id/button1"), AppiumBy.accessibilityId("OK"));

    public AlertDialog(AppiumDriver driver) {
        super(driver);
    }

    public String title() {
        return text(TITLE);
    }

    public String message() {
        return text(MESSAGE);
    }

    public void dismiss() {
        tap(OK);
        waitForInvisible(OK);
    }
}
