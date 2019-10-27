package com.eightbitlab.supportrenderscriptblur;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.Matrix3f;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.support.v8.renderscript.ScriptIntrinsicColorMatrix;

import eightbitlab.com.blurview.BlurAlgorithm;

/**
 * Blur using RenderScript, processed on GPU.
 * Uses Renderscript from support library
 */
public final class SupportRenderScriptBlur implements BlurAlgorithm {
    private final RenderScript renderScript;
    private final ScriptIntrinsicBlur blurScript;
    private final ScriptIntrinsicColorMatrix colorScript;
    private Allocation outAllocation;

    private int lastBitmapWidth = -1;
    private int lastBitmapHeight = -1;

    /**
     * @param context Context to create the {@link RenderScript}
     */
    public SupportRenderScriptBlur(Context context) {
        renderScript = RenderScript.create(context);
        blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        colorScript = ScriptIntrinsicColorMatrix.create(renderScript, Element.U8_4(renderScript));
    }

    private boolean canReuseAllocation(Bitmap bitmap) {
        return bitmap.getHeight() == lastBitmapHeight && bitmap.getWidth() == lastBitmapWidth;
    }

    /**
     * @param bitmap     bitmap to blur
     * @param blurRadius blur radius (1..25)
     * @return blurred bitmap
     */
    @Override
    public final Bitmap blur(Bitmap bitmap, float blurRadius, float saturate, float contrast) {
        //Allocation will use the same backing array of pixels as bitmap if created with USAGE_SHARED flag
        Allocation inAllocation = Allocation.createFromBitmap(renderScript, bitmap);

        if (!canReuseAllocation(bitmap)) {
            if (outAllocation != null) {
                outAllocation.destroy();
            }
            outAllocation = Allocation.createTyped(renderScript, inAllocation.getType());
            lastBitmapWidth = bitmap.getWidth();
            lastBitmapHeight = bitmap.getHeight();
        }

        blurScript.setRadius(blurRadius);
        blurScript.setInput(inAllocation);
        //do not use inAllocation in forEach. it will cause visual artifacts on blurred Bitmap
        Allocation tmpAllocation = Allocation.createTyped(renderScript, inAllocation.getType());
        blurScript.forEach(tmpAllocation);

        final float invSat = 1 - saturate;
        final float R = 0.213f * invSat;
        final float G = 0.715f * invSat;
        final float B = 0.072f * invSat;
        Matrix3f m = new Matrix3f(new float[]{
                contrast * (R + saturate), R, R,
                G, contrast * (G + saturate), G,
                B, B, contrast * (B + saturate),
        });
        colorScript.setColorMatrix(m);
        colorScript.setAdd((1f - contrast) / 2f, (1f - contrast) / 2f, (1f - contrast) / 2f, 0);
        colorScript.forEach(tmpAllocation, outAllocation);
        outAllocation.copyTo(bitmap);

        inAllocation.destroy();
        return bitmap;
    }

    @Override
    public final void destroy() {
        blurScript.destroy();
        renderScript.destroy();
        if (outAllocation != null) {
            outAllocation.destroy();
        }
    }

    @Override
    public boolean canModifyBitmap() {
        return true;
    }

    @NonNull
    @Override
    public Bitmap.Config getSupportedBitmapConfig() {
        return Bitmap.Config.ARGB_8888;
    }
}
