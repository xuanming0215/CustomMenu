package com.cz.custommenu;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class FloatingMenu {
    public View mView;
    Context mContext;
    private WindowManager windowManager;
    private Boolean isShowing = false;

    /**
     * 这里采用的自定义点击回调事件
     */
    FloatingMenu(Context context, int resourceID, View.OnClickListener onClickListener) {
        mContext = context;
        windowManager = (WindowManager) mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        mView = initView(mContext, resourceID, onClickListener);
    }

    /**
     * 显示弹框
     */
    public void show() {
        if (isShowing) {
            return;
        }
        isShowing = true;
        //检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(mContext)) {
                //启动Activity让用户授权
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                getActivity(mContext).startActivity(intent);
                return;
            }
        }
        final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        //检查版本，注意当type为TYPE_APPLICATION_OVERLAY时，铺满活动窗口，但在关键的系统窗口下面，如状态栏或IME
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        //覆盖状态栏
        //layoutParams.flags = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        layoutParams.x=0;
        layoutParams.y=0;
        windowManager.addView(mView, layoutParams);
    }

    /**
     * 隐藏弹框
     */
    public void dismiss() {
        if (isShowing && mView != null) {
            windowManager.removeViewImmediate(mView);
            isShowing = false;
        }
    }
    /**
     * 初始化
     */
    private View initView(Context context, int resourceID, View.OnClickListener onClickListener) {
        final View view = LayoutInflater.from(context).inflate(resourceID, null);
        //设置点击事件
        view.setOnClickListener(onClickListener);
        //监听BACK按键
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        //监听BACK按键时必须使得相应的View获取焦点(使用post方式取得焦点)，否则setOnKeyListener的onKey方法永远不会被回调，按键就监听不到！
        view.post(new Runnable() {
            @Override
            public void run() {
                view.setFocusable(true);
                view.setFocusableInTouchMode(true);
                view.requestFocus();
            }
        });
        return view;
    }

    /**
     * 得到 Context 对应的 Activity。
     */
    public static AppCompatActivity getActivity(Context context) {
        return context instanceof AppCompatActivity ? (AppCompatActivity) context : null;
    }

}
