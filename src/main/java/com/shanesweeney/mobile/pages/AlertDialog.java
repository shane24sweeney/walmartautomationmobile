package com.shanesweeney.mobile.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/** The native OS alert, reusable by any screen that raises one. Each platform exposes it differently. */
public class AlertDialog extends BasePage {
    private static final By TITLE = byPlatform(
            By.id("com.wdiodemoapp:id/alert_title"),
            AppiumBy.iOSClassChain("**/XCUIElementTypeAlert/**/XCUIElementTypeStaticText[1]"));
    private static final By MESSAGE = byPlatform(
            By.id("android:id/message"),
            AppiumBy.iOSClassChain("**/XCUIElementTypeAlert/**/XCUIElementTypeStaticText[2]"));
    private static final By OK = byPlatform(
            By.id("android:id/button1"),
            AppiumBy.iOSClassChain("**/XCUIElementTypeAlert/**/XCUIElementTypeButton[`label == \"OK\"`]"));

    public AlertDialog(AppiumDriver driver) {
        super(driver);
    }

    public String title() {
        return text(TITLE);
    }

    public String message() {
        return text(MESSAGE);
    }

    /** For asserting that no alert appears: waits only {@code timeout} instead of the full wait. */
    public boolean isShownWithin(Duration timeout) {
        return isDisplayed(TITLE, timeout);
    }

    /**
     * A tap while the alert is still animating in is ignored (on iOS after Face ID, the system overlay
     * is still closing), so tap again until the alert is gone.
     */
    public void dismiss() {
        tap(OK);
        waitUntil("waiting for the alert to close", d -> {
            List<WebElement> ok = d.findElements(OK);
            if (ok.isEmpty() || !ok.get(0).isDisplayed()) {
                return true;
            }
            ok.get(0).click();
            return false;
        });
    }
}
