package pt.tecnico.bicloin.hub.domain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HubInfo {

    private final HashMap<String, User> users = new HashMap<>();
    private final HashMap<String, Station> stations = new HashMap<>();

    public User getUser(String username) {
        return users.get(username);
    }

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
        gather.sort(Comparator.comparingDouble((Station s) -> s.haversine_distance(lat, lon)));
        return gather.subList(0, Math.min(gather.size(), k))
                .stream().map(Station::getId).collect(Collectors.toCollection(ArrayList::new));
    }

}
