package com.shanesweeney.mobile.core;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

/** Starts the project-local Appium server once per JVM unless one is already listening. */
public final class AppiumServer {
    private static final URL URL = Config.APPIUM_URL;
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    private static AppiumDriverLocalService service;

    private AppiumServer() {}

    public static synchronized URL ensureRunning() {
        if (service != null || isReady()) {
            return URL;
        }
        service = new AppiumServiceBuilder()
                .withAppiumJS(ROOT.resolve("node_modules/appium/index.js").toFile())
                .withIPAddress(URL.getHost())
                .usingPort(URL.getPort())
                .withArgument(() -> "--config", ROOT.resolve(".appiumrc.json").toString())
                .withEnvironment(Map.of("APPIUM_HOME", ROOT.resolve(".appium").toString()))
                .withLogFile(new File(ROOT.toFile(), "appium.log"))
                .build();
        // The service also mirrors logs to stdout by default; keep them in appium.log only.
        service.clearOutPutStreams();
        service.start();
        Runtime.getRuntime().addShutdownHook(new Thread(service::stop));
        return URL;
    }

    private static boolean isReady() {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(URL + "/status").toURL().openConnection();
            conn.setConnectTimeout(1_000);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
