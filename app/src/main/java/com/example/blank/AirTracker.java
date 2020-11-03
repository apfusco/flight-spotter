package com.example.blank;

import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;

public class AirTracker {

    private static final String URL_STRING = "https://opensky-network.org/api";
    public static final int CONNECTION_TIMEOUT = 5000;
    public static final int READ_TIMEOUT = 5000;
    public static final int VISUAL_DISTANCE = 92600; // In meters
    private static final int EARTH_RADIUS_EQUATOR = 6378137;
    private static final int EARTH_RADIUS_POLES = 6356752;

    private AircraftDataStructure mAircraft;
    private URL mUrl;

    public AirTracker() {
        this.mAircraft = new AircraftDataStructure();
    }

    public void reloadLocations(double posLon, double posLat, float posAlt) {
        JSONObject jsonObject = getAPILocations(posLon, posLat);
        if (jsonObject != null) {
            try {
                Log.i("API", jsonObject.toString()); // TODO: Remove this line
                int time = jsonObject.getInt("time");
                JSONArray states = jsonObject.getJSONArray("states");
                for (int i = 0; i < states.length(); i++) {
                    JSONArray state = states.getJSONArray(i);
                    // Handle any values that might be null
                    String callsign = null;
                    int timePosition = 0;
                    float longitude = 0;
                    float latitude = 0;
                    float baroAltitude = 0;
                    float velocity = 0;
                    float trueTrack = 0;
                    float verticalRate = 0;
                    float geoAltitude = 0;
                    String squawk = null;
                    if (state.getString(1) != null)
                        callsign = state.getString(1);
                    if (!state.isNull(3)) // FIXME
                        timePosition = state.getInt(3);
                    if (!state.isNull(5))
                        longitude = Float.parseFloat(state.getString(5));
                    if (!state.isNull(6))
                        latitude = Float.parseFloat(state.getString(6));
                    if (!state.isNull(7))
                        baroAltitude = Float.parseFloat(state.getString(7));
                    if (!state.isNull(9))
                        velocity = Float.parseFloat(state.getString(9));
                    if (!state.isNull(10))
                        trueTrack = Float.parseFloat(state.getString(10));
                    if (!state.isNull(11))
                        verticalRate = Float.parseFloat(state.getString(11));
                    if (!state.isNull(13))
                        geoAltitude = Float.parseFloat(state.getString(13));
                    if (!state.isNull(14))
                        squawk = state.getString(14);
                    Aircraft aircraft = new Aircraft(
                            time,
                            Integer.parseInt(state.getString(0), 16),
                            callsign,
                            state.getString(2),
                            timePosition,
                            state.getInt(4),
                            longitude,
                            latitude,
                            baroAltitude,
                            state.getBoolean(8),
                            velocity,
                            trueTrack,
                            verticalRate,
                            null,
                            geoAltitude,
                            squawk,
                            state.getBoolean(15),
                            state.getInt(16));
                    aircraft.updateSphericalPosition(posLon, posLat, posAlt);
                    this.mAircraft.addAircraft(aircraft);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public ArrayList<Aircraft> getAircraftInWindow(double minPhi,
                                                   double minTheta,
                                                   double maxPhi,
                                                   double maxTheta) {
        return this.mAircraft.getAircraftInWindow(minPhi, minTheta, maxPhi, maxTheta);
    }

    private JSONObject getAPILocations(double posLon, double posLat) {
        try {
            // Set parameters
            double diffLat = (double)VISUAL_DISTANCE * 360 / getEarthRadius((float)posLat) / 2 / Math.PI;
            double diffLon = (double)VISUAL_DISTANCE * 360 / getEarthRadius((float)posLat)
                    / Math.cos(posLat * 2 * Math.PI / 360) / 2 / Math.PI;
            Log.i("diffLat", Double.toString(diffLat));
            Log.i("diffLon", Double.toString(diffLon));
            double lamin = posLat - diffLat;
            double lamax = posLat + diffLat;
            double lomin = posLon - diffLon;
            double lomax = posLon + diffLon;
            // HashMap<String, String> params = new HashMap<>();
            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("lamin", Float.toString((float)lamin))
                    .appendQueryParameter("lamax", Float.toString((float)lamax))
                    .appendQueryParameter("lomin", Float.toString((float)lomin))
                    .appendQueryParameter("lomax", Float.toString((float)lomax));
            String params = builder.build().getEncodedQuery();
            Log.i("QUERY", params);

            // Make HTTP request
            HttpURLConnection connection;
            URL url = new URL(URL_STRING + "/states/all" + "?" + params);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setDoInput(true);
            connection.connect();
            InputStreamReader inputStreamReader =
                    new InputStreamReader(connection.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String responseString = "";
            String line;

            while (true) {
                line = bufferedReader.readLine();
                if (line == null)
                    break;
                responseString += line;
            }

            JSONTokener jsonTokener = new JSONTokener(responseString);
            connection.disconnect();
            return new JSONObject(jsonTokener);
        } catch (Exception e) {
            // As of now, this just happens on some queries
            e.printStackTrace();
            return null;
        }
    }

    public static int getEarthRadius(float latitude) {
        return (int)Math.round(
                (Math.pow((Math.pow(EARTH_RADIUS_EQUATOR, 2) * Math.cos(latitude)), 2)
                + Math.pow((Math.pow(EARTH_RADIUS_POLES, 2) * Math.sin(latitude)), 2))
                / (Math.pow((EARTH_RADIUS_EQUATOR * Math.cos(latitude)), 2)
                + Math.pow((EARTH_RADIUS_POLES * Math.sin(latitude)), 2)));
    }

}
