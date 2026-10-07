package com.shanesweeney.mobile.core;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;

/**
 * Hands each parallel test class its own device from {@link Config#DEVICES}, so concurrent
 * sessions never share an emulator/simulator or the local ports Appium uses to reach it.
 */
public final class DevicePool {
    public record Device(int index, String name) {
        /** UiAutomator2 server port on the host; must be unique per Android session. */
        public int systemPort() {
            return 8200 + index;
        }

        /** WebDriverAgent port on the host; must be unique per iOS session. */
        public int wdaLocalPort() {
            return 8100 + index;
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
            return FREE.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for a free device", e);
        }
    }

    public static void release(Device device) {
        FREE.add(device);
    }
}
