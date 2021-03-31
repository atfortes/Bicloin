package pt.tecnico.bicloin.hub.domain;


import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;


public class HubInfo {

    private HashMap<String, User> users;
    private HashMap<String, Station> stations;

    public Station getStation(String id) {
        return stations.get(id);
    }

    public void setUsers(List<User> userList) {
        for (User u : userList) { users.put(u.getUsername(), u); }
    }

    public void setStations(List<Station> stationList) {
        for (Station s : stationList) { stations.put(s.getId(), s); }
    }

    public ArrayList<String> sort_stations(int k, float lat, float lon) {

        ArrayList<Station> gather = new ArrayList<>(stations.values());
        gather.sort((Station s1, Station s2) ->
                Double.compare(s1.haversine_distance(lat, lon), s1.haversine_distance(lat, lon)));
                // compare to is cleaner but wasn't working
                //s1.haversine_distance(lat, lon).compareTo(s2.haversine_distance(lat, lon)));

        ArrayList<String> res = new ArrayList<>();

        for (int i = 0; i < Math.min(k, stations.size()); i++) {
            res.add(gather.get(i).getId());
        }

        return res;
    }

}
