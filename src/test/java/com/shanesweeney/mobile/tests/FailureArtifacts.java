package com.shanesweeney.mobile.tests;

import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.logging.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Saves a screenshot, the UI hierarchy, the device log and the Appium server and HTTP logs for every
 * failed test to {@code target/failure-artifacts/} and attaches them to the test in the Allure report
 * (and ReportPortal with {@code -Preportportal}), so flaky failures can be diagnosed after the run.
 */
public class FailureArtifacts implements BeforeEachCallback, AfterTestExecutionCallback, TestWatcher {
    private static final Logger log = LoggerFactory.getLogger(FailureArtifacts.class);
    private static final Logger reportPortal = LoggerFactory.getLogger("reportportal.attachments");
    private static final Path DIR = Path.of("target", "failure-artifacts");
    private static final Path HTTP_LOG = Path.of("target", "appium-http.log");
    private static final ExtensionContext.Namespace STORE = ExtensionContext.Namespace.create(FailureArtifacts.class);
    private static final String CAPTURED = "captured";

    /** Where each log ends before the test, so a failure attaches only the lines the test produced. */
    private record LogOffsets(long server, long http) {}

    @Override
    public void beforeEach(ExtensionContext context) {
        driver(context).ifPresent(driver -> context.getStore(STORE)
                .put(LogOffsets.class, new LogOffsets(size(serverLog(driver)), size(HTTP_LOG))));
    }

    /**
     * Captures while the test is still open, so reporters that close it right after (ReportPortal)
     * file the artifacts under the test rather than its class.
     */
    @Override
    public void afterTestExecution(ExtensionContext context) {
        context.getExecutionException().ifPresent(cause -> {
            capture(context, cause);
            context.getStore(STORE).put(CAPTURED, true);
        });
    }

    /** Covers failures before the test body ran, e.g. the app restart in @BeforeEach. */
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (!context.getStore(STORE).getOrDefault(CAPTURED, Boolean.class, false)) {
            capture(context, cause);
        }
    }

    private static void capture(ExtensionContext context, Throwable cause) {
        AppiumDriver driver = driver(context).orElse(null);
        if (driver == null) {
            return;
        }
        log.error("{} failed: {}", context.getDisplayName(), cause.toString());
        String name = context.getRequiredTestClass().getSimpleName() + "_" + context.getRequiredTestMethod().getName()
                + "_" + System.currentTimeMillis();
        LogOffsets offsets = context.getStore(STORE).getOrDefault(LogOffsets.class, LogOffsets.class, new LogOffsets(0, 0));
        String session = String.valueOf(driver.getSessionId());
        // Each capture is independent: a crashed session may still allow some of them.
        save(name, "Screenshot", "image/png", ".png", () -> driver.getScreenshotAs(OutputType.BYTES));
        save(name, "UI hierarchy", "text/xml", ".xml", () -> driver.getPageSource().getBytes(StandardCharsets.UTF_8));
        save(name, "Device log", "text/plain", ".log", () -> deviceLog(driver).getBytes(StandardCharsets.UTF_8));
        save(name + "_appium", "Appium server log", "text/plain", ".log",
                () -> sessionLines(serverLog(driver), offsets.server(), session));
        save(name + "_http", "Appium HTTP log", "text/plain", ".log",
                () -> sessionLines(HTTP_LOG, offsets.http(), session));
    }

    private static Optional<AppiumDriver> driver(ExtensionContext context) {
        return context.getTestInstance()
                .filter(BaseTest.class::isInstance)
                .map(test -> ((BaseTest) test).driver);
    }

    private static String deviceLog(AppiumDriver driver) {
        String type = driver.manage().logs().getAvailableLogTypes().contains("logcat") ? "logcat" : "syslog";
        return driver.manage().logs().get(type).getAll().stream()
                .map(LogEntry::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /** The appium-&lt;port&gt;.log AppiumServer writes for a local server; remote servers have none here. */
    private static Path serverLog(AppiumDriver driver) {
        URL server = driver.getRemoteAddress();
        return Path.of("appium-" + server.getPort() + ".log");
    }

    private static long size(Path file) {
        try {
            return Files.exists(file) ? Files.size(file) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Lines written since {@code offset} that belong to this session. Parallel sessions share the HTTP log
     * and may share a server; Appium tags its lines with the first 8 characters of the session ID.
     */
    private static byte[] sessionLines(Path file, long offset, String session) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException(file + " not found (remote server or BrowserStack)");
        }
        String tag = "[" + session.substring(0, 8) + "]";
        try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r")) {
            byte[] tail = new byte[(int) Math.max(0, in.length() - offset)];
            in.seek(Math.min(offset, in.length()));
            in.readFully(tail);
            return new String(tail, StandardCharsets.UTF_8).lines()
                    .filter(line -> line.contains(session) || line.contains(tag))
                    .collect(Collectors.joining(System.lineSeparator()))
                    .getBytes(StandardCharsets.UTF_8);
        }
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
            // The ReportPortal logback appender uploads the file; this logger is off without -Preportportal.
            reportPortal.info("RP_MESSAGE#FILE#{}#{}", file.toAbsolutePath(), title);
            log.info("Saved {}", file);
        } catch (Exception e) {
            log.warn("Could not save {} for {}: {}", title, name, e.getMessage());
        }
    }
}
