package pt.tecnico.rec;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.RecordServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RecFrontend implements AutoCloseable {

    final String PING_COUNTER_KEY = "ping_counter";
    final String READ_COUNTER_KEY = "read_counter";
    final String WRITE_COUNTER_KEY = "write_counter";
    final String PING_TIMER_KEY = "ping_timer";
    final String READ_TIMER_KEY = "read_timer";
    final String WRITE_TIMER_KEY = "write_timer";

    List<ManagedChannel> channels = new ArrayList<>();
    List<RecordServiceGrpc.RecordServiceStub> stubs = new ArrayList<>();
    List<Double> weights = new ArrayList<>();
    ZKNaming zk;
    int cid;
    double maxWeight = 1;
    long WAIT_TIME = 5000;
    private ConcurrentHashMap<String, Long> stats = new ConcurrentHashMap<>();

    public RecFrontend(String zooHost, int zooPort, String path, int cid) throws ZKNamingException {

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

        this.cid = cid;
    }

    // FIXME empty responses and exception handling
    public Rec.CtrlPingResponse ctrlPing(Rec.CtrlPingRequest request) {

        stats.merge(PING_COUNTER_KEY, 1L, Long::sum);
        long start = System.currentTimeMillis();

        ResponseCollector<Rec.CtrlPingResponse> collector = new ResponseCollector<>(maxWeight);

        SendRequestToAll<Rec.CtrlPingRequest, Rec.CtrlPingResponse> sender =
                new SendRequestToAll<>(request, collector, ((stub, req, observer) -> stub.ctrlPing(req, observer)));

        synchronized(collector) {
            sender.run();
            try {
                collector.wait(WAIT_TIME);
                var responses = collector.getResponses();
                if (responses.size() == 0) { return null; } // FIXME should emit error quorum

                stats.merge(PING_TIMER_KEY, System.currentTimeMillis()-start, Long::sum);
                return responses.get(0);
            }
            catch (InterruptedException e){
                System.err.println("Caught exception: " + e.toString());
            }
        }
        return null;
    }

    // FIXME exception handling
    public Rec.ReadResponse read(Rec.ReadRequest request) {

        stats.merge(READ_COUNTER_KEY, 1L, Long::sum);
        long start = System.currentTimeMillis();

        ResponseCollector<Rec.ReadResponse> collector = new ResponseCollector<>(maxWeight);

        SendRequestToAll<Rec.ReadRequest, Rec.ReadResponse> sender =
                new SendRequestToAll<>(request, collector, ((stub, req, observer) -> stub.read(req, observer)));
        
        synchronized(collector) {
            sender.run();
            try {
                collector.wait(WAIT_TIME);

                stats.merge(READ_TIMER_KEY, System.currentTimeMillis()-start, Long::sum);
                return collector.getResponses().stream().max(Comparator.comparing(el -> el.getSeq()*10 + el.getCid())).get();
            }
            catch (InterruptedException e){
                System.err.println("Caught exception: " + e.toString());
            }
        }
        return null;
    }

    // FIXME empty responses and exception handling
    public Rec.WriteResponse write(Rec.WriteRequest request) {

        stats.merge(WRITE_COUNTER_KEY, 1L, Long::sum);
        long start = System.currentTimeMillis();

        Rec.ReadRequest tagReq = Rec.ReadRequest.newBuilder().setName(request.getName()).build();
        Rec.ReadResponse tagResp = this.read(tagReq);
        int oldSeq = tagResp.getSeq();

        Rec.WriteRequest newRequest = Rec.WriteRequest.newBuilder().setName(request.getName()).setSeq(oldSeq + 1).setCid(cid).setValue(request.getValue()).build();

        ResponseCollector<Rec.WriteResponse> collector = new ResponseCollector<>(maxWeight);

        SendRequestToAll<Rec.WriteRequest, Rec.WriteResponse> sender =
                new SendRequestToAll<>(newRequest, collector, ((stub, req, observer) -> stub.write(req,observer)));

        synchronized(collector) {
            sender.run();
            try {
                collector.wait(WAIT_TIME);
                var responses = collector.getResponses();
                if (responses.size() == 0) { return null; } // FIXME should emit error quorum

                stats.merge(WRITE_TIMER_KEY, System.currentTimeMillis()-start, Long::sum);
                return responses.get(0);
            }
            catch (InterruptedException e){
                System.err.println("Caught exception: " + e.toString());
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

        System.out.println("Finished execution.");
        if (stats.containsKey(PING_COUNTER_KEY)) {
            long pingCount = stats.get(PING_COUNTER_KEY);
            System.out.println("[INFO] pings: " + pingCount + ", avg_time: " + stats.get(PING_TIMER_KEY)/pingCount + "ms");
        } else { System.out.println("[INFO] pings: 0"); }
        if (stats.containsKey(READ_COUNTER_KEY)) {
            long readCount = stats.get(READ_COUNTER_KEY);
            System.out.println("[INFO] reads: " + readCount + ", avg_time: " + stats.get(READ_TIMER_KEY)/readCount + "ms");
        } else { System.out.println("[INFO] reads: 0"); }
        if (stats.containsKey(WRITE_COUNTER_KEY)) {
            long writeCount = stats.get(WRITE_COUNTER_KEY);
            System.out.println("[INFO] writes: " + writeCount + ", avg_time: " + stats.get(WRITE_TIMER_KEY)/writeCount + "ms");
        } else { System.out.println("[INFO] writes: 0"); }

        for (ManagedChannel channel : channels) {
            channel.shutdown();
        }
    }
}
