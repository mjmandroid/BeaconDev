package com.mjm2.beacondev;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mjm2.beacondev.test.MyService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1001;
    private TextView tv_change,tv_area,tv_projectName,tv_position;
    private boolean isEnable;
    private TextView tv_count,tv_noupload_count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        EventBus.getDefault().register(this);

    }

    @Override
    public void onBackPressed() {

    }

    private void initView() {
        tv_change = (TextView) findViewById(R.id.tv_change);
        tv_area = (TextView) findViewById(R.id.tv_area);
        tv_projectName = (TextView) findViewById(R.id.tv_project);
        tv_position = (TextView) findViewById(R.id.tv_location);
        tv_count = (TextView) findViewById(R.id.tv_uplod_count);
        tv_noupload_count = (TextView) findViewById(R.id.tv_noupload_count);
        tv_change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSharedPreferences("config",MODE_PRIVATE).edit().putString("userid","");
                finish();
                startActivity(new Intent(MainActivity.this,LoginActivity.class));
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onResume() {
        super.onResume();
        isEnable = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            } else {
                startService();
            }
        } else {
            startService();
        }
    }

    private void startService() {
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //获取此设备的默认蓝牙适配器。
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(mBluetoothAdapter!=null) {
            if (mBluetoothAdapter.isEnabled()) {

                Intent startIntent = new Intent(MainActivity.this, MyService.class);
                startService(startIntent);
            } else {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivity(enableBluetooth);
            }
        } else {
            Toast.makeText(getApplicationContext(),"不支持蓝牙",Toast.LENGTH_LONG).show();
        }
    }
    public  boolean isGpsEnable(final Context context) {
        LocationManager locationManager
                = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!isGpsEnable(this)){
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent,100);
                    }else {
                        startService();
                    }
                } else {
                    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void eventBussCallback(MessageCall messageCall){
        if (isEnable){
            if (messageCall.getState().equalsIgnoreCase("1")){
                try {
                    JSONObject object = new JSONObject(messageCall.getData());
                    if ("1".equalsIgnoreCase(object.optString("code"))){
                        if (messageCall.getMove() == 1){
                            tv_area.setText("检测到您处在工作区");
                            JSONObject jsonObject = object.optJSONObject("data");
                            String name = jsonObject.optString("projectname");
                            String position = jsonObject.optString("position");
                            tv_projectName.setText("项目:"+name);
                            tv_position.setText("位置:"+position);
                            tv_count.setText("存在上传次数："+messageCall.getUploadCount());
                        }  else if (messageCall.getMove() == 0) {
                            tv_noupload_count.setText("离开上传次数:"+messageCall.getMoveCount());
                        }
                    } else {
                        tv_area.setText("检测到您未处在工作区");
                        tv_projectName.setText("项目:---");
                        tv_position.setText("位置:---");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(getApplicationContext(),"网络请求失败",Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void noMessageCallback(NoDataMessage message){
        tv_area.setText("检测到您未处在工作区");
        tv_projectName.setText("项目:---");
        tv_position.setText("位置:---");
    }



    @Override
    protected void onPause() {
        super.onPause();
        isEnable = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
