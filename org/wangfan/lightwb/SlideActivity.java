package org.wangfan.lightwb;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.OverScroller;

/**
 * 
 * 此类实现可以滑动的Activity。
 * <p>
 * 使用方法： </br>
 * <ul>
 * 1.让需要滑动关闭的Activity继承SlideActivity。</br>
 * 2.在AndroidManifest.xml中为Activity设置“窗口透明”的主题（&lt;item
 * name=“android:windowIsTranslucent”&gt;true&lt;/item&gt;）。</br> 3.使用
 * {@link #setLeftShadow(Drawable)}设置阴影。
 * </ul>
 * </p>
 * <p>
 * 另外，覆盖{@link #drawShadow(Canvas, View, Drawable)}方法实现自定义阴影绘制。覆盖
 * {@link #computeSpringbackBoundary(int)}方法自定义回弹界线
 * </p>
 * 
 * @author wangfan </br>wangfansh@foxmail.com
 * 
 */
public class SlideActivity extends Activity {
    /** 如果Fling速度大于此速度，则关闭Activity */
    public static final int MIN_CLOSE_VELOCITY = 1000;
    /** fling触发关闭时的速度 */
    public static final int FLING_CLOSE_VELOCITY = 800;
    /** 滑动回弹时的速度 */
    public static final int SPRINGBACK_VELOCITY = 700;
    /** 滑动时的底层背景RGB颜色 */
    public static final int DEFAULT_BACKGROUND_RGB = 0xFF000000;
    /** 滑动时透明度渐变的起始Alpha值 */
    public static final int DEFAULT_ALPHA = 160;
    private ViewGroup mDecorView;
    private Window mWindow;
    private GestureDetector mGD;
    private OverScroller mScroller;
    private boolean mIsFirstScroll = true;
    private SlideLayout mSlideLayout;
    private Drawable mLeftShadow;
    // 滑动角度判断
    private MoveDetector mMoveDetector;
    @SuppressWarnings("unused")
    private boolean mAllowSlide = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    /**
     * 获取当前左外阴影。
     * 
     * @return 当前左外阴影
     */
    public Drawable getLeftShadow() {
        return mLeftShadow;
    }

    /**
     * 设置需要显示的左外阴影。默认无阴影显示。
     * 
     * @param mLeftShadow
     *            阴影Drawable
     */
    public void setLeftShadow(Drawable mLeftShadow) {
        this.mLeftShadow = mLeftShadow;
    }

    /**
     * 设置需要显示的左外阴影。默认无阴影显示。
     * 
     * @param drawableRes
     *            阴影资源
     */
    public void setLeftShadow(int drawableRes) {
        Drawable leftShadow = this.getResources().getDrawable(drawableRes);
        setLeftShadow(leftShadow);
    }

    // 内置滑动布局类，插入到DecorView和其内容当中。响应滑动手势，执行滑动，绘制阴影。
    class SlideLayout extends FrameLayout {
        public SlideLayout(Context context) {
            super(context);
            init();
        }

        // 初始化。
        // 替换在DecorView和内容当中插入自定义Layout，用来实现外边框阴影。
        private void init() {
            View DecorLayout = mDecorView.getChildAt(0);
            mDecorView.removeView(DecorLayout);
            this.addView(DecorLayout);
            mDecorView.addView(this);
        }

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            boolean superReturn = super.drawChild(canvas, child, drawingTime);
            drawShadow(canvas, child, mLeftShadow);
            return superReturn;
        }

        // 拦截横向滑动。
        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            int moveStatus = mMoveDetector.getMoveStatus(ev);
            if (moveStatus == MoveDetector.HOR) {
                return true;
            }
            return super.onInterceptTouchEvent(ev);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
                if (-mSlideLayout.getScrollX() < computeSpringbackBoundary(mDecorView
                        .getWidth())) {
                    mScroller.startScroll(mSlideLayout.getScrollX(), 0,
                            -mSlideLayout.getScrollX(), 0, SPRINGBACK_VELOCITY);
                } else {
                    mScroller.startScroll(mSlideLayout.getScrollX(), 0,
                            -(mDecorView.getWidth() - Math.abs(mSlideLayout
                                    .getScrollX())), 0, SPRINGBACK_VELOCITY);
                }
                ViewCompat.postOnAnimation(mDecorView, ViewScroller);
                mIsFirstScroll = true;
            }
            mGD.onTouchEvent(ev);
            return true;
        }

        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            int color = computeBackgroundArgb(l, mDecorView.getWidth(),
                    DEFAULT_ALPHA, DEFAULT_BACKGROUND_RGB);
            this.setBackgroundColor(color);
        }
    }

    /**
     * 滑动时计算背景argb颜色。覆盖此方法实现自定义计算。
     * 
     * @param scrollX
     *            当前水平滑动值。
     * @param totalWidth
     *            可以滑动的总宽度。
     * @param maxAlpha
     *            alpha最大值。范围0～255，默认值160。
     * @param baseBgRgb
     *            背景颜色,不透明。默认值0xFF000000;
     * @return 返回合成的argb值。
     */
    protected int computeBackgroundArgb(int scrollX, int totalWidth,
            int maxAlpha, int baseBgRgb) {
        float percent = Math.abs(scrollX) / (float) totalWidth;
        int alpha = (int) ((1 - percent) * maxAlpha);
        return (alpha << 24) | (baseBgRgb & 0x00FFFFFF);
    }

    /**
     * 计算回弹界线。覆盖此方法以实现自己的回弹界限计算方法。 默认的实现是DecorView的一半。
     * 
     * @param DecorViewWidth
     *            DecorView宽度
     * @return 回弹界线
     */
    protected int computeSpringbackBoundary(int DecorViewWidth) {
        return DecorViewWidth / 2;
    }

    /**
     * 绘制阴影。覆盖此方法，可实现自己的绘制。
     * 
     * @param canvas
     * @param child
     *            Activity的内容布局。
     * @param leftShadow
     *            定义的阴影图片。参见：{@link setLeftShadow}
     */
    protected void drawShadow(Canvas canvas, View child, Drawable leftShadow) {
        if (leftShadow != null) {
            leftShadow.setBounds(-leftShadow.getIntrinsicWidth(),
                    child.getTop(), 0, child.getBottom());
            leftShadow.draw(canvas);
        }
    }

    // Activity初始化
    private void init() {
        mWindow = this.getWindow();
        mDecorView = (ViewGroup) this.getWindow().getDecorView();
        mDecorView.setBackgroundColor(Color.TRANSPARENT);
        WindowManager.LayoutParams wlp = mWindow.getAttributes();
        wlp.format = PixelFormat.TRANSLUCENT;// window的背景也必须透明
        mWindow.setAttributes(wlp);
        mGD = new GestureDetector(this, new MyGestureListener());
        mScroller = new OverScroller(this);
        mSlideLayout = new SlideLayout(this);
        mMoveDetector = new MoveDetector(this);
    }

    // 计算滚动，执行滚动。
    Runnable ViewScroller = new Runnable() {
        @Override
        public void run() {
            if (mScroller.computeScrollOffset()) {
                mSlideLayout.scrollTo(mScroller.getCurrX(), 0);
                if (mScroller.getCurrX() <= -mDecorView.getWidth())
                    finishActivity();
                ViewCompat.postOnAnimation(mDecorView, this);
            }
        }
    };

    /**
     * 无动画关闭Activity
     */
    public void finishActivity() {
        this.finish();
        this.overridePendingTransition(0, 0);
    }

    // 滑动方向判断类
    class MoveDetector {
        // 未识别方向
        public static final int UNKNOWN = 0;
        // 横向
        public static final int HOR = 1;
        // 纵向
        public static final int VER = 2;
        // 默认最大角度。如果小于这个角度，则判断为横向滑动。否则，则为纵向滑动。
        public static final int DEFAULT_MAX_ANGLE = 30;
        private float downX;
        private float downY;
        private float moveOffsetX;
        private float moveOffsetY;
        private int status;
        private int maxInterceptAngle;
        private int touchSlop;

        public MoveDetector(Context context) {
            this(context, DEFAULT_MAX_ANGLE);
        }

        public MoveDetector(Context context, int maxInterceptAngle) {
            this.maxInterceptAngle = maxInterceptAngle;
            this.touchSlop = ViewConfiguration.get(context)
                    .getScaledTouchSlop();
        }

        public int getMaxInterceptAngle() {
            return maxInterceptAngle;
        }

        public void setMaxInterceptAngle(int maxInterceptAngle) {
            this.maxInterceptAngle = maxInterceptAngle;
        }

        // 根据TouchEvent判断滑动方向
        public int getMoveStatus(MotionEvent ev) {
            switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getRawX();
                downY = ev.getRawY();
                status = UNKNOWN;
                break;
            case MotionEvent.ACTION_MOVE:
                moveOffsetX = ev.getRawX() - downX;
                moveOffsetY = ev.getRawY() - downY;
                if ((moveOffsetX * moveOffsetX + moveOffsetY * moveOffsetY) < (touchSlop * touchSlop))
                    return status;
                double angle = Math.atan2(Math.abs(moveOffsetY),
                        Math.abs(moveOffsetX));
                double angle2 = 180 * angle / Math.PI;
                if (angle2 < maxInterceptAngle)
                    status = HOR;
                else
                    status = VER;
                break;
            }
            return status;
        }
    }

    // 手势识别类
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            if (mSlideLayout.getScrollX() >= 0 && distanceX > 0)
                return false;
            // 避免慢速滑动时，卡顿的问题。跳过touchSlop的移动距离。
            if (mIsFirstScroll && -distanceX > 1) {
                distanceX = 0;
                mIsFirstScroll = false;
            }
            mSlideLayout.scrollBy((int) distanceX, 0);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            if (velocityX > MIN_CLOSE_VELOCITY) {
                mScroller.abortAnimation();
                mScroller.startScroll(mSlideLayout.getScrollX(), 0,
                        -(mDecorView.getWidth() - Math.abs(mSlideLayout
                                .getScrollX())), 0, FLING_CLOSE_VELOCITY);
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    }
}
