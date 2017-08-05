package com.example.tanzeelakhan.music_player;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by tanzeelakhan on 8/4/17.
 */

public class StepTimer extends Activity implements SensorEventListener{
//    private TextView time;
    private Button start;
    private Button cancel;
    private CountDownTimer countDownTimer;
    boolean running = false;
    SensorManager sensorManager;
    TextView tv_steps;
    float initialStep1 = 0;
    float initialStep = 0;
    float finalStep = 0;
    float finalStep1 = 0;
    boolean timerRunning = false;
    int onFinishStep = 0;


    private View.OnClickListener btnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.start :
                    start();
                    break;
//                case R.id.cancel :
//                    cancel();
//                    break;
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        // btnClickListener = (Button) findViewById(R.id.addTaskButton);
//        start = (Button) findViewById(R.id.start);
//        start.setOnClickListener(btnClickListener);
//        cancel = (Button) findViewById(R.id.cancel);
//        cancel.setOnClickListener(btnClickListener);
//        time = (TextView) findViewById(R.id.time);
//        tv_steps = (TextView) findViewById(R.id.tv_steps);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }


    public void start() {
        if(timerRunning == false) {
            timerRunning = true;
            initialStep = initialStep1;
//            time.setText("15");
            Log.d("time", "15");


            countDownTimer = new CountDownTimer(15 * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
//                    time.setText("" + millisUntilFinished / 1000);
                    Log.d("time", Integer.toString((int)(millisUntilFinished / 1000)));

                }

                @Override
                public void onFinish() {
//                    time.setText("Done !");
                    timerRunning =false;
                    finalStep = finalStep1;
                    onFinishStep = (int)(finalStep - initialStep);
//                    tv_steps.setText(String.valueOf(finalStep - initialStep));
                    Log.d("tv_steps", String.valueOf(finalStep - initialStep));
                }
            };

            countDownTimer.start();
        }
    }

    private void cancel() {
        if(countDownTimer != null){
            countDownTimer.cancel();
            countDownTimer = null;
            timerRunning = false;
        }
    }

    @Override
    protected  void onResume(){
        super.onResume();
        running = true;
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null){
            sensorManager.registerListener(this, countSensor, sensorManager.SENSOR_DELAY_UI);

        } else {
            Toast.makeText(this, "Sensor not found!!1", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        running = false;
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        if (running){
            initialStep1 = event.values[0];
            finalStep1 = event.values[0];
            start();

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public int getOnFinishStep(){
        return onFinishStep;
    }

    public boolean hasFinished(){
        if (timerRunning){
            return false;
        }
        else {
            return true;
        }
    }
}
