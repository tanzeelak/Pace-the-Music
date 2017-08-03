package com.example.tanzeelakhan.music_player;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
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
import java.util.List;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity implements MediaPlayerControl {

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
//        listAllTags(g0, 0);
//        listAllTags(g1, 1);
//        listAllTags(g2, 2);
//        listAllTags(g3, 3);
//        listAllTags(g4, 4);
//        listAllTags(g5, 5);
//        listAllTags(g6, 6);
//        listAllTags(g7, 7);
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
//            automatic playing
            musicSrv.setSong(2);
            musicSrv.playSong();
            controller.show();
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
        musicSrv.playSong();
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
                if (!thisArtist.equals("<unknown>")) {
                    Log.d("found","keep");
                    songList.add(new Song(thisId, thisTitle, thisArtist, thisBpm, thisTag));
                    Log.d("tagNum", Integer.toString(thisTag));
                    if (numBpm <= 80){
                        g0.add(thisTag);
                        Log.d("group", "0");
                        Log.d("name", thisTitle);
                    }
                    else if (numBpm > 80 && numBpm <= 88) {
                        g1.add(thisTag);
                        Log.d("group", "1");
                        Log.d("name", thisTitle);
                    }
                    else if (numBpm > 88 && numBpm <= 96) {
                        g2.add(thisTag);
                        Log.d("group", "2");
                        Log.d("name", thisTitle);
                    }
                    else if (numBpm > 96 && numBpm <= 104) {
                        g3.add(thisTag);
                        Log.d("group", "3");
                        Log.d("name", thisTitle);
                    }
                    else if (numBpm > 104 && numBpm <= 112) {
                        g4.add(thisTag);
                        Log.d("group", "4");
                        Log.d("name", thisTitle);
                    }
                    else if (numBpm > 112 && numBpm <= 120) {
                        g5.add(thisTag);
                        Log.d("group", "5");
                        Log.d("name", thisTitle);
                    }
                    else if (numBpm > 120 && numBpm <= 128) {
                        g6.add(thisTag);
                        Log.d("group", "6");
                        Log.d("name", thisTitle);
                    }
                    else if (numBpm > 128) {
                        g7.add(thisTag);
                        Log.d("group", "7");
                        Log.d("name", thisTitle);
                    }

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
                                bpm = "100";
                            }
                            Log.d("BPM", bpm);

                        } catch (JSONException e) {
                            // Oops
                        }
                    }
                }
                catch (Exception e){
                    Log.d("hello","lol");
                    bpm = "100";
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
            bpm = "100";
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

//        controller = new MusicController(this);
        //set previous and next button listeners
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        //set and show
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
    }

    private void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private void playPrev(){
        musicSrv.playPrev();
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
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
//            setController();
            paused=false;
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

    private List mapStepstoBPM(int steps){
        if (steps <= 20){
            return g0;
        }
        else if (steps > 20 && steps <= 22) {
            return g1;
        }
        else if (steps > 22 && steps <= 24) {
            return g2;
        }
        else if (steps > 24 && steps <= 26) {
            return g3;
        }
        else if (steps > 26 && steps <= 28) {
            return g4;
        }
        else if (steps > 28 && steps <= 30) {
            return g5;
        }
        else if (steps > 30 && steps <= 32) {
            return g6;
        }
        else if (steps > 32) {
            return g7;
        }
        return g7;
    }


}