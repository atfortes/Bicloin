package pt.tecnico.bicloin.app;

import pt.tecnico.bicloin.app.exceptions.BikePickupAndDropOffException;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.tecnico.bicloin.hub.HubFrontend;

import java.util.ArrayList;
import java.util.HashMap;

public class App {

    private float lat;
    private float lon;
    private final String id;
    private final String phone;
    private HashMap<String, ArrayList<Float>> tags;
    private final HubFrontend frontend;



    public App(float lat, float lon, String id, String phone, HubFrontend frontend) {
        this.lat = lat;
        this.lon = lon;
        this.id = id;
        this.phone = phone;
        this.frontend = frontend;
        this.tags = new HashMap<>();
    }

    public String balance() {



        BalanceRequest.Builder builder = BalanceRequest.newBuilder();
        builder.setUsername(id);
        BalanceResponse resp = frontend.balance(builder.build());

        return String.format("%s %d BIC", id, resp.getBalance());
    }


    public String topUp(int amount) {

        TopUpRequest.Builder builder = TopUpRequest.newBuilder();
        builder.setUsername(id);
        builder.setAmount(amount);
        builder.setPhoneNumber(phone);
        TopUpResponse resp = frontend.topUp(builder.build());

        return String.format("%s %d BIC", id, resp.getBalance());
    }

    public String tag(float lat, float lon, String name) {

        ArrayList<Float> place = new ArrayList<>();
        place.add(lat);
        place.add(lon);
        tags.put(name, place);
        return "OK";
    }

    public String move(String name) {
        ArrayList<Float> place = tags.get(name);
        lat = place.get(0);
        lon = place.get(1);

        return at();
    }

    public String move(float lat, float lon) {
        this.lat = lat;
        this.lon = lon;
        return at();
    }

    public String at() {
        return String.format("%s em https://www.google.com/maps/place/%f,%f", id, lat, lon);
    }

    public String scan(int n) {

        LocateStationRequest.Builder builder = LocateStationRequest.newBuilder();
        builder.setLatitude(lat);
        builder.setLongitude(lon);
        builder.setK(n);
        LocateStationResponse resp = frontend.locateStation(builder.build());

        StringBuilder res = new StringBuilder();

        for (int i = 0; i < n; i++) {
            String sid = resp.getIds(i);
            InfoStationResponse station = frontend.infoStation(InfoStationRequest.newBuilder().setStationId(sid).build());
            DistanceResponse distance = frontend.distance(DistanceRequest.newBuilder().setLat(lat).setLon(lon).build());

            res.append(String.format("%s, lat %f, long %f, %d docas, %f BIC prémio, %d bicicletas, a %d " +
                            "metros\n",
                    sid, station.getLatitude(), station.getLongitude(), station.getCapacity(), station.getAward(), station.getBikes(), distance.getDistance()));

        }
        return res.toString();
    }

    public String info(String name) {

        InfoStationRequest.Builder builder = InfoStationRequest.newBuilder();
        builder.setStationId(name);
        InfoStationResponse resp = frontend.infoStation(builder.build());
        String url = String.format("https://www.google.com/maps/place/%f,%f", resp.getLatitude(), resp.getLongitude());

        return String.format("%s, lat %f, long %f, %d docas, %f BIC prémio, %d bicicletas, %d levantamentos, %d devoluções, %s",
                resp.getName(), resp.getLatitude(), resp.getLongitude(), resp.getCapacity(), resp.getAward(), resp.getBikes(),
                resp.getPickups(), resp.getDeliveries(), url);
    }


    public String bikeUp(String name) throws BikePickupAndDropOffException {

        BikeRequest.Builder builder = BikeRequest.newBuilder();
        builder.setUsername(id);
        builder.setLatitude(lat);
        builder.setLongitude(lon);
        builder.setStationId(name);
        BikeResponse.Response resp = frontend.bikeUp(builder.build()).getResponse();

        switch (resp) {
            case OK:
            case OUT_OF_RANGE:
                throw new BikePickupAndDropOffException("ERRO fora de alcance");
            case ALREADY_HAS_BIKE:
                throw new BikePickupAndDropOffException("ERRO já tem bicicleta");
            case NO_BIKES_IN_STATION:
                throw new BikePickupAndDropOffException("ERRO estação sem bicicletas");
            case OUT_OF_MONEY:
                throw new BikePickupAndDropOffException("ERRO sem dinheiro");
        }

        return "OK";
    }


    public String bikeDown(String name) throws BikePickupAndDropOffException {

        BikeRequest.Builder builder = BikeRequest.newBuilder();
        builder.setUsername(id);
        builder.setLatitude(lat);
        builder.setLongitude(lon);
        builder.setStationId(name);
        BikeResponse.Response resp = frontend.bikeDown(builder.build()).getResponse();

        switch (resp) {
            case OK:
            case OUT_OF_RANGE:
                throw new BikePickupAndDropOffException("ERRO fora de alcance");
            case NO_BIKE_REQUESTED:
                throw new BikePickupAndDropOffException("ERRO não tem bicicleta");
            case STATION_IS_FULL:
                throw new BikePickupAndDropOffException("ERRO estação sem capacidade");
        }

        return "OK";
    }

    public String ping() {

        CtrlPingRequest.Builder builder = CtrlPingRequest.newBuilder();
        builder.setInput("hello");
        CtrlPingResponse resp = frontend.ctrlPing(builder.build());
        return "received: " + resp.getOutput();
    }


    public String sys_status() {
        // TODO
        return "";
    }


}
