package com.example.tanzeelakhan.music_player;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //retrieve list view
        songView = (ListView)findViewById(R.id.song_list);
        //instantiate list
        songList = new ArrayList<Song>();
        //get songs from device
        getSongList();
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
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisBpm = doInBackground(thisTitle, thisArtist);
                Log.d("TITLE", thisTitle);
                Log.d("ARTIST", thisArtist);
                Log.d("BPM", thisBpm);
                songList.add(new Song(thisId, thisTitle, thisArtist, thisBpm));
            }
            while (musicCursor.moveToNext());
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
                //Log.d("plsY","hello");
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                Log.d("json object", stringBuilder.toString());
//                    Log.d("bpm", stringBuilder.toString().tempo);

                JSONObject jObject = new JSONObject(stringBuilder.toString());
                try {
                    JSONArray jArray = jObject.getJSONArray("search");

                    for (int i = 0; i < jArray.length(); i++) {
                        try {
                            JSONObject oneObject = jArray.getJSONObject(i);
                            // Pulling items from the array
                            bpm = oneObject.getString("tempo");
                            // String oneObjectsItem2 = oneObject.getString("anotherSTRINGNAMEINtheARRAY");
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

//                    Log.d("BPM PLEASE", oneOb);

//                    return stringBuilder.toString();

                    return bpm;
                //return ("BPM: " + bpm).toString();
//                    return bpm;
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

}