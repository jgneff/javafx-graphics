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

import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.util.Logging;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * FramebufferY8 holds a buffer used to compose JavaFX scenes. The buffer stores
 * 32-bit pixels in ARGB32 format and is either the Linux frame buffer itself or
 * an off-screen composition buffer. This class can write the contents of this
 * buffer to a target byte channel, or copy them to a target byte buffer, as
 * pixels in the 32-bit ARGB32 color format, the 16-bit RGB565 color format, or
 * the 8-bit Y8 grayscale format.
 */
class FramebufferY8 extends Framebuffer {

    /**
     * The arithmetic right shift value to convert a bit depth to a byte depth.
     */
    private static final int BITS_TO_BYTES = 3;

    private final PlatformLogger logger = Logging.getJavaFXLogger();
    private final ByteBuffer bb;
    private final int width;
    private final int height;
    private final int bitDepth;
    private final int byteDepth;

    private ByteBuffer lineByteBuffer;
    private Buffer linePixelBuffer;

    /**
     * Creates a new FramebufferY8.
     *
     * @param bb the 32-bit ARGB32 composition buffer.
     * @param width the width of the buffer in pixels.
     * @param height the height of the buffer in pixels.
     * @param depth the color depth of the target, in bits per pixel.
     * @param clear <code>true</code> to clear the buffer on the first upload of
     * each frame unless that upload already overwrites the entire buffer;
     * otherwise <code>false</code>.
     */
    FramebufferY8(ByteBuffer bb, int width, int height, int depth, boolean clear) {
        super(bb, width, height, depth, clear);
        this.bb = bb;
        this.width = width;
        this.height = height;
        this.bitDepth = depth;
        this.byteDepth = depth >>> BITS_TO_BYTES;
    }

    /**
     * Copies the next pixel to a ByteBuffer with 8-bit Y8 pixels. Luma Y' can
     * be calculated from gamma-corrected R'G'B' using the Rec. 601 or Rec. 709
     * coefficients. This method uses the coefficients from Rec. 709.
     * <pre>
     * Rec. 601: Y' = 0.299  × R' + 0.587  × G' + 0.114  × B'
     * Rec. 709: Y' = 0.2126 × R' + 0.7152 × G' + 0.0722 × B'
     * </pre>
     * <b>Note:</b> Java rounds toward zero when converting a float to an int,
     * so this method adds 0.5 before the type conversion so that the number
     * ends up rounded to the nearest integer.
     *
     * @param source the source IntBuffer in ARGB32 format.
     * @param target the target ByteBuffer in Y8 format.
     */
    private void copyNextPixel(IntBuffer source, ByteBuffer target) {
        int pixel32 = source.get();
        int r = (pixel32 >> 16) & 0xFF;
        int g = (pixel32 >> 8) & 0xFF;
        int b = pixel32 & 0xFF;
        int y = (int) (0.2126f * r + 0.7152f * g + 0.0722f * b + 0.5f);
        target.put((byte) y);
    }

    /**
     * Copies the next pixel to a ShortBuffer with 16-bit RGB565 pixels.
     *
     * @param source the source IntBuffer in ARGB32 format.
     * @param target the target ShortBuffer in RGB565 format.
     */
    private void copyNextPixel(IntBuffer source, ShortBuffer target) {
        int pixel32 = source.get();
        int r = (pixel32 >> 8) & 0xF800;
        int g = (pixel32 >> 5) & 0x07E0;
        int b = (pixel32 >> 3) & 0x001F;
        int pixel16 = r | g | b;
        target.put((short) pixel16);
    }

    /**
     * Writes the contents of the composition buffer to the output channel,
     * converting the pixel format as necessary.
     * <p>
     * <b>Note:</b> When multi-core processors and high-resolution screens are
     * more common in embedded devices, consider converting the pixels with a
     * parallel stream operation instead of using <i>for loops</i>.
     *
     * @param out the output channel.
     * @throws IOException if an error occurs writing to the channel.
     */
    @Override
    void write(WritableByteChannel out) throws IOException {
        bb.clear();
        switch (byteDepth) {
            case Byte.BYTES: {
                if (lineByteBuffer == null) {
                    lineByteBuffer = ByteBuffer.allocate(width * Byte.BYTES);
                    lineByteBuffer.order(ByteOrder.nativeOrder());
                    linePixelBuffer = lineByteBuffer.duplicate();
                }
                IntBuffer srcPixels = bb.asIntBuffer();
                ByteBuffer byteBuffer = (ByteBuffer) linePixelBuffer;
                for (int y = 0; y < height; y++) {
                    byteBuffer.clear();
                    for (int x = 0; x < width; x++) {
                        copyNextPixel(srcPixels, byteBuffer);
                    }
                    lineByteBuffer.clear();
                    out.write(lineByteBuffer);
                }
                break;
            }
            case Short.BYTES: {
                if (lineByteBuffer == null) {
                    lineByteBuffer = ByteBuffer.allocate(width * Short.BYTES);
                    lineByteBuffer.order(ByteOrder.nativeOrder());
                    linePixelBuffer = lineByteBuffer.asShortBuffer();
                }
                IntBuffer srcPixels = bb.asIntBuffer();
                ShortBuffer shortBuffer = (ShortBuffer) linePixelBuffer;
                for (int y = 0; y < height; y++) {
                    shortBuffer.clear();
                    for (int x = 0; x < width; x++) {
                        copyNextPixel(srcPixels, shortBuffer);
                    }
                    lineByteBuffer.clear();
                    out.write(lineByteBuffer);
                }
                break;
            }
            case Integer.BYTES: {
                out.write(bb);
                break;
            }
            default:
                logger.severe("Unsupported color depth: {0} bpp", bitDepth);
                throw new IllegalArgumentException();
        }
    }

    /**
     * Copies the contents of the composition buffer to the output buffer,
     * converting the pixel format as necessary.
     *
     * @param out the output buffer.
     */
    @Override
    void copyToBuffer(ByteBuffer out) {
        bb.clear();
        switch (byteDepth) {
            case Byte.BYTES: {
                if (lineByteBuffer == null) {
                    lineByteBuffer = ByteBuffer.allocate(width * Byte.BYTES);
                    lineByteBuffer.order(ByteOrder.nativeOrder());
                    linePixelBuffer = lineByteBuffer.duplicate();
                }
                IntBuffer srcPixels = bb.asIntBuffer();
                ByteBuffer byteBuffer = (ByteBuffer) linePixelBuffer;
                for (int y = 0; y < height; y++) {
                    byteBuffer.clear();
                    for (int x = 0; x < width; x++) {
                        copyNextPixel(srcPixels, byteBuffer);
                    }
                    lineByteBuffer.clear();
                    out.put(lineByteBuffer);
                }
                break;
            }
            case Short.BYTES: {
                if (lineByteBuffer == null) {
                    lineByteBuffer = ByteBuffer.allocate(width * Short.BYTES);
                    lineByteBuffer.order(ByteOrder.nativeOrder());
                    linePixelBuffer = lineByteBuffer.asShortBuffer();
                }
                IntBuffer srcPixels = bb.asIntBuffer();
                ShortBuffer shortBuffer = (ShortBuffer) linePixelBuffer;
                for (int y = 0; y < height; y++) {
                    shortBuffer.clear();
                    for (int x = 0; x < width; x++) {
                        copyNextPixel(srcPixels, shortBuffer);
                    }
                    lineByteBuffer.clear();
                    out.put(lineByteBuffer);
                }
                break;
            }
            case Integer.BYTES: {
                out.put(bb);
                break;
            }
            default:
                logger.severe("Unsupported color depth: {0} bpp", bitDepth);
                throw new IllegalArgumentException();
        }
    }
}