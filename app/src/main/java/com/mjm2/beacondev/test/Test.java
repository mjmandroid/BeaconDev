package com.mjm2.beacondev.test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2017/10/20 0020.
 */

public class Test {
    private static String DEFAULT_TIME_FORMAT = "HH:mm:ss";
    public static void main(String[] args) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(DEFAULT_TIME_FORMAT);
        String date = dateFormatter.format(new Date(System.currentTimeMillis()));
        System.out.println(date);
    }


}
