package com.example.blank;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

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

    // For tracking position
    private double mSphereR;
    private double mPhi;
    private double mTheta;
    private int mPhiIndex;
    private int mThetaIndex;

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

    public int getPhiIndex() {
        return this.mPhiIndex;
    }

    public int getThetaIndex() {
        return this.mThetaIndex;
    }

    public double getPhi() { return this.mPhi; };

    public double getTheta() { return this.mTheta; };

    public String getCallsign() { return this.mCallsign; }

    public double[] updateSphericalPosition(double posLon, double posLat, double posAlt) {

        // Get cartesian position vector in meters
        double[] cartPosVector = new double[3];
        float altitude;
        if (this.mGeoAltitude != 0)
            altitude = this.mGeoAltitude;
        else if (this.mBaroAltitude != 0)
            altitude = this.mBaroAltitude;
        else { // TODO: Possibly come up with way to estimate altitude when not given
            Log.w("updateSphericalPosition", "Was not given altitude for "
                    + Integer.toString(this.mIcao24));
            return null;
        }
        double earthR = AirTracker.getEarthRadius((float)posLat) + altitude;
        cartPosVector[0] = (this.mLongitude - posLon) * 2 * Math.PI / 360 * earthR;
        cartPosVector[1] = (this.mLatitude - posLat) * 2 * Math.PI / 360 * earthR;
        cartPosVector[2] = altitude - posAlt;

        // Get spherical coordinates
        double cylinderR = Math.sqrt(Math.pow(cartPosVector[0], 2)
                + Math.pow(cartPosVector[1], 2));
        double sphereR = Math.sqrt(Math.pow(cartPosVector[0], 2)
                + Math.pow(cartPosVector[1], 2)
                + Math.pow(cartPosVector[2], 2));
        double phi = Math.atan(cartPosVector[1] / cartPosVector[0]);
        double theta = Math.atan(cylinderR / cartPosVector[2]);
        if (phi < 0)
            phi += 2 * Math.PI;
        if (theta < 0)
            theta += 2 * Math.PI;

        this.mSphereR = sphereR;
        this.mPhi = phi;
        this.mTheta = theta;
        this.mThetaIndex = (int)Math.round(Math.floor(theta / 2 / Math.PI
                * AircraftDataStructure.ARRAY_LENGTH));
        this.mPhiIndex = (int)Math.round(Math.floor(phi / 2 / Math.PI
                * AircraftDataStructure.ARRAY_LENGTH));

        return new double[] {sphereR, phi, theta};

    }


}
