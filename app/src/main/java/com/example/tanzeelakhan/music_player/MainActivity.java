package com.example.tanzeelakhan.music_player;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import com.example.tanzeelakhan.music_player.MusicService.MusicBinder;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class MainActivity extends Activity implements MediaPlayerControl,SensorEventListener {

    //song list variables
    private ArrayList<Song> songList;
    private List g0 = new ArrayList();
    private List g1 = new ArrayList();
    private List g2 = new ArrayList();
    private List g3 = new ArrayList();
    private List g4 = new ArrayList();
    private List g5 = new ArrayList();
    private List g6 = new ArrayList();
    private List g7 = new ArrayList();
    private List gChosen;
    private List groupLists = new ArrayList();
    private ListView songView;

    //service
    private MusicService musicSrv;
    private Intent playIntent;
    //binding
    private boolean musicBound=false;

    //controller
    private MusicController controller;

    //activity and playback pause flags
    private boolean paused=false, playbackPaused=false;

    static final String API_KEY = "e325699e517ecfe9167e0b3397e0a646";
    static final String API_URL = "https://api.getsongbpm.com/search/?";
    String bpm;
    int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 18;
    Random random = new Random();
//    StepTimer stepTimer = new StepTimer();

    private CountDownTimer countDownTimer;
    boolean running = false;
    SensorManager sensorManager;
    TextView currentSongTitle;
    TextView currentTime;
    TextView currentSteps;

    float initialStep1 = 0;
    float initialStep = 0;
    float finalStep = 0;
    float finalStep1 = 0;
    boolean timerRunning = false;
    int onFinishStep = 0;
    int iterateList;
    int currentTag;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant that should be quite unique

            return;
        }

        //retrieve list view
        songView = (ListView)findViewById(R.id.song_list);
        //instantiate list
        songList = new ArrayList<Song>();
        //get songs from device
        getSongList();
        Collections.sort(songList);
        for (int i = 0; i < songList.size(); i++) {
            int numBpm = Integer.parseInt(songList.get(i).getBpm());
            String title = songList.get(i).getTitle();
            addSongsToGroups(numBpm, i ,title);
        }

        groupLists.add(g0);
        groupLists.add(g1);
        groupLists.add(g2);
        groupLists.add(g3);
        groupLists.add(g4);
        groupLists.add(g5);
        groupLists.add(g6);
        groupLists.add(g7);

        listAllTags(g0, 0);
        listAllTags(g1, 1);
        listAllTags(g2, 2);
        listAllTags(g3, 3);
        listAllTags(g4, 4);
        listAllTags(g5, 5);
        listAllTags(g6, 6);
        listAllTags(g7, 7);
        //sort alphabetically by title
        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });
        //create and set adapter
        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        //setup controller
        setController();
//        SensorEvent event = new SensorEvent();
//        stepTimer.onResume();
        currentSongTitle = (TextView)findViewById(R.id.currentSongTitle);
        currentTime = (TextView)findViewById(R.id.currentTime);
        currentSteps = (TextView)findViewById(R.id.currentSteps);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicSrv.controller = controller;
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    //start and bind the service when the activity starts
    @Override
    protected void onStart() {
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
        super.onStart();
    }

    //user song select
    public void songPicked(View view){
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        Log.d("tag", view.getTag().toString());
        currentTag = (int)view.getTag();
        String title = musicSrv.playSong();
//        Log.d("song title", songList.get((int)view.getTag()).getTitle());
        currentSongTitle.setText(title);
        controller.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //method to retrieve song info from device
    public void getSongList(){
        //query external audio
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        //iterate over results if valid
        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            int thisTag = 0;
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisBpm = doInBackground(thisTitle, thisArtist);
                int numBpm = Integer.parseInt(thisBpm);
                Log.d("TITLE", thisTitle);
                Log.d("ARTIST", thisArtist);
                Log.d("BPM", thisBpm);
                if (!thisArtist.equals("<unknown>") && numBpm != 0) {
                    Log.d("found","keep");
                    songList.add(new Song(thisId, thisTitle, thisArtist, thisBpm));
                    Log.d("tagNum", Integer.toString(thisTag));
//                    addSongsToGroups(numBpm, thisTag, thisTitle);
                    thisTag++;
                }
            } while (musicCursor.moveToNext());
        }
    }

    protected String doInBackground(String songText, String artistText) {

        //works with test case
        //songText = "All of me";
        //artistText = "John Legend";

        String song = songText.toString();
        song = song.replaceAll("\\s","+");
        String artist = artistText.toString();
        artist = artist.replaceAll("\\s","+");
        // Do some validation here

        try {

            URL url = new URL(API_URL + "api_key=" + API_KEY + "&type=both&lookup=song:" + song + "%20artist:" + artist);
//            Log.d("url", url.toString());
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                Log.d("json object", stringBuilder.toString());

                JSONObject jObject = new JSONObject(stringBuilder.toString());
                try {
                    JSONArray jArray = jObject.getJSONArray("search");

                    for (int i = 0; i < jArray.length(); i++) {
                        try {
                            JSONObject oneObject = jArray.getJSONObject(i);
                            // Pulling items from the array
                            bpm = oneObject.getString("tempo");
                            if (bpm.equals("null")) {
                                bpm = "0";
                            }
                            Log.d("BPM", bpm);

                        } catch (JSONException e) {
                            // Oops
                        }
                    }
                }
                catch (Exception e){
                    Log.d("hello","lol");
                    bpm = "0";
                    return bpm;
                }
                return bpm;
            }
            finally{
                urlConnection.disconnect();
            }
        }
        catch(Exception e) {
            Log.d("plso","helloo");
            Log.e("ERROR", e.getMessage(), e);
            bpm = "0";
            return bpm;
        }
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public void start() {
        playbackPaused = false;
        musicSrv.go();
        controller.show(0);
    }

    //set the controller up
    private void setController(){
        if(controller == null)
            controller = new MusicController(this);
        else
            controller.invalidate();
        //set previous and next button listeners
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //currentSongTitle.setText(songList.get(currentTag + 1).getTitle());
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //currentSongTitle.setText(songList.get(currentTag - 1).getTitle());
                playPrev();
            }
        });
        //set and show
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    private void playNext(){
        String title = musicSrv.playNext();
        currentSongTitle.setText(title);
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private void playPrev(){
        String title = musicSrv.playPrev();
        currentSongTitle.setText(title);
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused=true;
        running = false;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            paused=false;
        }

        running = true;
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, sensorManager.SENSOR_DELAY_UI);

        } else {
            Toast.makeText(this, "Sensor not found!!1", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    private void listAllTags(List group, int groupNum) {
        Log.d("group num", Integer.toString(groupNum));
        for (int i = 0; i < group.size(); i++){
            Log.d("song element", group.get(i).toString());
        }
    }

    public List mapStepstoBPM(int steps){
        if (steps <= 20){
            Log.d("groupNumAgain", "0");
            iterateList = 0;
            return (List)groupLists.get(0);
        }
        else if (steps > 20 && steps <= 22) {
            Log.d("groupNumAgain", "1");
            iterateList = 1;
            return (List)groupLists.get(1);
        }
        else if (steps > 22 && steps <= 24) {
            Log.d("groupNumAgain", "2");
            iterateList = 2;
            return (List)groupLists.get(2);
        }
        else if (steps > 24 && steps <= 26) {
            Log.d("groupNumAgain", "3");
            iterateList = 3;
            return (List)groupLists.get(3);
        }
        else if (steps > 26 && steps <= 28) {
            Log.d("groupNumAgain", "4");
            iterateList = 4;
            return (List)groupLists.get(4);
        }
        else if (steps > 28 && steps <= 30) {
            Log.d("groupNumAgain", "5");
            iterateList = 5;
            return (List)groupLists.get(5);
        }
        else if (steps > 30 && steps <= 32) {
            Log.d("groupNumAgain", "6");
            iterateList = 6;
            return (List)groupLists.get(6);
        }
        else{// if (steps > 32) {
            Log.d("groupNumAgain", "7");
            iterateList = 7;
            return (List)groupLists.get(7);
        }
    }

    private int chooseSongbySteps(List group) {
        int size = group.size() - 1;
        int groupIndex = random.nextInt(size + 1);
        int index = (int) group.get(groupIndex);
        Log.d("hah", Integer.toString(index));
        return index;
    }

    private void addSongsToGroups(int bpmSong, int tagSong, String titleSong){
        if (bpmSong <= 80){
            g0.add(tagSong);
            Log.d("group", "0");
            Log.d("name", titleSong);
        }
        else if (bpmSong > 80 && bpmSong <= 88) {
            g1.add(tagSong);
            Log.d("group", "1");
            Log.d("name", titleSong);
        }
        else if (bpmSong > 88 && bpmSong <= 96) {
            g2.add(tagSong);
            Log.d("group", "2");
            Log.d("name", titleSong);
        }
        else if (bpmSong > 96 && bpmSong <= 104) {
            g3.add(tagSong);
            Log.d("group", "3");
            Log.d("name", titleSong);
        }
        else if (bpmSong > 104 && bpmSong <= 112) {
            g4.add(tagSong);
            Log.d("group", "4");
            Log.d("name", titleSong);
        }
        else if (bpmSong > 112 && bpmSong <= 120) {
            g5.add(tagSong);
            Log.d("group", "5");
            Log.d("name", titleSong);
        }
        else if (bpmSong > 120 && bpmSong <= 128) {
            g6.add(tagSong);
            Log.d("group", "6");
            Log.d("name", titleSong);
        }
        else if (bpmSong > 128) {
            g7.add(tagSong);
            Log.d("group", "7");
            Log.d("name", titleSong);
        }
    }

    public void startTimer() {
        if (timerRunning == false) {
            timerRunning = true;
            initialStep = initialStep1;
//            time.setText("15");
            currentTime.setText("15");
            Log.d("time", "15");


            countDownTimer = new CountDownTimer(15 * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    String timeText = Integer.toString((int) (millisUntilFinished / 1000 - 1));
                    currentTime.setText(timeText);
                    Log.d("time", timeText);

                }

                @Override
                public void onFinish() {
                    timerRunning = false;
                    finalStep = finalStep1;
                    onFinishStep = (int) (finalStep - initialStep);
                    String stepsText =  String.valueOf(finalStep - initialStep);
                    currentSteps.setText(stepsText);
                    Log.d("tv_steps", stepsText);
                    gChosen = mapStepstoBPM((int)(finalStep-initialStep));

                    while (gChosen.isEmpty())
                    {
                        if (iterateList == 7)
                            iterateList = 0;
                        else
                            iterateList++;
                        gChosen = (List)groupLists.get(iterateList);
                    }

                    int tag = chooseSongbySteps(gChosen);
                    Log.d("tag please", Integer.toString(tag));
                    musicSrv.setSong(tag);
                    String title = musicSrv.playSong();
                    currentTag = tag;
                    currentSongTitle.setText(title);
                    controller.show();

                }
            };

            countDownTimer.start();
        }
    }

    private void cancel() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
            timerRunning = false;
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (running) {
            initialStep1 = event.values[0];
            finalStep1 = event.values[0];
            Log.d("initialStep1 b4", Integer.toString((int)initialStep1));
            MyThread p = new MyThread();
            p.start();
            startTimer();
//            currentTime.setText("0");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class MyThread extends Thread{
        @Override
        public void run() {
            startTimer();
        }
    }
}