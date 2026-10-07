package com.shanesweeney.mobile.tests;

import io.appium.java_client.AppiumDriver;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.logging.LogEntry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Saves a screenshot, the UI hierarchy and the device log for every failed test to
 * {@code target/failure-artifacts/}, so flaky failures can be diagnosed after the run.
 */
public class FailureArtifacts implements TestWatcher {
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
        String name = context.getRequiredTestClass().getSimpleName() + "_" + context.getRequiredTestMethod().getName()
                + "_" + System.currentTimeMillis();
        // Each capture is independent: a crashed session may still allow some of them.
        save(name + ".png", () -> driver.getScreenshotAs(OutputType.BYTES));
        save(name + ".xml", () -> driver.getPageSource().getBytes());
        save(name + ".log", () -> deviceLog(driver).getBytes());
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

    private static void save(String fileName, Capture capture) {
        try {
            Files.createDirectories(DIR);
            Files.write(DIR.resolve(fileName), capture.get());
        } catch (Exception e) {
            System.err.println("Could not save " + fileName + ": " + e.getMessage());
        }
    }
}
