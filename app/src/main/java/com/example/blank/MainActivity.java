package com.example.blank;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity implements SensorEventListener {
    // Google Pixel XL in 16:9 crop mode
    private final double VERTICAL_FOV = 66.9;
    private final double DIAGONAL_FOV = 74.32;
    private final double HORIZONTAL_FOV = 40.77;

    // sensors
    private LocationManager locationManager;
    private LocationListener locationListener;
    private SensorManager sensorManager;

    // handler for thread communication
    private Handler mainHandler = new Handler();

    // UI components
    private View view;
    TextView x, y, z,lat,longi,alt,bThread,phiWin,thetaWin,testVisible;
    private float [] mRotationMatrix;

    // Globals
    public static float[] mOrientation;
    public static Location mLocation;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // system services
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        // orientations values
        mRotationMatrix =  new float[16];
        mOrientation = new float[3];

        // get textviews
        x = findViewById(R.id.xVal);
        y = findViewById(R.id.yVal);
        z = findViewById(R.id.zVal);
        lat = findViewById(R.id.latVal);
        longi = findViewById(R.id.longVal);
        alt = findViewById(R.id.altVal);
        bThread = findViewById(R.id.bThread);
        phiWin = findViewById(R.id.phiWindow);
        thetaWin = findViewById(R.id.thetaWindow);
        testVisible = findViewById(R.id.testAircraftVisible);

        // register sensor manager
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);

        // create new location listener
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                updateLocationInfo(location);
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {}
            @Override
            public void onProviderEnabled(String S){}
            @Override
            public void onProviderDisabled(String S){}
        };

        // start location listener
        startListening();

        // check permissions
        if (Build.VERSION.SDK_INT >= 23 &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,0,locationListener);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location!=null) {
            updateLocationInfo(location);
            mLocation = location;
        }

        // start calculation thread in background
        calcThread runner = new calcThread();
        new Thread(runner).start();
    }

    public void startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening();
        }
    }



    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,
                    SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, mOrientation);
            float azimuth = mOrientation[0];
            float pitch = mOrientation[1];
            float roll = mOrientation[2];
            float phi = azimuthToPhi(azimuth);
            float theta = pitchToTheta(pitch);
            float adjRoll = adjustRoll(roll);
            double phiBound1 = ((phi - Math.toRadians(DIAGONAL_FOV)/2) % (Math.PI*2) + (2*Math.PI)) % (2*Math.PI);
            double phiBound2 = (phi + Math.toRadians(DIAGONAL_FOV)/2) % (Math.PI*2);
            double thetaBound1 = theta - Math.toRadians(DIAGONAL_FOV)/2;
            double thetaBound2 = theta + Math.toRadians(DIAGONAL_FOV)/2;
            x.setText("Phi: " + Float.toString(phi));
            y.setText("Theta:       " + Float.toString(theta));
            z.setText("Adjusted Roll:          " + Float.toString(adjRoll));
            phiWin.setText("Phi Window: " + Float.toString((float)phiBound1) + " " + Float.toString((float)phiBound2));
            thetaWin.setText("Theta Window: " + Float.toString((float)thetaBound1) + " " + Float.toString((float)thetaBound2));
            AirTracker airTracker = new AirTracker();
            // Query all flights in a square the size of the diagonal fov.
            airTracker.
            ArrayList<Aircraft> visibleAircraft = airTracker
                    .getAircraftInWindow(0,0,2*Math.PI, Math.PI);
            if (visibleAircraft.size() > 0) {
                testVisible.setText("Test Visible: True");
            } else {
                testVisible.setText("Test Visible: False");
            }
        }
    }

    public void updateLocationInfo(Location location) {
        lat.setText("lat:        " + location.getLatitude());
        longi.setText("longi:   " + location.getLongitude());
        alt.setText("alt:       " + location.getAltitude());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

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

    // background thread that is always running and keeping track of time
    class calcThread implements Runnable {
        private long currentCount;

        // constructor method
        public calcThread() {
            currentCount = 0;
        }

        // main method for runnable
        @Override
        public void run() {
            // when spawned always run in background
            while (true) {
                // keep thread at a low load
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                currentCount += 1;

                // post to the main handler
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        bThread.setText("count: " + currentCount);
                    }
                });
            }
        }
    }

    private float azimuthToPhi(float az) {
        return (float) ( ((Math.PI/2 - az) % (2*Math.PI) + (2*Math.PI)) % (2*Math.PI) );
    }

    private float pitchToTheta(float pitch) {
        return (float) (pitch + Math.PI/2);
    }

    private float adjustRoll(float roll) {
        return (float) ( ((roll + Math.PI/2) % (Math.PI*2) + (2*Math.PI)) % (2*Math.PI));
    }
}