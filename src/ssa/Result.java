package ssa;

import pl434.DLX;
import pl434.Symbol;
import types.FloatType;
import types.FuncType;
import types.IntType;
import types.Type;

public class Result {

  public static final int CONST = 0;
  public static final int VAR = 1;
  public static final int PROC = 2;
  public static final int ADDY = 3;
  public static final int INST = 4;
  public static final int GDB = 5;
  public int kind;
  public Instruction inst;
  public Block proc;
  public int regno;
  public int value;
  public Type type = new IntType();
  public float fvalue;

  public int addy;
  public Symbol var;

  @Override
  public String toString() {
    switch (kind) {
      case INST:
        return "(" + inst.my_num + ")";
      case CONST:
        if (type.getClass().equals(FloatType.class)) {
          return fvalue + "";
        }
        return value + "";
      case PROC:
        return "[" + proc.my_num + "]";
      case VAR:
        if (var.type.getClass().equals(FuncType.class)) {
          return var.name;
        } else {
          return var.toString();
        }
      case ADDY:
        return var.name;
      case GDB:
        return "GDB";
    }
    return "Error(Could not format Result to any given type)";
  }

  public boolean isVariable() {
    if (this.kind == VAR) {
      return true;
    }
    return false;
  }
}
