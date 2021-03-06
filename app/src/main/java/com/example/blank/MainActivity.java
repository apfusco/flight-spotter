package com.example.blank;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.VectorDrawable;
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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements SensorEventListener, OnMapReadyCallback {
    public static boolean updating;
    // Google Pixel XL in 16:9 crop mode
    private final double VERTICAL_FOV = 66.9;
    private final double DIAGONAL_FOV = 74.32;
    private final double HORIZONTAL_FOV = 40.77;

    // sensors
    private LocationManager locationManager;
    private LocationListener locationListener;
    private SensorManager sensorManager;
    private CameraManager cameraManager;
    private TextureView textureView;

    // camera components
    private Bitmap person;
    private Bitmap plane;
    private String cameraId;
    private CameraDevice cameraDevice;
    private ImageReader imageReader;
    private Size imageDimension;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    CameraDevice.StateCallback stateCallBack = new CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
    private FlightMapper mapper;
    private View decorView;
    private boolean decorFlag = false;

    // handler for thread communication
    private Handler mainHandler = new Handler();

    // UI components
    private View view;
    private float [] mRotationMatrix;
    private TextView depCity, depAirport, arrCity, arrAirport, callsign, aircraftType, altitude, velocity, heading, longitude, latitude;
    private ImageView image;
    private FloatingActionButton fab_main;
    private Animation fab_open, fab_close, fab_clock, fab_anticlock;
    private Boolean isOpen = false;
    private FrameLayout dataFrame;
    private ImageView exitButton;
    private boolean exited = true;
    private Aircraft currAircraft;

    GoogleMap googleMap;
    SupportMapFragment mapFragment;
    Boolean MapUp = false;
    private long lastChecked;

    // Globals
    public static TextView callsignIndicator;
    public static ImageView [] mPlaneIcons = new ImageView[20];
    public static float[] mOrientation;
    public static Location mLocation;
    public static Context mContext;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
            mapFragment.getView().setVisibility(View.GONE);
        }

        callsignIndicator = findViewById(R.id.callsignIndicator);
        fab_main = findViewById(R.id.fab);
        fab_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // open map fragment
                if (MapUp) {
                    mapFragment.getView().setVisibility(View.GONE);
                    MapUp = false;
                } else {
                    mapFragment.getView().setVisibility(View.VISIBLE);
                    MapUp = true;
                }
            }
        });
        //fab_main.hide(); // hide button on boot, should just be camera view to user until clicked

        // keep 16:9 ratio by removing to bar
        decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {

                        if (decorFlag && (!isOpen || !MapUp)) {
                            new CountDownTimer(5000, 1000) {
                                @Override
                                public void onTick(long l) {}
                                public void onFinish() {
                                    // When timer is finished
                                    // Execute your code here
                                    if (isOpen || MapUp) {
                                        // restart timer
                                        //fab_main.setVisibility(View.VISIBLE);
                                        fab_main.show();
                                        this.start();
                                    } else {
                                        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                                        decorView.setSystemUiVisibility(uiOptions);
                                        //fab_main.setVisibility(View.GONE);
                                        fab_main.hide();
                                    }
                                }
                            }.start();
                            fab_main.show();
                        }
                        decorFlag = !decorFlag;
                    }
                });

        // Flight data fields
        image = findViewById(R.id.image);
        depCity = findViewById(R.id.depCity);
        depAirport = findViewById(R.id.depAirport);
        arrCity = findViewById(R.id.arrCity);
        arrAirport = findViewById(R.id.arrAirport);
        callsign = findViewById(R.id.callsign);
        aircraftType = findViewById(R.id.aircraftType);
        altitude = findViewById(R.id.altitude);
        velocity = findViewById(R.id.velocity);
        heading = findViewById(R.id.heading);
        longitude = findViewById(R.id.longitude);
        latitude = findViewById(R.id.latitude);
        // Hide flight data for now
        dataFrame = findViewById(R.id.dataFrame);
        exitButton = findViewById(R.id.dataExit);
        dataFrame.setVisibility(View.INVISIBLE);
        exitButton.setVisibility(View.INVISIBLE);
        exitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // set stop to false and spawn new counter thread and
                exitDialog(v);
            }
        });
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
        mContext = getApplicationContext();
        // Set up imageviews to use for planes
        FrameLayout main = (FrameLayout)findViewById(R.id.frameLayout);
        for (int i =0; i<20;i++) {
            mPlaneIcons[i] = new ImageView(mContext);
            mPlaneIcons[i].setImageResource(R.drawable.plane_icon);
            mPlaneIcons[i].setVisibility(View.INVISIBLE);
            mPlaneIcons[i].setAdjustViewBounds(true);
            mPlaneIcons[i].setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mPlaneIcons[i].setScaleType(ImageView.ScaleType.MATRIX);
            mPlaneIcons[i].setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // set stop to false and spawn new counter thread and
                    startDialog(v);
                }
            });
            main.addView(mPlaneIcons[i]);
        }


        // register sensor manager
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL);

        // create new location listener
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
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
            mLocation = location;
        }

        // Start flight mapping thread in the background once we get location
        while (mLocation == null) {}
        mapper = new FlightMapper();
        new Thread(mapper).start();

        // start flight data update thread in background
        calcThread runner = new calcThread();
        new Thread(runner).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
            return;
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
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[2];
            Log.i("Height: ", String.valueOf(imageDimension.getHeight()));
            Log.i("Width: ", String.valueOf(imageDimension.getWidth()));

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
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && mLocation != null) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,
                    SensorManager.AXIS_Z, mRotationMatrix);
            SensorManager.getOrientation(mRotationMatrix, mOrientation);
            FlightMapper.newOrientation = true;
        }
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

        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onPause() {
        // unregister listeners
        super.onPause();
        stopBackgroundThread();
        sensorManager.unregisterListener(this);
        // TODO pause the flightmapper
        // FIXME will need to update the onPause and onResume functions because Opening a new dialog
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
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

    public void startDialog(View v) {
        // Populate scroll view
        int icao24 = v.getId();
        ArrayList<Aircraft> aircrafts = mapper.getAirTracker().getAllAircraft();
        currAircraft = aircrafts.get(0);
        for (Aircraft aircraft1:aircrafts) {
            if (aircraft1.getmIcao24() == icao24) {
                currAircraft = aircraft1;
                break;
            }
        }
        // Update static fields
        if (currAircraft.getImageBitmap() == null) {
            mapper.update = currAircraft;
        }
        callsign.setText("Callsign: " + currAircraft.getCallsign());
        // TODO aircraftType.setText();
        // Allow thread to update dynamic fields
        exited  = false;
        // Reveal scroll view and exit button
        dataFrame.setVisibility(View.VISIBLE);
        exitButton.setVisibility(View.VISIBLE);
    }

    private void exitDialog(View v) {
        exited = true;
        dataFrame.setVisibility(View.INVISIBLE);
        exitButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        plane = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.air_plane_airport_2), 70, 70, false);
        person = BitmapFactory.decodeResource(this.getResources(), R.drawable.person);
        map.addMarker(new MarkerOptions().position(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()))
                        .title("You")
        );
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 9));
        googleMap = map;
    }


    // background thread that is always running and keeping track of time
    class calcThread implements Runnable {
        // main method for runnable
        @Override
        public void run() {
            // when spawned always run in background
            while (true) {
                if (!exited) {
                    mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (currAircraft.getImageBitmap() != null) {
                                    image.setImageBitmap(currAircraft.getImageBitmap());
                                } else {
                                    image.setImageResource(R.drawable.no_image_icon_6);
                                }
                                if (currAircraft.getEstDepartureAirportName() != null) {
                                    depCity.setText(currAircraft.getEstDepartureAirportName());
                                } else {
                                    depCity.setText("<City>");
                                }
                                if (currAircraft.getEstDepartureAirport() != null) {
                                    depAirport.setText(currAircraft.getEstDepartureAirport());
                                } else {
                                    depAirport.setText("<Airport>");
                                }
                                //arrCity.setText(currAircraft.getEstArrivalAirportName());
                                //arrAirport.setText(currAircraft.getEstArrivalAirport());
                                altitude.setText("Altitude: " + String.valueOf(currAircraft.getAltitude()) + "m");
                                velocity.setText("Velocity: " + String.valueOf(currAircraft.getVelocity()) + "m/s");
                                heading.setText("Heading: " + String.valueOf(currAircraft.getHeading()) + "°");
                                latitude.setText("Latitude: " + String.valueOf(currAircraft.getLocation()[0]) + "°");
                                longitude.setText("Longitude: " + String.valueOf(currAircraft.getLocation()[1]) + "°");
                            }
                    });
                }
                if (!MapUp) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            googleMap.clear();
                            googleMap.addMarker(new MarkerOptions().position(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()))
                                            .title("You").icon(BitmapDescriptorFactory.fromBitmap(person))
                            );
                            ArrayList<Aircraft> planes = mapper.getAirTracker().getAllAircraft();
                            for (Aircraft aircraft : planes) {
                                if (aircraft.getPitch() > .1) {
                                    float[] vals = aircraft.getLocation();
                                    googleMap.addMarker(new MarkerOptions().position(new LatLng(vals[0], vals[1]))
                                                    .title(aircraft.getCallsign()).icon(BitmapDescriptorFactory.fromBitmap(plane))
                                    );
                                }
                            }
                        }
                    });
                }
                // Query every 1/2 sec
                SystemClock.sleep(500);
            }
        }
    }
}