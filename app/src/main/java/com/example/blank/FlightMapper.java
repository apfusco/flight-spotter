package com.example.blank;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class FlightMapper implements Runnable{
    public static boolean newOrientation = false;
    // Google Pixel XL in 16:9 crop mode
    private final double VERTICAL_FOV = 66.9;
    private final double DIAGONAL_FOV = 74.32;
    private final double HORIZONTAL_FOV = 40.77;
    private final double TWO_MINS = 120000;
    private final double ONE_SEC = 1000;
    private long lastChecked = 0;
    private long lastUpdate = 0;
    private boolean once = false;
    private int maxX = 0;
    private int maxY = 0;
    private float windowWidthPx = 0;
    private float windowHeightPx = 0;

    private AirTracker airTracker = new AirTracker();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void run () {
        while (true) {
            // Query every 20 sec so your IP doesn't get banned by the API
            long currTime = Calendar.getInstance().getTimeInMillis();
            if (currTime > lastChecked + ONE_SEC*20) {
                lastChecked = currTime;
                airTracker.reloadLocations(MainActivity.mLocation.getLongitude(), MainActivity.mLocation.getLatitude(), (float) MainActivity.mLocation.getAltitude());
            }

            if (currTime > lastUpdate + ONE_SEC) {
                lastChecked = currTime;
                airTracker.updateLocations();
            }

            if (newOrientation) {
                newOrientation = false;
                // Get phone's orientation
                float azimuth = MainActivity.mOrientation[0];
                float pitch = MainActivity.mOrientation[1];
                float roll = MainActivity.mOrientation[2];
                float pitchInv = -pitch; // Invert to tell us what the camera angle is not the screen.
                float adjRoll = adjustRoll(roll);
                // TODO: fix this so that the window width doesn't decrease when pitch increases etc. How to compensate? Rotate so pitch = 0 straight up?
                double azBound1 = (((azimuth - Math.toRadians(DIAGONAL_FOV) / 2 + Math.PI) % (Math.PI * 2) + (2 * Math.PI)) % (2 * Math.PI)) - Math.PI;
                double azBound2 = (azimuth + Math.toRadians(DIAGONAL_FOV) / 2 + Math.PI) % (Math.PI * 2) - Math.PI;
                double pitchBound1 = (((pitchInv - Math.toRadians(DIAGONAL_FOV) / 2 + Math.PI / 2) % (Math.PI) + (Math.PI)) % (Math.PI)) - Math.PI / 2;
                ;
                double pitchBound2 = (pitchInv + Math.toRadians(DIAGONAL_FOV) / 2 + Math.PI / 2) % (Math.PI) - Math.PI / 2;
                if (pitchBound1 > 0 && pitchInv < 0) {
                    pitchBound1 = -pitchBound1;
                }
                if (pitchBound2 < 0 && pitchInv > 0) {
                    pitchBound2 = -pitchBound2;
                }
                // TODO Handle north pole case

                Log.v("Az: ", Float.toString(azimuth));
                Log.v("Pitch_inv: ", Float.toString(pitchInv));
                Log.v("Adjusted Roll:", Float.toString(adjRoll));
                Log.v("Az Window: ", Float.toString((float) azBound1) + " " + Float.toString((float) azBound2));
                Log.v("Pitch Window: ", Float.toString((float) pitchBound1) + " " + Float.toString((float) pitchBound2));

                // Query all flights in a square the size of the diagonal fov.
                // airTracker.
                final ArrayList<Aircraft> visibleAircraft = airTracker
                        .getAircraftInWindow(azBound1, pitchBound1, azBound2, pitchBound2);
                // Do a 2-D mapping of angles of flights relative to angle of phone to positions in a 2-D
                // square of positions on the screen.
                if (!once) {
                    once = true;
                    WindowManager windowManager = (WindowManager) MainActivity.mContext
                            .getSystemService(Context.WINDOW_SERVICE);
                    DisplayMetrics metrics = new DisplayMetrics();
                    windowManager.getDefaultDisplay().getMetrics(metrics);
                    maxX = metrics.widthPixels;
                    maxY = metrics.heightPixels;

                    windowWidthPx = Math.round(maxX * (DIAGONAL_FOV / HORIZONTAL_FOV));
                    windowHeightPx = Math.round(maxY * (DIAGONAL_FOV / VERTICAL_FOV));
                }

                //Log.v("Mapper", " maxX:" + maxX + " maxY:" + maxY + " windowWidthPx:" + windowWidthPx + " windowHeightPx:" + windowHeightPx);
                Log.v("Mapper", " Planes in window:" + visibleAircraft.size());
                // Get relative angles of flights from center of screen and transform from relative angle to
                // x,y pixel coordinates with the center of the screen being the origin.
                int count = 0;
                int closestPlane = -1;
                Aircraft closestAircraft = null;
                float minDist = 100000000;
                for (Aircraft aircraft : visibleAircraft) {
                    double aircraftAzimuth = aircraft.getAzimuth();
                    double aircraftPitch = aircraft.getPitch();

                    // Don't show planes that are essentially at or below the horizon
                    if (aircraftPitch < .1) {
                        Log.v("Mapper", "Skipped low plane pitch:" + aircraftPitch);
                        continue;
                    }
                    // Find relative "angular position" and convert from radians to pixels
                    // Handle cases where the azimuths are on opposite sides of the south direction
                    int x = 0;
                    if (aircraft.getAzimuth() - azimuth > Math.toRadians(DIAGONAL_FOV)) {
                        x = (int) Math.round((2 * Math.PI - aircraft.getAzimuth() - azimuth) * (windowHeightPx / Math.toRadians(DIAGONAL_FOV)));
                    } else if (aircraft.getAzimuth() - azimuth < -Math.toRadians(DIAGONAL_FOV)) {
                        x = (int) Math.round((2 * Math.PI + aircraft.getAzimuth() - azimuth) * (windowHeightPx / Math.toRadians(DIAGONAL_FOV)));
                    } else {
                        x = (int) Math.round((aircraft.getAzimuth() - azimuth) * (windowHeightPx / Math.toRadians(DIAGONAL_FOV)));
                    }
                    // TODO Anything needed for pitch here?
                    int y = (int) Math.round((aircraft.getPitch() - pitchInv) * (windowWidthPx / Math.toRadians(DIAGONAL_FOV)));
                    // Rotate point around origin according to roll
                    int rotX = (int) Math.round(x * Math.cos(adjRoll) - y * Math.sin(adjRoll));
                    int rotY = (int) Math.round(x * Math.sin(adjRoll) + y * Math.cos(adjRoll));
                    // Shift origin back to bottom left of screen (In landscape)
                    float screenX = Math.round(rotX + maxY / 2);
                    float screenY = Math.round(rotY + maxX / 2);

                    Log.v("Point mapped", "Rel Az:" + (aircraft.getAzimuth() - azimuth) + " X pos:" + rotX + " Rel Pitch:" + (aircraft.getPitch() - pitchInv) + " Y pos:" + rotY);
                    float relDist = (float) Math.sqrt(Math.pow(aircraft.getAzimuth() - azimuth,2) + Math.pow(aircraft.getPitch() - pitchInv,2));
                    if (relDist < minDist) {
                        minDist = relDist;
                        closestPlane = count;
                        closestAircraft = aircraft;
                    }
                    // Map point to screen
                    drawPlaneToScreenLocation(screenX, screenY, adjRoll, count, aircraft.getmIcao24(), aircraft.getScreenDirection());
                    count++;
                    if (count == 20) {
                        break;
                    }
                }
                Log.v("Mapper", " Count:" + count);
                clearUnusedPlanes(count);
                final int asdfa = closestPlane;
                final  int adfasdfa = count;
                final Aircraft adsfohapug4 = closestAircraft;
                if (closestPlane != -1) {
                    // Redraw closest plane to middle on top for clicking purposes
                    MainActivity.mPlaneIcons[closestPlane].post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.mPlaneIcons[asdfa].setElevation(1);
                            for (int i =0;i < adfasdfa;i++){
                                if (i == asdfa) {
                                    continue;
                                }
                                MainActivity.mPlaneIcons[i].setImageResource(R.drawable.plane_icon);
                                MainActivity.mPlaneIcons[i].setElevation(0);
                            }
                            MainActivity.mPlaneIcons[asdfa].setImageResource(R.drawable.center_plane_icon);
                            MainActivity.mPlaneIcons[asdfa].setElevation(1);
                            MainActivity.callsignIndicator.setText(adsfohapug4.getCallsign());
                        }
                    });
                } else {
                    MainActivity.mPlaneIcons[0].post(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.callsignIndicator.setText("            ");
                        }
                    });
                }
            }
        }
    }
    public AirTracker getAirTracker(){
        return airTracker;
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void clearUnusedPlanes(int count){
        // Clear all plane ImageViews from the screen
        for (int i = 20;i > count;i--){
            final int num = i;
            MainActivity.mPlaneIcons[count].post(new Runnable() {
                @Override
                public void run() {
                    MainActivity.mPlaneIcons[num - 1].setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void drawPlaneToScreenLocation(float screenX, float screenY, float adjRollRadians, final int count, final int icao24, double screenDirection){
        final Matrix myMat = new Matrix();
        myMat.setTranslate(screenY, screenX);
        myMat.postRotate((float) Math.toDegrees(-adjRollRadians), screenY, screenX);
        // Tell UI thread to draw ImageView to Screen
        MainActivity.mPlaneIcons[count].post(new Runnable() {
             @Override
             public void run() {
                 MainActivity.mPlaneIcons[count].setVisibility(View.VISIBLE);
                 MainActivity.mPlaneIcons[count].setImageMatrix(myMat);
                 MainActivity.mPlaneIcons[count].setId(icao24);
             }
         });

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
