package com.mjm2.beacondev.test;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.mjm2.beacondev.Constants;
import com.mjm2.beacondev.NoDataMessage;
import com.mjm2.beacondev.OkHttpUtils;
import com.mjm2.beacondev.R;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Administrator on 2017/10/20 0020.
 */

public class MyService extends Service implements RangeNotifier, BeaconConsumer {

    private static final long DEFAULT_BACKGROUND_SCAN_PERIOD = 1000L;
    private static final long DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD = 1000L;
    private BeaconManager beaconManager;
    //    public static final String beaconUUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825";
    public static final String beaconUUID = "E2C56DB5-DFFB-48D2-B060-D0F5A71096E0";
    /** 重新调整格式*/
    public static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    /** 设置兴趣UUID*/
    public static final String FILTER_UUID = "FDA50693-A4E2-4FB1-AFCF-C6EB07647826";
    private int myCount = 0;
    private String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        initBeacon();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));
        beaconManager.bind(this);
    }

    private void initBeacon() {
        beaconManager.setBackgroundScanPeriod(DEFAULT_BACKGROUND_SCAN_PERIOD);
        beaconManager.setBackgroundBetweenScanPeriod(DEFAULT_BACKGROUND_BETWEEN_SCAN_PERIOD);
    }

    @SuppressWarnings("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        minMac = "";
        startForeground(1, new Notification());

        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (beaconManager != null)
            beaconManager.removeRangeNotifier(this);
    }


    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> collections, Region region) {
        List<Beacon> beacons = new ArrayList<>();
        for (Beacon beacon : collections) {
            beacons.add(beacon);
        }
    }

    private int count = 0;
    private String minMac = "";
    private int minute = 0;
    private List<Beacon> newlist = new ArrayList<>();
    private int c = 0;

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> collections, Region region) {
                count ++;
//                c ++;
//                CallbackCount callbackCount = new CallbackCount();
//                callbackCount.setCount(c);
//                EventBus.getDefault().post(callbackCount);
                List<Beacon> beacons = new ArrayList<>();
                for (Beacon beacon : collections) {
                    if (beacon.getId1().toString().equalsIgnoreCase(FILTER_UUID)){
                        beacons.add(beacon);
                        Log.e("TAG2",beacon.getId1().toString()+"\n"
                                +beacon.getId2().toString()+"\n"
                                +beacon.getId3().toString()+"\n"
                                +beacon.getDistance()+"\n"
                                +beacon.getRssi()+"\n"
                                +beacon.getBluetoothAddress());
                    }
                }
                if (beacons.size() == 0){

                } else {
                    count = 0;
                }
                if (count > 20){
                    minMac = "";
                    myCount ++;
                    EventBus.getDefault().post(new NoDataMessage());
                    Map<String,String> map = new HashMap<>();
                    map.put("flag","person_position");
                    map.put("userid",getSharedPreferences("config",MODE_PRIVATE).getString("userid",""));
                    map.put("projectid","13");
                    map.put("state","0");
                    map.put("position","00000");
                    //上传服务器
                    SimpleDateFormat dateFormatter = new SimpleDateFormat(DEFAULT_TIME_FORMAT);
                    String date = dateFormatter.format(new Date(System.currentTimeMillis()));
//                    Toast.makeText(getApplicationContext(),"设备离开上传"+date,Toast.LENGTH_SHORT).show();
                    OkHttpUtils.requestServer(map, Constants.BEACON_INFO,0,myCount);
                    return;
                }
                if (beacons.size() > 0){
                    minute ++;
                    if (newlist.size() == 0){
                        newlist.addAll(beacons);
                    } else {
                        for (Beacon b : beacons) {
                            String addr = b.getBluetoothAddress();
                            Iterator<Beacon> iterator = newlist.iterator();
                            while (iterator.hasNext()){
                                if (iterator.next().getBluetoothAddress().equals(addr)){
                                    iterator.remove();
                                }
                            }
                            newlist.add(b);

                        }
                    }
                }

                Log.e("TAG","newlist.size()="+newlist.size());

                if (minute > 1){
                    minute = 0;
                    refrshState(newlist);
                    newlist.clear();
                }
                Log.e("TAG",beacons.size()+"__\n"+beacons.toString()
                        +"\n");


            }
        });
        try {
            beaconManager.startRangingBeaconsInRegion(new Region(FILTER_UUID, null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private void refrshState(List<Beacon> beacons) {
        if (newlist.size() == 0){
            return;
        }
        double minDis = newlist.get(0).getDistance();
        int index = 0;
        for (int i = 0; i < newlist.size(); i++) {
            if (minDis > newlist.get(i).getDistance()){
                minDis = newlist.get(i).getDistance();
                index = i;
            }
        }
        if (index != -1 && minDis > 0){
            Beacon minBeacon = newlist.get(index);
            /*if (!minBeacon.getBluetoothAddress().equalsIgnoreCase(minMac))*/
            if (true){
                minMac = minBeacon.getBluetoothAddress();
                myCount ++;
                Map<String,String> map = new HashMap<>();
                map.put("flag","person_position");
                map.put("userid",getSharedPreferences("config",MODE_PRIVATE).getString("userid",""));
                String str = minBeacon.getId2().toString();
                if (str.length() >= 2){
                    str = str.substring(str.length() - 2);
                    map.put("projectid",str);
                }
                map.put("state","1");
                map.put("position",minBeacon.getId3().toString());
                //上传服务器
                Log.e("TAG","上传服务器");
                SimpleDateFormat dateFormatter = new SimpleDateFormat(DEFAULT_TIME_FORMAT);
                String date = dateFormatter.format(new Date(System.currentTimeMillis()));
//                Toast.makeText(getApplicationContext(),"发现设备上传"+date,Toast.LENGTH_SHORT).show();
                OkHttpUtils.requestServer(map, Constants.BEACON_INFO,1,myCount);
            }
        }

    }
}
