package com.example.blank;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

public class AircraftDataStructure {

    public static final int ARRAY_LENGTH = 8;

    private ArrayList<Aircraft>[][] mAircraftBuckets;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AircraftDataStructure() {
        this.mAircraftBuckets = new ArrayList[ARRAY_LENGTH][ARRAY_LENGTH];
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            for (int j = 0; j < ARRAY_LENGTH; j++) {
                this.mAircraftBuckets[i][j] = new ArrayList<>();
            }
        }
        // FIXME: This was added for testing orientation stuff
        float latitude = (float)43.117686;
        float longitude = (float)-88.293514;
        int time = Calendar.getInstance().getTime().getSeconds();
        Aircraft testAircraft = new Aircraft(
                time,
                0x696969, // Nice
                "XES",
                "Kazakhstan",
                time,
                time,
                longitude,
                latitude,
                 3000,
                false,
                0,
                0,
                0,
                null,
                0,
                "Borat",
                false,
                0);
        testAircraft.updateSphericalPosition(MainActivity.mLocation.getLongitude(), MainActivity.mLocation.getLatitude(),
                MainActivity.mLocation.getAltitude());
        addAircraft(testAircraft);
    }

    public void addAircraft(Aircraft aircraft) {
        this.mAircraftBuckets[aircraft.getAzimuthIndex()][aircraft.getPitchIndex()].add(aircraft);
    }

    public ArrayList<Aircraft> getAircraftInWindow(double minAzimuth,
                                                   double minPitch,
                                                   double maxAzimuth,
                                                   double maxPitch) {

        // Calculate indices to use
        int minAzimuthIndex;
        if (minAzimuth <= -Math.PI + 0.01)
            minAzimuthIndex = 0;
        else
            minAzimuthIndex = (int)Math.round(Math.floor(minAzimuth / 2 / Math.PI
                    * AircraftDataStructure.ARRAY_LENGTH)) % AircraftDataStructure.ARRAY_LENGTH
                    + AircraftDataStructure.ARRAY_LENGTH / 2;

        int minPitchIndex;
        if (minPitch <= -Math.PI / 2 + 0.01)
            minPitchIndex = 0;
        else
            minPitchIndex = (int)Math.round(Math.floor(minPitch / Math.PI
                    * AircraftDataStructure.ARRAY_LENGTH)) % AircraftDataStructure.ARRAY_LENGTH
                    + AircraftDataStructure.ARRAY_LENGTH / 2;

        int maxAzimuthIndex;
        if (maxAzimuth >= Math.PI - 0.01)
            maxAzimuthIndex = AircraftDataStructure.ARRAY_LENGTH - 1;
        else
            maxAzimuthIndex = (int)Math.round(Math.floor(maxAzimuth / 2 / Math.PI
                    * AircraftDataStructure.ARRAY_LENGTH)) % AircraftDataStructure.ARRAY_LENGTH
                    + AircraftDataStructure.ARRAY_LENGTH / 2;

        int maxPitchIndex;
        if (maxPitch >= Math.PI / 2 - 0.01)
            maxPitchIndex = AircraftDataStructure.ARRAY_LENGTH - 1;
        else
            maxPitchIndex = (int)Math.round(Math.floor(maxPitch / Math.PI
                    * AircraftDataStructure.ARRAY_LENGTH)) % AircraftDataStructure.ARRAY_LENGTH
                    + AircraftDataStructure.ARRAY_LENGTH / 2;

        ArrayList<Aircraft> inWindow = new ArrayList<Aircraft>();

        System.out.println("maxPitchIndex: " + Integer.toString(maxPitchIndex) + " minPitchIndex: "
                + Integer.toString(minPitchIndex) + " maxAzimuthIndex: "
                + Integer.toString(maxAzimuthIndex) + " minAzimuthIndex: "
                + Integer.toString(minAzimuthIndex));

        for (int azIndex = minAzimuthIndex; (azIndex <= maxAzimuthIndex)
                || ((maxAzimuthIndex < minAzimuthIndex) && (azIndex >= minAzimuthIndex));
             azIndex++) {
            azIndex %= ARRAY_LENGTH;
            for (int pitIndex = minPitchIndex; pitIndex <= maxPitchIndex; pitIndex++) {
                for (int i = 0; i < this.mAircraftBuckets[azIndex][pitIndex].size(); i++) {
                    Aircraft aircraft = this.mAircraftBuckets[azIndex][pitIndex].get(i);
                    System.out.println("azIndex: " + Integer.toString(azIndex) + " pitIndex: "
                            + Integer.toString(pitIndex));
                    if (maxAzimuthIndex < minAzimuthIndex) {
                        if (((aircraft.getAzimuth() >= minAzimuth)
                                || (aircraft.getAzimuth() <= maxAzimuth))
                                && (aircraft.getPitch() >= minPitch)
                                && (aircraft.getPitch() <= maxPitch)) {
                            inWindow.add(this.mAircraftBuckets[azIndex][pitIndex].get(i));
                        }
                    } else if ((aircraft.getAzimuth() >= minAzimuth)
                                && (aircraft.getAzimuth() <= maxAzimuth)
                                && (aircraft.getPitch() >= minPitch)
                                && (aircraft.getPitch() <= maxPitch)) {
                        inWindow.add(this.mAircraftBuckets[azIndex][pitIndex].get(i));
                    }
                }
            }
        }

        return inWindow;
    }


    public void updateLocations(double posLon, double posLat, double posAlt) {

        int test = 0;

        for (int azIndex = 0; azIndex < ARRAY_LENGTH; azIndex++) {
            for (int pitIndex = 0; pitIndex < ARRAY_LENGTH; pitIndex++) {
                for (int i = 0; i < this.mAircraftBuckets[azIndex][pitIndex].size(); i++) {
                    Aircraft aircraft = this.mAircraftBuckets[azIndex][pitIndex].get(i);
                    aircraft.updateSphericalPosition(posLon, posLat, posAlt);
                    if ((aircraft.getAzimuthIndex() != azIndex)
                            || (aircraft.getPitchIndex() != pitIndex)) {
                        // Move aircraft
                        this.mAircraftBuckets[aircraft.getAzimuthIndex()][aircraft.getPitchIndex()]
                                .add(aircraft);
                        this.mAircraftBuckets[azIndex][pitIndex].remove(i--);

                    }
                }
            }
        }
    }

    public void clearAircraft() {
        for (int i = 0; i < this.mAircraftBuckets.length; i++) {
            for (int j = 0; j < this.mAircraftBuckets[i].length; j++) {
                this.mAircraftBuckets[i][j].clear();
            }
        }
    }
}
