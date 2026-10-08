package com.shanesweeney.mobile.core;

import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Touch gestures built on W3C Actions, so the same code drives UiAutomator2 and XCUITest.
 * Coordinates are screen pixels on Android and points on iOS; both come from the driver.
 */
public final class Gestures {
    private static final Logger log = LoggerFactory.getLogger(Gestures.class);
    private static final Duration SWIPE = Duration.ofMillis(500);
    // React Native drag handlers only pick up a drag after the finger rests for a moment.
    private static final Duration DRAG_HOLD = Duration.ofMillis(300);
    private static final Duration DRAG_MOVE = Duration.ofMillis(800);

    private final AppiumDriver driver;

    public Gestures(AppiumDriver driver) {
        this.driver = driver;
    }

    public void swipe(Point from, Point to) {
        step("Swipe from " + from + " to " + to);
        press(from, Duration.ZERO, to, SWIPE);
    }

    /**
     * Swipes right-to-left across the middle of the element, e.g. to advance a carousel. It starts
     * 70% across: a carousel ignores swipes that start in the gap after its current card.
     */
    public void swipeLeft(WebElement element) {
        Rectangle r = element.getRect();
        int y = r.y + r.height / 2;
        swipe(new Point(r.x + r.width * 7 / 10, y), new Point(r.x + r.width / 10, y));
    }

    /**
     * Scrolls the screen's content up by one swipe between the status bar and the tab bar. When
     * {@code avoid} (e.g. a horizontal carousel, which would capture the swipe) is in that band, the
     * swipe uses whichever part above or below it is larger.
     */
    public void scrollDown(Rectangle avoid) {
        Dimension screen = screenSize();
        int top = screen.height / 10;
        int bottom = screen.height * 3 / 4;
        if (avoid != null && avoid.y < bottom && avoid.y + avoid.height > top) {
            int spaceAbove = avoid.y - top;
            int spaceBelow = bottom - (avoid.y + avoid.height);
            if (spaceAbove >= spaceBelow) {
                bottom = avoid.y - 1;
            } else {
                top = avoid.y + avoid.height + 1;
            }
        }
        int x = screen.width / 2;
        swipe(new Point(x, bottom), new Point(x, top));
    }

    public void dragAndDrop(WebElement source, WebElement target) {
        Point from = center(source);
        Point to = center(target);
        step("Drag from " + from + " to " + to);
        press(from, DRAG_HOLD, to, DRAG_MOVE);
    }

    public Dimension screenSize() {
        return driver.manage().window().getSize();
    }

    private static Point center(WebElement element) {
        Rectangle r = element.getRect();
        return new Point(r.x + r.width / 2, r.y + r.height / 2);
    }

    private void press(Point from, Duration hold, Point to, Duration move) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence sequence = new Sequence(finger, 0)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), from.x, from.y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, hold))
                .addAction(finger.createPointerMove(move, PointerInput.Origin.viewport(), to.x, to.y))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(sequence));
    }

    private static void step(String action) {
        log.info(action);
        Allure.step(action);
    }
}
