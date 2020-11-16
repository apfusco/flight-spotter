package com.example.blank;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class AircraftDataStructure {

    public static final int ARRAY_LENGTH = 8;

    private ArrayList<Aircraft>[][] mAircraftBuckets;

    public AircraftDataStructure() {
        this.mAircraftBuckets = new ArrayList[ARRAY_LENGTH][ARRAY_LENGTH];
        for (int i = 0; i < ARRAY_LENGTH; i++) {
            for (int j = 0; j < ARRAY_LENGTH; j++) {
                this.mAircraftBuckets[i][j] = new ArrayList<>();
            }
        }
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

        for (int phi = minAzimuthIndex; phi <= maxAzimuthIndex; phi++) {
            for (int theta = minPitchIndex; theta <= maxPitchIndex; theta++) {
                for (int i = 0; i < this.mAircraftBuckets[phi][theta].size(); i++) {
                    Aircraft aircraft = this.mAircraftBuckets[phi][theta].get(i);
                    if ((aircraft.getAzimuth() >= minAzimuth)
                            && (aircraft.getAzimuth() <= maxAzimuth)
                            && (aircraft.getPitch() >= minPitch)
                            && (aircraft.getPitch() <= maxPitch))
                        inWindow.add(this.mAircraftBuckets[phi][theta].get(i));
                }
            }
        }

        return inWindow;
    }

    public void updateLocations(float posLon, float posLat, float posAlt) {
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
}
