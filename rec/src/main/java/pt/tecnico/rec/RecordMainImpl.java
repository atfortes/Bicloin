
package pt.tecnico.rec;

import java.util.logging.Logger;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessage;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import pt.tecnico.rec.domain.RecordInfo;
import pt.tecnico.rec.grpc.Rec;
import pt.tecnico.rec.grpc.RecordServiceGrpc;

public class RecordMainImpl extends RecordServiceGrpc.RecordServiceImplBase {

    private static final Logger LOGGER = Logger.getLogger(RecordMainImpl.class.getName());

    private final RecordInfo rec = new RecordInfo();

    @Override
    public void read(Rec.ReadRequest request, StreamObserver<Rec.ReadResponse> responseObserver) {
        LOGGER.info("Received Read");
        String name = request.getName();
        if (!isNameValid(name)){
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Name not valid").asRuntimeException());
            return;
        }
        LOGGER.info("Read: " + name);
        Any value = rec.getValue(name);
        LOGGER.info("Value: " + value);
        var builder = Rec.ReadResponse.newBuilder();

        if (value == null) { responseObserver.onNext(builder.build());}
        else { responseObserver.onNext(builder.setValue(value).build());}

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
        Any value = request.getValue();
        LOGGER.info("Write: (" + name + ", " + value + ")");
        rec.writeValue(name,value);
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
        if (name==null){ return false;}
        return true;
    }
}