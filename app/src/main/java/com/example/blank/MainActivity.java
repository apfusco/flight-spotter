package com.example.blank;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.Point;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
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
    private TextView x, y, z,lat,longi,alt,bThread,phiWin,thetaWin,testVisible;
    private float [] mRotationMatrix;
    private ImageView planeThing;

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
        planeThing = findViewById(R.id.planeThing);

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
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && mLocation != null) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,
                    SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, mOrientation);
            float azimuth = mOrientation[0];
            float pitch = mOrientation[1]; // Invert to tell us what the camera angle is not the screen.
            float roll = mOrientation[2];
            float phi = azimuthToPhi(azimuth);
            float theta = pitchToTheta(pitch);
            float pitchInv = -pitch; // Invert to tell us what the camera angle is not the screen.
            float adjRoll = adjustRoll(roll);
            // TODO: fix this so that the window width doesn't decrease when pitch increases etc. How to compensate? Rotate so pitch = 0 straight up?
            double azBound1 = (((azimuth - Math.toRadians(DIAGONAL_FOV)/2 + Math.PI) % (Math.PI*2) + (2*Math.PI)) % (2*Math.PI)) - Math.PI;
            double azBound2 = (azimuth + Math.toRadians(DIAGONAL_FOV)/2 + Math.PI) % (Math.PI*2) - Math.PI;
            double pitchBound1 = (((pitchInv - Math.toRadians(DIAGONAL_FOV)/2 + Math.PI/2) % (Math.PI) + (Math.PI)) % (Math.PI)) - Math.PI/2;;
            double pitchBound2 = (pitchInv + Math.toRadians(DIAGONAL_FOV)/2 + Math.PI/2) % (Math.PI) - Math.PI/2;
            if (pitchBound1 > 0 && pitchInv < 0){
                pitchBound1 = -pitchBound1;
            }
            if (pitchBound2 < 0 && pitchInv > 0){
                pitchBound2 = -pitchBound2;
            }
            // Handle north pole case
            x.setText("Az: " + Float.toString(azimuth));
            y.setText("Pitch_inv:       " + Float.toString(pitchInv));
            z.setText("Adjusted Roll:          " + Float.toString(adjRoll));
            phiWin.setText("Az Window: " + Float.toString((float)azBound1) + " " + Float.toString((float)azBound2));
            thetaWin.setText("Pitch Window: " + Float.toString((float)pitchBound1) + " " + Float.toString((float)pitchBound2));
            AirTracker airTracker = new AirTracker();
            // Query all flights in a square the size of the diagonal fov.
            // airTracker.
            ArrayList<Aircraft> visibleAircraft = airTracker
                    .getAircraftInWindow(azBound1,pitchBound1,azBound2, pitchBound2);
            if (visibleAircraft.size() > 0) {
                testVisible.setText("Test Visible: True");
            } else {
                testVisible.setText("Test Visible: False");
            }

            // Do a 2-D mapping of angles of flights relative to angle of phone to positions in a 2-D
            // square of positions on the screen.
            WindowManager windowManager = (WindowManager) getApplicationContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            float maxX = metrics.widthPixels;
            float maxY = metrics.heightPixels;

            float windowWidthPx = Math.round(maxX * (DIAGONAL_FOV/HORIZONTAL_FOV));
            float windowHeightPx = Math.round(maxY * (DIAGONAL_FOV/VERTICAL_FOV));

            Log.i("Screen Metrics", " maxX:" + maxX+ " maxY:"+ maxY+ " windowWidthPx:"+windowWidthPx+ " windowHeightPx:"+ windowHeightPx);

            // Get relative angles of flights from center of screen and transform from relative angle to
            // x,y pixel coordinates with the center of the screen being the origin.
            for (Aircraft aircraft: visibleAircraft) {
                double aircraftAzimuth = aircraft.getAzimuth();
                double aircraftPitch = aircraft.getPitch();
                // Find relative "angular position" and convert from radians to pixels
                // Handle cases where the azimuths are on opposite sides of the south direction
                int x = 0;
                if (aircraft.getAzimuth() - azimuth > Math.toRadians(DIAGONAL_FOV)) {
                    x = (int) Math.round((2*Math.PI - aircraft.getAzimuth() - azimuth) * (windowHeightPx/Math.toRadians(DIAGONAL_FOV)));
                }
                else if (aircraft.getAzimuth() - azimuth < -Math.toRadians(DIAGONAL_FOV)) {
                    x = (int) Math.round((2*Math.PI + aircraft.getAzimuth() - azimuth) * (windowHeightPx/Math.toRadians(DIAGONAL_FOV)));
                } else {
                    x = (int) Math.round((aircraft.getAzimuth() - azimuth) * (windowHeightPx / Math.toRadians(DIAGONAL_FOV)));
                }
                // TODO Anything needed for pitch here?
                int y = (int) Math.round((aircraft.getPitch() - pitchInv) * (windowWidthPx/Math.toRadians(DIAGONAL_FOV)));
                // Rotate point around origin according to roll
                int rotX = (int) Math.round(x*Math.cos(adjRoll) - y*Math.sin(adjRoll));
                int rotY = (int) Math.round(x*Math.sin(adjRoll) + y*Math.cos(adjRoll));
                // Shift origin back to bottom left of screen (In landscape)
                float screenX = Math.round(rotX + maxY/2);
                float screenY = Math.round(rotY + maxX/2);

                // If the plane falls outside of the screen draw on the edge to suggest the direction that it's in.
                // Map point to screen
                Log.i("Point mapped", "Rel Az:" + (aircraft.getAzimuth() - azimuth) + " X pos:" + rotX + " Rel Pitch:" + (aircraft.getPitch() - pitchInv) + " Y pos:"+rotY);
                //Log.i("Point mapped", "Rel Az:" + (aircraft.getAzimuth() - azimuth) + " X pos:" + screenX + " Rel Pitch:" + screenY + "Y pos"+);
                // post to the main handler
//                mainHandler.post(new Runnable() {
////                    @Override
////                    public void run() {
////                         ;
////                    }
////                });
              Matrix myMat = new Matrix();
              myMat.setTranslate(screenY, screenX);
              planeThing.setImageMatrix(myMat);
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