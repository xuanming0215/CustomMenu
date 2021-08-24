package com.cz.custommenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.cz.custommenu.eventbus.EventCenter;
import com.cz.custommenu.service.TcpServer;
import com.example.wisdomclassroomgroup.WatchContect;
import com.example.wisdomclassroomgroup.service.MediaReaderService;
import com.yinghe.whiteboardlib.fragment.WhiteBoardFragment;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private static final String TAG = MainActivity.class.getSimpleName();
    private MyReceiver receiver = null;
    private WhiteBoardFragment whiteBoardFragment = WhiteBoardFragment.newInstance();
    private PaintFragment paintFragment = PaintFragment.newInstance();

    private static final int RC_SCREEN_PERM = 124;
    private int REQUEST_MEDIA_PROJECTION = 18;
    private MediaProjectionManager mediaProjectionManager;
    private MyApplication myApplication;
    private Intent intent;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (null != intent) {
            stopService(intent);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, FloatWindowService.class));
        EventBus.getDefault().register(this);
        myApplication = (MyApplication) getApplication();
        checPermiss();
        initSocket();
        // 动态注册广播接收器
        receiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.cz.custommenu.FloatWindowService");
        MainActivity.this.registerReceiver(receiver, filter);

//        moveTaskToBack(true);
        //finish();
    }

    private void initSocket() {
        intent = new Intent(getApplicationContext(), TcpServer.class);
        startService(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void EventMess(EventCenter center) {

        if (!AppUtils.isAppForeground()) {
            ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);

            /**获得当前运行的task(任务)*/
            List<ActivityManager.RunningTaskInfo> taskInfoList = activityManager.getRunningTasks(100);
            for (ActivityManager.RunningTaskInfo taskInfo : taskInfoList) {
                /**找到本应用的 task，并将它切换到前台*/
                if (taskInfo.topActivity.getPackageName().equals(this.getPackageName())) {
                    activityManager.moveTaskToFront(taskInfo.id, 0);
                    break;
                }
            }
        }


        String data = (String) center.getData();
        //data 1.开启   0.不开
        if (data.equals("1") && !ActivityUtils.isActivityExistsInStack(WatchContect.class)) {

            Intent intent = new Intent(this, WatchContect.class);
            Bundle bundle = new Bundle();
//            bundle.putString("Address", "172.31.98.211");
            bundle.putString("Address", Constants.mainIP);
            intent.putExtras(bundle);
            startActivity(intent);
        }

    }

    //获取屏幕权限
    @AfterPermissionGranted(RC_SCREEN_PERM)
    private void checPermiss() {
        String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            requestCapturePermission();
        } else {
            EasyPermissions.requestPermissions(this, "需要访问存储权限",
                    RC_SCREEN_PERM, perms);
        }
    }

    private void requestCapturePermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            //5.0 之后才允许使用屏幕截图
            mediaProjectionManager = (MediaProjectionManager) this.getSystemService(
                    Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);

            moveTaskToBack(true);
        } else {
            Toast.makeText(this, "系统版本低于5.0!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection == null) {

                    return;
                }

                myApplication.mediaProjection = mediaProjection;
                start();
                MediaReaderService.ServerStatus serverStatus = myApplication.getServerStatus();
                serverStatus = MediaReaderService.ServerStatus.STARTED;
            }
        }
    }

    /**
     * 开启
     */
    private void start() {
        Intent intent = new Intent(this, MediaReaderService.class);
        intent.putExtra("CMD", 1);
        startService(intent);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            ToastUtils.showLong("没有相关权限，打开应用程序设置界面修改应用程序权限");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * 获取广播数据
     *
     * @author jiqinlin
     */
    public class MyReceiver extends BroadcastReceiver {

        public void replaceFragment(FragmentManager manager, Fragment to, int fgmId) {
            FragmentTransaction transaction = manager.beginTransaction();
            //遍历隐藏所有添加的fragment
            for (Fragment fragment : manager.getFragments()) {
                transaction.hide(fragment);
            }
            if (!to.isAdded()) { //若没有添加过
                transaction.add(fgmId, to).commitAllowingStateLoss();
            } else { //若已经添加
                transaction.show(to).commitAllowingStateLoss();
            }

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            String type = bundle.getString("type");
            Log.i(TAG, type);
            if ("wb".equals(type)) {
                bring2Front();
                //获取Fragment管理器
                //FragmentTransaction ts = getSupportFragmentManager().beginTransaction();
                //获取WhiteBoardFragment实例
                //WhiteBoardFragment whiteBoardFragment=WhiteBoardFragment.newInstance();
                //添加到界面中
                this.replaceFragment(getSupportFragmentManager(), whiteBoardFragment, R.id.fl_main);
            }
            if ("paint".equals(type)) {
                bring2Front();

//                Fragment whiteBoardFragment=getSupportFragmentManager().findFragmentById(R.id.fl_main);
//                if(whiteBoardFragment!=null){
//                    //获取Fragment管理器
//                    FragmentTransaction ts = getSupportFragmentManager().beginTransaction();
//                    ts.remove(whiteBoardFragment).commitAllowingStateLoss();
//                }
//                DisplayMetrics dm = getResources().getDisplayMetrics();
//                int windowHeight = dm.heightPixels;
//                int windowWidth = dm.widthPixels;
//
//                DrawView drawView=new DrawView(context,windowWidth,windowHeight);
//                //通知view组件重绘
//                drawView.invalidate();
//                FrameLayout.LayoutParams returnLayoutparams = new FrameLayout.
//                        LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
//                FrameLayout fl=findViewById(R.id.fl_main);
//                fl.addView(drawView,returnLayoutparams);
                //FragmentTransaction ts = getSupportFragmentManager().beginTransaction();
                //获取PaintFragment实例
                //Fragment paintFragment = getSupportFragmentManager().findFragmentById(R.id.fl_main);
                //添加到界面中
                this.replaceFragment(getSupportFragmentManager(), paintFragment, R.id.fl_main);
            }
            if ("back".equals(type)) {
                moveTaskToBack(true);
            }
        }
    }

    private void bring2Front() {
        ActivityManager activtyManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activtyManager.getRunningTasks(3);
        for (ActivityManager.RunningTaskInfo runningTaskInfo : runningTaskInfos) {
            if (this.getPackageName().equals(runningTaskInfo.topActivity.getPackageName())) {
                activtyManager.moveTaskToFront(runningTaskInfo.id, ActivityManager.MOVE_TASK_WITH_HOME);
                return;
            }
        }
    }

}