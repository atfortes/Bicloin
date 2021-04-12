package pt.tecnico.bicloin.hub;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.grpc.Status;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.tecnico.bicloin.hub.domain.*;
import pt.tecnico.rec.RecFrontend;
import pt.tecnico.rec.grpc.Rec;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import static pt.tecnico.bicloin.hub.HubMain.importStations;
import static pt.tecnico.bicloin.hub.HubMain.importUsers;

public class HubImpl extends HubServiceGrpc.HubServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(HubImpl.class.getName());

    private final int euro2bic = 10;
    private final String RESET_PASSWORD = "super_strong_reset_password";

    private HubInfo hub = new HubInfo();

    private RecFrontend frontend;

    public HubImpl(List<User> userList, List<Station> stationList, RecFrontend frontend) {
        hub.setUsers(userList);
        hub.setStations(stationList);
        this.frontend = frontend;
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {

        String username = request.getUsername();
        User user = hub.getUser(username);
        if (user == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("User not found").asRuntimeException());
            return;
        }

        try {

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

        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught when transferring data between hub and rec: " + e);
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

        try {

            Rec.ReadResponse res = frontend.read(Rec.ReadRequest.newBuilder().setName("users/" + username + "/balance").build());
            if (res.getValue().is(Int32Value.class)) {

                Int32Value balance = res.getValue().unpack(Int32Value.class);
                Int32Value newBalance = Int32Value.newBuilder().setValue(balance.getValue() + amount*euro2bic).build();
                frontend.write(Rec.WriteRequest.newBuilder().setName("users/" + username + "/balance").setValue(Any.pack(newBalance)).build());

                TopUpResponse response = TopUpResponse.newBuilder().setBalance(newBalance.getValue()).build();

                // Send a single response through the stream.
                responseObserver.onNext(response);
                // Notify the client that the operation has been completed.
                responseObserver.onCompleted();
            }
            else {
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Balance not found").asRuntimeException());
            }

        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught when transferring data between hub and rec: " + e);
        }
    }

    @Override
    public void bikeUp(BikeRequest request, StreamObserver<BikeResponse> responseObserver) {

        String username = request.getUsername();
        User user = hub.getUser(username);
        if (user == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("User not found").asRuntimeException());
            return;
        }
        String stationId = request.getStationId();
        Station station = hub.getStation(stationId);
        if (station == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Station not found").asRuntimeException());
            return;
        }
        float latitude = request.getLatitude();
        float longitude = request.getLongitude();
        if (station.haversine_distance(latitude, longitude) >= 200) {
            BikeResponse response = BikeResponse.newBuilder().setResponse(BikeResponse.Response.OUT_OF_RANGE).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        try {

            boolean userHasBike = frontend.read(Rec.ReadRequest.newBuilder().setName("users/" + username + "/bike").build()).getValue().unpack(BoolValue.class).getValue();
            int bikesInStation = frontend.read(Rec.ReadRequest.newBuilder().setName("stations/" + stationId + "/bikes").build()).getValue().unpack(Int32Value.class).getValue();
            int userBalance = frontend.read(Rec.ReadRequest.newBuilder().setName("users/" + username + "/balance").build()).getValue().unpack(Int32Value.class).getValue();
            BikeResponse response;

            if (userHasBike) {
                response = BikeResponse.newBuilder().setResponse(BikeResponse.Response.ALREADY_HAS_BIKE).build();
            }
            else if (bikesInStation == 0) {
                response = BikeResponse.newBuilder().setResponse(BikeResponse.Response.NO_BIKES_IN_STATION).build();
            }
            else if (userBalance < 10) {
                response = BikeResponse.newBuilder().setResponse(BikeResponse.Response.OUT_OF_MONEY).build();
            }
            else {
                int bikeRequests = frontend.read(Rec.ReadRequest.newBuilder().setName("stations/" + stationId + "/requests").build()).getValue().unpack(Int32Value.class).getValue();
                // register new values in rec after everything is verified
                frontend.write(Rec.WriteRequest.newBuilder().setName("users/" + username + "/balance").setValue(Any.pack(Int32Value.newBuilder().setValue(userBalance-10).build())).build());
                frontend.write(Rec.WriteRequest.newBuilder().setName("stations/" + stationId + "/bikes").setValue(Any.pack(Int32Value.newBuilder().setValue(--bikesInStation).build())).build());
                frontend.write(Rec.WriteRequest.newBuilder().setName("users/" + username + "/bike").setValue(Any.pack(BoolValue.newBuilder().setValue(true).build())).build());
                frontend.write(Rec.WriteRequest.newBuilder().setName("stations/" + stationId + "/requests").setValue(Any.pack(Int32Value.newBuilder().setValue(++bikeRequests).build())).build());
                response = BikeResponse.newBuilder().setResponse(BikeResponse.Response.OK).build();
            }

            // Send a single response through the stream.
            responseObserver.onNext(response);
            // Notify the client that the operation has been completed.
            responseObserver.onCompleted();

        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught when transferring data between hub and rec: " + e);
        }
    }

    @Override
    public void bikeDown(BikeRequest request, StreamObserver<BikeResponse> responseObserver) {

        String username = request.getUsername();
        User user = hub.getUser(username);
        if (user == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("User not found").asRuntimeException());
            return;
        }
        String stationId = request.getStationId();
        Station station = hub.getStation(stationId);
        if (station == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Station not found").asRuntimeException());
            return;
        }
        float latitude = request.getLatitude();
        float longitude = request.getLongitude();
        if (station.haversine_distance(latitude, longitude) >= 200) {
            BikeResponse response = BikeResponse.newBuilder().setResponse(BikeResponse.Response.OUT_OF_RANGE).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return;
        }

        try {

            int stationCapacity = station.getCapacity();
            boolean userHasBike = frontend.read(Rec.ReadRequest.newBuilder().setName("users/" + username + "/bike").build()).getValue().unpack(BoolValue.class).getValue();
            int bikesInStation = frontend.read(Rec.ReadRequest.newBuilder().setName("stations/" + stationId + "/bikes").build()).getValue().unpack(Int32Value.class).getValue();

            BikeResponse response;

            if (!userHasBike) {
                response = BikeResponse.newBuilder().setResponse(BikeResponse.Response.NO_BIKE_REQUESTED).build();
            }
            else if (stationCapacity-bikesInStation == 0) {
                response = BikeResponse.newBuilder().setResponse(BikeResponse.Response.STATION_IS_FULL).build();
            }
            else {
                int userBalance = frontend.read(Rec.ReadRequest.newBuilder().setName("users/" + username + "/balance").build()).getValue().unpack(Int32Value.class).getValue();
                int bikeReturns = frontend.read(Rec.ReadRequest.newBuilder().setName("stations/" + stationId + "/returns").build()).getValue().unpack(Int32Value.class).getValue();
                // register new values in rec after everything is verified
                frontend.write(Rec.WriteRequest.newBuilder().setName("users/" + username + "/bike").setValue(Any.pack(BoolValue.newBuilder().setValue(false).build())).build());
                frontend.write(Rec.WriteRequest.newBuilder().setName("stations/" + stationId + "/bikes").setValue(Any.pack(Int32Value.newBuilder().setValue(++bikesInStation).build())).build());
                frontend.write(Rec.WriteRequest.newBuilder().setName("users/" + username + "/balance").setValue(Any.pack(Int32Value.newBuilder().setValue(userBalance+station.getAward()).build())).build());
                frontend.write(Rec.WriteRequest.newBuilder().setName("stations/" + stationId + "/returns").setValue(Any.pack(Int32Value.newBuilder().setValue(++bikeReturns).build())).build());
                response = BikeResponse.newBuilder().setResponse(BikeResponse.Response.OK).build();
            }

            // Send a single response through the stream.
            responseObserver.onNext(response);
            // Notify the client that the operation has been completed.
            responseObserver.onCompleted();

        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught when transferring data between hub and rec: " + e);
        }
    }

    @Override
    public void infoStation(InfoStationRequest request, StreamObserver<InfoStationResponse> responseObserver) {

        String id = request.getStationId();
        Station station = hub.getStation(id);

        if (station == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Station not found").asRuntimeException());
            return;
        }

        InfoStationResponse.Builder builder = InfoStationResponse.newBuilder();
        builder.setName(station.getName());
        builder.setLatitude(station.getLatitude());
        builder.setLongitude(station.getLongitude());
        builder.setCapacity(station.getCapacity());
        builder.setAward(station.getAward());
        int bikes = 0;
        int requests = 0 ;
        int returns = 0;

        try {
            bikes = frontend.read(Rec.ReadRequest.newBuilder().setName("stations/" + id + "/bikes").build()).getValue().unpack(Int32Value.class).getValue();
            requests = frontend.read(Rec.ReadRequest.newBuilder().setName("stations/" + id + "/requests").build()).getValue().unpack(Int32Value.class).getValue();
            returns = frontend.read(Rec.ReadRequest.newBuilder().setName("stations/" + id + "/returns").build()).getValue().unpack(Int32Value.class).getValue();

        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught when transferring data between hub and rec: " + e);
            // FIXME throw responseObserver.onError()
        }


        builder.setBikes(bikes);
        builder.setPickups(requests);
        builder.setDeliveries(returns);

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

    @Override
    public void distance(DistanceRequest request, StreamObserver<DistanceResponse> responseObserver) {

        float lat = request.getLat();
        float lon = request.getLon();
        Station station = hub.getStation(request.getStationId());

        if (station == null) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Station not found").asRuntimeException());
        }

        else {
            Double d = station.haversine_distance(lat, lon);
            DistanceResponse response = DistanceResponse.newBuilder().setDistance(d.intValue()).build();

            // Send a single response through the stream.
            responseObserver.onNext(response);
            // Notify the client that the operation has been completed.
            responseObserver.onCompleted();
        }
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

        try {
            SysStatusResponse.Builder builder = SysStatusResponse.newBuilder();
            ArrayList<ZKRecord> paths = new ArrayList<>(frontend.getZkNaming().listRecords("/grpc/bicloin/hub"));

            //FIXME define ping in shared proto

            for (ZKRecord record : paths) {
                ManagedChannel channel = ManagedChannelBuilder.forTarget(record.getURI()).usePlaintext().build();
                HubServiceGrpc.HubServiceBlockingStub stub = HubServiceGrpc.newBlockingStub(channel);
                CtrlPingResponse response = stub.ctrlPing(CtrlPingRequest.newBuilder().setInput("OK").build());
                boolean res = response.getOutput().equals("OK");
                builder.addSequence(SysStatusResponse.Reply.newBuilder().setPath(record.getPath()).setStatus(res ? SysStatusResponse.Reply.Status.UP:SysStatusResponse.Reply.Status.DOWN).build());
                channel.shutdownNow();
            }

            // FIXME hardcode rec/1
            Rec.CtrlPingResponse response = frontend.ctrlPing(Rec.CtrlPingRequest.newBuilder().setInput("OK").build());
            builder.addSequence(SysStatusResponse.Reply.newBuilder().setPath("/grpc/bicloin/rec/1").setStatus(response.getOutput().equals("OK") ? SysStatusResponse.Reply.Status.UP:SysStatusResponse.Reply.Status.DOWN).build());

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        }  catch (ZKNamingException e) {
            System.out.println(e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Unable to communicate with zk").asRuntimeException());
        }

    }

    @Override
    public void ctrlReset(CtrlResetRequest request, StreamObserver<CtrlResetResponse> responseObserver) {
        String password = request.getPassword();

        if (!password.equals(RESET_PASSWORD)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Incorrect Password").asRuntimeException());
        }
        else {

            try {
                importUsers(frontend, true);
                importStations(frontend, true);

                CtrlResetResponse response = CtrlResetResponse.newBuilder().build();

                // Send a single response through the stream.
                responseObserver.onNext(response);
                // Notify the client that the operation has been completed.
                responseObserver.onCompleted();
            } catch (IOException ie) {
                System.err.println(String.valueOf(ie));
            }

        }
    }

}
