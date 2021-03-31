package pt.tecnico.bicloin.hub.domain;


import java.util.HashMap;
import java.util.ArrayList;
import java.lang.Double;


public class HubInfo {

    private HashMap<String, Station> stations;
    private HashMap<String, User> users;

    public HubInfo() {
        // TODO import data from csv
    }

    public Station getStation(String id) {
        return stations.get(id);
    }

    public ArrayList<String> sort_stations(int k, float lat, float lon) {

        ArrayList<Station> gather = new ArrayList<>(stations.values());
        gather.sort((Station s1, Station s2) ->
                Double.compare(s1.haversine_distance(lat, lon), s1.haversine_distance(lat, lon)));
                // compare to is cleaner but wasn't working
                //s1.haversine_distance(lat, lon).compareTo(s2.haversine_distance(lat, lon)));

        ArrayList<String> res = new ArrayList<>();
        // assuming k < #stations
        for (int i = 0; i < k; i++) {
            res.add(gather.get(i).getId());
        }

        return res;
    }



}
