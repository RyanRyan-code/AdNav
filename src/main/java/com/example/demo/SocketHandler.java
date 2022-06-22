package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SocketHandler extends TextWebSocketHandler {

    List sessions = new CopyOnWriteArrayList<>();

    double earthRadius = 6371000;

    String cacheResult="";

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws InterruptedException, IOException {

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
        String urlMain = "https://data-live.flightradar24.com/zones/fcgi/feed.js";
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
        URL url = new URL(urlMain + urlBounds);

        //connect and get response
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/json");

        //read json
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();

        //remove irrelevant entries
        JsonObject jsonObject = JsonParser.parseString(content.toString()).getAsJsonObject();
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

        //close connection and return result
        con.disconnect();
        String res = jsonObject.toString();

        //only return new information
        if(Objects.equals(jsonObject.toString(), cacheResult)){
            session.sendMessage(new TextMessage("no change"));
        }else{
            cacheResult = res;
            session.sendMessage(new TextMessage(res));
        }

    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //the messages will be broadcasted to all users.
        sessions.add(session);
    }
}
