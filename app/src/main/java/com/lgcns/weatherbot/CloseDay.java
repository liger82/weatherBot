package com.lgcns.weatherbot;

import android.util.Log;

/*가까운 시일(당일, 내일, 모레)에 대한 일기예보
* 상세한 내용을 담고 있다.*/
public class CloseDay {
    private int hour; //3시간마다 업데이트
    private int day; //오늘 내일 모레 중 하나 0,1,2
    private double temp;  // 온도
    private String wfKor; // 날씨 상태 한국어로
    private int pop; // 강수확률 %
    private int reh; // 습도 %
    private double tmx; //최고온도
    private double tmn; //최저온도
    private double ws; // 풍속

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public double getTemp() {
        return temp;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public String getWfKor() {
        return wfKor;
    }

    public void setWfKor(String wfKor) {
        this.wfKor = wfKor;
    }

    public int getPop() {
        return pop;
    }

    public void setPop(int pop) {
        this.pop = pop;
    }

    public int getReh() {
        return reh;
    }

    public void setReh(int reh) {
        this.reh = reh;
    }

    public double getTmx() {
        return tmx;
    }

    public void setTmx(double tmx) {
        this.tmx = tmx;
    }

    public double getTmn() {
        return tmn;
    }

    public void setTmn(double tmn) {
        this.tmn = tmn;
    }

    public double getWs() {
        return ws;
    }

    public void setWs(double ws) {
        this.ws = ws;
    }

    public String makeSpeechScriptForCurrentInfo(){
        String script= "";
        script += "기온은 "+temp;
        if(getWfKor().equals("맑음")) {
            script += "도이며 맑습니다.";
        }else if(getWfKor().equals("구름 조금")) {
            script += "도이며 구름이 조금 있습니다.";
        }else if(getWfKor().equals("눈")) {
            script += "도이며 눈이 오고 있으니 우산을 챙겨주시기 바랍니다";
        }else if(getWfKor().equals("구름 많음")) {
            script += "도이며 구름이 많이 끼었습니다.";
        }else if(getWfKor().equals("흐림")) {
            script += "도이며 하늘이 흐립니다.";
        }else if(getWfKor().equals("비")) {
            script += "도이며 비가 오고 있으니 우산을 챙겨주시기 바랍니다";
        }else if(getWfKor().equals("눈/비")) {
            script += "도이며 눈비가 오고 있으니 우산을 챙겨주시기 바랍니다.";
        }

        if(getTmx()!=-999) script += String.format(" 최고기온은  %.1f도",getTmx());

        if(getTmn()!=-999) script += String.format(" 최저기온은  %.1f도",getTmn());

        script += String.format(" 강수확률 %d퍼센트,습도 %d퍼센트, 풍속 %.1f미터퍼 세컨드입니다. ",
                getPop(),getReh(),getWs()
        );

        Log.d("====CLOSE DAY[0] ","=======script : "+script);

        return script;
    }

    public String makeSpeechScriptForCloseFutureInfo(){
        String script= "";

        if(getTmx()!=-999) script += String.format("최고기온 %.1f도 ",getTmx());

        if(getTmn()!=-999) script += String.format("최저기온 %.1f도이며 ",getTmn());

        if(getWfKor().equals("맑음")) {
            script += "맑을 예정입니다.";
        }else if(getWfKor().equals("구름 조금")) {
            script += "구름이 조금 있을 예정입니다.";
        }else if(getWfKor().equals("눈")) {
            script += "눈이 올 예정이오니 우산을 챙겨주시기 바랍니다.";
        }else if(getWfKor().equals("구름 많음")) {
            script += "구름이 많이 낄 예정입니다.";
        }else if(getWfKor().equals("흐림")) {
            script += "하늘이 흐릴 예정입니다.";
        }else if(getWfKor().equals("비")) {
            script += "비가 올 예정이오니 우산을 챙겨주시기 바랍니다.";
        }else if(getWfKor().equals("눈/비")) {
            script += "눈비가 올 예정이오니 우산을 챙겨주시기 바랍니다.";
        }


        script += String.format(" 강수확률 %d퍼센트,습도 %d퍼센트, 풍속 %.1f미터퍼 세컨드로 예상됩니다. ",
                getPop(),getReh(),getWs()
        );

        Log.d("====CLOSE DAY[1] ","=======script : "+script);

        return script;
    }

    //날씨 정보 시각적으로 보여주는 문서 문자열 만들기
    public String makeWeatherDocs(boolean isToday){
        String result="";

        //오늘이면
        if(isToday){
            result = String.format(
                    "\n기온 : %s *C\n",getTemp());
        }else{
            result = String.format(
                    "\n최고 기온 : %s *C\n" +
                            "최저 기온 : %s *C\n",getTmx(),getTmn());
        }


        result +=  String.format(
                "날씨 : %s\n" +
                        "강수확률 : %s%%\n" +
                        "습도 : %s%%\n" +
                        "풍속 : %.1sm/s\n",
                getWfKor(),
                getPop(),
                getReh(),
                getWs());

        return result;
    }

}
