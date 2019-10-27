package eightbitlab.com.blurview;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.Matrix3f;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicColorMatrix;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

/**
 * Blur using RenderScript, processed on GPU.
 * Requires API 17+
 */
public final class RenderScriptBlur implements BlurAlgorithm {
    private final RenderScript renderScript;
    private final ScriptIntrinsicBlur blurScript;
    private final ScriptIntrinsicColorMatrix colorScript;
    private Allocation outAllocation;

    private int lastBitmapWidth = -1;
    private int lastBitmapHeight = -1;

    /**
     * @param context Context to create the {@link RenderScript}
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    public RenderScriptBlur(Context context) {
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
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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
