package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class Task implements Runnable {

    public static String cachedResult = "";

    private final String urlMain = "https://data-live.flightradar24.com/zones/fcgi/feed.js";

    WebSocketSession session;
    TextMessage message;

    double earthRadius = 6371000;

    Task(WebSocketSession session, TextMessage message){
        this.session = session;
        this.message = message;
    }

    public void run()
    {
        //get user input
        Map value = new Gson().fromJson(message.getPayload(), Map.class);
        double latitude = (double)value.get("latitude");
        double longitude = (double)value.get("longitude");
        double r = (double)value.get("radius");

        //geo calculations
        double deltaLatitude = r/111000;
        double deltaLongitude = Math.abs(r*180/(earthRadius*Math.sin(latitude)*Math.PI));

        //build http request
        //build the full url with query parameters

        String urlBounds = "?bounds=";

        double minLat, maxLat, minLon, maxLon;

        //edge cases
        minLat = Math.min(latitude+deltaLatitude, 90);
        maxLat = Math.max(latitude-deltaLatitude, -90);
        if(longitude-deltaLongitude>180 || longitude-deltaLongitude<-180 || longitude+deltaLongitude>180 || longitude+deltaLongitude<-180){
            minLon = -180;
            maxLon = 180;
        }else{
            minLon = longitude-deltaLongitude;
            maxLon = longitude+deltaLongitude;
        }

        urlBounds += minLat+",";
        urlBounds += maxLat+",";
        urlBounds += minLon+",";
        urlBounds += maxLon+"";
        URL url = null;

        //build full url
        try {
            url = new URL(urlMain + urlBounds);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        //execute http-request and get request-body
        JsonObject jsonObject;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
            //why can't I assert (response.body!=null) here?
            //response can only be consumed once?
            jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //remove irrelevant entries
        jsonObject.remove("full_count");
        jsonObject.remove("version");

        //use geoTools to calculate distance
        DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
        GeodeticCalculator calc = new GeodeticCalculator(crs);
        calc.setStartingGeographicPoint(longitude, latitude);

        //check the distance of each plane
        List<String> outOfRange = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            double lat = entry.getValue().getAsJsonArray().get(1).getAsDouble();
            double lon = entry.getValue().getAsJsonArray().get(2).getAsDouble();

            calc.setDestinationGeographicPoint(lon, lat);
            double distance = calc.getOrthodromicDistance();
            if(distance>r){
                outOfRange.add(entry.getKey());
            }
        }

        //remove out-of-range planes
        for (String key:outOfRange){
            jsonObject.remove(key);
        }

        //return result
        String res = jsonObject.toString();
        if(Objects.equals(cachedResult, res)){
            try {
                session.sendMessage(new TextMessage("nothing changed"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }else{
            cachedResult = res;
            try {
                session.sendMessage(new TextMessage(res));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }



    }

}

