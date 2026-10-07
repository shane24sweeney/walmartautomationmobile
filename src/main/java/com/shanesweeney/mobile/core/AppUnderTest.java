package com.shanesweeney.mobile.core;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.InteractsWithApps;

import java.nio.file.Path;

/** The app being tested: where its builds live and how to put it back in a clean state. */
public final class AppUnderTest {
    public static final String ANDROID_PACKAGE = "com.wdiodemoapp";
    public static final String IOS_BUNDLE_ID = "org.wdiodemoapp";

    // Fetched by scripts/download-apps.sh. Appium installs them on session start.
    private static final Path ROOT = Path.of(System.getProperty("user.dir"));
    public static final Path ANDROID_APP = ROOT.resolve("apps/android.apk");
    public static final Path IOS_APP = ROOT.resolve("apps/ios.app");

    private AppUnderTest() {}

    public static String id() {
        return Config.PLATFORM == Config.Platform.IOS ? IOS_BUNDLE_ID : ANDROID_PACKAGE;
    }

    /**
     * Kills and relaunches the app so every test starts on the home screen with no state left
     * by the previous test. Much faster than a new session, which reinstalls the app.
     */
    public static void restart(AppiumDriver driver) {
        InteractsWithApps apps = (InteractsWithApps) driver;
        apps.terminateApp(id());
        apps.activateApp(id());
    }
}
