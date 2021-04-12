package pt.tecnico.rec;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.RecordServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;

public class RecFrontend implements AutoCloseable {

    final ManagedChannel channel;
    RecordServiceGrpc.RecordServiceBlockingStub stub;
    ZKNaming zk;

    public RecFrontend(String zooHost, int zooPort, String path) throws ZKNamingException {
        zk = new ZKNaming(zooHost, String.valueOf(zooPort));
        ZKRecord record = zk.lookup(path);
        String target = record.getURI();
        channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        stub = RecordServiceGrpc.newBlockingStub(channel);
    }

    public Rec.CtrlPingResponse ctrlPing(Rec.CtrlPingRequest request) {
        return stub.ctrlPing(request);
    }

    public Rec.ReadResponse read(Rec.ReadRequest request) {
        return stub.read(request);
    }

    public Rec.WriteResponse write(Rec.WriteRequest request) {
        return stub.write(request);
    }

    // FIXME
    public ZKNaming getZkNaming() {
        return zk;
    }

    @Override
    public final void close() {
        channel.shutdown();
    }
}
