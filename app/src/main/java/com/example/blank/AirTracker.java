package com.example.blank;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class AirTracker {

    private static final String OPENSKY_URL = "https://opensky-network.org/api";
    private static final String AIRPORT_URL = "https://www.airport-data.com/api/ap_info.json";
    private static final String PIC_URL = "https://www.airport-data.com/api/ac_thumb.json";
    public static final int CONNECTION_TIMEOUT = 5000;
    public static final int READ_TIMEOUT = 5000;
    public static final int VISUAL_DISTANCE = 50000; // In meters
    private static final int EARTH_RADIUS_EQUATOR = 6378137;
    private static final int EARTH_RADIUS_POLES = 6356752;

    private AircraftDataStructure mAircraft;
    private HashMap<Integer, AircraftInfo> cache;
    private URL mUrl;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AirTracker() {
        this.mAircraft = new AircraftDataStructure();
        this.cache = new HashMap<>();
    }

    public void reloadLocations(double posLon, double posLat, float posAlt) {
        JSONObject jsonObject = getAPILocations(posLon, posLat);
        if (jsonObject != null) {
            try {
                Log.i("API", jsonObject.toString()); // TODO: Remove this line
                int time = jsonObject.getInt("time");
                JSONArray states = jsonObject.getJSONArray("states");
                this.mAircraft.clearAircraft();
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
                        callsign = state.getString(1).trim();
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
                    // updateMoreInfo(aircraft);
                    this.mAircraft.addAircraft(aircraft);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    /**
     *
     * @param urlString
     * @param requestType
     * @param params
     * @return JSONTokener of response, or null on error.
     */
    private JSONTokener makeRequest(String urlString, String requestType, String params) {
        JSONTokener jsonTokener = null;
        try {
            // Make HTTP request
            HttpURLConnection connection;
            URL url = new URL(urlString + "?" + params);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(requestType);
            connection.setConnectTimeout(CONNECTION_TIMEOUT * 10);
            connection.setReadTimeout(READ_TIMEOUT * 10);
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

            jsonTokener = new JSONTokener(responseString);
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonTokener;
    }

    public void updateMoreInfo(Aircraft aircraft) {
        // FIXME: This method is broken because the API doesn't work
        System.out.println("\n\n\nupdateCache()\n\n\n"); // TODO
        if (this.cache.containsKey(aircraft.getIcao24())) {
            // Aircraft is already in cache.
            AircraftInfo info = this.cache.get(aircraft.getIcao24());
            aircraft.setEstDepartureAirport(info.getEstDepartureAirport());
            aircraft.setEstArrivalAirport(info.getEstArrivalAirport());
            return;
        }

        try {
            // Thread.sleep(10); // Don't overwhelm the API.
            int time = (new Long(Calendar.getInstance().getTime().getTime() / 1000))
                    .intValue();
            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("icao24", Integer.toString(aircraft.getIcao24(), 16))
                    .appendQueryParameter("begin", Integer.toString(time - 60 * 60 * 24 * 20)) // Past 20 days
                    .appendQueryParameter("end", Integer.toString(time + 60 * 60 * 24 * 5)); // Next 5 days
            String params = builder.build().getEncodedQuery();
            Log.i("QUERY", params);

            JSONTokener jsonTokener = this.makeRequest((OPENSKY_URL + "/flights/aircraft"), "GET",
                    params);
            JSONArray responseJSON = new JSONArray(jsonTokener);

            AircraftInfo info = new AircraftInfo(aircraft.getIcao24(), aircraft.getCallsign(),
                    aircraft.getOriginCountry());

            /* // TODO: Get current arrival airport another way
            if ((responseJSON.length() > 0)
                    && (!responseJSON.getJSONObject(0).isNull("estDepartureAirport"))) {
                String estDepAirport = responseJSON.getJSONObject(0)
                        .getString("estDepartureAirport");
                info.setEstDepartureAirport(estDepAirport);
                aircraft.setEstDepartureAirport(estDepAirport);
            }
            */

            if ((responseJSON.length() > 0)
                    && (!responseJSON.getJSONObject(0).isNull("estArrivalAirport"))) {
                String estArivAirport = responseJSON.getJSONObject(0)
                        .getString("estArrivalAirport");
                info.setEstDepartureAirport(estArivAirport);
                aircraft.setEstDepartureAirport(estArivAirport);
                getAirportInfo(aircraft);
            }

            this.cache.put(aircraft.getIcao24(), info);

        } catch (Exception e) {
            // This might happen every once in a while
            e.printStackTrace();
        }
        this.getImage(aircraft);
    }

    private void getImage(Aircraft aircraft) {
        // Set parameters
        Uri.Builder builder = new Uri.Builder()
                .appendQueryParameter("m", Integer.toString(aircraft.getIcao24(), 16))
                .appendQueryParameter("n", Integer.toString(1));
        String params = builder.build().getEncodedQuery();
        Log.i("QUERY", params);

        JSONTokener jsonTokener = this.makeRequest(PIC_URL, "GET", params);
        JSONObject jsonResponse;
        try {
            jsonResponse = new JSONObject(jsonTokener);
            URL imageURL = new URL(jsonResponse.getJSONArray("data").getJSONObject(0)
                    .getString("image"));
            InputStream inputStream = imageURL.openStream();
            Bitmap imageBitmap = BitmapFactory.decodeStream(new BufferedInputStream(inputStream));
            aircraft.setImageBitmap(imageBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getAirportInfo(Aircraft aircraft) {
        // Set parameters
        Uri.Builder builder = new Uri.Builder()
                .appendQueryParameter("icao", aircraft.getEstDepartureAirport());
        String params = builder.build().getEncodedQuery();
        Log.i("QUERY", params);

        JSONTokener jsonTokener = this.makeRequest(PIC_URL, "GET", params);
        JSONObject jsonResponse;
        try {
            jsonResponse = new JSONObject(jsonTokener);
            aircraft.setEstDepartureAirportName(jsonResponse.getString("name"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Aircraft> getAircraftInWindow(double minAzimuth,
                                                   double minPitch,
                                                   double maxAzimuth,
                                                   double maxPitch) {
        return this.mAircraft.getAircraftInWindow(minAzimuth, minPitch, maxAzimuth, maxPitch);
    }

    public ArrayList<Aircraft> getAllAircraft() {
        return this.getAircraftInWindow(-Math.PI, -Math.PI / 2, Math.PI, Math.PI / 2);
    }

    public void updateLocations() {
        this.mAircraft.updateLocations(MainActivity.mLocation.getLongitude(),
                MainActivity.mLocation.getLatitude(),
                MainActivity.mLocation.getAltitude());
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

            JSONTokener jsonTokener = this.makeRequest((OPENSKY_URL + "/states/all"), "GET",
                    params);
            return new JSONObject(jsonTokener);
        } catch (Exception e) {
            // As of now, this just happens on some queries
            e.printStackTrace();
            return null;
        }
    }

    public static int getEarthRadius(float latitude) {
        double theta = latitude / 360 * 2 * Math.PI;
        return (int)Math.round(Math.sqrt(
                (Math.pow((Math.pow(EARTH_RADIUS_EQUATOR, 2) * Math.cos(theta)), 2)
                + Math.pow((Math.pow(EARTH_RADIUS_POLES, 2) * Math.sin(theta)), 2))
                / (Math.pow((EARTH_RADIUS_EQUATOR * Math.cos(theta)), 2)
                + Math.pow((EARTH_RADIUS_POLES * Math.sin(theta)), 2))));
    }

}
