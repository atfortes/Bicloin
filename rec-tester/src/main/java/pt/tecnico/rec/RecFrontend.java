package pt.tecnico.rec;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.RecordServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;

public class RecFrontend implements AutoCloseable {

    final ManagedChannel channel;
    RecordServiceGrpc.RecordServiceBlockingStub stub;

    public RecFrontend(String zooHost, int zooPort) throws ZKNamingException {
        ZKNaming zkNaming = new ZKNaming(zooHost, String.valueOf(zooPort));
        ZKRecord record = zkNaming.lookup("/grpc/bicloin/rec/1");   // FIXME hard coded ?
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

    @Override
    public final void close() {
        channel.shutdown();
    }
}
