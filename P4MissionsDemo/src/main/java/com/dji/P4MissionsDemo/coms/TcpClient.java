package com.dji.P4MissionsDemo.coms;

import android.content.Intent;
import android.util.Log;

import com.dji.P4MissionsDemo.funcs.FuncTcpClient;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by Jason Zhu on 2017-04-25.
 * Email: cloud_happy@163.com
 */

public class TcpClient implements Runnable {
    private String TAG = "TcpClient";
    private String serverIP = "192.168.88.141";
    private int serverPort = 1234;
    private PrintWriter pw;
    private InputStream is;
    private DataInputStream dis;
    private boolean isRun = true;
    private Socket socket = null;
    byte buff[]  = new byte[4096];
    private String rcvMsg;
    private int rcvLen;
    private OutputStream mOutputstream;



    public TcpClient(String ip , int port){
        this.serverIP = ip;
        this.serverPort = port;

    }

    public void closeSelf(){
        isRun = false;
    }

    public void send(String msg){
        pw.print(msg);
        pw.flush();
    }

    public OutputStream getmOutputstream() {
        return mOutputstream;
    }

    public boolean isConnecting(){

        return (socket != null)&&(socket.isConnected()&&!socket.isClosed() && !socket.isInputShutdown() && !socket.isOutputShutdown());
    }

    @Override
    public void run() {
        try {
            socket = new Socket(serverIP, serverPort);
            socket.setSoTimeout(5000);
            pw = new PrintWriter(socket.getOutputStream(), true);
            is = socket.getInputStream();
            mOutputstream = socket.getOutputStream();
            dis = new DataInputStream(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (isRun){
            try {
                rcvLen = dis.read(buff);
                rcvMsg = new String(buff,0,rcvLen,"utf-8");
                Log.i(TAG, "run: 收到消息:"+ rcvMsg);
                if(rcvMsg.equals("pintPosX")){
                    rcvLen = dis.read(buff);
                    rcvMsg = new String(buff,0,rcvLen,"utf-8");
                    float pintPosX = Float.valueOf(rcvMsg.split("pintPosY")[0]);
                    float pintPosY = Float.valueOf(rcvMsg.split("pintPosY")[1]);
                    Intent intent1 =new Intent();
                    intent1.setAction("pintPosition");
//                    Bundle bundle = new Bundle();
//                    bundle.putFloat("pintPosX",pintPosX);
//                    bundle.putFloat("pintPosY",pintPosY);
//                    intent1.putExtras(bundle);
                    intent1.putExtra("pintPosX",pintPosX);
                    intent1.putExtra("pintPosY",pintPosY);
                    FuncTcpClient.context.sendBroadcast(intent1);//将消息发送给主界面
                    continue;
                }
                Intent intent =new Intent();
                intent.setAction("tcpClientReceiver");
                intent.putExtra("tcpClientReceiver",rcvMsg);
                FuncTcpClient.context.sendBroadcast(intent);//将消息发送给主界面
                if (rcvMsg.equals("QuitClient")){   //服务器要求客户端结束
                    isRun = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        try {
            pw.close();
            is.close();
            dis.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
