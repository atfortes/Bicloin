package pt.tecnico.bicloin.hub;


import java.util.logging.Logger;
import io.grpc.stub.StreamObserver;

public class HubMainImpl extends HubServiceGrpc.HubServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(HubMainImpl.class.getName());

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

}
