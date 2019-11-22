package com.lgcns.weatherbot;

import android.util.Log;

import java.util.Date;


public class VariousUtils {
    private Date today= new Date();;
    private static String[] clarr = new String[]{"오늘","내일","모레","현재","지금"};
/*
    public VariousUtils(){
        today = new Date();
    }
*/


    //가까운 시일(3일) 내의 날씨 정보를 원하는지
    public static boolean isCloseDay(String indir1, String indir2){
        for(int i=0;i<clarr.length;i++){
            if(indir1.equals(clarr[i]) || indir2.equals(clarr[i])){
                return true;
            }
        }
        return false;
    }



}
