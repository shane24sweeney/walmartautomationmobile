package com.shanesweeney.mobile.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

/**
 * Hands each parallel test class its own device from {@link Config#DEVICES}, so concurrent
 * sessions never share an emulator/simulator or the local ports Appium uses to reach it.
 */
public final class DevicePool {
    private static final Logger log = LoggerFactory.getLogger(DevicePool.class);
    public record Device(int index, String name) {
        /** UiAutomator2 server port on the host; must be unique per Android session. */
        public int systemPort() {
            return 8200 + index;
        }

        /** WebDriverAgent port on the host; must be unique per iOS session. */
        public int wdaLocalPort() {
            return 8100 + index;
        }

        /** Spreads devices across the configured Appium servers, round robin. */
        public URL appiumUrl() {
            return Config.APPIUM_URLS.get(index % Config.APPIUM_URLS.size());
        }
    }

    private static final List<Device> DEVICES = IntStream.range(0, Config.DEVICES.size())
            .mapToObj(i -> new Device(i, Config.DEVICES.get(i)))
            .toList();
    private static final BlockingQueue<Device> FREE = new LinkedBlockingQueue<>(DEVICES);

    private DevicePool() {}

    /** Blocks until a device is free, so extra test threads queue instead of colliding. */
    public static Device acquire() {
        try {
            Device device = FREE.take();
            log.info("Acquired device {}", device.name());
            return device;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a free device", e);
        }
    }

    public static void release(Device device) {
        log.info("Released device {}", device.name());
        FREE.add(device);
    }
}
