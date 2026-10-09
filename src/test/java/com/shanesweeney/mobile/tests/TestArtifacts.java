package com.shanesweeney.mobile.tests;

import io.appium.java_client.AppiumDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.opentest4j.TestAbortedException;
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
import java.util.Base64;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Attaches every test's logs to the Allure report, and to ReportPortal with {@code -Preportportal}: its
 * own log lines, the device log, and its Appium server and HTTP log lines. A failed test also gets a
 * screenshot and the UI hierarchy, and its artifacts are saved to {@code target/failure-artifacts/},
 * so failures can be diagnosed after the run.
 */
public class TestArtifacts implements BeforeEachCallback, AfterTestExecutionCallback, TestWatcher {
    private static final Logger log = LoggerFactory.getLogger(TestArtifacts.class);
    private static final Logger reportPortal = LoggerFactory.getLogger("reportportal.attachments");
    private static final Path DIR = Path.of("target", "failure-artifacts");
    private static final Path TEST_LOG = Path.of("target", "test.log");
    private static final Path HTTP_LOG = Path.of("target", "appium-http.log");
    private static final ExtensionContext.Namespace STORE = ExtensionContext.Namespace.create(TestArtifacts.class);
    private static final String CAPTURED = "captured";

    /** Where each log ends before the test, so only the lines the test produced are attached. */
    private record LogOffsets(long test, long server, long http) {}

    @Override
    public void beforeEach(ExtensionContext context) {
        driver(context).ifPresent(driver -> context.getStore(STORE)
                .put(LogOffsets.class, new LogOffsets(size(TEST_LOG), size(serverLog(driver)), size(HTTP_LOG))));
    }

    /**
     * Captures while the test is still open, so reporters that close it right after (ReportPortal)
     * file the artifacts under the test rather than its class.
     */
    @Override
    public void afterTestExecution(ExtensionContext context) {
        // A skipped test (assumption not met) gets its logs like a passed one, not failure artifacts.
        capture(context, context.getExecutionException()
                .filter(cause -> !(cause instanceof TestAbortedException))
                .orElse(null));
        context.getStore(STORE).put(CAPTURED, true);
    }

    /** Covers failures before the test body ran, e.g. the app restart in @BeforeEach. */
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (!context.getStore(STORE).getOrDefault(CAPTURED, Boolean.class, false)) {
            capture(context, cause);
        }
    }

    /** @param failure null when the test passed */
    private static void capture(ExtensionContext context, Throwable failure) {
        AppiumDriver driver = driver(context).orElse(null);
        if (driver == null) {
            return;
        }
        boolean failed = failure != null;
        if (failed) {
            log.error("{} failed: {}", context.getDisplayName(), failure.toString());
        }
        String name = context.getRequiredTestClass().getSimpleName() + "_" + context.getRequiredTestMethod().getName()
                + "_" + System.currentTimeMillis();
        LogOffsets offsets = context.getStore(STORE).getOrDefault(LogOffsets.class, LogOffsets.class, new LogOffsets(0, 0, 0));
        String session = String.valueOf(driver.getSessionId());
        String sessionTag = "[" + session.substring(0, 8) + "]";
        String threadTag = "[" + Thread.currentThread().getName() + "]";
        Path serverLog = serverLog(driver);
        // Each capture is independent: a crashed session may still allow some of them.
        if (failed) {
            attach(name, failed, "Screenshot", "image/png", ".png", () -> driver.getScreenshotAs(OutputType.BYTES));
            attach(name, failed, "UI hierarchy", "text/xml", ".xml", () -> driver.getPageSource().getBytes(StandardCharsets.UTF_8));
        }
        // Parallel classes share test.log; each runs on its own thread, which every line names.
        attach(name + "_test", failed, "Test log", "text/plain", ".log",
                () -> linesSince(TEST_LOG, offsets.test(), line -> line.contains(threadTag)));
        attach(name, failed, "Device log", "text/plain", ".log", () -> deviceLog(driver).getBytes(StandardCharsets.UTF_8));
        // A remote server or BrowserStack writes no local server log.
        if (Files.exists(serverLog)) {
            attach(name + "_appium", failed, "Appium server log", "text/plain", ".log",
                    () -> linesSince(serverLog, offsets.server(), line -> line.contains(session) || line.contains(sessionTag)));
        }
        attach(name + "_http", failed, "Appium HTTP log", "text/plain", ".log",
                () -> linesSince(HTTP_LOG, offsets.http(), line -> line.contains(session)));
    }

    private static Optional<AppiumDriver> driver(ExtensionContext context) {
        return context.getTestInstance()
                .filter(BaseTest.class::isInstance)
                .map(test -> ((BaseTest) test).driver);
    }

    /** Device log entries since the previous call, which is the previous test's capture. */
    private static String deviceLog(AppiumDriver driver) {
        String type = driver.manage().logs().getAvailableLogTypes().contains("logcat") ? "logcat" : "syslog";
        return driver.manage().logs().get(type).getAll().stream()
                .map(LogEntry::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /** The appium-&lt;port&gt;.log AppiumServer writes for a local server. */
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
     * Lines written since {@code offset} that belong to this test. Parallel sessions share the logs;
     * Appium tags its lines with the first 8 characters of the session ID.
     */
    private static byte[] linesSince(Path file, long offset, Predicate<String> belongs) throws IOException {
        try (RandomAccessFile in = new RandomAccessFile(file.toFile(), "r")) {
            byte[] tail = new byte[(int) Math.max(0, in.length() - offset)];
            in.seek(Math.min(offset, in.length()));
            in.readFully(tail);
            return new String(tail, StandardCharsets.UTF_8).lines()
                    .filter(belongs)
                    .collect(Collectors.joining(System.lineSeparator()))
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    private interface Capture {
        byte[] get() throws Exception;
    }

    /** Attaches to Allure and ReportPortal; a failed test's artifacts are also saved to disk. */
    private static void attach(String name, boolean failed, String title, String mimeType, String extension, Capture capture) {
        try {
            byte[] content = capture.get();
            // E.g. a test skipped before its first step has no log lines of its own.
            if (content.length == 0) {
                return;
            }
            Allure.attachment(title, mimeType, new ByteArrayInputStream(content), AttachmentOptions.withFileExtension(extension));
            // The ReportPortal logback appender uploads it; this logger is off without -Preportportal.
            if (reportPortal.isInfoEnabled()) {
                reportPortal.info("RP_MESSAGE#BASE64#{}#{}", Base64.getEncoder().encodeToString(content), title);
            }
            if (failed) {
                Files.createDirectories(DIR);
                Path file = DIR.resolve(name + extension);
                Files.write(file, content);
                log.info("Saved {}", file);
            }
        } catch (Exception e) {
            log.warn("Could not capture {} for {}: {}", title, name, e.getMessage());
        }
    }
}
