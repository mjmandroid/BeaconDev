package com.mjm2.beacondev.test;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.mjm2.beacondev.R;

/**
 * Created by Administrator on 2017/10/20 0020.
 */

public class SwitchButton extends android.support.v7.widget.AppCompatImageButton implements View.OnClickListener {
    private OnSwitchListener listener;
    private int bg_openid;
    private int bg_closesid;
    public final int OPEN = 1;//打开
    public final int CLOSE = 0;//关闭
    private int STATE = -1;//0

    public SwitchButton(Context context) {
        this(context,null);
    }

    public SwitchButton(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public SwitchButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void setListener(OnSwitchListener listener) {
        this.listener = listener;
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.customImageButton);
        bg_openid = typedArray.getResourceId(R.styleable.customImageButton_bg_openid, 0);
        bg_closesid = typedArray.getResourceId(R.styleable.customImageButton_bg_closedid, 0);
        boolean isopen = typedArray.getBoolean(R.styleable.customImageButton_default_state,true);
        if (bg_openid != 0){
            if (isopen){
                STATE = OPEN;
                setBackgroundResource(bg_openid);
            } else {
                STATE = CLOSE;
                setBackgroundResource(bg_closesid);
            }
        }

        typedArray.recycle();

    }

    public void setEnable(boolean isEnable){
        if (!isEnable){
            setSelected(false);
            setEnable(false);
        } else {
            setSelected(true);
            setEnable(true);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(this);
    }


    public int getClickState() {
        return STATE;
    }

    public void setSelected(boolean isOpend){
        if (isOpend) {
            STATE = OPEN;
            if (bg_openid != 0)
                setBackgroundResource(bg_openid);
        } else {
            STATE = CLOSE;
            if (bg_closesid != 0)
                setBackgroundResource(bg_closesid);
        }
    }

    @Override
    public void onClick(View v) {
        if (STATE == CLOSE){
            STATE = OPEN;
            if (listener != null){
                listener.onSwitchChanged(this,true);
            }
            if (bg_openid != 0)
            setBackgroundResource(bg_openid);
        } else {
            STATE = CLOSE;
            if (listener != null){
                listener.onSwitchChanged(this,false);
            }
            if (bg_closesid != 0)
            setBackgroundResource(bg_closesid);
        }
    }

    public interface OnSwitchListener{
        void onSwitchChanged(View view,boolean isOpen);
    }
}
