package com.shanesweeney.mobile.pages;

import com.shanesweeney.mobile.core.Config;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.function.Function;

/**
 * Shared element interactions every page builds on. Each page declares its own locators;
 * every interaction waits for its element, and the driver has no implicit wait.
 */
public abstract class BasePage {
    private static final Duration POLLING = Duration.ofMillis(250);

    protected final AppiumDriver driver;
    // Ignores lookups that fail mid re-render, so custom conditions simply retry.
    private final FluentWait<AppiumDriver> wait;

    protected BasePage(AppiumDriver driver) {
        this.driver = driver;
        this.wait = new FluentWait<>(driver)
                .withTimeout(Config.WAIT_TIMEOUT)
                .pollingEvery(POLLING)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }

    /** Picks the locator for the platform under test; use only where the platforms differ. */
    protected static By byPlatform(By android, By ios) {
        return Config.PLATFORM == Config.Platform.IOS ? ios : android;
    }

    /** Runs a condition until it returns a non-null, non-false value. */
    protected <T> T waitUntil(Function<? super AppiumDriver, T> condition) {
        return wait.until(condition);
    }

    protected WebElement waitForVisible(By locator) {
        return waitUntil(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void waitForInvisible(By locator) {
        waitUntil(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected void tap(By locator) {
        // Re-finds the element each poll, so a re-render between lookup and click is retried.
        waitUntil(d -> {
            WebElement element = d.findElement(locator);
            if (!element.isDisplayed() || !element.isEnabled()) {
                return false;
            }
            element.click();
            return true;
        });
    }

    protected void type(By locator, String text) {
        WebElement input = waitForVisible(locator);
        input.clear();
        input.sendKeys(text);
    }

    /** Waits for the element to show non-blank text, since labels often render before their content. */
    protected String text(By locator) {
        return waitUntil(d -> {
            String value = d.findElement(locator).getText();
            return value == null || value.isBlank() ? null : value;
        });
    }

    /** Returns false instead of throwing when the element never becomes visible. */
    protected boolean isDisplayed(By locator) {
        try {
            waitForVisible(locator);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }
}
