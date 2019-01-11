/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.glass.ui.monocle;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * EPDInputDeviceRegistry maintains an observable set of input devices for the
 * keypad buttons and touch screen on the system. It is responsible for
 * detecting the attached input devices and generating input events from these
 * devices.
 * <p>
 * Run the following commands as <i>root</i> to list the properties of the
 * keypad (event0) and touch screen (event1) input devices:
 * <pre>
 * # udevadm info -q all -n /dev/input/event0
 * # udevadm info -q all -n /dev/input/event1
 * </pre>
 * <b>Notes:</b>
 * <p>
 * EPDPlatform creates an instance of this class instead of creating a
 * LinuxInputDeviceRegistry because this class needs to override the following
 * methods:
 * <dl>
 * <dt>{@link #createDevice}</dt>
 * <dd>to work around bug JDK-8201568 by opening the device before creating its
 * LinuxInputDevice.</dd>
 * <dt>{@link #addDeviceInternal}</dt>
 * <dd>to work around older versions of <i>udev</i>, such as version 142, which
 * do not return the property <code>ID_INPUT_TOUCHSCREEN</code> for the touch
 * screen device. The property value <code>ID_INPUT_TOUCHSCREEN=1</code> is
 * required by {@link LinuxInputDevice#isTouch} to detect a touch screen device.
 * Otherwise, the method returns <code>false</code> and the touch screen is
 * mistakenly given a keyboard input processor. Newer versions of <i>udev</i>,
 * such as version 204, correctly return this property.</dd>
 * </dl>
 * Therefore, once JDK-8201568 is fixed and the old version of <i>udev</i> is no
 * longer in use, this entire class can be removed and replaced by
 * LinuxInputDeviceRegistry.
 */
class EPDInputDeviceRegistry extends InputDeviceRegistry {

    /**
     * The file name of the keypad input device.
     */
    private static final String KEYPAD_FILENAME = "event0";

    /**
     * The file name of the touch screen input device.
     */
    private static final String TOUCH_FILENAME = "event1";

    /**
     * Creates an observable set of input devices.
     * <p>
     * <b>Note:</b> This is a verbatim copy of the LinuxInputDeviceRegistry
     * constructor.
     *
     * @param headless <code>true</code> if this environment cannot support a
     * display, keyboard, and mouse; otherwise <code>false</code>.
     */
    EPDInputDeviceRegistry(boolean headless) {
        if (headless) {
            // Keep the registry but do not bind it to udev.
            return;
        }
        Map<File, LinuxInputDevice> deviceMap = new HashMap<>();
        UdevListener udevListener = (action, event) -> {
            String subsystem = event.get("SUBSYSTEM");
            String devPath = event.get("DEVPATH");
            String devName = event.get("DEVNAME");
            if (subsystem != null && subsystem.equals("input")
                    && devPath != null && devName != null) {
                try {
                    File sysPath = new File("/sys", devPath);
                    if (action.equals("add")
                            || (action.equals("change")
                            && !deviceMap.containsKey(sysPath))) {
                        File devNode = new File(devName);
                        LinuxInputDevice device = createDevice(
                                devNode, sysPath, event);
                        if (device != null) {
                            deviceMap.put(sysPath, device);
                        }
                    } else if (action.equals("remove")) {
                        LinuxInputDevice device = deviceMap.get(sysPath);
                        deviceMap.remove(sysPath);
                        if (device != null) {
                            devices.remove(device);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Udev.getInstance().addListener(udevListener);
        // Request updates for existing devices
        SysFS.triggerUdevNotification("input");
    }

    /**
     * Creates an input device.
     * <p>
     * <b>Note:</b> This method works around bug
     * <a href="https://bugs.openjdk.java.net/browse/JDK-8201568">JDK-8201568</a>,
     * "zForce touchscreen input device fails when closed and immediately
     * reopened." It avoids the problem by opening the device before creating
     * its LinuxInputDevice.
     *
     * @param devNode the file representing the device name, such as
     * <code>/dev/input/event1</code>.
     * @param sysPath the system path to the device, such as
     * <code>/sys/devices/virtual/input/input1/event1</code>.
     * @param udevManifest the set of properties for the device.
     * @return the new LinuxInputDevice, or <code>null</code> if no processor is
     * found for the device.
     * @throws IOException if an error occurs opening the device.
     */
    private LinuxInputDevice createDevice(File devNode, File sysPath,
            Map<String, String> udevManifest) throws IOException {
        LinuxSystem system = LinuxSystem.getLinuxSystem();
        system.open(devNode.getPath(), LinuxSystem.O_RDONLY);

        var device = new LinuxInputDevice(devNode, sysPath, udevManifest);
        return addDeviceInternal(device, "Linux input: " + devNode.toString());
    }

    /**
     * Creates an input processor for the device. Run the following commands as
     * <i>root</i> to display the events generated by the keypad (0) and touch
     * screen (1) input devices when you press buttons or touch the screen:
     * <pre>
     * # input-events 0
     * # input-events 1
     * </pre>
     * <b>Note:</b> The "mxckpd" keypad device driver does not generate EV_SYN
     * events, but {@link LinuxInputDevice#run} schedules an event for
     * processing only after receiving the EV_SYN event terminator (see
     * {@link LinuxEventBuffer#put}). The events from this device, therefore,
     * are never delivered to the JavaFX application. The "gpio-keys" device
     * driver on more recent systems, though, correctly generates the EV_SYN
     * event terminator for keypad events.
     *
     * @param device the LinuxInputDevice.
     * @param name the device name, such as <code>/dev/input/event0</code>.
     * @return the LinuxInputDevice, or <code>null</code> if no input processor
     * is found for the device.
     */
    private LinuxInputDevice addDeviceInternal(LinuxInputDevice device, String name) {
        LinuxInputProcessor processor = null;
        if (name.endsWith(KEYPAD_FILENAME)) {
            processor = new LinuxKeyProcessor();
        } else if (name.endsWith(TOUCH_FILENAME)) {
            processor = new LinuxSimpleTouchProcessor(device);
        }
        if (processor == null) {
            return null;
        } else {
            device.setInputProcessor(processor);
            var thread = new Thread(device);
            thread.setName(name);
            thread.setDaemon(true);
            thread.start();
            devices.add(device);
            return device;
        }
    }
}
