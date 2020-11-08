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
        this.mAircraftBuckets[aircraft.getPhiIndex()][aircraft.getThetaIndex()].add(aircraft);
    }

    public ArrayList<Aircraft> getAircraftInWindow(double minPhi,
                                                   double minTheta,
                                                   double maxPhi,
                                                   double maxTheta) {

        // Calculate indices to use
        int minPhiIndex = (int)Math.round(Math.floor(minPhi / 2 / Math.PI * ARRAY_LENGTH)) % ARRAY_LENGTH;
        int minThetaIndex = (int)Math.round(Math.floor(minTheta / Math.PI * ARRAY_LENGTH)) % ARRAY_LENGTH;
        int maxPhiIndex = (int)Math.round(Math.floor(maxPhi / 2 / Math.PI * ARRAY_LENGTH)) % ARRAY_LENGTH;
        int maxThetaIndex = (int)Math.round(Math.floor(maxTheta / Math.PI * ARRAY_LENGTH)) % ARRAY_LENGTH;

        ArrayList<Aircraft> inWindow = new ArrayList<Aircraft>();

        for (int phi = minPhiIndex; phi <= maxPhiIndex; phi++) {
            for (int theta = minThetaIndex; theta <= maxThetaIndex; theta++) {
                for (int i = 0; i < this.mAircraftBuckets[phi][theta].size(); i++) {
                    Aircraft aircraft = this.mAircraftBuckets[phi][theta].get(i);
                    if ((aircraft.getPhi() >= minPhi) && (aircraft.getPhi() <= maxPhi)
                            && (aircraft.getTheta() >= minTheta)
                            && (aircraft.getTheta() <= maxTheta))
                        inWindow.add(this.mAircraftBuckets[phi][theta].get(i));
                }
            }
        }

        return inWindow;
    }

    public void updateLocations(float posLon, float posLat, float posAlt) {
        for (int phi = 0; phi < ARRAY_LENGTH; phi++) {
            for (int theta = 0; theta < ARRAY_LENGTH; theta++) {
                for (int i = 0; i < this.mAircraftBuckets[phi][theta].size(); i++) {
                    Aircraft aircraft = this.mAircraftBuckets[phi][theta].get(i);
                    aircraft.updateSphericalPosition(posLon, posLat, posAlt);
                    if ((aircraft.getPhiIndex() != phi) || (aircraft.getThetaIndex() != theta)) {
                        // Move aircraft
                        this.mAircraftBuckets[aircraft.getPhiIndex()][aircraft.getThetaIndex()]
                                .add(aircraft);
                        this.mAircraftBuckets[phi][theta].remove(i--);
                    }
                }
            }
        }
    }
}
