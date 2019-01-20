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

import com.sun.glass.utils.NativeLibLoader;
import java.security.Permission;

/**
 * A Java-language interface to the device API of the Electrophoretic Display
 * Controller (EPDC) frame buffer driver. EPDSystem is a singleton. Its instance
 * is obtained by calling the {@link EPDSystem#getEPDSystem} method. This class
 * also extends {@link LinuxSystem.FbVarScreenInfo} to provide all of the fields
 * in {@code fb_var_screeninfo}, defined in {@code linux/fb.h}.
 */
class EPDSystem {

    /**
     * The value for {@link FbVarScreenInfo#setActivate} to ensure that the EPDC
     * driver receives the initialization request.
     */
    static final int FB_ACTIVATE_FORCE = 128;

    /**
     * The value for {@link FbVarScreenInfo#setRotate} to set the frame buffer
     * rotation to un-rotated (landscape mode).
     */
    static final int FB_ROTATE_UR = 0;

    /**
     * The value for {@link FbVarScreenInfo#setRotate} to set the frame buffer
     * rotation to 90-degrees clockwise (upside-down portrait mode).
     */
    static final int FB_ROTATE_CW = 1;

    /**
     * The value for {@link FbVarScreenInfo#setRotate} to set the frame buffer
     * rotation to 180-degrees upside-down (upside-down landscape mode).
     */
    static final int FB_ROTATE_UD = 2;

    /**
     * The value for {@link FbVarScreenInfo#setRotate} to set the frame buffer
     * rotation to 90-degrees counter-clockwise (portrait mode).
     */
    static final int FB_ROTATE_CCW = 3;

    /**
     * The value for {@link FbVarScreenInfo#setGrayscale} to set the frame
     * buffer to an 8-bit grayscale pixel format.
     */
    static final int GRAYSCALE_8BIT = 0x1;

    /**
     * The value for {@link FbVarScreenInfo#setGrayscale} to set the frame
     * buffer to an inverted 8-bit grayscale pixel format.
     */
    static final int GRAYSCALE_8BIT_INVERTED = 0x2;

    /**
     * Region update mode, in which updates to the display must be submitted
     * with an IOCTL call to {@link #MXCFB_SEND_UPDATE}.
     */
    static final int AUTO_UPDATE_MODE_REGION_MODE = 0;

    /**
     * Automatic mode, in which updates are generated automatically by the
     * driver when it detects that pages in a frame buffer memory region have
     * been modified.
     */
    static final int AUTO_UPDATE_MODE_AUTOMATIC_MODE = 1;

    /**
     * Snapshot update scheme, which processes the contents of the frame buffer
     * immediately and stores the update in a memory buffer internal to the
     * driver. When the IOCTL call to {@link #MXCFB_SEND_UPDATE} returns, the
     * frame buffer region is free and can be modified without affecting the
     * update.
     */
    static final int UPDATE_SCHEME_SNAPSHOT = 0;

    /**
     * Queue update scheme, which uses a work queue to handle the processing of
     * updates asynchronously. When updates are submitted with an IOCTL call to
     * {@link #MXCFB_SEND_UPDATE}, they are added to the queue and processed in
     * order as the EPDC hardware resources become available. The frame buffer
     * contents processed and displayed, therefore, may not reflect what was
     * present in the frame buffer when the update was sent.
     */
    static final int UPDATE_SCHEME_QUEUE = 1;

    /**
     * Queue and Merge update scheme, which adds a merging step to the Queue
     * update scheme. Before an update is added to the work queue, it is
     * compared with other pending updates. If a pending update matches the mode
     * and flags of the current update and also overlaps the update region, it
     * will be merged with the current update. After all such merges, the final
     * merged update is submitted to the queue.
     */
    static final int UPDATE_SCHEME_QUEUE_AND_MERGE = 2;

    /**
     * Partial update mode, which applies the waveform to only the pixels that
     * change in a given region.
     */
    static final int UPDATE_MODE_PARTIAL = 0x0;

    /**
     * Full update mode, which applies the waveform to all pixels in a given
     * region.
     */
    static final int UPDATE_MODE_FULL = 0x1;

    /**
     * Auto waveform mode, which requests the driver to select the actual
     * waveform mode automatically based on the contents of the updated region.
     */
    static final int WAVEFORM_MODE_AUTO = 257;

    /**
     * The temperature value that requests the driver to use the ambient
     * temperature of the device.
     */
    static final int TEMP_USE_AMBIENT = 0x1000;

    /**
     * An update flag to enable inversion of all pixels in the updated region.
     */
    static final int EPDC_FLAG_ENABLE_INVERSION = 0x01;

    /**
     * An update flag to enable black-and-white posterization of all pixels in
     * the updated region.
     */
    static final int EPDC_FLAG_FORCE_MONOCHROME = 0x02;

    /**
     * An update flag to enable dithering of an 8-bit grayscale frame buffer to
     * 1-bit black and white, if supported by the driver or hardware.
     */
    static final int EPDC_FLAG_USE_DITHERING_Y1 = 0x2000;

    /**
     * An update flag to enable dithering of an 8-bit grayscale frame buffer to
     * 4-bit grayscale, if supported by the driver or hardware.
     */
    static final int EPDC_FLAG_USE_DITHERING_Y4 = 0x4000;

    /**
     * The power-down delay value to disable the powering down of the EPDC and
     * display power supplies.
     */
    static final int FB_POWERDOWN_DISABLE = -1;

    /**
     * Initialization waveform (0x0...0xF → 0xF in ~4000 ms). Clears the screen
     * to all white.
     * <p>
     * "A first exemplary drive scheme provides waveforms that may be used to
     * change the display state of a pixel from any initial display state to a
     * new display state of white. The first drive scheme may be referred to as
     * an initialization or 'INIT' drive scheme." [United States Patent
     * 9,280,955]
     */
    static final int WAVEFORM_MODE_INIT = 0;

    /**
     * Direct update waveform (0x0...0xF → 0x0 or 0xF in ~260 ms). Changes gray
     * pixels to black or white.
     * <p>
     * "A second exemplary drive scheme provides waveforms that may be used to
     * change the display state of a pixel from any initial display state to a
     * new display state of either white or black. The second drive scheme may
     * be referred to as a 'DU' drive scheme." [United States Patent 9,280,955]
     */
    static final int WAVEFORM_MODE_DU = 1;

    /**
     * Gray 4-level waveform (0x0...0xF → 0x0, 0x5, 0xA, or 0xF in ~500 ms).
     * Supports 2-bit grayscale images and text with lower quality.
     * <p>
     * "A third exemplary drive scheme provides waveforms that may be used to
     * change the display state of a pixel from any initial display state to a
     * new display state. The initial state may be any four-bit (16 gray states)
     * value. The new display state may be any two-bit (4 gray states) value.
     * The third drive scheme may be referred to as a 'GC4' drive scheme."
     * [United States Patent 9,280,955]
     */
    static final int WAVEFORM_MODE_GC4 = 3;

    /**
     * Gray 16-level waveform (0x0...0xF → 0x0...0xF in ~760 ms). Supports 4-bit
     * grayscale images and text with high quality.
     * <p>
     * "A fourth exemplary drive scheme provides waveforms that may be used to
     * change the display state of a pixel from any initial display state to a
     * new display state. The initial state may be any four-bit (16 gray states)
     * value. The new display state may be any four-bit (16 gray states) value.
     * The fourth drive scheme may be referred to as a 'GC16' drive scheme."
     * [United States Patent 9,280,955]
     */
    static final int WAVEFORM_MODE_GC16 = 2;

    /**
     * Animation waveform (0x0 or 0xF → 0x0 or 0xF in ~120 ms). Provides a fast
     * 1-bit black-and-white animation mode of up to eight frames per second.
     * <p>
     * "A fifth exemplary drive scheme provides waveforms that may be used to
     * change the display state of a pixel from an initial display state to a
     * new display state. The initial state must be white or black. The new
     * display state may be black or white. The fifth drive scheme may be
     * referred to as an 'A2' drive scheme. An advantage of A2 waveforms is that
     * they have generally short waveform periods, providing rapid display
     * updates. A disadvantage of A2 waveforms is that there use may result in
     * ghosting artifacts." [United States Patent 9,280,955]
     */
    static final int WAVEFORM_MODE_A2 = 4;

    private static final Permission PERMISSION = new RuntimePermission("loadLibrary.*");
    private static final EPDSystem INSTANCE = new EPDSystem();

    /**
     * Checks for permission to load native libraries if running under a
     * security manager.
     */
    private static void checkPermissions() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(PERMISSION);
        }
    }

    /**
     * Obtains the single instance of EPDSystem. Calling this method requires
     * the runtime permission "{@code loadLibrary.*}". The {@link #loadLibrary}
     * method must be called on the EPDSystem instance before any system calls
     * can be made using it.
     *
     * @return the EPDSystem instance
     */
    static EPDSystem getEPDSystem() {
        checkPermissions();
        return INSTANCE;
    }

    /**
     * The IOCTL request code to define a mapping for common waveform modes.
     */
    int MXCFB_SET_WAVEFORM_MODES;

    /**
     * The IOCTL request code to set the temperature used by the EPDC driver in
     * subsequent panel updates.
     */
    int MXCFB_SET_TEMPERATURE;

    /**
     * The IOCTL request code to select between automatic and region update
     * mode.
     */
    int MXCFB_SET_AUTO_UPDATE_MODE;

    /**
     * The IOCTL request code to update a region of the frame buffer to the
     * display.
     */
    int MXCFB_SEND_UPDATE;

    /**
     * The IOCTL request code to block and wait for a previous update to
     * complete.
     */
    int MXCFB_WAIT_FOR_UPDATE_COMPLETE;

    /**
     * The IOCTL request code to set the delay between the completion of all
     * updates in the driver and when the driver should power down the EPDC and
     * display power supplies.
     */
    int MXCFB_SET_PWRDOWN_DELAY;

    /**
     * The IOCTL request code to get the current power-down delay value from the
     * driver.
     */
    int MXCFB_GET_PWRDOWN_DELAY;

    /**
     * The IOCTL request code to select a scheme for the flow of updates within
     * the driver.
     */
    int MXCFB_SET_UPDATE_SCHEME;

    private final LinuxSystem system;

    /**
     * Creates the single instance of EPDSystem.
     */
    private EPDSystem() {
        system = LinuxSystem.getLinuxSystem();
    }

    /**
     * Loads the native libraries required to make system calls using this
     * EPDSystem instance. This method must be called before any other instance
     * methods of EPDSystem. If this method is called multiple times, it has no
     * effect after the first call.
     */
    void loadLibrary() {
        NativeLibLoader.loadLibrary("glass_monocle_epd");

        /*
         * Creating IOCTL request codes requires the native "sizeof" function.
         */
        var modes = new MxcfbWaveformModes();
        var update = new MxcfbUpdateData();
        var integer = new IntStructure();

        MXCFB_SET_WAVEFORM_MODES = system.IOW('F', 0x2B, modes.sizeof());
        MXCFB_SET_TEMPERATURE = system.IOW('F', 0x2C, Integer.BYTES);
        MXCFB_SET_AUTO_UPDATE_MODE = system.IOW('F', 0x2D, Integer.BYTES);
        MXCFB_SEND_UPDATE = system.IOW('F', 0x2E, update.sizeof());
        MXCFB_WAIT_FOR_UPDATE_COMPLETE = system.IOW('F', 0x2F, Integer.BYTES);
        MXCFB_SET_PWRDOWN_DELAY = system.IOW('F', 0x30, Integer.BYTES);
        MXCFB_GET_PWRDOWN_DELAY = system.IOR('F', 0x31, integer.sizeof());
        MXCFB_SET_UPDATE_SCHEME = system.IOW('F', 0x32, Integer.BYTES);
    }

    /**
     * Passes an integer parameter by value to the device driver through the
     * IOCTL interface. ({@link LinuxSystem#ioctl}, instead, takes a pointer as
     * its third parameter, passing its data by reference.)
     *
     * @param fd an open file descriptor
     * @param request a device-dependent request code
     * @param value the integer value
     * @return 0 if successful; otherwise -1 and {@code errno} is set
     * appropriately
     */
    native int ioctl(long fd, int request, int value);

    /**
     * A structure for passing an integer by value in an IOCTL call.
     */
    static class IntStructure extends C.Structure {

        IntStructure() {
            checkPermissions();
        }

        @Override
        native int sizeof();

        native int getInteger(long p);

        native void setInteger(long p, int value);
    }

    /**
     * Wraps the C structure {@code mxcfb_waveform_modes}, defined in
     * {@code mxcfb.h}.
     */
    static class MxcfbWaveformModes extends C.Structure {

        MxcfbWaveformModes() {
            checkPermissions();
        }

        @Override
        native int sizeof();

        native int getModeInit(long p);

        native int getModeDu(long p);

        native int getModeGc4(long p);

        native int getModeGc8(long p);

        native int getModeGc16(long p);

        native int getModeGc32(long p);

        native void setModes(long p, int init, int du, int gc4, int gc8, int gc16, int gc32);

        native void print(long p);
    }

    /**
     * Wraps the C structure {@code mxcfb_update_data}, defined in
     * {@code mxcfb.h}.
     */
    static class MxcfbUpdateData extends C.Structure {

        MxcfbUpdateData() {
            checkPermissions();
        }

        @Override
        native int sizeof();

        native int getUpdateRegionTop(long p);

        native int getUpdateRegionLeft(long p);

        native int getUpdateRegionWidth(long p);

        native int getUpdateRegionHeight(long p);

        native int getWaveformMode(long p);

        native int getUpdateMode(long p);

        native int getUpdateMarker(long p);

        native int getTemp(long p);

        native int getFlags(long p);

        native long getAltBufferDataVirtAddr(long p);

        native long getAltBufferDataPhysAddr(long p);

        native int getAltBufferDataWidth(long p);

        native int getAltBufferDataHeight(long p);

        native int getAltBufferDataUpdateRegionTop(long p);

        native int getAltBufferDataUpdateRegionLeft(long p);

        native int getAltBufferDataUpdateRegionWidth(long p);

        native int getAltBufferDataUpdateRegionHeight(long p);

        native void setUpdateRegion(long p, int top, int left, int width, int height);

        native void setWaveformMode(long p, int mode);

        native void setUpdateMode(long p, int mode);

        native void setUpdateMarker(long p, int marker);

        native void setTemp(long p, int temp);

        native void setFlags(long p, int flags);

        native void setAltBufferData(long p, long virtAddr, long physAddr, int width, int height,
                int updateRegionTop, int updateRegionLeft, int updateRegionWidth, int updateRegionHeight);

        native void print(long p);
    }

    /**
     * Wraps the entire C structure {@code fb_var_screeninfo}, defined in
     * {@code linux/fb.h}.
     */
    static class FbVarScreenInfo extends LinuxSystem.FbVarScreenInfo {

        native int getGrayscale(long p);

        native int getRedOffset(long p);

        native int getRedLength(long p);

        native int getRedMsbRight(long p);

        native int getGreenOffset(long p);

        native int getGreenLength(long p);

        native int getGreenMsbRight(long p);

        native int getBlueOffset(long p);

        native int getBlueLength(long p);

        native int getBlueMsbRight(long p);

        native int getTranspOffset(long p);

        native int getTranspLength(long p);

        native int getTranspMsbRight(long p);

        native int getNonstd(long p);

        native int getActivate(long p);

        native int getHeight(long p);

        native int getWidth(long p);

        native int getAccelFlags(long p);

        native int getPixclock(long p);

        native int getLeftMargin(long p);

        native int getRightMargin(long p);

        native int getUpperMargin(long p);

        native int getLowerMargin(long p);

        native int getHsyncLen(long p);

        native int getVsyncLen(long p);

        native int getSync(long p);

        native int getVmode(long p);

        native int getRotate(long p);

        native void setGrayscale(long p, int grayscale);

        native void setNonstd(long p, int nonstd);

        native void setHeight(long p, int height);

        native void setWidth(long p, int width);

        native void setAccelFlags(long p, int accelFlags);

        native void setPixclock(long p, int pixclock);

        native void setLeftMargin(long p, int leftMargin);

        native void setRightMargin(long p, int rightMargin);

        native void setUpperMargin(long p, int upperMargin);

        native void setLowerMargin(long p, int lowerMargin);

        native void setHsyncLen(long p, int hsyncLen);

        native void setVsyncLen(long p, int vsyncLen);

        native void setSync(long p, int sync);

        native void setVmode(long p, int vmode);

        native void setRotate(long p, int rotate);
    }
}
