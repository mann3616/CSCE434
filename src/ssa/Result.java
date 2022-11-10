package ssa;

import pl434.DLX;
import pl434.Symbol;
import types.FloatType;
import types.FuncType;
import types.IntType;
import types.Type;

public class Result {

  static final int CONST = 0;
  static final int VAR = 1;
  static final int PROC = 2;
  static final int ADDY = 3;
  static final int INST = 4;
  static final int GDB = 5;
  public int kind;
  public Instruction inst;
  public Block proc;
  public int regno;
  public int value;
  public Type type = new IntType();

  public int addy;
  public Symbol var;

  @Override
  public String toString() {
    switch (kind) {
      case INST:
        return "(" + inst.my_num + ")";
      case CONST:
        if (type.getClass().equals(FloatType.class)) {
          //return DLX.toFP32FromFP16(value) + "";
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
}
