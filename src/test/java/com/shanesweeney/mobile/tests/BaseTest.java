package com.shanesweeney.mobile.tests;

import com.shanesweeney.mobile.core.DevicePool;
import com.shanesweeney.mobile.core.DriverFactory;
import io.appium.java_client.AppiumDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Layer 1 (tests): owns the app session; test classes extend this and use pages only.
 * Each class claims its own device from the pool, so classes can run in parallel.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(FailureArtifacts.class)
public abstract class BaseTest {
    protected AppiumDriver driver;
    private DevicePool.Device device;

    @BeforeAll
    void startSession() {
        device = DevicePool.acquire();
        try {
            driver = DriverFactory.create(device);
        } catch (RuntimeException e) {
            DevicePool.release(device);
            device = null;
            throw e;
        }
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
