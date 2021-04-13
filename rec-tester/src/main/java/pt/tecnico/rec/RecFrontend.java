package pt.tecnico.rec;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.RecordServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

public class RecFrontend implements AutoCloseable {

    final ManagedChannel channel;
    RecordServiceGrpc.RecordServiceBlockingStub stub;
    ZKNaming zk;

    public RecFrontend(String zooHost, int zooPort, String path) throws ZKNamingException {
        zk = new ZKNaming(zooHost, String.valueOf(zooPort));
        channel = ManagedChannelBuilder.forTarget(zk.lookup(path).getURI()).usePlaintext().build();
        stub = RecordServiceGrpc.newBlockingStub(channel);

        if (!ctrlPing(Rec.CtrlPingRequest.newBuilder().setInput("OK").build()).getOutput().equals("OK")) {
            throw new ZKNamingException("Could not connect to Rec");
        }

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

    public ZKNaming getZkNaming() {
        return zk;
    }

    @Override
    public final void close() {
        channel.shutdown();
    }
}
