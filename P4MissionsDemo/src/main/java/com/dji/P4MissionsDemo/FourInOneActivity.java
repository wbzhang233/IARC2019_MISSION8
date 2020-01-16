package com.dji.P4MissionsDemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.speech.asr.SpeechConstant;
import com.dji.P4MissionsDemo.coms.TcpServer;
import com.dji.P4MissionsDemo.control.MyRecognizer;
import com.dji.P4MissionsDemo.control.MyWakeup;
import com.dji.P4MissionsDemo.funcs.FuncTcpClient;
import com.dji.P4MissionsDemo.qrdetect.qrCodeDetect;
import com.dji.P4MissionsDemo.recognization.CommonRecogParams;
import com.dji.P4MissionsDemo.recognization.MessageStatusRecogListener;
import com.dji.P4MissionsDemo.recognization.StatusRecogListener;
import com.dji.P4MissionsDemo.recognization.offline.OfflineRecogParams;
import com.dji.P4MissionsDemo.tensorflow.DetectorAPI;
import com.dji.P4MissionsDemo.tensorflow.OverlayView;
import com.dji.P4MissionsDemo.udp.UDPServer;
import com.dji.P4MissionsDemo.udp.handleReceiveData;
import com.dji.P4MissionsDemo.wakeup.IWakeupListener;
import com.dji.P4MissionsDemo.wakeup.RecogWakeupListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dji.sdk.codec.DJICodecManager;

import static com.dji.P4MissionsDemo.qrdetect.QRCodeUtil.decodeFromPhoto;
import static com.dji.P4MissionsDemo.recognization.IStatus.STATUS_READY;
import static com.dji.P4MissionsDemo.recognization.IStatus.STATUS_RECOGNITION;
import static com.dji.P4MissionsDemo.recognization.IStatus.STATUS_SPEAKING;
import static com.dji.P4MissionsDemo.recognization.IStatus.STATUS_WAKEUP_SUCCESS;

public class FourInOneActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MSG_WHAT_SHOW_TOAST = 0;
    private static final int MSG_WHAT_UPDATE_TITLE = 1;
    private static final int MSG_WHAT_tcpServerReceiver_ip = 2;
    private static final int MSG_WHAT_tcpServerReceiver_rcvMsg = 3;
    private static final int MSG_WHAT_tcpServerReceiver_liveFrame = 4;
    private static final int MSG_WHAT_tcpServerReceiver_liveSurface_videobuffer = 5;
    private static final int MSG_WHAT_tcpServerReceiver_State = 6;
    private static final int MSG_WHAT_tcpServerReceiver_photo = 7;
    private static final int MSG_WHAT_tcpServerSave_photo = 8;
    //存照片技术变量
    private int photoIdx=0;
    //***********************语音控制参数********************
    protected MyRecognizer myRecognizer;
    protected MyWakeup myWakeup;
    protected CommonRecogParams apiParams;
    protected boolean enableOffline = true;
    private String[] mNumberDJI = {"洞幺洞幺","洞两洞两","上一首","下一首","全体都有"};
    private int num = 0 ;

    //***********************网络设置参数********************
    public static int UDPport = 7777;
    private UDPServer fourInOneUDP;
    private List<String> UDPiplist = new ArrayList<String>();
    //    private List<String>  IpAddress = new ArrayList<String>();
    private int port=8888;
    private static TcpServer tcpServer = null;
    private List<String> Iplist = new ArrayList<String>();
    ExecutorService exec = Executors.newCachedThreadPool();

    //***********************视图控件********************
    private TextView titleTv;//标题
    //    private ArrayList<TextureView> videostreamPreviewTtViewList = new ArrayList<TextureView>();
    private ArrayList<DJICodecManager> mCodecManagerList = new ArrayList<DJICodecManager>(4);
    private TextView infoSendDataTv, infoIpTv, savePath,ipclientPushTv,mAsrPushTv,infoAsrTv;
    private TextureView DJI1ttv,DJI2ttv,DJI3ttv,DJI4ttv;

    private ImageView mdetectorPoinntRstPointIv,dji2imageFrameIv,dji3imageFrameIv,dji1imageFrameIv,dji4imageFrameIv;
    private Switch infoScrollViewSw;
    private Button infoSendBt;
    private List<Button>mdetectorPointStartBtn = new ArrayList<Button>();
    private SlidingDrawer mPushDrawerSd;
    private StringBuilder stringBuilder;
    private int videoViewWidth;
    private int videoViewHeight;
    private boolean startLive = true;
    private boolean startAsr = false;

    private List<DetectorAPI> mDetectorAPI = new ArrayList<DetectorAPI>();
    private List<RectF> mDetectorResult;
    private List<Integer> ShowIndex = new ArrayList<Integer>();

    public Handler handler,mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WHAT_SHOW_TOAST:
                    Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_WHAT_UPDATE_TITLE:
                    if (titleTv != null) { titleTv.setText((String) msg.obj); }
                    break;
                case MSG_WHAT_tcpServerReceiver_ip:
                    if(((String) msg.obj).indexOf("--") == -1) { Iplist.add(msg.obj.toString()); }
                    ipclientPushTv.append( "\n["+Iplist.indexOf(msg.obj.toString())+"]"+ msg.obj.toString());
//                    try {
//                        if(Iplist.indexOf(msg.obj.toString()) <= 3) {
//                            mDetectorAPI.get(Iplist.indexOf(msg.obj.toString())).onStart();
//                        }
//                    }
//                    catch (Exception e){
//                        showToast(e.getMessage());
//                    }
                    break;
                case MSG_WHAT_tcpServerReceiver_State:
                    ipclientPushTv.append( "\n"+ msg.obj.toString());
                    break;
                case MSG_WHAT_tcpServerReceiver_rcvMsg:                     // display the data received
                    if(msg.arg1 == 0) {                                              // display the ip of client which sending message now
                        ipclientPushTv.append("\nfrom:["+ msg.obj.toString()+"]:"); }
                    else{                                                       // display the data of client which sending message now
                        ipclientPushTv.append( msg.obj.toString()); }
                    break;
                case MSG_WHAT_tcpServerReceiver_liveFrame:                   // display the frame (image) of client which sending message now
                    if(FuncTcpClient.PlanControl == 1 || FuncTcpClient.PlanControl == 2 ) {
                        if (msg.arg1 == 0) {
                            dji1imageFrameIv.setVisibility(View.VISIBLE);
//                        videostreamPreviewTtViewList.get(msg.arg1).setVisibility(View.GONE);
                            dji1imageFrameIv.setImageBitmap((Bitmap) msg.obj);
                        } else if (msg.arg1 == 1) {
                            dji2imageFrameIv.setImageBitmap((Bitmap) msg.obj);
                        } else if (msg.arg1 == 2) {
                            dji3imageFrameIv.setImageBitmap((Bitmap) msg.obj);
                        } else if (msg.arg1 == 3) {
                            dji4imageFrameIv.setImageBitmap((Bitmap) msg.obj);
                        }
                    }
                    break;
                case MSG_WHAT_tcpServerReceiver_photo:
                    if (msg.arg1 == 0) {
//                        videostreamPreviewTtViewList.get(msg.arg1).setVisibility(View.GONE);
                        dji2imageFrameIv.setImageBitmap((Bitmap) msg.obj);
                    } else if (msg.arg1 == 1) {
                        dji3imageFrameIv.setImageBitmap((Bitmap) msg.obj);
                    } else if (msg.arg1 == 2) {
                        dji4imageFrameIv.setImageBitmap((Bitmap) msg.obj);
                    } else if (msg.arg1 == 3) {
                        dji1imageFrameIv.setImageBitmap((Bitmap) msg.obj);
                    }
                    break;
                case MSG_WHAT_tcpServerSave_photo:
                    photoIdx=(int)msg.obj;
//                    for(int i=0;i<4;i++) {
//                        tcpServer.SST.get(num).setPictureIndex(photoIdx);
//                    }
                    break;
                case MSG_WHAT_tcpServerReceiver_liveSurface_videobuffer:  // display the frame (surfaceTexture) of client which sending message now
                    // The callback for receiving the raw H264 video data for camera live view
                    if(startLive) {
                        dji2imageFrameIv.setVisibility(View.GONE);
//                        DJI1svCodecManager.sendDataToDecoder((byte[]) msg.obj, msg.arg2);
//                        videostreamPreviewTtViewList.get(msg.arg1).setVisibility(View.VISIBLE);
//                        mCodecManagerList.get(msg.arg1).sendDataToDecoder((byte[]) msg.obj, msg.arg2);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        startLive = true;
    }

    @Override
    protected void onPause() {
        startLive = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        for(DJICodecManager mCodecManager : mCodecManagerList) {
            if (mCodecManager != null) {
                mCodecManager.cleanSurface();
                mCodecManager.destroyCodec();
            }
        }
        myWakeup.stop();
        myWakeup.release();
        myRecognizer.cancel();
        myRecognizer.release();
        mDetectorAPI.clear();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_4in1);
        initUi();
        initPreviewerTextureView();

        handler = new Handler() {

            /*
             * @param msg
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleMsg(msg);
            }

        };

        StatusRecogListener recogListener = new MessageStatusRecogListener(handler);
        // 改为 SimpleWakeupListener 后，不依赖handler，但将不会在UI界面上显示
        myRecognizer = new MyRecognizer(this, recogListener);

        IWakeupListener listener = new RecogWakeupListener(handler);
        myWakeup = new MyWakeup(this, listener);

        apiParams = getApiParams();
        if (enableOffline) {
            myRecognizer.loadOfflineEngine(OfflineRecogParams.fetchOfflineParams()); }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        OverlayView olv =  (OverlayView) findViewById(R.id.DJI1_tracking_overlay);
        Context ctx = this;
        mDetectorAPI.add(new DetectorAPI(ctx,olv,rotation));
        mDetectorAPI.add(new DetectorAPI(ctx,(OverlayView) findViewById(R.id.DJI2_tracking_overlay),rotation));
        mDetectorAPI.add(new DetectorAPI(ctx,(OverlayView) findViewById(R.id.DJI3_tracking_overlay),rotation));
        mDetectorAPI.add(new DetectorAPI(ctx,(OverlayView) findViewById(R.id.DJI4_tracking_overlay),rotation));

        ShowIndex.add(0);
        ShowIndex.add(0);
        ShowIndex.add(0);
        ShowIndex.add(0);

        if(FuncTcpClient.PlanControl == 4){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        fourInOneUDP = new UDPServer(UDPport);
                        fourInOneUDP.setReceiveCallback(new handleReceiveData() {
                            @Override
                            public void handleReceive(byte[] data) {

                            }

                            @Override
                            public void handleReceive(final byte[] data,final String ipAddress) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Bitmap bit = BitmapFactory.decodeByteArray(data, 0, data.length);
                                        if (Iplist.indexOf(ipAddress) == 0) {
                                            dji1imageFrameIv.setImageBitmap((Bitmap) bit);
                                        } else if (Iplist.indexOf(ipAddress) == 1) {
                                            dji2imageFrameIv.setImageBitmap((Bitmap) bit);
                                        } else if (Iplist.indexOf(ipAddress) == 2) {
                                            dji3imageFrameIv.setImageBitmap((Bitmap) bit);
                                        } else if (Iplist.indexOf(ipAddress) == 3) {
                                            dji4imageFrameIv.setImageBitmap((Bitmap) bit);
                                        }
                                        else {
                                            if(UDPiplist.indexOf(ipAddress) < 0){
                                                UDPiplist.add(ipAddress);
                                            }
                                            if (UDPiplist.indexOf(ipAddress) == 0) {
                                                dji1imageFrameIv.setImageBitmap((Bitmap) bit);
                                            } else if (UDPiplist.indexOf(ipAddress) == 1) {
                                                dji2imageFrameIv.setImageBitmap((Bitmap) bit);
                                            } else if (UDPiplist.indexOf(ipAddress) == 2) {
                                                dji3imageFrameIv.setImageBitmap((Bitmap) bit);
                                            } else if (UDPiplist.indexOf(ipAddress) == 3) {
                                                dji4imageFrameIv.setImageBitmap((Bitmap) bit);
                                            }
                                        }
                                        dji1imageFrameIv.requestLayout();
                                        dji2imageFrameIv.requestLayout();
                                        dji3imageFrameIv.requestLayout();
                                        dji4imageFrameIv.requestLayout();
                                    }
                                });
                            }
                        });
                        fourInOneUDP.start();
                    } catch (SocketException e) {
                        //button.setEnabled(true);
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    protected CommonRecogParams getApiParams() {
        return new OfflineRecogParams(this);
    }

    private void showToast(String s) {
        mainHandler.sendMessage(
                mainHandler.obtainMessage(MSG_WHAT_SHOW_TOAST, s)
        );
    }

    private void updateTitle(String s) {
        mainHandler.sendMessage(
                mainHandler.obtainMessage(MSG_WHAT_UPDATE_TITLE, s)
        );
    }

    private void initUi() {

        titleTv = (TextView) findViewById(R.id.title_tv);

//        videostreamPreviewTtViewList.add((TextureView) findViewById(R.id.DJI1_ttv));
//        videostreamPreviewTtViewList.add((TextureView) findViewById(R.id.DJI2_ttv));
//        videostreamPreviewTtViewList.add((TextureView) findViewById(R.id.DJI3_ttv));
//        videostreamPreviewTtViewList.add((TextureView) findViewById(R.id.DJI4_ttv));
//        DJI1ttv = (TextureView) findViewById(R.id.DJI1_ttv);
//        mdetectorPoinntRstPointIv = (ImageView)findViewById(R.id.detector_pointing_rst_point_iv);
        mdetectorPointStartBtn.add((Button)findViewById(R.id.DJI1detector_pointing_start_btn));
        mdetectorPointStartBtn.add((Button)findViewById(R.id.DJI2detector_pointing_start_btn));
        mdetectorPointStartBtn.add((Button)findViewById(R.id.DJI3detector_pointing_start_btn));
        mdetectorPointStartBtn.add((Button)findViewById(R.id.DJI4detector_pointing_start_btn));

        infoIpTv = (TextView) findViewById(R.id.info_ip_tv);
        infoSendDataTv = (TextView) findViewById(R.id.info_sendData_tv);
        infoScrollViewSw = (Switch) findViewById(R.id.info_scrollView_sw);
        infoSendBt = (Button) findViewById(R.id.info_sendButton_bt);
        mPushDrawerSd = (SlidingDrawer) findViewById(R.id.pointing_drawer_sd);
        ipclientPushTv = (TextView)findViewById(R.id.ip_client_push_tv);
        mAsrPushTv = (TextView)findViewById(R.id.ASR_push_tv);
        dji2imageFrameIv = (ImageView)findViewById(R.id.DJI2_image_frame_iv);
        dji4imageFrameIv = (ImageView)findViewById(R.id.DJI4_image_frame_iv);
        dji1imageFrameIv = (ImageView)findViewById(R.id.DJI1_image_frame_iv);
        dji3imageFrameIv = (ImageView)findViewById(R.id.DJI3_image_frame_iv);
        infoAsrTv = (TextView)findViewById(R.id.info_asr_tv);
        infoAsrTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAsr = !startAsr;
                if(startAsr){
                    start();
                    setPointBtn();
                    infoAsrTv.setText("关闭语音");
                }
                else {
                    stop();
                    infoAsrTv.setText("开启语音");
                }
            }
        });

        infoIpTv.setText(getHostIP());

        infoSendBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tcpServer != null) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            for(TcpServer.ServerSocketThread server :tcpServer.SST)
                                server.send(infoSendDataTv.getText().toString());
                        }
                    });
                }
            }
        });
        infoIpTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tcpServer == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            infoIpTv.setText(getHostIP()+":"+port);
                        }
                    });
//                    tcpServer = new TcpServer(port,mainHandler);
                    tcpServer = new TcpServer(port,mainHandler,mDetectorAPI);
                    exec.execute(tcpServer);
                }
                else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            infoIpTv.setText(getHostIP());
                            Iplist.clear();
                            UDPiplist.clear();
                        }
                    });
                    tcpServer.closeSelf();
                    tcpServer = null;
                    ipclientPushTv.setText("client IP push:");
                }
            }
        });

        infoScrollViewSw.setChecked(true);
        mPushDrawerSd.animateOpen();
        infoScrollViewSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    infoSendDataTv.setVisibility(View.VISIBLE);
                    infoSendBt.setVisibility(View.VISIBLE);
//                    mPushDrawerSd.animateClose();
                    infoIpTv.setVisibility(View.VISIBLE);
                    infoAsrTv.setVisibility(View.VISIBLE);
                } else {
                    infoSendDataTv.setVisibility(View.INVISIBLE);
                    infoSendBt.setVisibility(View.INVISIBLE);
//                    mPushDrawerSd.animateOpen();
                    infoIpTv.setVisibility(View.INVISIBLE);
                    infoAsrTv.setVisibility(View.INVISIBLE);
                }

            }
        });
    }

    private void setPointBtn(){
        for(final DetectorAPI detectorAPI : mDetectorAPI) {
            mDetectorResult = detectorAPI.getResults();
            int ind = mDetectorAPI.indexOf(detectorAPI);
            if(mDetectorResult != null && ShowIndex.get(ind).intValue() >= mDetectorResult.size()){
                ShowIndex.remove(ind);
                ShowIndex.add(ind,0);
            }

            if (mDetectorResult != null && ShowIndex.get(ind) < mDetectorResult.size()) {
                final int number = mDetectorAPI.indexOf(detectorAPI);

                View parent = (View) mdetectorPointStartBtn.get(number).getParent();
                float centerX = mDetectorResult.get(ShowIndex.get(ind)).centerX();
                float centerY = mDetectorResult.get(ShowIndex.get(ind)).centerY();

                centerX = centerX < 0 ? 0 : centerX;
                centerX = centerX > parent.getWidth() ? parent.getWidth() : centerX;
                centerY = centerY < 0 ? 0 : centerY;
                centerY = centerY > parent.getHeight() ? parent.getHeight() : centerY;
                final float pintPosX = centerX / parent.getWidth();
                final float pintPosY = centerY / parent.getHeight();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            tcpServer.SST.get(number).send("pintPosX");
                            tcpServer.SST.get(number).send(String.valueOf(pintPosX));
                            tcpServer.SST.get(number).send("pintPosY");
                            tcpServer.SST.get(number).send(String.valueOf(pintPosY));
                        } catch (Exception e) {
                            showToast("SetPointBtn wrong");
                        }
                    }
                }).start();
                mdetectorPointStartBtn.get(number).setVisibility(View.VISIBLE);
                mdetectorPointStartBtn.get(number).setX(centerX);
                mdetectorPointStartBtn.get(number).setY(centerY);
                mdetectorPointStartBtn.get(number).requestLayout();

            }
        }
    }

    /**
     * Init a fake texture view to for the codec manager, so that the video raw data can be received
     * by the camera
     */
    private void initPreviewerTextureView() {
        //
//        DJI1sv.getHolder().addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder holder) {
//                if(DJI1svCodecManager == null){
//                    DJI1svCodecManager = new DJICodecManager(getApplicationContext(), holder, DJI1sv.getWidth(),DJI1sv.getHeight());
//                }
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//
//            }
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder holder) {
//                if(DJI1svCodecManager != null){
//                    DJI1svCodecManager.cleanSurface();
//                    DJI1svCodecManager.destroyCodec();
//                }
//            }
//        });
//
//        videostreamPreviewTtViewList.get(0).setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//            @Override
//            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//                Log.d(TAG, "real onSurfaceTextureAvailable");
//                videoViewWidth = width;
//                videoViewHeight = height;
//                Log.d(TAG, "real onSurfaceTextureAvailable: width " + videoViewWidth + " height " + videoViewHeight);
//                if (mCodecManagerList.size() == 0) {
//                    mCodecManagerList.add(0, new DJICodecManager(getApplicationContext(), surface, width, height));
//                }
//            }
//
//            @Override
//            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//                videoViewWidth = width;
//                videoViewHeight = height;
//                Log.d(TAG, "real onSurfaceTextureAvailable2: width " + videoViewWidth + " height " + videoViewHeight);
//            }
//
//            @Override
//            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//                if (mCodecManagerList.get(0) != null) {
//                    mCodecManagerList.get(0).cleanSurface();
//                }
//                return false;
//            }
//
//            @Override
//            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//
//            }
//        });
//
//        videostreamPreviewTtViewList.get(1).setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//            @Override
//            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//                Log.d(TAG, "real onSurfaceTextureAvailable");
//                videoViewWidth = width;
//                videoViewHeight = height;
//                Log.d(TAG, "real onSurfaceTextureAvailable: width " + videoViewWidth + " height " + videoViewHeight);
//                if (mCodecManagerList.size() == 1) {
//                    mCodecManagerList.add(1, new DJICodecManager(getApplicationContext(), surface, width, height));
//                }
//            }
//
//            @Override
//            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//                videoViewWidth = width;
//                videoViewHeight = height;
//                Log.d(TAG, "real onSurfaceTextureAvailable2: width " + videoViewWidth + " height " + videoViewHeight);
//            }
//
//            @Override
//            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//                if (mCodecManagerList.get(1) != null) {
//                    mCodecManagerList.get(1).cleanSurface();
//                }
//                return false;
//            }
//
//            @Override
//            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//
//            }
//        });
//
//        videostreamPreviewTtViewList.get(2).setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//            @Override
//            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//                Log.d(TAG, "real onSurfaceTextureAvailable");
//                videoViewWidth = width;
//                videoViewHeight = height;
//                Log.d(TAG, "real onSurfaceTextureAvailable: width " + videoViewWidth + " height " + videoViewHeight);
//                if (mCodecManagerList.size() == 2) {
//                    mCodecManagerList.add(2, new DJICodecManager(getApplicationContext(), surface, width, height));
//                }
//            }
//
//            @Override
//            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//                videoViewWidth = width;
//                videoViewHeight = height;
//                Log.d(TAG, "real onSurfaceTextureAvailable2: width " + videoViewWidth + " height " + videoViewHeight);
//            }
//
//            @Override
//            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//                if (mCodecManagerList.get(2) != null) {
//                    mCodecManagerList.get(2).cleanSurface();
//                }
//                return false;
//            }
//
//            @Override
//            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//
//            }
//        });
//
//        videostreamPreviewTtViewList.get(3).setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
//            @Override
//            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//                Log.d(TAG, "real onSurfaceTextureAvailable");
//                videoViewWidth = width;
//                videoViewHeight = height;
//                Log.d(TAG, "real onSurfaceTextureAvailable: width " + videoViewWidth + " height " + videoViewHeight);
//                if (mCodecManagerList.size() == 3) {
//                    mCodecManagerList.add(3, new DJICodecManager(getApplicationContext(), surface, width, height));
//                }
//            }
//
//            @Override
//            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//                videoViewWidth = width;
//                videoViewHeight = height;
//                Log.d(TAG, "real onSurfaceTextureAvailable2: width " + videoViewWidth + " height " + videoViewHeight);
//            }
//
//            @Override
//            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//                if (mCodecManagerList.get(3) != null) {
//                    mCodecManagerList.get(3).cleanSurface();
//                }
//                return false;
//            }
//
//            @Override
//            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//
//            }
//        });
    }

    /**
     * 获取ip地址
     *
     * @return
     */
    public String getHostIP() {

        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.i(TAG, "SocketException");
            e.printStackTrace();
        }
        return hostIp;

    }

    /** ************************ 语音识别 ************************** */
    //-------------  v2 -------------------
    protected void handleMsg(Message msg) {

        if (mAsrPushTv != null && (msg.what == STATUS_READY || msg.what == STATUS_SPEAKING || msg.what == STATUS_RECOGNITION || msg.what == 1 )) {
            mAsrPushTv.append(msg.obj.toString() + "\n");
        }
        if (msg.what == STATUS_WAKEUP_SUCCESS) {
            String wakeupWord = msg.obj.toString();
            mAsrPushTv.setText( "【唤醒词】：" + wakeupWord + "\n");  // display the wake up word

            for(num =0 ;num < 4; num++){
                if(wakeupWord.equals(mNumberDJI[num])){ break; }
            }
            if(num <= 4){
                // 此处 开始正常识别流程
//  ---------------*--------- 在线识别，则打开此注释 ------------*-----------begin //
//                Map<String, Object> params = new LinkedHashMap<String, Object>();
//                params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
//                params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
//                // 如识别短句，不需要需要逗号，使用1536搜索模型。其它PID参数请看文档
//                params.put(SpeechConstant.PID, 1536);
//                myRecognizer.cancel();
//                myRecognizer.start(params);
//  ---------------*--------- 在线识别，则打开此注释 ------------*-----------end //
                //            if (backTrackInMs > 0) { // 方案1， 唤醒词说完后，直接接句子，中间没有停顿。
                //                params.put(SpeechConstant.AUDIO_MILLS, System.currentTimeMillis() - backTrackInMs);
                //            }
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(FourInOneActivity.this);
                final Map<String, Object> params = apiParams.fetch(sp);
                myRecognizer.start(params);
            }
            else {
                mAsrPushTv.append( "[唤醒词]：" + "继续休眠" + "\n");
            }
        }
        if(msg.what == 1){                  // msg.what == 1 means msg.obj is the result of recognition defined in "MessageStatusRecogListener"
            String AsrWordGot = "";
            AsrWordGot = msg.obj.toString().substring(0,msg.obj.toString().length()-1);  // display the result without "enter" char
            titleTv.setText(AsrWordGot);
            if(tcpServer != null && num<4) {
                if (AsrWordGot.equals("起飞") || AsrWordGot.equals("弃妃")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("1");
                            try
                            {
                                Thread.currentThread().sleep(5000);//毫秒
                            }
                            catch(Exception e){}
                            tcpServer.SST.get(num).send("3");
                            tcpServer.SST.get(num).send("3190");
                            try
                            {
                                Thread.currentThread().sleep(2000);//毫秒
                            }
                            catch(Exception e){}
                            tcpServer.SST.get(num).send("27");


                        }
                    });
                }
                if (AsrWordGot.equals("降落")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("2");
                        }
                    });
                }
                if (AsrWordGot.equals("前进三")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            //tcpServer.SST.get(num).send("233");
                            tcpServer.SST.get(num).send("23");
                            try
                            {
                                Thread.currentThread().sleep(2500);//毫秒
                            }
                            catch(Exception e){}
                            tcpServer.SST.get(num).send("18");
                        }
                    });
                }
                if (AsrWordGot.equals("前进四")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("23");
                            try
                            {
                                Thread.currentThread().sleep(3000);//毫秒
                            }
                            catch(Exception e){}
                            tcpServer.SST.get(num).send("23");
                            try
                            {
                                Thread.currentThread().sleep(3000);//毫秒
                            }
                            catch(Exception e){}
                            tcpServer.SST.get(num).send("18");
                        }
                    });
                }
                if (AsrWordGot.equals("前进一")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                           // tcpServer.SST.get(num).send("3");
//                                sst.send("17");
                            tcpServer.SST.get(num).send("23");
                        }
                    });
                }
                if (AsrWordGot.equals("悬停")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("3");
                            tcpServer.SST.get(num).send("18");
                        }
                    });
                }
                if (AsrWordGot.equals("拍照")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("14");
                            try
                            {
                                Thread.currentThread().sleep(3000);//毫秒
                            }
                            catch(Exception e){}
                            tcpServer.SST.get(num).setPictureIndex(photoIdx);
                            tcpServer.SST.get(num).send("20");
                        }
                    });
                }
                if (AsrWordGot.equals("打开摄像头")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("131");
                        }
                    });
                }
                if (AsrWordGot.equals("关闭摄像头")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("130");
                        }
                    });
                }
                if (AsrWordGot.equals("俯视九十度")||AsrWordGot.equals("俯视90度")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("3");
//                            tcpServer.SST.get(num).send("12");
                            tcpServer.SST.get(num).send("3190");

                        }
                    });
                }
                if (AsrWordGot.equals("俯视四十五度")||AsrWordGot.equals("俯视45度")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("3");
                            tcpServer.SST.get(num).send("3145");
                        }
                    });
                }
                if (AsrWordGot.equals("打开指示灯")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("41");
                        }
                    });
                }
                if (AsrWordGot.equals("关闭指示灯")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("40");
                        }
                    });
                }
                if (AsrWordGot.equals("向右转动")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("160");
                        }
                    });
                }
                if (AsrWordGot.equals("向左转动")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("3");
                            tcpServer.SST.get(num).send("161");
                        }
                    });
                }
                if (AsrWordGot.equals("方案一")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
//                            tcpServer.SST.get(num).send("131");
//                            setPointBtn();
                            tcpServer.SST.get(num).send("25");
                        }
                    });
                }
                if (AsrWordGot.equals("方案二")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
//                            tcpServer.SST.get(num).send("3");
//                            tcpServer.SST.get(num).send("17");
                            tcpServer.SST.get(num).send("35");
                        }
                    });
                }
                if (AsrWordGot.equals("方案三")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
//                            tcpServer.SST.get(num).send("3");
//                            tcpServer.SST.get(num).send("17");
                            tcpServer.SST.get(num).send("26");
                        }
                    });
                }
                if (AsrWordGot.equals("方案四")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
//                            tcpServer.SST.get(num).send("3");
//                            tcpServer.SST.get(num).send("17");
                            tcpServer.SST.get(num).send("36");
                        }
                    });
                }
                if (AsrWordGot.equals("上升")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
//                            tcpServer.SST.get(num).send("3");
//                            tcpServer.SST.get(num).send("17");
                            tcpServer.SST.get(num).send("21");
                        }
                    });
                }
                if(AsrWordGot.equals("下降")){
//                    int tempShowIndex = ShowIndex.get(num) + 1;
//                    ShowIndex.remove(num);
//                    ShowIndex.add(num,tempShowIndex );

//                    setPointBtn();
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
//                            tcpServer.SST.get(num).send("3");
//                            tcpServer.SST.get(num).send("17");
                            tcpServer.SST.get(num).send("221");
                        }
                    });
                }
                if (AsrWordGot.equals("搜索")) { //下降2m
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            tcpServer.SST.get(num).send("222");
                        }
                    });
                }
                if (AsrWordGot.equals("前进二")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
//                            tcpServer.SST.get(num).send("151");
                            tcpServer.SST.get(num).send("232");
                        }
                    });
                }
                if (AsrWordGot.equals("返回")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
//                            tcpServer.SST.get(num).send("191");
                            tcpServer.SST.get(num).send("24");
                        }
                    });
                }
                if (AsrWordGot.equals("回传")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
//                            tcpServer.SST.get(num).send("191");
                            tcpServer.SST.get(num).send("20");
                        }
                    });
                }
                if (AsrWordGot.equals("开始飞行")) {
                    //切换活动
                    Intent intent =new Intent();
                    intent.setClass(FourInOneActivity.this, qrCodeDetect.class);
                    startActivity(intent);
//                    exec.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            tcpServer.SST.get(num).send("151");
//                        }
//                    });
                }
                if (AsrWordGot.equals("删除")) {
                    photoIdx--;
                }
            }
            else if(tcpServer != null && num==4){
                for(final TcpServer.ServerSocketThread sst : tcpServer.SST){
                    if (AsrWordGot.equals("起飞") || AsrWordGot.equals("弃妃")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("1");
                                try
                                {
                                    Thread.currentThread().sleep(5000);//毫秒
                                }
                                catch(Exception e){}
                                sst.send("3");
                                sst.send("3190");
                                try
                                {
                                    Thread.currentThread().sleep(2000);//毫秒
                                }
                                catch(Exception e){}
                                sst.send("27");
                            }
                        });
                    }
                    if (AsrWordGot.equals("降落")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("2");
                            }
                        });
                    }
                    if (AsrWordGot.equals("前进三")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("23");
                                try
                                {
                                    Thread.currentThread().sleep(2500);//毫秒
                                }
                                catch(Exception e){}
                                sst.send("18");
                            }
                        });
                    }
                    if (AsrWordGot.equals("前进四")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("23");
                                try
                                {
                                    Thread.currentThread().sleep(3000);//毫秒
                                }
                                catch(Exception e){}
                                sst.send("23");
                                try
                                {
                                    Thread.currentThread().sleep(3000);//毫秒
                                }
                                catch(Exception e){}
                                sst.send("18");
                            }
                        });
                    }
                    if (AsrWordGot.equals("前进一")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("3");
//                                sst.send("17");
                                sst.send("231");
                            }
                        });
                    }
                    if (AsrWordGot.equals("悬停")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("3");
                                sst.send("18");
                            }
                        });
                    }
                    if (AsrWordGot.equals("拍照")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("14");
                                try
                                {
                                    Thread.currentThread().sleep(3000);//毫秒
                                }
                                catch(Exception e){}
                                sst.setPictureIndex(photoIdx);
                                sst.send("20");
                            }
                        });
                    }
                    if (AsrWordGot.equals("打开摄像头")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("131");
                            }
                        });
                    }
                    if (AsrWordGot.equals("关闭摄像头")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("130");
                            }
                        });
                    }
                    if (AsrWordGot.equals("俯视九十度")||AsrWordGot.equals("俯视90度")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("3");
                                sst.send("3190");
                            }
                        });
                    }
                    if (AsrWordGot.equals("俯视四十五度")||AsrWordGot.equals("俯视45度")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("3");
                                sst.send("3145");
                            }
                        });
                    }
                    if (AsrWordGot.equals("打开指示灯")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("41");
                            }
                        });
                    }
                    if (AsrWordGot.equals("关闭指示灯")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("40");
                            }
                        });
                    }
                    if (AsrWordGot.equals("向右转动")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("160");
                            }
                        });
                    }
                    if (AsrWordGot.equals("向左转动")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("3");
                                sst.send("161");
                            }
                        });
                    }
                    if (AsrWordGot.equals("方案一")) {  //向左平移0.5m
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
//                                sst.send("131");
//                                setPointBtn();
                                sst.send("25");

                            }
                        });
                    }
                    if (AsrWordGot.equals("方案二")) {  //向左平移2m
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
//                                sst.send("3");
//                                sst.send("17");
                                sst.send("35");
                            }
                        });
                    }
                    if (AsrWordGot.equals("方案三")) {  //向右平移0.5m
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
//                                sst.send("3");
//                                sst.send("17");
                                sst.send("26");
                            }
                        });
                    }
                    if (AsrWordGot.equals("方案四")) {  //向右平移2m
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
//                                sst.send("3");
//                                sst.send("17");
                                sst.send("36");
                            }
                        });
                    }
                    if (AsrWordGot.equals("上升")) {  //向上平移
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
//                                sst.send("3");
//                                sst.send("17");
                                sst.send("21");
                            }
                        });
                    }
                    if(AsrWordGot.equals("下降")){  //向下平移
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
//                                sst.send("3");
//                                sst.send("17");
                                sst.send("221");
                            }
                        });

                    }
                    if(AsrWordGot.equals("搜索")){  //向下平移2m
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
//                                sst.send("3");
//                                sst.send("17");
                                sst.send("222");
                            }
                        });

                    }
                    if (AsrWordGot.equals("前进二")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
//                                sst.send("151");
                                sst.send("232");
                            }
                        });
                    }
                    if (AsrWordGot.equals("返回")) {
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
//                                sst.send("191");
                                sst.send("24");
                            }
                        });
                    }
                    if (AsrWordGot.equals("回传")) {  //删除当前最新保存的图片
                        exec.execute(new Runnable() {
                            @Override
                            public void run() {
                                sst.send("28");
                            }
                        });
                    }
                    if (AsrWordGot.equals("开始飞行")) {
                        Intent intent =new Intent();
                        intent.setClass(FourInOneActivity.this, qrCodeDetect.class);
                        startActivity(intent);
                    }
                    if (AsrWordGot.equals("删除")) {
                        photoIdx--;
                    }
                }
            }
        }

    }

    private void start() {
        mAsrPushTv.setText("");
        Map<String, Object> params = new HashMap<String, Object>();
        String[] filePath = {"assets:///WakeUp-QuanDong12ShangXia.bin","assets:///WakeUp-dji-012.bin","assets:///WakeUp-baidu.bin"};
        params.put(SpeechConstant.WP_WORDS_FILE, filePath[0]);
        myWakeup.start(params);
        printLog("输入参数：" + params);
    }

    private void stop() {
        mAsrPushTv.setText("");
        myWakeup.stop(); //
        myRecognizer.stop();
    }

    private void printLog(String text) {
        if (true) {
            text += "  ;time=" + System.currentTimeMillis();
        }
        text += "\n";
        Log.i(getClass().getName(), text);
        mAsrPushTv.append(text + "\n");
    }

/** ************************************二维码识别任务******************************************/
    /**************************二维码识别*****************************/
    private String file_path = "/mission8/1m-jpg/";
    private void qrCodeRecognization() throws FileNotFoundException {
        File photo = new File(Environment.getExternalStorageDirectory().getPath() + file_path);
        FileInputStream in = new FileInputStream(photo.getPath()+"/intactCode.png");
        Bitmap bitmap  = BitmapFactory.decodeStream(in);
        String result = decodeFromPhoto(bitmap);
        if (TextUtils.isEmpty(result)) {
            Toast.makeText(getApplicationContext(), "未识别出二维码内容！", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(FourInOneActivity.this,result,Toast.LENGTH_SHORT).show();
    }

//    public List<String> hints;
//    public String qrCodeDecode(){
//        /* 二维码一键识别、拼接、解码入口函数
//         * 输入：~（请确认将四张拍好的破碎二维码图片存储到固定路径即可）
//         * 输出：二维码解码出来的四位数的string，算法提示
//         **/
//        String decodeResult = "";
//        //[1]实例化一个拼接类，构造时自动从固定路径读图
//        detectAndJoint jointer=new detectAndJoint();
//        //根据提示数量，查看是否读取成功
//        if (jointer.getHints().size()==0)
//        {
//            try {
//                //[2]调用新线程进行一键拼接
//                Boolean jointFlag = jointer.onekeyJoint();
//                //[3]如果拼接成功，获取拼接好的intactQrcode和result
//                if (jointFlag) {
//                    try {
//                        qrCodeRecognization();
//                        decodeResult=jointer.getResult();//获取解码结果
//                        hints=jointer.getHints();//获取算法提示
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }catch (Exception e){ }
//        }
//
//        return decodeResult;
//    }

}