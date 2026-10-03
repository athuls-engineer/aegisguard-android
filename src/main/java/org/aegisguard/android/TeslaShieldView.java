package org.aegisguard.android;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * TeslaShieldView — Ultra-premium, hardware-accelerated Tesla-styled interactive visual center.
 * Features:
 * 1. Concentric Tesla speedometer/sentry tick dial
 * 2. Rotating continuous energy arc (electric cyan / emerald)
 * 3. Breathing outer aura / radar pulse
 * 4. Multi-faceted stealth titanium shield geometry
 * 5. Tactile spring physics compression on touch
 */
public class TeslaShieldView extends View {

    private boolean isProtected = false;
    private float rotationAngle = 0f;
    private float pulseAlpha = 0.3f;
    private float pulseRadiusScale = 1.0f;
    private float touchScale = 1.0f;

    private ValueAnimator rotationAnimator;
    private ValueAnimator pulseAnimator;

    private Paint tickPaint;
    private Paint arcPaint;
    private Paint pulsePaint;
    private Paint shieldFillPaint;
    private Paint shieldStrokePaint;
    private Paint shieldCorePaint;
    private Paint glowPaint;
    private Paint textPaint;
    private Paint subtextPaint;

    private final Path shieldPath = new Path();
    private final Path innerShieldPath = new Path();
    private final RectF arcRect = new RectF();
    private final RectF centerRect = new RectF();

    private OnShieldClickListener clickListener;

    public interface OnShieldClickListener {
        void onShieldClick();
    }

    public TeslaShieldView(Context context) {
        super(context);
        init();
    }

    public TeslaShieldView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TeslaShieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);

        tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setStrokeCap(Paint.Cap.ROUND);

        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pulsePaint.setStyle(Paint.Style.STROKE);

        shieldFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shieldFillPaint.setStyle(Paint.Style.FILL);

        shieldStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shieldStrokePaint.setStyle(Paint.Style.STROKE);

        shieldCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shieldCorePaint.setStyle(Paint.Style.FILL);

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        subtextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtextPaint.setTextAlign(Paint.Align.CENTER);

        // Continuous Arc Rotation Animator
        rotationAnimator = ValueAnimator.ofFloat(0f, 360f);
        rotationAnimator.setDuration(3000);
        rotationAnimator.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimator.setInterpolator(new LinearInterpolator());
        rotationAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (isProtected) {
                    rotationAngle = (float) animation.getAnimatedValue();
                    invalidate();
                }
            }
        });

        // Breathing Pulse Halo Animator
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(2200);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (isProtected) {
                    float val = (float) animation.getAnimatedValue();
                    pulseAlpha = 0.15f + (val * 0.45f);
                    pulseRadiusScale = 1.0f + (val * 0.08f);
                    invalidate();
                }
            }
        });
    }

    public void setOnShieldClickListener(OnShieldClickListener listener) {
        this.clickListener = listener;
    }

    public void setShieldState(boolean active) {
        this.isProtected = active;
        if (active) {
            if (!rotationAnimator.isStarted()) rotationAnimator.start();
            else rotationAnimator.resume();
            if (!pulseAnimator.isStarted()) pulseAnimator.start();
            else pulseAnimator.resume();
        } else {
            rotationAnimator.pause();
            pulseAnimator.pause();
            rotationAngle = 0f;
            pulseAlpha = 0.1f;
            pulseRadiusScale = 1.0f;
        }
        invalidate();
    }

    public boolean isShieldActive() {
        return isProtected;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredSize = (int) (240 * getResources().getDisplayMetrics().density);
        int width = resolveSize(desiredSize, widthMeasureSpec);
        int height = resolveSize(desiredSize, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float baseRadius = (Math.min(w, h) / 2f) * 0.88f;

        canvas.save();
        // Tactile Spring Scale
        canvas.scale(touchScale, touchScale, cx, cy);

        // 1. Outer Pulse Aura (Tesla Sentry Halo)
        if (isProtected) {
            pulsePaint.setStrokeWidth(3f * getResources().getDisplayMetrics().density);
            pulsePaint.setColor(Color.argb((int) (pulseAlpha * 255), 0, 229, 255));
            float outerR = baseRadius * pulseRadiusScale;
            canvas.drawCircle(cx, cy, outerR, pulsePaint);

            // Secondary subtle echo ring
            pulsePaint.setStrokeWidth(1f * getResources().getDisplayMetrics().density);
            pulsePaint.setColor(Color.argb((int) (pulseAlpha * 120), 16, 185, 129));
            canvas.drawCircle(cx, cy, outerR * 1.06f, pulsePaint);
        }

        // 2. Tesla Radial Speedometer / Sentry Dial Ticks (48 Segments)
        int tickCount = 48;
        float tickOuterR = baseRadius * 0.94f;
        float tickInnerR = baseRadius * 0.87f;

        for (int i = 0; i < tickCount; i++) {
            float angle = (float) (i * (360.0 / tickCount));
            double rad = Math.toRadians(angle);
            float x1 = (float) (cx + Math.cos(rad) * tickInnerR);
            float y1 = (float) (cy + Math.sin(rad) * tickInnerR);
            float x2 = (float) (cx + Math.cos(rad) * tickOuterR);
            float y2 = (float) (cy + Math.sin(rad) * tickOuterR);

            boolean isMajor = (i % 6 == 0);
            if (isProtected) {
                if (isMajor) {
                    tickPaint.setColor(Color.parseColor("#00E5FF")); // Electric cyan
                    tickPaint.setStrokeWidth(3.5f);
                } else {
                    tickPaint.setColor(Color.parseColor("#1E3A5F"));
                    tickPaint.setStrokeWidth(1.8f);
                }
            } else {
                tickPaint.setColor(isMajor ? Color.parseColor("#334155") : Color.parseColor("#1E293B"));
                tickPaint.setStrokeWidth(isMajor ? 3.0f : 1.5f);
            }
            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }

        // 3. Rotating Energy Telemetry Arc (Sweep Gradient)
        float arcRadius = baseRadius * 0.78f;
        arcRect.set(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius);
        float strokeW = 4.5f * getResources().getDisplayMetrics().density;
        arcPaint.setStrokeWidth(strokeW);

        if (isProtected) {
            canvas.save();
            canvas.rotate(rotationAngle, cx, cy);

            int[] sweepColors = new int[] {
                Color.TRANSPARENT,
                Color.parseColor("#00E5FF"),
                Color.parseColor("#10B981"),
                Color.TRANSPARENT
            };
            float[] sweepPositions = new float[] { 0.0f, 0.45f, 0.85f, 1.0f };
            SweepGradient sweep = new SweepGradient(cx, cy, sweepColors, sweepPositions);
            arcPaint.setShader(sweep);
            canvas.drawCircle(cx, cy, arcRadius, arcPaint);
            arcPaint.setShader(null);
            canvas.restore();
        } else {
            arcPaint.setColor(Color.parseColor("#1A202C"));
            canvas.drawCircle(cx, cy, arcRadius, arcPaint);
        }

        // 4. Center Stealth Titanium Shield Housing
        float centerR = baseRadius * 0.68f;
        centerRect.set(cx - centerR, cy - centerR, cx + centerR, cy + centerR);

        // Circular background disc with subtle obsidian drop
        int discTop = isProtected ? Color.parseColor("#0F172A") : Color.parseColor("#0B0F19");
        int discBottom = isProtected ? Color.parseColor("#020617") : Color.parseColor("#05070D");
        shieldFillPaint.setShader(new LinearGradient(cx, cy - centerR, cx, cy + centerR, discTop, discBottom, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, centerR, shieldFillPaint);
        shieldFillPaint.setShader(null);

        // Disc boundary highlight
        shieldStrokePaint.setStrokeWidth(1.8f * getResources().getDisplayMetrics().density);
        shieldStrokePaint.setColor(isProtected ? Color.parseColor("#1E293B") : Color.parseColor("#131B2E"));
        canvas.drawCircle(cx, cy, centerR, shieldStrokePaint);

        // 5. Faceted Stealth Geometry Emblem (Center Iconic Shield)
        float shieldScale = centerR * 0.52f;
        buildShieldPath(cx, cy - (shieldScale * 0.22f), shieldScale, shieldPath);

        if (isProtected) {
            // Neon cyan shield with emerald energy core
            shieldFillPaint.setShader(new LinearGradient(
                cx, cy - shieldScale, cx, cy + shieldScale,
                Color.parseColor("#00E5FF"), Color.parseColor("#059669"),
                Shader.TileMode.CLAMP
            ));
            canvas.drawPath(shieldPath, shieldFillPaint);
            shieldFillPaint.setShader(null);

            // Inner checkmark / core emblem
            buildInnerCorePath(cx, cy - (shieldScale * 0.22f), shieldScale * 0.55f, innerShieldPath);
            shieldCorePaint.setColor(Color.parseColor("#020617"));
            canvas.drawPath(innerShieldPath, shieldCorePaint);

            // Glowing boundary line
            glowPaint.setStrokeWidth(2.5f);
            glowPaint.setColor(Color.parseColor("#FFFFFF"));
            canvas.drawPath(shieldPath, glowPaint);
        } else {
            // Standby stealth matte dark slate
            shieldFillPaint.setColor(Color.parseColor("#1E293B"));
            canvas.drawPath(shieldPath, shieldFillPaint);

            buildInnerCorePath(cx, cy - (shieldScale * 0.22f), shieldScale * 0.55f, innerShieldPath);
            shieldCorePaint.setColor(Color.parseColor("#0F172A"));
            canvas.drawPath(innerShieldPath, shieldCorePaint);

            glowPaint.setStrokeWidth(2f);
            glowPaint.setColor(Color.parseColor("#334155"));
            canvas.drawPath(shieldPath, glowPaint);
        }

        // 6. Integrated Digital Status Indicator Text
        float dp = getResources().getDisplayMetrics().density;
        textPaint.setTextSize(11f * dp);
        textPaint.setLetterSpacing(0.14f);

        subtextPaint.setTextSize(8.5f * dp);
        subtextPaint.setLetterSpacing(0.18f);

        float textY = cy + (centerR * 0.55f);
        if (isProtected) {
            textPaint.setColor(Color.parseColor("#FFFFFF"));
            canvas.drawText("SENTRY ACTIVE", cx, textY, textPaint);

            subtextPaint.setColor(Color.parseColor("#00E5FF"));
            canvas.drawText("HARDWARE TUN // 0% DRAIN", cx, textY + (14f * dp), subtextPaint);
        } else {
            textPaint.setColor(Color.parseColor("#64748B"));
            canvas.drawText("STANDBY", cx, textY, textPaint);

            subtextPaint.setColor(Color.parseColor("#475569"));
            canvas.drawText("TAP TO ENGAGE", cx, textY + (14f * dp), subtextPaint);
        }

        canvas.restore();
    }

    private void buildShieldPath(float cx, float cy, float r, Path path) {
        path.reset();
        // Modern hexagonal faceted crest
        path.moveTo(cx, cy - r);
        path.lineTo(cx + (r * 0.85f), cy - (r * 0.45f));
        path.lineTo(cx + (r * 0.85f), cy + (r * 0.25f));
        path.lineTo(cx, cy + r);
        path.lineTo(cx - (r * 0.85f), cy + (r * 0.25f));
        path.lineTo(cx - (r * 0.85f), cy - (r * 0.45f));
        path.close();
    }

    private void buildInnerCorePath(float cx, float cy, float r, Path path) {
        path.reset();
        path.moveTo(cx, cy - (r * 0.8f));
        path.lineTo(cx + (r * 0.65f), cy - (r * 0.35f));
        path.lineTo(cx + (r * 0.65f), cy + (r * 0.2f));
        path.lineTo(cx, cy + (r * 0.8f));
        path.lineTo(cx - (r * 0.65f), cy + (r * 0.2f));
        path.lineTo(cx - (r * 0.65f), cy - (r * 0.35f));
        path.close();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchScale = 0.93f;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                touchScale = 1.0f;
                invalidate();
                if (clickListener != null) {
                    clickListener.onShieldClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                touchScale = 1.0f;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
