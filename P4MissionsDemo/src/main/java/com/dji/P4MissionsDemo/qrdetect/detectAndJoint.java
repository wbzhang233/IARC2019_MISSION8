/**
 * Created by wbzhang on 2019/8/13.
 * Author:Captain of BUAA Yuyuan-Y.IARC2019
 * ******************************************************
 * Hints:class for QRcode Detect,joint and decode
 * this class offer an onekey_btn API for QRcode Task
 * ******************************************************
 * input: ~
 * output: intactCode,result，hints
 * ******************************************************
 * 继承该类
 * 实例化一个对象，调用其onekey_joint方法
 * */
package com.dji.P4MissionsDemo.qrdetect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static com.dji.P4MissionsDemo.qrdetect.QRCodeUtil.decodeFromPhoto;
import static java.lang.Math.abs;
import static org.opencv.core.Core.BORDER_DEFAULT;
import static org.opencv.core.CvType.CV_32FC1;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;


public class detectAndJoint {
    //********************二维码拼接所用全局变量*********************
    public static final double PI = 3.141592653;
    public int[] codeSize={21,42,64,84,105,126,147,168,189,210};//完整二维码的大小的枚举数组
    public static int intactLNGH = 210;
    public static final int length= 108;
    private List<Mat> pics = new ArrayList<>(4);
    private Mat pic; //检测时的当前图片
    private int picIdx = 0;//当前图片的顺序号
    private String img_path = new String();
    private String file_path = "/mission8/1m-jpg";

    //提取破碎二维码部分
    private Mat thresholdOutput;//阈值图
    public ArrayList<MatOfPoint> allContours=new ArrayList<>(0);
    public ArrayList<MatOfPoint> cornerContours=new ArrayList<>(0);//轮廓法中所有轮廓和角点的轮廓
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
    public Mat intactQrcode;//拼接成的完整二维码
    public String result;//解码出的结果

    public List<String> hints=new ArrayList<>(0);//返回错误提示
    public Boolean onekeyFlag=false;//一键拼接成功与否flag

    //*********************************************函数构造与变量获取部分********************************************************
    public detectAndJoint() {
//        List<Mat> imgs =loadPics();
//        this.pics = imgs;
        loadPics();//构造时自动读取图片
    }



    public Mat getIntactQrcode() {
        return intactQrcode;
    }

    public Rect getCheckedBox() {
        return checkedBox;
    }

    public List<String> getHints() {
        return hints;
    }

    public String getResult() {
        return result;
    }

    //**********************二维码拼接与解码线程**************************
    class qrcodeJointThread extends Thread{
        @Override
        public void run() {
            super.run();
            if(pics.size()==4) {
                List<Mat> jointResults = detectAndJoint();
                if(jointResults.size()==5){
                    onekeyFlag=true;
                    String text="已成功实现拼接...\n";
                    //成功拼接后进行解码
                    try {
                        qrCodeRecognization();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    hints.add(text);
                }
                else{
                    String text="未能成功实现拼接...\n";
                    hints.add(text);
                }
            }
            else{
                String text="未能读取到四张图片！\n";
                hints.add(text);
            }
        }
    }

    //*********************************************图像处理部分********************************************************
    //OpenCV库静态加载并初始化
    private void staticLoadCVLibraries(){
        boolean load = OpenCVLoader.initDebug();
        if(load) {
            Log.i("CV", "Open CV Libraries loaded...");
        }
    }

    //一键拼接接口
    public Boolean onekeyJoint()
    {
        qrcodeJointThread onekeyJointThread=new qrcodeJointThread();
        onekeyJointThread.start();
        return onekeyFlag;
    }

    //从机身内存载入图片
    public List<Mat> loadPics()
    {
        List<Mat> imgs=new ArrayList<>(4);
        Mat img;
        for (int i = 0; i < 4; i++)
        {
            String path;
            path=String.format("%s/%d.jpg",file_path, i+1);
            path= Environment.getExternalStorageDirectory()+path;
            try{
                img = Imgcodecs.imread(path,Imgcodecs.IMREAD_REDUCED_COLOR_4);
                if (!img.empty()){
                    pics.add(img);
                }
                else{
                    String text=String.format("Failed to read image%d!",i);
                    hints.add(text);
                }
            }catch (Exception e){
            }
        }
        imgs=pics;
        return imgs;
    }

    //***********************************二维码识别、拼接任务***************************
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

        double dist =  Math.sqrt(Math.pow((double)(point1.x - point2.x), 2) + Math.pow((double)(point1.y - point2.y), 2));
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
                detectedROI.add(checkedBox);//存入roi区域
                brc=new Mat(pic,checkedBox);//破碎二维码图片（原图）
                Imgproc.resize(brc,brc,new Size(length,length));
                brokenPics.add(brc);

                brcThre=new Mat(thresholdOutput,checkedBox);//破碎二维码图片（阈值图）
                Imgproc.threshold(brcThre,brcThre,180,255,Imgproc.THRESH_BINARY_INV);//ROI二值图反向
                Imgproc.resize(brcThre,brcThre,new Size(length,length));//大小归一化
                //setImageView(brc,imageView6);

                fetchPuryCode();//实际上输入为brc,输出为persPic
                persPics.add(persPic);
            }
            else {
                String text=String.format("pic%d：未能提取出二维码!\n",picIdx);
                hints.add(text);
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
            String text="未能提取出四张破碎二维码，拼接条件不具备\n";
            hints.add(text);
            return jointResults;
        }
    }

    //    //******************【1】提取破碎二维码*****************
    public Boolean detectAndFetch()
    {
//        setImageView(pic,imageView5);
        //图像预处理
        List<Mat> preImages = imgProcess(pic);
        thresholdOutput = preImages.get(2);

        //区块法提取
        final Mat thresholdOpen10 = preImages.get(4);
        fetchPack fetchPack1 = fetchBrokenCode(thresholdOpen10);
        if(!fetchPack1.getFetch_flag()){
            String text=String.format("pic%d:区块法,未能找到破碎二维码！\n",picIdx);
            hints.add(text);
        }

        //轮廓法提取
        final Mat thresholdOpen2=preImages.get(3);
        fetchPack fetchPack2 = findBrokenCode(thresholdOpen2);
        if(!fetchPack2.getFetch_flag()){
            String text=String.format("pic%d:轮廓法,未能找到破碎二维码！\n",picIdx);
            hints.add(text);
        }

        //两种方法找到的boundingBox进行校验
        Boolean flag = roiCheck();
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
        Imgproc.equalizeHist(gray,grayEqualizeHist);//直方图均衡
        preProcess.add(grayEqualizeHist);
        Imgproc.threshold(grayEqualizeHist,thresholdOut,250,255,Imgproc.THRESH_BINARY_INV);  //阈值化
        preProcess.add(thresholdOut);
        //形态学操作
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(3,3));
        //两次开操作
        Imgproc.morphologyEx(thresholdOut, thresholdOpen2,Imgproc.MORPH_OPEN,kernel,new Point(-1,-1),1);
        preProcess.add(thresholdOpen2);
        //多次开操作
        Imgproc.morphologyEx(thresholdOut, thresholdOpen10,Imgproc.MORPH_OPEN,kernel,new Point(-1,-1),6);
        preProcess.add(thresholdOpen10);
        //闭操作
        Imgproc.morphologyEx(thresholdOut, thresholdClose,Imgproc.MORPH_CLOSE,kernel,new Point(-1,-1),2);
        preProcess.add(thresholdClose);

        return preProcess;
    }

    public Mat scratchPart(Mat gray)
    {
        //cornerHarris角点检测
        Mat harris = gray.clone();
        Mat harris_dst = pic.clone();
        Mat img_corner = Mat.zeros(pic.size(), CV_32FC1);
        Imgproc.cornerHarris(harris, img_corner, 2, 3,0.04,BORDER_DEFAULT);
//        //归一化
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
        //Imgproc.putText(img_close,"img_close",new Point(0,0),1,1,Scalar.all(1));

        //轮廓提取
        List<MatOfPoint> closeContours = new ArrayList<MatOfPoint>();//闭操作图的所有轮廓
        List<MatOfPoint> squareContours = new ArrayList<MatOfPoint>();//方形轮廓
        Mat hierarchy = new Mat();
        Imgproc.findContours(img_close,closeContours,hierarchy,Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_NONE);//区块法只查找外部轮廓

        double image_area = thresholdOpen10.cols()*thresholdOpen10.rows();
        double area;
        int contourIdx = 0;
        //直接遍历所有外部轮廓，进行圆度和面积筛选
        for(MatOfPoint contour : closeContours)
        {
            area = Imgproc.contourArea(contour);//计算轮廓面积
            Point center = getCenter(contour);//获取轮廓中心
            double roundness = calRoundness(contour);//计算圆度
            //进行圆度和面积判定。圆度误差限为0.25，轮廓面积小于全图的1/4，且大于40*50
            if ( (abs(roundness-2/PI)<0.25) && (abs(roundness-2/PI)>0.005)
                    && (area<0.25*image_area) &&(area>2000)  )
            {
                squareContours.add(contour);//符合条件的方形轮廓
                String text;
                text=String.format("%s%.3f","roundness:",roundness);//蓝色标注圆度
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
        }
//        else
//        {
//            String text=String.format("pic%d:开操作区块法：没有找到方形轮廓，或者找到了多个方形轮廓\n",picIdx);
//            Message msg=handler.obtainMessage(MSG_WHAT_SHOW_TOAST,text);
//            handler.sendMessage(msg);
//            Message msg1=handler.obtainMessage(MSG_WHAT_UPDATE_HINTS,text);
//            handler.sendMessage(msg1);
//        }

        fetchMats.add(img_close);
        fetchMats.add(img_square);
        fetchResults.setFetchResult(fetchMats);
        return fetchResults;
        /*返回 1-检测flag 2-提取到的boundingBox1  3-（img_close，img_square） */
    }

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
            if ((abs(roundness-2/PI)<0.10) &&(area<image_area*0.25))
            {
                cornerContours.add(allContours.get(idx));
                Imgproc.drawContours(drawCornerContours,allContours,idx, new Scalar(255,0,0),2);
            }
        }

        //筛选结果判定
        Boolean find_flag=false;
        if (cornerContours.size() == 1)
        {
            boundingBox2 = Imgproc.boundingRect(cornerContours.get(0));
            fetchResults.setFetch_flag(true);
            fetchResults.setBoundingBox(boundingBox2);
            Imgproc.rectangle(drawBoundingbox2,boundingBox2.tl(),boundingBox2.br(), new Scalar(0,255,0));
        }
//        else
//        {
//            find_flag=true;
//            String text=String.format("pic%d:轮廓法,未能找到破碎二维码");
//            Message msg=handler.obtainMessage(MSG_WHAT_SHOW_TOAST,text);
//            handler.sendMessage(msg);
//        }

        fetchMats.add(drawAllContours);
        fetchMats.add(drawCornerContours);
        fetchMats.add(drawBoundingbox2);
        fetchResults.setFetchResult(fetchMats);
        return fetchResults;
        /*  返回 1-检测flag 2-boundingBox2  3-(drawAllContours,drawCornerContours,drawBoundingbox2)    */
    }

    //两种方法提取到的roi进行校验
    public Boolean roiCheck()
    {
        double existFlag1=boundingBox1.area();
        double existFlag2=boundingBox2.area();

        if(existFlag1*existFlag2==0)//至少有一个为0
        {
            //如果两个都为0，则返回false
            if(existFlag1+existFlag2==0) {
                return false;
            }
            //否则两个boundingBox中必有一个不为0，则取其输出到boundingBox
            else if(existFlag1!=0) {
                checkedBox=boundingBox1;
                return true;
            }
            else{
                checkedBox=boundingBox2;
                return true;
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

    //******************【2】寻找破碎二维码的投影角点*****************

    //大小归一化
//    public void normSize()
//    {
//        for (Mat pic:brokenPics)
//        {
//            //pic = pic.inv();//翻转
//            Imgproc.resize(pic,pic,new Size(length,length));
//        }
//    }

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

    //返回具有角点图片的顺序号（0，1，3）
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

    //提取纯净的破碎二维码
    public void fetchPuryCode()
    {
        Mat hierarchy = new Mat();
        Mat drawCornerContour=brc.clone();
        Mat drawContour=brc.clone();
        double imageArea = brc.cols()*brc.rows();
        Mat thre = new Mat(brc.size(),CV_8UC1);
        //Imgproc.threshold(brc,thre,128,255,Imgproc.THRESH_BINARY_INV);
        //Mat thre=brcThre.clone();
        //setImageView(thre,imageView6);
        Imgproc.cvtColor(brc, thre, Imgproc.COLOR_BGR2GRAY);//灰度化
        //Imgproc.equalizeHist(thre,thre);//直方图均衡
        //setImageView(thre,imageView6);
        Imgproc.threshold(thre,thre,128,255,Imgproc.THRESH_BINARY);//二值化
        /*setImageView(thre,imageView6);*/

        //得到ROI区域，提取轮廓并筛选得到定位标识的轮廓
        ArrayList<MatOfPoint> roiContours=new ArrayList<>();
        ArrayList<MatOfPoint> roiCornerContours = new ArrayList<>();
        Imgproc.findContours(thre,roiContours,hierarchy,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_NONE, new Point(0,0));
        Imgproc.drawContours(drawCornerContour,roiContours,-1,new Scalar(0,255,0),1);
        /*setImageView(drawCornerContour,imageView6);*/

        //通过定位标识具有两层内轮廓的特点找到定位标识轮廓，在进行轮廓面积筛选和轮廓筛选，并获取纯净二维码的四个角点
        //[1]筛选出只具有两层内轮廓的轮廓
        List<Integer> layer2in = childContours(roiContours,hierarchy);
        //[2]进行圆度筛选
        for(int i=0;i<layer2in.size();i++)
        {
            int idx = layer2in.get(i);
            double roundness= calRoundness(roiContours.get(idx));
            if (abs(roundness-2/PI)<0.10)
            {
                roiCornerContours.add(roiContours.get(idx));
            }
        }

        //筛选结果判定
        Point[] quadraPts = new Point[4];
        int order = -1;
        if (roiCornerContours.size() == 1)
        {
            Imgproc.drawContours(drawContour,roiCornerContours,-1,new Scalar(0,255,0),1);
            //setImageView(drawContour,imageView6);
            MatOfPoint2f contour= new MatOfPoint2f(roiCornerContours.get(0).toArray());
            order = getCornerOrder(contour);
            quadraPts = getPuryPts(contour,order);
        }
        else
        {
            //不存在定位标识，判定该图片顺序为2
            order=2;
            //此时 纯净四角点即为ROI四角点
            quadraPts[0]=new Point(0,0);
            quadraPts[1]=new Point(length,0);
            quadraPts[2]=new Point(length,length);
            quadraPts[3]=new Point(0,length);
        }

        //*********************从纯净的四个角点投影到ROI的全图四个角点***************

        //在区域中画出四个纯净角点
        Mat drawPuryCorners = brc.clone();
        for (Point pt:quadraPts){
            Imgproc.circle(drawPuryCorners,pt,1,new Scalar(255,100,100),1);
        }
        /*setImageView(drawPuryCorners,imageView6);*/

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
        //实际上该方法作用于perPics和persPic
    }

    //********************【3】进行四张图片的拼接*************
    public Mat bruteJoint()
    {
        intactQrcode = new Mat(new Size(intactLNGH,intactLNGH),brcThre.type());
        List<Mat> rois = new ArrayList<Mat>(4);
        rois.add(new Mat(intactQrcode, new Rect(0,0,length,length)));
        rois.add(new Mat(intactQrcode, new Rect(intactLNGH-length,0,length,length)));
        rois.add(new Mat(intactQrcode, new Rect(intactLNGH-length,intactLNGH-length,length,length)));
        rois.add(new Mat(intactQrcode, new Rect(0,intactLNGH-length,length,length)));
        int i=0;
        for (Mat roi:rois)
        {
            Mat mask=new Mat(length,length,roi.depth(),Scalar.all(1));
            Mat puryPic=persPics.get(i);
            puryPic.copyTo(roi,mask);
            i++;
        }
        img_path= file_path+"/intactCode.png";
        img_path= Environment.getExternalStorageDirectory() + img_path;
        Imgcodecs.imwrite(img_path,intactQrcode);
        return intactQrcode;
    }

    //********************【4】zxing库进行二维码的解码*************
    private void qrCodeRecognization() throws FileNotFoundException {
        File photo = new File(Environment.getExternalStorageDirectory().getPath() + file_path);
        FileInputStream in = new FileInputStream(photo.getPath()+"/intactCode.png");
        Bitmap bitmap  = BitmapFactory.decodeStream(in);
        result = decodeFromPhoto(bitmap);
        if (result.isEmpty()){
            String text="未识别出二维码内容！\n";
            hints.add(text);
        }
        else{
            String text="二维码内容是："+ result + "\n";
            hints.add(text);
        }
    }

}
