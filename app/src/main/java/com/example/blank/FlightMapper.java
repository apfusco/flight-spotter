package com.example.blank;

public class FlightMapper {
    // Tells the main UI thread where to display icons for flights.
    private void mapPlanesToScreen () {
        float azimuth = MainActivity.mOrientation[0];
        float pitch = MainActivity.mOrientation[1];
        float roll = MainActivity.mOrientation[2];

        // Determine the center of the phone's fov in our spherical coordinate system and the bounds
        // of a square around it the size of the diagonal fov of the phone. This gives us all the
        // angles we might be able to see for a specific heading and polar angle of the phone.


        // Query all flights in a square the size of the diagonal fov.


        // Do a 2-D mapping of angles of flights relative to angle of phone to positions in a 2-D
        // square of positions on the screen


        // Rotate the points around the center of the mapping equivalent to the rotation of the
        // phone


        // Tell UI thread to draw planes to the screen and attach a key to each for flight data
        // retrieval

    }
}
