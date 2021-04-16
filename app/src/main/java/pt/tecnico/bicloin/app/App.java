package pt.tecnico.bicloin.app;

import pt.tecnico.bicloin.hub.grpc.*;
import pt.tecnico.bicloin.hub.HubFrontend;
import java.util.ArrayList;
import java.util.HashMap;

public class App {

    private double lat;
    private double lon;
    private final String id;
    private final String phone;
    private final HashMap<String, ArrayList<Double>> tags;
    private final HubFrontend frontend;

    public App(double lat, double lon, String id, String phone, HubFrontend frontend) {
        this.lat = lat;
        this.lon = lon;
        this.id = id;
        this.phone = phone;
        this.frontend = frontend;
        this.tags = new HashMap<>();
    }

    public String balance() {

        BalanceRequest request = BalanceRequest.newBuilder()
                .setUsername(id)
                .build();
        BalanceResponse resp = frontend.balance(request);

        return String.format("%s %d BIC", id, resp.getBalance());
    }

    public String topUp(int amount) {

        TopUpRequest request = TopUpRequest.newBuilder()
                .setUsername(id)
                .setAmount(amount)
                .setPhoneNumber(phone)
                .build();
        TopUpResponse resp = frontend.topUp(request);

        return String.format("%s %d BIC", id, resp.getBalance());
    }

    public String tag(double lat, double lon, String name) {

        ArrayList<Double> place = new ArrayList<>();
        place.add(lat);
        place.add(lon);
        tags.put(name, place);
        return "OK";
    }

    public String move(String name) throws BicloinAppException {
        ArrayList<Double> place = tags.get(name);

        if (place == null) {
            throw new BicloinAppException("ERRO tag não definida");
        }

        lat = place.get(0);
        lon = place.get(1);
        return at();
    }

    public String move(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        return at();
    }

    public String at() {
        return String.format("%s em https://www.google.com/maps/place/%f,%f", id, lat, lon);
    }

    public String scan(int n) throws BicloinAppException {

        if (n < 1) {
            throw new BicloinAppException("ERRO número de estações inválido");
        }

        LocateStationRequest request = LocateStationRequest.newBuilder()
                .setLatitude(lat)
                .setLongitude(lon)
                .setK(n)
                .build();
        LocateStationResponse resp = frontend.locateStation(request);

        StringBuilder res = new StringBuilder();

        for (int i = 0; i < resp.getIdsCount(); i++) {
            String sid = resp.getIds(i);
            InfoStationResponse station = frontend.infoStation(InfoStationRequest.newBuilder().setStationId(sid).build());
            DistanceResponse distance = frontend.distance(DistanceRequest.newBuilder().setStationId(sid).setLat(lat).setLon(lon).build());

            res.append(String.format("%s, lat %f, long %f, %d docas, %d BIC prémio, %d bicicletas, a %d " +
                            "metros\n",
                    sid, station.getLatitude(), station.getLongitude(), station.getCapacity(), station.getAward(), station.getBikes(), distance.getDistance()));

        }

        // delete last "/n"
        return res.deleteCharAt(res.length()-1).toString();
    }

    public String info(String name) {

        InfoStationRequest request = InfoStationRequest.newBuilder()
                .setStationId(name)
                .build();
        InfoStationResponse resp = frontend.infoStation(request);
        return String.format("%s, lat %f, long %f, %d docas, %d BIC prémio, %d bicicletas, %d levantamentos, %d devoluções, %s",
                resp.getName(), resp.getLatitude(), resp.getLongitude(), resp.getCapacity(), resp.getAward(), resp.getBikes(),
                resp.getPickups(), resp.getDeliveries(), String.format("https://www.google.com/maps/place/%f,%f", resp.getLatitude(), resp.getLongitude()));
    }

    public String bikeUp(String name) throws BicloinAppException {

        BikeRequest request = BikeRequest.newBuilder()
                .setUsername(id)
                .setLatitude(lat)
                .setLongitude(lon)
                .setStationId(name)
                .build();
        BikeResponse.Response resp = frontend.bikeUp(request).getResponse();

        switch (resp) {
            case OK:
                break;
            case OUT_OF_RANGE:
                throw new BicloinAppException("ERRO fora de alcance");
            case ALREADY_HAS_BIKE:
                throw new BicloinAppException("ERRO já tem bicicleta");
            case NO_BIKES_IN_STATION:
                throw new BicloinAppException("ERRO estação sem bicicletas");
            case OUT_OF_MONEY:
                throw new BicloinAppException("ERRO sem dinheiro");
        }

        return "OK";
    }


    public String bikeDown(String name) throws BicloinAppException {

        BikeRequest request = BikeRequest.newBuilder()
                .setUsername(id)
                .setLatitude(lat)
                .setLongitude(lon)
                .setStationId(name)
                .build();
        BikeResponse.Response resp = frontend.bikeDown(request).getResponse();

        switch (resp) {
            case OK:
                break;
            case OUT_OF_RANGE:
                throw new BicloinAppException("ERRO fora de alcance");
            case NO_BIKE_REQUESTED:
                throw new BicloinAppException("ERRO não tem bicicleta");
            case STATION_IS_FULL:
                throw new BicloinAppException("ERRO estação sem capacidade");
        }

        return "OK";
    }

    public String ping() {
        CtrlPingRequest request = CtrlPingRequest.newBuilder()
                .setInput("hello")
                .build();
        CtrlPingResponse resp = frontend.ctrlPing(request);
        return "Recebido: " + resp.getOutput();
    }

    public String sys_status() {
        SysStatusResponse resp = frontend.sysStatus(SysStatusRequest.newBuilder().build());
        StringBuilder res = new StringBuilder();

        for (int i = 0; i < resp.getSequenceCount(); i++) {
            SysStatusResponse.Reply reply = resp.getSequence(i);
            res.append(String.format("Server %s contactado com estado: %s\n", reply.getPath(), reply.getStatus()));
        }

        return res.deleteCharAt(res.length()-1).toString();
    }


}
