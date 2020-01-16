package com.dji.P4MissionsDemo.udp;

/**
 * Created by BrainWang on 2016/3/21.
 */
public interface handleReceiveData {
     public abstract void handleReceive(byte[] data);
     public abstract void handleReceive(byte[] data,String ipAddress);
}
