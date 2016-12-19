/*
 * Copyright (C) 2016 CaMnter yuanyu.camnter@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.camnter.easycountdowntextureview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.TextureView;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.camnter.easycountdowntextureview.EasyCountDownTextureView.MainHandler.WHAT_COUNT_DOWN_COMPLETED;

/**
 * Description：EasyCountDownTextureView
 * Created by：CaMnter
 * Time：2016-03-16 13:45
 */
public class EasyCountDownTextureView extends TextureView
    implements TextureView.SurfaceTextureListener {

    private static final String TAG = EasyCountDownTextureView.class.getSimpleName();

    private static final String LESS_THAN_TEN_FORMAT = "%02d";
    private static final String COLON = ":";

    private static final int DEFAULT_COLOR_BACKGROUND = Color.BLACK;
    private static final int DEFAULT_COLOR_COLON = Color.BLACK;
    private static final int DEFAULT_COLOR_TIME = Color.WHITE;
    private static final int DEFAULT_COLOR_RECT_BORDER = Color.BLACK;
    private DisplayMetrics mMetrics;

    private static final int COUNT_DOWN_INTERVAL = 1000;

    private static final long ONE_HOUR = 1000 * 60 * 60L;
    private static final long ONE_MINUTE = 1000 * 60L;
    private static final long ONE_SECOND = 1000L;

    private volatile long mMillisInFuture = 0L;

    /**************
     * Default dp *
     **************/
    private static final float DEFAULT_BACKGROUND_PAINT_WIDTH = 0.01f;
    private static final float DEFAULT_COLON_PAINT_WIDTH = 0.66f;
    private static final float DEFAULT_TIME_PAINT_WIDTH = 0.77f;
    private static final float DEFAULT_ROUND_RECT_RADIUS = 2.66f;
    private static final float DEFAULT_RECT_WIDTH = 18.0f;
    private static final float DEFAULT_RECT_HEIGHT = 17.0f;
    private static final float DEFAULT_RECT_SPACING = 6.0f;
    private static final float DEFAULT_TIME_TEXT_SIZE = 13.0f;
    private static final float DEFAULT_COLON_TEXT_SIZE = 13.0f;

    // 66dp
    private static final float DEFAULT_VIEW_WIDTH = DEFAULT_RECT_WIDTH * 3 +
        DEFAULT_RECT_SPACING * 2;
    // 17dp
    private static final float DEFAULT_VIEW_HEIGHT = DEFAULT_RECT_HEIGHT;

    /**************
     * Default px *
     **************/
    private float rectWidth;
    private float rectHeight;
    private float rectSpacing;
    private float rectRadius;
    private boolean drawRectBorder = false;

    private float paddingLeft;
    private float paddingTop;
    private float paddingRight;
    private float paddingBottom;

    private float firstTranslateX;
    private float firstTranslateColonX;
    private float secondTranslateX;
    private float secondTranslateColonX;

    private int timeHour;
    private int timeMinute;
    private int timeSecond;

    private int viewWidth;
    private int viewHeight;
    private float defaultWrapContentWidth;
    private float defaultWrapContentHeight;

    private EasyThread mThread;

    private final Locale locale = Locale.getDefault();
    private final Calendar mCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));

    private Paint colonPaint;
    private Paint rectBorderPaint;

    private Paint timePaint;
    private float timePaintBaseLine;
    private float timePaintBaseLineFixed;

    private Paint backgroundPaint;
    private RectF backgroundRectF;

    private volatile long lastRecordTime = 0L;
    private volatile boolean runningState = false;

    private boolean autoResume = true;
    private long pauseTime = 0L;

    private EasyCountDownListener mEasyCountDownListener;


    static class MainHandler extends Handler {

        static final int WHAT_COUNT_DOWN_COMPLETED = 0x26;

        private final WeakReference<EasyCountDownListener> listenerReference;


        MainHandler(@NonNull EasyCountDownListener easyCountDownListener) {
            super(Looper.getMainLooper());
            this.listenerReference = new WeakReference<>(easyCountDownListener);
        }


        /**
         * Handle system messages here.
         */
        @Override public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case WHAT_COUNT_DOWN_COMPLETED:
                    final EasyCountDownListener easyCountDownListener
                        = this.listenerReference.get();
                    if (easyCountDownListener == null) return;
                    easyCountDownListener.onCountDownCompleted();
                    break;
            }
        }

    }


    private MainHandler mainHandler;


    public EasyCountDownTextureView(Context context) {
        super(context);
        this.init(context, null);
    }


    public EasyCountDownTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.init(context, attrs);
    }


    public EasyCountDownTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context, attrs);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EasyCountDownTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.init(context, attrs);
    }


    private void init(Context context, AttributeSet attrs) {
        this.mMetrics = this.getResources().getDisplayMetrics();
        this.defaultWrapContentWidth = this.dp2px(DEFAULT_VIEW_WIDTH);
        this.defaultWrapContentHeight = this.dp2px(DEFAULT_VIEW_HEIGHT);

        this.setSurfaceTextureListener(this);
        this.setOpaque(false);

        TypedArray typedArray = context.obtainStyledAttributes(attrs,
            R.styleable.EasyCountDownTextureView);
        this.timeHour = typedArray.getInteger(R.styleable.EasyCountDownTextureView_easyCountHour,
            0);
        this.timeMinute = typedArray.getInteger(
            R.styleable.EasyCountDownTextureView_easyCountMinute, 0);
        this.timeSecond = typedArray.getInteger(
            R.styleable.EasyCountDownTextureView_easyCountSecond, 0);

        this.initTimePaint(typedArray);
        this.initColonPaint(typedArray);
        this.initRectBorderPaint(typedArray);
        this.initBackgroundPaint(typedArray);

        this.rectWidth = typedArray.getDimension(
            R.styleable.EasyCountDownTextureView_easyCountRectWidth,
            this.dp2px(DEFAULT_RECT_WIDTH));
        this.rectHeight = typedArray.getDimension(
            R.styleable.EasyCountDownTextureView_easyCountRectHeight,
            this.dp2px(DEFAULT_RECT_HEIGHT));
        this.rectSpacing = typedArray.getDimension(
            R.styleable.EasyCountDownTextureView_easyCountRectSpacing,
            this.dp2px(DEFAULT_RECT_SPACING));
        this.refitBackgroundAttribute();

        final Paint.FontMetricsInt timePaintFontMetrics = this.timePaint.getFontMetricsInt();
        this.timePaintBaseLine = (this.backgroundRectF.bottom + this.backgroundRectF.top -
            timePaintFontMetrics.bottom - timePaintFontMetrics.top) / 2;
        // for colon
        this.timePaintBaseLineFixed = this.timePaintBaseLine / 40 * 37;
        this.rectRadius = typedArray.getDimension(
            R.styleable.EasyCountDownTextureView_easyCountRectRadius,
            this.dp2px(DEFAULT_ROUND_RECT_RADIUS));
        typedArray.recycle();

        this.updateTime();
    }


    private void initColonPaint(@NonNull final TypedArray typedArray) {
        this.colonPaint = new Paint();
        this.colonPaint.setAntiAlias(true);
        this.colonPaint.setColor(
            typedArray.getColor(R.styleable.EasyCountDownTextureView_easyCountColonColor,
                DEFAULT_COLOR_COLON));
        this.colonPaint.setTextSize(
            typedArray.getDimension(R.styleable.EasyCountDownTextureView_easyCountColonSize,
                this.dp2px(DEFAULT_TIME_TEXT_SIZE)));
        this.colonPaint.setStrokeWidth(this.dp2px(DEFAULT_COLON_PAINT_WIDTH));
        this.colonPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.colonPaint.setTextAlign(Paint.Align.CENTER);
        this.colonPaint.setStrokeCap(Paint.Cap.ROUND);
    }


    private void initTimePaint(@NonNull final TypedArray typedArray) {
        this.timePaint = new Paint();
        this.timePaint.setAntiAlias(true);
        this.timePaint.setColor(
            typedArray.getColor(R.styleable.EasyCountDownTextureView_easyCountTimeColor,
                DEFAULT_COLOR_TIME));
        this.timePaint.setTextSize(
            typedArray.getDimension(R.styleable.EasyCountDownTextureView_easyCountTimeSize,
                this.dp2px(DEFAULT_COLON_TEXT_SIZE)));
        this.timePaint.setStrokeWidth(this.dp2px(DEFAULT_TIME_PAINT_WIDTH));
        this.timePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.timePaint.setTextAlign(Paint.Align.CENTER);
        this.timePaint.setStrokeCap(Paint.Cap.ROUND);
    }


    private void initRectBorderPaint(@NonNull final TypedArray typedArray) {
        final float rectBorderSize = typedArray.getDimension(
            R.styleable.EasyCountDownTextureView_easyCountRectBorderSize, Float.MIN_VALUE);
        this.checkRectBorder(rectBorderSize);
        if (!this.drawRectBorder) return;
        this.rectBorderPaint = new Paint();
        this.rectBorderPaint.setAntiAlias(true);
        this.rectBorderPaint.setColor(
            typedArray.getColor(R.styleable.EasyCountDownTextureView_easyCountRectBorderColor,
                DEFAULT_COLOR_RECT_BORDER));
        this.rectBorderPaint.setTextSize(rectBorderSize);
        this.rectBorderPaint.setStyle(Paint.Style.STROKE);
        this.rectBorderPaint.setTextAlign(Paint.Align.CENTER);
        this.rectBorderPaint.setStrokeCap(Paint.Cap.ROUND);
    }


    private void initBackgroundPaint(@NonNull final TypedArray typedArray) {
        this.backgroundPaint = new Paint();
        this.backgroundPaint.setAntiAlias(true);
        this.backgroundPaint.setColor(
            typedArray.getColor(R.styleable.EasyCountDownTextureView_easyCountBackgroundColor,
                DEFAULT_COLOR_BACKGROUND));
        this.backgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.backgroundPaint.setStrokeWidth(this.dp2px(DEFAULT_BACKGROUND_PAINT_WIDTH));
        this.backgroundPaint.setTextAlign(Paint.Align.CENTER);
        this.backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
    }


    private void checkRectBorder(final float rectBorder) {
        this.drawRectBorder = rectBorder != Float.MIN_VALUE;
    }


    private void updateTime() {
        this.mMillisInFuture = this.timeHour * ONE_HOUR + this.timeMinute * ONE_MINUTE +
            this.timeSecond * ONE_SECOND;
        this.setTime(this.mMillisInFuture);
    }


    private void refitBackgroundAttribute() {
        this.paddingLeft = this.getPaddingLeft();
        this.paddingTop = this.getPaddingTop();
        this.paddingRight = this.getPaddingRight();
        this.paddingBottom = this.getPaddingBottom();

        this.firstTranslateX = this.rectWidth + this.rectSpacing + paddingLeft;
        this.secondTranslateX = this.rectWidth * 2 + this.rectSpacing * 2 + paddingLeft;
        this.firstTranslateColonX = this.firstTranslateX - this.rectSpacing / 2;
        this.secondTranslateColonX = this.secondTranslateX - this.rectSpacing / 2;

        this.backgroundRectF = new RectF(0, 0, this.rectWidth, this.rectHeight);
    }


    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        this.viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        this.viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        float resultWidth;
        float resultHeight;

        switch (widthMode) {
            // wrap_content
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                resultWidth = this.defaultWrapContentWidth;
                break;
            // match_parent
            case MeasureSpec.EXACTLY:
            default:
                resultWidth = Math.max(this.viewWidth, this.defaultWrapContentWidth);
                break;
        }
        switch (heightMode) {
            // wrap_content
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                resultHeight = this.defaultWrapContentHeight;
                break;
            // match_parent
            case MeasureSpec.EXACTLY:
            default:
                resultHeight = Math.max(this.viewHeight, this.defaultWrapContentHeight);
                break;
        }
        resultWidth += (this.paddingLeft + this.paddingRight);
        resultHeight += (this.paddingTop + this.paddingBottom);
        this.setMeasuredDimension((int) resultWidth, (int) resultHeight);
    }


    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        this.viewWidth = width;
        this.viewHeight = height;
        this.refitBackgroundAttribute();
        this.invalidate();
    }


    public void setTimeHour(int timeHour) {
        this.timeHour = timeHour;
        this.updateTime();
    }


    public void setTimeMinute(int timeMinute) {
        this.timeMinute = timeMinute;
        this.updateTime();
    }


    public void setTimeSecond(int timeSecond) {
        this.timeSecond = timeSecond;
        this.updateTime();
    }


    public void setRectWidth(float rectWidthDp) {
        this.rectWidth = this.dp2px(rectWidthDp);
        this.refitBackgroundAttribute();
    }


    public void setRectHeight(float rectHeightDp) {
        this.rectHeight = this.dp2px(rectHeightDp);
        this.refitBackgroundAttribute();
    }


    public void setRectSpacing(float rectSpacingDp) {
        this.rectSpacing = this.dp2px(rectSpacingDp);
        this.refitBackgroundAttribute();
    }


    public void setAutoResume(boolean autoResume) {
        this.autoResume = autoResume;
    }


    public void setEasyCountDownListener(EasyCountDownListener easyCountDownListener) {
        this.mEasyCountDownListener = easyCountDownListener;
        this.mainHandler = new MainHandler(easyCountDownListener);
    }


    public float getRectWidth() {
        return rectWidth;
    }


    public float getRectHeight() {
        return rectHeight;
    }


    public float getRectSpacing() {
        return rectSpacing;
    }


    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (this.pauseTime > 0) {
            this.mMillisInFuture += (SystemClock.elapsedRealtime() - this.pauseTime);
            this.pauseTime = 0;
        }
        this.start();
    }


    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }


    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (this.autoResume) {
            this.pauseTime = SystemClock.elapsedRealtime();
        }
        this.stop();
        return true;
    }


    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }


    public void start() {
        if (this.runningState) return;
        if (mMillisInFuture > 0) {
            this.mThread = new EasyThread();
            this.mThread.startThread();
            this.mThread.start();
            this.runningState = true;
            if (this.mEasyCountDownListener != null) {
                this.mEasyCountDownListener.onCountDownStart();
            }
        } else {
            this.drawZeroZeroZero();
            this.runningState = false;
        }
    }


    public void stop() {
        if (!this.runningState) return;
        if (this.mThread != null) {
            this.mThread.interrupt();
            this.mThread = null;
        }
        if (this.mEasyCountDownListener != null) {
            this.mEasyCountDownListener.onCountDownStop(this.mMillisInFuture);
        }
        this.runningState = false;
    }


    /**
     * Start count down by date
     *
     * @param date date
     */
    public void setTime(Date date) {
        this.mMillisInFuture = date.getTime();
    }


    /**
     * Start count down by timeMillis
     *
     * @param timeMillis timeMillis
     */
    public void setTime(final long timeMillis) {
        this.mMillisInFuture = timeMillis;
        this.mCalendar.setTimeInMillis(this.mMillisInFuture);
    }


    private void drawZeroZeroZero() {
        Canvas canvas = null;
        try {
            synchronized (this) {
                canvas = EasyCountDownTextureView.this.lockCanvas();
                if (canvas == null) return;
                this.drawTimeAndBackground(canvas, String.format(locale, LESS_THAN_TEN_FORMAT, 0),
                    String.format(locale, LESS_THAN_TEN_FORMAT, 0),
                    String.format(locale, LESS_THAN_TEN_FORMAT, 0));
                unlockCanvasAndPost(canvas);
            }
        } catch (Exception e) {
            e.printStackTrace();
            unlockCanvasAndPost(canvas);
        }
    }


    private class EasyThread extends Thread {

        private volatile boolean running = false;
        private volatile boolean completed = false;


        EasyThread() {
            this.running = true;
        }


        final void startThread() {
            synchronized (this) {
                this.completed = false;
                this.running = true;
            }
        }


        final void stopThread() {
            synchronized (this) {
                this.completed = true;
                this.running = false;
            }
        }


        @Override public void run() {
            while (!this.completed) {
                while (this.running) {
                    Canvas canvas = null;
                    try {
                        synchronized (this) {
                            canvas = EasyCountDownTextureView.this.lockCanvas();
                            if (canvas == null) continue;
                            timeHour = mCalendar.get(Calendar.HOUR_OF_DAY);
                            timeMinute = mCalendar.get(Calendar.MINUTE);
                            timeSecond = mCalendar.get(Calendar.SECOND);
                            drawTimeAndBackground(canvas,
                                String.format(locale, LESS_THAN_TEN_FORMAT, timeHour),
                                String.format(locale, LESS_THAN_TEN_FORMAT, timeMinute),
                                String.format(locale, LESS_THAN_TEN_FORMAT, timeSecond));
                            // refresh time
                            mMillisInFuture -= 1000;
                            mCalendar.setTimeInMillis(mMillisInFuture);

                            if (mMillisInFuture < 0) {
                                this.completed = true;
                                this.running = false;
                                if (mainHandler != null) {
                                    mainHandler.sendEmptyMessageDelayed(WHAT_COUNT_DOWN_COMPLETED,
                                        1000);
                                }
                            }

                            final long pastTime = SystemClock.uptimeMillis() - lastRecordTime;
                            if (pastTime < COUNT_DOWN_INTERVAL) {
                                this.wait(COUNT_DOWN_INTERVAL - pastTime);
                            }
                            lastRecordTime = SystemClock.uptimeMillis();
                        }
                    } catch (InterruptedException interruptedException) {
                        Log.i(TAG, "[run]\t\t\t thread interrupted", interruptedException);
                        this.stopThread();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            unlockCanvasAndPost(canvas);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }


    private void drawTimeAndBackground(@NonNull final Canvas canvas,
                                       @NonNull final String hour,
                                       @NonNull final String minute,
                                       @NonNull final String second) {
        // background
        canvas.save();
        canvas.translate(paddingLeft, paddingTop);
        canvas.drawRoundRect(backgroundRectF, rectRadius, rectRadius, backgroundPaint);
        // border
        this.drawRectBorder(canvas, backgroundRectF, rectRadius, rectBorderPaint);
        canvas.drawText(hour, backgroundRectF.centerX(), timePaintBaseLine, timePaint);
        canvas.restore();

        // colon
        canvas.save();
        canvas.translate(firstTranslateColonX, paddingTop);
        canvas.drawText(COLON, 0, timePaintBaseLineFixed, colonPaint);
        canvas.restore();

        // background
        canvas.save();
        canvas.translate(firstTranslateX, paddingTop);
        canvas.drawRoundRect(backgroundRectF, rectRadius, rectRadius, backgroundPaint);
        // border
        this.drawRectBorder(canvas, backgroundRectF, rectRadius, rectBorderPaint);
        canvas.drawText(minute, backgroundRectF.centerX(), timePaintBaseLine, timePaint);
        canvas.restore();

        // colon
        canvas.save();
        canvas.translate(secondTranslateColonX, paddingTop);
        canvas.drawText(COLON, 0, timePaintBaseLineFixed, colonPaint);
        canvas.restore();

        // background
        canvas.save();
        canvas.translate(secondTranslateX, paddingTop);
        canvas.drawRoundRect(backgroundRectF, rectRadius, rectRadius, backgroundPaint);
        // border
        this.drawRectBorder(canvas, backgroundRectF, rectRadius, rectBorderPaint);
        canvas.drawText(second, backgroundRectF.centerX(), timePaintBaseLine, timePaint);
        canvas.restore();
    }


    private void drawRectBorder(@NonNull final Canvas canvas,
                                @NonNull final RectF rect,
                                final float rectRadius,
                                @Nullable Paint paint) {
        if (paint == null) return;
        if (rectRadius > 0) {
            canvas.drawRoundRect(rect, rectRadius, rectRadius, paint);
        } else {
            canvas.drawRect(rect, paint);
        }
    }


    /**
     * Dp to px
     *
     * @param dp dp
     * @return px
     */
    private float dp2px(final float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, this.mMetrics);
    }


    public interface EasyCountDownListener {

        /**
         * When count down start
         */
        void onCountDownStart();

        /**
         * When count down stop
         *
         * @param millisInFuture millisInFuture
         */
        void onCountDownStop(long millisInFuture);

        /**
         * When count down completed
         */
        void onCountDownCompleted();

    }

}
