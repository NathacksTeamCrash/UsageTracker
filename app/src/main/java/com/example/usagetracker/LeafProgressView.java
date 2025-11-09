package com.example.usagetracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class LeafProgressView extends View {
    private Paint leafPaint;
    private Paint fillPaint;
    private Paint strokePaint;
    private float progress = 0; // 0-100

    public LeafProgressView(Context context) {
        super(context);
        init();
    }

    public LeafProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LeafProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        leafPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        leafPaint.setColor(Color.parseColor("#4CAF50")); // Green
        leafPaint.setStyle(Paint.Style.FILL);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor("#81C784")); // Light green
        fillPaint.setStyle(Paint.Style.FILL);

        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.parseColor("#2E7D32")); // Dark green
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(4);
    }

    public void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(100, progress));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = Math.min(width, height) / 2.5f;

        // Draw leaf shape
        Path leafPath = createLeafPath(centerX, centerY, radius);
        
        // Draw unfilled part (light green)
        canvas.drawPath(leafPath, fillPaint);
        
        // Draw filled part based on progress (dark green)
        if (progress > 0) {
            Path filledPath = createFilledLeafPath(centerX, centerY, radius, progress);
            canvas.drawPath(filledPath, leafPaint);
        }
        
        // Draw outline
        canvas.drawPath(leafPath, strokePaint);
    }

    private Path createLeafPath(float centerX, float centerY, float radius) {
        Path path = new Path();
        
        // Create a leaf-like shape (simplified)
        path.moveTo(centerX, centerY - radius);
        
        // Left side curve
        path.cubicTo(
            centerX - radius * 0.3f, centerY - radius * 0.5f,
            centerX - radius * 0.5f, centerY,
            centerX - radius * 0.2f, centerY + radius * 0.3f
        );
        
        // Bottom point
        path.lineTo(centerX, centerY + radius);
        
        // Right side curve
        path.cubicTo(
            centerX + radius * 0.2f, centerY + radius * 0.3f,
            centerX + radius * 0.5f, centerY,
            centerX + radius * 0.3f, centerY - radius * 0.5f
        );
        
        path.close();
        return path;
    }

    private Path createFilledLeafPath(float centerX, float centerY, float radius, float progress) {
        Path path = new Path();
        
        // Calculate fill from bottom to top based on progress
        float totalHeight = radius * 2;
        float fillHeight = totalHeight * (progress / 100f);
        float fillBottom = centerY + radius;
        float fillTop = fillBottom - fillHeight;
        
        // Create filled portion from bottom up
        if (fillTop <= centerY - radius) {
            // Fully filled - use full leaf path
            return createLeafPath(centerX, centerY, radius);
        }
        
        // Partially filled - create bottom portion
        path.moveTo(centerX, fillTop);
        
        // Calculate width at fillTop level
        float yFromCenter = centerY - fillTop;
        float normalizedY = yFromCenter / radius; // -1 to 1
        
        // Approximate leaf width at this height
        float widthFactor = 1.0f - Math.abs(normalizedY) * 0.6f;
        float width = radius * 0.4f * widthFactor;
        
        // Left curve point
        path.cubicTo(
            centerX - width * 0.5f, fillTop + fillHeight * 0.3f,
            centerX - width * 0.3f, fillTop + fillHeight * 0.6f,
            centerX - width * 0.2f, fillBottom
        );
        
        // Bottom point
        path.lineTo(centerX, fillBottom);
        
        // Right curve point
        path.cubicTo(
            centerX + width * 0.2f, fillBottom,
            centerX + width * 0.3f, fillTop + fillHeight * 0.6f,
            centerX + width * 0.5f, fillTop + fillHeight * 0.3f
        );
        
        path.close();
        return path;
    }
}

