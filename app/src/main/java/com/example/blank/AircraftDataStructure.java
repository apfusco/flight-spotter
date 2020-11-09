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
        int minPhiIndex = (int)Math.round(Math.floor(minAzimuth / 2 / Math.PI * ARRAY_LENGTH)) % ARRAY_LENGTH;
        int minThetaIndex = (int)Math.round(Math.floor(minPitch / Math.PI * ARRAY_LENGTH)) % ARRAY_LENGTH;
        int maxPhiIndex = (int)Math.round(Math.floor(maxAzimuth / 2 / Math.PI * ARRAY_LENGTH)) % ARRAY_LENGTH;
        int maxThetaIndex = (int)Math.round(Math.floor(maxPitch / Math.PI * ARRAY_LENGTH)) % ARRAY_LENGTH;

        ArrayList<Aircraft> inWindow = new ArrayList<Aircraft>();

        for (int phi = minPhiIndex; phi <= maxPhiIndex; phi++) {
            for (int theta = minThetaIndex; theta <= maxThetaIndex; theta++) {
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
        for (int azimuth = 0; azimuth < ARRAY_LENGTH; azimuth++) {
            for (int pitch = 0; pitch < ARRAY_LENGTH; pitch++) {
                for (int i = 0; i < this.mAircraftBuckets[azimuth][pitch].size(); i++) {
                    Aircraft aircraft = this.mAircraftBuckets[azimuth][pitch].get(i);
                    aircraft.updateSphericalPosition(posLon, posLat, posAlt);
                    if ((aircraft.getAzimuthIndex() != azimuth)
                            || (aircraft.getPitchIndex() != pitch)) {
                        // Move aircraft
                        this.mAircraftBuckets[aircraft.getAzimuthIndex()][aircraft.getPitchIndex()]
                                .add(aircraft);
                        this.mAircraftBuckets[azimuth][pitch].remove(i--);
                    }
                }
            }
        }
    }
}
