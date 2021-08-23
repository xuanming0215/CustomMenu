package com.cz.custommenu;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.fragment.app.FragmentTransaction;

import com.github.clans.fab.FloatingActionButton;
import com.yinghe.whiteboardlib.fragment.WhiteBoardFragment;

public class FloatWindowService extends Service {
    private static final String TAG = FloatWindowService.class.getSimpleName();

    public FloatWindowService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on start command");
        FloatingMenu menu=new FloatingMenu(getApplicationContext(),R.layout.float_menu, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"点击按钮");
            }
        });
        menu.show();
        FloatingActionButton fab1 = (FloatingActionButton) menu.mView.findViewById(R.id.camera);
        fab1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 发送广播
                Intent intent = new Intent();
                intent.putExtra("type", "wb");
                intent.setAction("com.cz.custommenu.FloatWindowService");
                sendBroadcast(intent);
                Log.i(TAG,"白板点击");
            }
        });
        FloatingActionButton fab3 = (FloatingActionButton) menu.mView.findViewById(R.id.back);
        fab3.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //返回桌面
                Intent intent = new Intent();
                intent.putExtra("type", "back");
                intent.setAction("com.cz.custommenu.FloatWindowService");
                sendBroadcast(intent);
                Log.i(TAG,"返回点击");
            }
        });

        FloatingActionButton fab2 = (FloatingActionButton) menu.mView.findViewById(R.id.gallery);
        fab2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // 发送广播
                Intent intent = new Intent();
                intent.putExtra("type", "paint");
                intent.setAction("com.cz.custommenu.FloatWindowService");
                sendBroadcast(intent);
                Log.i(TAG,"标绘点击");
            }
        });
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}