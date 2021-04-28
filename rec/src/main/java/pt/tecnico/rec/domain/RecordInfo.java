package pt.tecnico.rec.domain;

import com.google.protobuf.Any;

import java.util.concurrent.ConcurrentHashMap;

/** Domain root, where state and behavior of the server are implemented. */
public class RecordInfo{

    private ConcurrentHashMap<String, RecObject> values = new ConcurrentHashMap<>();

    public RecObject getValue(String key){
        RecObject response =  values.get(key);

        if (response == null) {
            writeValue(key, 1,0, Any.newBuilder().build());
            response = values.get(key);
        }
        return response;
    }

    public void writeValue(String key, int seq, int cid, Any value){
        values.put(key,new RecObject(seq, cid, value));
    }
}