package pt.tecnico.rec;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.RecordServiceGrpc;
import pt.ulisboa.tecnico.sdis.zk.*;

import java.util.ArrayList;
import java.util.Collections;
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
    String path;
    int cid;
    double maxWeight;
    long WAIT_TIME = 3000;
    ConcurrentHashMap<String, Long> stats = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, RecWrapper> records = new ConcurrentHashMap<>();

    public RecFrontend(String zooHost, int zooPort, String path, int cid) throws ZKNamingException {

        zk = new ZKNaming(zooHost, String.valueOf(zooPort));
        setStubs(path);

        this.path = path;
        this.cid = cid;
    }

    public void setStubs(String path) throws ZKNamingException {
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

    public Rec.CtrlPingResponse ctrlPing(Rec.CtrlPingRequest request) {

        try {
            setStubs(path);
        }
        catch (ZKNamingException e) {
            throw new StatusRuntimeException(Status.INTERNAL);
        }

        stats.merge(PING_COUNTER_KEY, 1L, Long::sum);
        long start = System.nanoTime();

        ResponseCollector<Rec.CtrlPingResponse> collector = new ResponseCollector<>(1);

        SendRequestToAll<Rec.CtrlPingRequest, Rec.CtrlPingResponse> sender =
                new SendRequestToAll<>(request, collector, (RecordServiceGrpc.RecordServiceStub::ctrlPing));

        synchronized(collector) {
            try {
                sender.run();
                collector.wait(WAIT_TIME);
                if (!collector.quorum())
                    throw new StatusRuntimeException(Status.UNAVAILABLE);
                var responses = collector.getResponses();

                stats.merge(PING_TIMER_KEY, System.nanoTime()-start, Long::sum);
                return responses.get(0);
            } catch (InterruptedException e){
                throw new StatusRuntimeException(Status.INTERNAL);
            }
        }
    }

    public Rec.ReadResponse read(Rec.ReadRequest request) {

        try {
            setStubs(path);
        }
        catch (ZKNamingException e) {
            throw new StatusRuntimeException(Status.INTERNAL);
        }

        stats.merge(READ_COUNTER_KEY, 1L, Long::sum);
        long start = System.nanoTime();

        ResponseCollector<Rec.ReadResponse> collector = new ResponseCollector<>(maxWeight);

        SendRequestToAll<Rec.ReadRequest, Rec.ReadResponse> sender =
                new SendRequestToAll<>(request, collector, (RecordServiceGrpc.RecordServiceStub::read));

        synchronized(collector) {
            try {
                sender.run();
                collector.wait(WAIT_TIME);

                if (collector.exceptionConsensus()) {
                    throw new StatusRuntimeException(Status.INVALID_ARGUMENT);
                } else if (!collector.quorum()) {
                    throw new StatusRuntimeException(Status.UNAVAILABLE);
                }

                Rec.ReadResponse response = Collections.max(collector.getResponses(), (first, second) -> {
                    int seqComparison = Integer.compare(first.getSeq(), second.getSeq());
                    if (seqComparison == 0) return Integer.compare(first.getCid(), second.getCid());
                    else return seqComparison;
                });

                // write back
                Rec.WriteRequest writeBackRequest = Rec.WriteRequest.newBuilder().setName(request.getName())
                        .setSeq(response.getSeq()).setCid(response.getCid()).setValue(response.getValue()).build();
                this.write(writeBackRequest);

                stats.merge(READ_TIMER_KEY, System.nanoTime()-start, Long::sum);
                return response;
            }
            catch (InterruptedException e) {
                throw new StatusRuntimeException(Status.INTERNAL);
            }
        }
    }

    public Rec.WriteResponse write(Rec.WriteRequest request) {

        try {
            setStubs(path);
        }
        catch (ZKNamingException e) {
            throw new StatusRuntimeException(Status.INTERNAL);
        }

        stats.merge(WRITE_COUNTER_KEY, 1L, Long::sum);
        long start = System.nanoTime();

        int seq = request.getSeq();
        if (seq == 0) seq = this.read(Rec.ReadRequest.newBuilder().setName(request.getName()).build()).getSeq() + 1;

        Rec.WriteRequest newRequest = Rec.WriteRequest.newBuilder().setName(request.getName()).setSeq(seq).setCid(cid).setValue(request.getValue()).build();
        ResponseCollector<Rec.WriteResponse> collector = new ResponseCollector<>(maxWeight);

        SendRequestToAll<Rec.WriteRequest, Rec.WriteResponse> sender =
                new SendRequestToAll<>(newRequest, collector, (RecordServiceGrpc.RecordServiceStub::write));

        synchronized(collector) {
            sender.run();
            try {
                collector.wait(WAIT_TIME);

                if (collector.exceptionConsensus()) {
                    throw new StatusRuntimeException(Status.INVALID_ARGUMENT);
                } else if (!collector.quorum()) {
                    throw new StatusRuntimeException(Status.UNAVAILABLE);
                }

                var responses = collector.getResponses();
                stats.merge(WRITE_TIMER_KEY, System.nanoTime()-start, Long::sum);
                return responses.get(0);
            }
            catch (InterruptedException e) {
                throw new StatusRuntimeException(Status.INTERNAL);
            }
        }
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
            for (RecWrapper record : records.values()) {
                fn.sendRequestToStub(record.getStub(), req, new RecObserver<>(collector, record.getWeight()));
            }
        }
    }

    private class RecWrapper {
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
