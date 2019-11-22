package com.lgcns.weatherbot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import com.google.api.services.language.v1beta2.CloudNaturalLanguage;
import com.google.api.services.language.v1beta2.CloudNaturalLanguageRequestInitializer;
import com.google.api.services.language.v1beta2.model.AnalyzeSyntaxRequest;
import com.google.api.services.language.v1beta2.model.AnalyzeSyntaxResponse;
import com.google.api.services.language.v1beta2.model.AnnotateTextRequest;
import com.google.api.services.language.v1beta2.model.AnnotateTextResponse;
import com.google.api.services.language.v1beta2.model.Document;
import com.google.api.services.language.v1beta2.model.Entity;
import com.google.api.services.language.v1beta2.model.Features;
import com.google.api.services.language.v1beta2.model.Token;


import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener, TextToSpeech.OnInitListener{

    private final String apiKey = "AIzaSyB9Gt1nEIQcx6-fkyoDikbu9214955q9H8";
    private final String TAG = MainActivity.class.getSimpleName();

    TextView textViewSpeechResult;
    TextView textViewSpeechStatus;
    TextView textViewTitle;
    ImageView weatherState;
    private SpeechRecognizer speechRecognizer;
    private Intent intentSpeech;

    //날씨 관련 기준 정보들 미리 리스트나 맵에 담기
    public static List<String> directClue = new ArrayList<>();
    public static List<String> indirectClue = new ArrayList<>();
    public static List<String> localClue = new ArrayList<>();
    public static Map<String,String> localClue2 = new HashMap<String, String>();

    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private final int PERMISSIONS_ACCESS_COARSE_LOCATION = 1001;
    private final int PERMISSIONS_RECORD_AUDIO = 1002;
    private boolean isAccessFineLocation = false;
    private boolean isAccessCoarseLocation = false;
    private boolean isRecordAudio = false;
    private boolean isPermission = false;

    private GpsInfo gps;

    private TextToSpeech tts;

    public static List<CloseDay> clList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        while(!isPermission){
            callPermission();
            if(isPermission)
                break;
        }

        textViewSpeechStatus = findViewById(R.id.textView_speech_status);
        textViewSpeechResult = findViewById(R.id.textView_speech_result);
        textViewTitle = findViewById(R.id.title_wt);
        weatherState = findViewById(R.id.weatherState);

        //TTS
        tts = new TextToSpeech(this, this);


        init();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_ACCESS_FINE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            isAccessFineLocation = true;

        }

/*        else if (requestCode == PERMISSIONS_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            isAccessCoarseLocation = true;

        }*/

        if(requestCode == PERMISSIONS_RECORD_AUDIO
                && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            isRecordAudio = true;

        if (isAccessFineLocation /*&& isAccessCoarseLocation*/ && isRecordAudio) {
            isPermission = true;
        }
    }

    //permission 요청
    //권한 없으면 요청
    private void callPermission() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_ACCESS_FINE_LOCATION);

        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED){

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_RECORD_AUDIO);
        }

        else {
            isPermission = true;
        }
    }

    public void init(){
        //STT를 위한 intent
        intentSpeech = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intentSpeech.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        intentSpeech.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intentSpeech.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN); //언어설정
        //SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        //Listener 설정
        speechRecognizer.setRecognitionListener(recognitionListener);

        //답변 기준 로딩
        try{

            //주변 단서 목록
            InputStream is = getResources().openRawResource(R.raw.clue);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line="";
            int i = 0;
            while((line = br.readLine())!=null){
                //첫번째줄은 앞에 자를 것이 있음.
                if(i==0)
                    line = line.substring(1);
                i++;
                indirectClue.add(line);
            }
            i=0;
            //직접 단서 목록
            is= getResources().openRawResource(R.raw.direct);
            br = new BufferedReader(new InputStreamReader(is));
            while((line = br.readLine())!=null){
                if(i==0)
                    line = line.substring(1);
                i++;
                directClue.add(line);

            }
            //지역 단서 목록
            is= getResources().openRawResource(R.raw.local);
            br = new BufferedReader(new InputStreamReader(is));
            i=0;
            while((line = br.readLine())!=null){
                if(i==0)
                    line = line.substring(1);
                i++;
                localClue.add(line);
            }

            //지역 단서 목록2
            is = getResources().openRawResource(R.raw.localcode);
            br = new BufferedReader(new InputStreamReader(is));
            i = 0;
            StringTokenizer st = null;
            String local = "";
            String lCode ="";
            while((line = br.readLine())!=null){
                if(i==0){
                    line = line.substring(1);
                }
                i++;
                st = new StringTokenizer(line);
                lCode = st.nextToken(); //지역 코드
                local = st.nextToken(); //지역명
                localClue2.put(local,lCode);

            }


            br.close();
            is.close();
        }catch (IOException e){
            e.printStackTrace();
        }
/*
        Log.d(TAG,"=======directClue : "+directClue.toString());
        Log.d(TAG,"=======indirectClue : "+indirectClue.toString());
        Log.d(TAG,"=======localClue: "+localClue.toString());
*/


        //버튼 클릭 이벤트 처리
        findViewById(R.id.button_speech).setOnClickListener(this);
    }

    public void startCustomSTT() {
        if(speechRecognizer != null && intentSpeech != null) {
            Log.d(TAG,"=============startSTT");
            speechRecognizer.startListening(intentSpeech);
        }
    }

    public void stopCustomSTT() {
        if(speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.button_speech){
            if(!isPermission){
                callPermission();
                return;
            }else{
                //안보이게 함.
                textViewTitle.setVisibility(View.INVISIBLE);
                weatherState.setVisibility(View.INVISIBLE);
                startCustomSTT();
            }
        }
    }

    @Override
    protected void onPause() {
        stopCustomSTT();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCustomSTT();
        tts.shutdown();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        textViewSpeechStatus.setText(R.string.tap_on_the_mic);
    }

    private RecognitionListener recognitionListener = new RecognitionListener() {
        //startListening() 호출 후 음성이 입력되기 전 상태
        @Override
        public void onReadyForSpeech(Bundle params) {
            //Log.d(TAG, "onReadyForSpeech");
            textViewSpeechStatus.setText("말씀하세요");
            textViewSpeechResult.setText("");
        }

        //음성이 입력되고 있는 상태
        @Override
        public void onBeginningOfSpeech() {
//            Log.d(TAG, "onBeginningOfSpeech");
            textViewSpeechStatus.setText("음성 입력 중");
        }

        //사운드 레벨이 변경된 상태
        @Override
        public void onRmsChanged(float rmsdB) {
//            Log.d(TAG, "onRmsChanged");
            //textViewSpeechStatus.setText("onRmsChanged");
        }

        //많은 소리가 수신된 상태
        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived");
            //textViewSpeechStatus.setText("onBufferReceived");
        }

        //음성인식을 마친 상태
        @Override
        public void onEndOfSpeech() {
//            Log.d(TAG, "onEndOfSpeech");
            stopCustomSTT();
            textViewSpeechStatus.setText("음성 입력 종료");
        }

        //네트워크 혹은 인식 오류가 발생한 상태
        @Override
        public void onError(int error) {
            Log.d(TAG, "error :"+error);
            textViewSpeechStatus.setText("다시 시도해주세요");
        }

        //음성인식을 마치고 결과가 나온 상태
        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "onResults");
            String text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
            textViewSpeechResult.setText(text);
            analyzeText(text);
        }

        //부분적으로 음성인식을 마치고 결과가 나온 상태
        @Override
        public void onPartialResults(Bundle partialResults) {
//            Log.d(TAG, "onPartialResults");
//            textViewSpeechStatus.setText("onPartialResults");
        }

        //향후 이벤트를 추가하기 위해 예약된 상태
        @Override
        public void onEvent(int eventType, Bundle params) {
//            Log.d(TAG, "onEvent");
//            textViewSpeechStatus.setText("onEvent");
        }
    };

    /*STT 이후 텍스트를 분석하는 작업은 실제 디바이스 그 결과가 나올 필요가 없다.
     * 분석을 명령하는 버튼도 필요가 없다. 바로 분석을 하면 된다.*/

    /* NLP하기 위해서는 CloudNaturalLanguage object를 생성해서
     * CloudNaturalLanguage.Builer class를 사용한다.
     * 이것의 constructor는 Http transport와 JSON factory를 인자로 받는다.
     * Initializer에 cloud api key를 넣는다.
     * */

    final CloudNaturalLanguage naturalLanguageService = new CloudNaturalLanguage.Builder(
            AndroidHttp.newCompatibleTransport(),
            new AndroidJsonFactory(),
            null
    ).setCloudNaturalLanguageRequestInitializer(
            new CloudNaturalLanguageRequestInitializer(apiKey)
    ).build();

    //텍스트 분석하고 다이얼로그로 출력
    public void analyzeText(String transcript){
        Document document = new Document();
        document.setType("PLAIN_TEXT");
        document.setLanguage("ko-KR");
        document.setContent(transcript);

        Features features = new Features();
        features.setExtractEntities(true); //엔터티 추출
        features.setExtractDocumentSentiment(true); // 감정 추출

        //엔터티, 감정 분석용 - request에 정보를 담는다.
        //final로 선언하는 이유는 inner class 안에서 사용하기 위함
        final AnnotateTextRequest request = new AnnotateTextRequest();
        request.setDocument(document);
        request.setFeatures(features);

        //구문분석용
        final AnalyzeSyntaxRequest asRequest = new AnalyzeSyntaxRequest();
        asRequest.setDocument(document);


        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    AnnotateTextResponse response = naturalLanguageService.documents()
                            .annotateText(request).execute();
                    //엔터티를 담은 리스트
                    final List<Entity> entityList = response.getEntities();
                    final float sentiment = response.getDocumentSentiment().getScore();

                    AnalyzeSyntaxResponse response2 =  naturalLanguageService.documents().analyzeSyntax(asRequest).execute();
                    //토큰을 담은 리스트
                    final List<Token> tokenList = response2.getTokens();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String entities = "";
                            for(Entity entity:entityList) {
                                //                  엔터티 이름
                                entities += "\n" + entity.getName();
                            }
                            String tokens = "";
                            List<String> vnList = new ArrayList<String>();
                            for(Token token:tokenList){
                                                        //품사                              토큰 내용
                                tokens += "\n("+token.getPartOfSpeech().getTag()+") "+token.getText().getContent();
                                //동사나 명사일때 넣는다.
                                if(token.getPartOfSpeech().getTag().equals("VERB") || token.getPartOfSpeech().getTag().equals("NOUN"))
                                    vnList.add(token.getText().getContent());
                            }

                            //팝업창 다이얼로그
                            //분석이 어떻게 되고 있는지 보고 싶을때 사용
                            /*AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Sentiment: " + sentiment)
                                    .setMessage("Entities : " + entities
                                    +"\nTokens : "+tokens)
                                    .setNeutralButton("Close", null)
                                    .create();
                            dialog.show();*/
                            textViewSpeechStatus.setText("tap on the mic");

                            //답장 도출 메서드 호출
                            replyOnlyWeather(vnList);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }



    //답장 도출 메서드
    public void replyOnlyWeather(List<String> vnList){
        //날씨 직접 단서를 담은 변수
        String dir = "";
        //날씨 주변 단서를 담은 변수 2개
        String indir1 = "",indir2 = "";
        String location="";
        Log.d(TAG,"=======vnList : "+vnList.toString());
        //단서 담기

        for(int i=0;i<vnList.size();i++){
            //지역 단어가 나오면 삽입, 없으면 현재 위치 사용

            //오늘 실시간 날씨
            if(directClue.contains(vnList.get(i))  ){
                dir = vnList.get(i);
            }else if(indirectClue.contains(vnList.get(i)) && indir1.equals("")){
                indir1 = vnList.get(i);
            }else if(indirectClue.contains(vnList.get(i)) && !indir1.equals("")) {
                indir2 = vnList.get(i);
            }else if(localClue2.containsKey(vnList.get(i))){
                location = vnList.get(i);
            }else if(vnList.get(i).equals("이름")){
                indir2 = vnList.get(i);
            }

        }
        //너 이름이 모야?에 대한 답변만 개별적으로 추가
        if(indir2.equals("이름")){
            speak("안녕하세요 제 이름은 웨더봇입니다.");
            return;
        }
        Log.d(TAG, "=======dir[1] : "+dir);
        Log.d(TAG, "=======indir1[1] : "+indir1);
        Log.d(TAG, "=======indir2[1] : "+indir2);
        Log.d(TAG, "=======location[1] : "+location);

        String targetUrl="";

        gps = new GpsInfo(MainActivity.this);
        String loc = "";
        if(location==null || location.equals("")){
            if(gps.isGetLocation()){
                double lat = gps.getLatitude();
                double lon = gps.getLongitude();

                //위도 경도를 주소로 변환하는 작업
                loc = convertAddress(lat,lon);
                targetUrl = "http://www.kma.go.kr/wid/queryDFSRSS.jsp?zone="+localClue2.get(loc);

            }else{
                //gps 활성화 안했으면 설정창으로 가느냐는 알림창 뜨기
                gps.showSettingAlert();
                //명령 취소
                textViewSpeechResult.setText("다시 시도해주시기 바랍니다.");
                return;
            }
        }else{
            //음성으로 지역을 명시한 경우
            targetUrl = "http://www.kma.go.kr/wid/queryDFSRSS.jsp?zone="+localClue2.get(location);
        }


        //오늘을 말 안하고 "날씨"라고만 말해도 오늘 날씨로 인식해야함
        //오늘, 내일, 모레 중에 하나 인지.
        if((!dir.equals("") && indir1.equals("")) || VariousUtils.isCloseDay(indir1,indir2)){

            //서울 상세 날씨
            new RetrieveWeatherInfo().execute(targetUrl);

        }else{
            speak("현재 이 앱은 3일 이내의 날씨 정보만 제공하고 있습니다. 다음에 더 편리한 기능으로 찾아뵙겠습니다. 뿅!");
            return;
        }


        //날씨 정보 가져올때까지 기다린다.
        while(clList ==null){}
        Log.d(TAG,"clList : "+clList);

        String script="";
        String textResult="";
        String title="";

        //== 단서에 따른 분류 ==
        //************상세 시간별 날씨로 나옴**************
        //오늘 날씨
        if((!dir.equals("") && indir1.equals("")) || //"날씨" 라고만 답했을 경우
                (!dir.equals("") && (indir1.equals("오늘") || indir2.equals("오늘"))) ||
                (!dir.equals("") && (indir1.equals("현재") || indir2.equals("현재"))) ||
                (!dir.equals("") && (indir1.equals("지금") || indir2.equals("지금"))) ) { // "오늘 날씨"라고 했을 경우
            //요일로 요청했을 경우에 그 요일이 오늘일 경우 추가
            //정확한 일월을 말했을 경우에 그 날이 오늘일 경우 추가
            script+="오늘의 날씨를 말씀드리겠습니다. " ;
            script +="현재"+ clList.get(0).makeSpeechScriptForCurrentInfo();
            //textResult = String.format("< 현재 날씨 >\n기온 : %s *C\n날씨 : 맑음", clList.get(0).getTemp());

            title = "[ 오늘 날씨 ]";


            textResult = clList.get(0).makeWeatherDocs(true);

/*
            textResult += String.format(
                    "\n기온 : %s *C\n" +
                    "날씨 : %s\n" +
                    "강수확률 : %s%%\n" +
                    "습도 : %s%%\n" +
                    "풍속 : %.1sm/s\n",clList.get(0).getTemp(),
                                        clList.get(0).getWfKor(),
                                        clList.get(0).getPop(),
                                        clList.get(0).getReh(),
                                        clList.get(0).getWs());
*/


            insertImage(clList.get(0).getWfKor());

            Log.d(TAG,"=======script : "+script);

         //내일 날씨
        }else if(!dir.equals("") && (indir1.equals("내일") || indir2.equals("내일"))){ // "내일 날씨"라고 했을 경우
            script += "내일 날씨를 알려드리겠습니다.";
            //최저, 최고, 날씨 상태, 강수 확률,습도, 풍속
            /*확인 과정을 거치는 이유
            * 기상청 데이터에서 3일 이내 데이터는 3시간씩 업데이트된 정보가 올라온다.
            * 그 중에서 아침은 맑은데 저녁에 비가 오는 경우라면
            * 비가 온다고 하는 것이 예보로써의 성격을 유지하는 것이기 때문에
            * 비나 눈을 확인하는 과정을 거친다.
            * */
            int j=0;
            boolean flag = false;

            for(int i=0;i<clList.size();i++){
                // 내일이고 getDay ==1
                //날씨 상태가 비, 눈, 비/눈인 것을 확인한다.
                if(clList.get(i).getDay()==1){
                    j = i;
                    if((clList.get(i).getWfKor().equals("비") ||
                        clList.get(i).getWfKor().equals("눈") ||
                        clList.get(i).getWfKor().equals("비/눈"))){
                        flag = true;
                        script += clList.get(i).makeSpeechScriptForCloseFutureInfo();
                    }
                }
            }

            // 비나 눈이 안올때
            if(!flag)
                script += clList.get(j).makeSpeechScriptForCloseFutureInfo();

            title="[ 내일 날씨 ]";
            textResult = clList.get(j).makeWeatherDocs(false);
            insertImage(clList.get(j).getWfKor());

            //모레 날씨
        }else if(!dir.equals("") && (indir1.equals("모레") || indir2.equals("모레"))){ // "모레 날씨"라고 했을 경우
            script += "모레 날씨를 알려드리겠습니다.";

            int j=0;
            boolean flag = false;

            for(int i=0;i<clList.size();i++){
                // 모레 getDay ==2
                //날씨 상태가 비, 눈, 비/눈인 것을 확인한다.
                if(clList.get(i).getDay()==2){
                    j = i;
                    if((clList.get(i).getWfKor().equals("비") ||
                            clList.get(i).getWfKor().equals("눈") ||
                            clList.get(i).getWfKor().equals("비/눈"))){
                        flag = true;
                        script += clList.get(i).makeSpeechScriptForCloseFutureInfo();
                    }
                }
            }

            // 비나 눈이 안올때
            if(!flag)
                script += clList.get(j).makeSpeechScriptForCloseFutureInfo();

            title="[ 모레 날씨 ]";
            textResult = clList.get(j).makeWeatherDocs(false);
            insertImage(clList.get(j).getWfKor());

            //************중기 예보 날씨로 나옴**************
        //오늘을 기준으로 3일에서 10일 이후까지 날씨는 "오전/오후", 날씨상태로 나온다.

        //
        }
        /*else if(!dir.equals("") && ((indir1.equals("이번") && indir2.equals("주말")) || indir1.equals("주말") )){ // "이번 주말 날씨"라고 했을 경우
            //이번주 토, 일 보여줄것


        }else if(!dir.equals("") && (indir1.equals("다음") && indir2.equals("주말"))  ){ // "다음 주 주말 날씨"라고 했을 경우
            //다음주 토, 일 보여줄것
            //중기예보의 경우 최대 11일까지 보여줄 수 있으므로 다음주 주말이 그 이상일 경우는 장기예보로 넘긴다.


        }else if(!dir.equals("") && ((indir1.equals("다음") && indir2.equals("주")) || (indir1.equals("일주일") && indir2.equals("후")) )){ // "다음 주 날씨"라고 했을 경우
            //다음 주 월화수 보여줌.



            //************장기 예보 날씨로 나옴**************
            // 1. 1달 내에 1주, 2주, 3주, 4주째 기온을
        }else if(!dir.equals("") && ((indir1.equals("다음") && indir2.equals("달")) || (indir1.equals("한달") && indir2.equals("후")) )){ // "다음 달 날씨"


        }else if(!dir.equals("") && ((indir1.equals("다다음") && indir2.equals("달")) || (indir1.equals("두달") && indir2.equals("후")) )){ // "다다음 달 날씨"

        }*/

        textViewTitle.setText(title);

        //날씨정보 시각적으로 보여주기
        textViewSpeechResult.setText(textResult);


        //tts
        speak(script);
    }


    class RetrieveWeatherInfo extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... urls) {
            List<CloseDay> list=null;
            try{
                //"http://www.kma.go.kr/wid/queryDFS.jsp?gridx=61&gridy=125"
                list = parsingWeatherInfo(urls[0]);
                clList = list;
                Log.d(TAG, "====list 받기 절차[2] clList 사이즈 : "+clList.size());


            }catch (Exception e){
                e.printStackTrace();
                return null;
            }

            return null;
        }

    }


    public static List<CloseDay> parsingWeatherInfo(String targetUrl) throws Exception{
        List<CloseDay> list=null;
        URL url = new URL(targetUrl);
        HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        if(urlConnection == null) {return null;}

        urlConnection.setConnectTimeout(10000); //10 seconds
        urlConnection.setUseCaches(false); // 매번 서버에서 읽어오기

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
        int i = 0; //list의 index
        Log.d("====stuart tag : ","======parsingWeatherInfo 진입");

        if(urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK){

            InputStream inputStream = urlConnection.getInputStream();

            //xmlpullparser 이용

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

            factory.setNamespaceAware(true);

            XmlPullParser xpp = factory.newPullParser();

            xpp.setInput(inputStream,"utf-8");//웹서버에서 utf-8로 문자처리

            int eventType = xpp.getEventType();

            list = new ArrayList<CloseDay>();

            // 태그별 숫자 반환값
            //START_TAG == 2
            //END_TAG == 3
            //TEXT == 4
            //START_DOCUMENT == 0
            //END_DOCUMENT ==1

            while (eventType != XmlPullParser.END_DOCUMENT) {

                if(eventType == XmlPullParser.START_DOCUMENT) {
                    Log.d("====stuart tag : ","=====Start_document");
                } else if(eventType == XmlPullParser.START_TAG) {

                    tagName = xpp.getName();
                    //Log.d("====stuart tag : ","=====getName : "+tagName);
                    if (tagName.equalsIgnoreCase("data")) {
                        list.add(new CloseDay());
                        onEnd = false;
                        isItemTag1 = true;
                    }

                } else if(eventType == XmlPullParser.END_TAG) {
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
                } else if(eventType == XmlPullParser.TEXT && isItemTag1) {
                    //data setting
                    if (tagName.equals("hour") && !onHour) {
                        list.get(i).setHour(Integer.parseInt(xpp.getText()));
                        onHour = true;
                    }
                    if (tagName.equals("day") && !onDay) {
                        list.get(i).setDay(Integer.parseInt(xpp.getText()));
                        onDay = true;
                    }
                    if (tagName.equals("temp") && !onTem) {
                        list.get(i).setTemp(Double.parseDouble(xpp.getText()));
                        onTem = true;
                    }
                    if (tagName.equals("wfKor") && !onWfKor) {
                        list.get(i).setWfKor(xpp.getText());
                        onWfKor = true;
                    }
                    if (tagName.equals("pop") && !onPop) {
                        list.get(i).setPop(Integer.parseInt(xpp.getText()));
                        onPop = true;
                    }
                    if (tagName.equals("reh") && !onReh) {
                        list.get(i).setReh(Integer.parseInt(xpp.getText()));
                        onReh = true;
                    }
                    if (tagName.equals("ws") && !onWs) {
                        list.get(i).setWs(Double.parseDouble(xpp.getText()));
                        onWs = true;
                    }
                    if (tagName.equals("tmx") && !onTmx) {
                        list.get(i).setTmx(Double.parseDouble(xpp.getText()));
                        onTmx = true;
                    }
                    if (tagName.equals("tmn") && !onTmn) {
                        list.get(i).setTmn(Double.parseDouble(xpp.getText()));
                        onTmn = true;
                    }
                }
                eventType = xpp.next();
                //Log.d("====stuart tag : ","======eventType[3] : "+eventType);
            }//while의 끝
            inputStream.close();
        }//end if
        return list;
    }


    //tts 말하는 메서드, content에 담아서 말하면 됨.
    public void speak(String content){
        tts.speak(content,TextToSpeech.QUEUE_FLUSH,null);

    }

    //TextSpeech.onInitListener의 abstract method
    //원래는 이 메서드 내에서 speak를 하는 것 같으나 speak 하는 거 자체에 이 메서드에 대한 제약이 없어서
    //사용하지 않음.
    public void onInit(int status){}

    //위도 경도를 주소나 지명으로 변환하는 메서드
    // 특히 이 앱이 기준으로 삼는 지명만 반환하면 개꿀
    public String convertAddress(double lat, double lon){
        final Geocoder geocoder = new Geocoder(this);
        List<Address> list = null;
        try{
            list = geocoder.getFromLocation(lat,lon,10);

        }catch (IOException e){
            e.printStackTrace();
            Log.d("====stuart tag ","====입출력 오류 : 서버에서 주소변환시 에러발생");
        }
        if(list != null){
            if(list.size()==0){
                //speak("해당되는 주소가 없습니다.");
                Log.d("====stuart tag ","해당되는 주소 없음.");
            }else{
                //요게 보통 서울 기준으로 행정구 임.
                return list.get(0).getLocality();
            }
        }
        return null;
    }

    //날씨 상태 이미지 삽입하는 메서드
    public void insertImage(String state){
        //보이게 함.
        weatherState.setVisibility(View.VISIBLE);
        textViewTitle.setVisibility(View.VISIBLE);

        if(state.equals("맑음")){
            weatherState.setImageDrawable(getResources().getDrawable(R.drawable.day_clear));

        }else if(state.equals("구름 조금")) {
            weatherState.setImageDrawable(getResources().getDrawable(R.drawable.day_partial_cloud));

        }else if(state.equals("눈")) {
            weatherState.setImageDrawable(getResources().getDrawable(R.drawable.snow1));

        }else if(state.equals("구름 많음")) {
            weatherState.setImageDrawable(getResources().getDrawable(R.drawable.overcast));

        }else if(state.equals("흐림")) {
            weatherState.setImageDrawable(getResources().getDrawable(R.drawable.clouds));

        }else if(state.equals("비")) {
            weatherState.setImageDrawable(getResources().getDrawable(R.drawable.rain));

        }else if(state.equals("눈/비")) {
            weatherState.setImageDrawable(getResources().getDrawable(R.drawable.sleet));

        }

    }


}
