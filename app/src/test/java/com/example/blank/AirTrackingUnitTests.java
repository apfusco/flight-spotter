package com.example.blank;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;

import static org.junit.Assert.*;

public class AirTrackingUnitTests {
    @Test
    public void testSphericalConversion() {
        float viewLatitude = (float)43.117686;
        float viewLongitude = (float)-88.293514;

        float planeLatitude = (float)43.058370;
        float planeLongitude = (float)-88.306496;
        int time = (new Long(Calendar.getInstance().getTime().getTime() / 1000)).intValue();
        Aircraft testAircraft = new Aircraft(
                time,
                0x696969, // Nice
                "XES",
                "Kazakhstan",
                time,
                time,
                planeLongitude,
                planeLatitude,
                3000, // 3000 m altitude
                false,
                0,
                0,
                0,
                null,
                0,
                "Borat",
                false,
                0);
        testAircraft.updateSphericalPosition(viewLongitude, viewLatitude, 200); // 200 m altitude
        System.out.println("Azimuth: " + testAircraft.getAzimuth());
        System.out.println("Azimuth Index: " + testAircraft.getAzimuthIndex());
        System.out.println("Pitch: " + testAircraft.getPitch());
        System.out.println("Pitch Index: " + testAircraft.getPitchIndex());
        assertEquals(-2.9831, testAircraft.getAzimuth(), 0.01);
        assertEquals(0, testAircraft.getAzimuthIndex());
        assertEquals(0.3969, testAircraft.getPitch(),0.01);
        assertEquals(5, testAircraft.getPitchIndex());
    }

    @Test
    public void testDataStructureWindow() {
        AircraftDataStructure dataStructure = new AircraftDataStructure();
        float viewLatitude = (float)43.117686;
        float viewLongitude = (float)-88.293514;

        float planeLatitude = (float)43.058370;
        float planeLongitude = (float)-88.306496;
        int time = (new Long(Calendar.getInstance().getTime().getTime() / 1000)).intValue();
        Aircraft testAircraft = new Aircraft(
                time,
                0x696969, // Nice
                "XES",
                "Kazakhstan",
                time,
                time,
                planeLongitude,
                planeLatitude,
                3000, // 3000 m altitude
                false,
                0,
                0,
                0,
                null,
                0,
                "Borat",
                false,
                0);
        dataStructure.addAircraft(testAircraft);
        dataStructure.updateLocations(viewLongitude, viewLatitude, 200); // 200 m altitude
        System.out.println("Azimuth: " + testAircraft.getAzimuth());
        System.out.println("Azimuth Index: " + testAircraft.getAzimuthIndex());
        System.out.println("Pitch: " + testAircraft.getPitch());
        System.out.println("Pitch Index: " + testAircraft.getPitchIndex());
        assertEquals(-2.9831, testAircraft.getAzimuth(), 0.01);
        assertEquals(0, testAircraft.getAzimuthIndex());
        assertEquals(0.3969, testAircraft.getPitch(), 0.01);
        assertEquals(5, testAircraft.getPitchIndex());

        // Retrieve all aircraft in sky
        ArrayList<Aircraft> aircraftList = dataStructure.getAircraftInWindow(-Math.PI,
                (-Math.PI / 2), Math.PI, (Math.PI / 2));
        assertEquals(1, aircraftList.size());
        assertEquals(testAircraft, aircraftList.get(0));

        // Incorrect bounds
        aircraftList = dataStructure.getAircraftInWindow(-2.7, 0.3, 0, 0.5);
        assertEquals(0, aircraftList.size());

        // Test regular window
        aircraftList = dataStructure.getAircraftInWindow(-3.1, 0.3, -2.8, 0.5);
        assertEquals(1, aircraftList.size());
        assertEquals(testAircraft, aircraftList.get(0));

        // Test edge case to the South
        aircraftList = dataStructure.getAircraftInWindow(2.8, 0.3, -2.8, 0.5);
        assertEquals(1, aircraftList.size());
        assertEquals(testAircraft, aircraftList.get(0));
    }

    @Test
    public void testTestMovingAircraft() {
        final float velocity = 100;
        final float trueTrack = 270; // South
        final float verticalRate = 0;

        float viewLatitude = (float)43.117686;
        float viewLongitude = (float)-88.293514;

        float planeLatitude = (float)43.058370;
        float planeLongitude = (float)-88.306496;
        int time = (new Long(Calendar.getInstance().getTime().getTime() / 1000)).intValue();
        Aircraft testAircraft = new Aircraft(
                time,
                0x696969, // Nice
                "XES",
                "Kazakhstan",
                time,
                time,
                planeLongitude,
                planeLatitude,
                3000, // 3000 m altitude
                false,
                velocity,
                trueTrack,
                verticalRate,
                null,
                0,
                "Borat",
                false,
                0);
        testAircraft.updateSphericalPosition(viewLongitude, viewLatitude, 200); // 200 m altitude
        System.out.println("Azimuth: " + testAircraft.getAzimuth());
        System.out.println("Azimuth Index: " + testAircraft.getAzimuthIndex());
        System.out.println("Pitch: " + testAircraft.getPitch());
        System.out.println("Pitch Index: " + testAircraft.getPitchIndex());
        assertEquals(-2.9831, testAircraft.getAzimuth(), 0.01);
        assertEquals(0, testAircraft.getAzimuthIndex());
        assertEquals(0.3969, testAircraft.getPitch(),0.01);
        assertEquals(5, testAircraft.getPitchIndex());

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        testAircraft.updateSphericalPosition(viewLongitude, viewLatitude, 200);
        System.out.println("Azimuth: " + testAircraft.getAzimuth());
        System.out.println("Azimuth Index: " + testAircraft.getAzimuthIndex());
        System.out.println("Pitch: " + testAircraft.getPitch());
        System.out.println("Pitch Index: " + testAircraft.getPitchIndex());
        // assertEquals(-2.9831, testAircraft.getAzimuth(), 0.01);
        assertTrue(testAircraft.getAzimuth() > -2.9);
        assertEquals(0.3969, testAircraft.getPitch(),0.8);
    }

    @Test
    public void testUpdateLocations() {
        final float velocity = 100;
        final float trueTrack = 270; // South
        final float verticalRate = 0;

        float viewLatitude = (float)43.117686;
        float viewLongitude = (float)-88.293514;
        float viewAltitude = 200;

        float planeLatitude = (float)43.058370;
        float planeLongitude = (float)-88.306496;
        int time = (new Long(Calendar.getInstance().getTime().getTime() / 1000)).intValue();
        Aircraft testAircraft = new Aircraft(
                time,
                0x696969, // Nice
                "XES",
                "Kazakhstan",
                time,
                time,
                planeLongitude,
                planeLatitude,
                3000, // 3000 m altitude
                false,
                velocity,
                trueTrack,
                verticalRate,
                null,
                0,
                "Borat",
                false,
                0);
        AircraftDataStructure dataStructure = new AircraftDataStructure();
        dataStructure.addAircraft(testAircraft);

        dataStructure.updateLocations(viewLongitude, viewLatitude, viewAltitude);
        System.out.println("Azimuth: " + testAircraft.getAzimuth());
        System.out.println("Azimuth Index: " + testAircraft.getAzimuthIndex());
        System.out.println("Pitch: " + testAircraft.getPitch());
        System.out.println("Pitch Index: " + testAircraft.getPitchIndex());
        assertEquals(-2.9831, testAircraft.getAzimuth(), 0.01);
        assertEquals(0, testAircraft.getAzimuthIndex());
        assertEquals(0.3969, testAircraft.getPitch(),0.01);
        assertEquals(5, testAircraft.getPitchIndex());


        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        dataStructure.updateLocations(viewLongitude, viewLatitude, viewAltitude);
        System.out.println("Azimuth: " + testAircraft.getAzimuth());
        System.out.println("Azimuth Index: " + testAircraft.getAzimuthIndex());
        System.out.println("Pitch: " + testAircraft.getPitch());
        System.out.println("Pitch Index: " + testAircraft.getPitchIndex());
        // assertEquals(-2.9831, testAircraft.getAzimuth(), 0.01);
        assertTrue(testAircraft.getAzimuth() > -2.9);
        assertEquals(0.3969, testAircraft.getPitch(),0.8);
    }
}
