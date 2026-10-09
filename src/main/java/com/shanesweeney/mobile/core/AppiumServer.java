package com.shanesweeney.mobile.core;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import org.openqa.selenium.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Starts each local Appium server in {@link Config#APPIUM_URLS} once per JVM unless one is already
 * listening; each writes its own {@code appium-<port>.log}. Remote servers and grids are managed
 * elsewhere and used as is.
 */
public final class AppiumServer {
    private static final Logger log = LoggerFactory.getLogger(AppiumServer.class);
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    // Matches the appium version pinned in package.json; an older global install may be listening on the port.
    private static final int REQUIRED_MAJOR = 3;
    // Servers this JVM started or already checked, so later sessions skip the status call.
    // Keyed by string: URL.equals resolves host names over DNS.
    private static final Set<String> READY = new HashSet<>();

    private AppiumServer() {}

    public static synchronized URL ensureRunning(URL url) {
        if (READY.contains(url.toString()) || !isLocal(url)) {
            return url;
        }
        String runningVersion = runningVersion(url);
        if (runningVersion != null) {
            if (!runningVersion.startsWith(REQUIRED_MAJOR + ".")) {
                throw new IllegalStateException("The Appium server at " + url + " is " + runningVersion
                        + "; this framework needs Appium " + REQUIRED_MAJOR + ". Stop it and let the tests start"
                        + " the project's server, or run `npm run appium`.");
            }
            log.info("Using the Appium {} server already running at {}", runningVersion, url);
        } else {
            start(url);
        }
        READY.add(url.toString());
        return url;
    }

    private static void start(URL url) {
        String logFile = "appium-" + url.getPort() + ".log";
        log.info("Starting Appium at {}; its log is {}", url, logFile);
        AppiumDriverLocalService service = new AppiumServiceBuilder()
                .withAppiumJS(ROOT.resolve("node_modules/appium/index.js").toFile())
                .withIPAddress(url.getHost())
                .usingPort(url.getPort())
                .withArgument(() -> "--config", ROOT.resolve(".appiumrc.json").toString())
                .withEnvironment(Map.of("APPIUM_HOME", ROOT.resolve(".appium").toString()))
                .withLogFile(new File(ROOT.toFile(), logFile))
                .build();
        // The service also mirrors logs to stdout by default; keep them in the log file only.
        service.clearOutPutStreams();
        service.start();
        Runtime.getRuntime().addShutdownHook(new Thread(service::stop));
    }

    private static boolean isLocal(URL url) {
        return url.getHost().equals("127.0.0.1") || url.getHost().equals("localhost");
    }

    /** The version a server already listening on {@code url} reports, or null if none is listening. */
    @SuppressWarnings("unchecked")
    private static String runningVersion(URL url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url + "/status").toURL().openConnection();
            conn.setConnectTimeout(1_000);
            if (conn.getResponseCode() != 200) {
                return null;
            }
            try (InputStream body = conn.getInputStream()) {
                Map<String, Object> status = new Json().toType(new String(body.readAllBytes(), StandardCharsets.UTF_8), Map.class);
                Map<String, Object> build = (Map<String, Object>) ((Map<String, Object>) status.get("value")).get("build");
                return build == null ? "unknown" : String.valueOf(build.get("version"));
            }
        } catch (Exception e) {
            return null;
        }
    }
}
