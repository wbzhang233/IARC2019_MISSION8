package com.dji.P4MissionsDemo.recognization.offline;

import android.app.Activity;
import android.content.SharedPreferences;

import com.baidu.speech.asr.SpeechConstant;
import com.dji.P4MissionsDemo.recognization.CommonRecogParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fujiayi on 2017/6/13.
 */

public class OfflineRecogParams extends CommonRecogParams {

    private static final String TAG = "OnlineRecogParams";

    public OfflineRecogParams(Activity context) {
        super(context);
    }


    public Map<String, Object> fetch(SharedPreferences sp) {

        Map<String, Object> map = super.fetch(sp);
        map.put(SpeechConstant.DECODER, 2);
        map.remove(SpeechConstant.PID); // 去除pid，只支持中文
        return map;

    }

    public static Map<String, Object> fetchOfflineParams() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(SpeechConstant.DECODER, 2);
        map.put(SpeechConstant.ASR_OFFLINE_ENGINE_GRAMMER_FILE_PATH, "assets:///baidu_speech_grammar.bsg");
        map.putAll(fetchSlotDataParam());
        return map;
    }

    public static Map<String, Object> fetchSlotDataParam() {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            JSONObject json = new JSONObject();
            json.put("cname",new JSONArray().put("摄像头"))
                    .put("lname",new JSONArray("指示灯"))
                    .put("direction", new JSONArray().put("上").put("下").put("左").put("右").put("前").put("后"))
                    .put("command", new JSONArray().put("搜索").put("起飞").put("降落").put("返回").put("拍照").put("前进").put("后退").put("转动").put("悬停"))
                    .put("act",new JSONArray().put("打开").put("关闭"))
                    .put("plan",new JSONArray().put("方案一").put("方案二"))
                    .put("number",new JSONArray().put("第一个").put("第二个").put("第三个").put("第四个"))
                    .put("cameraact",new JSONArray().put("俯视").put("仰视"))
                    .put("angle",new JSONArray().put("四十五度").put("九十度").put("一百四十五度").put("一百八十度"));
            map.put(SpeechConstant.SLOT_DATA, json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

}
