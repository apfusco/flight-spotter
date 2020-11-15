package com.example.blank;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

public class AirTrackingUnitTests {
    @Test
    public void testSphericalConversion() {
        float viewLatitude = (float)43.117686;
        float viewLongitude = (float)-88.293514;

        float planeLatitude = (float)43.058370;
        float planeLongitude = (float)-88.306496;
        int time = Calendar.getInstance().getTime().getSeconds(); // TODO: Uncertain about format
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
        assertEquals(testAircraft.getAzimuth(), -2.9831, 0.01);
        assertEquals(testAircraft.getAzimuthIndex(), 4);
        assertEquals(testAircraft.getPitch(), 0.3969, 0.01);
        assertEquals(testAircraft.getPitchIndex(), 1);
    }
}
