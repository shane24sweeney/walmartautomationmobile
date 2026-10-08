package com.shanesweeney.mobile.tests;

import com.shanesweeney.mobile.core.AppUnderTest;
import com.shanesweeney.mobile.core.DevicePool;
import com.shanesweeney.mobile.core.DriverFactory;
import com.shanesweeney.mobile.pages.TabBar;
import io.appium.java_client.AppiumDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Layer 1 (tests): owns the app session; test classes extend this and use pages only.
 * Each class claims its own device from the pool, so classes can run in parallel.
 * The session is shared by a class for speed, but the app restarts before every test,
 * so tests never depend on each other or on the order they run in.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(FailureArtifacts.class)
public abstract class BaseTest {
    protected AppiumDriver driver;
    // Entry point to every screen: tests navigate from here and never touch the driver.
    protected TabBar tabBar;
    private DevicePool.Device device;

    @BeforeAll
    void startSession() {
        device = DevicePool.acquire();
        try {
            driver = DriverFactory.create(device);
            tabBar = new TabBar(driver);
        } catch (RuntimeException e) {
            DevicePool.release(device);
            device = null;
            throw e;
        }
    }

    @BeforeEach
    void restartApp() {
        AppUnderTest.restart(driver);
    }

    @AfterAll
    void endSession() {
        try {
            if (driver != null) {
                driver.quit();
            }
        } finally {
            if (device != null) {
                DevicePool.release(device);
            }
        }
    }
}
