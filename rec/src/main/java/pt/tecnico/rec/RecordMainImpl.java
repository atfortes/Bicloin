
package pt.tecnico.rec;

import java.util.logging.Logger;

import com.google.protobuf.Any;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.rec.domain.RecObject;
import pt.tecnico.rec.domain.RecordInfo;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.RecordServiceGrpc;

public class RecordMainImpl extends RecordServiceGrpc.RecordServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(RecordMainImpl.class.getName());

    private final RecordInfo rec = new RecordInfo();

    @Override
    public void read(Rec.ReadRequest request, StreamObserver<Rec.ReadResponse> responseObserver) {
        String name = request.getName();
        LOGGER.info(String.format("Received Read: %s", name));
        if (!isNameValid(name)){
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Name not valid").asRuntimeException());
            return;
        }

        RecObject object = rec.getValue(name);
        Any value = object.getValue();
        int seq = object.getSeq();
        int cid = object.getCid();

        var replyBuilder = Rec.ReadResponse.newBuilder().setSeq(seq).setCid(cid);
        if (value != null) { replyBuilder.setValue(value); }

        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void write(Rec.WriteRequest request, StreamObserver<Rec.WriteResponse> responseObserver) {
        LOGGER.info("Received Write");
        String name = request.getName();
        if (!isNameValid(name)){
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Name not valid").asRuntimeException());
            return;
        }
        int seq = request.getSeq();
        int cid = request.getCid();
        Any value = request.getValue();

        RecObject currentObject = rec.getValue(name);
        int currentSeq = currentObject.getSeq(); // FIXME this can be improved for efficiency purposes
        int currentCid = currentObject.getCid();

        if (!((seq > currentSeq) || ((seq == currentSeq) && (cid >= currentCid)))) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Tag not valid").asRuntimeException());
            return;
        }

        rec.writeValue(name, seq, cid, value);
        LOGGER.info("Write Success");
        responseObserver.onNext(Rec.WriteResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void ctrlPing(Rec.CtrlPingRequest request, StreamObserver<Rec.CtrlPingResponse> responseObserver) {
        LOGGER.info("Received Ping");
        String input = request.getInput();
        responseObserver.onNext(Rec.CtrlPingResponse.newBuilder().setOutput(input).build());
        responseObserver.onCompleted();
    }

    public boolean isNameValid(String name){
        return !name.isEmpty();
    }
}