package pt.tecnico.rec.domain;

import com.google.protobuf.Any;

public class RecObject{

    int seq = 0;
    int cid = 0;
    Any value;

    RecObject(int seq, int cid, Any value){
        this.seq = seq;
        this.cid = cid;
        this.value = value;
    }

    public Any getValue(){
        return this.value;
    }

    public int getSeq(){
        return this.seq;
    }

    public int getCid() { return this.cid; }

    public void setObject(int newSeq, int newCid, Any newValue){
        this.seq = newSeq;
        this.cid = newCid;
        this.value = newValue;
    }
}
