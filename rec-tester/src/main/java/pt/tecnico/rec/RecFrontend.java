package pt.tecnico.rec;

import com.google.common.cache.CacheBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.RecordServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RecFrontend implements AutoCloseable {

    final String PING_COUNTER_KEY = "ping_counter";
    final String READ_COUNTER_KEY = "read_counter";
    final String WRITE_COUNTER_KEY = "write_counter";
    final String TOTAL_COUNTER_KEY = "total_counter";
    final String PING_TIMER_KEY = "ping_timer";
    final String READ_TIMER_KEY = "read_timer";
    final String WRITE_TIMER_KEY = "write_timer";

    ZKNaming zk;
    String path;
    int cid;
    double maxWeight;
    long WAIT_TIME = 3000;
    final int STUB_UPDATE = 10;
    ConcurrentHashMap<String, Long> stats = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, RecWrapper> records = new ConcurrentHashMap<>();
    ConcurrentMap<String, Integer> seqCache = CacheBuilder.newBuilder().maximumSize(100L).<String, Integer>build().asMap();

    public RecFrontend(String zooHost, int zooPort, String path, int cid) throws ZKNamingException {

        zk = new ZKNaming(zooHost, String.valueOf(zooPort));
        setStubs(path);

        this.path = path;
        this.cid = cid;
    }

    public void setStubs(String path) {
        try {
            ArrayList<ZKRecord> zkRecords = new ArrayList<>(zk.listRecords(path));
            maxWeight = zkRecords.size();
            for (ZKRecord zkRecord : zkRecords) {
                if (!(records.containsKey(zkRecord.getPath()) && records.get(zkRecord.getPath()).getUri().equals(zkRecord.getURI()))) {
                    ManagedChannel channel = ManagedChannelBuilder.forTarget(zkRecord.getURI()).usePlaintext().build();
                    RecordServiceGrpc.RecordServiceStub stub = RecordServiceGrpc.newStub(channel);
                    records.put(zkRecord.getPath(), new RecWrapper(zkRecord.getURI(), channel, stub, 1.0));
                }
            }
        }
        catch (ZKNamingException e) {
            System.err.println("Caught exception during Zookeeper listing: " + e);
        }
    }

    public Rec.CtrlPingResponse ctrlPing(Rec.CtrlPingRequest request) {

        stats.merge(PING_COUNTER_KEY, 1L, Long::sum);
        stats.merge(TOTAL_COUNTER_KEY, 1L, Long::sum);
        long start = System.nanoTime();

        // update stub information from zookeeper
        if (stats.get(TOTAL_COUNTER_KEY) % STUB_UPDATE == 0) { setStubs(path); }

        ResponseCollector<Rec.CtrlPingResponse> collector = new ResponseCollector<>(1);

        SendRequestToAll<Rec.CtrlPingRequest, Rec.CtrlPingResponse> sender =
                new SendRequestToAll<>(request, collector, (RecordServiceGrpc.RecordServiceStub::ctrlPing));

        synchronized(collector) {
            try {
                sender.run();
                collector.wait(WAIT_TIME);
                if (!collector.quorum())
                    throw Status.UNAVAILABLE.withDescription("Ping was not answered").asRuntimeException();
                List<Rec.CtrlPingResponse> responses = new ArrayList<>(collector.getResponses().values());

                stats.merge(PING_TIMER_KEY, System.nanoTime()-start, Long::sum);
                return responses.get(0);
            } catch (InterruptedException e){
                throw Status.INTERNAL.withDescription("Quorum was not reached").asRuntimeException();
            }
        }
    }

    public Rec.ReadResponse read(Rec.ReadRequest request) {

        stats.merge(READ_COUNTER_KEY, 1L, Long::sum);
        stats.merge(TOTAL_COUNTER_KEY, 1L, Long::sum);
        long start = System.nanoTime();

        // update stub information from zookeeper
        if (stats.get(TOTAL_COUNTER_KEY) % STUB_UPDATE == 0) { setStubs(path); }

        ResponseCollector<Rec.ReadResponse> collector = new ResponseCollector<>(maxWeight);

        SendRequestToAll<Rec.ReadRequest, Rec.ReadResponse> sender =
                new SendRequestToAll<>(request, collector, (RecordServiceGrpc.RecordServiceStub::read));

        synchronized(collector) {
            try {
                sender.run();
                collector.wait(WAIT_TIME);

                if (collector.exceptionConsensus()) {
                    throw Status.INVALID_ARGUMENT.withDescription("Register not found").asRuntimeException();
                } else if (!collector.quorum()) {
                    throw Status.UNAVAILABLE.withDescription("Quorum was not reached").asRuntimeException();
                }

                HashMap<String, Rec.ReadResponse> responses = collector.getResponses();
                Rec.ReadResponse response = Collections.max(responses.values(), (first, second) -> {
                    int seqComparison = Integer.compare(first.getSeq(), second.getSeq());
                    if (seqComparison == 0) return Integer.compare(first.getCid(), second.getCid());
                    else return seqComparison;
                });
                seqCache.put(request.getName(), response.getSeq());

                // write back
                Rec.WriteRequest writeBackRequest = Rec.WriteRequest.newBuilder().setName(request.getName())
                        .setSeq(response.getSeq()).setCid(response.getCid()).setValue(response.getValue()).build();
                List<String> paths = new ArrayList<>();
                responses.forEach((k, v) -> { if (v.getSeq() < response.getSeq() || (v.getSeq() == response.getSeq() && v.getCid() < response.getCid())) paths.add(k); });
                if (!paths.isEmpty()) this.writeBack(writeBackRequest, paths);

                stats.merge(READ_TIMER_KEY, System.nanoTime()-start, Long::sum);
                return response;
            }
            catch (InterruptedException e) {
                throw new StatusRuntimeException(Status.INTERNAL);
            }
        }
    }

    public Rec.WriteResponse write(Rec.WriteRequest request) {

        stats.merge(WRITE_COUNTER_KEY, 1L, Long::sum);
        stats.merge(TOTAL_COUNTER_KEY, 1L, Long::sum);
        long start = System.nanoTime();

        // update stub information from zookeeper
        if (stats.get(TOTAL_COUNTER_KEY) % STUB_UPDATE == 0) { setStubs(path); }

        int seq;
        // check if most recent is in cache or if a read is needed
        if (seqCache.containsKey(request.getName())) { seq = seqCache.get(request.getName()) + 1; }
        else { seq = this.read(Rec.ReadRequest.newBuilder().setName(request.getName()).build()).getSeq() + 1; }
        //seq = this.read(Rec.ReadRequest.newBuilder().setName(request.getName()).build()).getSeq() + 1;

        Rec.WriteRequest newRequest = Rec.WriteRequest.newBuilder().setName(request.getName()).setSeq(seq).setCid(cid).setValue(request.getValue()).build();
        ResponseCollector<Rec.WriteResponse> collector = new ResponseCollector<>(maxWeight);

        SendRequestToAll<Rec.WriteRequest, Rec.WriteResponse> sender =
                new SendRequestToAll<>(newRequest, collector, (RecordServiceGrpc.RecordServiceStub::write));

        synchronized(collector) {
            sender.run();
            try {
                collector.wait(WAIT_TIME);

                if (collector.exceptionConsensus()) {
                    throw Status.INVALID_ARGUMENT.withDescription("Register not found").asRuntimeException();
                } else if (!collector.quorum()) {
                    throw Status.UNAVAILABLE.withDescription("Quorum was not reached").asRuntimeException();
                }

                List<Rec.WriteResponse> responses = new ArrayList<>(collector.getResponses().values());
                seqCache.put(request.getName(), seq);

                stats.merge(WRITE_TIMER_KEY, System.nanoTime()-start, Long::sum);
                return responses.get(0);
            }
            catch (InterruptedException e) {
                throw new StatusRuntimeException(Status.INTERNAL);
            }
        }
    }

    public void writeBack(Rec.WriteRequest request, List<String> paths) {

        stats.merge(WRITE_COUNTER_KEY, 1L, Long::sum);
        stats.merge(TOTAL_COUNTER_KEY, 1L, Long::sum);
        long start = System.nanoTime();

        // update stub information from zookeeper
        if (stats.get(TOTAL_COUNTER_KEY) % STUB_UPDATE == 0) { setStubs(path); }

        for (String path : paths)
            records.get(path).getStub().write(request, new RecObserver<>(null, 0.0, "") );

        stats.merge(WRITE_TIMER_KEY, System.nanoTime()-start, Long::sum);
    }

    public ZKNaming getZkNaming() {
        return zk;
    }

    private class SendRequestToAll<Req, Resp>{
        Req req;

        RequestCall<Req,Resp> fn;

        ResponseCollector<Resp> collector;

        SendRequestToAll(Req req, ResponseCollector<Resp> collector, RequestCall<Req,Resp> fn) {
            this.req = req;
            this.fn = fn;
            this.collector = collector;
        }

        public void run() {
            records.forEach((k, v) -> fn.sendRequestToStub(v.getStub(), req, new RecObserver<>(collector, v.getWeight(), k)));
        }
    }

    private static class RecWrapper {
        String uri;
        ManagedChannel channel;
        RecordServiceGrpc.RecordServiceStub stub;
        double weight;

        RecWrapper(String uri, ManagedChannel channel, RecordServiceGrpc.RecordServiceStub stub, double weight) {
            this.uri = uri;
            this.channel = channel;
            this.stub = stub;
            this.weight = weight;
        }

        public String getUri() {
            return uri;
        }

        public ManagedChannel getChannel() {
            return channel;
        }

        public RecordServiceGrpc.RecordServiceStub getStub() {
            return stub;
        }

        public double getWeight() {
            return weight;
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
            System.out.println("[INFO] pings: " + pingCount + ", avg_time: " + stats.get(PING_TIMER_KEY)/pingCount/1000000 + "ms");
        } else { System.out.println("[INFO] pings: 0"); }
        if (stats.containsKey(READ_COUNTER_KEY)) {
            long readCount = stats.get(READ_COUNTER_KEY);
            System.out.println("[INFO] reads: " + readCount + ", avg_time: " + stats.get(READ_TIMER_KEY)/readCount/1000000 + "ms");
        } else { System.out.println("[INFO] reads: 0"); }
        if (stats.containsKey(WRITE_COUNTER_KEY)) {
            long writeCount = stats.get(WRITE_COUNTER_KEY);
            System.out.println("[INFO] writes: " + writeCount + ", avg_time: " + stats.get(WRITE_TIMER_KEY)/writeCount/1000000 + "ms");
        } else { System.out.println("[INFO] writes: 0"); }

        for (RecWrapper record : records.values()) {
            record.getChannel().shutdown();
        }
    }
}
