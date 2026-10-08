package com.shanesweeney.mobile.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("regression")
class GesturesTest extends BaseTest {

    @Test
    @Tag("smoke")
    @DisplayName("swiping the carousel shows the next card")
    void swipesCarousel() {
        assertEquals("GREAT COMMUNITY", tabBar.openSwipe().swipeCarouselLeft().activeCardTitle());
    }

    @Test
    @DisplayName("scrolling down reveals the hidden logo")
    void scrollsToHiddenLogo() {
        assertTrue(tabBar.openSwipe().scrollToHiddenLogo().isHiddenLogoVisible());
    }

    @Test
    @DisplayName("dragging every piece into place completes the puzzle")
    void completesDragAndDropPuzzle() {
        assertTrue(tabBar.openDrag().completePuzzle().isCompleted());
    }
}
