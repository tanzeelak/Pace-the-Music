package com.example.tanzeelakhan.music_player;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.MediaController;

/**
 * Created by tanzeelakhan on 7/17/17.
 */

public class MusicController extends MediaController {
    public MusicController(Context c) {
        super(c);
    }

    public void hide(){}
}


//public class MusicController extends MediaController {
//    Context c;
//    public MusicController(Context c){
//        super(c);
//        this.c = c;
//    }
//
//    public void hide(){}
//
//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        int keyCode = event.getKeyCode();
//        if(keyCode == KeyEvent.KEYCODE_BACK){
//            ((MainActivity)c).onBackPressed();
//            return true;
//        }
//        return super.dispatchKeyEvent(event);
//    }
//
//}
//

