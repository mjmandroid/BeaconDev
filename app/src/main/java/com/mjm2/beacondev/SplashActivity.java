package com.mjm2.beacondev;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

/**
 * Created by Administrator on 2017/10/18 0018.
 */

public class SplashActivity extends AppCompatActivity {
    private Handler handler = new Handler();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(getSharedPreferences("config",MODE_PRIVATE).getString("userid",""))) {
                    startActivity(new Intent(SplashActivity.this,LoginActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this,MainActivity.class));
                }
            }
        },1500);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
