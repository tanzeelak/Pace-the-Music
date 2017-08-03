package com.example.tanzeelakhan.music_player;

/**
 * Created by tanzeelakhan on 7/17/17.
 */

public class Song {

    private long id;
    private String title;
    private String artist;
    private String bpm;
    private int tagNum;

    public Song(long songID, String songTitle, String songArtist, String songBpm, int songTag){
        id=songID;
        title=songTitle;
        artist=songArtist;
        bpm=songBpm;
        tagNum=songTag;
    }

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public String getBpm(){return bpm;}
    public int getTagNum(){return tagNum;}

}