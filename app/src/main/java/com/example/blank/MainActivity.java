package com.example.blank;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private View view;
    TextView x, y, z,lat,longi,alt;
    private float initialSteps;
    private float[] mRotationMatrix;
    private float[] mOrientation;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRotationMatrix =  new float[16];
        mOrientation = new float[3];

        setContentView(R.layout.activity_main);

        // get textviews
        x = (TextView) findViewById(R.id.xVal);
        y = (TextView) findViewById(R.id.yVal);
        z = (TextView) findViewById(R.id.zVal);
        lat = (TextView) findViewById(R.id.latVal);
        longi = (TextView) findViewById(R.id.longVal);
        alt = (TextView) findViewById(R.id.altVal);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            Log.i("AYYYY","ROTATION VECTOR[0]:"+event.values[0]);
            Log.i("AYYYY","ROTATION VECTOR[1]:"+event.values[1]);
            Log.i("AYYYY","ROTATION VECTOR[2]:"+event.values[2]);
            Log.i("AYYYY","ROTATION VECTOR[3]:"+event.values[3]);
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,
                    SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, mOrientation);
            float azimuth = mOrientation[0];
            float pitch = mOrientation[1];
            float roll = mOrientation[2];
            x.setText(Float.toString(azimuth));
            y.setText(Float.toString(pitch));
            z.setText(Float.toString(roll));
        }
    }
    //    private void displayDirection(SensorEvent event) {
//        // Displays the rough cardinal direction detected by the magnetometer
//        float angle = event.values[0];
//
//        if ((angle > 337.5 && angle < 360) || (angle > 0 && angle < 22.5)) {
//            dir = "N";
//        } else if (angle > 22.5 && angle < 67.5) {
//            dir = "NE";
//        } else if (angle > 67.5 && angle < 112.5) {
//            dir = "E";
//        } else if (angle > 112.5 && angle < 157.5) {
//            dir = "SE";
//        } else if (angle > 157.5 && angle < 202.5) {
//            dir = "S";
//        } else if (angle > 202.5 && angle < 247.5) {
//            dir = "SW";
//        } else if (angle > 247.5 && angle < 292.5) {
//            dir = "W";
//        } else if (angle > 292.5 && angle < 337.5) {
//            dir = "NW ";
//        }
//
//        textDir.setText(dir);
//    }
//
//    private void checkStep(SensorEvent event) {
//
//        // Movement
//        //float x = event.values[0];
//        //float y = event.values[1];
//        float z = event.values[2];
//        Log.d("Acc-Z", Float.toString(z));
//        //
//        if (z >= threshHi)
//        {
//            if (triggerHi) {
//                myStepCount++;
//                triggerHi = false;
//                textMy.setText(Integer.toString(myStepCount));
//                //textMy.post(new Runnable()  { public void run() { textMy.setText(myStepCount);} });
//            }
//        }
//        if (z <= threshLow) {
//            triggerHi = true;
//        }
//    }
//
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}