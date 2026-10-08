package com.shanesweeney.mobile.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A horizontal card carousel, with a logo hidden below it that only shows after scrolling. */
public class SwipeScreen extends BasePage {
    private static final By CAROUSEL = byPlatform(
            AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"Carousel\")"),
            AppiumBy.accessibilityId("Carousel"));
    private static final By CARDS = byPlatform(
            AppiumBy.androidUIAutomator("new UiSelector().resourceIdMatches(\"__CAROUSEL_ITEM_[0-9]+__\")"),
            AppiumBy.iOSNsPredicateString("name BEGINSWITH '__CAROUSEL_ITEM_'"));
    // Direct text children of a card, in order: icon glyph, title, description.
    private static final By CARD_TEXTS = byPlatform(
            By.className("android.widget.TextView"),
            AppiumBy.iOSClassChain("XCUIElementTypeStaticText"));
    private static final By HIDDEN_LOGO = AppiumBy.accessibilityId("WebdriverIO logo");

    public SwipeScreen(AppiumDriver driver) {
        super(driver);
    }

    public SwipeScreen swipeCarouselLeft() {
        String before = activeCardTitle();
        gestures.swipeLeft(waitForVisible(CAROUSEL));
        // Wait out the snap animation, so callers read the card the carousel settles on.
        waitUntil("waiting for the carousel to settle on the next card", d -> !before.equals(activeCardTitle()));
        return this;
    }

    /**
     * The active card is the left-most one on screen. Cards swiped past report a negative x on iOS
     * and a zero-width rectangle on Android, so both are skipped.
     */
    public String activeCardTitle() {
        return waitUntil("waiting for an active carousel card", d -> {
            Map<WebElement, Rectangle> cards = d.findElements(CARDS).stream()
                    .collect(Collectors.toMap(Function.identity(), WebElement::getRect));
            return cards.entrySet().stream()
                    .filter(card -> card.getValue().x >= 0 && card.getValue().width > 0)
                    .min(Comparator.comparingInt((Map.Entry<WebElement, Rectangle> card) -> card.getValue().x)
                            .thenComparing(card -> -card.getValue().width))
                    .map(card -> card.getKey().findElements(CARD_TEXTS).get(1).getText())
                    .orElse(null);
        });
    }

    public SwipeScreen scrollToHiddenLogo() {
        scrollTo(HIDDEN_LOGO, CAROUSEL);
        return this;
    }

    public boolean isHiddenLogoVisible() {
        return isDisplayed(HIDDEN_LOGO);
    }
}
