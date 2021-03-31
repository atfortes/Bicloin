package pt.tecnico.bicloin.hub;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import io.grpc.stub.StreamObserver;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.tecnico.bicloin.hub.domain.*;

public class HubMainImpl extends HubServiceGrpc.HubServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(HubMainImpl.class.getName());

    private HubInfo hub = new HubInfo();

    public HubMainImpl(String fileUsers, String fileStations, boolean initRec) {
        importUsers(fileUsers, initRec);
        importStations(fileStations, initRec);
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        // TODO
        BalanceResponse response;

        // Send a single response through the stream.
        //responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void topUp(TopUpRequest request, StreamObserver<TopUpResponse> responseObserver) {
        // TODO
        TopUpResponse response;

        // Send a single response through the stream.
        //responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void bikeUp(BikeRequest request, StreamObserver<BikeResponse> responseObserver) {
        // TODO
        BikeResponse response;

        // Send a single response through the stream.
        //responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void bikeDown(BikeRequest request, StreamObserver<BikeResponse> responseObserver) {
        // TODO
        BikeResponse response;

        // Send a single response through the stream.
        //responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void infoStation(InfoStationRequest request, StreamObserver<InfoStationResponse> responseObserver) {

        String id = request.getStationId();
        Station station = hub.getStation(id);
        // need to set attributes, requires implementing Station

        InfoStationResponse.Builder builder = InfoStationResponse.newBuilder();
        builder.setName(id);
        builder.setLatitude(station.getLatitude());
        builder.setLongitude(station.getLongitude());
        builder.setCapacity(station.getCapacity());
        builder.setAward(station.getAward());
        //  TODO set bikes, pickups, deliveries from reg

        // Send a single response through the stream.
        responseObserver.onNext(builder.build());
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void locateStation(LocateStationRequest request, StreamObserver<LocateStationResponse> responseObserver) {

        int k = request.getK();
        float lat = request.getLatitude();
        float lon = request.getLongitude();

        // untested
        LocateStationResponse response = LocateStationResponse.newBuilder().addAllIds(hub.sort_stations(k, lat, lon)).build();

        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    private void importUsers(String fileUsers, boolean initRec) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileUsers));

            List<User> userList = new ArrayList<>();

            String line = "";
            while ((line = br.readLine()) != null) {
                String[] userDetails = line.split(",");

                if(userDetails.length > 0 ) {
                    // TODO if initRec
                    User u = new User(userDetails[0], userDetails[1], userDetails[2]);
                    userList.add(u);
                }
            }

            hub.setUsers(userList);
        }
        catch(Exception e) {
            System.err.println("Caught exception while parsing the users file: " + e);
        }
        finally {
            try {
                if (br != null) { br.close(); }
            }
            catch(IOException ie) {
                System.err.println("Caught exception while closing the BufferedReader: " + ie);
            }
        }
    }

    private void importStations(String fileStations, boolean initRec) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileStations));

            List<Station> stationList = new ArrayList<>();

            String line = "";
            while ((line = br.readLine()) != null) {
                String[] stationDetails = line.split(",");

                if(stationDetails.length > 0 ) {
                    // TODO if initRec
                    Station s = new Station(stationDetails[0], stationDetails[1], Float.parseFloat(stationDetails[2]),
                            Float.parseFloat(stationDetails[3]), Integer.parseInt(stationDetails[4]),
                            Integer.parseInt(stationDetails[6]));
                    stationList.add(s);
                }
            }

            hub.setStations(stationList);
        }
        catch(Exception e) {
            System.err.println("Caught exception while parsing the stations file: " + e);
        }
        finally {
            try {
                if (br != null) { br.close(); }
            }
            catch(IOException ie) {
                System.err.println("Caught exception while closing the BufferedReader: " + ie);
            }
        }
    }
}
