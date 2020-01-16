package com.dji.P4MissionsDemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.dji.P4MissionsDemo.funcs.FuncTcpClient;
import com.dji.P4MissionsDemo.funcs.FuncTcpServer;

public class WiFiTestActivity extends Activity {

    private static final String TAG = "WiFiTestActivity";
    private RadioButton radioBtnServer, radioBtnClient;
    private Button btnFuncEnsure;
    private TextView txtShowFunc;
    private MyRadioButtonCheck myRadioButtonCheck = new MyRadioButtonCheck();
    private MyButtonClick myButtonClick = new MyButtonClick();

    private class MyRadioButtonCheck implements RadioButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            switch (compoundButton.getId()) {
                case R.id.radio_Server:
                    if (b) {
                        txtShowFunc.setText("你选则的功能是：服务器");
                    }
                    break;
                case R.id.radio_Client:
                    if (b) {
                        txtShowFunc.setText("你选则的功能是：客户端");
                    }
                    break;
            }
        }
    }

    private class MyButtonClick implements Button.OnClickListener {

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.btn_FunctionEnsure:
                    Intent intent = new Intent();
                    if (radioBtnServer.isChecked()) {
                        intent.setClass(WiFiTestActivity.this, FuncTcpServer.class);
                        startActivity(intent);
                    }
                    if (radioBtnClient.isChecked()) {
                        intent.setClass(WiFiTestActivity.this, FuncTcpClient.class);
                        startActivity(intent);
                    }
                    break;
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.function);
        bindID();
        bindListener();
        Log.e(TAG, "onStopwifi");
    }

    private void bindID() {
        radioBtnServer = (RadioButton) findViewById(R.id.radio_Server);
        radioBtnClient = (RadioButton) findViewById(R.id.radio_Client);
        btnFuncEnsure = (Button) findViewById(R.id.btn_FunctionEnsure);
        txtShowFunc = (TextView) findViewById(R.id.txt_ShowFunction);
    }

    private void bindListener() {
        radioBtnClient.setOnCheckedChangeListener(myRadioButtonCheck);
        radioBtnServer.setOnCheckedChangeListener(myRadioButtonCheck);
        btnFuncEnsure.setOnClickListener(myButtonClick);
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResumewifi");
        super.onResume();

    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPausewifi");
        super.onPause();
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

    }
}