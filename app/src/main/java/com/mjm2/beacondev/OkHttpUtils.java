package com.mjm2.beacondev;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Administrator on 2017/10/18 0018.
 */

public class OkHttpUtils {
    private static OkHttpClient client;
    private static int uploadCount = 0,moveCount = 0;

    public static void requestServer(Map<String,String> map,String url, final int tag,final int count){
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)//连接超时(单位:秒)
                    .writeTimeout(20, TimeUnit.SECONDS)//写入超时(单位:秒)
                    .readTimeout(20, TimeUnit.SECONDS)//读取超时(单位:秒)
                    .build();
        }
        FormBody.Builder builder = new FormBody.Builder();
        Iterator iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            builder.add(key.toString(), value.toString());
        }
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println(e.toString());
                MessageCall messageCall = new MessageCall();
                messageCall.setState("0");
                EventBus.getDefault().post(messageCall);
                Log.e("TAG2","onResponse");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String data = response.body().string();
                Log.d("res:","chengg"+data);
                Log.e("TAG2","onResponse");
                MessageCall messageCall = new MessageCall();
                messageCall.setState("1");
                messageCall.setData(data);
                messageCall.setMove(tag);
                messageCall.setMyCOunt(count);
                try {
                    JSONObject object = new JSONObject(data);
                    if ("1".equalsIgnoreCase(object.optString("code")) && tag == 1) {
                        uploadCount ++;
                        messageCall.setUploadCount(uploadCount);
                    } else if ("1".equalsIgnoreCase(object.optString("code")) && tag == 0){
                        moveCount ++;
                        messageCall.setMoveCount(moveCount);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                EventBus.getDefault().post(messageCall);
            }
        });
    }
}
