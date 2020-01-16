/**
 * Created by wbzhang on 2019/7/15.
 * Captain of BUAA Yuyuan-Y.IARC2019
 */
package com.dji.P4MissionsDemo.qrdetect;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.P4MissionsDemo.OpencvTestActivity;
import com.dji.P4MissionsDemo.R;
import com.dji.P4MissionsDemo.funcs.FuncTcpClient;
import static com.dji.P4MissionsDemo.qrdetect.QRCodeUtil.decodeFromPhoto;


import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import static java.lang.Math.sqrt;
import static org.opencv.core.Core.BORDER_DEFAULT;
import static org.opencv.core.Core.batchDistance;
import static org.opencv.core.Core.cartToPolar;
import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_32FC1;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.abs;
import static java.lang.Math.max;


public class qrCodeDetect extends Activity implements qrCodeDetectAndJoint{
    //********************安卓活动所用控件变量*********************
    private Button start_btn,capture_btn,confirm_btn,onekeyJoint_btn,select_btn,display_btn,detect_btn,joint_btn,decode_btn;
    private ImageView imageView1,imageView2,imageView3,imageView4;
    private ImageView imageView5,imageView6;
    private TextView resultTextview,qrcodeDetectTitle;
    private Handler handler,errorHandler;
    private final int PICK_IMAGE_REQUEST = 1;
    private static final int MSG_WHAT_SHOW_TOAST = 0;
    private static final int MSG_WHAT_UPDATE_TITLE = 1;
    private static final int MSG_WHAT_UPDATE_HINTS = 2;
    private static final int MSG_WHAT_SET_IMAGE = 3;
    private static final int MSG_WHAT_SHOW_JOINTRESULT = 12;
    private static final int MSG_WHAT_ERROR_PESPECT = 22;
    private static final int MSG_WHAT_TRY_MODIFYRATIO = 31;
    private static final int MSG_WHAT_TRY_MODEFYPARAMS=32;
    private static final int MSG_WHAT_ERROR_FATAL = 4;
    //********************二维码拼接所用全局变量*********************
    public static final double PI = 3.141592653;
    public int[] codeSize={21,42,64,84,105,126,147,168,189,210};//完整二维码的大小的枚举数组
    public static final int intactLNGH = 210;
//    public static final int length= 114;
    public int length=111;
    private List<Mat> pics = new ArrayList<>(4);
    private Boolean rawFlag=true;
    private List<Mat> picsRaw = new ArrayList<>(4);
    //private List<List<Mat>> pyramidPics;//可对原图建立金字塔，以原图1/4大小（1014*760）为最佳测试图
    private Mat pic; //检测时的当前图片
    private int picIdx = 0;//当前图片的顺序号
    private String img_path = new String();
//    private String file_path="/qrCodes";
        private String file_path="/qrCodes-23pm2";

    //提取破碎二维码部分
    private Mat thresholdOutput;//阈值图
    public ArrayList<MatOfPoint> allContours=new ArrayList<>(0);
    public ArrayList<MatOfPoint> cornerContours=new ArrayList<>(0);//轮廓法中所有轮廓和角点的轮廓
    //两种方法筛选出的rois
    public List<Rect> rois1=new ArrayList<>(0);
    public List<Rect> rois2=new ArrayList<>(0);
    //两种方法提取出的boundingBox
    public Rect boundingBox1=new Rect(0,0,0,0);
    public Rect boundingBox2=new Rect(0,0,0,0);
    public Rect checkedBox=new Rect(0,0,0,0);//校验之后的ROI
    private List<Rect> detectedROI = new ArrayList<>(4);//校验之后的ROI集

    private List<Mat> brokenPics = new ArrayList<>(4);
    public Mat brc;//大小归一化成length*length的破碎二维码图
    public Mat brcThre;//从阈值图中取出的归一化后的破碎二维码ROI图
    public Mat persPic;//投影变换之后的图
    public List<Mat> persPics=new ArrayList<>(4);//投影之后的图片集
    public int[] codeOrder=new int[4];
    public Boolean decodeFlag=false;
    //********************打印日志输出标签*********************
    public static final String TAG_DETECT="Detect";
    public static final String TAG_JOINT="Joint";
    public static final String TAG_DECODE="Decode";
    //*********************************************安卓活动部分********************************************************

    /** *********************构造器*******************************/
    /**
     * 如果想要采用不同的算法参数，可以再此设置构造函数
     * */

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        staticLoadCVLibraries();
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏展示
        setContentView(R.layout.activity_qr_code_detect);//设定视图
        initViews();//视图变量初始化
        bindListener();//绑定监听器
        handler = new qrcodeHandler();
        errorHandler = new errorHandler();

        //触发查看按键
        display_btn.callOnClick();
        //触发一键拼接
        onekeyJoint_btn.callOnClick();
    }

    private void initViews(){
        //**********************视图控件变量初始化*************************
        imageView1=(ImageView)findViewById(R.id.qrcode1);
        imageView2=(ImageView)findViewById(R.id.qrcode2);
        imageView3=(ImageView)findViewById(R.id.qrcode3);
        imageView4=(ImageView)findViewById(R.id.qrcode4);
        imageView5=(ImageView)findViewById(R.id.qrcodeIntact1);
        imageView6=(ImageView)findViewById(R.id.qrcodeIntact2);
        qrcodeDetectTitle=(TextView)findViewById(R.id.qrcodeDetectTitle);
        resultTextview=(TextView)findViewById(R.id.qrCodeHints);
        resultTextview.setMovementMethod(ScrollingMovementMethod.getInstance());
        //语音控制
        start_btn =(Button)findViewById(R.id.qrcodeStartDetectBtn);
        capture_btn=(Button)findViewById(R.id.qrcodeCaptureBtn);
        confirm_btn=(Button)findViewById(R.id.qrcodeConfirmBtn);
        onekeyJoint_btn=(Button)findViewById(R.id.qrCodeOneKeyBtn);
        //手动qrcodeJoint
        select_btn=(Button)findViewById(R.id.qrcodeSelectPathBtn);
        display_btn=(Button)findViewById(R.id.qrcodeDisplayBtn);
        detect_btn=(Button)findViewById(R.id.qrcodeDetectBtn);
        joint_btn=(Button)findViewById(R.id.qrcodeJointBtn);
        decode_btn=(Button)findViewById(R.id.qrcodeDecodeBtn);
        joint_btn=(Button)findViewById(R.id.qrcodeJointBtn);
        decode_btn=(Button)findViewById(R.id.qrcodeDecodeBtn);
    }

    private void bindListener(){
        //**********************绑定监听器*************************
        confirm_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =new Intent();
                intent.setClass(qrCodeDetect.this, OpencvTestActivity.class);
                startActivity(intent);
            }
        });
        onekeyJoint_btn.setOnClickListener(new qrDetectBtnListener());

        display_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                loadPics();
                setRawImage();//设置原图
            }
        });
        detect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            if(pics.size()==4) {
                                List<Mat> jointResults = detectAndJoint();
                                if(jointResults.size()==5){
                                    Message msg=handler.obtainMessage(MSG_WHAT_SHOW_JOINTRESULT,jointResults);
                                    handler.sendMessage(msg);
                                    String text="已成功实现拼接...\n";
                                    Message msg1=handler.obtainMessage(MSG_WHAT_UPDATE_HINTS,text);
                                    handler.sendMessage(msg1);
                                }
                                else{
                                    String text="未能成功实现拼接...\n";
                                    Message msg1=handler.obtainMessage(MSG_WHAT_UPDATE_HINTS,text);
                                    handler.sendMessage(msg1);
                                }
                            }
                            else{
                                String text="未能读取到四张图片！";
                                Message msg=errorHandler.obtainMessage(MSG_WHAT_SHOW_TOAST,text);
                            }
                        }
                        catch (Exception e)
                        { }
                    }
                }).start();
            }
        });
        joint_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //退出二维码拼接程序
                Intent intent =new Intent();
                intent.setClass(qrCodeDetect.this, FuncTcpClient.class);
                startActivity(intent);
            }
        });
        decode_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    qrCodeRecognization();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
        select_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"选择路径..."), PICK_IMAGE_REQUEST);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        switch(requestCode){
            case PICK_IMAGE_REQUEST:
                if(resultCode == RESULT_OK){
                    Uri uri = data.getData();
                    String path = uri.getPath();
                    resultTextview.setText(path);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    class qrDetectBtnListener implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            qrcodeJointThread t = new qrcodeJointThread();
            t.start();
        }
    }

    //**********************多线程协作**************************
    class qrcodeJointThread extends Thread{
        @Override
        public void run() {
            super.run();
            if(pics.size()==4) {
                List<Mat> jointResults = detectAndJoint();
                if(jointResults.size()==5){
                    Message msg=handler.obtainMessage(MSG_WHAT_SHOW_JOINTRESULT,jointResults);
                    handler.sendMessage(msg);
                    String msg_length=String.format("边长：%d\t",length);
                    handler.sendMessage(handler.obtainMessage(MSG_WHAT_UPDATE_HINTS,msg_length));
                    String text="已成功实现拼接...\n";
                    Message msg1=handler.obtainMessage(MSG_WHAT_UPDATE_HINTS,text);
                    handler.sendMessage(msg1);
                    Log.i(TAG_JOINT,text);

                    //成功拼接后，进行解码
                    try {
                        qrCodeRecognization();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    String text="未能成功实现拼接...\n";
                    Message msg1=handler.obtainMessage(MSG_WHAT_UPDATE_HINTS,text);
                    handler.sendMessage(msg1);
                }
            }
            else{
                String text="未能读取到四张图片！\n";
                Message msg=errorHandler.obtainMessage(MSG_WHAT_SHOW_TOAST,text);
            }
        }
    }

    //修改参数，再次尝试算法（可能得使用面向过程的方法）；否则再产生本类实例，setter参数进行尝试
    class modifyParamsThread extends Thread{
        morphoParams params;


    }

    class morphoParams{
        //图像预处理参数
        int kernalSize=3;
        int fetchIters=6;//开操作多次
        int findIters=2;//开操作少次
        //fetch方法参数
        double fetchRoundLimit=0.25;
        double maxfetchArea=12000;
        double minfetchArea=2000;
        /** 参数说明，限定破碎二维码区域的面积area
         * 建议设置范围为2200-12000
         *  60-（10500，11500）
         *  85- 4800±300
         *  100- 3600±200
         *  120- 2500±200
         *  140- 1900±100
         * */
        //find方法参数
        double findRoundLimit=0.10;
        double maxfindAreaRatio=2000;

        //拼接参数
        int mergerLength=110;


        public void setKernalSize(int kernalSize) {
            this.kernalSize = kernalSize;
        }

        public void setFetchIters(int fetchIters) {
            this.fetchIters = fetchIters;
        }

        public void setFindIters(int findIters) {
            this.findIters = findIters;
        }

        public void setFetchRoundLimit(double fetchRoundLimit) {
            this.fetchRoundLimit = fetchRoundLimit;
        }

        public void setMinfetchArea(double minfetchArea) {
            this.minfetchArea = minfetchArea;
        }

        public void setFindRoundLimit(double findRoundLimit) {
            this.findRoundLimit = findRoundLimit;
        }

        public void setMaxfetchArea(double maxfetchArea) {
            this.maxfetchArea = maxfetchArea;
        }

        public void setMaxfindAreaRatio(double maxfindAreaRatio) {
            this.maxfindAreaRatio = maxfindAreaRatio;
        }
    }

    //二维码识别结果Handler
    class qrcodeHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what)
            {
                case MSG_WHAT_SHOW_TOAST:
                    String msgStr=(String) msg.obj;
                    showToast(msgStr);
                    break;
                case MSG_WHAT_SHOW_JOINTRESULT:
                    List<Mat> msgObj=(List<Mat>) msg.obj;
                    setImageView(msgObj.get(0),imageView5);
                    setImageView(msgObj.get(1),imageView1);
                    setImageView(msgObj.get(2),imageView2);
                    setImageView(msgObj.get(3),imageView3);
                    setImageView(msgObj.get(4),imageView4);
                    break;
                case MSG_WHAT_UPDATE_TITLE:
                    if (qrcodeDetectTitle != null) {
                        qrcodeDetectTitle.setText((String) msg.obj);
                    }
                    break;
                case MSG_WHAT_UPDATE_HINTS:
                    refreshHintView(resultTextview,(String)msg.obj);
                    //                    resultTextview.append((String) msg.obj);
//                    appendResultText((String) msg.obj);
                    break;
                case  MSG_WHAT_SET_IMAGE:
                    setImageView((Mat)msg.obj,imageView6);
                    break;
                case MSG_WHAT_TRY_MODIFYRATIO:
                    if(length<120){
                        length++;
                        //此处应该将该类参数初始化
                        onekeyJoint_btn.callOnClick();
                    }
                    break;
                case MSG_WHAT_ERROR_FATAL:
                    //展示提示信息
                    appendResultText((String) msg.obj);
                    //10秒后自动结束本活动，返回上一活动
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 6000);//延时10s执行
                    break;
                default:
                    break;
            }
        }
    }

    //错误及提示处理handler
    class errorHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what)
            {
                case MSG_WHAT_TRY_MODIFYRATIO:
                    if(length<120){
                        length++;
                        //此处应该将该类参数初始化
                        onekeyJoint_btn.callOnClick();
                    }
                    break;
                case MSG_WHAT_ERROR_PESPECT:
                    break;
                case MSG_WHAT_ERROR_FATAL:
                    //展示提示信息
                    refreshHintView(resultTextview,(String) msg.obj);
                    //appendResultText((String) msg.obj);
                    //6秒后自动结束本活动，返回上一活动
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 6000);//延时6s执行
                    break;
                case MSG_WHAT_TRY_MODEFYPARAMS:
                    break;
                default:
                    break;
            }
        }
    }

    //检测黑色背景板的类
    class scratchBlackPart{}

    public void setRawImage() {
        int setIdx=0;
        for (Mat img:pics)
        {
            Bitmap bitmap1 = Bitmap.createBitmap(img.cols(),img.rows(),Bitmap.Config.ARGB_8888);
            Imgproc.cvtColor(img,img,Imgproc.COLOR_BGR2RGB);
            Utils.matToBitmap(img, bitmap1);
            switch (setIdx)
            {
                case 0:
                    imageView1.setImageBitmap(bitmap1);
                    break;
                case 1:
                    imageView2.setImageBitmap(bitmap1);
                    break;
                case 2:
                    imageView3.setImageBitmap(bitmap1);
                    break;
                case 3:
                    imageView4.setImageBitmap(bitmap1);
                    break;
            }
            setIdx++;
        }
    }

    public void setImageView(Mat pic,ImageView imageView) {
        Bitmap bitmap = Bitmap.createBitmap(pic.cols(),pic.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(pic,bitmap);
        imageView.setImageBitmap(bitmap);
    }

    public void showToast(final String string) {
        qrCodeDetect.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(qrCodeDetect.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void appendResultText(final String string) {
        qrCodeDetect.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextview.append(string);
            }
        });
    }

    /**
     * 自动滚动resultView到最后一行，并展示活动条
     * */
    private void refreshHintView(TextView textView,String msg){
        textView.append(msg);
        int offset=textView.getLineCount()*textView.getLineHeight();
        if(offset>(textView.getHeight()-textView.getLineHeight()-20)){
            textView.scrollTo(0,offset-textView.getHeight()+textView.getLineHeight()+20);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

/** *******************************************图像处理部分********************************************************/
    //OpenCV库静态加载并初始化
    private void staticLoadCVLibraries(){
        boolean load = OpenCVLoader.initDebug();
        if(load) {
            Log.i("CV", "Open CV Libraries loaded...");
        }
    }

    //从机身内存载入图片
    public void loadPics()
    {
        Mat img,imgRaw;
        for (int i = 0; i < 4; i++)
        {
            String path;
            path=String.format("%s/%d.jpg",file_path, i+1);
            path= Environment.getExternalStorageDirectory()+path;
            img = Imgcodecs.imread(path,Imgcodecs.IMREAD_REDUCED_COLOR_4);
            if (!img.empty()){
                pics.add(img);
                if(rawFlag) {
                    imgRaw=Imgcodecs.imread(path);
                    picsRaw.add(imgRaw);}
            }
            else{
                String text=String.format("Failed to read image%d!\n",i);
                Message MSG_NOTREAD_IMAGES =handler.obtainMessage(MSG_WHAT_SHOW_TOAST,text);
                handler.sendMessage(MSG_NOTREAD_IMAGES);
                /** 未能读取图片为致命错误，若干秒后返回*/
                errorHandler.sendMessage(errorHandler.obtainMessage(MSG_WHAT_ERROR_FATAL,text));
            }
        }
    }
/** *********************************二维码识别、拼接任务***************************/
    //求某个轮廓的中心点
    public Point getCenter(MatOfPoint contour)
    {
        Point[] contourTemp = contour.toArray();
        double sumX = 0 ,sumY = 0;
        for(int i=0;i<contourTemp.length;i++)
        {
            sumX = sumX+contourTemp[i].x;
            sumY = sumY+contourTemp[i].y;
        }
        double centerX = 0,centerY = 0;
        centerX = sumX/contourTemp.length;
        centerY = sumY/contourTemp.length;
        Point center=new Point(centerX,centerY);
        return center;
    }

    //计算两点之间的欧氏距离
    public double eulerDist(Point point1,Point point2)
    {

        double dist =  sqrt(Math.pow((double)(point1.x - point2.x), 2) + Math.pow((double)(point1.y - point2.y), 2));
        return  dist;
    }

    //计算某一个轮廓的圆度
    public double calRoundness(MatOfPoint contour)
    {
        double area = Imgproc.contourArea(contour);
        double rmax=0;
        Point center=getCenter(contour);
        Point[] contourTemp=contour.toArray();
        for (Point pt:contourTemp)
        {
            double r = eulerDist(center,pt);
            rmax=Math.max(rmax,r);
        }
        double roundness = area/(Math.pow(rmax,2)*PI);//计算圆度
        return roundness;
    }

    //提取结果的结构体
    class fetchPack{
        Boolean fetch_flag=false;//默认值
        Rect boundingBox=new Rect(0,0,0,0);//默认值
        List<Mat> fetchResult=new ArrayList<>(0);

        public Boolean getFetch_flag() {
            return fetch_flag;
        }

        public Rect getBoundingBox() {
            return boundingBox;
        }

        public List<Mat> getFetchResult() {
            return fetchResult;
        }

        public void setBoundingBox(Rect boundingBox) {
            this.boundingBox = boundingBox;
        }

        public void setFetch_flag(Boolean fetch_flag) {
            this.fetch_flag = fetch_flag;
        }

        public void setFetchResult(List<Mat> fetchResult) {
            this.fetchResult = fetchResult;
        }
    }

    //运行二维码提取与拼接算法
    public List<Mat> detectAndJoint()
    {
        detectedROI.clear();
        brokenPics.clear();
        persPics.clear();
        for (picIdx=0;picIdx<4;picIdx++)
        {
            pic = pics.get(picIdx);
            Boolean flag = detectAndFetch();//flag为true时，实际上通过checkedBox返回了检测结果
            if (flag)
            {
                //校验成功
//                detectedROI.add(checkedBox);//存入roi区域
                brc=new Mat(pic,checkedBox);//破碎二维码图片（原图）
                Imgproc.resize(brc,brc,new Size(length,length));
                brokenPics.add(brc);

                if(rawFlag){
                    /** 从未压缩的原图获取破碎二维码图
                     * 目的在于使得用于拼接的图质量更高，但该要求需要读入原图，会增加读图的时间*/
                    Point tl=new Point(checkedBox.tl().x*4,checkedBox.tl().y*4);
                    Point br=new Point(checkedBox.br().x*4,checkedBox.br().y*4);
                    Rect checkBoxRaw=new Rect(tl,br);
                    Mat picRawGray=new Mat();
                    Imgproc.cvtColor(picsRaw.get(picIdx), picRawGray, Imgproc.COLOR_BGR2GRAY);//灰度化
                    Mat picRawGrayEqualizehist=new Mat();
//                    Imgproc.equalizeHist(picRawGray,picRawGrayEqualizehist);//直方图均衡
                    Imgproc.medianBlur(picRawGray,picRawGrayEqualizehist,3);


                    Mat rawThresholdOut = new Mat();
                    Imgproc.threshold(picRawGrayEqualizehist,rawThresholdOut,250,255,Imgproc.THRESH_BINARY);
                    handler.sendMessage(handler.obtainMessage(MSG_WHAT_SET_IMAGE,rawThresholdOut));

//                    //加入一次闭操作（或者去除空洞算法）
//                    Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(5,5));
//                    Imgproc.morphologyEx(rawThresholdOut, rawThresholdOut,Imgproc.MORPH_CLOSE,kernel,new Point(-1,-1),1);
//                    Imgproc.threshold(rawThresholdOut,rawThresholdOut,128,255,Imgproc.THRESH_BINARY_INV);

                    brcThre=new Mat(rawThresholdOut,checkBoxRaw);
                    Imgproc.resize(brcThre,brcThre,new Size(length,length));//大小归一化
                    handler.sendMessage(handler.obtainMessage(MSG_WHAT_SET_IMAGE,brcThre));

                    String pic_path= String.format("/brcThre%d.png",picIdx);
                    img_path= Environment.getExternalStorageDirectory() + file_path+pic_path;
                    Imgcodecs.imwrite(img_path,brcThre);
                }
                else{
                    brcThre=new Mat(thresholdOutput,checkedBox);//破碎二维码图片（阈值图）
                    Imgproc.threshold(brcThre,brcThre,180,255,Imgproc.THRESH_BINARY_INV);//ROI二值图反向
                    Imgproc.resize(brcThre,brcThre,new Size(length,length));//大小归一化
                }

                fetchPuryCode();//实际上输入为brc,输出为persPic
                persPics.add(persPic);
            }
            else {
                String text=String.format("pic%d：未能提取出二维码! 6秒后活动将退出！\n",picIdx);
                Message msg1 = handler.obtainMessage(MSG_WHAT_UPDATE_HINTS,text);
                handler.sendMessage(msg1);
                Message msg = handler.obtainMessage(MSG_WHAT_SHOW_TOAST,text);
                handler.sendMessage(msg);
                /** 未能成功提取到任何一张破碎二维码为致命错误，展示提示，若干秒后结束本活动*/
                /** 未能成功提取只能通过修改检测算法的参数进行尝试，modifyParams*/
                errorHandler.sendMessage(errorHandler.obtainMessage(MSG_WHAT_ERROR_FATAL,text));
                break;
            }
        }
        List<Mat> jointResults=new ArrayList<>(0);
        if(brokenPics.size()==4)
        {
            Mat intactQrcode = bruteJoint();
            jointResults.add(intactQrcode);
            jointResults.add(persPics.get(0));
            jointResults.add(persPics.get(1));
            jointResults.add(persPics.get(2));
            jointResults.add(persPics.get(3));
            return jointResults;
        }
        else{
            String text="未能提取出四张破碎二维码，拼接条件不具备。6秒后活动将退出！\n";
            errorHandler.sendMessage(errorHandler.obtainMessage(MSG_WHAT_UPDATE_HINTS,text));
            errorHandler.sendMessage(errorHandler.obtainMessage(MSG_WHAT_ERROR_FATAL,text));
            /** 提取任务失败，其实此处已经可以结束本活动，或者使程序修改参数再次运行*/
            return jointResults;
        }
    }

    /** ****************【1】提取破碎二维码*****************/
    public Boolean detectAndFetch()
    {
//        setImageView(pic,imageView5);
        //图像预处理
        List<Mat> preImages = imgProcess(pic);
        thresholdOutput = preImages.get(2);

        Mat gray=preImages.get(0);
//        Mat mag=findGradContours(gray);


        //


        //区块法提取
        final Mat thresholdOpen10 = preImages.get(4);
        fetchPack fetchPack1 = fetchBrokenCode(thresholdOpen10);
        if(!fetchPack1.getFetch_flag()){
            String text=String.format("pic%d:区块法,未能找到破碎二维码！\n",picIdx);
            Message msg=errorHandler.obtainMessage(MSG_WHAT_UPDATE_HINTS,text);
            errorHandler.sendMessage(msg);
        }

        //轮廓法提取
        final Mat thresholdOpen2=preImages.get(3);
        fetchPack fetchPack2 = findBrokenCode(thresholdOpen2);
        if(!fetchPack2.getFetch_flag()){
            String text=String.format("pic%d:轮廓法,未能找到破碎二维码！\n",picIdx);
            Message msg=handler.obtainMessage(MSG_WHAT_UPDATE_HINTS,text);
            handler.sendMessage(msg);
        }

        //两种方法找到的boundingBox进行校验
        Boolean flag = roiCheck();
//        //如果初次校验不成功，则进行更严格意义上的认真校验
//        if (!flag){
//           Rect checkedBox2 = roiStrictCheck(rois1,rois2);
//           if (checkedBox2.area()!=0){
//               checkedBox=checkedBox2;
//               flag=true;
//           }
//        }
        return flag;
    }

    public List<Mat> imgProcess(Mat pic)
    {
        List<Mat> preProcess=new ArrayList<Mat>(6);
        /*  0-gray;1-grayEqualizeHist;2-thresholdOut;3-thresholdOpen2;4-thresholdOpen10;5:thresholdClose */
        Mat gray= new Mat(pic.size(),pic.type());
        Mat grayEqualizeHist=new Mat(pic.size(),pic.type());
        Mat thresholdOut=new Mat(pic.size(),pic.type());
        Mat thresholdOpen2=new Mat(pic.size(),pic.type());
        Mat thresholdOpen10=new Mat(pic.size(),pic.type());
        Mat thresholdClose=new Mat(pic.size(),pic.type());

        Imgproc.cvtColor(pic, gray, Imgproc.COLOR_BGR2GRAY);//灰度化
        preProcess.add(gray);
//        Imgproc.equalizeHist(gray,grayEqualizeHist);//直方图均衡
        Imgproc.medianBlur(gray,grayEqualizeHist,3);//中值滤波

        Message msg3= handler.obtainMessage(MSG_WHAT_SET_IMAGE,grayEqualizeHist);
        handler.sendMessage(msg3);
        String pic_path= String.format("/medianBlur%d.png",picIdx);
        img_path= Environment.getExternalStorageDirectory() + file_path+pic_path;
        Imgcodecs.imwrite(img_path,grayEqualizeHist);

        preProcess.add(grayEqualizeHist);
        Imgproc.threshold(grayEqualizeHist,thresholdOut,250,255,Imgproc.THRESH_BINARY_INV);  //阈值化
        preProcess.add(thresholdOut);
        //形态学操作
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(3,3));
        //两次开操作
        Imgproc.morphologyEx(thresholdOut, thresholdOpen2,Imgproc.MORPH_OPEN,kernel,new Point(-1,-1),1);
        preProcess.add(thresholdOpen2);
        pic_path= String.format("/thresholdOpen2%d.png",picIdx);
        img_path= Environment.getExternalStorageDirectory() + file_path+pic_path;
        Imgcodecs.imwrite(img_path,thresholdOpen2);


        Message msg= handler.obtainMessage(MSG_WHAT_SET_IMAGE,thresholdOpen2);
        handler.sendMessage(msg);

        //多次开操作
        Imgproc.morphologyEx(thresholdOut, thresholdOpen10,Imgproc.MORPH_OPEN,kernel,new Point(-1,-1),6);
        preProcess.add(thresholdOpen10);
        String pic_path2= String.format("/thresholdOpen10%d.png",picIdx);
        img_path= Environment.getExternalStorageDirectory() + file_path+pic_path2;
        Imgcodecs.imwrite(img_path,thresholdOpen10);

        Message msg2= handler.obtainMessage(MSG_WHAT_SET_IMAGE,thresholdOpen10);
        handler.sendMessage(msg2);

        //闭操作
        Imgproc.morphologyEx(thresholdOut, thresholdClose,Imgproc.MORPH_CLOSE,kernel,new Point(-1,-1),2);
        preProcess.add(thresholdClose);

//        setImageView(gray,imageView1);
//        setImageView(thresholdOut,imageView2);
//        setImageView(thresholdOpen2,imageView3);
//        setImageView(thresholdOpen10,imageView4);
        return preProcess;
    }

    public Mat cornerHarrisSelect(Mat gray)
    {
        //cornerHarris角点检测
        Mat harris = gray.clone();
        Mat harris_dst = pic.clone();
        Mat img_corner = Mat.zeros(pic.size(), CV_32FC1);
        Imgproc.cornerHarris(harris, img_corner, 2, 3,0.04,BORDER_DEFAULT);
        //归一化
//        normalize(img_corner, img_corner, 0, 255, NORM_MINMAX, CV_32FC1, Mat());

//        Imgproc.convertScaleAbs(img_corner, img_corner);
//        for (int row = 0; row < img_corner.rows(); row++)
//        {
//        	uchar* now_row = img_corner.ptr(row);//获取当前行
//        	for (size_t col = 0; col < img_corner.cols; col++)
//        	{
//        		int value = (int)*now_row;
//        		if (value > 115)
//        		{
//        			circle(harris_dst, Point(col, row), 2, Scalar(rng1.uniform(0, 255), rng1.uniform(0, 255), rng1.uniform(0, 255)), 2, 8, 0);
//        		}
//        		now_row++;
//        	}
//        }
//        //imshow("img_corner", img_corner);
//        sprintf_s(save,"fantasy1/harrisCorner%d.png",picIdx);
//        imwrite(save, harris_dst);
//        sprintf_s(save, "fantasy1/img_corner%d.png", picIdx);
//        imwrite(save, img_corner);
        return harris;
    }

    /********************* 矩形检测函数****************/
    public double angleCal(Point pt1,Point pt2,Point pt0){
        double dx1=pt1.x-pt0.x;
        double dy1=pt1.y-pt0.y;
        double dx2=pt2.x-pt0.x;
        double dy2=pt2.y-pt0.y;

        return (dx1*dx2+dy1*dy2)/sqrt((dx1*dx1+dy1*dy1)*(dx2*dx2+dy2*dy2)+1E-10);
    }

    public void rectangleDetect(){

    }

    /** 提取黑色背景板*/
    public fetchPack scratchBlackPart(Mat grayEqualizeHist){
        fetchPack scratchPack=new fetchPack();
        List<Mat> scratchMats=new ArrayList<>(0);

        Rect blackPart=new Rect(0,0,0,0);
        double image_area = grayEqualizeHist.cols()*grayEqualizeHist.rows();
        Mat black_square=pic.clone();

        Mat thresholdOutput=new Mat(grayEqualizeHist.size(),grayEqualizeHist.type());
        Imgproc.threshold(grayEqualizeHist,thresholdOutput,35,255,Imgproc.THRESH_BINARY);  //阈值化
        Imgproc.threshold(thresholdOutput,thresholdOutput,128,255,Imgproc.THRESH_BINARY_INV);//二值化反转

        Message msg=handler.obtainMessage(MSG_WHAT_SET_IMAGE,thresholdOutput);
        handler.sendMessage(msg);

        //轮廓提取
        List<MatOfPoint> blackContours = new ArrayList<MatOfPoint>();//闭操作图的所有轮廓
        List<MatOfPoint> squareContours = new ArrayList<MatOfPoint>();//方形轮廓
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresholdOutput,blackContours,hierarchy,Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_NONE);//区块法只查找外部轮廓

        Mat drawAllContours=new Mat(thresholdOutput.size(),CV_8UC3, Scalar.all(0));//黑色背景
        Imgproc.drawContours(drawAllContours,blackContours,-1,Scalar.all(1),1);
        handler.sendMessage(handler.obtainMessage(MSG_WHAT_SET_IMAGE,drawAllContours));

        double area;
        int contourIdx = 0;
        //直接遍历所有外部轮廓，进行圆度和面积筛选
        for(MatOfPoint contour : blackContours)
        {
            area = Imgproc.contourArea(contour);//计算轮廓面积
            Point center = getCenter(contour);//获取轮廓中心
            double roundness = calRoundness(contour);//计算圆度
            //进行圆度和面积判定。设该矩形圆度误差限为0.15，轮廓面积小于全图的1/8，且大于80*50
            /** 或许可以根据斜向矩形和最小边界框的大小以及轮廓面积的关系进行判定*/
            //若某轮廓的面积与斜向矩形框的面积比例超过95%，则近似为矩形
            //&& (abs(roundness-2/PI)>0.00005)
            if ( (abs(roundness-2/PI)<0.15)
                    && (area<image_area/8) &&(area>20000))
            {
                MatOfPoint2f contour2f=new MatOfPoint2f(contour.toArray());
                Imgproc.approxPolyDP(contour2f,contour2f,40,true);
                squareContours.add(contour);//符合条件的方形轮廓
                String text;
                text=String.format("%s%.3f","roundErr:",abs(roundness-2/PI));//蓝色标注圆度误差
                Imgproc.putText(black_square,text,center,0,1, new Scalar(255,0,0),2);
                String areaText = String.format("%s%.3f","area",area);//绿色标注面积大小
                Imgproc.putText(black_square,areaText,new Point(center.x-15,center.y-15),0,1, new Scalar(0,255,0),4);
                Imgproc.drawContours(black_square,blackContours,contourIdx, new Scalar(0,0,255),2);//红色标注轮廓
            }
            contourIdx++;
        }

        //进行结果判定，返回所有大致呈矩形黑色区块的boundingBox

        return scratchPack;
    }

//    public Mat findGradContours(Mat gray){
//
//        /** **************** Sobel算子 *******************/
//        //计算梯度图
//        gray.convertTo(gray, CV_32F, 1 / 255.0);
//
//        Mat gradX=new Mat(gray.size(),gray.type());
//        Mat gradY=new Mat(gray.size(),gray.type());
//        // Calculate gradients gx, gy
//        //对X梯度绝对值
//        int ddepth= CV_32FC1;
//        Imgproc.Sobel(gray, gradX, CV_32F, ddepth, 0, 1);
//        //对Y梯度绝对值
//        Imgproc.Sobel(gray, gradY, CV_32F, ddepth, 1, 0);
//
//        Mat mag=new Mat();
//        Mat angle=new Mat();
//        //mag 坡度梯度 大小;angle就是方向
//        cartToPolar(gradX, gradY, mag, angle);
//        mag.convertTo(mag, CV_8UC1, 255);
//        handler.sendMessage(handler.obtainMessage(MSG_WHAT_SET_IMAGE,mag));
//        //阈值分割
//        Imgproc.threshold(mag,mag,128,255,Imgproc.THRESH_BINARY);
//        handler.sendMessage(handler.obtainMessage(MSG_WHAT_SET_IMAGE,mag));
//
//
//        /** **************** 查找mag的轮廓 ************************/
//        List<MatOfPoint> allMagContours=new ArrayList<>();
//        Mat hierarchy = new Mat();
//        Mat drawMagContours=new Mat(gray.size(),CV_8UC3,Scalar.all(0));
//        Imgproc.findContours(mag,allMagContours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_NONE);
//        Imgproc.drawContours(drawMagContours,allMagContours,-1,new Scalar(0,255,0));
//        handler.sendMessage(handler.obtainMessage(MSG_WHAT_SET_IMAGE,drawMagContours));
//
//        return mag;
//    }

    //区块法
    public fetchPack fetchBrokenCode(Mat thresholdOpen10)
    {
//        Mat img_square= new Mat(thresholdOpen10.size(),pic.type(), new Scalar(255,255,255));
        fetchPack fetchResults=new fetchPack();
        List<Mat> fetchMats=new ArrayList<>(0);
        Mat img_square=pic.clone();
        Mat img_close = new Mat(thresholdOpen10.size(),thresholdOpen10.type());
        Mat drawBoundingbox1=pic.clone();
        boundingBox1= new Rect(0,0,0,0);

        //开操作去除黑色小点
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.morphologyEx(thresholdOpen10, img_close, Imgproc.MORPH_OPEN, kernel, new Point(-1, -1), 2);
        Imgproc.threshold(img_close,img_close,128,255,Imgproc.THRESH_BINARY_INV);//阈值图翻转

        handler.sendMessage(handler.obtainMessage(MSG_WHAT_SET_IMAGE,img_close));

        //轮廓提取
        List<MatOfPoint> closeContours = new ArrayList<MatOfPoint>();//闭操作图的所有轮廓
        List<MatOfPoint> squareContours = new ArrayList<MatOfPoint>();//方形轮廓
        Mat hierarchy = new Mat();
        Imgproc.findContours(img_close,closeContours,hierarchy,Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_NONE);//区块法只查找外部轮廓

//        Message msg1=handler.obtainMessage(MSG_WHAT_SET_IMAGE,img_close);
//        handler.sendMessage(msg1);

        double image_area = thresholdOpen10.cols()*thresholdOpen10.rows();
        double area;
        int contourIdx = 0;
        //直接遍历所有外部轮廓，进行圆度和面积筛选
        for(MatOfPoint contour : closeContours)
        {
            area = Imgproc.contourArea(contour);//计算轮廓面积
            Point center = getCenter(contour);//获取轮廓中心
            double roundness = calRoundness(contour);//计算圆度
            //进行圆度和面积判定。圆度误差限为0.25，轮廓面积小于全图的1/8，且大于80*50
            //&& (abs(roundness-2/PI)>0.00005)
            if ( (abs(roundness-2/PI)<0.13)
                    && (area<14000) &&(area>4400))
            {
//                MatOfPoint2f contour2f=new MatOfPoint2f(contour.toArray());
//                Imgproc.approxPolyDP(contour2f,contour2f,45,true);
//                contour2f.convertTo(contour,CvType.CV_32S);
                squareContours.add(contour);//符合条件的方形轮廓
                String text;
                text=String.format("%s%.3f","roundErr:",abs(roundness-2/PI));//蓝色标注圆度误差
                Imgproc.putText(img_square,text,center,0,1, new Scalar(255,0,0),2);
                String areaText = String.format("%s%.3f","area",area);//绿色标注面积大小
                Imgproc.putText(img_square,areaText,new Point(center.x-15,center.y-15),0,1, new Scalar(0,255,0),4);
                Imgproc.drawContours(img_square,closeContours,contourIdx, new Scalar(0,0,255),2);//红色标注轮廓
            }
            contourIdx++;
        }
        //筛选结果判定
        if (squareContours.size() == 1)
        {
            boundingBox1 = Imgproc.boundingRect(squareContours.get(0));
            fetchResults.setFetch_flag(true);
            fetchResults.setBoundingBox(boundingBox1);
            Imgproc.rectangle(img_square,boundingBox1.tl(),boundingBox1.br(), new Scalar(0,255,0));
            //setImageView(drawBoundingbox1,imageView1);
        }
//        else
//        {
//            String text=String.format("pic%d:区块法：没有找到方形轮廓，或者找到了多个方形轮廓\n",picIdx);
////            Message msg=handler.obtainMessage(MSG_WHAT_SHOW_TOAST,text);
////            handler.sendMessage(msg);
//            Message msg1=handler.obtainMessage(MSG_WHAT_UPDATE_HINTS,text);
//            handler.sendMessage(msg1);
//            //把每一个轮廓的boundingBox都存入rois1
//            rois1.clear();
//            for (MatOfPoint contour:squareContours){
//                rois1.add(Imgproc.boundingRect(contour));
//            }
//        }

//        setImageView(img_square,imageView2);
        Message msg=handler.obtainMessage(MSG_WHAT_SET_IMAGE,img_square);
        handler.sendMessage(msg);
        String pic_path= String.format("/img_square%d.png",picIdx);
        img_path= Environment.getExternalStorageDirectory() + file_path + pic_path;
        Imgcodecs.imwrite(img_path,img_square);

        fetchMats.add(img_close);
        fetchMats.add(img_square);
        fetchResults.setFetchResult(fetchMats);
        return fetchResults;
        /*返回 1-检测flag 2-提取到的boundingBox1  3-（img_close，img_square） */
    }

    //轮廓法
    public fetchPack findBrokenCode(Mat thresholdOpen2)
    {
        fetchPack fetchResults=new fetchPack();
        List<Mat> fetchMats=new ArrayList<>(0);
        Mat drawAllContours=new Mat(thresholdOpen2.size(),CV_8UC3, Scalar.all(0));//黑色背景
        Mat drawCornerContours = new Mat(thresholdOpen2.size(),CV_8UC3,Scalar.all(255));//白色背景
        Mat drawBoundingbox2 = pic.clone();
        Mat hierarchy = new Mat();
        allContours.clear();
        cornerContours.clear();
        boundingBox2=new Rect(0,0,0,0);
        Imgproc.findContours(thresholdOpen2,allContours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_NONE, new Point(0,0));
        Imgproc.drawContours(drawAllContours,allContours,-1,Scalar.all(255),2);

        Message msg1=handler.obtainMessage(MSG_WHAT_SET_IMAGE,drawAllContours);
        handler.sendMessage(msg1);

        //setImageView(drawAllContours,imageView3);

        //通过定位标识具有两层内轮廓的特点，筛选三个定位角轮廓
        double image_area = thresholdOpen2.cols()*thresholdOpen2.rows();
        List<Integer> layer2in = childContours(allContours,hierarchy);
        //[2]进行圆度筛选和面积筛选
        for(int i=0;i<layer2in.size();i++)
        {
            int idx = layer2in.get(i);
            double roundness= calRoundness(allContours.get(idx));
            double area=Imgproc.contourArea(allContours.get(idx));
            if ((abs(roundness-2/PI)<0.10) &&(area<4000))
            {
                cornerContours.add(allContours.get(idx));
                Imgproc.drawContours(drawCornerContours,allContours,idx, new Scalar(255,0,0),2);
            }
        }

        //筛选结果判定
        if (cornerContours.size() == 1)
        {
            boundingBox2 = Imgproc.boundingRect(cornerContours.get(0));
            fetchResults.setFetch_flag(true);
            fetchResults.setBoundingBox(boundingBox2);
            Imgproc.rectangle(drawBoundingbox2,boundingBox2.tl(),boundingBox2.br(), new Scalar(0,255,0));
//            //严格checkROI
//            rois2.clear();
//            rois2.add(boundingBox2);
        }
//        else
//        {
//            String text=String.format("pic%d:轮廓法,未能找到破碎二维码\n",picIdx);
//            Message msg=handler.obtainMessage(MSG_WHAT_UPDATE_HINTS,text);
//            handler.sendMessage(msg);
//            //把每一个轮廓的boundingBox都存入rois2，严格checkROI
//            rois2.clear();
//            for (MatOfPoint contour:cornerContours){
//                rois2.add(Imgproc.boundingRect(contour));
//            }
//        }

//        setImageView(drawCornerContours,imageView3);
//        setImageView(drawBoundingbox2,imageView4);
        Message msg=handler.obtainMessage(MSG_WHAT_SET_IMAGE,drawBoundingbox2);
        handler.sendMessage(msg);

        String pic_path= String.format("/drawBoundingbox2-%d.png",picIdx);
        img_path= Environment.getExternalStorageDirectory() + file_path+pic_path;
        Imgcodecs.imwrite(img_path,drawBoundingbox2);

        fetchMats.add(drawAllContours);
        fetchMats.add(drawCornerContours);
        fetchMats.add(drawBoundingbox2);
        fetchResults.setFetchResult(fetchMats);
        return fetchResults;
        /*  返回 1-检测flag 2-boundingBox2  3-(drawAllContours,drawCornerContours,drawBoundingbox2)    */
    }

     //两种方法提取到的roi进行校验
    /**1.说明
     * 该函数将综合若干种检测方法得到的所有rois集合(rois1\rois2)，将两个集合中的ROI进行校验
     * 旨在精确提取出整块破碎二维码区域的roi(checkedBox),并返回校验成功与否的标识
     * 实际上，四张图片的检测结果校验后，一旦有任何一张图片未能校验成功，此时应该结束本活动
     * 2.校验策略
     * 1）如果两种方法都未能找到唯一ROI
     * a)对于无角标区域，find方法失效；
     * 若fetch方法未找到roi，则返回false，宣告检测失败，此时结束本活动;
     * 若fetch方法找到唯一roi,则返回true，输出该roi到boundingBox1
     * 若fetch方法找到多个roi，则取其中最大的区域（或者圆度误差最小的区域\\或者取内部角点最多的区域），输出该roi到boundingBox1，此时算法仍可进行
     * b)对于有角标区域，find方法能稳定找到唯一解
     * 若fetch方法找到了0个解，结束本活动
     * 若fetch方法也找到了唯一解，进行校验，如果find找到的boundingBox2存在于boundingBox1内部，则校验成功，boundingBox1输出到chechedBox
     * 若fetch方法找到了多个解，则有可能是
     * */
    public Rect roiInner(Rect box1,Rect box2){
        Rect checkResult=new Rect(0,0,0,0);
        //box2为大框，box1为小框
        Boolean flag1=(box2.tl().x-box1.tl().x)<0
                && (box2.tl().y-box2.tl().y)<0;
        Boolean flag2=(box2.br().x-box1.br().x)>0
                && (box2.br().y -box1.br().y)>0;
        if (flag1 && flag2){
            checkResult=box2;
        }
        return checkResult;
    }

    public Rect roiStrictCheck(List<Rect> rois1,List<Rect> rois2){
        /** rois2只能有1个或者0个*/
        Rect checkedRoi = new Rect(0,0,0,0);
        if (rois1.size()!=0){
            if (rois2.size()==1){
                /** 对于有角标的区域*/
                //将rois2中的与rois1中的每一个都进行校对,输出校对成功地到checkedBox
                for (Rect rt:rois1){
                    if(roiInner(rois2.get(0),rt).area()!=0){
                        checkedRoi=rt;
                        break;
                    }
                }
            }
            else {
                /** 对于无角标的区域*/
                //输出rois1中面积最大的区域
                double maxArea=0;
                for (Rect rt:rois1){
                    if (rt.area()>maxArea){
                        maxArea=rt.area();
                        checkedRoi=rt;
                    }
                }
                //或者圆度误差最小的区域
            }
        }
        return checkedRoi;
    }

    public Boolean roiCheck()
    {
        double existFlag1=boundingBox1.area();
        double existFlag2=boundingBox2.area();

        if(existFlag1*existFlag2==0)//   至少有一个为0
        {
            //如果两个都为0，则返回false
            if(existFlag1+existFlag2==0) {
                return false;
            }
            //否则两个boundingBox中必有一个不为0，则取其输出到boundingBox
            /** 如果有一个  */
            else if(existFlag1!=0) {
                checkedBox=boundingBox1;
                return true;
            }
            else{
//                checkedBox=boundingBox2;
//                return true;
                return false;
            }
        }
        else { //如果两个boundingBox均存在(width*height！=0)，则根据位置进行校验，选择尺寸大的boundingBox输出
            /*Point center1= new Point(boundingBox1.x+boundingBox1.width*1/2,boundingBox1.y+boundingBox1.height*1/2);
            Point center2= new Point(boundingBox2.x+boundingBox2.width*1/2,boundingBox2.y+boundingBox2.height*1/2);*/
            if (existFlag1>existFlag2) {
                //如果面积小的boundingBox包含在面积大的boundingBox内，则校验成功，输出大的BoundingBox
                Boolean flag1=(boundingBox1.x-boundingBox2.x)<0 && (boundingBox1.y-boundingBox2.y)<0;
                Boolean flag2=(boundingBox1.x+boundingBox1.width-boundingBox2.x-boundingBox2.width)>0
                        && (boundingBox1.y+boundingBox1.height -boundingBox2.y-boundingBox2.height)>0;
                if (flag1 && flag2){
                    checkedBox=boundingBox1;
                }
            }
            else {
                Boolean flag1=(boundingBox2.x-boundingBox1.x)<0 && (boundingBox2.y-boundingBox1.y)<0;
                Boolean flag2=(boundingBox2.x+boundingBox2.width-boundingBox1.x-boundingBox1.width)>0
                        && (boundingBox2.y+boundingBox2.height -boundingBox1.y-boundingBox1.height)>0;
                if (flag1 && flag2){
                    checkedBox=boundingBox2;
                }
            }
            return true;
        }
    }

    /** ****************【2】寻找破碎二维码的投影角点*****************/

    //寻找固定点
    public Point findFixedPts(Point[] rtPts, int order, Point fixedPt)
    {
        Point fixedCorner=new Point(0,0);
        double min_dist = 100000;
        //根据序号选择图片角点
        switch (order)
        {
            case 0:
                fixedCorner = new Point(0,0);
                break;
            case 1:
                fixedCorner = new Point(length, 0);
                break;
            case 2:
                fixedCorner = new Point(length, length);
                break;
            case 3:
                fixedCorner = new Point(0, length);
                break;
            default:
                break;
        }
        //计算离图片角点最近的角点
        for (int i = 0; i < 4; i++)
        {
            if (eulerDist(fixedCorner, rtPts[i]) < min_dist)
            {
                fixedPt = rtPts[i];
                min_dist = eulerDist(fixedCorner, rtPts[i]);
            }
        }
        return fixedPt;
    }

    //给四个点排序
    public Point[] sortOrder(Point[] pts)
    {
        //求四个点的中心点
        Point[] sortedPts;
        Point center = new Point(0,0);
        for (Point pt:pts)
        {
            center.x += pt.x;
            center.y += pt.x;
        }
        center.x /= 4;
        center.y /= 4;
        //确定顺序号
        double relX=0;
        double relY=0;
        sortedPts = new Point[4];
        int i=0;
        for(Point pt:pts)
        {
            relX = pt.x - center.x;
            relY = pt.y - center.y;
            if ((relX < 0)&&(relY < 0))
            {
                sortedPts[0] = pt;
            }
            else if ((relX > 0) && (relY < 0))
            {
                sortedPts[1] = pt;
            }
            else if ((relX > 0) && (relY > 0))
            {
                sortedPts[2] = pt;
            }
            else if ((relX < 0) && (relY > 0))
            {
                sortedPts[3] = pt;
            }
            i++;
        }
        return sortedPts;
    }

    public List<Integer> childContours(ArrayList<MatOfPoint> contours,Mat hierarchy)
    {
        List<Integer> layer2in = new ArrayList<>(0);
        int layer_num = 0;
        for(int i=0;i<contours.size();i++)
        {
            int idx =i;
            double area=Imgproc.contourArea(contours.get(idx));
            while(hierarchy.get(0,idx)[2]!=-1){
                layer_num++;
                idx =(int)hierarchy.get(0,idx)[2];
            }
            if (layer_num==2) layer2in.add(i);
            layer_num=0;
        }
        return layer2in;
    }

    //返回具有角点图片的顺序号（0，1，2,3）
    public int getCornerOrder(MatOfPoint2f contour){
        int order=-1;
        RotatedRect rtRect=Imgproc.minAreaRect(contour);
        double deltaX = rtRect.center.x-0.5*brc.cols();
        double deltaY = rtRect.center.y-0.5*brc.rows();

        //根据角点位置来判断图片序号
        if(deltaX<0&&deltaY<0) {
            order = 0;
        }
        else if(deltaX>0&&deltaY<0) {
            order = 1;
        }
        else if(deltaX>0&&deltaY>0) {
            order = 2;
        }
        else if(deltaX<0&&deltaY>0) {
            order = 3;
        }

        return order;
    }

    //获取纯净二维码的四个角点,并确定该图片的顺序
    public Point[] getPuryPts(MatOfPoint2f contour, int order)
    {
        Point[] quadraPts=new Point[4];
        //求得该轮廓的斜向矩形框
        RotatedRect rtRect=Imgproc.minAreaRect(contour);
        Point[] rtPts =new Point[4];
        rtRect.points(rtPts);
        Point fixedPt=new Point();
        Point tempPt;
        int fixPtsIdx = 0;
        double px,py;

        //图片序号与矩形框位置确定，则根据距离角点的距离大小选择固定点
        fixedPt = findFixedPts(rtPts,order, fixedPt);
        for (int count=0;count<rtPts.length;count++) {
            if (fixedPt==rtPts[count]) fixPtsIdx=count;
        }
        //求除了固定点外其余三个点的坐标
        for (int i = 0; i < 4; i++)
        {
            if (i!= fixPtsIdx)
            {
                tempPt = rtPts[i];
                px = length * (tempPt.x - fixedPt.x) / 70 + fixedPt.x;
                py = length * (tempPt.y - fixedPt.y) / 70 + fixedPt.y;
                tempPt.x = px;
                tempPt.y = py;
            }
            else{
                tempPt=rtPts[fixPtsIdx];
            }
            quadraPts[i]=tempPt;
        }
        //将求得的纯净二维码的四个角点排序
        quadraPts = sortOrder(quadraPts);
        return quadraPts;
    }

    //获取无角点破碎二维码的纯净区域角点
    public Point[] getQuadraPts(ArrayList<MatOfPoint> roiContours)
    {
        Point[] quadraPts=new Point[4];

        //[1]取该区域最大轮廓的斜向矩形框的四个角点
        int maxContourSize=0;
        int maxContourIdx=0;
        int i=0;
        for (MatOfPoint contour:roiContours){
            if(contour.rows()>maxContourSize){
                maxContourIdx=i;
                maxContourSize=contour.rows();
            }
            i++;
        }
        MatOfPoint2f contour2f= new MatOfPoint2f(roiContours.get(maxContourIdx).toArray());
        RotatedRect rtRect=Imgproc.minAreaRect(contour2f);
        Point[] rtPts =new Point[4];
        rtRect.points(rtPts);
        //[2]
        quadraPts = sortOrder(rtPts);
        return quadraPts;
    }

    //根据提取到的4个ROI区域大小确定待拼接好的完整二维码的尺寸
    public int[] determinLength(List<Rect> roiRects) {
        int[] lengths=new int[2];
        //选择最小的rect
        int minLength=10000;
        for (Rect rect:roiRects) {
            int sideLngh=max(rect.height,rect.width);
            if(sideLngh<minLength) minLength=sideLngh;
        }
        //确定与最小rect最接近的length
        int delta=10000;
        int length=0,intactLNGH;
        for(int x:codeSize){
             if(abs(x-minLength)<delta){
                 length=x;
                 delta=abs(x-minLength);
             }
        }
        //返回length；intactLNGH
        intactLNGH=length*210/108;//完整二维码比破碎二维码210/108

        lengths[0]=intactLNGH;
        lengths[1]=length;
        return lengths;
    }

    //提取纯净的破碎二维码
    public void fetchPuryCode()
    {
        Mat hierarchy = new Mat();
        Mat drawCornerContour=brc.clone();
        Mat drawContour=brc.clone();
        double imageArea = brc.cols()*brc.rows();
//        Mat thre = new Mat(brc.size(),CV_8UC1);
        //Imgproc.threshold(brc,thre,128,255,Imgproc.THRESH_BINARY_INV);
        Mat thre=brcThre.clone();
        //setImageView(thre,imageView6);
//        Imgproc.cvtColor(brc, thre, Imgproc.COLOR_BGR2GRAY);//灰度化
        //Imgproc.equalizeHist(thre,thre);//直方图均衡
        //setImageView(thre,imageView6);
//        Imgproc.threshold(thre,thre,128,255,Imgproc.THRESH_BINARY);//二值化
        /*setImageView(thre,imageView5);*/
        Message msg=handler.obtainMessage(MSG_WHAT_SET_IMAGE,thre);
        handler.sendMessage(msg);

        //得到ROI区域，提取轮廓并筛选得到定位标识的轮廓
        ArrayList<MatOfPoint> roiContours = new ArrayList<>();
        ArrayList<MatOfPoint> roiCornerContours = new ArrayList<>();
        Imgproc.findContours(thre,roiContours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_NONE, new Point(0,0));

//        //筛选轮廓面积，去除孔洞（应当对图进行操作）
//        ArrayList<MatOfPoint> roiSelectedContours = new ArrayList<>();
//        for (MatOfPoint contour:roiContours){
//            if (Imgproc.contourArea(contour)>50) {
//                roiSelectedContours.add(contour);
//            }
//        }

        Imgproc.drawContours(drawContour,roiContours,-1,new Scalar(0,255,0),1);
//        setImageView(drawCornerContour,imageView5);
        Message msg1=handler.obtainMessage(MSG_WHAT_SET_IMAGE,drawContour);
        handler.sendMessage(msg1);


        //通过定位标识具有两层内轮廓的特点找到定位标识轮廓，在进行轮廓面积筛选和轮廓筛选，并获取纯净二维码的四个角点
        //[1]筛选出只具有两层内轮廓的轮廓
        List<Integer> layer2in = childContours(roiContours,hierarchy);
        //[2]进行圆度筛选
        for(int i=0;i<layer2in.size();i++)
        {
            int idx = layer2in.get(i);
            double roundness= calRoundness(roiContours.get(idx));
            if (abs(roundness-2/PI)<0.08)
            {
                roiCornerContours.add(roiContours.get(idx));
            }
        }

        Imgproc.drawContours(drawCornerContour,roiCornerContours,-1,new Scalar(0,255,0),1);
//        setImageView(drawCornerContour,imageView5);
        Message msg2=handler.obtainMessage(MSG_WHAT_SET_IMAGE,drawCornerContour);
        handler.sendMessage(msg2);

        String pic_path= String.format("/drawCornerContour%d.png",picIdx);
        img_path= Environment.getExternalStorageDirectory() + file_path+pic_path;
        Imgcodecs.imwrite(img_path,drawCornerContour);

        //筛选结果判定
        Point[] quadraPts = new Point[4];
        int order = -1;
//        int[] orderCorner=new int[4];
//        for(int i=0;i<4;i++){
//            orderCorner[i]=5;
//        }
        if (roiCornerContours.size() == 1)
        {
            Imgproc.drawContours(drawContour,roiCornerContours,-1,new Scalar(0,255,0),1);
            //setImageView(drawContour,imageView6);
            MatOfPoint2f contour= new MatOfPoint2f(roiCornerContours.get(0).toArray());
            order = getCornerOrder(contour);
//            orderCorner[picIdx]=order;
            quadraPts = getPuryPts(contour,order);
        }
        else
        {
            //不存在定位标识，判定该图片顺序为(上述三个之外的order)
            order=5;
            //根据白色边界产生最大轮廓的特点，求最大轮廓的斜向矩形框的四个点
            quadraPts=getQuadraPts(roiContours);
//            //此时 纯净四角点即为ROI四角点
//            quadraPts[0]=new Point(0,0);
//            quadraPts[1]=new Point(length,0);
//            quadraPts[2]=new Point(length,length);
//            quadraPts[3]=new Point(0,length);
        }
        codeOrder[picIdx]=order;

        //*********************从纯净的四个角点投影到ROI的全图四个角点***************

        //在区域中画出四个纯净角点
        Mat drawPuryCorners = brc.clone();
        for (Point pt:quadraPts){
            Imgproc.circle(drawPuryCorners,pt,1,new Scalar(255,100,100),1);
        }
        /*setImageView(drawPuryCorners,imageView6);*/
        handler.sendMessage(handler.obtainMessage(MSG_WHAT_SET_IMAGE,drawPuryCorners));
        pic_path= String.format("/drawPuryCorners%d.png",picIdx);
        img_path= Environment.getExternalStorageDirectory() + file_path+pic_path;
        Imgcodecs.imwrite(img_path,drawPuryCorners);

        //ROI全图的四个角点
        Point[] canonicalPoints = new Point[4];
        canonicalPoints[0] = new Point(0, 0);
        canonicalPoints[1] = new Point(length, 0);
        canonicalPoints[2] = new Point(length, length);
        canonicalPoints[3] = new Point(0, length);
        MatOfPoint2f canonicalMarker = new MatOfPoint2f();
        canonicalMarker.fromArray(canonicalPoints);
        //纯净二维码的四个角点
        MatOfPoint2f marker=new MatOfPoint2f(quadraPts);
        //计算投影变换
        Mat H = Imgproc.getPerspectiveTransform(marker,canonicalMarker);
        //进行投影变换
        Mat persimg=new Mat();
        Imgproc.warpPerspective(brcThre,persimg,H,brc.size(),Imgproc.INTER_LINEAR,0, new Scalar(0,255,0));
        persPic=persimg;

        pic_path= String.format("/persPic%d.png",picIdx);
        img_path= Environment.getExternalStorageDirectory() + file_path+pic_path;
        Imgcodecs.imwrite(img_path,persPic);
//        setImageView(persimg,imageView6);\
        //实际上该方法作用于perPics和persPic
    }

    /** ******************【3】四张图片的拼接*******************/
    public Mat bruteJoint()
    {
        Mat intactQrcode = new Mat(new Size(intactLNGH,intactLNGH),brcThre.type());
        List<Mat> rois = new ArrayList<Mat>(4);
        rois.add(new Mat(intactQrcode, new Rect(0,0,length,length)));
        rois.add(new Mat(intactQrcode, new Rect(intactLNGH-length,0,length,length)));
        rois.add(new Mat(intactQrcode, new Rect(intactLNGH-length,intactLNGH-length,length,length)));
        rois.add(new Mat(intactQrcode, new Rect(0,intactLNGH-length,length,length)));

        //如果codeOrder中的数为5，调整改无定位标识的顺序号
        int num_sum = 0;
        int num5 = 0;
        for (int i=0;i<4;i++) {
            if (codeOrder[i] != 5) {
                num_sum = num_sum + codeOrder[i];
            } else {
                num5 = i;
            }
        }
        codeOrder[num5]=6-num_sum;

//        //按照自己定义的顺序
//        int i=0;
//        for (Mat roi:rois)
//        {
//            Mat mask=new Mat(length,length,roi.depth(),Scalar.all(1));
//            Mat puryPic=persPics.get(i);
//            puryPic.copyTo(roi,mask);
//            i++;
//        }
        //使用算法判定的图片顺序
        for (int k=0;k<rois.size();k++)
        {
            Mat mask=new Mat(length,length,rois.get(codeOrder[k]).depth(),Scalar.all(1));
            Mat puryPic=persPics.get(k);
            puryPic.copyTo(rois.get(codeOrder[k]),mask);
        }
        img_path= file_path+"/intactCode.png";
        img_path= Environment.getExternalStorageDirectory() + img_path;
        Imgcodecs.imwrite(img_path,intactQrcode);
//        setImageView(intactQrcode,imageView6);
        return intactQrcode;
    }

    /** ******************【4】zxing库进行二维码的解码*************/
    private void qrCodeRecognization() throws FileNotFoundException {
        File photo = new File(Environment.getExternalStorageDirectory().getPath() + file_path);
        FileInputStream in = new FileInputStream(photo.getPath()+"/intactCode.png");
        Bitmap bitmap  = BitmapFactory.decodeStream(in);
        String result = decodeFromPhoto(bitmap);
        if (TextUtils.isEmpty(result)) {
            Message msg = handler.obtainMessage(MSG_WHAT_SHOW_TOAST,"未能解码出二维码内容！");
            handler.sendMessage(msg);
            Message msgModifyRatio=handler.obtainMessage(MSG_WHAT_TRY_MODIFYRATIO);
            handler.sendMessage(msgModifyRatio);
        }
        else {
            decodeFlag=true;
            //"拼接成功"或者Length=120退出尝试
            if(decodeFlag||length==120){
//                String text="";
//                errorHandler.obtainMessage(MSG_WHAT_TRY_MODEFYPARAMS,text);
                //定时器，本活动10秒后自动结束，返回上一活动
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 10000);//延时10s执行
            }
            String text=String.format("%d\n",length);
            Log.i(TAG_DECODE,text);
            Message msg_length = handler.obtainMessage(MSG_WHAT_UPDATE_HINTS,"最小满足需求的边长是："+text);
            handler.sendMessage(msg_length);
            Message msg = handler.obtainMessage(MSG_WHAT_SHOW_TOAST, result);
            handler.sendMessage(msg);
            Message msg1 = handler.obtainMessage(MSG_WHAT_UPDATE_HINTS, "二维码解码结果：" + result + "\n");
            handler.sendMessage(msg1);
            Message msg2 = handler.obtainMessage(MSG_WHAT_UPDATE_TITLE, "result：" + result);
            handler.sendMessage(msg2);
            Log.i(TAG_DECODE,"result:"+result);
        }
        return;
    }
}