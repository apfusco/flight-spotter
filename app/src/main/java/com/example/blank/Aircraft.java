package com.example.blank;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import java.util.Calendar;

public class Aircraft {

    // From OpenSky SPI
    private int mTimeQuery;        // Time that the query was made (applies to position)
    private int mIcao24;           // 24-bit address of transponder
    private String mCallsign;      // Should be 8 characters (can be null)
    private String mOriginCountry; // Country name
    private int mTimePosition;     // Last time position was updated (can be null)
    private int mLastContact;      // Last time any update was made
    private float mLongitude;      // (can be null)
    private float mLatitude;       // (can be null)
    private float mBaroAltitude;   // (can be null)
    private boolean mOnGround;     // Position was received from a surface position report
    private float mVelocity;       // Ground velocity in m/s (can be null)
    private float mTrueTrack;      // (can be null)
    private float mVerticalRate;   // (can be null)
    private int[] mSensors;
    private float mGeoAltitude;    // (can be null)
    private String mSquawk;        // Transponder code of the squawk (can be null)
    private boolean mSpi;          // Special purpose indicator
    private int mPositionSource;   // {0=ADS-B, 1=ASTERIX, 2=MLAT}

    // Spherical position
    private double mSphereR;
    private double mAzimuth;
    private double mPitch;
    private int mAzimuthIndex;
    private int mPitchIndex;

    public Aircraft(int timeQuery,
                    int icao24,
                    String callsign,
                    String originCountry,
                    int timePosition,
                    int lastContact,
                    float longitude,
                    float latitude,
                    float baroAltitude,
                    boolean onGround,
                    float velocity,
                    float trueTrack,
                    float verticalRate,
                    int[] sensors,
                    float geoAltitude,
                    String squawk,
                    boolean spi,
                    int positionSource) {

        // Initialize fields from API
        this.mTimeQuery = timeQuery;
        this.mIcao24 = icao24;
        this.mCallsign = callsign;
        this.mOriginCountry = originCountry;
        this.mTimePosition = timePosition;
        this.mLastContact = lastContact;
        this.mLongitude = longitude;
        this.mLatitude = latitude;
        this.mBaroAltitude = baroAltitude;
        this.mOnGround = onGround;
        this.mVelocity = velocity;
        this.mTrueTrack = trueTrack;
        this.mVerticalRate = verticalRate;
        this.mSensors = sensors;
        this.mGeoAltitude = geoAltitude;
        this.mSquawk = squawk;
        this.mSpi = spi;
        this.mPositionSource = positionSource;

    }

    public int getAzimuthIndex() { return this.mAzimuthIndex; }

    public int getPitchIndex() { return this.mPitchIndex; }

    public double getAzimuth() { return this.mAzimuth; };

    public double getPitch() { return this.mPitch; };

    public String getCallsign() { return this.mCallsign; }

    public double[] updateSphericalPosition(double posLon, double posLat, double posAlt) {

        // Get current time
        int curTime = (new Long(Calendar.getInstance().getTime().getTime() / 1000)).intValue();

        // Get cartesian position vector in meters
        double[] cartPosVector = new double[3];
        double altitude;
        if (this.mGeoAltitude != 0)
            altitude = this.mGeoAltitude + this.mVerticalRate * (curTime - this.mTimePosition);
        else if (this.mBaroAltitude != 0)
            altitude = this.mBaroAltitude + this.mVerticalRate * (curTime - this.mTimePosition);
        else { // TODO: Possibly come up with way to estimate altitude when not given
            Log.w("updateSphericalPosition", "Was not given altitude for "
                    + Integer.toString(this.mIcao24));
            return null;
        }
        double earthR = AirTracker.getEarthRadius((float)posLat) + altitude;
        // FIXME: This kinda assumes the Earth is flat
        // FIXME: Ignores International Dateline edge case
        // x-axis points East, y-axis points North for cartesian
        cartPosVector[0] = (this.mLongitude - posLon) / 360 * 2 * Math.PI * earthR
                * Math.cos((this.mLatitude + posLat) / 360 * Math.PI)
                + this.mVelocity * (curTime - this.mTimePosition)
                * Math.sin(this.mTrueTrack / 360 * 2 * Math.PI);
        cartPosVector[1] = (this.mLatitude - posLat) / 360 * 2 * Math.PI * earthR
                + this.mVelocity * (curTime - this.mTimePosition)
                * Math.cos(this.mTrueTrack / 360 * 2 * Math.PI);
        cartPosVector[2] = altitude - posAlt;

        // Get spherical coordinates
        double cylinderR = Math.sqrt(Math.pow(cartPosVector[0], 2)
                + Math.pow(cartPosVector[1], 2));
        double sphereR = Math.sqrt(Math.pow(cartPosVector[0], 2)
                + Math.pow(cartPosVector[1], 2)
                + Math.pow(cartPosVector[2], 2));

        double arctan;
        double azimuth;
        if (cartPosVector[1] == 0)
            arctan = Math.PI / 2;
        else
            arctan = Math.abs(Math.atan(cartPosVector[0] / cartPosVector[1]));
        if (cartPosVector[0] >= 0)
            if (cartPosVector[1] >= 0)
                azimuth = arctan;
            else
                azimuth = Math.PI - arctan;
        else
            if (cartPosVector[1] >= 0)
                azimuth = -arctan;
            else
                azimuth = -Math.PI + arctan;

        double pitch;
        if (cylinderR == 0) {
            if (cartPosVector[2] >= 0)
                pitch = Math.PI / 2;
            else
                pitch = -Math.PI / 2;
        } else
            pitch = Math.atan(cartPosVector[2] / cylinderR);

        this.mSphereR = sphereR;
        this.mAzimuth = azimuth;
        this.mPitch = pitch;
        this.mAzimuthIndex = (int)Math.round(Math.floor(azimuth / 2 / Math.PI
                * AircraftDataStructure.ARRAY_LENGTH)) % AircraftDataStructure.ARRAY_LENGTH
                + AircraftDataStructure.ARRAY_LENGTH / 2;
        // if (this.mAzimuthIndex < 0)
        //     this.mAzimuthIndex += AircraftDataStructure.ARRAY_LENGTH;
        this.mPitchIndex = (int)Math.round(Math.floor(pitch / Math.PI
                * AircraftDataStructure.ARRAY_LENGTH)) % AircraftDataStructure.ARRAY_LENGTH
                + AircraftDataStructure.ARRAY_LENGTH / 2;
        // if (this.mPitchIndex < 0)
        //     this.mPitchIndex += AircraftDataStructure.ARRAY_LENGTH;

        return new double[] {sphereR, azimuth, pitch};

    }


}
