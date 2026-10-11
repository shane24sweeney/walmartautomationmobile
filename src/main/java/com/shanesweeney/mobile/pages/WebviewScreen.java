package com.shanesweeney.mobile.pages;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.remote.SupportsContextSwitching;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * The Web tab: a webview showing the live https://webdriver.io site. Reading or acting on the page
 * means switching the session into the webview's context; each method switches back to the native
 * app before returning, even when it fails, so native commands (and the next test's app restart)
 * always reach the app.
 */
public class WebviewScreen extends BasePage {
    private static final String NATIVE = "NATIVE_APP";
    // A live site over the network, so it gets longer than a native screen.
    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(60);
    // Docusaurus's own class and the link target; the site's hashed class names change on every build.
    private static final By GET_STARTED = By.cssSelector("a.button--primary[href='/docs/gettingstarted']");

    public WebviewScreen(AppiumDriver driver) {
        super(driver);
    }

    public String pageTitle() {
        return inWebview(() -> waitUntil("waiting for the web page to load", PAGE_LOAD_TIMEOUT, d -> {
            boolean complete = "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState"));
            String title = ((WebDriver) d).getTitle();
            return complete && title != null && !title.isBlank() ? title : null;
        }));
    }

    public String pageHost() {
        return inWebview(() -> URI.create(((WebDriver) driver).getCurrentUrl()).getHost());
    }

    public String pagePath() {
        return inWebview(() -> URI.create(((WebDriver) driver).getCurrentUrl()).getPath());
    }

    /** Follows the page's "Get Started" link and waits for the docs page it opens. */
    public WebviewScreen openGetStarted() {
        inWebview(() -> {
            waitUntil("waiting for the Get Started link", PAGE_LOAD_TIMEOUT, d -> !d.findElements(GET_STARTED).isEmpty());
            tap(GET_STARTED);
            return waitUntil("waiting for the getting started page", PAGE_LOAD_TIMEOUT,
                    d -> ((WebDriver) d).getCurrentUrl().contains("/docs/gettingstarted"));
        });
        return this;
    }

    private <T> T inWebview(Supplier<T> action) {
        SupportsContextSwitching contexts = (SupportsContextSwitching) driver;
        // The webview only registers once the tab has rendered it.
        String webview = waitUntil("waiting for the webview to be available", PAGE_LOAD_TIMEOUT,
                d -> contexts.getContextHandles().stream().filter(c -> c.startsWith("WEBVIEW")).findFirst().orElse(null));
        contexts.context(webview);
        try {
            return action.get();
        } finally {
            contexts.context(NATIVE);
        }
    }
}
