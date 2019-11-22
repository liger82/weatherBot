package com.lgcns.weatherbot;


import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;


public class CloseDayActivity {

    private String url;
    private ArrayList<CloseDay> closeinfos = new ArrayList<CloseDay>();
    private final String TAG = CloseDayActivity.class.getSimpleName();

    public CloseDayActivity(String url){
        this.url = url;
        //getXmlParser("utf-8");
        new ReceiveCloseWeather().execute();
    }

    public ArrayList<CloseDay> getList(){
        return closeinfos;
    }



    public class ReceiveCloseWeather extends AsyncTask<URL, Integer, Long> {


        @Override
        protected Long doInBackground(URL... urls) {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = null;
            Log.d(TAG,"======doInBackground ");

            try{
                Log.d(TAG,"======try 안");
                response = client.newCall(request).execute();
                parseXML(response.body().toString());
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                //response.body().close();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Long result) {


        }

        void parseXML(String xml) {
            try {
                String tagName = "";
                boolean onHour = false;
                boolean onDay = false;
                boolean onTem = false;
                boolean onWfKor = false;
                boolean onPop = false;
                boolean onEnd = false;
                boolean onWs = false;
                boolean onTmx = false;
                boolean onTmn = false;
                boolean onReh = false;
                boolean isItemTag1 = false;
                int i = 0;
                Log.d(TAG,"======parseXML 안");

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();

                parser.setInput(new StringReader(xml));

                int eventType = parser.getEventType();
                Log.d(TAG,"======eventType[1] : "+eventType);


                //END_DOCUMENT == 1
                while (eventType != XmlPullParser.END_DOCUMENT) {

                    Log.d(TAG,"======eventType[2] : "+eventType);

                    //START_TAG == 2
                    //END_TAG == 3
                    //TEXT == 4
                    //START_DOCUMENT == 0
                    //XmlPullParser.T
                    if (eventType == XmlPullParser.START_TAG) {
                        tagName = parser.getName();
                        Log.d(TAG,"=====getName : "+tagName);
                        if (tagName.equalsIgnoreCase("data")) {
                            closeinfos.add(new CloseDay());
                            onEnd = false;
                            isItemTag1 = true;
                        }
                    } else if (eventType == XmlPullParser.TEXT && isItemTag1) {
                        //data setting
                        if (tagName.equals("hour") && !onHour) {
                            closeinfos.get(i).setHour(Integer.parseInt(parser.getText()));
                            onHour = true;
                        }
                        if (tagName.equals("day") && !onDay) {
                            closeinfos.get(i).setDay(Integer.parseInt(parser.getText()));
                            onDay = true;
                        }
                        if (tagName.equals("temp") && !onTem) {
                            closeinfos.get(i).setTemp(Double.parseDouble(parser.getText()));
                            onTem = true;
                        }
                        if (tagName.equals("wfKor") && !onWfKor) {
                            closeinfos.get(i).setWfKor(parser.getText());
                            onWfKor = true;
                        }
                        if (tagName.equals("pop") && !onPop) {
                            closeinfos.get(i).setPop(Integer.parseInt(parser.getText()));
                            onPop = true;
                        }
                        if (tagName.equals("reh") && !onReh) {
                            closeinfos.get(i).setReh(Integer.parseInt(parser.getText()));
                            onReh = true;
                        }
                        if (tagName.equals("ws") && !onWs) {
                            closeinfos.get(i).setWs(Double.parseDouble(parser.getText()));
                            onWs = true;
                        }
                        if (tagName.equals("tmx") && !onTmx) {
                            closeinfos.get(i).setTmx(Double.parseDouble(parser.getText()));
                            onTmx = true;
                        }
                        if (tagName.equals("tmn") && !onTmn) {
                            closeinfos.get(i).setTmn(Double.parseDouble(parser.getText()));
                            onTmn = true;
                        }

                    } else if (eventType == XmlPullParser.END_TAG) {
                        if (tagName.equals("s06") && onEnd == false) {
                            i++;
                            onHour = false;
                            onDay = false;
                            onTem = false;
                            onWfKor = false;
                            onPop = false;
                            onWs = false;
                            onTmx = false;
                            onTmn = false;
                            onReh = false;

                            isItemTag1 = false;
                            onEnd = true;
                        }
                    }

                    eventType  = parser.next();
                    Log.d(TAG,"======eventType[3] : "+eventType);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally {

            }
        }
    }

}
