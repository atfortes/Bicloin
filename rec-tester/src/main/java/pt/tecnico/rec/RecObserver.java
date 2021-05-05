package pt.tecnico.rec;

import io.grpc.stub.StreamObserver;

public class RecObserver<R> implements StreamObserver<R> {

    ResponseCollector<R> collector;
    double weight = 1;
    String path;

    RecObserver(ResponseCollector<R> collector, Double weight, String path) {
        this.collector = collector;
        this.weight = weight;
        this.path = path;
    }

    @Override
    public void onNext(R r) {
        collector.registerResponse(weight, r, path);
    }

    @Override
    public void onError(Throwable throwable) {
        collector.registerException(weight);
    }

    @Override
    public void onCompleted() {}
}