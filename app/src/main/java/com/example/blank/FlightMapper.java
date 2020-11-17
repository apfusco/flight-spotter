package com.example.blank;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class FlightMapper {
    // Google Pixel XL in 16:9 crop mode
    private final double VERTICAL_FOV = 66.9;
    private final double DIAGONAL_FOV = 74.32;
    private final double HORIZONTAL_FOV = 40.77;
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
//
//    }
    private void clearAllPlanes(){

    }

    private void drawPlaneToScreenLocation(){

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
