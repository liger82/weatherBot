package com.lgcns.weatherbot;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;

public class GpsInfo extends Service implements LocationListener {

    private final Context mContext;

    //현재 GPS 사용유무
    boolean isGPSEnabled = false;

    //네트워크 사용유무
    boolean isNetworkEnabled = false;

    //GPS state value
    boolean isGetLocation = false;

    Location location;
    double lat; //위도 latitude
    double lon; //경도 longitude

    protected LocationManager locationManager;

    // 최소 GPS 정보 업데이트 거리 10미터
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;

    // 최소 GPS 정보 업데이트 시간 밀리세컨이므로 1분
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;

    public GpsInfo(Context context){
        this.mContext = context;
        getLocation();
    }

    public Location getLocation(){
        if(Build.VERSION.SDK_INT >= 28 &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED
           )
            return null;

        try{
            locationManager = (LocationManager)mContext.
                    getSystemService(Context.LOCATION_SERVICE);

            //GPS 사용유무 정보
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            //현재 네트워크 상태값 알아오기
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if(!isGPSEnabled && !isNetworkEnabled) {
                //GPS, network 사용 불가시
            }else{
                //둘다 되거나 하나씩 안될 경우

                isGetLocation = true;
                //네트워크 정보로부터 위치값 가져오기
                //네트워크 사용 가능할 때
                if(isNetworkEnabled){
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,this);

                    if(locationManager!=null){
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if(location != null){
                            //위도 경도
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                        }
                    }
                }

                //GPS가 사용 가능할 때
                if(isGPSEnabled){
                    if(location!=null){
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES,this);
                        if(locationManager!=null){
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if(location!=null){
                                //위도 경도 받기
                                lat = location.getLatitude();
                                lon = location.getLongitude();
                            }
                        }
                    }
                }

            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return location;
    }

    //GPS 종료
    public void stopUsingGPS(){
        if(locationManager !=null){
            locationManager.removeUpdates(GpsInfo.this);
        }
    }

    //위도 값을 가져온다.
    public double getLatitude(){
        if(location !=null){
            lat = location.getLatitude();
        }
        return lat;
    }

    //경도 값을 가져온다.
    public double getLongitude(){
        if(location !=null){
            lon = location.getLongitude();
        }
        return lon;
    }

    //GPS나 wifi 정보가 켜져있는지 확인합니다.
    public boolean isGetLocation(){
        return this.isGetLocation;
    }

    /*
    * GPS 정보를 가져오지 못했을때
    * 설정값으로 갈지 물어보는 alert*/
    public void showSettingAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        alertDialog.setTitle("GPS 사용 유무 셋팅");
        alertDialog.setMessage("GPS가 활성화되어 있지 않습니다. \n 설정창으로 가시겠습니까?");

        //OK를 누르면 설정창으로 이동
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        //cancel 하면 종료합니다.
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.cancel();

                    }
                });
        alertDialog.show();
    }

    @Override
    public IBinder onBind(Intent arg0){
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
