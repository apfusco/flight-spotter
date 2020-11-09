package com.example.blank;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends Activity implements SensorEventListener {
    // sensors
    private LocationManager locationManager;
    private LocationListener locationListener;
    private SensorManager sensorManager;
    private CameraManager cameraManager;
    private TextureView textureView;

    // camera components
    private String cameraId;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private Size imageDimension;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    CameraDevice.StateCallback stateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;


    // handler for thread communication
    private Handler mainHandler = new Handler();

    // UI components
    private View view;
    TextView x, y, z,lat,longi,alt,bThread;
    ImageView planeThing;
    private float [] mRotationMatrix;

    // Globals
    public static float[] mOrientation;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // system services
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
        }
        textureView = (TextureView) findViewById(R.id.textureView);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);


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
        }

        // start calculation thread in background
        calcThread runner = new calcThread();
        new Thread(runner).start();
    }

    private void createCameraPreview() {
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            }
            Surface surface = new Surface(texture);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder.addTarget(surface);
            }
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice == null) {
                        return;
                    }
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void updatePreview() {
        if(cameraDevice == null) {

        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void openCamera() {
        try {
            cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA,
                }, REQUEST_CAMERA_PERMISSION);
                return;
            }
            cameraManager.openCamera(cameraId, stateCallBack, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
//            Log.i("AYYYY","ROTATION VECTOR[0]:"+event.values[0]);
//            Log.i("AYYYY","ROTATION VECTOR[1]:"+event.values[1]);
//            Log.i("AYYYY","ROTATION VECTOR[2]:"+event.values[2]);
//            Log.i("AYYYY","ROTATION VECTOR[3]:"+event.values[3]);
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,
                    SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, mOrientation);
            float azimuth = mOrientation[0];
            float pitch = mOrientation[1];
            float roll = mOrientation[2];
            x.setText("Azimuth: " + Float.toString(azimuth));
            y.setText("Pitch:       " + Float.toString(pitch));
            z.setText("Roll:          " + Float.toString(roll));
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

    public void updateLocationInfo(Location location) {
        lat.setText("lat:        " + location.getLatitude());
        longi.setText("longi:   " + location.getLongitude());
        alt.setText("alt:       " + location.getAltitude());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);

        startBackgroundThread();
        if(textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        stopBackgroundThread();
        sensorManager.unregisterListener(this);
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // background thread that is always running and keeping track of time
    class calcThread implements Runnable {
        private long currentCount;
        private Matrix myMat = new Matrix();

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
                myMat.setTranslate(150, 1000);
                myMat.postRotate(currentCount*7, 675, 1000);

                // post to the main handler
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        bThread.setText("count: " + currentCount);
                        planeThing.setImageMatrix(myMat);
                    }
                });
            }
        }
    }
}