package com.shanesweeney.mobile.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import java.util.List;

/** A 3x3 puzzle: each piece "drag-xx" must be dropped on its slot "drop-xx". */
public class DragScreen extends BasePage {
    private static final List<String> PIECES = List.of("l1", "l2", "l3", "c1", "c2", "c3", "r1", "r2", "r3");
    private static final By CONGRATULATIONS = byText("Congratulations");
    private static final By RETRY = AppiumBy.accessibilityId("button-Retry");

    public DragScreen(AppiumDriver driver) {
        super(driver);
    }

    public DragScreen dragPiece(String piece) {
        By pieceLocator = AppiumBy.accessibilityId("drag-" + piece);
        gestures.dragAndDrop(waitForVisible(pieceLocator), waitForVisible(AppiumBy.accessibilityId("drop-" + piece)));
        // A placed piece leaves the tray and the rest re-layout; wait before locating the next one.
        waitForInvisible(pieceLocator);
        return this;
    }

    public DragScreen completePuzzle() {
        PIECES.forEach(this::dragPiece);
        return this;
    }

    public boolean isCompleted() {
        return isDisplayed(CONGRATULATIONS);
    }

    public DragScreen retry() {
        tap(RETRY);
        waitForInvisible(CONGRATULATIONS);
        return this;
    }
}
