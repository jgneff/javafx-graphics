/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.d3d;

import com.sun.javafx.geom.transform.Affine3D;
import com.sun.prism.Graphics;
import com.sun.prism.RenderTarget;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.ps.BaseShaderGraphics;
import com.sun.prism.paint.Color;

class D3DGraphics extends BaseShaderGraphics implements D3DContextSource {

    private final D3DContext context;

    private D3DGraphics(D3DContext context, RenderTarget target) {
        super(context, target);
        this.context = context;
    }

    @Override
    public void getPaintShaderTransform(Affine3D ret) {
        super.getPaintShaderTransform(ret);
        ret.preTranslate(-0.5, -0.5, 0.0);
    }

    static Graphics create(RenderTarget target, D3DContext context) {
        if (target == null) {
            return null;
        }
        long resourceHandle = ((D3DRenderTarget)target).getResourceHandle();
        if (resourceHandle == 0) {
            return null;
        }

        if (PrismSettings.verbose && context.isLost()) {
            System.err.println("Create graphics while the device is lost");
        }

        return new D3DGraphics(context, target);
    }

    @Override
    public void clear(Color color) {
        context.validateClearOp(this);
        this.getRenderTarget().setOpaque(color.isOpaque());
        int res = nClear(context.getContextHandle(),
                          color.getIntArgbPre(), isDepthBuffer(), false);
        D3DContext.validate(res);
    }

    @Override
    public void sync() {
        context.flushVertexBuffer();
    }

    @Override
    public D3DContext getContext() {
        return context;
    }

    private static native int nClear(long pContext, int colorArgbPre,
                                      boolean clearDepth, boolean ignoreScissor);
}
