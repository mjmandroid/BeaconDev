package com.mjm2.beacondev;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2017/10/18 0018.
 */

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    EditText et_userid;
    EditText et_psw;
    private boolean isEnable = false;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        EventBus.getDefault().register(this);
        et_userid = (EditText) findViewById(R.id.et_userid);
        et_psw = (EditText) findViewById(R.id.et_psw);
        findViewById(R.id.btn_login).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isEnable = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isEnable = false;
    }

    @Override
    public void onClick(View v) {
        if (TextUtils.isEmpty(et_userid.getText().toString().trim())){
            Toast.makeText(getApplicationContext(),"用户名不能为空！",Toast.LENGTH_LONG).show();
            return;
        }
        if (TextUtils.isEmpty(et_psw.getText().toString().trim())){
            Toast.makeText(getApplicationContext(),"密码不能为空！",Toast.LENGTH_LONG).show();
            return;
        }
        Map<String,String> map = new HashMap<>();
        map.put("user_name",et_userid.getText().toString().trim());
        map.put("password",et_psw.getText().toString().trim());
        map.put("login_type","APP_Beacon");
        OkHttpUtils.requestServer(map,Constants.LOGIN_CONFIRM,1,0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventBussCallback(MessageCall messageCall){
        if (isEnable){
            if ("1".equalsIgnoreCase(messageCall.getState())) {
                try {
                    JSONObject object = new JSONObject(messageCall.getData());
                    if ("1".equalsIgnoreCase(object.optString("code"))){
                        JSONObject jsonObject = object.optJSONObject("data");
                        String userid = jsonObject.optString("userid");
                        getSharedPreferences("config",MODE_PRIVATE).edit().putString("userid",userid).commit();
                        Intent intent = new Intent(this,MainActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getApplicationContext(),object.optString("errMsg"),Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(getApplicationContext(),"网络出错...",Toast.LENGTH_LONG).show();
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
