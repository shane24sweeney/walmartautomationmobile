package com.shanesweeney.mobile.tests;

import com.shanesweeney.mobile.pages.WebviewScreen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The hybrid part of the app: native tab bar, web content, and switching between the two. */
@Tag("regression")
class WebviewTest extends BaseTest {

    @Test
    @Tag("smoke")
    @DisplayName("the Web tab shows the WebdriverIO site in a webview")
    void showsWebsiteInWebview() {
        WebviewScreen web = tabBar.openWebview();
        assertAll(
                () -> assertTrue(web.pageTitle().contains("WebdriverIO"), "page title"),
                () -> assertEquals("webdriver.io", web.pageHost()));
    }

    @Test
    @DisplayName("follows a link inside the web page")
    void followsLinkInWebview() {
        assertEquals("/docs/gettingstarted", tabBar.openWebview().openGetStarted().pagePath());
    }

    @Test
    @DisplayName("native screens still work after using the web page")
    void returnsToNativeApp() {
        tabBar.openWebview().pageTitle();
        assertEquals("Back in the app", tabBar.openForms().typeText("Back in the app").typedText());
    }
}
