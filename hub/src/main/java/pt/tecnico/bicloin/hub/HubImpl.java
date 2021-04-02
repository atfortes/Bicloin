package pt.tecnico.bicloin.hub;


import java.util.List;
import java.util.logging.Logger;
import io.grpc.stub.StreamObserver;
import io.grpc.Status;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.tecnico.bicloin.hub.domain.*;

public class HubImpl extends HubServiceGrpc.HubServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(HubImpl.class.getName());

    private HubInfo hub = new HubInfo();

    public HubImpl(List<User> userList, List<Station> stationList) {
        hub.setUsers(userList);
        hub.setStations(stationList);
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        // TODO
        BalanceResponse response = BalanceResponse.newBuilder().build();

        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void topUp(TopUpRequest request, StreamObserver<TopUpResponse> responseObserver) {
        // TODO
        TopUpResponse response = TopUpResponse.newBuilder().build();

        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void bikeUp(BikeRequest request, StreamObserver<BikeResponse> responseObserver) {
        // TODO
        BikeResponse response = BikeResponse.newBuilder().build();

        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void bikeDown(BikeRequest request, StreamObserver<BikeResponse> responseObserver) {
        // TODO
        BikeResponse response = BikeResponse.newBuilder().build();

        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void infoStation(InfoStationRequest request, StreamObserver<InfoStationResponse> responseObserver) {

        String id = request.getStationId();
        Station station = hub.getStation(id);

        if (station == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Station not found").asRuntimeException());
        }

        else {
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

}
