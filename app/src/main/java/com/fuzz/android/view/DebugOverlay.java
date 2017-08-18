package com.fuzz.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Johan on 2017-08-06.
 */

public class DebugOverlay extends View {
    Paint p1 = new Paint(Color.BLUE);
    Paint p2 = new Paint(Color.RED);
    public DebugOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        final android.os.Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                invalidate();
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (ArticlesView.articleInfoBtnBounds != null){
            canvas.drawRect(ArticlesView.articleInfoBtnBounds, p1);
            canvas.drawRect(ArticlesView.shoppingCartBtnBounds, p1);
            canvas.drawRect(ArticlesView.draggableArticleBounds, p2);
        }
    }
}
