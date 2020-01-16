package com.dji.P4MissionsDemo.coms;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Size;

import com.dji.P4MissionsDemo.tensorflow.DetectorAPI;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import dji.sdk.codec.DJICodecManager;

/**
 * Created by Jason Zhu on 2017-04-24.
 * Email: cloud_happy@163.com
 */

public class TcpServer implements Runnable {
    private String TAG = "TcpServer";
    private int port = 1234;
    private boolean isListen = true;   //线程监听标志位
    public ArrayList<ServerSocketThread> SST = new ArrayList<ServerSocketThread>();
    private Handler mHandler = null;
    private Context mcontext = null;
    private static final int MSG_WHAT_tcpServerReceiver_ip = 2;
    private static final int MSG_WHAT_tcpServerReceiver_rcvMsg = 3;
    private static final int MSG_WHAT_tcpServerReceiver_liveFrame = 4;
    private static final int MSG_WHAT_tcpServerReceiver_liveSurface_videobuffer = 5;
    private static final int MSG_WHAT_tcpServerReceiver_State = 6;
    private static final int MSG_WHAT_tcpServerReceiver_photo = 7;
    private static final int MSG_WHAT_tcpServerSave_photo = 8;

    ArrayList<DJICodecManager> mDJICodecManagerList;
    private List<DetectorAPI> mDetectorAPI;
    private DJICodecManager mDJICodecManager;
    private int count = 0;
    private boolean loseFrame = false;


    public TcpServer(int port){
        this.port = port;
    }

    public TcpServer(int port, Handler handler){
        this.port = port;
        this.mHandler = handler;
    }
    public TcpServer(int port, Handler handler,List<DetectorAPI> detectorAPI){
        this.port = port;
        this.mHandler = handler;
        this.mDetectorAPI = detectorAPI;
//        mDetectorAPI.onStart();
    }
    public TcpServer(int port, Handler handler,DJICodecManager manager){
        this.port = port;
        this.mHandler = handler;
        this.mDJICodecManager = manager;
    }
    public TcpServer(int port, Handler handler,ArrayList<DJICodecManager> mList){
        this.port = port;
        this.mHandler = handler;
        mDJICodecManagerList = mList;
    }
    public TcpServer(int port, Context mcontext){
        this.port = port;
        this.mcontext = mcontext;
    }

    //更改监听标志位
    public void setIsListen(boolean b){
        isListen = b;
    }

    public void closeSelf(){
        isListen = false;
        for (ServerSocketThread s : SST){
            s.isRun = false;
        }
        SST.clear();
    }

    private Socket getSocket(ServerSocket serverSocket){
        try {
            return serverSocket.accept();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "run: 监听超时");
            return null;
        }
    }

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(5000);
            while (isListen){
                Log.i(TAG, "run: 开始监听...");

                Socket socket = getSocket(serverSocket);
                if (socket != null){
                    new ServerSocketThread(socket);
                }
            }

            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class ServerSocketThread extends Thread {
        Socket socket = null;
        private PrintWriter pw;
        private InputStream is = null;
        private OutputStream os = null;
        private String ip = null;
        private boolean isRun = true;
        private boolean isFull = false;
        private long startTime = 0;
        private int fpsCount = 0;
        public int pictureIndex=0;

        public Boolean deletePicture() {
            pictureIndex--;
            return true;
        }

        public void setPictureIndex(int pictureIndex) {
            this.pictureIndex = pictureIndex;
        }

        //        boolean load = OpenCVLoader.initDebug();

        ServerSocketThread(Socket socket){
            this.socket = socket;
            ip = socket.getInetAddress().toString();
            Log.i(TAG, "ServerSocketThread:检测到新的客户端联入,ip:" + ip);
            if (mHandler != null && SST.indexOf(this)<0) {
                mHandler.sendMessage(mHandler.obtainMessage(MSG_WHAT_tcpServerReceiver_ip, ip));
            }
//            Intent intent_ip =new Intent();
//            intent_ip.setAction("tcpServerReceiver_ip");
//            intent_ip.putExtra("tcpServerReceiver_ip",ip);
//            FuncTcpServer.context.sendBroadcast(intent_ip);//将消息发送给主界面
            try {
                socket.setSoTimeout(5000);
                os = socket.getOutputStream();
                is = socket.getInputStream();
                pw = new PrintWriter(os,true);
                start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void saveBitmap(Bitmap bitmap,String path, String filename) throws IOException
        {
            File file = new File(path + filename+".jpg");
            if(file.exists()){
                file.delete();
            }
            FileOutputStream out;
            try{
                out = new FileOutputStream(file);
                if(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out))
                {
                    out.flush();
                    out.close();
                }
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        public void send(String msg){
            pw.print(msg);
            pw.flush(); //强制送出数据
        }

        public OutputStream getmOutputstream() {
            return os;
        }

        @Override
        public void run() {
            byte buff[]  = new byte[4];
            String rcvMsg;
            int rcvLen;
            boolean needIp = true;
            SST.add(this);
            boolean firstTime = true;
            while (isRun && !socket.isClosed() && !socket.isInputShutdown()){

                try {
//                    byte[] sizeArray = new byte[4];
//                    int frameKey_len = is.read(sizeArray);
//                    String frameKey = new String(buff,0,frameKey_len,"utf-8");;

                    if ((rcvLen = is.read(buff)) != -1 ){
                        rcvMsg = new String(buff,0,rcvLen,"utf-8");
                        Log.i(TAG, "run:收到消息: " + rcvMsg);
                        //*-----------------------------------------*-- ImageView ----*-------------------------------//
                        if(rcvMsg.equals("LIVE")){
                            loseFrame = true;
                            if (mHandler != null && firstTime) {
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_WHAT_tcpServerReceiver_State, ip+" -- on LIVE\n"));
                                firstTime = false;
                            }
                            boolean isRunLive = true;
                            while (isRunLive && isRun && mHandler !=null) {
                                if (!isFull) {
                                    startTime = System.nanoTime();
                                    isFull = true;
                                }
                                byte[] sizeArray = new byte[4];
                                try {
                                    is.read(sizeArray);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                int picLength = ByteBuffer.wrap(sizeArray).asIntBuffer().get();
                                if (picLength == 0){
                                    isRunLive = false;
                                }
                                else if(picLength > 5000000||picLength<0){
                                    break;
                                }
                                Log.d(TAG, "picLength:" + picLength);
                                byte[] b = new byte[picLength];
                                try {
                                    int totalLen = 0;
                                    int bufferSize = 4 * 1024;
                                    //when the read totalLen is less than the picLength
                                    while (totalLen < picLength) {
                                        int len = 0;
                                        //if the left data is less than bufferSize,read them all ,
                                        //else read them by bufferSize
                                        if (bufferSize >= picLength - totalLen) {
                                            len = is.read(b, totalLen, picLength - totalLen);
                                        } else {
                                            len = is.read(b, totalLen, bufferSize);
                                        }
                                        totalLen += len;
                                    }

                                    //*********-------------for opencv -------------************//
//                                    if(!load){
//                                        load = OpenCVLoader.initDebug();
//                                    }
//                                    ByteArrayInputStream inputStream = new ByteArrayInputStream(b);
//                                    Bitmap srcBitmap = BitmapFactory.decodeStream(inputStream);
//                                    int top, bottom, left, right;
//                                    int borderType = Core.BORDER_CONSTANT;
//                                    Random rng;
//                                    Mat src = new Mat(srcBitmap.getWidth(),srcBitmap.getHeight(), CvType.CV_8UC1);
//                                    Mat dst = new Mat(srcBitmap.getWidth(),srcBitmap.getHeight(), CvType.CV_8UC1);
//                                    Utils.bitmapToMat(srcBitmap,src);
//                                    top = (int) (0.05*src.rows()); bottom = top;
//                                    left = (int) (0.05*src.cols()); right = left;
//                                    rng = new Random();
//                                    Scalar value = new Scalar( rng.nextInt(256),
//                                            rng.nextInt(256), rng.nextInt(256) );
//                                    Core.copyMakeBorder( src, dst, top, bottom, left, right, borderType, value);
//                                    Bitmap bitmap = Bitmap.createBitmap(dst.width(),dst.height(), Bitmap.Config.ARGB_8888);
//                                    Utils.matToBitmap(dst,bitmap);
                                    //*********-------------for opencv -------------************//

                                    ByteArrayInputStream inputStream = new ByteArrayInputStream(b);
                                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

//                                    mDetectorAPI.onStart();
//                                    mDetectorAPI.onFeed(bitmap,new Size(bitmap.getWidth(), bitmap.getHeight()));
                                    Message message = mHandler.obtainMessage();
                                    message.what = MSG_WHAT_tcpServerReceiver_liveFrame;
                                    message.arg1 = SST.indexOf(this);
                                    if (bitmap != null) {
                                        message.obj = bitmap;
                                        mHandler.sendMessage(message);
                                        if (fpsCount < 60){
                                            fpsCount++;
                                        }else {
                                            isFull = false;
                                            long endTime = System.nanoTime();
                                            long gap = endTime - startTime;
                                            long fps = 1000000000 / (gap / fpsCount);
                                            Log.d(TAG, "gap:" + gap);
                                            Log.d(TAG, "fps:" + fps);
                                            fpsCount = 0;
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                isRunLive = false;
                            }
                        }
                        //*-----------------------------------------*-- YUV420  ----*-------------------------------//
                        if(rcvMsg.equals("YUV4")){
                            loseFrame = true;
                            if (mHandler != null && firstTime) {
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_WHAT_tcpServerReceiver_State, ip+" -- on YUV4\n"));
                                firstTime = false;
                            }
                            boolean isRunLive = true;
                            while (isRunLive && isRun && mHandler !=null) {

//                                byte[] widthArray = new byte[4];
//                                try {
//                                    is.read(widthArray);
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                int width = ByteBuffer.wrap(widthArray).asIntBuffer().get();
//
//                                byte[] heightArray = new byte[4];
//                                try {
//                                    is.read(heightArray);
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                                int height = ByteBuffer.wrap(heightArray).asIntBuffer().get();

                                byte[] sizeArray = new byte[4];
                                try {
                                    is.read(sizeArray);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                int picLength = ByteBuffer.wrap(sizeArray).asIntBuffer().get();
                                if(picLength >= 99999999){
                                    {
                                        send("130");
                                        try {
                                            while (is.skip(picLength) >= 0) {

                                            }
                                            ;
                                        } catch (Exception e1) {
                                            send("131");
                                        }
                                        break;
                                    }
                                }
                                byte[] yuvFrame = new byte[picLength];
                                try {
                                    int totalLen = 0;
                                    int bufferSize = 4 * 1024;
                                    //when the read totalLen is less than the picLength
                                    while (totalLen < picLength) {
                                        int len = 0;
                                        //if the left data is less than bufferSize,read them all ,
                                        //else read them by bufferSize
                                        if (bufferSize >= picLength - totalLen) {
                                            len = is.read(yuvFrame, totalLen, picLength - totalLen);
                                        } else {
                                            len = is.read(yuvFrame, totalLen, bufferSize);
                                        }
                                        totalLen += len;
                                    }
                                    ByteArrayInputStream inputStream = new ByteArrayInputStream(yuvFrame);
                                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                    if (bitmap != null) {
                                        Message message = mHandler.obtainMessage();
                                        message.what = MSG_WHAT_tcpServerReceiver_liveFrame;
                                        message.arg1 = SST.indexOf(this);
//                                        mDetectorAPI.get(SST.indexOf(this)).onStart();
//                                        mDetectorAPI.get(SST.indexOf(this)).onFeed(bitmap, new Size(bitmap.getWidth(), bitmap.getHeight()));
                                        message.obj = bitmap;
                                        mHandler.sendMessage(message);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    send("130");
                                    try {
                                        while (is.skip(picLength) >=0){

                                        };
                                    }
                                    catch (Exception e1){
                                        send("131");
                                    }
                                }
                                isRunLive = false;
                            }
                        }
                        //*-----------------------------------------*-- surface ----*-------------------------------//
                        else if(rcvMsg.equals("SURF")){
                            if (mHandler != null) {
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_WHAT_tcpServerReceiver_State, ip+" -- on surface LIVE\n"));
                            }
                            boolean isRunLive = true;
                            while (isRunLive && isRun && mHandler !=null) {
                                if (!isFull) {
                                    startTime = System.nanoTime();
                                    isFull = true;
                                }
                                byte[] sizeArray = new byte[4];
                                try {
                                    is.read(sizeArray);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                int picLength = ByteBuffer.wrap(sizeArray).asIntBuffer().get();
                                if (picLength == 0){
                                    isRunLive = false;
                                }else if(picLength > 50000||picLength<0){
                                    break;
                                }
                                byte[] videobufferSize = new byte[4];
                                try {
                                    is.read(videobufferSize);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                int surfaceLength = ByteBuffer.wrap(videobufferSize).asIntBuffer().get();
                                if (surfaceLength == 0){
                                    isRunLive = false;
                                }else if(surfaceLength > 50000||surfaceLength<0){
                                    break;
                                }
                                Log.d(TAG, "surfaceLength:" + surfaceLength);
                                byte[] videoBuffer = new byte[surfaceLength];
                                try {
                                    int totalLen = 0;
                                    int bufferSize = 4 * 1024;
                                    //when the read totalLen is less than the picLength
                                    while (totalLen < surfaceLength) {
                                        int len = 0;
                                        //if the left data is less than bufferSize,read them all ,
                                        //else read them by bufferSize
                                        if (bufferSize >= surfaceLength - totalLen) {
                                            len = is.read(videoBuffer, totalLen, surfaceLength - totalLen);
                                        } else {
                                            len = is.read(videoBuffer, totalLen, bufferSize);
                                        }
                                        totalLen += len;
                                    }
                                    Message message = mHandler.obtainMessage();
                                    message.what = MSG_WHAT_tcpServerReceiver_liveSurface_videobuffer;
                                    message.arg1 = SST.indexOf(this);
                                    message.arg2 = picLength;
//                                    if (count++ %60==0) {
                                    message.obj = videoBuffer;
                                    mHandler.sendMessage(message);

//                                    }
//                                    mDJICodecManager.sendDataToDecoder(videoBuffer,picLength);
//                                    mDJICodecManagerList.get(SST.indexOf(this)).sendDataToDecoder(videoBuffer, surfaceLength);
                                    if (fpsCount < 60){
                                        fpsCount++;
                                    }else {
                                        isFull = false;
                                        long endTime = System.nanoTime();
                                        long gap = endTime - startTime;
                                        long fps = 1000000000 / (gap / fpsCount);
                                        Log.d(TAG, "gap:" + gap);
                                        Log.d(TAG, "fps:" + fps);
                                        fpsCount = 0;
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        //*-----------------------------------------*-- Picture detector FOR debug ----*-------------------------------//
                        else if(rcvMsg.equals("PICT")){
                            if (mHandler != null && firstTime) {
                                mHandler.sendMessage(mHandler.obtainMessage(MSG_WHAT_tcpServerReceiver_State, ip+" -- on PICTURE\n"));
                                firstTime = false;
                            }
                            boolean isRunLive = true;
                            while (isRunLive && isRun && mHandler !=null) {
                                byte[] sizeArray = new byte[4];
                                try {
                                    is.read(sizeArray);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                int picLength = ByteBuffer.wrap(sizeArray).asIntBuffer().get();
                                if (picLength <= 0){
                                    isRunLive = false;
                                }
                                Log.d(TAG, "picLength:" + picLength);
                                byte[] b = new byte[picLength];
                                try {
                                    int totalLen = 0;
                                    int bufferSize = 4 * 1024;
                                    //when the read totalLen is less than the picLength
                                    while (totalLen < picLength) {
                                        int len = 0;
                                        //if the left data is less than bufferSize,read them all ,
                                        //else read them by bufferSize
                                        if (bufferSize >= picLength - totalLen) {
                                            len = is.read(b, totalLen, picLength - totalLen);
                                        } else {
                                            len = is.read(b, totalLen, bufferSize);
                                        }
                                        totalLen += len;
                                    }
                                    ByteArrayInputStream inputStream = new ByteArrayInputStream(b);
                                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

//                                    File jpgdir = new File(Environment.getExternalStorageDirectory().getPath() + "/mission8/test");
                                    File jpgdir = new File(Environment.getExternalStorageDirectory().getPath() + "/qrCodes");
                                    if (!jpgdir.exists()){
                                        jpgdir.mkdirs();
                                    }

                                    //String filename="1";
                                    String jpgpath=jpgdir.getPath()+"/";
                                    File[] tempList = jpgdir.listFiles();

//                                    int file_num=tempList.length;
                                    int file_num=pictureIndex%4+1;
//                                    String filename=Integer.toString(file_num+1);
                                    String filename=String.format("%d",file_num);
                                    saveBitmap(bitmap,jpgpath, filename);
                                    pictureIndex++;

                                    mHandler.sendMessage(mHandler.obtainMessage(MSG_WHAT_tcpServerReceiver_State, ip+" finished\n"+jpgpath+filename));
                                    mHandler.sendMessage(mHandler.obtainMessage(MSG_WHAT_tcpServerSave_photo, pictureIndex));

//                                    mDetectorAPI.get(SST.indexOf(this)).onStart();
//                                    mDetectorAPI.get(SST.indexOf(this)).onFeed(bitmap, new Size(bitmap.getWidth(), bitmap.getHeight()));
                                    Message message = mHandler.obtainMessage();
                                    message.what = MSG_WHAT_tcpServerReceiver_photo;
                                    message.arg1 = SST.indexOf(this);
                                    if (bitmap != null) {
                                        message.obj = bitmap;
                                        mHandler.sendMessage(message);
                                        if (fpsCount < 60){
                                            fpsCount++;
                                        }else {
                                            isFull = false;
                                            long endTime = System.nanoTime();
                                            long gap = endTime - startTime;
                                            long fps = 1000000000 / (gap / fpsCount);
                                            Log.d(TAG, "gap:" + gap);
                                            Log.d(TAG, "fps:" + fps);
                                            fpsCount = 0;
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                isRunLive = false;
                            }
                        }
                        else {
//                            if(loseFrame) {
//                                if (mcontext != null) {
//                                    Intent intent = new Intent();
//                                    intent.setAction("tcpServerReceiver");
//                                    intent.putExtra("tcpServerReceiver", rcvMsg);
//                                    mcontext.sendBroadcast(intent);//将消息发送给主界面
//                                    if (rcvMsg.equals("QuitServer")) {
//                                        isRun = false;
//                                    }
//                                } else if (mHandler != null) {
//                                    if (needIp) {
//                                        mHandler.sendMessage(mHandler.obtainMessage(MSG_WHAT_tcpServerReceiver_rcvMsg, 0, 0, ip));
//                                        needIp = false;
//                                    }
//                                    mHandler.sendMessage(mHandler.obtainMessage(MSG_WHAT_tcpServerReceiver_rcvMsg, 1, 0, "lose Frame once\n"));
//                                    send("130");
//                                    try {
//                                        is.skip(99999999);
//                                    }
//                                    catch (Exception e1){
////                                        send("131");
//                                    }
//
//                                }
//                                loseFrame = false;
//                                firstTime = true;
//                            }
                        }
                    }
                    else {
                        needIp = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    needIp = true;
                }
            }
            try {
                send("QuitClient");
                is.close();
                os.close();
                pw.close();
                socket.close();
                SST.clear();
                Log.i(TAG, "run: 断开连接");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
