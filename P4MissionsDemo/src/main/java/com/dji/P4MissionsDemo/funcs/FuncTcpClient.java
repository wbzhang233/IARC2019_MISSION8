package com.dji.P4MissionsDemo.funcs;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.speech.EventManager;
import com.baidu.speech.asr.SpeechConstant;
import com.dji.P4MissionsDemo.DJIDemoApplication;
import com.dji.P4MissionsDemo.DemoBaseActivity;
import com.dji.P4MissionsDemo.FourInOneActivity;
import com.dji.P4MissionsDemo.R;
import com.dji.P4MissionsDemo.Utils;
import com.dji.P4MissionsDemo.coms.TcpClient;
import com.dji.P4MissionsDemo.control.MyRecognizer;
import com.dji.P4MissionsDemo.control.MyWakeup;
import com.dji.P4MissionsDemo.media.DJIVideoStreamDecoder;
import com.dji.P4MissionsDemo.qrdetect.detectAndJoint;
import com.dji.P4MissionsDemo.qrdetect.qrCodeDetect;
import com.dji.P4MissionsDemo.recognization.CommonRecogParams;
import com.dji.P4MissionsDemo.recognization.IStatus;
import com.dji.P4MissionsDemo.recognization.MessageStatusRecogListener;
import com.dji.P4MissionsDemo.recognization.StatusRecogListener;
import com.dji.P4MissionsDemo.recognization.offline.OfflineRecogParams;
import com.dji.P4MissionsDemo.tensorflow.DetectorAPI;
import com.dji.P4MissionsDemo.tensorflow.OverlayView;
import com.dji.P4MissionsDemo.udp.UDPServer;
import com.dji.P4MissionsDemo.udp.handleReceiveData;
import com.dji.P4MissionsDemo.wakeup.IWakeupListener;
import com.dji.P4MissionsDemo.wakeup.RecogWakeupListener;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.ControlMode;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.ObstacleDetectionSector;
import dji.common.flightcontroller.RemoteControllerFlightMode;
import dji.common.flightcontroller.VisionControlState;
import dji.common.flightcontroller.VisionDetectionState;
import dji.common.flightcontroller.VisionSystemWarning;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.Attitude;
import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.GimbalState;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.mission.tapfly.TapFlyExecutionState;
import dji.common.mission.tapfly.TapFlyMission;
import dji.common.mission.tapfly.TapFlyMissionState;
import dji.common.mission.tapfly.TapFlyMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.common.util.DJIParamCapability;
import dji.sdk.camera.Camera;
import dji.sdk.camera.DownloadListener;
import dji.sdk.camera.MediaFile;
import dji.sdk.camera.MediaManager;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.mission.tapfly.TapFlyMissionEvent;
import dji.sdk.mission.tapfly.TapFlyMissionOperator;
import dji.sdk.mission.tapfly.TapFlyMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

import static com.dji.P4MissionsDemo.DJIDemoApplication.getGimbalInstance;
import static com.dji.P4MissionsDemo.DJIDemoApplication.getProductInstance;
import static com.dji.P4MissionsDemo.qrdetect.QRCodeUtil.decodeFromPhoto;
import static org.opencv.imgcodecs.Imgcodecs.imencode;

/**
 * Created by Jason Zhu on 2017-04-24.
 * Email: cloud_happy@163.com
 */

public class FuncTcpClient extends DemoBaseActivity implements SurfaceTextureListener, View.OnClickListener,IStatus,DJICodecManager.YuvDataCallback{

    public static int PlanControl = 1;    // 用于控制不同方案的代码
    // 方案1，2: 使用TCP通讯协议，图像四合一至平板，持平板入场
    // 方案3： 不利用进行图像传输，但是可以TCP进行急停指令控制，持遥控器端入场
    // 方案4： 利用UDP进行图像传输，利用TCP进行指令传输，持平板入场

    private boolean plan3VideoEnable = true;
    private TapFlyMission mTapFlyMission;

    private float altitude;
    private float heightUltr;
    private Float altitudeMax = (float) 1.5;
    private boolean mStart = false;
    private int mState=0;
    private float debugYawdji1 = 0;
    private int [] rgbBytes;
    private byte[] rgbByteArray;
    private ByteArrayOutputStream rgbBAOS = null;   
    private int mYUVWidth,mYUVHeight,mYUVdatasize;

    private Camera camera;
    private ProgressDialog mDownloadDialog;
    File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/DJI_ScreenShot/");
    private int currentProgress = -1;
    private MediaManager mMediaManager;
    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();

    public Handler handler,mHandler;
    public static final String FLAG_FlyCommand_Down = "dji_sdk_FLAG_FlyCommand_Down";
    private LayoutInflater mInflater;
    private View connectWifi;
    private View showVideo;
    private FlightController mFlightController;
    private Gimbal mGimbal;
    private float mPitch;
    private float mRoll;
    private float mYaw;
    private float mThrottle;
    private Timer mSendVirtualStickDataTimer;
    private Timer mSendlandingTimer;
    private SendlandingTask mSendlandingTask;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;
    private boolean mVirtualStickEnabled = false;

    private String  upwardsAvoidance ;
    private String  collisionAvoidance ;
    private String  activiteAvoidance ;
    private String  smartCapture ;
    private String  TerrainFollowMode ,Simulatorstate,controlCommand ;
    private String  VisionAssistedPosition ,systemWarning,visionSensorPosition,obstacleDistanceMeters ,distanceSectors;
    private String braking,AdvancePilotAssistanceSystemactive,landingprotectionstate,performinglanding,activiteavoide;
    private String gimbalPitch,gimbalRoll,gimbalYaw,gimbalmotor,gimbalMode,gimbalAdjustPitch;
    private int Tasktimes=0,videoBufferSize = 0,TasktimesMax = 10;
    private int landingTasktimes=0,LedTasktimes = 0;
    private int grayvalue=230 ,count;
    private boolean activateAvoid=false ,isUDPvideoRun = false,isUDPFrameOk = false, isFrameOk = false,ish264OutputRun = false, isVidoOutputRun = false,readytoSendh264 = false,keyfinished = false;
    private int DebugCFtimes = 0;
    private int mNumberInitial = 1;  //
    private ByteArrayOutputStream baos = null;

    private byte[] videoh264buffer = null;
    private String[] mNumberDJI = {"全体都有","洞幺洞幺","洞两洞两","上一首","下一首"};

    private ImageButton mPushDrawerIb;
    private SlidingDrawer mPushDrawerSd;
    private Button mStartBtn,mWifiBtn;
    private ImageButton mStopBtn;
    private TextView mPushTv;
    private TextView mAsrPushTv,mSimulatorPushTv;
    private RelativeLayout mBgLayout;
    private ImageView mPhotoTakedIv,mRstPointIv;
    private TextView mAsrTv,mAssisTv;
    private Switch mAssisSw,mAsrSw,mWifiSw;
    private TextView mNumberTv,mSpeedTv;
    private SeekBar mSpeedSb;
    private TextureView mSendTestTtv;

    private OverlayView mOverlayView_tracking;
    private FrameLayout mFrameLayout;
//    public SurfaceView mVideoShowImageFrame;

    private String AsrWordGot;
    private EventManager wakeup;

    private String TAG = "FuncTcpClient";

    @SuppressLint("StaticFieldLeak")
    public static Context context ;
    private Button btnStartClient,btnCloseClient, btnCleanClientSend, btnCleanClientRcv,btnClientSend,btnQuickConnect,btnClientRandom;
    private TextView txtRcv,txtSend;
    private EditText editClientSend,editClientID, editClientPort,editClientIp;
    private static TcpClient tcpClient = null;
//    private OutputStream mOutputStream;
//    private MyBtnClicker myBtnClicker = new MyBtnClicker();
    private final MyHandler myHandler = new MyHandler(this);
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private MyBroadcastReceiver myBroadcastReceiverPoint = new MyBroadcastReceiver();
    ExecutorService exec = Executors.newCachedThreadPool();

    protected MyRecognizer myRecognizer;
    protected MyWakeup myWakeup;
    protected CommonRecogParams apiParams;
    protected boolean enableOffline = true;
    private  DetectorAPI mDetectorAPI;
    private  List<RectF> mDetectorResult;
    private  int ShowIndex = 0;

    private class MyHandler extends android.os.Handler{
        private WeakReference<FuncTcpClient> mActivity;

        MyHandler(FuncTcpClient activity){
            mActivity = new WeakReference<FuncTcpClient>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mActivity != null){
                switch (msg.what){
                    case 1:
                        txtRcv.append(msg.obj.toString());
                        myFlightController(msg.obj.toString());
                        break;
                    case 2:
                        txtSend.append(msg.obj.toString());
                        break;
                }
            }
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String mAction = intent.getAction();
            switch (mAction){
                case "tcpClientReceiver":
                    String msg = intent.getStringExtra("tcpClientReceiver");
                    Message message = Message.obtain();
                    message.what = 1;
                    message.obj = msg;
                    myHandler.sendMessage(message);
                    break;
                case "pintPosition":
                    if (mTapFlyMission != null) {
                        float pointPosX = intent.getFloatExtra("pintPosX", 0);
                        float pointPoxY = intent.getFloatExtra("pintPosY", 0);
                        mStartBtn.setVisibility(View.VISIBLE);
                        final View parent = (View) mStartBtn.getParent();
                        mStartBtn.setX(pointPosX * parent.getWidth());
                        mStartBtn.setY(pointPoxY * parent.getHeight());
                        mStartBtn.requestLayout();
                        mTapFlyMission.target = getTapFlyPoint(mStartBtn);
                        getTapFlyOperator().setAutoFlightSpeed(1, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError error) {
                                showToast(error == null ? "Set Auto Flight Speed Success" : error.getDescription());
                            }
                        });
                    } else {
                        showToast("TapFlyMission is null");
                    }
                    break;
            }
        }
    }

    private int getPort(String msg){
        if (msg.equals("")){
            msg = "1234";
        }
        return Integer.parseInt(msg);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initViews();
        Log.i("Initial","finished");
        super.onCreate(savedInstanceState);
        // 为传输camera得到的H264数据，需修改DemoBaseActivity 中的mReceivedVideoDataCallBack
//        boolean load = OpenCVLoader.initDebug();
//        mSimulatorPushTv.append("Opencv Enable:"+(load?"sucess":"false\n"));

//        super.mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback() {
//
//            @Override
//            public void onReceive(final byte[] videoBuffer,final int size) {
//                mByte = videoBuffer;
//
//                AsyncTask.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        if(!readytoSendh264) {
//                            videoh264buffer = videoBuffer;
//                            videoBufferSize = size;
//                            readytoSendh264 = true;
//                        }
//                    }
//                });
//                if(mCodecManager != null){
//                    mCodecManager.sendDataToDecoder(videoBuffer, size);
//                }
//            }
//        };

        mHandler = new Handler(Looper.getMainLooper());
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
        context = this;

//        wakeup = EventManagerFactory.create(this, "wp");
//        wakeup.registerListener(this); //  EventListener 中 onEvent方法

        StatusRecogListener recogListener = new MessageStatusRecogListener(handler);
        // 改为 SimpleWakeupListener 后，不依赖handler，但将不会在UI界面上显示
        myRecognizer = new MyRecognizer(this, recogListener);

        IWakeupListener listener = new RecogWakeupListener(handler);
        myWakeup = new MyWakeup(this, listener);

        apiParams = getApiParams();
        if (enableOffline) {
            myRecognizer.loadOfflineEngine(OfflineRecogParams.fetchOfflineParams());
        }

        //TapFlyMissionOnCreate();
        bindListener();
        bindReceiver();
        //initialUDP();
//        Ini();
        camera = DJIDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {

                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        showToast("Switch Camera Mode Succeeded");
                    }
                }
            });
        }
        //Init Download Dialog
        mDownloadDialog = new ProgressDialog(FuncTcpClient.this);
        mDownloadDialog.setTitle("Downloading file");
        mDownloadDialog.setIcon(android.R.drawable.ic_dialog_info);
        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDownloadDialog.setCanceledOnTouchOutside(false);
        mDownloadDialog.setCancelable(true);
        mDownloadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (mMediaManager != null) {
                    mMediaManager.exitMediaDownloading();
                }
            }
        });
        Log.e(TAG, "onCreateclient");
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        mOverlayView_tracking =  (OverlayView) findViewById(R.id.DJI_tracking_overlay);
//        mVideoShowImageFrame=(SurfaceView) findViewById(R.id.video_previewer_surface);
        Context ctx = this;
//        if(PlanControl == 3) {
//            mDetectorAPI = new DetectorAPI(ctx, mOverlayView_tracking, rotation);
//        }

    }

    protected CommonRecogParams getApiParams() {
        return new OfflineRecogParams(this);
    }

    //---------**------------------------------ use the UDP protocol ------------------**----------//
    private UDPServer mUDPserver;

    private void initialUDP(){
        if(FuncTcpClient.PlanControl == 1){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mUDPserver = new UDPServer(7777);
                        mUDPserver.setReceiveCallback(new handleReceiveData() {
                            @Override
                            public void handleReceive(byte[] data) {

                            }

                            @Override
                            public void handleReceive(final byte[] data,final String ipAddress) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                    }
                                });
                            }
                        });
                        mUDPserver.start(); // 手机端不接收平板的图像，控制指令通过TCP进行传输
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
/*-----------------------------------------------------------语音识别----------------------------start */
//-------------  v2 -------------------
    protected void handleMsg(Message msg) {
        if (mAsrPushTv != null && (msg.what == STATUS_READY || msg.what == STATUS_SPEAKING || msg.what == STATUS_RECOGNITION || msg.what == 1 )) {
            mAsrPushTv.append(msg.obj.toString() + "\n");
        }
        if (msg.what == STATUS_WAKEUP_SUCCESS) {
            String wakeupWord = msg.obj.toString();
            mAsrPushTv.setText( "【唤醒词】：" + wakeupWord + "\n");  // display the wake up word
            if(wakeupWord.equals(mNumberDJI[mNumberInitial]) || wakeupWord.equals(mNumberDJI[0])){
                // 此处 开始正常识别流程
//                Map<String, Object> params = new LinkedHashMap<String, Object>();
//                params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
//                params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
//                // 如识别短句，不需要需要逗号，使用1536搜索模型。其它PID参数请看文档
//                params.put(SpeechConstant.PID, 1536);
//                myRecognizer.cancel();
//                myRecognizer.start(params);
    //            if (backTrackInMs > 0) { // 方案1， 唤醒词说完后，直接接句子，中间没有停顿。
    //                params.put(SpeechConstant.AUDIO_MILLS, System.currentTimeMillis() - backTrackInMs);
    //            }
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                final Map<String, Object> params = apiParams.fetch(sp);
                myRecognizer.start(params);

            }
            else {
                mAsrPushTv.append( "[唤醒词]：" + "继续休眠" + "\n");
            }
        }
        if(msg.what == 1){                  // msg.what == 1 means msg.obj is the result of recognition defined in "MessageStatusRecogListener"
            AsrWordGot = msg.obj.toString().substring(0,msg.obj.toString().length()-1);  // display the result without "enter" char
            mSpeedTv.setText(AsrWordGot);
            try {
                if (AsrWordGot.equals("起飞") || AsrWordGot.equals("弃妃")) {
//                    if (mFlightController != null) {
//                        mFlightController.startTakeoff(
//                                new CommonCallbacks.CompletionCallback() {
//                                    @Override
//                                    public void onResult(DJIError djiError) {
//                                        if (djiError != null) {
//                                            showToast(djiError.getDescription());
//                                        } else {
//                                            showToast("Take off Success");
//                                        }
//                                    }
//                                }
//                        );
//                    }
                    myFlightController("1");
                    try
                    {
                        Thread.currentThread().sleep(5000);//毫秒
                    }
                    catch(Exception e){}
                    myFlightController("3");
                    myFlightController("3190");
                    try
                    {
                        Thread.currentThread().sleep(2000);//毫秒
                    }
                    catch(Exception e){}
                    myFlightController("27");
                }
                if (AsrWordGot.equals("降落")) {
                    djiLanding();
                }
                if (AsrWordGot.equals("前进三")) {
                    djiStartControl();
                    myFlightController("233");
                }
                if (AsrWordGot.equals("前进二")) {
                    djiStartControl();
                    myFlightController("232");
                }
                if (AsrWordGot.equals("前进一")) {
                    djiStartControl();
                    myFlightController("231");
                }
                if (AsrWordGot.equals("方案一")) {
                    djiStartControl();
                    myFlightController("25");
                }
                if (AsrWordGot.equals("方案二")) {
                    djiStartControl();
                    myFlightController("35");
                }
                if (AsrWordGot.equals("方案三")) {
                    djiStartControl();
                    myFlightController("26");
                }
                if (AsrWordGot.equals("方案四")) {
                    djiStartControl();
                    myFlightController("36");
                }
                if (AsrWordGot.equals("上升")) {
                    djiStartControl();
                    myFlightController("21");
                }
                if (AsrWordGot.equals("下降")) {
                    djiStartControl();
                    myFlightController("221");
                }
                if (AsrWordGot.equals("搜索")) {
                    djiStartControl();
                    myFlightController("222");
                }
                if (AsrWordGot.equals("返回")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myFlightController("3");
                            myFlightController("24");
                        }
                    });
                }
                if (AsrWordGot.equals("悬停")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myFlightController("3");
                            myFlightController("18");
                        }
                    });
                }
                if (AsrWordGot.equals("拍照")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myFlightController("14");
                            //延迟若干秒进行
                            Timer timer = new Timer();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    myFlightController("20");
                                }
                            }, 3000);

                        }
                    });
                }
                if (AsrWordGot.equals("俯视九十度")||AsrWordGot.equals("俯视90度")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myFlightController("3");
                            myFlightController("3190");
                        }
                    });
                }
                if (AsrWordGot.equals("俯视四十五度")||AsrWordGot.equals("俯视45度")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myFlightController("3");
                            myFlightController("3145");
                        }
                    });
                }
                if (AsrWordGot.equals("打开指示灯")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myFlightController("41");
                        }
                    });
                }
                if (AsrWordGot.equals("关闭指示灯")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myFlightController("40");
                        }
                    });
                }
                if (AsrWordGot.equals("向右转动")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myFlightController("3");
                            myFlightController("160");
                        }
                    });
                }
                if (AsrWordGot.equals("向左转动")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            myFlightController("3");
                            myFlightController("161");
                        }
                    });
                }
                if (AsrWordGot.equals("破译密码")) {
                    exec.execute(new Runnable() {
                        @Override
                        public void run() {
                            //切换活动
                            Intent intent =new Intent();
                            intent.setClass(FuncTcpClient.this, qrCodeDetect.class);
                            startActivity(intent);
                        }
                    });
                }
                if (AsrWordGot.equals("删除")) {
                    qrcode_idx--;
                    String text=String.format("qrcodeIdx:%d",qrcode_idx);
                    showToast("已删除！"+text);
                }

            }
            catch (Exception e){
                showToast("something wrong in asr command analysis");
            }
        }

    }

    private void start() {
        mAsrPushTv.setText("");
        Map<String, Object> params = new HashMap<String, Object>();
        String[] filePath = {"assets:///WakeUp-QuanDong12ShangXia.bin","assets:///WakeUp-dji-012.bin"};
        params.put(SpeechConstant.WP_WORDS_FILE, filePath[mNumberInitial-1]);
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

    private void setPointBtn() {
        if (PlanControl == 3) {
            mDetectorResult = mDetectorAPI.getResults();

            if (mDetectorResult != null && ShowIndex >= mDetectorResult.size()) {
                ShowIndex = 0;
            }

            if (mDetectorResult != null && ShowIndex < mDetectorResult.size()) {

                View parent = (View) mStartBtn.getParent();
                float centerX = mDetectorResult.get(ShowIndex).centerX();
                float centerY = mDetectorResult.get(ShowIndex).centerY();

                centerX = centerX < 0 ? 0 : centerX;
                centerX = centerX > parent.getWidth() ? parent.getWidth() : centerX;
                centerY = centerY < 0 ? 0 : centerY;
                centerY = centerY > parent.getHeight() ? parent.getHeight() : centerY;

                mStartBtn.setVisibility(View.VISIBLE);
                mStartBtn.setX(centerX);
                mStartBtn.setY(centerY);
                mStartBtn.requestLayout();
                mTapFlyMission.target = getTapFlyPoint(mStartBtn);
                getTapFlyOperator().setAutoFlightSpeed(1, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        showToast(error == null ? "Set Auto Flight Speed Success" : error.getDescription());
                    }
                });

            }
        }
    }

// -----------------------------------------------------------语音识别----------------------------end

    private void bindReceiver(){
        IntentFilter intentFilter = new IntentFilter("tcpClientReceiver");
        registerReceiver(myBroadcastReceiver,intentFilter);
        IntentFilter intentFilterPoint = new IntentFilter("pintPosition");
        registerReceiver(myBroadcastReceiverPoint,intentFilterPoint);
    }

    private void initViews() {
        //初始化布局加载器
        mInflater = LayoutInflater.from(this);
        //加载wifi连接页面
        connectWifi = mInflater.inflate(R.layout.tcp_client, null);
        //加载飞行视频播放页面
        showVideo =mInflater.inflate(R.layout.activity_zty, null);
        //调用setContentView(View view)方法，传入一个View
        setContentView(showVideo);
        //**********************wifi连接页面中的控件**************************
        btnStartClient = (Button) connectWifi.findViewById(R.id.btn_tcpClientConn);
        btnCloseClient = (Button) connectWifi.findViewById(R.id.btn_tcpClientClose);
        btnCleanClientRcv = (Button) connectWifi.findViewById(R.id.btn_tcpCleanClientRecv);
        btnCleanClientSend = (Button) connectWifi.findViewById(R.id.btn_tcpCleanClientSend);
        btnClientRandom = (Button) connectWifi.findViewById(R.id.btn_tcpClientRandomID);

        btnClientSend = (Button) connectWifi.findViewById(R.id.btn_tcpClientSend);
        editClientPort = (EditText) connectWifi.findViewById(R.id.edit_tcpClientPort);
        editClientIp = (EditText) connectWifi.findViewById(R.id.edit_tcpClientIp);
        editClientSend = (EditText) connectWifi.findViewById(R.id.edit_tcpClientSend);
        txtRcv = (TextView) connectWifi.findViewById(R.id.txt_ClientRcv);
        txtSend = (TextView) connectWifi.findViewById(R.id.txt_ClientSend);
        //**********************飞行视频播放页面中的控件**************************
        mPhotoTakedIv = (ImageView)showVideo.findViewById(R.id.takedPhoto_iv);
        btnQuickConnect = (Button) showVideo.findViewById(R.id.btnQuickConnect);
        mPushDrawerIb = (ImageButton)showVideo.findViewById(R.id.pointing_drawer_control_ib);
        mPushDrawerSd = (SlidingDrawer)showVideo.findViewById(R.id.pointing_drawer_sd);
        mStartBtn = (Button)showVideo.findViewById(R.id.pointing_start_btn);
        mWifiBtn= (Button)showVideo.findViewById(R.id.btnWifiConnection);
        mStopBtn = (ImageButton)showVideo.findViewById(R.id.pointing_stop_btn);
        mPushTv = (TextView)showVideo.findViewById(R.id.pointing_push_tv);
        mSimulatorPushTv = (TextView)showVideo.findViewById(R.id.simulator_push_tv);
        mAsrPushTv = (TextView)showVideo.findViewById(R.id.ASR_push_tv);

        mBgLayout = (RelativeLayout)showVideo.findViewById(R.id.pointing_bg_layout);
        mRstPointIv = (ImageView)showVideo.findViewById(R.id.pointing_rst_point_iv);
        mAssisTv = (TextView)showVideo.findViewById(R.id.pointing_assistant_tv);    //
        mAssisSw = (Switch)showVideo.findViewById(R.id.pointing_assistant_sw);      // used to switch between simulator and real controler
        mAsrTv = (TextView)showVideo.findViewById(R.id.asr_state_tv);
        mAsrSw = (Switch)showVideo.findViewById(R.id.asr_state_sw);             // used to turn on or off the Speaking control

        mWifiSw = (Switch)showVideo.findViewById(R.id.wifi_connection_sw);
        mNumberTv = (TextView)showVideo.findViewById(R.id.textView);              // use to display the number of DJI
        mSpeedTv = (TextView)showVideo.findViewById(R.id.pointing_speed_tv);  // use to display the word from Speaker
        mSpeedSb = (SeekBar)showVideo.findViewById(R.id.pointing_speed_sb);   // use to configure the APP for different DJI
        mFrameLayout = (FrameLayout)showVideo.findViewById(R.id.imageV_OverlayV_layout);

        setVisible(btnCloseClient,false);
        setVisible(btnClientSend,false);
        setVisible(mRstPointIv,false);
        setVisible(mStopBtn,true);

    }

    private void bindListener(){
        btnStartClient.setOnClickListener(this);
        btnCloseClient.setOnClickListener(this);
        btnCleanClientRcv.setOnClickListener(this);
        btnCleanClientSend.setOnClickListener(this);
        btnClientRandom.setOnClickListener(this);
        btnClientSend.setOnClickListener(this);
        mWifiBtn.setOnClickListener(this);

        mNumberTv.setOnClickListener(this);
        mPushDrawerIb.setOnClickListener(this);
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);

        btnQuickConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(PlanControl == 1||PlanControl == 2 || PlanControl == 4) {
                    try {
                        if (tcpClient == null || !tcpClient.isConnecting()) {
                            tcpClient = new TcpClient(editClientIp.getText().toString(), getPort(editClientPort.getText().toString()));
                            exec.execute(tcpClient);
                        }
                    } catch (Exception e) {
                        showToast(e.toString());
                    }
                }
                else if(PlanControl == 3) {
                    new mobilePhoneDetectorDebug(true).start();
                }
                else if(PlanControl == 4){
                    new sendPictureThroughUDP().start();
                }
//                captureAction();

//                new sendPictureThroughWiFi().start();



//                FuncTcpClient.this.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        mSimulatorPushTv.append(mDetectorAPI.getState());
//                    }
//                });
            }
        });
        mWifiSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean modeChoose = true; // 0 -- h264 ; 1 --- jpeg   tcpClient != null &&
//                if( isChecked && !modeChoose){
//                    isVidoOutputRun = false;
//                    ish264OutputRun = true;
//                    FuncTcpClient.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mSimulatorPushTv.append("enable send H264 Data\n");
//                        }
//                    });
//                    exec.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            tcpClient.send("SURF");
//                        }
//                    });
//                    new h264Output(tcpClient.getmOutputstream()).start();
//                }
//                if(isChecked && modeChoose){
//                    mCodecManager.enabledYuvData(true);
//                    mCodecManager.setYuvDataCallback(FuncTcpClient.this);
//
//                    FuncTcpClient.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mSimulatorPushTv.append("enable YUV Data\n");
//                            mFrameLayout.setVisibility(View.VISIBLE);
//                            mFrameLayout.requestLayout();
//
//                            mVideoSurface.setVisibility(View.GONE);
//                            mVideoSurface.requestLayout();
//                        }
//                    });
//
//                }
//

                if(tcpClient != null && isChecked && modeChoose){
                    mCodecManager.enabledYuvData(true);
                    mCodecManager.setYuvDataCallback(FuncTcpClient.this);
//                    FuncTcpClient.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mVideoShowImageFrame.setVisibility(View.VISIBLE);
//                            FuncTcpClient.super.mVideoSurface.setVisibility(View.GONE);
//                        }
//                    });
                    FuncTcpClient.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSimulatorPushTv.append("enable YUV Data\n");
//                            mFrameLayout.setVisibility(View.VISIBLE);
//                            mFrameLayout.requestLayout();

                            mVideoSurface.setVisibility(View.GONE);
                            mVideoSurface.requestLayout();
                        }
                    });
                    if(PlanControl ==1 || PlanControl == 2) {
                        isVidoOutputRun = true;
                        new videoOutputYUVBytes(tcpClient.getmOutputstream()).start();
                    }
                }
                if(PlanControl == 3){
                    mCodecManager.enabledYuvData(true);
                    mCodecManager.setYuvDataCallback(FuncTcpClient.this);
                    FuncTcpClient.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSimulatorPushTv.append("enable YUV Data\n");
                            mFrameLayout.setVisibility(View.VISIBLE);
                            mFrameLayout.requestLayout();

                            mVideoSurface.setVisibility(View.GONE);
                            mVideoSurface.requestLayout();
                        }
                    });
                }
                if(PlanControl == 4){
                    mCodecManager.enabledYuvData(true);
                    mCodecManager.setYuvDataCallback(FuncTcpClient.this);
                    FuncTcpClient.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSimulatorPushTv.append("enable YUV Data\n");
                            mFrameLayout.setVisibility(View.VISIBLE);
                            mFrameLayout.requestLayout();

                            mVideoSurface.setVisibility(View.GONE);
                            mVideoSurface.requestLayout();
                        }
                    });
                    isUDPvideoRun = true;
                    new videoOutputYUVBytesThroughUDP().start();
                    showToast("videoOutputYUVBytesThroughUDP");
                }
                if(!isChecked){
                    isVidoOutputRun = false;
                    ish264OutputRun = false;
                    mCodecManager.enabledYuvData(false);
                    mCodecManager.setYuvDataCallback(null);

                    FuncTcpClient.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSimulatorPushTv.append("disenable Send Data\n");
                            mFrameLayout.setVisibility(View.GONE);
                            mFrameLayout.requestLayout();

                            mVideoSurface.setVisibility(View.VISIBLE);
                            mVideoSurface.requestLayout();
                        }
                    });
                }
            }
        });


        mAssisSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                FuncTcpClient.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(isChecked){
                            mPushTv.setVisibility(View.GONE);
                            mSimulatorPushTv.setVisibility(View.VISIBLE);
                        }
                        else {
                            mPushTv.setVisibility(View.VISIBLE);
                            mSimulatorPushTv.setVisibility(View.GONE);
                        }
                    }
                });
            }
        });

        mAsrSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    start();
                }
                else {
                    stop();
                }
            }
        });

        mSpeedSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mNumberTv.setText(progress + 1 + "");
                mNumberInitial = progress + 1;
                djiFlightChannelInitialize();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        if (v.getId() ==R.id.btn_tcpClientConn) {

            Log.i(TAG, "onClick: 开始");
            setVisible(btnStartClient, false);
            setVisible(btnCloseClient, true);
            setVisible(btnClientSend, true);
//            btnStartClient.setEnabled(false);
//            btnCloseClient.setEnabled(true);
//            btnClientSend.setEnabled(true);
            tcpClient = new TcpClient(editClientIp.getText().toString(), getPort(editClientPort.getText().toString()));
            exec.execute(tcpClient);

            if(PlanControl ==4){
                Log.i(TAG, "onClick: 开始");
                setVisible(btnStartClient, false);
                setVisible(btnCloseClient, true);
                setVisible(btnClientSend, true);
                mUDPserver.setDestination(editClientIp.getText().toString(),FourInOneActivity.UDPport);
            }
        }
        if (v.getId() ==R.id.btn_tcpClientClose){
            tcpClient.closeSelf();
            setVisible(btnStartClient,true);
            setVisible(btnCloseClient,false);
            setVisible(btnClientSend,false);
//            btnStartClient.setEnabled(true);
//            btnCloseClient.setEnabled(false);
//            btnClientSend.setEnabled(false);
        }
        if (v.getId() ==R.id.btn_tcpCleanClientRecv){
            txtRcv.setText("");
            if(mFlightController!=null) {
                mFlightController.getControlMode(new CommonCallbacks.CompletionCallbackWith<ControlMode>() {
                    @Override
                    public void onSuccess(ControlMode controlMode) {
                        showToast("control mode:\n" + controlMode.name());
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        showToast(djiError.getDescription());
                    }
                });
            }
        }
        if (v.getId() ==R.id.btn_tcpCleanClientSend){
            txtSend.setText("");
            if(mFlightController!=null) {
                mFlightController.getRCSwitchFlightModeMapping(new CommonCallbacks.CompletionCallbackWith<RemoteControllerFlightMode[]>() {
                    @Override
                    public void onSuccess(RemoteControllerFlightMode[] remoteControllerFlightModes) {
                        showToast("RC flight mode map:\n" + Arrays.toString(remoteControllerFlightModes) );
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        showToast(djiError.getDescription());
                    }
                });
            }

        }
        if (v.getId() ==R.id.btn_tcpClientRandomID){
            finish();
            overridePendingTransition(0, 0);
            this.startActivity(getIntent());
            overridePendingTransition(0, 0);
        }
        if (v.getId() ==R.id.btn_tcpClientSend){
            Message message = Message.obtain();
            message.what = 2;
            message.obj = editClientSend.getText().toString();
            myHandler.sendMessage(message);
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    if(tcpClient.isConnecting()) {
                        tcpClient.send(editClientSend.getText().toString());
                    }
                    else {
                        showToast("socket wrong");
                    }
                }
            });
        }
        if (v.getId() ==R.id.btnWifiConnection){
            setContentView(connectWifi);
        }
        if (v.getId() == R.id.textView){  // display the Seekbar to configure the number for DJI or not
            mNumberTv.setText(mNumberInitial + "");
            setVisible(mSpeedSb,mSpeedSb.getVisibility()!=View.VISIBLE);        // change visibility every click
        }
        if (v.getId() == R.id.pointing_drawer_control_ib) {   // open the Scroll view
//            setVisible(mPushTv,!mAssisSw.isChecked());
//            setVisible(mSimulatorPushTv,mAssisSw.isChecked());
            if (mPushDrawerSd.isOpened()) {
                mPushDrawerSd.animateClose();
            } else {
                mPushDrawerSd.animateOpen();
            }

            return;
        }
        if (v.getId() ==R.id.pointing_stop_btn){
            getTapFlyOperator().stopMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    showToast(error == null ? "Stop Mission Successfully" : error.getDescription());
                }
            });
            djiLanding();
        }
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResumewifi");
        super.onResume();
        if(PlanControl == 3) {
            mDetectorAPI.onStart();
        }
        initFlightController();
        initTapFlyMission();
        djiStopControl();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPausewifi");
        super.onPause();
        if(PlanControl == 3) {
            mDetectorAPI.onPause();
        }
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStopwifi");
        super.onStop();
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturnwifi");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroywifi");
        super.onDestroy();
        djiStopControl();
        myWakeup.release();
        myRecognizer.release();
        unregisterReceiver(myBroadcastReceiver);
        if (null != mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask.cancel();
            mSendVirtualStickDataTask = null;
            mSendVirtualStickDataTimer.cancel();
            mSendVirtualStickDataTimer.purge();
            mSendVirtualStickDataTimer = null;
        }
        if (null != mSendlandingTimer) {
            mSendlandingTask.cancel();
            mSendlandingTask = null;
            mSendlandingTimer.cancel();
            mSendlandingTimer.purge();
            mSendlandingTimer = null;
        }
    }

    /** ------------------------------------------TapFly Mission  控制 ----------------------------start */
    private TapFlyMissionOperator getTapFlyOperator() {
        return DJISDKManager.getInstance().getMissionControl().getTapFlyMissionOperator();
    }

    private void TapFlyMissionOnCreate(){
        getTapFlyOperator().addListener(new TapFlyMissionOperatorListener() {
            @Override
            public void onUpdate(@Nullable TapFlyMissionEvent aggregation) {
                TapFlyExecutionState executionState = aggregation.getExecutionState();
                if (executionState != null){
                    showPointByTapFlyPoint(executionState.getImageLocation(), mRstPointIv);
                }

                StringBuffer sb = new StringBuffer();
                String errorInformation = (aggregation.getError() == null ? "null" : aggregation.getError().getDescription()) + "\n";
                String currentState = aggregation.getCurrentState() == null ? "null" : aggregation.getCurrentState().getName();
                String previousState = aggregation.getPreviousState() == null ? "null" : aggregation.getPreviousState().getName();
                Utils.addLineToSB(sb, "CurrentState: ", currentState);
                Utils.addLineToSB(sb, "PreviousState: ", previousState);
                Utils.addLineToSB(sb, "Error:", errorInformation);

                TapFlyExecutionState progressState = aggregation.getExecutionState();

                if (progressState != null) {
                    Utils.addLineToSB(sb, "Heading: ", progressState.getRelativeHeading());
                    Utils.addLineToSB(sb, "PointX: ", progressState.getImageLocation().x);
                    Utils.addLineToSB(sb, "PointY: ", progressState.getImageLocation().y);
                    Utils.addLineToSB(sb, "BypassDirection: ", progressState.getBypassDirection().name());
                    Utils.addLineToSB(sb, "VectorX: ", progressState.getDirection().getX());
                    Utils.addLineToSB(sb, "VectorY: ", progressState.getDirection().getY());
                    Utils.addLineToSB(sb, "VectorZ: ", progressState.getDirection().getZ());
                    setResultToText(sb.toString());
                }

                TapFlyMissionState missionState = aggregation.getCurrentState();
                if (!((missionState == TapFlyMissionState.EXECUTING) || (missionState == TapFlyMissionState.EXECUTION_PAUSED)
                        || (missionState == TapFlyMissionState.EXECUTION_RESETTING))){
                    setVisible(mRstPointIv, false);
//                    setVisible(mStopBtn, false);
                }else
                {
//                    setVisible(mStopBtn, true);
                    setVisible(mStartBtn, false);
                }
            }
        });
    }

    private PointF getTapFlyPoint(View iv) {
        if (iv == null) return null;
        View parent = (View)iv.getParent();
        float centerX = iv.getLeft() + iv.getX()  + ((float)iv.getWidth()) / 2;
        float centerY = iv.getTop() + iv.getY() + ((float)iv.getHeight()) / 2;
        centerX = centerX < 0 ? 0 : centerX;
        centerX = centerX > parent.getWidth() ? parent.getWidth() : centerX;
        centerY = centerY < 0 ? 0 : centerY;
        centerY = centerY > parent.getHeight() ? parent.getHeight() : centerY;

        return new PointF(centerX / parent.getWidth(), centerY / parent.getHeight());
    }

    private void showPointByTapFlyPoint(final PointF point, final ImageView iv) {
        if (point == null || iv == null) {
            return;
        }
        final View parent = (View)iv.getParent();
        FuncTcpClient.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                iv.setX(point.x * parent.getWidth() - iv.getWidth() / 2);
                iv.setY(point.y * parent.getHeight() - iv.getHeight() / 2);
                iv.setVisibility(View.VISIBLE);
                iv.requestLayout();
            }
        });
    }

    private void initTapFlyMission() {
        mTapFlyMission = new TapFlyMission();
        mTapFlyMission.isHorizontalObstacleAvoidanceEnabled = mAssisSw.isChecked();
        mTapFlyMission.tapFlyMode = TapFlyMode.FORWARD;
    }

    /** ----------------------------------------------------------Mission 控制----------------------------end */

    private void initFlightController() {

        Aircraft aircraft = DJIDemoApplication.getAircraftInstance();
        if (aircraft == null || !aircraft.isConnected()) {
            showToast("Disconnected");
            mFlightController = null;
            mGimbal = null;
            return;
        } else {
            mGimbal = getGimbalInstance();
            mGimbal.setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(@NonNull GimbalState gimbalState) {
                    Attitude attitude = gimbalState.getAttitudeInDegrees();
                    gimbalPitch = String.format("\n Pitch: %.2f",attitude.getPitch());
                    gimbalRoll = String.format("\n Roll: %.2f",attitude.getRoll());
                    gimbalYaw = String.format("\n Yaw: %.2f",attitude.getYaw());
                    gimbalMode = "\n Gimbal Mode: " + gimbalState.getMode().name();
                }
            });
            Map<CapabilityKey, DJIParamCapability> gibmalCapas = mGimbal.getCapabilities();
            gimbalAdjustPitch = gibmalCapas.get(CapabilityKey.ADJUST_PITCH).isSupported()? "\n can adjust pitch":"\n cann't adjust pitch";
            mGimbal.getMotorEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    gimbalmotor = "\n gimbal motor: enabled";
                }

                @Override
                public void onFailure(DJIError djiError) {
                    gimbalmotor = "\n gimbal motor: " + djiError.getDescription();
                }
            });

            mFlightController = aircraft.getFlightController();
            mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
            mFlightController.getSimulator().setStateCallback(new SimulatorState.Callback() {
                @Override
                public void onUpdate(final SimulatorState stateData) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {

                            String yaw = String.format("%.2f", stateData.getYaw());
                            String pitch = String.format("%.2f", stateData.getPitch());
                            String roll = String.format("%.2f", stateData.getRoll());
                            String positionX = String.format("%.2f", stateData.getPositionX());
                            String positionY = String.format("%.2f", stateData.getPositionY());
                            String positionZ = String.format("%.2f", stateData.getPositionZ());

                            setSimulatorResultToText("From simulator \n Yaw : " + yaw + ", Pitch : " + pitch + ", Roll : " + roll + "\n" + ", PosX : " + positionX +
                                    ", PosY : " + positionY +
                                    ", PosZ : " + positionZ);
                        }
                    });
                }
            });
            mFlightController.getFlightAssistant().setVisionDetectionStateUpdatedCallback(new VisionDetectionState.Callback() {
                @Override
                public void onUpdate(@NonNull VisionDetectionState visionDetectionState) {
                    if(visionDetectionState.isSensorBeingUsed()){
                        systemWarning = visionDetectionState.getSystemWarning().name();
                        if(visionDetectionState.getSystemWarning() == VisionSystemWarning.DANGEROUS){
                            activateAvoid = true;
                            systemWarning = systemWarning + "\n avoid now";
                        }
                        else {
                            activateAvoid = false;
                        }
                        visionSensorPosition = visionDetectionState.getPosition().name();
                        obstacleDistanceMeters = String.format("%.2f", visionDetectionState.getObstacleDistanceInMeters());
                        ObstacleDetectionSector[] detectionSectors = visionDetectionState.getDetectionSectors();
                        distanceSectors="";
                        for(int i = 0 ;i<detectionSectors.length;i++){
                            distanceSectors = distanceSectors+ String.format("[%d]  %.2f\n",i, detectionSectors[i].getObstacleDistanceInMeters());
                        }
                    }
                    else {
                        systemWarning = "Sensor is not used";
                    }

                }
            });
            mFlightController.getFlightAssistant().setVisionControlStateUpdatedcallback(new VisionControlState.Callback() {
                @Override
                public void onUpdate(VisionControlState visionControlState) {
                        braking = visionControlState.isBraking() ? "\n braking to avoid collision: on":"\n braking to avoid collision: off";
                        activiteavoide = visionControlState.isAvoidingActiveObstacleCollision() ? "\n activite avoiding: on":"\n activite acoiding: off";
                        performinglanding = visionControlState.isPerformingPrecisionLanding() ? "\n landing precisely: on" : "\n landing precisely: off";
                        landingprotectionstate ="\n landing Protection State: "+ visionControlState.landingProtectionState().name();
                        AdvancePilotAssistanceSystemactive = visionControlState.isAdvancedPilotAssistanceSystemActive()? "\n APAS: on":"\n APAS: off";


                }
            });

            mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState flightControllerState) {
                    String flightMode =  flightControllerState.getFlightModeString();
                     heightUltr =flightControllerState.isUltrasonicBeingUsed()?flightControllerState.getUltrasonicHeightInMeters():10;
                    String assistant = mFlightController.isFlightAssistantSupported()?"\n can be used":"\n not support";
                    String visionPosition = flightControllerState.isVisionPositioningSensorBeingUsed()?"on":"off";

                    String latitude = String.format("%.2f",flightControllerState.getAircraftLocation().getLatitude());
                    String longtitude = String.format("%.2f",flightControllerState.getAircraftLocation().getLongitude());
                    altitude = flightControllerState.getAircraftLocation().getAltitude();
                    String pitch = String.format("%.2f",flightControllerState.getAttitude().pitch);
                    String roll = String.format("%.2f",flightControllerState.getAttitude().roll);
                    String yaw = String.format("%.2f",flightControllerState.getAttitude().yaw);

                    mFlightController.getFlightAssistant().getUpwardsAvoidanceEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                        @Override
                        public void onSuccess(Boolean aBoolean) {
                            upwardsAvoidance=aBoolean?"enabled":"disabled";
                            /*
                                upwards-facing infrared sensor detects an obstacle, the aircraft will slow its ascent and
                                maintain a minimum distance of 1 meter from the obstacle.
                             */
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            if (djiError != null) {
                                upwardsAvoidance = djiError.getDescription();
                            }
                        }
                    });
                    mFlightController.getFlightAssistant().getCollisionAvoidanceEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                        @Override
                        public void onSuccess(Boolean aBoolean) {
                            collisionAvoidance = aBoolean ? "enabled": "disabled";
                            /*
                                When enabled, the aircraft will stop and try to go around detected obstacles.
                             */
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            if (djiError != null) {
                                collisionAvoidance = "some thing wrong";
                            }
                        }
                    });
                    mFlightController.getFlightAssistant().getActiveObstacleAvoidanceEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                        @Override
                        public void onSuccess(Boolean aBoolean) {
                            activiteAvoidance = aBoolean ? "enabled" : "disabled";
                            /*
                                When enabled, and an obstacle is moving toward the aircraft, the aircraft will actively fly away from it.
                                If while actively avoiding a moving obstacle,the aircraft detects another obstacle in its avoidance path, it will stop.
                                setCollisionAvoidanceEnabled must also be enabled.
                             */
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            if (djiError != null) {
                                activiteAvoidance = "some thing wrong";
                            }
                        }
                    });
                    if (mFlightController.getFlightAssistant().isSmartCaptureSupported()) {
                        mFlightController.getFlightAssistant().getAdvancedGestureControlEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                            @Override
                            public void onSuccess(Boolean aBoolean) {
                                smartCapture = aBoolean ? "enabled" : "disabled";
                                /*
                                When enabled, deep learning gesture recognition allows the user to take selfies, record videos,
                                and control the aircraft (GestureLaunch, Follow and GestureLand) using simple hand gestures.
                                 */
                            }

                            @Override
                            public void onFailure(DJIError djiError) {
                                if (djiError != null) {
                                    smartCapture = djiError.getDescription();
                                }
                            }
                        });
                    }
                    else {
                        smartCapture = "\n not support";
                    }
                    mFlightController.getTerrainFollowModeEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                        @Override
                        public void onSuccess(Boolean aBoolean) {
                            TerrainFollowMode = aBoolean ? "enabled" : "disabled";
                                /*
                                The aircraft uses height information gathered by the onboard ultrasonic system and its downward facing cameras
                                to keep flying at the same height above the ground.
                                 */
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            if (djiError != null) {
                                TerrainFollowMode = "some thing wrong";
                            }
                        }
                    });
                    mFlightController.getFlightAssistant().getVisionAssistedPositioningEnabled(new  CommonCallbacks.CompletionCallbackWith<Boolean>() {
                        @Override
                        public void onSuccess(Boolean aBoolean) {
                            VisionAssistedPosition = aBoolean ? "enabled" : "disabled";
                                /*
                                The aircraft uses height information gathered by the onboard ultrasonic system and its downward facing cameras
                                to keep flying at the same height above the ground.
                                 */
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            if (djiError != null) {
                                VisionAssistedPosition = djiError.getDescription();
                            }
                        }
                    });
                    Simulatorstate =  mFlightController.getSimulator().isSimulatorActive()?"\n simulator: on":"\n simulator: off";
                    setResultToText("From real time data: \n"+"flightMode:\t" + flightMode  + Simulatorstate + AdvancePilotAssistanceSystemactive+"\n TerrainFollowMode:\t"+TerrainFollowMode+
                            "\n upwardsAvoidance:\t"+upwardsAvoidance+"\n VisionAssistedPosition:\t"+VisionAssistedPosition+
                            "\n collisionAvoidance:\t" + collisionAvoidance + braking +"\n activiteAvoidance:\t" + activiteAvoidance +activiteavoide+"\n assistant:\t"+assistant +
                            "\n smartCapture:\t" + smartCapture + "\n heightUltr: "+heightUltr + landingprotectionstate+ performinglanding+
                            "\n visionPosition:\t"+visionPosition+"\n latitude:\t"+latitude+"\n altitude:\t"+altitude+"\n longtitude:\t"+longtitude+
                            "\n altitudeBottom:\t " + altitudeBottom + "\n altitudeTop:\t " + altitudeTop+ "\n middle:\t " + (altitudeTop+altitudeBottom)/2 +
                            "\n pitch:\t"+pitch+"\n roll:\t"+roll+"\n yaw:\t"+yaw+"\n visionSensorPosition:\n" + visionSensorPosition+"\n systemWarning:\n"+systemWarning +
                            "\n obstacleDistanceMeters:\t"+obstacleDistanceMeters + "\n distance in sectors:\n" + distanceSectors + gimbalMode+gimbalAdjustPitch+ gimbalmotor+
                            "\n Gimbal data: " + gimbalPitch + gimbalRoll + gimbalYaw + "\n \n control command: " + controlCommand
                    );
                }
            });


        }
    }

    /** ----------------------------------------------------------WiFi 控制 ----------------------------start */

    private void myFlightController(String str){
        if(str.equals("QuitClient")){
            showToast("TCP quited");
        }
        if(str.equals("0")){            ///————————————————————come back -----------------------///
            Intent intent = null;
            intent = new Intent(FuncTcpClient.this, FuncTcpClient.class);
            this.startActivity(intent);
        }
        if(str.equals("1")){            ///————————————————————take off -----------------------///
            djiMissionConfig(false,0);

            if (mFlightController != null){
                mFlightController.startTakeoff(
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    showToast(djiError.getDescription());
                                } else {
                                    showToast("Take off Success");
                                }
                            }
                        }
                );
                djiFlightChannelInitialize();
            }
//            mPitch = 0;                 //左右
//            mRoll = 0 ;                 //前后
//            mYaw = 0;                   //偏航（-90，90）
////            mThrottle = (float) -0;   //升降
//
//            switch (mNumberInitial){
//                case 1:
//                    mThrottle = (float) -0.4;   //升降
//                    break;
//                case 2:
//                    mThrottle = (float) -0.2;   //升降
//                    break;
//                case 3:
//                    mThrottle = (float) 0;   //升降
//                    break;
//                case 4:
//                    mThrottle = (float) 0.2;   //升降
//                    break;
//            }
//            Tasktimes = 0;
//            TasktimesMax = 5;
            if (null == mSendVirtualStickDataTimer) {
                mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                mSendVirtualStickDataTimer = new Timer();
                mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
            }
        }
        else if(str.equals("2")){             ///————————————————————land -----------------------///
            djiLanding();
        }
        else if(str.equals("3")){            ///————————————————————start control -----------------------///
            djiStartControl();
        }
        else if(str.equals("4")){            ///————————————————————stop control -----------------------///
            djiStopControl();
//            if (mFlightController != null){
//                Tasktimes = 6;                      //  Tasktimes must smaller than 5 (can be set to other number in "SendVirtualStickDataTask") otherwise the control command is useless.
//                if (null != mSendVirtualStickDataTimer) {
//                    mSendVirtualStickDataTask.cancel();
//                    mSendVirtualStickDataTask = null;
//                    mSendVirtualStickDataTimer.cancel();
//                    mSendVirtualStickDataTimer.purge();
//                    mSendVirtualStickDataTimer = null;
//                }
//
//                mFlightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError djiError) {
//                        if (djiError != null){
//                            showToast(djiError.getDescription());
//                        }else
//                        {
//                            showToast("stop Virtual Stick Success");
//                        }
//                    }
//                });
//                mFlightController.setTerrainFollowModeEnabled(false, new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError djiError) {
//                        if (djiError != null){
//                            showToast(djiError.getDescription());
//                        }else
//                        {
//                            showToast("stop TerrainFollow Success");
//                        }
//                    }
//                });
//
//            }
        }
        else if(str.equals("5")){            ///————————————————————start simulator -----------------------///
            if (mFlightController != null) {
                mFlightController.getSimulator()
                        .start(InitializationData.createInstance(new LocationCoordinate2D(23, 113), 10, 10),
                                new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError != null) {
                                            showToast(djiError.getDescription());
                                        }else
                                        {
                                            showToast("Start Simulator Success");
                                        }
                                    }
                                });
            }
        }
        else if(str.equals("6")){            ///————————————————————stop simulator -----------------------///
            if (mFlightController != null) {
                mFlightController.getSimulator().stop(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        }else
                        {
                            showToast("stop Simulator Success");
                        }
                    }
                });
            }
        }
        else if(str.equals("7")){            ///———————————————————— start Novice mode  -----------------------///
            if (mFlightController != null) {
                mFlightController.setNoviceModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        }else
                        {
                            showToast("start Novice mode  Success");
                        }
                    }
                });
            }
        }
        else if(str.equals("8")){            ///———————————————————— stop Novice mode  -----------------------///
            if (mFlightController != null) {
                mFlightController.setNoviceModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        }else
                        {
                            showToast("stop Novice mode  Success");
                        }
                    }
                });
            }
        }
        else if(str.equals("9")){            ///———————————————————— enable active avoidance mode  -----------------------///
            if (mFlightController != null) {
                 mFlightController.getFlightAssistant().setCollisionAvoidanceEnabled(true, new CommonCallbacks.CompletionCallback() {
                     @Override
                     public void onResult(DJIError djiError) {
                         if (djiError != null) {
                             showToast(djiError.getDescription());
                         }else
                         {
                             showToast("enable active avoidance mode  Success");
                         }
                     }
                 });
                 mFlightController.getFlightAssistant().setActiveObstacleAvoidanceEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        }else
                        {
                            showToast("enable active avoidance mode  Success");
                        }
                    }
                });
            }
        }
        else if(str.equals("10")){            ///———————————————————— disenable active avoidance mode  -----------------------///
            if (mFlightController != null) {
                mFlightController.getFlightAssistant().setActiveObstacleAvoidanceEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        }else
                        {
                            showToast("disenable active avoidance mode  Success");
                        }
                    }
                });
            }
        }
        else if(str.equals("11")){            ///———————————————————— enable deal  -----------------------///

        }
        else if(str.equals("12")){            ///———————————————————— gimbal reset  -----------------------///
            if (mGimbal != null) {
                mGimbal.reset(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            showToast(djiError.getDescription());
                        }else
                        {
                            showToast("reset gimbal success");
                        }
                    }
                });
            }
        }
        else if(str.equals("131")){            ///———————————————————— 打开视频回传  -----------------------///
            mCodecManager.enabledYuvData(true);
            mCodecManager.setYuvDataCallback(FuncTcpClient.this);
            //  mCodecManager.getYuvDataCallback();
//                    FuncTcpClient.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mVideoShowImageFrame.setVisibility(View.VISIBLE);
//                            FuncTcpClient.super.mVideoSurface.setVisibility(View.GONE);
//                        }
//                    });

            FuncTcpClient.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSimulatorPushTv.append("enable YUV Data\n");
                    mPhotoTakedIv.setVisibility(View.VISIBLE);
                    mPhotoTakedIv.requestLayout();
                    mVideoSurface.setVisibility(View.GONE);
                    mVideoSurface.requestLayout();
                    mWifiSw.setChecked(true);
                }
            });
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    tcpClient.send("LIVE");
                    showToast("send LIVE");
                }
            });
            if(PlanControl == 3) {
                isVidoOutputRun = true;
                new videoOutputYUVBytes(tcpClient.getmOutputstream()).start();
            }
            else if(PlanControl == 4){
                isUDPvideoRun = true;
                new videoOutputYUVBytesThroughUDP().start();
//                isVidoOutputRun = true;
//                new videoOutputYUVBytes(tcpClient.getmOutputstream()).start();
            }
        }
        else if(str.equals("130")){         ///———————————————————— 关闭视频回传  -----------------------///
            isVidoOutputRun = false;
            ish264OutputRun = false;
            mCodecManager.enabledYuvData(false);
            mCodecManager.setYuvDataCallback(null);
//                    FuncTcpClient.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mVideoShowImageFrame.setVisibility(View.VISIBLE);
//                            FuncTcpClient.super.mVideoSurface.setVisibility(View.GONE);
//                        }
//                    });
            FuncTcpClient.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSimulatorPushTv.append("disable YUV Data\n");
                    mPhotoTakedIv.setVisibility(View.GONE);
                    mPhotoTakedIv.requestLayout();
                    mVideoSurface.setVisibility(View.VISIBLE);
                    mVideoSurface.requestLayout();
                    mWifiSw.setChecked(false);
                }
            });
//                    exec.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            tcpClient.send("LIVE");
//                        }
//                    });
            isVidoOutputRun = false;
        }
        else if(str.equals("14")){          ///———————————————————— 拍照回传  -----------------------///
            //djiMissionConfig(true,0);
//            new sendPictureThroughWiFi().start();
            captureAction();
        }
        else if(str.equals("151")){          ///———————————————————— 启动 TapFly Mission  -----------------------///
            getTapFlyOperator().startMission(mTapFlyMission, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    showToast(error == null ? "Start Mission Successfully" : error.getDescription());
                    if (error == null){
                        setVisible(mStartBtn, false);
                    }
                }
            });
        }
        else if(str.equals("150")){          ///———————————————————— 关闭 TapFly Mission  -----------------------///
            getTapFlyOperator().stopMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    showToast(error == null ? "Stop Mission Successfully" : error.getDescription());
                }
            });
        }
        else if(str.equals("160")){          ///———————————————————— 向左偏航  -----------------------///
            djiYawTune(true,(float)10);
        }
        else if(str.equals("161")){          ///———————————————————— 向右偏航  -----------------------///
            djiYawTune(true, (float) -10);
        }
        else if(str.equals("17")){          ///———————————————————— 执行直行 + 平移 Mission -----------------------///
            djiMissionConfig(true,0);
        }
        else if(str.equals("18")){          ///——————————————————————— 悬停 ------------------------------------///
//            getTapFlyOperator().stopMission(new CommonCallbacks.CompletionCallback() {
//                @Override
//                public void onResult(DJIError error) {
//                    showToast(error == null ? "Stop Mission Successfully" : error.getDescription());
//                }
//            });
            djiMove(0,0,0,0);
//            djiMissionConfig(false,mState);
//            djiFlightChannelAutoControl(false);

        }
        else if(str.equals("191")){         ///——————————————————————— 使能自动航道控制 ------------------------------------///
            djiFlightChannelAutoControl(true);
        }
        else if(str.equals("190")){         ///——————————————————————— 关闭自动航道控制 ------------------------------------///
            djiFlightChannelAutoControl(false);
        }
        else if(str.equals("20")){         ///——————————————————————— ------------------------------------///
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        getFileList();
                    }
                    catch(Exception e){
                        showToast("Wrong!");
                    }
                }
            });
        }
        else if(str.equals("21")){          ///————————————————————上升0.5m -----------------------///
            djiStartControl();
            djiMove(0, 0, 0, (float) 0.5);
            try
            {
                Thread.currentThread().sleep(1000);//毫秒
            }
            catch(Exception e){}
            djiMove(0, 0, 0, (float) 0);
        }
        else if(str.equals("221")){          ///————————————————————下降0.5m -----------------------///
            djiStartControl();
            djiMove(0, 0, 0, (float) -0.5);
            try
            {
                Thread.currentThread().sleep(1000);//毫秒
            }
            catch(Exception e){}
            djiMove(0, 0, 0, (float) 0);
        }
        else if(str.equals("222")){          ///————————————————————下降2.5m -----------------------///
            djiStartControl();
            djiMove(0, 0, 0, (float) -1);
            try
            {
                Thread.currentThread().sleep(2500);//毫秒
            }
            catch(Exception e){}
            djiMove(0, 0, 0, (float) 0);
        }
        else if(str.equals("23")){          ///————————————————————2m/s前进 -----------------------///
            djiStartControl();
            djiMove(0, (float) 2, 0, 0);
        }
        else if(str.equals("231")){          ///————————————————————前进一 0.5m -----------------------///
            djiStartControl();
            djiMove(0, (float) 0.5, 0, 0);
            try
            {
                Thread.currentThread().sleep(1000);//毫秒
            }
            catch(Exception e){}
            exec.execute(new Runnable() {
                @Override
                public void run() {

                    myFlightController("18");
                }
            });
        }
        else if(str.equals("232")){          ///————————————————————前进二 2m -----------------------///
            djiStartControl();
            djiMove(0, (float) 1, 0, 0);
            try
            {
                Thread.currentThread().sleep(3000);//毫秒
            }
            catch(Exception e){}
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    myFlightController("3");
                    myFlightController("18");
                }
            });
        }
        else if(str.equals("233")){          ///————————————————————前进三 12m -----------------------///
            djiStartControl();
            djiMove(0, (float) 2, 0, 0);
            try
            {
                Thread.currentThread().sleep(6000);//毫秒
            }
            catch(Exception e){}
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    myFlightController("3");
                    myFlightController("18");
                }
            });
        }
        else if(str.equals("234")){          ///————————————————————前进三 15m -----------------------///
            djiStartControl();
            djiMove(0, (float) 3, 0, 0);
            try
            {
                Thread.currentThread().sleep(5000);//毫秒
            }
            catch(Exception e){}
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    myFlightController("3");
                    myFlightController("18");
                }
            });
        }
        else if(str.equals("24")){          ///————————————————————后退0.5m -----------------------///
            djiStartControl();
            djiMove(0, (float) -0.5, 0, 0);
            try
            {
                Thread.currentThread().sleep(1000);//毫秒
            }
            catch(Exception e){}
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    myFlightController("3");
                    myFlightController("18");
                }
            });
        }
        else if(str.equals("25")){          ///————————————————————左移0.5m -----------------------///
            djiStartControl();
            djiMove((float)-0.5, (float) 0, 0, 0);
            try
            {
                Thread.currentThread().sleep(1000);//毫秒
            }
            catch(Exception e){}
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    myFlightController("3");
                    myFlightController("18");
                }
            });
        }
        else if(str.equals("35")){          ///————————————————————左移2m -----------------------///
            djiStartControl();
            djiMove((float)-1, (float) 0, 0, 0);
            try
            {
                Thread.currentThread().sleep(2700);//毫秒
            }
            catch(Exception e){}
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    myFlightController("3");
                    myFlightController("18");
                }
            });
        }
        else if(str.equals("26")){          ///————————————————————右移0.5m -----------------------///
            djiStartControl();
            djiMove((float)0.5, (float) 0, 0, 0);
            try
            {
                Thread.currentThread().sleep(1000);//毫秒
            }
            catch(Exception e){}
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    myFlightController("3");
                    myFlightController("18");
                }
            });
        }
        else if(str.equals("36")){          ///————————————————————右移2m -----------------------///
            djiStartControl();
            djiMove((float)1, (float) 0, 0, 0);
            try
            {
                Thread.currentThread().sleep(2700);//毫秒
            }
            catch(Exception e){}
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    myFlightController("3");
                    myFlightController("18");
                }
            });
        }
        else if(str.equals("27")){          ///——————————————————————— 上升1.5m ------------------------------------///
            djiStartControl();
            djiMove(0, 0, 0, (float) 0.5);
            try
            {
                Thread.currentThread().sleep(3000);//毫秒
            }
            catch(Exception e){}
            djiMove(0, 0, 0, (float) 0);
        }
        else if(str.equals("28")){          ///——————————————————————— 删除最新一张照片 ------------------------------------///
            qrcode_idx--;
            String text=String.format("qrcodeIdx:%d",qrcode_idx);
            showToast("已删除！"+text);
        }
        else if(str.charAt(0)=='3'){            ///————————————————————gimbal control -----------------------///
            if(str.length()==4) {
                if (mGimbal != null) {
                    Rotation.Builder builder = new Rotation.Builder();
                    builder = builder.mode(RotationMode.RELATIVE_ANGLE);
                    builder = builder.pitch((float) Math.pow(-1,Float.valueOf(str.substring(1,2)))* Float.valueOf(str.substring(2,4)));
                    builder = builder.time(2);
                    final Rotation rotation = builder.build();
                    mGimbal.rotate(rotation, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                String str = String.format("Mode:%s pitch: %.2f  roll: %.2f  yaw: %.2f  time: %.2f", rotation.getMode().name(), rotation.getPitch(), rotation.getRoll(), rotation.getYaw(), rotation.getTime());
                                showToast(str);
                            }
                        }
                    });
                }
            }
        }
        else if(str.charAt(0)=='4'){            ///————————————————————turn on /off led -----------------------///
            if(str.length()==2) {
                if (mFlightController != null) {
                    mFlightController.setLEDsEnabled((str.charAt(1) != '0'), new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        showToast("Enable led Success");
                                    }
                                }
                            }
                    );
                }
            }
        }
        else if(str.charAt(0)=='5'){            ///————————————————————wifi command useless -----------------------///

            if(str.length()==9) {
                float pitchJoyControlMaxSpeed = 5;
                float rollJoyControlMaxSpeed = 5;
                float yawJoyControlMaxSpeed = 10;
                float verticalJoyControlMaxSpeed = 10;
                float a = (float) Math.pow(-1, Float.valueOf(str.substring(1, 2))) * (Float.valueOf(str.substring(2, 3)));
                float b = (float) Math.pow(-1, Float.valueOf(str.substring(3, 4))) * (Float.valueOf(str.substring(4, 5)));
                float c = (float) Math.pow(-1, Float.valueOf(str.substring(5, 6))) * (Float.valueOf(str.substring(6, 7)));
                float d = (float) Math.pow(-1, Float.valueOf(str.substring(7, 8))) * (Float.valueOf(str.substring(8, 9)));
                // float d = (float) (Float.valueOf(str.substring(7, 9)));
                //            float a = str.charAt(1)=='0'?Float.valueOf(str.charAt(2)):(0-Float.valueOf(str.charAt(2)));
                //            float b = str.charAt(3)=='0'?Float.valueOf(str.substring(4,5)):(0-Float.valueOf(str.substring(4,5)));
                //            float c = str.charAt(5)=='0'?Float.valueOf(str.charAt(6)):(0-Float.valueOf(str.charAt(6)));
                //            float d = str.charAt(7)=='0'?Float.valueOf(str.charAt(8)):(0-Float.valueOf(str.charAt(8)));

                mPitch = (float) (a / pitchJoyControlMaxSpeed);   //左右
                mRoll = (float) (b / rollJoyControlMaxSpeed);                  //前后
                mYaw = (float) (c * yawJoyControlMaxSpeed);                  //偏航（-90，90）
                mThrottle = (float) (d / verticalJoyControlMaxSpeed);             //升降
                if (mFlightController != null) {
                    mFlightController.sendVirtualStickFlightControlData(
                            new FlightControlData(
                                    mPitch, mRoll, mYaw, mThrottle
                            ), new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        showToast("Throttle : " + mThrottle + ", Yaw : " + mYaw + ", Pitch : " + mPitch + ", Roll : " + mRoll);
                                    }
                                }
                            }
                    );
                }
            }
        }
        else if(str.charAt(0)=='6'){            ///————————————————————wifi command -----------------------///

            TasktimesMax = 10;
            if(str.length()==9) {
                Tasktimes = 0;
                float pitchJoyControlMaxSpeed = 5;
                float rollJoyControlMaxSpeed = 5;
                float yawJoyControlMaxSpeed = 10;
                float verticalJoyControlMaxSpeed = 10;
                float a = (float) Math.pow(-1, Float.valueOf(str.substring(1, 2))) * (Float.valueOf(str.substring(2, 3)));
                float b = (float) Math.pow(-1, Float.valueOf(str.substring(3, 4))) * (Float.valueOf(str.substring(4, 5)));
                float c = (float) Math.pow(-1, Float.valueOf(str.substring(5, 6))) * (Float.valueOf(str.substring(6, 7)));
                float d = (float) Math.pow(-1, Float.valueOf(str.substring(7, 8))) * (Float.valueOf(str.substring(8, 9)));
                // float d = (float) (Float.valueOf(str.substring(7, 9)));
                //            float a = str.charAt(1)=='0'?Float.valueOf(str.charAt(2)):(0-Float.valueOf(str.charAt(2)));
                //            float b = str.charAt(3)=='0'?Float.valueOf(str.substring(4,5)):(0-Float.valueOf(str.substring(4,5)));
                //            float c = str.charAt(5)=='0'?Float.valueOf(str.charAt(6)):(0-Float.valueOf(str.charAt(6)));
                //            float d = str.charAt(7)=='0'?Float.valueOf(str.charAt(8)):(0-Float.valueOf(str.charAt(8)));

                mPitch = (float) (a / pitchJoyControlMaxSpeed);   //左右
                mRoll = (float) (b / rollJoyControlMaxSpeed);                  //前后
                mYaw = (float) (c * yawJoyControlMaxSpeed);                  //偏航（-90，90）
                mThrottle = (float) (d / verticalJoyControlMaxSpeed);             //升降
                if(altitudeMax<heightUltr||altitudeMax < altitude){
                    mThrottle = 0;
                }
                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                }

            }
        }
        else if(str.charAt(0)=='7'){            ///————————————————————前飞指定距离 command ：EX：720 前飞40s-----------------------///

            if(str.length()==3) {
                Tasktimes = 0;
                float rollJoyControlMaxSpeed = 5;

                TasktimesMax = Integer.valueOf(str.substring(1, 3)) * 10;
                mPitch = (float) 0;   //左右
                mRoll = (float) (2 / rollJoyControlMaxSpeed);                  //前后
                mYaw = (float) 0;                  //偏航（-90，90）
                mThrottle = (float) 0;             //升降
                if(altitudeMax<heightUltr||altitudeMax < altitude){
                    mThrottle = 0;
                }
                if (null == mSendVirtualStickDataTimer) {
                    mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                    mSendVirtualStickDataTimer = new Timer();
                    mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
                }

            }
        }
        else if(str.charAt(0)=='8'){            ///————————————————————校正偏航 command ：EX: 80020 = 0.2 -----------------------///

            if(str.length()==5) {
                debugYawdji1 = (float) Math.pow(-1, Float.valueOf(str.substring(1,2)))* Float.valueOf(str.substring(2,5))/100;
                showToast("debugYaw = "+debugYawdji1);
            }
        }

    }

    /** ----------------------------------------------------------WiFi 控制----------------------------end */

    private void djiLanding(){
        if (mFlightController != null){
            Tasktimes = TasktimesMax + 1;                      //  Tasktimes must smaller than 5 (can be set to other number in "SendVirtualStickDataTask") otherwise the control command is useless.
            if (null != mSendVirtualStickDataTimer) {
                mSendVirtualStickDataTask.cancel();
                mSendVirtualStickDataTask = null;
                mSendVirtualStickDataTimer.cancel();
                mSendVirtualStickDataTimer.purge();
                mSendVirtualStickDataTimer = null;
            }

            mFlightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null){
                        showToast(djiError.getDescription());
                    }else
                    {
                        //showToast("stop Virtual Stick Success");
                    }
                }
            });
            mFlightController.setTerrainFollowModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null){
                        showToast(djiError.getDescription());
                    }else
                    {
                        //showToast("stop TerrainFollow Success");
                    }
                }
            });
            mFlightController.getFlightAssistant().setLandingProtectionEnabled(true, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        showToast(djiError.getDescription());
                    } else {
                        showToast("Virtual Stick: off\n TerrainFollow: off\n landing protection: on");
                    }
                }
            });
            showToast("Start Landing");
            if(getProductInstance().getModel()== Model.MAVIC_PRO) {
                landingTasktimes = 0;
                DebugCFtimes = 0;
            }
            else if(getProductInstance().getModel()==Model.MAVIC_AIR){ // Mavic Air need landing command only once
                landingTasktimes = 235;
                DebugCFtimes = 0;
            }
            if (null == mSendlandingTimer) {
                mSendlandingTask = new SendlandingTask();
                mSendlandingTimer = new Timer();
                mSendlandingTimer.schedule(mSendlandingTask, 0, 200);
            }
        }
    }

    private void djiStartControl(){
        if (mFlightController != null){

            mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null){
                        showToast(djiError.getDescription());
                    }else
                    {
                        showToast("Enable Virtual Stick Success");
                        mVirtualStickEnabled = true;
                    }
                }
            });
            if (null == mSendVirtualStickDataTimer) {
                mSendVirtualStickDataTask = new SendVirtualStickDataTask();
                mSendVirtualStickDataTimer = new Timer();
                mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
            }
        }
    }

    private void djiStopControl(){
        if (mFlightController != null){
            Tasktimes = TasktimesMax + 1;                      //  Tasktimes must smaller than 5 (can be set to other number in "SendVirtualStickDataTask") otherwise the control command is useless.
            if (null != mSendVirtualStickDataTimer) {
                mSendVirtualStickDataTask.cancel();
                mSendVirtualStickDataTask = null;
                mSendVirtualStickDataTimer.cancel();
                mSendVirtualStickDataTimer.purge();
                mSendVirtualStickDataTimer = null;
            }

            mFlightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null){
                        showToast(djiError.getDescription());
                    }else
                    {
                        showToast("stop Virtual Stick Success");
                        mVirtualStickEnabled = false;
                    }
                }
            });
            mFlightController.setTerrainFollowModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null){
                        showToast(djiError.getDescription());
                    }else
                    {
                        showToast("stop TerrainFollow Success");
                    }
                }
            });

        }
    }

    private void djiMove(float pitch,float roll,float yaw,float throttle){
        mPitch = pitch;
        mRoll = roll;
        mYaw = yaw;
        mThrottle = throttle;

        Tasktimes = 0;
    }

    /** ******************************************** mission ******************************************************/
    private void djiMissionConfig(boolean start,int state){
        mStart = start;
        mState = state;
    }

    private void djiMission(){
        if(mStart) {
            switch (mNumberInitial) {
                // -----------------------*----- 1 号机 --------*------------------//
                case 1:
                    switch (mState) {
                        case 0:
                            TasktimesMax = 240;
                            djiMove(0, (float) 0.6, 0, 0);
                            mState = 1;
                            FuncTcpClient.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSimulatorPushTv.append("mState = 1\n" + Tasktimes);
                                }
                            });
                            break;
                        case 1:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 20;
                                djiMove((float) -0.2, 0, 0, 0);
                                mState = 2;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 2\n");
                                    }
                                });
                            }
                            break;
                        case 2:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 20;
                                djiMove(0, 0, 0, (float) 0);
                                mState = 3;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 3\n");
                                    }
                                });
                            }
                            break;
                        case 3:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 20;
                                djiMove(0, 0, 0, (float) 0);
                                mState = 4;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 4\n");
                                    }
                                });
                            }
                            break;
                        default:
                            djiMissionConfig(false,mState);
                            FuncTcpClient.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSimulatorPushTv.append("mStart = false\n");
                                }
                            });
                            break;
                    }
                    break;
                // -----------------------*----- 2 号机 --------*------------------//
                case 2:
                    switch (mState) {
                        case 0:
                            TasktimesMax = 40;
                            djiMove(0, (float) 0,0, 0);

                            mState = 1;

                            FuncTcpClient.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSimulatorPushTv.append("mState = 0\n" + Tasktimes);
                                }
                            });
                            break;
                        case 1:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 240;
                                djiMove(0, (float) 0.4,  (float)0.23, 0);
                                mState = 2;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 1\n" + Tasktimes);
                                    }
                                });
                            }
                            break;
                        case 2:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 20;
                                djiMove(0, 0, 0, (float) -0.2);
                                mState = 3;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 2\n");
                                    }
                                });
                            }
                            break;
                        case 3:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 20;
                                djiMove(0, 0, 0, (float) 0.2);
                                mState = 4;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 3\n");
                                    }
                                });
                            }
                            break;
                        case 4:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 5;
                                djiMove(0, 0, 0, 0);
                                mState = 5;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 4\n");
                                    }
                                });
                            }
                            break;
                        default:
                            djiMissionConfig(false,mState);
                            FuncTcpClient.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSimulatorPushTv.append("mStart = false\n");
                                }
                            });
                            break;
                    }
                    break;
                // -----------------------*----- 3 号机 --------*------------------//
                case 3:
                    switch (mState) {
                        case 0:
                            TasktimesMax = 80;
                            djiMove(0, (float) 0, 0, 0);

                            mState = 1;

                            FuncTcpClient.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSimulatorPushTv.append("mState = 0\n" + Tasktimes);
                                }
                            });
                            break;
                        case 1:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 240;
                                djiMove(0, (float) 0.4,  (float)0.25, 0);
                                mState = 2;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 1\n" + Tasktimes);
                                    }
                                });
                            }
                            break;
                        case 2:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 20;
                                djiMove(0, 0, 0, (float) -0.2);
                                mState = 3;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 2\n");
                                    }
                                });
                            }
                            break;
                        case 3:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 20;
                                djiMove(0, 0, 0, (float) 0.2);
                                mState = 4;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 3\n");
                                    }
                                });
                            }
                            break;
                        case 4:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 5;
                                djiMove(0, 0, 0, 0);
                                mState = 5;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 4\n");
                                    }
                                });
                            }
                            break;
                        default:
                            djiMissionConfig(false,mState);
                            FuncTcpClient.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSimulatorPushTv.append("mStart = false\n");
                                }
                            });
                            break;
                    }
                    break;
                // -----------------------*----- 4 号机 --------*------------------//
                case 4:
                    switch (mState) {
                        case 0:
                            TasktimesMax = 120;
                            djiMove(0, (float) 0, 0, 0);

                            mState = 1;

                            FuncTcpClient.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSimulatorPushTv.append("mState = 0\n" + Tasktimes);
                                }
                            });
                            break;
                        case 1:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 240;
                                djiMove(0, (float) 0.4, 0, 0);
                                mState = 2;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 1\n" + Tasktimes);
                                    }
                                });
                            }
                            break;
                        case 2:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 20;
                                djiMove((float) 0.2, 0, 0, 0);
                                mState = 3;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 2\n");
                                    }
                                });
                            }
                            break;
                        case 3:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 20;
                                djiMove(0, 0, 0, (float) -0.2);
                                mState = 4;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 3\n");
                                    }
                                });
                            }
                            break;
                        case 4:
                            if (Tasktimes > TasktimesMax) {
                                TasktimesMax = 20;
                                djiMove(0, 0, 0, (float) 0.2);
                                mState = 5;
                                FuncTcpClient.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSimulatorPushTv.append("mState = 4\n");
                                    }
                                });
                            }
                            break;
                        default:
                            djiMissionConfig(false,mState);
                            FuncTcpClient.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mSimulatorPushTv.append("mStart = false\n");
                                }
                            });
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private int djiHeightControlState = 1 , djiFlightChannelState = 1;
    private boolean djiFlightChannelAutoControlEnabled = true;

    private float mThrottleSpeed =(float) 0.4;      // 控制避障时升降速度
//    private float altitudeTop =(float) 1.3 , altitudeBottom = (float)0.5;   // 控制飞机航道上下限
    private float altitudeTop =(float) 3 , altitudeBottom = (float)0.5; // 控制飞机航道上下限

    float tempRoll = 0 , tempThrottle = 0;
    int tempTasktimes = 0 ;

    int YawTuneTaskTimes = 10;
    private float YawTuneParameter = (float) 0.3;
    private float tempYaw = 0;

    private void djiYawTune(boolean bln , float flt){
        YawTuneParameter = flt;
        YawTuneTaskTimes = bln? 0 : 10;
    }

    private void djiFlightChannelAutoControl(boolean enable){
        djiFlightChannelAutoControlEnabled = enable;
    }

    private void djiFlightChannelInitialize(){
        float altitudeChannelWidth = (float) 2.5;
        switch (mNumberInitial){
            case 1:
                altitudeTop =(float) 3;
                altitudeBottom = altitudeTop - altitudeChannelWidth;
                break;
            case 2:
                altitudeTop =(float) 3;
                altitudeBottom = altitudeTop - altitudeChannelWidth;
                break;
            case 3:
                altitudeTop =(float) 3;
                altitudeBottom = altitudeTop - altitudeChannelWidth;
                break;
            case 4:
                altitudeTop =(float) 3;
                altitudeBottom = altitudeTop - altitudeChannelWidth;
                break;
            default:
                break;
        }
    }

    private void djiHeightControl(){
        if(mVirtualStickEnabled){  // climbing 0.2 when some obstacle is too close
            //----------------------*-----------  主动避障  --------*----------------------//
            switch (djiHeightControlState){
                case 1:
                    if(activateAvoid){
                        tempRoll = mRoll;
                        tempThrottle = mThrottle;
                        tempTasktimes = Tasktimes;
                        mRoll = 0;
                        mThrottle = mThrottleSpeed;  //升
                        Tasktimes = 0;
                        djiHeightControlState = 2;
                        setSimulatorResultToText("\nControl:"+djiHeightControlState);// debug
                    }
                    break;
                case 2:
                    if(activateAvoid){
                        Tasktimes = 0;
                    }
                    else {
                        mRoll = tempRoll;
                        mThrottle = tempThrottle;
                        Tasktimes = tempTasktimes;
                        djiHeightControlState = ((altitude + heightUltr)/2 < (altitudeBottom + altitudeBottom)/2)? 1:3;
                        setSimulatorResultToText("\nControl:"+djiHeightControlState);// debug
                    }
                    break;
                case 3:
                    if(activateAvoid){
                        tempRoll = mRoll;
                        tempThrottle = mThrottle;
                        tempTasktimes = Tasktimes;
                        mRoll = 0;
                        mThrottle = 0-mThrottleSpeed;  //降
                        Tasktimes = 0;
                        djiHeightControlState = 2;
                        setSimulatorResultToText("\nControl:"+djiHeightControlState);// debug
                    }
                    break;
//                case 4:
//                    if(activateAvoid){
//                        Tasktimes = 0;
//                    }
//                    else {
//                        mRoll = tempRoll;
//                        mThrottle = tempThrottle;
//                        Tasktimes = tempTasktimes;
//                        TasktimesMax = tempTasktimesMax;
//                        djiHeightControlState = ((altitude < (altitudeBottom + altitudeBottom)/2)
//                                ||(heightUltr < (altitudeBottom + altitudeBottom)/2))? 1:3;
//                        setSimulatorResultToText("\ndjiHeightControlState:"+djiHeightControlState);// debug
//                    }
                default:
                    break;
            }
            //----------------------*-----------  航道控制  --------*----------------------//
            if(djiFlightChannelAutoControlEnabled) {
                switch (djiFlightChannelState) {
                    case 1:
                        if ((altitudeBottom > (heightUltr + altitude)/2 )|| (altitudeTop <(heightUltr + altitude)/2)) {
                            tempRoll = mRoll;
                            tempThrottle = mThrottle;
                            tempTasktimes = Tasktimes;
                            mRoll = 0;
                            mThrottle = mThrottleSpeed / 5;  //升
                            if ((altitudeBottom + altitudeTop) / 2 <= (heightUltr + altitude)/2) {
                                mThrottle = 0 - mThrottle;
                            }
                            Tasktimes = 0;
                            djiFlightChannelState = 2;
                            setSimulatorResultToText("\nChannelState:" + djiFlightChannelState);// debug
                        }
                        break;
                    case 2:
                        if(mThrottle > 0) {
                            if ((altitudeBottom + altitudeTop) / 2 <= (heightUltr + altitude)/2) {
                                mRoll = tempRoll;
                                mThrottle = tempThrottle;
                                Tasktimes = tempTasktimes;
                                djiFlightChannelState = 1;
                                setSimulatorResultToText("\nChannelState:" + djiFlightChannelState);// debug
                            } else {
                                Tasktimes = 0;
                            }
                        }
                        else {
                            if ((altitudeBottom + altitudeTop) / 2 >= (heightUltr + altitude)/2) {
                                mRoll = tempRoll;
                                mThrottle = tempThrottle;
                                Tasktimes = tempTasktimes;
                                djiFlightChannelState = 1;
                                setSimulatorResultToText("\nChannelState:" + djiFlightChannelState);// debug
                            } else {
                                Tasktimes = 0;
                            }
                        }
                        break;

                    default:
                        break;
                }
            }

        }
    }

    class SendVirtualStickDataTask extends TimerTask {

        @Override
        public void run() {
            djiMission();
//            mYaw = debugYawdji1;
            djiHeightControl();

            if(YawTuneTaskTimes < 9) {
                YawTuneTaskTimes = YawTuneTaskTimes + 1;

                if (YawTuneTaskTimes < 2) {
                    tempYaw = mYaw;         // 记录调整前yaw 的控制参数
                } else if (YawTuneTaskTimes < 8) {
                    mYaw = YawTuneParameter;
                } else if (YawTuneTaskTimes == 8) {
                    mYaw = tempYaw;
                }
                setSimulatorResultToText("\nYawTuneTaskTimes:["+YawTuneTaskTimes + "]\nmYaw:["+mYaw);
            }

            if (Tasktimes <= TasktimesMax) {                                               // only action a little ; 5 times can be other number if necessary
                Tasktimes = Tasktimes+1;
                if (mFlightController != null) {
                    mFlightController.sendVirtualStickFlightControlData(
                            new FlightControlData(
                                    mPitch, mRoll, mYaw, mThrottle
                            ), new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        controlCommand =  "Throttle : " + mThrottle + ", Yaw : " + mYaw + ", Pitch : " + mPitch + ", Roll : " + mRoll;
                                    }
                                }
                            }
                    );
                }
            }
            else if(YawTuneTaskTimes < 9){
                if (mFlightController != null) {
                    mFlightController.sendVirtualStickFlightControlData(
                            new FlightControlData(
                                    mPitch, mRoll, mYaw, mThrottle
                            ), new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast(djiError.getDescription());
                                    } else {
                                        controlCommand =  "Throttle : " + mThrottle + ", Yaw : " + mYaw + ", Pitch : " + mPitch + ", Roll : " + mRoll;
                                    }
                                }
                            }
                    );
                }
            }
        }
    }

    class SendlandingTask extends TimerTask {

        @Override
        public void run() {
            landingTasktimes = landingTasktimes + 1;
            if(landingTasktimes <= 240) {                                               // Mavic Pro need continue command to complete landing task
                if(mFlightController.getState().isLandingConfirmationNeeded()){
                    //showToast("need confirme Landing");
                    mFlightController.confirmLanding(
                            new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    if (djiError != null) {
                                        showToast("cfLanding Error:" + djiError.getDescription());
                                    } else {
                                        DebugCFtimes = DebugCFtimes +1;                // debug for counting  the times of confirming landing
                                        showToast("confirme landing:"+ String.valueOf(DebugCFtimes));
                                        landingTasktimes = 238;
                                    }
                                }
                            }
                    );
                }
                else {
                    if (mFlightController != null && DebugCFtimes <=1) {             //  start landing if there haven't confirmed the landing task
                        mFlightController.startLanding(
                                new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        if (djiError != null) {
                                            showToast("stLading Error:"+djiError.getDescription());
                                        }
                                    }
                                }
                        );

                    }
                }
            }
        }
    }

    public void showToast(final String string) {
        FuncTcpClient.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FuncTcpClient.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setVisible(final View v, final boolean visible) {
        if (v == null) return;
        FuncTcpClient.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                v.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }

    private void setResultToText(final String string) {
        if (mPushTv == null) {
            showToast("Push info tv has not be init...");
        }
        FuncTcpClient.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPushTv.setText(string);
            }
        });
    }

    private void setSimulatorResultToText(final String string) {
        if (mSimulatorPushTv == null) {
            showToast("simulator Push info tv has not be init...");
        }
        FuncTcpClient.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSimulatorPushTv.append(string);
            }
        });
    }

    /** ******************************************** Method for taking photo *********************************/

    private void captureAction(){
        if (camera != null) {
            camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        showToast("take photo: success");
//                        handler.postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                getFileList();
//                            }
//                        }, 500);
                    } else {
                        showToast(djiError.getDescription());
                    }
                }
            });
        }
        else {
            FuncTcpClient.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSimulatorPushTv.append("camera is null\n");
                }
            });
        }
    }

    private void getFileList() {
        mMediaManager = DJIDemoApplication.getCameraInstance().getMediaManager();
        if (mMediaManager != null) {
            mMediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        mediaFileList = mMediaManager.getInternalStorageFileListSnapshot();

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                downloadFileByIndex(mediaFileList.size()-1 );
                            }
                        }, 0);
                        showToast("get File");
                    }
                }
            });
        }
    }

    private void ShowDownloadProgressDialog() {
        if (mDownloadDialog != null) {
            FuncTcpClient.this.runOnUiThread(new Runnable() {
                public void run() {
                    mDownloadDialog.incrementProgressBy(-mDownloadDialog.getProgress());
                    mDownloadDialog.show();
                }
            });
        }
    }

    private void HideDownloadProgressDialog() {
        if (null != mDownloadDialog && mDownloadDialog.isShowing()) {
            FuncTcpClient.this.runOnUiThread(new Runnable() {
                public void run() {
                    mDownloadDialog.dismiss();
                }
            });
        }
    }

    public int qrcode_idx=0;//复制照片的计数变量
    String pics_folder=Environment.getExternalStorageDirectory().getPath()+"/qrCodes/";//二维码照片存储至手机端的文件夹
    private void downloadFileByIndex(final int index){
        if ((mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA)
                || (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS)) {
            return;
        }
        showToast("patient");
        mediaFileList.get(index).fetchFileData(destDir, "0", new DownloadListener<String>() {

            @Override
            public void onStart() {
                currentProgress = -1;
                ShowDownloadProgressDialog();
            }

            @Override
            public void onRateUpdate(long total, long current, long persize) {
                int tmpProgress = (int) (1.0 * current / total * 100);
                if (tmpProgress != currentProgress) {
                    mDownloadDialog.setProgress(tmpProgress);
                    currentProgress = tmpProgress;
                }
            }

            @Override
            public void onProgress(long total, long current) {
            }

            @Override
            public void onSuccess(String filePath) {
                HideDownloadProgressDialog();
                String oldPath$Name=filePath+"/0.jpg";
                showToast("Download File Success:" + oldPath$Name);
                currentProgress = -1;
                /** 复制照片到二维码任务文件夹*/
                File file = new File(pics_folder);
                if (!file.exists()) {
                    try {
                        //按照指定的路径创建文件夹
                        file.mkdirs();
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
                String newPath$Name=pics_folder+String.format("%d.jpg",qrcode_idx%4+1);
               if(copyFile(oldPath$Name,newPath$Name)){
                   qrcode_idx++;
                   showToast("Copy File To phone Success:" + newPath$Name);
               }

                /** 若不用四合一，注释此处*****/

                if(PlanControl == 1 || PlanControl == 2) {
                    new sendPictureThroughWiFi().start();
                }
                else if(PlanControl == 3) {
                    plan3VideoEnable = false;
                    new mobilePhoneDetectorDebug(false).start();

                }
            }

            @Override
            public void onFailure(DJIError error) {
                HideDownloadProgressDialog();
                showToast("Download File Failed" + error.getDescription());
                currentProgress = -1;
            }
        });
    }

    /**
     * 复制单个文件
     *
     * @param oldPath$Name String 原文件路径+文件名 如：data/user/0/com.test/files/abc.txt
     * @param newPath$Name String 复制后路径+文件名 如：data/user/0/com.test/cache/abc.txt
     * @return <code>true</code> if and only if the file was copied;
     *         <code>false</code> otherwise
     */
    public boolean copyFile(String oldPath$Name, String newPath$Name) {
        try {
            File oldFile = new File(oldPath$Name);
            Boolean read_flag=false;
            if (!oldFile.exists() || !oldFile.isFile() || !oldFile.canRead()) {
                return read_flag;
            }
            if(oldFile.exists()){
                File img_file = new File(newPath$Name);
                if (!img_file.exists()) {
                    img_file.createNewFile();
                }
                FileInputStream fileInputStream = new FileInputStream(oldPath$Name);
                FileOutputStream fileOutputStream = new FileOutputStream(newPath$Name);
                byte[] buffer = new byte[1024];
                int byteRead;
                while (-1 != (byteRead = fileInputStream.read(buffer))) {
                    fileOutputStream.write(buffer, 0, byteRead);
                }
                fileInputStream.close();
                fileOutputStream.flush();
                fileOutputStream.close();
                read_flag = true;
            }
            return read_flag;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    class sendPictureThroughWiFi extends Thread{

        @Override
        public void run(){
            try {
                if (destDir.exists()&&tcpClient!= null) {
                    Bitmap bm = BitmapFactory.decodeFile(destDir.getPath()+"/0.jpg");
                    FileInputStream in = new FileInputStream(destDir.getPath()+"/0.jpg");
                    ByteArrayOutputStream bao=new ByteArrayOutputStream();
                    byte[] b = new byte[1024];
                    int i = 0;
                    while ((i = in.read(b)) != -1) {
                        bao.write(b, 0, b.length);
                    }
                    bao.close();
                    in.close();
                    tcpClient.getmOutputstream().write("PICT".getBytes());
                    tcpClient.getmOutputstream().write(ByteBuffer.allocate(4).putInt(bao.size()).array());
                    tcpClient.getmOutputstream().write(bao.toByteArray());
                    FuncTcpClient.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSimulatorPushTv.append("picture submit success");
                        }
                    });
                }
            }
            catch (final Exception e){
                e.printStackTrace();
                FuncTcpClient.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSimulatorPushTv.append("Exception: "+e.getMessage());
                    }
                });
            }
        }
    }

    private Bitmap smallizeTheBitmap(@NotNull Bitmap bm){
        int smallWidth, smallHeight;
        int dimension = 500;
        if (bm.getWidth() > bm.getHeight()) {
            smallWidth = dimension;
            smallHeight = dimension * bm.getHeight() / bm.getWidth();
        } else {
            smallHeight = dimension;
            smallWidth = dimension * bm.getWidth() / bm.getHeight();
        }
        return Bitmap.createScaledBitmap(bm, smallWidth, smallHeight, false);
    }

    class sendPictureThroughUDP extends Thread{
        @Override
        public void run(){
            try {
                if (destDir.exists()) {
                    Bitmap bm = BitmapFactory.decodeFile(destDir.getPath()+"/0.jpg");
                    Bitmap bmpSmall = smallizeTheBitmap(bm);
                    ByteArrayOutputStream baosUDP = new ByteArrayOutputStream();
                    bmpSmall.compress(Bitmap.CompressFormat.WEBP, 100, baosUDP);

                    mUDPserver.sendMsg(baosUDP.toByteArray());
                    FuncTcpClient.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSimulatorPushTv.append("picture submit success through UDP\n");
                        }
                    });
                }
            }
            catch (final Exception e){
                e.printStackTrace();
                FuncTcpClient.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSimulatorPushTv.append("Exception: "+e.getMessage());
                    }
                });
            }
        }
    }

    class mobilePhoneDetectorDebug extends Thread{
        private boolean mDetectEnable = true;

        public mobilePhoneDetectorDebug(boolean detectEnable){
            mDetectEnable = detectEnable;
        }

        @Override
        public void run(){
            try {
                if (destDir.exists()) {
                    final Bitmap bm = BitmapFactory.decodeFile(destDir.getPath()+"/0.jpg");
                    FuncTcpClient.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            mSimulatorPushTv.append("picture submit success");
                        }
                    });
                    FuncTcpClient.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mPhotoTakedIv.setImageBitmap(bm);
                            mPhotoTakedIv.requestLayout();
                            if(PlanControl == 3 && mDetectEnable) {
                                mDetectorAPI.onFeed(bm, new Size(bm.getWidth(), bm.getHeight()));
//                                mSimulatorPushTv.append("onFeed detector\n");
                            }
//                mSimulatorPushTv.append("onFeed detector\n");
                        }
                    });
                }
            }
            catch (final Exception e){
                e.printStackTrace();
                FuncTcpClient.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSimulatorPushTv.append("Exception: "+e.getMessage());
                    }
                });
            }
        }
    }

    //********************************************************* VIDEO decode*********************

    @Override
    public void onYuvDataReceived(final ByteBuffer yuvFrame, final int dataSize, final int width, final int height) {
        //In this demo, we test the YUV data by saving it into JPG files.
        //DJILog.d(TAG, "onYuvDataReceived " + dataSize);
//        FuncTcpClient.this.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mSimulatorPushTv.append("on Received Yuv Data: ["+count%30+"]"+((count % 30 == 0 && yuvFrame != null)?"[true]":"")+"\n");
//            }
//        });
//        if (count++ % 30 == 0 && yuvFrame != null) {
        final byte[] bytes = new byte[dataSize];
        yuvFrame.get(bytes);
//        saveYuvDataToJPEG(bytes, width, height);

       /* if(!isFrameOk) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    rgbByteArray = new byte[dataSize];
                    mYUVdatasize = dataSize;
                    mYUVWidth = width;
                    mYUVHeight = height;
                    System.arraycopy(bytes, 0, rgbByteArray, 0, dataSize);
                    isFrameOk = true;
//                    FuncTcpClient.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mSimulatorPushTv.append("dataSize:["+dataSize+"]width:["+width+"]height:["+height+"]\n");
//                        }
//                    });
                }
            }).start();

        }*/
        // showToast("onYuvDataReceived");
        saveYuvDataToRGBbyTF(bytes,width,height);

        //DJILog.d(TAG, "onYuvDataReceived2 " + dataSize);
//            AsyncTask.execute(new Runnable() {
//                @Override
//                public void run() {

//                }
//            });
//        }

    }

    public ByteArrayOutputStream getBytesByBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.setScale(0.3f, 0.3f);
        Bitmap dstBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(dstBitmap.getByteCount());
        dstBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return outputStream;
    }

    public ByteArrayOutputStream getBytesByMat(Mat img) {
        MatOfByte mob=new MatOfByte();
        imencode(".jpg",img,mob);
        byte[] byteArray=mob.toArray();
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream(byteArray.length);
        try {
            outputStream.write(byteArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream;
    }

    private void saveYuvDataToJPEG(byte[] yuvFrame, int width, int height){
        if (yuvFrame.length < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return;
        }
        if(!isFrameOk) {
            byte[] y = new byte[width * height];
            byte[] u = new byte[width * height / 4];
            byte[] v = new byte[width * height / 4];
            byte[] nu = new byte[width * height / 4]; //
            byte[] nv = new byte[width * height / 4];

            System.arraycopy(yuvFrame, 0, y, 0, y.length);
            for (int i = 0; i < u.length; i++) {
                v[i] = yuvFrame[y.length + 2 * i];
                u[i] = yuvFrame[y.length + 2 * i + 1];
            }
            int uvWidth = width / 2;
            int uvHeight = height / 2;
            for (int j = 0; j < uvWidth / 2; j++) {
                for (int i = 0; i < uvHeight / 2; i++) {
                    byte uSample1 = u[i * uvWidth + j];
                    byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
                    byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
                    byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
                    nu[2 * (i * uvWidth + j)] = uSample1;
                    nu[2 * (i * uvWidth + j) + 1] = uSample1;
                    nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
                    nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
                    nv[2 * (i * uvWidth + j)] = vSample1;
                    nv[2 * (i * uvWidth + j) + 1] = vSample1;
                    nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
                    nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
                }
            }
            //nv21test
            byte[] bytes = new byte[yuvFrame.length];
            System.arraycopy(y, 0, bytes, 0, y.length);
            for (int i = 0; i < u.length; i++) {
                bytes[y.length + (i * 2)] = nv[i];
                bytes[y.length + (i * 2) + 1] = nu[i];
            }
            Log.d(TAG,
                    "onYuvDataReceived: frame index: "
                            + DJIVideoStreamDecoder.getInstance().frameIndex
                            + ",array length: "
                            + bytes.length);
//            screenShot(bytes, Environment.getExternalStorageDirectory() + "/DJI_ScreenShot", width, height);

            YuvImage yuvImage = new YuvImage(bytes,ImageFormat.NV21,width,height,null);


            baos = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
            isFrameOk = true;
        }
//        FuncTcpClient.this.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mSimulatorPushTv.append("isFrameOk:"+(isFrameOk?"true":"false")+"\n");
//            }
//        });
    }

    public int[] decodeYUV420SP(byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        int rgb[] = new int[width * height];
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);
                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;
                rgb[yp] = 0xff000000 | ((b << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((r >> 10) & 0xff);
            }
        }
        return rgb;
    }

    private void saveYuvDataToRGBbyTF(byte[] yuvFrame,final int width, final int height){
        // showToast("saveYuvDataToRGBbyTF");
        if (yuvFrame.length < width * height) {
            //DJILog.d(TAG, "yuvFrame size is too small " + yuvFrame.length);
            return;
        }
//        if(!isFrameOk) {
        rgbBytes = new int[width * height];
        rgbBytes = decodeYUV420SP(yuvFrame,width,height);
//        ImageUtils.convertYUV420SPToARGB8888(yuvFrame, width, height, rgbBytes);

        final Bitmap rgbFrameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        rgbFrameBitmap.setPixels(rgbBytes,0, width, 0, 0, width, height);


//        /** 以下尝试将int[] 转化成Mat*/
//        byte[] byteArray=intToByte(rgbBytes);
//        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
//        Mat int2Mat=new Mat(width,height, CvType.CV_8UC3,byteBuffer);
//        Mat smallMat=new Mat();
//        org.opencv.core.Size ssize=new org.opencv.core.Size(width/2,height/2);
//        Imgproc.resize(int2Mat,smallMat,ssize);
        //将rgbFrameBitmap缩小到原来的1/4大小
//        Mat rgbFrameBitmapMat=new Mat(width,height, CvType.CV_8UC3);
//        org.opencv.android.Utils.bitmapToMat(rgbFrameBitmap, rgbFrameBitmapMat);
//        Imgproc.cvtColor(rgbFrameBitmapMat,rgbFrameBitmapMat,Imgproc.COLOR_RGB2BGR);
//        org.opencv.core.Size smallSize=new org.opencv.core.Size(rgbFrameBitmapMat.width()/2,rgbFrameBitmapMat.height()/2);
//        Imgproc.resize(rgbFrameBitmapMat,rgbFrameBitmapMat,smallSize);
//        //mat转回bitmap发送出去
//        final Bitmap rgbFrameBitmapSmallSize=Bitmap.createBitmap(width/2,height/2,Bitmap.Config.ARGB_8888);
//        org.opencv.android.Utils.matToBitmap(rgbFrameBitmapMat, rgbFrameBitmapSmallSize);
//        Log.i("LIVE","已缩小Bitmap");

        if ((PlanControl == 1 || PlanControl == 2)) {
            if (!isFrameOk) {
                rgbBAOS = getBytesByBitmap(smallizeTheBitmap(rgbFrameBitmap));
                //rgbBAOS = getBytesByBitmap(rgbFrameBitmapSmallSize);
                //  rgbBAOS=getBytesByMat(smallMat);
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isFrameOk = true;
                    }
                }, 0);
            }
        }
//        else if(PlanControl == 4){
//            if(!isUDPFrameOk){
////                Bitmap bmpSmall = smallizeTheBitmap(rgbFrameBitmap);
////                bmpSmall.compress(Bitmap.CompressFormat.WEBP, 100, rgbBAOS);
//                rgbBAOS = getBytesByBitmap(smallizeTheBitmap(rgbFrameBitmap));
//                isUDPFrameOk = true;
//            }
//        }


//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    rgbBAOS = getBytesByBitmap(rgbFrameBitmap);
//                    isFrameOk = true;
//                }
//            }).start();
        FuncTcpClient.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(plan3VideoEnable) {
                    mPhotoTakedIv.setImageBitmap(rgbFrameBitmap);
                    mPhotoTakedIv.requestLayout();
                }
                if(PlanControl == 3) {
                    mDetectorAPI.onFeed(rgbFrameBitmap, new Size(width, height));
//                    mSimulatorPushTv.append("onFeed detector\n");
                }
//                mSimulatorPushTv.append("onFeed detector\n");
            }
        });

//        }
    }
    /**
     * Save the buffered data into a JPG image file
     */
    private void screenShot(byte[] buf, String shotDir, int width, int height) {
        File dir = new File(shotDir);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        YuvImage yuvImage = new YuvImage(buf,
                ImageFormat.NV21,
                width,
                height,
                null);
        OutputStream outputFile;
        final String path = dir + "/ScreenShot_" + System.currentTimeMillis() + ".jpg";
        try {
            outputFile = new FileOutputStream(new File(path));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "test screenShot: new bitmap output file error: " + e);
            return;
        }
        if (outputFile != null) {
            yuvImage.compressToJpeg(new Rect(0,
                    0,
                    width,
                    height), 100, outputFile);
        }
        try {
            outputFile.close();
        } catch (IOException e) {
            Log.e(TAG, "test screenShot: compress yuv image error: " + e);
            e.printStackTrace();
        }
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                displayPath(path);
//            }
//        });
    }

    //    @Override
//    public void onReceive(final byte[] videoBuffer, final int size){
//        mByte = videoBuffer;
//        AsyncTask.execute(new Runnable() {
//            @Override
//            public void run() {
//                if(!readytoSendh264) {
//                    videoh264buffer = videoBuffer;
//                    videoBufferSize = size;
//                    readytoSendh264 = true;
//                }
//            }
//        });
////                if(!readytoSendh264) {
////                    videoh264buffer = videoBuffer;
////                    videoBufferSize = size;
////                    readytoSendh264 = true;
////                }
//        if(mCodecManager != null){
//            mCodecManager.sendDataToDecoder(videoBuffer, size);
//        }
//    }

//    class h264Output extends Thread{
//        private OutputStream mOutputStream;
//
//        private h264Output(OutputStream outputStream){
//            mOutputStream = outputStream;
//        }
//
//        @Override
//        public void run() {
//
//            while (ish264OutputRun) {
//
//                if (readytoSendh264) {
//                    try {
////                        FuncTcpClient.this.runOnUiThread(new Runnable() {
////                            @Override
////                            public void run() {
////                                mSimulatorPushTv.append("readytoSendh264:"+(readytoSendh264?"true":"false")+"\n"
////                                        +"ish264OutputRun:"+(ish264OutputRun?"true":"false")+ "\nvideoBufferSize: "+videoBufferSize+"\n");
////                            }
////                        });
//                        //first write the data length to the outputStream ,it need a int size 4
//                        byte[] temp = ByteBuffer.allocate(4).putInt(videoBufferSize).array();
////                        AsyncTask.execute(new Runnable() {
////                            @Override
////                            public void run() {
////                                FuncTcpClient.this.runOnUiThread(new Runnable() {
////                                    @Override
////                                    public void run() {
////                                        mSimulatorPushTv.append("videoBufferSize: "+videoBufferSize
////                                                +"["+ByteBuffer.allocate(4).putInt(videoBufferSize).array()[0]+"-"+ByteBuffer.allocate(4).putInt(videoBufferSize).array()[1]
////                                                +"-"+ByteBuffer.allocate(4).putInt(videoBufferSize).array()[2]+"-"+ByteBuffer.allocate(4).putInt(videoBufferSize).array()[3]+"]["+
////                                                ByteBuffer.allocate(4).putInt(videoh264buffer.length).array()[0]+"-"+ByteBuffer.allocate(4).putInt(videoh264buffer.length).array()[1]
////                                                +"-"+ByteBuffer.allocate(4).putInt(videoh264buffer.length).array()[2]+"-"+ByteBuffer.allocate(4).putInt(videoh264buffer.length).array()[3]+"]\n");
////                                    }
////                                });
////                            }
////                        });
//                        mOutputStream.write(temp);
//                        byte[] h264bufferSize = ByteBuffer.allocate(4).putInt(videoh264buffer.length).array();
//                        mOutputStream.write(h264bufferSize);
//                        //then write the data to the outputStream
//                        mOutputStream.write(videoh264buffer);
//                        mOutputStream.flush();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        //if have any exception,close the thread
//                        ish264OutputRun = false;
//                    }
//                    readytoSendh264 = false;
//                }
//            }
//        }
//    }

    class videoOutput extends Thread{
        private OutputStream mOutputStream;

        private videoOutput(OutputStream outputStream){
            mOutputStream = outputStream;
        }

        @Override
        public void run() {
//            FuncTcpClient.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    mSimulatorPushTv.append("isFrameOk:"+(isFrameOk?"true":"false")+"\n"
//                            +"isVidoOutputRun:"+(isVidoOutputRun?"true":"false")+"\n");
//                }
//            });
            super.run();
            while (isVidoOutputRun) {
                if (isFrameOk) {
//                    if(!keyfinished) {
//                        exec.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                if(tcpClient.isConnecting()) {
//                                    tcpClient.send("LIVE");
//                                    keyfinished = true;
//                                }
//                                else {
//                                    showToast("socket wrong");
//                                }
//                            }
//                        });
//                    }
//                    else {
                    try {
                        //first write the data length to the outputStream ,it need a int size 4
                        if(baos.size()!= 0) {
                            final byte[] keyFrame = "LIVE".getBytes();
                            mOutputStream.write(keyFrame);
                            //showToast("LIVE");
                            final byte[] temp = ByteBuffer.allocate(4).putInt(baos.size()).array();
//                            FuncTcpClient.this.runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        mSimulatorPushTv.append("videoBufferSize: "+baos.size()
//                                                +"["+temp[0]+"-"+temp[1]
//                                                +"-"+temp[2]+"-"+temp[3]+"]\n");
//                                    }
//                                });
                            mOutputStream.write(temp);
                            //then write the data to the outputStream
                            mOutputStream.write(baos.toByteArray());
                            final Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(baos.toByteArray()));
                            FuncTcpClient.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mPhotoTakedIv.setImageBitmap(bitmap);
                                    mPhotoTakedIv.requestLayout();
//                                    mDetectorAPI.onFeed(bitmap,new Size(bitmap.getWidth(),bitmap.getHeight()));
//                                    mSimulatorPushTv.append("onFeed detector\n");
                                }
                            });
                            mOutputStream.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        //if have any exception,close the thread
                        isVidoOutputRun = false;
                    }
                    isFrameOk = false;
//                        keyfinished = false;
//                    }
                }
            }
        }

    }

    private int videoFrameDebug = 0;
    class videoOutputYUVBytes extends Thread{
        private OutputStream mOutputStream;

        private videoOutputYUVBytes(OutputStream outputStream){
            mOutputStream = outputStream;
        }
        @Override
        public void run() {
//            FuncTcpClient.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    mSimulatorPushTv.append("isFrameOk:"+(isFrameOk?"true":"false")+"\n"
//                            +"isVidoOutputRun:"+(isVidoOutputRun?"true":"false")+"\n");
//                }
//            });
            super.run();
            while (isVidoOutputRun) {
                if (isFrameOk) {
//                    if(!keyfinished) {
//                        exec.execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                if(tcpClient.isConnecting()) {
//                                    tcpClient.send("LIVE");
//                                    keyfinished = true;
//                                }
//                                else {
//                                    showToast("socket wrong");
//                                }
//                            }
//                        });
//                    }
//                    else {
                    try {
                        if(rgbBAOS.size() != 0) {
                            videoFrameDebug = videoFrameDebug + 1;
                            //first write the data length to the outputStream ,it need a int size 4
                            final byte[] keyFrame = "YUV4".getBytes();
                            mOutputStream.write(keyFrame);
                            //showToast("YUV4");
//                        final byte[] width = ByteBuffer.allocate(4).putInt(mYUVWidth).array();
//                        mOutputStream.write(width);
//                        final byte[] height = ByteBuffer.allocate(4).putInt(mYUVHeight).array();
//                        mOutputStream.write(height);
//                        //then write the data to the outputStream
//                        final byte[] datasize = ByteBuffer.allocate(4).putInt(mYUVdatasize).array();
//                        mOutputStream.write(datasize);
                            mOutputStream.write(ByteBuffer.allocate(4).putInt(rgbBAOS.size()).array());
                            mOutputStream.write(rgbBAOS.toByteArray());
                            mOutputStream.flush();
                            setSimulatorResultToText("keyFrame:["+ videoFrameDebug + "]\nrgbBAOS:[" + rgbBAOS.size()+"]\n" );
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        //if have any exception,close the thread
                        isVidoOutputRun = false;
                    }
                    isFrameOk = false;
//                        keyfinished = false;
//                    }
                }
            }
        }

    }


    class videoOutputYUVBytesThroughUDP extends Thread{

        @Override
        public void run() {
            super.run();
            while (isUDPvideoRun) {
                try {
                    if (isUDPFrameOk) {
                        mUDPserver.sendMsg(rgbBAOS.toByteArray());
                        videoFrameDebug = videoFrameDebug + 1;
                        if (videoFrameDebug < 100) {
                            setSimulatorResultToText("keyFrame:[" + videoFrameDebug + "]\nrgbBAOS:[" + rgbBAOS.size() + "]\n");
                        }
                        isUDPFrameOk = false;
                    }
                }
                catch (Exception e){
                    isUDPvideoRun = false;
                    e.printStackTrace();
                }
            }
        }
    }

//    public String deal(byte[] byteData){
//
//
//        // Mat image1 = new Mat(240,320, CvType.CV_8UC1);
//
//        //int height=temp.cols();
//        //int width=temp.rows();
//        int height=320;
//        int width=240;
//        Mat temp = new Mat (height,width, CvType.CV_8UC1);
//        Mat erzhi = new Mat (height,width, CvType.CV_8UC1);
//        Mat tmp = new Mat (height,width, CvType.CV_8UC1);
//        temp.put(0,0,byteData);//将byte[]流数据转化为mat
//        Mat bianyuan= new Mat (height,width, CvType.CV_8UC1);
//        Imgproc.threshold(temp, erzhi, grayvalue, 255.0, Imgproc.THRESH_BINARY );
//
//        Mat element= Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(25,25));
//        Imgproc.dilate(erzhi, tmp, element);
//
//        Imgproc.GaussianBlur(tmp, tmp, new Size(3,3), 0);
//        Imgproc.Canny(tmp, bianyuan, 50, 200);
//        Moments bbb = new Moments();
//        Mat hierarchy = new Mat();
//        // 保存轮廓
//        ArrayList<MatOfPoint> contourList = new ArrayList<MatOfPoint>();
//        // 检测轮廓
//        Imgproc.findContours(bianyuan, contourList, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//        Log.e(TAG, "轮廓数量"+String.valueOf(contourList.size()));
//        double maxaera=0;
//        int k2=0;
//        for (int i = 0; i < 10&&i < contourList.size(); i++) {
//            //for (int i = 0; i < 1; i++) {
//
//            double value2 = Imgproc.contourArea(contourList.get(i));
//            if(value2>maxaera)
//            {
//                maxaera = value2;
//                bbb = Imgproc.moments(contourList.get(i));
//                k2=i;
//            }
//        }
//        Imgproc.drawContours(temp, contourList, k2, new Scalar(0,245,255),2);
//        double xxx=bbb.get_m10() / bbb.get_m00();
//        double yyy = bbb.get_m01() / bbb.get_m00();
//        return String.valueOf(xxx)+'#'+String.valueOf(yyy);//返回字符串，以#隔开
//
//    }

    public static byte[] intToByte(int[] intarr) {
        int bytelength = intarr.length * 4;//长度
        byte[] bt = new byte[bytelength];//开辟数组
        int curint = 0;
        for (int j = 0, k = 0; j < intarr.length; j++, k += 4) {
            curint = intarr[j];
            bt[k] = (byte) ((curint >> 24) & 0b1111_1111);//右移4位，与1作与运算
            bt[k + 1] = (byte) ((curint >> 16) & 0b1111_1111);
            bt[k + 2] = (byte) ((curint >> 8) & 0b1111_1111);
            bt[k + 3] = (byte) ((curint >> 0) & 0b1111_1111);
        }
        return bt;
    }

}