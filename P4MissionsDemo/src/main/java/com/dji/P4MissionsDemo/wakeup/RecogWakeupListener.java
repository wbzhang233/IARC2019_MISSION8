package com.dji.P4MissionsDemo.wakeup;

import android.os.Handler;
import android.os.Message;

import com.dji.P4MissionsDemo.recognization.IStatus;

/**
 * Created by fujiayi on 2017/9/21.
 */

public class RecogWakeupListener extends SimpleWakeupListener implements IStatus {

    private static final String TAG = "RecogWakeupListener";

    private Handler handler;

    public RecogWakeupListener(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void onSuccess(String word, WakeUpResult result) {
        super.onSuccess(word, result);
        Message msg = Message.obtain();
        msg.what = STATUS_WAKEUP_SUCCESS;
        msg.obj = word;
        handler.sendMessage(msg);
    }
}
