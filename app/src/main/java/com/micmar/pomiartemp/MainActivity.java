package com.micmar.pomiartemp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.Image;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public ImageView iv;
    public Bitmap bitmap;
    public Canvas canvas;
    public Paint paint;
    public Paint text;
    public FFT fft;

    int samplingfrequency = 12000;
    int blockSize = 8192;

    double[] x;
    double[] y;
    double[] ampl;

    int frequency = 2048;

    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

    boolean loop = false;

    private final static float BASE = 370;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        x = new double[blockSize];
        y = new double[blockSize];

        ampl = new double[blockSize / 2];

        iv = (ImageView) this.findViewById(R.id.graph);
        bitmap = Bitmap.createBitmap((int) blockSize / 2, (int) 410, Bitmap.Config.ARGB_8888);

        canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);

        paint = new Paint();
        paint.setColor(Color.YELLOW);

        text = new Paint();
        text.setColor(Color.BLACK);
        text.setStyle(Paint.Style.FILL);
        canvas.drawPaint(text);

        text.setColor(Color.YELLOW);
        text.setTextSize(24);
        text.setTextScaleX((float) 7.0);

        iv.setImageBitmap(bitmap);

        fft = new FFT(blockSize);

        Button btn1 = (Button) findViewById(R.id.start);
        btn1.setOnClickListener(view -> {
            loop = true;
        });

        Button btn2 = (Button) findViewById(R.id.stop);
        btn2.setOnClickListener(view -> {
            loop = false;
        });

        this.startThread();
    }

    private void startThread() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    while (loop) {
                        drawView();
                        //Log.d("APP", "Dzialam w petli");
                        try { Thread.sleep(100); } catch (Throwable e) { }
                    }
                }
            }
        });
        t.start();
    }

    public void drawView() {

        readAudio();

        //for(int i = 0; i <= x.length - 1; i++) {
        //    Log.d("MIKROFON -> ", x[i] + "");
        //}

        int pick = 0;
        double max = 0.0;

        canvas.drawColor(Color.BLACK);
        paint.setColor(Color.YELLOW);
        iv.setImageBitmap(bitmap);

        //double t;
        //double c;

        fft.calculate(x, y);

        for(int i = 0; i < blockSize / 2; i++) {

            ampl[i] = x[i] * x[i] + y[i] * y[i];
            if (i > 0) {
                if(ampl[i] > max) {
                    max = ampl[i];
                    pick = i;
                }
            }
        }

        for (int i = 0; i < blockSize / 2; i++) {

            //t = (double) i / (double) samplingfrequency;
            //c = Math.sin(2 * Math.PI * frequency * t) * 50;

            //canvas.drawCircle((float) i + 2, (float) ((double) BASE - (double) 50.0  + (double) c), (float) 2.0, paint);
            //canvas.drawCircle((float) i, (float) ((double) BASE - (double) 10.0  + (double) +  x[i]), (float) 2.0, paint);

            canvas.drawLine(
                    (float) i,
                    (float) ((double) BASE - (double) 10.0),
                    (float) i,
                    (float) ((double) BASE - (double) 10.0  - (double)  ampl[i]), paint);
        }


        int freq = (pick * samplingfrequency) / blockSize;

         //Michal M.
         double a = 0.013;
         double b = -14.4;
         double temp = Math.round((a * freq + b) * 100.0) / 100.0;

        // Bartek. R
//        double a = 0.014;
//        double b = -16.86;
//        double temp = Math.round((a * freq + b) * 100.0) / 100.0;

        canvas.drawText("freq: " + freq, (float) 250.0, (float) 50.0, text);
        canvas.drawText("pick: " + pick, (float) 250.0, (float) 75.0, text);
        canvas.drawText("temp: " + temp, (float) 250.0, (float) 100.0, text);

        iv.invalidate();
        //frequency += 20;
    }

    protected void readAudio() {

        short[] audioBuffer = new short[blockSize];
        int bufferSize = AudioRecord.getMinBufferSize(samplingfrequency, channelConfiguration, audioEncoding);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                samplingfrequency,
                channelConfiguration,
                audioEncoding,
                bufferSize
        );

        audioRecord.startRecording();

        int bufferReadResult = audioRecord.read(audioBuffer, 0, blockSize);

        for(int i = 0; i < blockSize && i < bufferReadResult; i++) {
            x[i] = (double) audioBuffer[i] / 32768.0;
        }

        for(int i = 0; i < blockSize && i < bufferReadResult; i++) {
            y[i] = 0;
        }

        audioRecord.stop();
    }
}