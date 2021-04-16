package pt.tecnico.bicloin.hub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.tecnico.bicloin.hub.domain.*;
import pt.tecnico.rec.RecFrontend;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.RecordServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;

import static pt.tecnico.bicloin.hub.HubMain.importStations;
import static pt.tecnico.bicloin.hub.HubMain.importUsers;

public class HubImpl extends HubServiceGrpc.HubServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(HubImpl.class.getName());

    private final int EURO2BIC = 10;
    private final String RESET_PASSWORD = "super_strong_reset_password";
    private final HubInfo hub = new HubInfo();
    private final RecFrontend frontend;

    public HubImpl(List<User> userList, List<Station> stationList, RecFrontend frontend) {
        hub.setUsers(userList);
        hub.setStations(stationList);
        this.frontend = frontend;
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {

        LOGGER.info("Received balance");

        String username = request.getUsername();
        if (!userExists(username)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("ERRO utlizador não encontrado").asRuntimeException());
            return;
        }

        try {

            Rec.ReadResponse res = frontend.read(Rec.ReadRequest.newBuilder().setName("users/" + username + "/balance").build());
            Int32Value balance = res.getValue().unpack(Int32Value.class);
            BalanceResponse response = BalanceResponse.newBuilder().setBalance(balance.getValue()).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Rec took too long to answer").asRuntimeException());
        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught exception when transferring data between hub and rec: " + e);
            responseObserver.onError(Status.DATA_LOSS.withDescription("Failure transferring data between hub and rec").asRuntimeException());
        }
    }

    @Override
    public void topUp(TopUpRequest request, StreamObserver<TopUpResponse> responseObserver) {

        LOGGER.info("Received topUp");

        String username = request.getUsername();
        if (!userExists(username)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("ERRO utilizador não encontrado").asRuntimeException());
            return;
        }

        if (!request.getPhoneNumber().equals(hub.getUser(username).getPhone())) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("ERRO número de telemóvel incorreto").asRuntimeException());
            return;
        }

        int amount = request.getAmount();
        if (!(1 <= amount && amount <= 20)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("ERRO quantia fora do intervalo 1-20").asRuntimeException());
            return;
        }

        try {

            if (Context.current().isCancelled()) {
                responseObserver.onError(Status.CANCELLED.withDescription("Cancelled by client").asRuntimeException());
                return;
            }

            Rec.ReadResponse res = frontend.read(Rec.ReadRequest.newBuilder().setName("users/" + username + "/balance").build());
            Int32Value balance = res.getValue().unpack(Int32Value.class);
            Int32Value newBalance = Int32Value.newBuilder().setValue(balance.getValue() + amount*EURO2BIC).build();
            frontend.write(Rec.WriteRequest.newBuilder().setName("users/" + username + "/balance").setValue(Any.pack(newBalance)).build());
            TopUpResponse response = TopUpResponse.newBuilder().setBalance(newBalance.getValue()).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Rec took too long to answer").asRuntimeException());
        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught exception when transferring data between hub and rec: " + e);
            responseObserver.onError(Status.DATA_LOSS.withDescription("Failure transferring data between hub and rec").asRuntimeException());
        }
    }

    @Override
    public void bikeUp(BikeRequest request, StreamObserver<BikeResponse> responseObserver) {

        LOGGER.info("Received bikeUp");

        String username = request.getUsername();
        if (!userExists(username)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("ERRO utilizador não encontrado").asRuntimeException());
            return;
        }

        String stationId = request.getStationId();
        if (!stationExists(stationId)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("ERRO estação desconhecida").asRuntimeException());
            return;
        }

        if (!validDistance(stationId, request.getLatitude(), request.getLongitude())) {
            responseObserver.onNext(BikeResponse.newBuilder().setResponse(BikeResponse.Response.OUT_OF_RANGE).build());
            responseObserver.onCompleted();
            return;
        }

        try {

            if (Context.current().isCancelled()) {
                responseObserver.onError(Status.CANCELLED.withDescription("Cancelled by client").asRuntimeException());
                return;
            }

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

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Rec took too long to answer").asRuntimeException());
        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught exception when transferring data between hub and rec: " + e);
            responseObserver.onError(Status.DATA_LOSS.withDescription("Failure transferring data between hub and rec").asRuntimeException());
        }
    }

    @Override
    public void bikeDown(BikeRequest request, StreamObserver<BikeResponse> responseObserver) {

        LOGGER.info("Received bikeDown");

        String username = request.getUsername();
        if (!userExists(username)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("User not found").asRuntimeException());
            return;
        }

        String stationId = request.getStationId();
        if (!stationExists(stationId)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Station not found").asRuntimeException());
            return;
        }

        if (!validDistance(stationId, request.getLatitude(), request.getLongitude())) {
            responseObserver.onNext(BikeResponse.newBuilder().setResponse(BikeResponse.Response.OUT_OF_RANGE).build());
            responseObserver.onCompleted();
            return;
        }

        try {

            if (Context.current().isCancelled()) {
                responseObserver.onError(Status.CANCELLED.withDescription("Cancelled by client").asRuntimeException());
                return;
            }

            Station station = hub.getStation(stationId);
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

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Rec took too long to answer").asRuntimeException());
        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught exception when transferring data between hub and rec: " + e);
            responseObserver.onError(Status.DATA_LOSS.withDescription("Failure transferring data between hub and rec").asRuntimeException());
        }
    }

    @Override
    public void infoStation(InfoStationRequest request, StreamObserver<InfoStationResponse> responseObserver) {

        LOGGER.info("Received infoStation");

        String stationId = request.getStationId();
        if (!stationExists(stationId)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Station not found").asRuntimeException());
            return;
        }

        Station station = hub.getStation(stationId);
        InfoStationResponse.Builder builder = InfoStationResponse.newBuilder();
        builder.setName(station.getName());
        builder.setLatitude(station.getLatitude());
        builder.setLongitude(station.getLongitude());
        builder.setCapacity(station.getCapacity());
        builder.setAward(station.getAward());

        try {

            // no need to verify client cancellation, innocuous function
            int bikes = frontend.read(Rec.ReadRequest.newBuilder().setName("stations/" + stationId + "/bikes").build()).getValue().unpack(Int32Value.class).getValue();
            int requests = frontend.read(Rec.ReadRequest.newBuilder().setName("stations/" + stationId + "/requests").build()).getValue().unpack(Int32Value.class).getValue();
            int returns = frontend.read(Rec.ReadRequest.newBuilder().setName("stations/" + stationId + "/returns").build()).getValue().unpack(Int32Value.class).getValue();

            builder.setBikes(bikes);
            builder.setPickups(requests);
            builder.setDeliveries(returns);

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Rec took too long to answer").asRuntimeException());

        } catch (InvalidProtocolBufferException e) {
            System.err.println("Caught exception when transferring data between hub and rec: " + e);
            responseObserver.onError(Status.DATA_LOSS.withDescription("Failure transferring data between hub and rec").asRuntimeException());
        }
    }

    @Override
    public void locateStation(LocateStationRequest request, StreamObserver<LocateStationResponse> responseObserver) {

        LOGGER.info("Received locateStation");

        int k = request.getK();
        double lat = request.getLatitude();
        double lon = request.getLongitude();

        LocateStationResponse response = LocateStationResponse.newBuilder()
                .addAllIds(hub.sort_stations(k, lat, lon))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void distance(DistanceRequest request, StreamObserver<DistanceResponse> responseObserver) {

        LOGGER.info("Received distance");

        if (!stationExists(request.getStationId())) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("ERRO estação desconhecida").asRuntimeException());
            return;
        }

        double lat = request.getLat();
        double lon = request.getLon();
        Station station = hub.getStation(request.getStationId());
        double d = station.haversine_distance(lat, lon);
        DistanceResponse response = DistanceResponse.newBuilder().setDistance((int) d).build();

        responseObserver.onNext(response);
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

        try {
            ArrayList<ZKRecord> hubPaths = new ArrayList<>(frontend.getZkNaming().listRecords("/grpc/bicloin/hub"));
            ArrayList<ZKRecord> recPaths = new ArrayList<>(frontend.getZkNaming().listRecords("/grpc/bicloin/rec"));
            SysStatusResponse.Builder builder = SysStatusResponse.newBuilder();

            for (ZKRecord record : hubPaths) {
                ManagedChannel channel = ManagedChannelBuilder.forTarget(record.getURI()).usePlaintext().build();
                HubServiceGrpc.HubServiceBlockingStub stub = HubServiceGrpc.newBlockingStub(channel);
                SysStatusResponse.Reply.Builder replyBuilder = SysStatusResponse.Reply.newBuilder();
                replyBuilder.setPath(record.getPath());

                try {
                    CtrlPingResponse response = stub.withDeadlineAfter(1, TimeUnit.SECONDS).ctrlPing(CtrlPingRequest.newBuilder().setInput("OK").build());
                    replyBuilder.setStatus(response.getOutput().equals("OK") ? SysStatusResponse.Reply.Status.UP:SysStatusResponse.Reply.Status.DOWN);
                } catch (StatusRuntimeException e) {
                    replyBuilder.setStatus(SysStatusResponse.Reply.Status.DOWN);
                } finally {
                    builder.addSequence(replyBuilder.build());
                    channel.shutdown();
                }
            }

            for (ZKRecord record : recPaths) {
                ManagedChannel channel = ManagedChannelBuilder.forTarget(record.getURI()).usePlaintext().build();
                RecordServiceGrpc.RecordServiceBlockingStub stub = RecordServiceGrpc.newBlockingStub(channel);
                SysStatusResponse.Reply.Builder replyBuilder = SysStatusResponse.Reply.newBuilder();
                replyBuilder.setPath(record.getPath());

                try {
                    Rec.CtrlPingResponse response = stub.withDeadlineAfter(1, TimeUnit.SECONDS).ctrlPing(Rec.CtrlPingRequest.newBuilder().setInput("OK").build());
                    replyBuilder.setStatus(response.getOutput().equals("OK") ? SysStatusResponse.Reply.Status.UP:SysStatusResponse.Reply.Status.DOWN);
                } catch (StatusRuntimeException e) {
                    replyBuilder.setStatus(SysStatusResponse.Reply.Status.DOWN);
                } finally {
                    builder.addSequence(replyBuilder.build());
                    channel.shutdown();
                }
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        }  catch (ZKNamingException e) {
            System.out.println(e.getMessage());
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Erro a comunicar com o ZKNaming ").asRuntimeException());
        }

    }

    @Override
    public void ctrlReset(CtrlResetRequest request, StreamObserver<CtrlResetResponse> responseObserver) {

        String password = request.getPassword();
        if (!password.equals(RESET_PASSWORD)) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Incorrect Password").asRuntimeException());
            return;
        }

        try {
            importUsers(frontend, true);
            importStations(frontend, true);

            CtrlResetResponse response = CtrlResetResponse.newBuilder().build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (IOException | ImportDataException ie) {
            System.err.println(ie.getMessage());
            responseObserver.onError(Status.INTERNAL.withDescription("Failed to reset the values").asRuntimeException());
        } catch (StatusRuntimeException e) {
            System.err.println("Caught exception with description: " + e.getStatus().getDescription());
            responseObserver.onError(Status.DEADLINE_EXCEEDED.withDescription("Rec took too long to answer").asRuntimeException());
        }

    }

    private boolean userExists(String username) {
        return hub.getUser(username) != null;
    }

    private boolean stationExists(String stationId) {
        return hub.getStation(stationId) != null;
    }

    private boolean validDistance(String stationId, double latitude, double longitude) {
        return hub.getStation(stationId).haversine_distance(latitude, longitude) < 200;
    }

}
