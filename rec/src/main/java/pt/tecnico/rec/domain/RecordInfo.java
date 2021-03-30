package pt.tecnico.rec.domain;

import com.google.protobuf.Any;

import java.util.Map;

/** Domain root, where state and behavior of the server are implemented. */
public class RecordInfo{

    private Map<String, Any> values;

    public synchronized Any getValue(String key){
        Any response =  values.get(key);
        if (response == null) { writeValue(key,null);}
        return response;
    }

    public synchronized void writeValue(String key, Any value){
        values.put(key,value);
    }
}