package pt.tecnico.bicloin.hub;


import java.util.List;
import java.util.logging.Logger;

import com.google.protobuf.Any;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.Status;
import org.apache.commons.lang.enums.EnumUtils;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.tecnico.bicloin.hub.domain.*;
import pt.tecnico.rec.RecFrontend;
import pt.tecnico.rec.grpc.Rec;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class HubImpl extends HubServiceGrpc.HubServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(HubImpl.class.getName());

    // FIXME hard coded ?
    private final String zooHost = "localhost";
    private final int zooPort = 2181;
    private final String recPath = "/grpc/bicloin/rec/1";

    private final int euro2bic = 10;

    private HubInfo hub = new HubInfo();

    public HubImpl(List<User> userList, List<Station> stationList) {
        hub.setUsers(userList);
        hub.setStations(stationList);
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {

        String username = request.getUsername();
        User user = hub.getUser(username);
        if (user == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("User not found").asRuntimeException());
            return;
        }

        RecFrontend frontend = null;
        try {
            frontend = new RecFrontend(zooHost, zooPort, recPath);

            Rec.ReadResponse res = frontend.read(Rec.ReadRequest.newBuilder().setName("users/" + username + "/balance").build());
            if (res.getValue().is(Int32Value.class)) {
                Int32Value balance = res.getValue().unpack(Int32Value.class);
                BalanceResponse response = BalanceResponse.newBuilder().setBalance(balance.getValue()).build();

                // Send a single response through the stream.
                responseObserver.onNext(response);
                // Notify the client that the operation has been completed.
                responseObserver.onCompleted();
            }

            else {
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Balance not found").asRuntimeException());
            }



        } catch (ZKNamingException e) {
            System.err.println("Caught exception when searching for Rec: " + e);
        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught when unpacking data from Rec: " + e);
        } finally {
            if (frontend != null) {
                frontend.close();
            }
        }
    }

    @Override
    public void topUp(TopUpRequest request, StreamObserver<TopUpResponse> responseObserver) {

        String username = request.getUsername();
        User user = hub.getUser(username);
        if (user == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("User not found").asRuntimeException());
            return;
        }
        if (!request.getPhoneNumber().equals(user.getPhone())) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Phone number incorrect").asRuntimeException());
            return;
        }
        int amount = request.getAmount();
        if (!(1 <= amount && amount <= 20)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Amount not in 1-20 interval").asRuntimeException());
            return;
        }

        RecFrontend frontend = null;
        try {
            frontend = new RecFrontend(zooHost, zooPort, recPath);

            Rec.ReadResponse res = frontend.read(Rec.ReadRequest.newBuilder().setName("users/" + username + "/balance").build());
            Int32Value balance = res.getValue().unpack(Int32Value.class);
            Int32Value newBalance = Int32Value.newBuilder().setValue(balance.getValue() + amount*euro2bic).build();
            frontend.write(Rec.WriteRequest.newBuilder().setName("users/" + username + "/balance").setValue(Any.pack(newBalance)).build());

            TopUpResponse response = TopUpResponse.newBuilder().setBalance(balance.getValue()).build();

            // Send a single response through the stream.
            responseObserver.onNext(response);
            // Notify the client that the operation has been completed.
            responseObserver.onCompleted();

        } catch (ZKNamingException e) {
            System.err.println("Caught exception when searching for Rec: " + e);
        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught when unpacking data from Rec: " + e);
        } finally {
            if (frontend != null) {
                frontend.close();
            }
        }
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

    @Override
    public void ctrlPing(CtrlPingRequest request, StreamObserver<CtrlPingResponse> responseObserver) {
        LOGGER.info("Received Ping");
        String input = request.getInput();
        responseObserver.onNext(CtrlPingResponse.newBuilder().setOutput(input).build());
        responseObserver.onCompleted();
    }

    @Override
    public void sysStatus(SysStatusRequest request, StreamObserver<SysStatusResponse> responseObserver) {
        // TODO
    }



}
