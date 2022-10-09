package types;

import java.util.ArrayList;

public class ArrayType extends Type {
    int dims;
    ArrayList<Integer> dimVals;
    Type type;
    public ArrayType(Type type, int dims, ArrayList<Integer> dimVals){
        this.dims = dims;
        this.dimVals = dimVals;
        this.type = type;
    }
    public ArrayType(Type type, int dims){
        this.type = type;
        this.dims = dims;
        this.dimVals = new ArrayList<Integer>();
    }
    public int dims(){
        return this.dims;
    }
    public ArrayList<Integer> dimVals(){
        return this.dimVals;
    }
    public Type type(){
        return this.type;
    }
}