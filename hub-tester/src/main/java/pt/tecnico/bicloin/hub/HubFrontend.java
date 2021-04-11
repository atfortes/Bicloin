package pt.tecnico.bicloin.hub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.tecnico.bicloin.hub.grpc.HubServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;
import java.util.ArrayList;

public class HubFrontend implements AutoCloseable {

    ManagedChannel channel;
    HubServiceGrpc.HubServiceBlockingStub stub;

    public HubFrontend(String zooHost, int zooPort, String path) throws ZKNamingException {
        ZKNaming zkNaming = new ZKNaming(zooHost, String.valueOf(zooPort));
        findHub(zkNaming, path);
    }

    public void findHub(ZKNaming zk, String path) throws ZKNamingException {

        ArrayList<ZKRecord> records = new ArrayList<>(zk.listRecords(path));
        for (ZKRecord record : records) {
            if (connectHub(record.getURI())) {
                return;
            }
        }

        throw new ZKNamingException("No hub available");
    }

    public boolean connectHub(String hubURI) {
        channel = ManagedChannelBuilder.forTarget(hubURI).usePlaintext().build();
        stub = HubServiceGrpc.newBlockingStub(channel);
        CtrlPingResponse response = ctrlPing(CtrlPingRequest.newBuilder().setInput("OK").build());

        // FIXME timeout
        return response.getOutput().equals("OK");
    }

    public BalanceResponse balance(BalanceRequest request) {
        return stub.balance(request);
    }

    public BikeResponse bikeUp(BikeRequest request) {
        return stub.bikeUp(request);
    }

    public BikeResponse bikeDown(BikeRequest request) {
        return stub.bikeDown(request);
    }

    public InfoStationResponse infoStation(InfoStationRequest request) {
        return stub.infoStation(request);
    }

    public LocateStationResponse locateStation(LocateStationRequest request) {
        return stub.locateStation(request);
    }

    public CtrlPingResponse ctrlPing(CtrlPingRequest request) {
        return stub.ctrlPing(request);
    }

    public CtrlResetResponse ctrlReset(CtrlResetRequest request) { return stub.ctrlReset(request); }

    public SysStatusResponse sysStatus(SysStatusRequest request) {
        return stub.sysStatus(request);
    }

    public TopUpResponse topUp(TopUpRequest request) {
        return stub.topUp(request);
    }

    public DistanceResponse distance(DistanceRequest request) {
        return stub.distance(request);
    }

    @Override
    public final void close() {
        channel.shutdown();
    }
}
