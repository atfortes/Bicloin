package pt.tecnico.rec;

import java.util.ArrayList;
import java.util.List;

public class ResponseCollector<Resp>{

    private double maxWeight;
    private volatile double success = 0;
    private volatile double fail = 0;
    private List<Resp> responses= new ArrayList<>();

    public ResponseCollector(double maxWeight){
        this.maxWeight = maxWeight;
    }

    synchronized void registerResponse(double weight, Resp response){
        responses.add(response);
        success += weight;
        if (quorum())
            this.notifyAll();
    }

    synchronized void registerException(double weight){
        fail += weight;
        if (exceptionConsensus())
            this.notifyAll();
    }

    public boolean quorum() {
        return success > maxWeight/2;
    }

    public boolean exceptionConsensus() {
        return fail > maxWeight/2;
    }

    public List<Resp> getResponses(){
        return responses;
    }
}
