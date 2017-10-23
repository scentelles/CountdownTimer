package com.blogspot.chunkingz.countdowntimer;

import android.app.KeyguardManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import static java.nio.charset.StandardCharsets.UTF_8;


public class MainActivity extends AppCompatActivity {

    SeekBar timerSeekBar;
    TextView timerTextView;
    ImageView go;
    ImageView stop;
    Button controllerButton;
    Boolean counterIsActive = false;
    CountDownTimer countDownTimer;
    PowerManager pm;
    PowerManager.WakeLock wl;

    MqttClient myclient = null; //Persistence


    public MainActivity() throws MqttException {
    }

    public void resetTimer(){
        //timerTextView.setText("0:30");
        controllerButton.setText("Go!");
        timerSeekBar.setEnabled(true);
        //timerSeekBar.setProgress(30);
        countDownTimer.cancel();
        counterIsActive = false;
    }

    public void updateTimer(int secondsLeft){
        int mins =  secondsLeft / 60;
        int secs =  secondsLeft - mins * 60;

        String mins2 = String.valueOf(mins);
        String secs2 = String.valueOf(secs);

        if (secs <= 9){
            secs2 = "0" + secs2;
        }
               /* if (mins2.equals("0")){
                    mins2 = "00";
                }*/


        assert timerTextView != null;
        timerTextView.setText(mins2 +":"+ secs2);
    }
    public void turnTvOff(View view){
        if(!myclient.isConnected())
        {
            Log.e("myTimer", "Trying to reconnect");
            try {
                myclient.connect();

            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        Log.e("myTimer", "Publishing");
        try {
            myclient.publish(
                    "PRISE_TV/command", // topic
                    "1".getBytes(UTF_8), // payload
                    0, // QoS
                    false); // retained?
        } catch (MqttException e) {
            Log.e("myTimer", "publish failed");
            e.printStackTrace();
        }

    }
    public void turnTvOn(View view){
        if(!myclient.isConnected())
        {
            Log.e("myTimer", "Trying to reconnect");
            try {
                myclient.connect();

            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
        Log.e("myTimer", "Publishing");
        try {
            myclient.publish(
                    "PRISE_TV/command", // topic
                    "2".getBytes(UTF_8), // payload
                    0, // QoS
                    false); // retained?
        } catch (MqttException e) {
            Log.e("myTimer", "publish failed");
            e.printStackTrace();
        }

    }

    public void controlTimer(View view){
        Log.i("button pressed", "pressed");

        if (counterIsActive == false) {

            wl.acquire();

            counterIsActive = true;
            timerSeekBar.setEnabled(false);
            controllerButton.setText("Stop");
            go.setVisibility(View.VISIBLE);
            stop.setVisibility(View.INVISIBLE);

            countDownTimer = new CountDownTimer(timerSeekBar.getProgress() * 1000 + 100, 1000) {
                int countStop = 0;
                @Override
                public void onTick(long millisUntilFinished) {
                    updateTimer((int) millisUntilFinished / 1000);
                    if((millisUntilFinished <= 120000) &&  (countStop == 0)){
                        MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.airhorn);
                        mediaPlayer.start();
                        countStop ++;
                    }

                }

                @Override
                public void onFinish() {
                    timerTextView.setText("0:00");
                    go.setVisibility(View.INVISIBLE);
                    stop.setVisibility(View.VISIBLE);
                    MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.airhorn);
                    mediaPlayer.start();
                    resetTimer();


                    PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");
                    wakeLock.acquire();

                    KeyguardManager keyguardManager = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
                    KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
                    keyguardLock.disableKeyguard();


                    Log.e("myTimer", "checking if connected");
                    if(!myclient.isConnected())
                    {
                        Log.e("myTimer", "Trying to reconnect");
                        try {
                            myclient.connect();

                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.e("myTimer", "Publishing");
                    try {
                        myclient.publish(
                                "PRISE_TV/command", // topic
                                "1".getBytes(UTF_8), // payload
                                0, // QoS
                                false); // retained?
                    } catch (MqttException e) {
                        Log.e("myTimer", "publish failed");
                        e.printStackTrace();
                    }
                    wl.release();
                }
            }.start();
        }else {
            timerTextView.setText("10:00");
            timerSeekBar.setProgress(600);
            resetTimer();
            wl.release();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toast.makeText(getApplicationContext(),"Countdown Timer \nCreated by Kingston Fortune",Toast.LENGTH_LONG).show();


        timerSeekBar = (SeekBar)findViewById(R.id.timerSeekBar);
        timerTextView = (TextView) findViewById(R.id.timerTextView);
        go = (ImageView)findViewById(R.id.imageView);
        stop = (ImageView)findViewById(R.id.imageView2);
        controllerButton = (Button)findViewById(R.id.controllerButton);
        assert timerSeekBar != null;
        timerSeekBar.setMax(6000);
        timerSeekBar.setProgress(600);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");

        timerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTimer(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        try {
            myclient = new MqttClient(
                    "tcp://192.168.1.27:1883", //URI
                    MqttClient.generateClientId(), //ClientId
                    new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
        }

          try {
              myclient.connect();

          } catch (MqttException e) {
              e.printStackTrace();
         }
    }
}
