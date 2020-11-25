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
    private final double FIVE_MINS = 300000;
    private long lastChecked = 0;
    private AirTracker airTracker = new AirTracker();
    // Full 4:3
//    WidthDegrees	66.9°
//    HeightDegrees	51.95°
//    Diagonal Degrees	78.76°
    // Tells the main UI thread where to display icons for flights.
//    public void mapPlanesToScreen (Context context) {
//        // Get phone's orientation
//        float myAzimuth = MainActivity.mOrientation[0];
//        float myPitch = MainActivity.mOrientation[1];
//        float myRoll = MainActivity.mOrientation[2];

        // Determine the center of the phone's fov in our spherical coordinate system and the bounds
        // of a square around it the size of the diagonal fov of the phone. This gives us all the
        // angles we might be able to see for a specific heading and polar angle of the phone before
        // we actually account for the roll of the phone.
//        double azBound1 = ((myAzimuth - Math.toRadians(DIAGONAL_FOV)/2) % (Math.PI*2) + (2*Math.PI)) % (2*Math.PI);
//        double azBound2 = (myAzimuth + Math.toRadians(DIAGONAL_FOV)/2) % (Math.PI*2);
//        double thetaBound1 = myPitch - Math.toRadians(DIAGONAL_FOV)/2;
//        double thetaBound2 = myPitch + Math.toRadians(DIAGONAL_FOV)/2;
//        if ((thetaBound1 < 0 && thetaBound2 > 0) ||
//                (thetaBound1 < Math.PI && thetaBound2 > Math.PI)) {
//            // Don't do anything if we are looking at the poles. TODO remove this and account for
//            //  that case.
//            return;
//        }
//
//        AirTracker airTracker = new AirTracker();
//
//        // Query all flights in a square the size of the diagonal fov.
//        ArrayList<Aircraft> visibleAircraft = airTracker
//                .getAircraftInWindow(azBound1,thetaBound1,azBound2,thetaBound2);
//
//        // Do a 2-D mapping of angles of flights relative to angle of phone to positions in a 2-D
//        // square of positions on the screen.
//        WindowManager windowManager = (WindowManager) context
//                .getSystemService(Context.WINDOW_SERVICE);
//        DisplayMetrics metrics = new DisplayMetrics();
//        windowManager.getDefaultDisplay().getMetrics(metrics);
//        float maxX = metrics.widthPixels;
//        float maxY = metrics.heightPixels;
//        float windowWidthPx = Math.round(maxX * (DIAGONAL_FOV/HORIZONTAL_FOV));
//        float windowHeightPx = Math.round(maxY * (DIAGONAL_FOV/VERTICAL_FOV));
//
//        // Get relative angles of flights from center of screen and transform from relative angle to
//        // x,y pixel coordinates with the center of the screen being the origin.
//        ArrayList<Point> points = new ArrayList<Point>();
//        for (Aircraft aircraft: visibleAircraft) {
//            Point newPoint = new Point();
//            // Find relative angle and convert from radians to pixels
//            int x = (int) Math.round((myPhi - aircraft.getPhi()) * (windowHeightPx/Math.toRadians(DIAGONAL_FOV)));
//            int y = (int) Math.round((myTheta - aircraft.getTheta()) * (windowWidthPx/Math.toRadians(DIAGONAL_FOV)));
//            // Rotate point around origin according to roll
//            int rotX = (int) Math.round(x*Math.cos(myAdjustRoll) - y*Math.sin(myAdjustRoll));
//            int rotY = (int) Math.round(x*Math.sin(myAdjustRoll) + y*Math.cos(myAdjustRoll));
//            newPoint.x = rotX;
//            newPoint.y = rotY;
//            // Shift origin back to top left of screen
//            points.add(newPoint);
//        }
//
//        // Tell UI thread to draw planes to the screen and attach a key to each for flight data
//        // retrieval
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void run () {
        while (true) {
            // Query every 5 minutes so your IP doesn't get banned by the API
            long currTime = Calendar.getInstance().getTimeInMillis();
            if (currTime > lastChecked + FIVE_MINS) {
                lastChecked = currTime;
                airTracker.reloadLocations(MainActivity.mLocation.getLongitude(), MainActivity.mLocation.getLatitude(), (float) MainActivity.mLocation.getAltitude());
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

                Log.i("Az: ", Float.toString(azimuth));
                Log.i("Pitch_inv: ", Float.toString(pitchInv));
                Log.i("Adjusted Roll:", Float.toString(adjRoll));
                Log.i("Az Window: ", Float.toString((float) azBound1) + " " + Float.toString((float) azBound2));
                Log.i("Pitch Window: ", Float.toString((float) pitchBound1) + " " + Float.toString((float) pitchBound2));


                // Query all flights in a square the size of the diagonal fov.
                // airTracker.
                ArrayList<Aircraft> visibleAircraft = airTracker
                        .getAircraftInWindow(azBound1, pitchBound1, azBound2, pitchBound2);
                // Do a 2-D mapping of angles of flights relative to angle of phone to positions in a 2-D
                // square of positions on the screen.
                WindowManager windowManager = (WindowManager) MainActivity.mContext
                        .getSystemService(Context.WINDOW_SERVICE);
                DisplayMetrics metrics = new DisplayMetrics();
                windowManager.getDefaultDisplay().getMetrics(metrics);
                float maxX = metrics.widthPixels;
                float maxY = metrics.heightPixels;

                float windowWidthPx = Math.round(maxX * (DIAGONAL_FOV / HORIZONTAL_FOV));
                float windowHeightPx = Math.round(maxY * (DIAGONAL_FOV / VERTICAL_FOV));

                Log.i("Screen Metrics", " maxX:" + maxX + " maxY:" + maxY + " windowWidthPx:" + windowWidthPx + " windowHeightPx:" + windowHeightPx);

                // Get relative angles of flights from center of screen and transform from relative angle to
                // x,y pixel coordinates with the center of the screen being the origin.
                int count = 0;
                for (Aircraft aircraft : visibleAircraft) {
                    double aircraftAzimuth = aircraft.getAzimuth();
                    double aircraftPitch = aircraft.getPitch();
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

                    // TODO If the plane falls outside of the screen draw on the edge to suggest the direction that it's in.
                    // Map point to screen
                    Log.i("Point mapped", "Rel Az:" + (aircraft.getAzimuth() - azimuth) + " X pos:" + rotX + " Rel Pitch:" + (aircraft.getPitch() - pitchInv) + " Y pos:" + rotY);
                    drawPlaneToScreenLocation(screenX, screenY, adjRoll, count);
                    count++;
                    if (count == 20) {
                        break;
                    }
                }
                clearUnusedPlanes(count);
            }
        }
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
    private void drawPlaneToScreenLocation(float screenX, float screenY, float adjRollRadians, final int count){
        final Matrix myMat = new Matrix();
        myMat.setTranslate(screenY, screenX);
        myMat.postRotate((float) Math.toDegrees(-adjRollRadians), screenY, screenX);
        // Tell UI thread to draw ImageView to Screen
        MainActivity.mPlaneIcons[count].post(new Runnable() {
             @Override
             public void run() {
                 MainActivity.mPlaneIcons[count].setVisibility(View.VISIBLE);
                 MainActivity.mPlaneIcons[count].setImageMatrix(myMat);
             }
         });

    }
    //Log.i("Point mapped", "Rel Az:" + (aircraft.getAzimuth() - azimuth) + " X pos:" + screenX + " Rel Pitch:" + screenY + "Y pos"+);
    // post to the main handler
    //                mainHandler.post(new Runnable() {
    ////                    @Override
    ////                    public void run() {
    ////                         ;
    ////                    }
    ////                });
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
