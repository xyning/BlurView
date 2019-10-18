package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

class NoOpBlurAlgorithm implements BlurAlgorithm {
    @Override
    public Bitmap blur(Bitmap bitmap, float blurRadius) {
        return blur(bitmap, blurRadius, 1);
    }

    @Override
    public Bitmap blur(Bitmap bitmap, float blurRadius, float saturate) {
        return bitmap;
    }

    @Override
    public void destroy() {
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
