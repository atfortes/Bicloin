
package pt.tecnico.rec;

import java.util.logging.Logger;

import com.google.protobuf.Any;
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
        LOGGER.info("Read: " + name);
        Any value = rec.getValue(name);
        LOGGER.info("Value: " + value);
        if (value == null) {
            responseObserver.onNext(Rec.ReadResponse.newBuilder().build());
        }
        else {
            responseObserver.onNext(Rec.ReadResponse.newBuilder().setValue(value).build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void write(Rec.WriteRequest request, StreamObserver<Rec.WriteResponse> responseObserver) {
        LOGGER.info("Received Write");
        String name = request.getName();
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

}