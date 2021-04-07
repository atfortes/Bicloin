package pt.tecnico.bicloin.hub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.bicloin.hub.grpc.*;
import pt.tecnico.bicloin.hub.grpc.HubServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;

public class HubFrontend implements AutoCloseable {

    final ManagedChannel channel;
    HubServiceGrpc.HubServiceBlockingStub stub;

    public HubFrontend(String zooHost, int zooPort, String path) throws ZKNamingException {
        // FIXME path unknown at this point
        // implement find hub here?

        ZKNaming zkNaming = new ZKNaming(zooHost, String.valueOf(zooPort));
        ZKRecord hub = zkNaming.lookup(path);
        String target = hub.getURI();
        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        stub = HubServiceGrpc.newBlockingStub(channel);
    }

    public BalanceResponse balance(BalanceRequest request) { return stub.balance(request); }

    public BikeResponse bikeUp(BikeRequest request) { return stub.bikeUp(request); }

    public BikeResponse bikeDown(BikeRequest request) {return stub.bikeDown(request);}

    public InfoStationResponse infoStation(InfoStationRequest request) { return  stub.infoStation(request);}

    public LocateStationResponse locateStation(LocateStationRequest request) { return stub.locateStation(request);}

    public CtrlPingResponse ctrlPing(CtrlPingRequest request) {
        return stub.ctrlPing(request);
    }

    public SysStatusResponse sysStatus(SysStatusRequest request) {
        return stub.sysStatus(request);
    }

    public TopUpResponse topUp(TopUpRequest request) {return stub.topUp(request);}

    public DistanceResponse distance(DistanceRequest request) {return stub.distance(request);}


    @Override
    public final void close() {
        channel.shutdown();
    }
}
