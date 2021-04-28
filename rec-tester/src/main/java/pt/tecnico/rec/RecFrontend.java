package pt.tecnico.rec;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.RecordServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class RecFrontend implements AutoCloseable {

    List<ManagedChannel> channels = new ArrayList<>();
    List<RecordServiceGrpc.RecordServiceStub> stubs = new ArrayList<>();
    List<Double> weights = new ArrayList<>();
    ZKNaming zk;
    double maxWeight = 1;
    long WAIT_TIME = 50000000;
    int cid = 1;

    public RecFrontend(String zooHost, int zooPort, String path) throws ZKNamingException {
        zk = new ZKNaming(zooHost, String.valueOf(zooPort));
        ArrayList<ZKRecord> records = new ArrayList<>(zk.listRecords(path));
        maxWeight = records.size();
        for (ZKRecord record : records) {
                ManagedChannel channel = ManagedChannelBuilder.forTarget(record.getURI()).usePlaintext().build();
                channels.add(channel);
                RecordServiceGrpc.RecordServiceStub stub = RecordServiceGrpc.newStub(channel);
                stubs.add(stub);
                weights.add(1.0);
        }
    }

    public Rec.CtrlPingResponse ctrlPing(Rec.CtrlPingRequest request) {
        ResponseCollector<Rec.CtrlPingResponse> collector = new ResponseCollector<Rec.CtrlPingResponse>(maxWeight);

        SendRequestToAll<Rec.CtrlPingRequest, Rec.CtrlPingResponse> sender =
                new SendRequestToAll<Rec.CtrlPingRequest, Rec.CtrlPingResponse>(request, collector,
                        ((stub, req, observer) -> {stub.withDeadlineAfter(1, TimeUnit.SECONDS).ctrlPing(req,observer);}));
        synchronized(collector) {
            sender.run();
            try {
                collector.wait(WAIT_TIME);
                var responses = collector.getResponses();
                if (responses.size() == 0) {} // FIXME should emit error
                return responses.get(0);
            }
            catch (Exception e){
                System.out.println("Caught exception: " + e.toString());
            }
        }
        return null;
    }

    public Rec.ReadResponse read(Rec.ReadRequest request) {
        ResponseCollector<Rec.ReadResponse> collector = new ResponseCollector<Rec.ReadResponse>(maxWeight);

        SendRequestToAll<Rec.ReadRequest, Rec.ReadResponse> sender =
                new SendRequestToAll<Rec.ReadRequest, Rec.ReadResponse>(request, collector,
                        ((stub, req, observer) -> {stub.withDeadlineAfter(1, TimeUnit.SECONDS).read(req,observer);}));

        synchronized(collector) {
            sender.run();
            try {
                collector.wait(WAIT_TIME);
                return collector.getResponses().stream().max(Comparator.comparing(el -> el.getSeq()*10 + el.getCid())).get();
            }
            catch (Exception e){
                System.out.println("Caught exception: " + e.toString());
            }
        }
        return null;
    }

    public Rec.WriteResponse write(Rec.WriteRequest request) {
        Rec.ReadRequest tagReq = Rec.ReadRequest.newBuilder().setName(request.getName()).build();
        Rec.ReadResponse tagResp = this.read(tagReq);
        int oldSeq = tagResp.getSeq();

        Rec.WriteRequest newRequest = Rec.WriteRequest.newBuilder().setName(request.getName()).setSeq(oldSeq + 1).setCid(cid).build();

        ResponseCollector<Rec.WriteResponse> collector = new ResponseCollector<Rec.WriteResponse>(maxWeight);

        SendRequestToAll<Rec.WriteRequest, Rec.WriteResponse> sender =
                new SendRequestToAll<Rec.WriteRequest, Rec.WriteResponse>(newRequest, collector,
                        ((stub, req, observer) -> {stub.withDeadlineAfter(1, TimeUnit.SECONDS).write(req,observer);}));
        synchronized(collector) {
            sender.run();
            try {
                collector.wait(WAIT_TIME);
                var responses = collector.getResponses();
                if (responses.size() == 0) {} // FIXME should emit error
                return responses.get(0);
            }
            catch (Exception e){
                System.out.println("Caught exception: " + e.toString());
            }
        }
        return null;
    }

    public ZKNaming getZkNaming() {
        return zk;
    }

    private class SendRequestToAll<Req, Resp>{
        Req req;

        RequestCall<Req,Resp> fn;

        ResponseCollector<Resp> collector;

        SendRequestToAll(Req req, ResponseCollector<Resp> collector, RequestCall<Req,Resp> fn){
            this.req = req;
            this.fn = fn;
            this.collector = collector;
        }

        public void run(){
            for (int i=0; i<stubs.size(); i++) {
                fn.sendRequestToStub(stubs.get(i), req, new RecObserver<Resp>(collector, weights.get(i)));
            }
        }
    }

    @FunctionalInterface
    public interface RequestCall<Req,Ret>{
        void sendRequestToStub(RecordServiceGrpc.RecordServiceStub stub, Req request, RecObserver<Ret> observer);
    }


    @Override
    public final void close() {
        for (ManagedChannel channel : channels){
            channel.shutdown();
        }
    }
}
