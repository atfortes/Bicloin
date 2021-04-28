package pt.tecnico.rec;

import java.util.ArrayList;
import java.util.List;

public class ResponseCollector<Resp>{
    private double maxWeight;

    private volatile double counter = 0;

    private List<Resp> responses= new ArrayList<>();

    ResponseCollector(double maxWeight){
        this.maxWeight = maxWeight;
    }

    synchronized void registerResponse(double weight, Resp response){
        responses.add(response);
        counter += weight;
        if (counter > maxWeight/2){ this.notifyAll(); }
    }

    public List<Resp> getResponses(){
        return responses;
    }
}
