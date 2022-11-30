package types;

import java.util.ArrayList;

public class ArrayType extends Type {

  int dims;
  public ArrayList<Integer> dimVals;
  Type type;
  public int currDim;

  public ArrayType(Type type, int dims, ArrayList<Integer> dimVals) {
    this.dims = dims;
    this.dimVals = dimVals;
    this.type = type;
  }

  public ArrayType(Type type, int dims) {
    this.type = type;
    this.dims = dims;
    this.dimVals = new ArrayList<Integer>();
  }

  public int dims() {
    return this.dims;
  }

  public ArrayList<Integer> dimVals() {
    return this.dimVals;
  }

  public Type type() {
    return this.type;
  }

  @Override
  public String toString() {
    String param = type.toString();
    int i = 0;
    while (i < dims) {
      param += "[" + (dimVals.size() > i ? dimVals.get(i) : "") + "]";
      i++;
    }
    return param;
  }
}
