package pt.tecnico.rec;

import io.grpc.stub.StreamObserver;

public class RecObserver<R> implements StreamObserver<R> {

    ResponseCollector<R> collector;

    double weight = 1;

    RecObserver(ResponseCollector<R> collector, Double weight){
        this.collector = collector;
        this.weight = weight;
    }

    @Override
    public void onNext(R r) {
        System.out.println("Received response: " + r);
        collector.registerResponse(weight, r);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Received error: " + throwable);
    }

    @Override
    public void onCompleted() {
        System.out.println("Request completed");
    }
}