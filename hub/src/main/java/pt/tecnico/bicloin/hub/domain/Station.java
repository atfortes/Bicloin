package pt.tecnico.bicloin.hub.domain;

public class Station {

    private final String name;
    private final String id;
    private final double latitude;
    private final double longitude;
    private final int capacity;
    private final int award;
    
    public Station(String name, String id, double latitude, double longitude, int capacity, int award) {
        this.name = name;
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
        this.award = award;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getAward() {
        return award;
    }

    public double haversine_distance(double user_lat, double user_lon) {
        // earth radius in m
        final double r = 6371000;

        double avg_lat = Math.toRadians(latitude - user_lat) / 2;
        double avg_lon = Math.toRadians(longitude - user_lon) / 2;
        double lat1 = Math.toRadians(user_lat);
        double lat2 = Math.toRadians(latitude);

        double c = Math.pow(Math.sin(avg_lat), 2) +
                   Math.pow(Math.sin(avg_lon), 2) *
                            Math.cos(lat1) *
                            Math.cos(lat2);

        return 2*r*Math.atan2(Math.sqrt(c), Math.sqrt(1-c));
    }
}
