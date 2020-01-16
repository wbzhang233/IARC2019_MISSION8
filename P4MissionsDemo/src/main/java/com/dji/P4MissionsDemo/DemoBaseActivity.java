package com.dji.P4MissionsDemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class DemoBaseActivity extends FragmentActivity implements SurfaceTextureListener,Thread.UncaughtExceptionHandler {

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    protected DJICodecManager mCodecManager = null;
    protected byte[] mByte=null;
    protected BaseProduct mProduct;

    //To store index chosen in PopupNumberPicker listener
    protected static int[] INDEX_CHOSEN = {-1, -1, -1};

    protected TextView mConnectStatusTextView;

    protected TextureView mVideoSurface = null;

    private Thread.UncaughtExceptionHandler defalutHandler;

    /**
     * @param thread 抛出异常的线程
     * @param ex 抛出异常的一些信息
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        HandleException(thread,ex);
    }
    /*
     * 默认处理保存信息
     */
    public void HandleException(Thread thread, Throwable ex){
        //打印出日志，方便调试的时候查看，否则不抛出异常
        Log.d("BaseActivity",thread.getName()+"exception==="+ex.getMessage());
        defalutHandler.uncaughtException(thread,ex);

        HashMap<String,String> map = new HashMap<>();
        map.put("DEVICE_ID", Build.DEVICE);
        map.put("CURRENT_VERSION","1");
        map.put("EXCEPTION_CAUSE",ex.getMessage());
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String,String> entry :map.entrySet()){
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause() ;
        if(cause!=null){
            cause.printStackTrace(printWriter);
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + "-zty.log";
            String path = Environment.getExternalStorageDirectory() + "/DJI_ScreenShot"+"/crash";
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(dir.getAbsolutePath() +"/"+fileName);
            fos.write(sb.toString().getBytes());
            fos.close();
        } catch (Exception e) {
        }


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();  
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                mByte = videoBuffer;
                if(mCodecManager != null){
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateTitleBar();
            onProductChange();
        }
        
    };
    
    protected void onProductChange() {
        initPreviewer();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        updateTitleBar();
        initPreviewer();
        onProductChange();

        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }
    
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
    
    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(DemoBaseActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    protected void initPreviewer() {
        try {
            mProduct = DJIDemoApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
        }
        
        if (mProduct == null || !mProduct.isConnected()) {
            Log.d(TAG, "Disconnect");
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }

            if (!mProduct.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(mReceivedVideoDataCallBack);
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = DJIDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().setCallback(null);
        }
    }
    
    /**
     * @param surface
     * @param width
     * @param height
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureAvailable(android.graphics.SurfaceTexture,
     *      int, int)
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    /**
     * @param surface
     * @param width
     * @param height
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture,
     *      int, int)
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    /**
     * @param surface
     * @return
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureDestroyed(android.graphics.SurfaceTexture)
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCodecManager != null)
            mCodecManager.cleanSurface();
        return false;
    }

    /**
     * @param surface
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureUpdated(android.graphics.SurfaceTexture)
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
    
    private void updateTitleBar() {
        if(mConnectStatusTextView == null) return;
        boolean ret = false;
        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null) {
            if(product.isConnected()) {
                mConnectStatusTextView.setText(DJIDemoApplication.getProductInstance().getModel().getDisplayName() + " Connected");
                FlightController mFlightController = DJIDemoApplication.getAircraftInstance().getFlightController();
                if(mFlightController != null){
                    mFlightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null){
                                showToast(djiError.getDescription());
                            }else
                            {
                                showToast("stop Virtual Stick Success");
                            }
                        }
                    });
                }
                ret = true;
            } else {
                if(product instanceof Aircraft) {
                    Aircraft aircraft = (Aircraft)product;
                    if(aircraft.getRemoteController() != null) {
                    }
                    if(aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        mConnectStatusTextView.setText("only RC Connected");
                        ret = true;
                    }
                }
            }
        }
        
        if(!ret) {
//            mConnectStatusTextView.setText("Disconnected");
        }
    }
    
    /**
     * @Description : RETURN BTN RESPONSE FUNCTION
     * @author : andy.zhao
     * @param view
     * @return : void
     */
    public void onReturn(View view) {
        this.finish();
    }
    
    public void resetIndex() {
        INDEX_CHOSEN = new int[3];
        INDEX_CHOSEN[0] = -1;
        INDEX_CHOSEN[1] = -1;
        INDEX_CHOSEN[2] = -1;
    }
    
    public ArrayList<String> makeListHelper(Object[] o) {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < o.length - 1; i++) {
            list.add(o[i].toString());
        }
        return list;
    }
}

