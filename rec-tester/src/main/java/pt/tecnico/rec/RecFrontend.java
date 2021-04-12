package pt.tecnico.rec;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.RecordServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;
import java.util.concurrent.TimeUnit;

public class RecFrontend implements AutoCloseable {

    final ManagedChannel channel;
    RecordServiceGrpc.RecordServiceBlockingStub stub;
    ZKNaming zk;

    public RecFrontend(String zooHost, int zooPort, String path) throws ZKNamingException {
        zk = new ZKNaming(zooHost, String.valueOf(zooPort));
        channel = ManagedChannelBuilder.forTarget(zk.lookup(path).getURI()).usePlaintext().build();
        stub = RecordServiceGrpc.newBlockingStub(channel);
        //FIXME make sure rec responds?
    }

    public Rec.CtrlPingResponse ctrlPing(Rec.CtrlPingRequest request) {
        return stub.withDeadlineAfter(1, TimeUnit.SECONDS).ctrlPing(request);
    }

    public Rec.ReadResponse read(Rec.ReadRequest request) {
        return stub.withDeadlineAfter(1, TimeUnit.SECONDS).read(request);
    }

    public Rec.WriteResponse write(Rec.WriteRequest request) {
        return stub.withDeadlineAfter(1, TimeUnit.SECONDS).write(request);
    }

    // FIXME best implementation
    public ZKNaming getZkNaming() {
        return zk;
    }

    @Override
    public final void close() {
        channel.shutdown();
    }
}
