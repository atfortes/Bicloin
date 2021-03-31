package pt.tecnico.bicloin.hub.domain;

public class Station {

    private final String name;
    private final String id;
    private final float latitude;
    private final float longitude;
    private final int capacity;
    private final int award;


    public Station(String name, String id, float latitude, float longitude, int capacity, int award) {
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

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getAward() {
        return award;
    }

    public double haversine_distance(float lat, float lon) {
        // earth radius
        final double r = 63354390;
        double avg_lat = (lat - latitude) / 2;
        double avg_lon = (lon - longitude) / 2;

        double res = Math.sqrt(Math.pow(Math.sin(avg_lat), 2) +
                Math.cos(latitude)*Math.cos(lat)*Math.pow(Math.sin(avg_lon), 2));

        return 2*r*Math.asin(res);
    }

}
