package com.dji.P4MissionsDemo.udp;

/**
 * Created by BrainWang on 2016/3/21.
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

/**
 * 基于UDP协议的服务器端，对来自客户端的数据包进行应答
 *
 * @author <a href="http://wangbaiyuan">王柏元</a>
 */
public class UDPServer {
    /**
     * 端口
     */
    //int port=1888;
    DatagramSocket socket;
    String lastString = (-1 + "");
    int sameTime = 0;
    public handleReceiveData callback;

    public UDPServer(int port) throws SocketException {

        socket = new DatagramSocket(port);  //服务端DatagramSocket
        System.out.println("服务器启动。");
    }


    public void setReceiveCallback(handleReceiveData call) {
        callback = call;
    }

    public void service() throws IOException {
        while (true) {
            DatagramPacket dp = new DatagramPacket(new byte[102400], 102400);
            socket.receive(dp); //接收客户端信息

            byte[] data = dp.getData();

            callback.handleReceive(data,dp.getAddress().toString());
        }
    }

    public void start() throws SocketException, IOException {
        service();
    }

    private String mDestinationIP = "192.168.43.157";
    private int mPort = 7777;
    public void setDestination(String ip,int port){
        mDestinationIP = ip;
        mPort = port;
    }

    public void sendMsg(final byte[] data) {
        Thread send = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket != null) {

                        SocketAddress socketAddres = new InetSocketAddress(mDestinationIP, mPort);
                        socket.send(new DatagramPacket(data, data.length, socketAddres));
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        send.start();
    }


}