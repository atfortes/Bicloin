package pt.tecnico.rec.domain;

import com.google.protobuf.Any;

import java.util.HashMap;

/** Domain root, where state and behavior of the server are implemented. */
public class RecordInfo{

    private HashMap<String, Any> values = new HashMap<>();

    public synchronized Any getValue(String key){

        Any response =  values.get(key);
        if (response == null) { writeValue(key, Any.newBuilder().build());}
        return response;
    }

    public synchronized void writeValue(String key, Any value){
        values.put(key,value);
    }
}