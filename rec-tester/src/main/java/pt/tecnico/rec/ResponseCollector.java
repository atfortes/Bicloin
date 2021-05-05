package pt.tecnico.rec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResponseCollector<Resp>{

    private double maxWeight;
    private volatile double success = 0;
    private volatile double fail = 0;
    private HashMap<String, Resp> responses = new HashMap<>();

    public ResponseCollector(double maxWeight){
        this.maxWeight = maxWeight;
    }

    synchronized void registerResponse(double weight, Resp response, String path){
        responses.put(path, response);
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

    public HashMap<String, Resp> getResponses(){
        return responses;
    }
}
