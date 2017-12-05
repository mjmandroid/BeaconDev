package com.mjm2.beacondev;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2017/10/18 0018.
 */

public class IbeaconService extends Service {
    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    BluetoothManager bluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    private List<BeconBean> datas = new ArrayList<>();
//    public static final String beaconUUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825";
//    public static final String beaconUUID = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0";
    public static final String beaconUUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647826";
    private Timer timer = new Timer();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher));
        builder.setContentText("beacon");
        Notification n = builder.build();
        startForeground(1, n);
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothManager = (BluetoothManager) getApplication().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        registerReceiver(mReceiver, makeFilter());
        timer.schedule(task,10000,3000);
    }

    private TimerTask task = new TimerTask() {
        @Override
        public void run() {
            if (datas.size() > 0){
                double m = 0;
                for (BeconBean data : datas) {
                    if (m <  Double.parseDouble(data.getDistance())){
                        m = Double.parseDouble(data.getDistance());
                    }
                }
                if (m > minDistance){
                    minDistance = 0;
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("tag", "ibeacon service startLeScan");
        minDistance = 0;
        datas.clear();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher));
        builder.setContentText("beacon");
        Notification n = builder.build();
        startForeground(1, n);


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("tag", "stopScanningForBeacons");
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        timer.cancel();
        unregisterReceiver(mReceiver);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {
            int startByte = 2;
            boolean patternFound = false;
            // 寻找ibeacon
            while (startByte <= 5) {
                if (((int) scanRecord[startByte + 2] & 0xff) == 0x02 && // Identifies an iBeacon
                        ((int) scanRecord[startByte + 3] & 0xff) == 0x15) { // Identifies  correct  data  length
                    patternFound = true;
                    break;
                }
                startByte++;
            }
            // 如果找到了的话
            if (patternFound) {
                // 转换为16进制
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
                String hexString = bytesToHex(uuidBytes);

                // ibeacon的UUID值
                String uuid = hexString.substring(0, 8) + "-"
                        + hexString.substring(8, 12) + "-"
                        + hexString.substring(12, 16) + "-"
                        + hexString.substring(16, 20) + "-"
                        + hexString.substring(20, 32);

                // ibeacon的Major值
                int major = (scanRecord[startByte + 20] & 0xff) * 0x100
                        + (scanRecord[startByte + 21] & 0xff);

                // ibeacon的Minor值
                int minor = (scanRecord[startByte + 22] & 0xff) * 0x100
                        + (scanRecord[startByte + 23] & 0xff);

                String ibeaconName = device.getName();
                String mac = device.getAddress();
                int txPower = (scanRecord[startByte + 24]);
                Log.d("BLE", bytesToHex(scanRecord));
                Log.d("BLE", "Name：" + ibeaconName + "\nMac：" + mac
                        + " \nUUID：" + uuid + "\nMajor：" + major + "\nMinor："
                        + minor + "\nTxPower：" + txPower + "\nrssi：" + rssi);

                Log.d("BLE", "distance：" + calculateAccuracy(txPower, rssi));
                DecimalFormat df = new DecimalFormat("#.00");
                String distance = df.format(calculateAccuracy(txPower, rssi));//将距离保留两位小数
//                Toast.makeText(getApplicationContext(),uuid,Toast.LENGTH_SHORT).show();
                if (beaconUUID.equalsIgnoreCase(uuid)){
                    BeconBean bean = new BeconBean();
                    bean.setUuid(uuid);
                    bean.setMajor(major);
                    bean.setMinor(minor);
                    bean.setDistance(distance);
                    bean.setAddress(device.getAddress());
                    bean.setDevice(device);
                    addDevice(bean);
                }

            }
        }
    };
    private int prevCount;
    private int current;
    private double minDistance = 0;

    private void addDevice(BeconBean beconBean) {
        boolean isExits = false;
        for (BeconBean data : datas) {
            if (data.getAddress().equals(beconBean.getAddress())){
                isExits = true;
                data.setDistance(beconBean.getDistance());
                data.setMinor(beconBean.getMinor());
                data.setMajor(beconBean.getMajor());
                break;
            }
        }
        if (!isExits){
            datas.add(beconBean);

        }
        current = datas.size();
        if (current == prevCount && current > 0){
//            Toast.makeText(getApplicationContext(),"===",Toast.LENGTH_SHORT).show();
            int min = -1;
            for (int i = 0; i < datas.size(); i++) {
                if (minDistance < Double.parseDouble(datas.get(i).getDistance())){
                    double m = Double.parseDouble(datas.get(i).getDistance());
                    minDistance = m;
                    min = i;
                }
            }
            if (min != -1){
//                Toast.makeText(getApplicationContext(),"上传了",Toast.LENGTH_SHORT).show();
                BeconBean mBeacon  = datas.get(min);
                Log.d("address==",mBeacon.getAddress());
                Map<String,String> map = new HashMap<>();
                map.put("flag","person_position");
                map.put("userid",getSharedPreferences("config",MODE_PRIVATE).getString("userid",""));
                String str = mBeacon.getMajor()+"";
                if (str.length() >= 2){
                    str = str.substring(str.length() - 2);
                    map.put("projectid",str);
                }
                map.put("state","1");
                map.put("position",mBeacon.getMinor()+"");
                mac = mBeacon.getAddress();
                //上传服务器
//                OkHttpUtils.requestServer(map,Constants.BEACON_INFO);
                mBeacon.getDevice().connectGatt(getApplicationContext(), false, new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        Toast.makeText(getApplicationContext(),"super.onConnectionStateChange",Toast.LENGTH_SHORT).show();
                        if (status == BluetoothProfile.STATE_DISCONNECTED){
                            Toast.makeText(getApplicationContext(),"断开连接...",Toast.LENGTH_SHORT).show();
                            Log.e("state:","super.onConnectionStateChange(gatt, status, newState);");
                            int dd = -1;
                            for (int i = 0; i < datas.size(); i++) {
                                if(mac.equalsIgnoreCase(datas.get(i).getAddress())){
                                    dd = i;
                                    break;
                                }
                            }
                            if (dd != -1){
                                BeconBean remove = datas.remove(dd);
                                Map<String,String> map = new HashMap<>();
                                map.put("flag","person_position");
                                map.put("userid",getSharedPreferences("config",MODE_PRIVATE).getString("userid",""));
                                String str = remove.getMajor()+"";
                                if (str.length() >= 2){
                                    str = str.substring(str.length() - 2);
                                }
                                map.put("projectid","11");
                                map.put("state","0");
                                map.put("position","11111");
                                //上传服务器
//                                OkHttpUtils.requestServer(map,Constants.BEACON_INFO);
                            }
                        }

                    }
                });
            }

        }
        prevCount = datas.size();
        Log.d("size",datas.size()+"");
    }


    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    protected static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return accuracy;
        }
    }
    private IntentFilter makeFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return filter;
    }
    private String mac = "";
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()){

                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch(blueState){
                        case BluetoothAdapter.STATE_OFF:
                                int dd = -1;
                                for (int i = 0; i < datas.size(); i++) {
                                    if(mac.equalsIgnoreCase(datas.get(i).getAddress())){
                                        dd = i;
                                        break;
                                    }
                                }
                                if (dd != -1){
                                    BeconBean remove = datas.remove(dd);
                                    Map<String,String> map = new HashMap<>();
                                    map.put("flag","person_position");
                                    map.put("userid",getSharedPreferences("config",MODE_PRIVATE).getString("userid",""));
                                    String str = remove.getMajor()+"";
                                    if (str.length() >= 2){
                                        str = str.substring(str.length() - 2);
                                    }
                                    map.put("projectid","11");
                                    map.put("state","0");
                                    map.put("position","11111");
                                    //上传服务器
//                                    OkHttpUtils.requestServer(map,Constants.BEACON_INFO);
                                }

                            break;
                    }
                    break;
            }
        }
    };
    class DisconnectReceive extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String addr = intent.getStringExtra("address");
            int index = -1;
            for (int i = 0; i < datas.size(); i++) {
                if (addr.equals(datas.get(i).getAddress())){
                    index = i;
                    break;
                }
            }
            if (index != -1){
                BeconBean data = datas.remove(index);
                Map<String,String> map = new HashMap<>();
                map.put("flag","person_position");
                map.put("userid",getSharedPreferences("config",MODE_PRIVATE).getString("userid",""));
                String str = data.getMajor()+"";
                if (str.length() >= 2){
                    str = str.substring(str.length() - 2);
                }
                map.put("projectid",str);
                map.put("state","0");
                map.put("position",data.getMinor()+"");
                //上传服务器
//                OkHttpUtils.requestServer(map,Constants.BEACON_INFO);
                System.out.println("蓝牙断开回调。。。");
            }
        }
    }
}
