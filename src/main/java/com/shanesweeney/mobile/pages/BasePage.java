package com.shanesweeney.mobile.pages;

import com.shanesweeney.mobile.core.Config;
import com.shanesweeney.mobile.core.Gestures;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.HasOnScreenKeyboard;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Shared element interactions every page builds on. Each page declares its own locators;
 * every interaction waits for its element, and the driver has no implicit wait.
 */
public abstract class BasePage {
    private static final Duration POLLING = Duration.ofMillis(250);
    private static final int MAX_SCROLLS = 8;
    // iOS offers "Use Strong Password?" in place of the keyboard on sign-up forms and swallows the keys.
    private static final By IOS_STRONG_PASSWORD_CLOSE =
            AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND name == 'xmark'");

    protected final AppiumDriver driver;
    protected final Gestures gestures;

    protected BasePage(AppiumDriver driver) {
        this.driver = driver;
        this.gestures = new Gestures(driver);
    }

    /** Picks the locator for the platform under test; use only where the platforms differ. */
    protected static By byPlatform(By android, By ios) {
        return Config.PLATFORM == Config.Platform.IOS ? ios : android;
    }

    /** Text with no test ID, such as validation messages. iOS exposes visible text as the accessibility ID. */
    protected static By byText(String text) {
        return byPlatform(
                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + text.replace("\"", "\\\"") + "\")"),
                AppiumBy.accessibilityId(text));
    }

    /**
     * Runs a condition until it returns a non-null, non-false value. The description ("waiting for ...")
     * is the timeout message, so a failure says what never happened instead of naming a lambda.
     */
    protected <T> T waitUntil(String description, Function<? super AppiumDriver, T> condition) {
        return new FluentWait<>(driver)
                .withTimeout(Config.WAIT_TIMEOUT)
                .pollingEvery(POLLING)
                // Lookups that fail mid re-render are retried instead of failing the wait.
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .withMessage(description)
                .until(condition);
    }

    protected WebElement waitForVisible(By locator) {
        return waitUntil("waiting for " + locator + " to be visible", ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected void waitForInvisible(By locator) {
        waitUntil("waiting for " + locator + " to disappear", ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected void tap(By locator) {
        // Re-finds the element each poll, so a re-render between lookup and click is retried.
        waitUntil("waiting to tap " + locator, d -> {
            WebElement element = d.findElement(locator);
            if (!element.isDisplayed() || !element.isEnabled()) {
                return false;
            }
            element.click();
            return true;
        });
    }

    /**
     * Types, then reads the value back and retries the poll if any characters were lost. iOS types real
     * keystrokes, which are dropped while the keyboard slides in, so it waits for the keyboard first;
     * UiAutomator2 sets the text directly and needs no keyboard.
     */
    protected void type(By locator, String text) {
        waitUntil("waiting to type into " + locator, d -> {
            WebElement input = d.findElement(locator);
            if (Config.PLATFORM == Config.Platform.IOS && !((HasOnScreenKeyboard) d).isKeyboardShown()) {
                d.findElements(IOS_STRONG_PASSWORD_CLOSE).forEach(WebElement::click);
                input.click();
                return false;
            }
            input.clear();
            input.sendKeys(text);
            return hasValue(input.getText(), text);
        });
    }

    /** Password fields read back as one mask character per typed character on both platforms. */
    private static boolean hasValue(String actual, String expected) {
        return expected.equals(actual)
                || (actual != null && actual.length() == expected.length() && actual.chars().allMatch(c -> c == '•'));
    }

    /** Waits for the element to show non-blank text, since labels often render before their content. */
    protected String text(By locator) {
        return waitUntil("waiting for text in " + locator, d -> {
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

    /**
     * Scrolls down until the element is on screen; Android only adds off-screen elements to the tree
     * once scrolled to. Swipes stay clear of {@code avoid}, e.g. a carousel that would capture them.
     */
    protected WebElement scrollTo(By locator, By avoid) {
        for (int i = 0; i < MAX_SCROLLS; i++) {
            List<WebElement> found = driver.findElements(locator);
            if (!found.isEmpty() && found.get(0).isDisplayed()) {
                return found.get(0);
            }
            gestures.scrollDown(driver.findElements(avoid).stream().findFirst().map(WebElement::getRect).orElse(null));
        }
        return waitForVisible(locator);
    }

    /**
     * Closes the on-screen keyboard, which covers the tab bar and lower controls, by tapping a
     * non-interactive element as a user would. driver.hideKeyboard() does not close it on iOS,
     * pressing Return would submit the form, and fixed screen coordinates differ per screen.
     */
    protected void hideKeyboard(By nonInteractiveElement) {
        // A tap while the keyboard is still sliding in is ignored, so tap again until it is gone.
        waitUntil("waiting for the keyboard to close", d -> {
            if (!((HasOnScreenKeyboard) d).isKeyboardShown()) {
                return true;
            }
            d.findElement(nonInteractiveElement).click();
            return false;
        });
    }
}
