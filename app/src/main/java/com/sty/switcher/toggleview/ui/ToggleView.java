package com.sty.switcher.toggleview.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * 自定义开关
 * Created by Shi Tianyi on 2017/10/7/0007.
 *
 * Android 的界面绘制流程
 *
 *  测量         摆放      绘制
 * measure --> layout --> draw
 *
 * onMeasure --> onLayout --> onDraw 重写这些方法，实现自定义控件[这些方法都在onResume()方法之后执行]
 *
 * View
 * onMeasure()[在这个方法里指定自己的宽高] --> onDraw()[绘制自己的内容]
 *
 * ViewGroup
 * onMeasure()[指定自己的宽高,所有子View的宽高] --> onLayout()[摆放所有子View] --> onDraw()[绘制内容]
 */
public class ToggleView extends View {
    private static final String TAG = ToggleView.class.getSimpleName();

    private Bitmap switchBackgroundBitmap; //背景图片
    private Bitmap slideButtonBitmap; //滑块图片
    private Paint paint; //画笔
    private boolean mSwitchState = false; //开关状态，默认为false
    private float currentX;
    private boolean isTouchMode = false; //是否为滑动状态

    private OnSwitchStateUpdateListener onSwitchStateUpdateListener;

    private void init(){
        paint = new Paint();
    }

    /**
     * 用于代码创建控件
     * @param context
     */
    public ToggleView(Context context) {
        super(context);
        init();
    }

    /**
     * 用于在xml里面使用，可指定自定义属性
     * @param context
     * @param attrs
     */
    public ToggleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();

        //获取配置的自定义属性
        String namespace = "http://schemas.android.com/apk/res-auto";
        int switchBackgroundResource = attrs.getAttributeResourceValue(namespace, "switch_background", -1);
        int slideButtonResource = attrs.getAttributeResourceValue(namespace, "slide_button", -1);

        mSwitchState = attrs.getAttributeBooleanValue(namespace, "switch_state", false);
        setSwitchBackgroundResource(switchBackgroundResource);
        setSlideButtonResource(slideButtonResource);
    }

    /**
     * 用于在xml里面使用，可指定自定义属性，如果指定了样式，则走此构造函数
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public ToggleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 设置背景图片
     * @param switchBackground
     */
    public void setSwitchBackgroundResource(int switchBackground){
        switchBackgroundBitmap = BitmapFactory.decodeResource(getResources(), switchBackground);
    }

    /**
     * 设置滑块图片资源
     * @param slideButton
     */
    public void setSlideButtonResource(int slideButton){
        slideButtonBitmap = BitmapFactory.decodeResource(getResources(), slideButton);
    }

    /**
     * 设置开关状态
     * @param state
     */
    public void setSwitchState(boolean state){
        this.mSwitchState = state;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(switchBackgroundBitmap.getWidth(), switchBackgroundBitmap.getHeight());
    }

    /**
     *
     * @param canvas 画布，画板，在上面绘制的内容都会显示到界面上
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // 1. 绘制背景
        canvas.drawBitmap(switchBackgroundBitmap, 0, 0, paint);

        //2. 绘制滑块
        if(isTouchMode) {
            //根据当前用户触摸到的位置画滑块
            //让滑块向左移动自身一半大小的位置
            float newLeft = currentX - slideButtonBitmap.getWidth() / 2.0f;

            int maxLeft = switchBackgroundBitmap.getWidth() - slideButtonBitmap.getWidth();
            //判断滑块范围
            if(newLeft < 0){
                newLeft = 0; //左边范围
            }else if(newLeft > maxLeft){
                newLeft = maxLeft;
            }
            canvas.drawBitmap(slideButtonBitmap, newLeft, 0, paint);
        }else {
            //根据开关状态，直接设置图片位置
            if (mSwitchState) { //开
                int newLeft = switchBackgroundBitmap.getWidth() - slideButtonBitmap.getWidth();
                canvas.drawBitmap(slideButtonBitmap, newLeft, 0, paint);
            } else { //关
                canvas.drawBitmap(slideButtonBitmap, 0, 0, paint);
            }
        }
    }

    /**
     * 重新触摸事件，响应用户的触摸
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                isTouchMode = true;
                Log.i(TAG, "event-->ACTION_DOWN:" + event.getX());
                currentX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                Log.i(TAG, "event-->ACTION_MOVE:" + event.getX());
                currentX = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                isTouchMode = false;
                Log.i(TAG, "event-->ACTION_UP:" + event.getX());
                currentX = event.getX();

                float center = switchBackgroundBitmap.getWidth() / 2.0f;
                //根据当前按下的位置和控件中心的位置进行比较
                boolean state = currentX > center;

                //如果开关状态变化了，通知界面，里面开关状态更新了
                if(state != mSwitchState && onSwitchStateUpdateListener != null){
                    //把最新的状态传出去了
                    onSwitchStateUpdateListener.onStateUpdate(state);
                }
                mSwitchState = state;

                break;
            default:
                break;
        }

        //重绘界面
        invalidate(); // 会引发onDraw被调用，里面的遍历会重新生效，界面会更新

        return true; //消费了用户的触摸事件，这样才能收到其他的事件
    }

    public interface OnSwitchStateUpdateListener{
        //状态回调，把当前状态传出去
        void onStateUpdate(boolean state);
    }

    public void setOnSwitchStateUpdateListener(OnSwitchStateUpdateListener onSwitchStateUpdateListener){
        this.onSwitchStateUpdateListener = onSwitchStateUpdateListener;
    }
}
