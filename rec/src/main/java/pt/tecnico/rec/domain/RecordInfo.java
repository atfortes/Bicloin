package pt.tecnico.rec.domain;

import com.google.protobuf.Any;

import java.util.concurrent.ConcurrentHashMap;

/** Domain root, where state and behavior of the server are implemented. */
public class RecordInfo{

    private ConcurrentHashMap<String, Any> values = new ConcurrentHashMap<>();

    public Any getValue(String key){
        Any response =  values.get(key);

        if (response == null) {
            writeValue(key, Any.newBuilder().build());
        }
        return response;
    }

    public void writeValue(String key, Any value){
        values.put(key,value);
    }
}