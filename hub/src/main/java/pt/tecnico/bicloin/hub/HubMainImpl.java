package pt.tecnico.bicloin.hub;


import java.util.logging.Logger;
import io.grpc.stub.StreamObserver;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.tecnico.bicloin.hub.domain.*;

public class HubMainImpl extends HubServiceGrpc.HubServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(HubMainImpl.class.getName());

    private HubInfo hub = new HubInfo();

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        // TODO FIXME
        BalanceResponse response;

        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void top_up(TopUpRequest request, StreamObserver<TopUpResponse> responseObserver) {
        // TODO FIXME
        TopUpResponse response;

        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void bike_up(BikeRequest request, StreamObserver<BikeResponse> responseObserver) {
        // TODO FIXME
        BikeResponse response;

        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void bike_down(BikeRequest request, StreamObserver<BikeResponse> responseObserver) {
        // TODO FIXME
        BikeResponse response;

        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();
    }

    @Override
    public void info_station(InfoStationRequest request, StreamObserver<InfoStationResponse> responseObserver) {

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
    public void locate_station(LocateStationRequest request, StreamObserver<LocateStationResponse> responseObserver) {

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
