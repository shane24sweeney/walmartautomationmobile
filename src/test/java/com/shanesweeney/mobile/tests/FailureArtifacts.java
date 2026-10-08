package com.shanesweeney.mobile.tests;

import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.logging.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Saves a screenshot, the UI hierarchy and the device log for every failed test to
 * {@code target/failure-artifacts/} and attaches them to the test in the Allure report,
 * so flaky failures can be diagnosed after the run.
 */
public class FailureArtifacts implements TestWatcher {
    private static final Logger log = LoggerFactory.getLogger(FailureArtifacts.class);
    private static final Path DIR = Path.of("target", "failure-artifacts");

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        AppiumDriver driver = context.getTestInstance()
                .filter(BaseTest.class::isInstance)
                .map(test -> ((BaseTest) test).driver)
                .orElse(null);
        if (driver == null) {
            return;
        }
        log.error("{} failed: {}", context.getDisplayName(), cause.toString());
        String name = context.getRequiredTestClass().getSimpleName() + "_" + context.getRequiredTestMethod().getName()
                + "_" + System.currentTimeMillis();
        // Each capture is independent: a crashed session may still allow some of them.
        save(name, "Screenshot", "image/png", ".png", () -> driver.getScreenshotAs(OutputType.BYTES));
        save(name, "UI hierarchy", "text/xml", ".xml", () -> driver.getPageSource().getBytes(StandardCharsets.UTF_8));
        save(name, "Device log", "text/plain", ".log", () -> deviceLog(driver).getBytes(StandardCharsets.UTF_8));
    }

    private static String deviceLog(AppiumDriver driver) {
        String type = driver.manage().logs().getAvailableLogTypes().contains("logcat") ? "logcat" : "syslog";
        return driver.manage().logs().get(type).getAll().stream()
                .map(LogEntry::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private interface Capture {
        byte[] get() throws Exception;
    }

    private static void save(String name, String title, String mimeType, String extension, Capture capture) {
        try {
            byte[] content = capture.get();
            Files.createDirectories(DIR);
            Path file = DIR.resolve(name + extension);
            Files.write(file, content);
            Allure.attachment(title, mimeType, new ByteArrayInputStream(content), AttachmentOptions.withFileExtension(extension));
            log.info("Saved {}", file);
        } catch (Exception e) {
            log.warn("Could not save {} for {}: {}", title, name, e.getMessage());
        }
    }
}
