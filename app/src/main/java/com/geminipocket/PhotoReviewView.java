package com.geminipocket;

import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.view.*;
import android.view.animation.PathInterpolator;
import android.widget.*;
import java.io.File;

/** Owns a bounded review bitmap and a reversible viewfinder-to-card transition. */
public final class PhotoReviewView extends FrameLayout {
    private final ImageView image;
    private final LinearLayout card;
    private final LinearLayout actions;
    private final View scrim;
    private final Rect source;
    private final Runnable retake;
    private Bitmap bitmap;
    private ValueAnimator transition;
    private boolean moving = true;
    private boolean disposed;
    private RectF target;
    private final GradientDrawable imageShape;

    public PhotoReviewView(Context context, File file, Rect from, boolean animate, Runnable onRetake, Runnable onUse) {
        super(context);
        source = new Rect(from);
        retake = onRetake;
        boolean dark = (getResources().getConfiguration().uiMode & 48) == 32;
        int surface = dark ? 0xFF1E222B : 0xFFF2F5FC;
        int ink = dark ? 0xFFE5E9F3 : 0xFF1B2334;
        int accent = dark ? 0xFFAAC7FF : 0xFF3459CF;
        setClickable(true);
        setFocusableInTouchMode(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        scrim = new View(context);
        scrim.setBackgroundColor(0xB3000000);
        addView(scrim, new LayoutParams(-1, -1));
        card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(20), dp(16), dp(16));
        GradientDrawable background = shape(surface, 32);
        card.setBackground(background);
        card.setElevation(dp(12));
        LayoutParams cardParams = new LayoutParams(-1, -2, Gravity.CENTER);
        cardParams.setMargins(dp(20), dp(20), dp(20), dp(20));
        addView(card, cardParams);
        TextView title = new TextView(context);
        title.setText("Ready to attach?");
        title.setTextSize(24);
        title.setTextColor(ink);
        title.setPadding(dp(8), 0, dp(8), dp(6));
        card.addView(title);
        TextView hint = new TextView(context);
        hint.setText("Review your photo before sharing with Gemini.");
        hint.setTextSize(14);
        hint.setTextColor(ink);
        hint.setAlpha(.75f);
        hint.setPadding(dp(8), 0, dp(8), dp(16));
        card.addView(hint);
        View imageSlot = new View(context);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        card.addView(imageSlot, new LinearLayout.LayoutParams(-1, Math.max(dp(140), Math.min(dp(340), screenHeight / 2))));
        actions = new LinearLayout(context);
        actions.setPadding(0, dp(16), 0, 0);
        Button again = button("Retake", false, ink, accent, surface);
        Button use = button("Use photo", true, ink, accent, surface);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, dp(52), 1);
        left.setMargins(0, 0, dp(6), 0);
        actions.addView(again, left);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, dp(52), 1);
        right.setMargins(dp(6), 0, 0, 0);
        actions.addView(use, right);
        card.addView(actions);
        image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription("Captured photo");
        imageShape = shape(Color.BLACK, 24);
        image.setBackground(imageShape);
        image.setClipToOutline(true);
        image.setElevation(dp(13));
        addView(image, new LayoutParams(1, 1));
        try {
            bitmap = decode(file);
            if (bitmap == null) throw new IllegalArgumentException("Cannot decode photo");
            image.setImageBitmap(bitmap);
        } catch (Exception e) {
            dispose();
            throw new IllegalArgumentException("Cannot display captured photo", e);
        }
        again.setOnClickListener(v -> reverse());
        use.setOnClickListener(v -> { if (!moving && !disposed) { moving = true; onUse.run(); } });
        card.setAlpha(0);
        scrim.setAlpha(0);
        post(() -> {
            if (disposed) return;
            int[] slotLocation = new int[2], location = new int[2];
            imageSlot.getLocationOnScreen(slotLocation);
            getLocationOnScreen(location);
            target = new RectF(slotLocation[0] - location[0], slotLocation[1] - location[1], slotLocation[0] - location[0] + imageSlot.getWidth(), slotLocation[1] - location[1] + imageSlot.getHeight());
            if (source.width() <= 0 || source.height() <= 0) source.set((int)target.left, (int)target.top, (int)target.right, (int)target.bottom);
            animateTo(animate ? 0 : 1, 1, null);
            requestFocus();
            announceForAccessibility("Photo ready. Retake or use photo.");
        });
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int radius) { GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(radius)); return bg; }
    private Button button(String text, boolean primary, int ink, int accent, int surface) {
        Button b = new Button(getContext()); b.setText(text); b.setAllCaps(false); b.setTextSize(15);
        boolean dark = (getResources().getConfiguration().uiMode & 48) == 32;
        b.setTextColor(primary ? (dark ? 0xFF142347 : Color.WHITE) : ink);
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33666666), shape(primary ? accent : (dark ? 0xFF30394A : 0xFFE0E8F8), 28), null));
        return b;
    }
    private void position(float fraction) {
        float l = source.left + (target.left - source.left) * fraction;
        float t = source.top + (target.top - source.top) * fraction;
        float w = source.width() + (target.width() - source.width()) * fraction;
        float h = source.height() + (target.height() - source.height()) * fraction;
        LayoutParams lp = (LayoutParams) image.getLayoutParams();
        lp.width = Math.max(1, Math.round(w)); lp.height = Math.max(1, Math.round(h));
        lp.leftMargin = Math.round(l); lp.topMargin = Math.round(t); image.setLayoutParams(lp);
        imageShape.setCornerRadius(dp(28) + fraction * dp(0));
        card.setAlpha(fraction); scrim.setAlpha(fraction);
        card.setScaleX(.94f + .06f * fraction); card.setScaleY(.94f + .06f * fraction);
    }
    private void animateTo(float from, float to, Runnable end) {
        moving = true;
        if (!ValueAnimator.areAnimatorsEnabled() || from == to) { position(to); moving = false; if (end != null) end.run(); return; }
        transition = ValueAnimator.ofFloat(from, to); transition.setDuration(350);
        transition.setInterpolator(new PathInterpolator(.2f, 0, 0, 1));
        transition.addUpdateListener(a -> position((float)a.getAnimatedValue()));
        transition.addListener(new AnimatorListenerAdapter() { @Override public void onAnimationEnd(Animator a) { if (!disposed) { moving = false; if (end != null) end.run(); } } });
        transition.start();
    }
    public void reverse() { if (!moving && !disposed && target != null) animateTo(1, 0, retake); }
    public void dispose() {
        disposed = true;
        if (transition != null) { transition.removeAllListeners(); transition.cancel(); }
        image.setImageDrawable(null);
        if (bitmap != null) { bitmap.recycle(); bitmap = null; }
    }
    private static Bitmap decode(File file) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), options); options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1600) options.inSampleSize *= 2;
        options.inJustDecodeBounds = false; Bitmap raw = BitmapFactory.decodeFile(file.getPath(), options);
        if (raw == null) return null;
        int orientation = new ExifInterface(file.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Matrix m = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90: m.postRotate(90); break;
            case ExifInterface.ORIENTATION_ROTATE_180: m.postRotate(180); break;
            case ExifInterface.ORIENTATION_ROTATE_270: m.postRotate(270); break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: m.setScale(-1, 1); break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL: m.setScale(1, -1); break;
            case ExifInterface.ORIENTATION_TRANSPOSE: m.setScale(-1, 1); m.postRotate(90); break;
            case ExifInterface.ORIENTATION_TRANSVERSE: m.setScale(-1, 1); m.postRotate(270); break;
            default: return raw;
        }
        Bitmap rotated = Bitmap.createBitmap(raw, 0, 0, raw.getWidth(), raw.getHeight(), m, true);
        if (rotated != raw) raw.recycle();
        return rotated;
    }
}
