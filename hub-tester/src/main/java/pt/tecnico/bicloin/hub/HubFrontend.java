package pt.tecnico.bicloin.hub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.tecnico.bicloin.hub.grpc.HubServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class HubFrontend implements AutoCloseable {

    ManagedChannel channel;
    HubServiceGrpc.HubServiceBlockingStub stub;

    public HubFrontend(String zooHost, int zooPort, String path) throws ZKNamingException {
        ZKNaming zkNaming = new ZKNaming(zooHost, String.valueOf(zooPort));
        ArrayList<ZKRecord> records = new ArrayList<>(zkNaming.listRecords(path));
        for (ZKRecord record : records) {
            if (connectHub(record.getURI())) {
                return;
            }
        }

        throw new ZKNamingException("Nenhum hub disponível");
    }

    public boolean connectHub(String hubURI) {

        channel = ManagedChannelBuilder.forTarget(hubURI).usePlaintext().build();
        stub = HubServiceGrpc.newBlockingStub(channel);

        try {
            CtrlPingResponse response = stub.withDeadlineAfter(1, TimeUnit.SECONDS).ctrlPing(CtrlPingRequest.newBuilder().setInput("OK").build());
            return response.getOutput().equals("OK");

        } catch (StatusRuntimeException e) {
            // if hub fails to respond
            channel.shutdown();
            return false;
        }
    }

    public BalanceResponse balance(BalanceRequest request) {
        return stub.withDeadlineAfter(1, TimeUnit.SECONDS).balance(request);
    }

    public BikeResponse bikeUp(BikeRequest request) {
        return stub.withDeadlineAfter(3, TimeUnit.SECONDS).bikeUp(request);
    }

    public BikeResponse bikeDown(BikeRequest request) {
        return stub.withDeadlineAfter(3, TimeUnit.SECONDS).bikeDown(request);
    }

    public InfoStationResponse infoStation(InfoStationRequest request) {
        return stub.withDeadlineAfter(2, TimeUnit.SECONDS).infoStation(request);
    }

    public LocateStationResponse locateStation(LocateStationRequest request) {
        return stub.withDeadlineAfter(1, TimeUnit.SECONDS).locateStation(request);
    }

    public CtrlPingResponse ctrlPing(CtrlPingRequest request) {
        return stub.withDeadlineAfter(1, TimeUnit.SECONDS).ctrlPing(request);
    }

    public CtrlResetResponse ctrlReset(CtrlResetRequest request) {
        return stub.withDeadlineAfter(20, TimeUnit.SECONDS).ctrlReset(request);
    }

    public SysStatusResponse sysStatus(SysStatusRequest request) {
        return stub.withDeadlineAfter(1, TimeUnit.SECONDS).sysStatus(request);
    }

    public TopUpResponse topUp(TopUpRequest request) {
        return stub.withDeadlineAfter(1, TimeUnit.SECONDS).topUp(request);
    }

    public DistanceResponse distance(DistanceRequest request) {
        return stub.withDeadlineAfter(1, TimeUnit.SECONDS).distance(request);
    }

    @Override
    public final void close() {
        channel.shutdown();
    }
}
