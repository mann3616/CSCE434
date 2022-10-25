package ssa;

import pl434.DLX;
import pl434.Symbol;
import types.FloatType;
import types.FuncType;
import types.Type;

public class Result {

  static final int CONST = 0;
  static final int VAR = 1;
  static final int PROC = 2;
  static final int ADDY = 3;
  static final int SELECT = 4;
  static final int INST = 5;
  public int kind;
  public int inst;
  public int regno;
  public int value;
  public Type type;

  public int addy;
  public Symbol var;

  @Override
  public String toString() {
    switch (kind) {
      case INST:
        return "(" + inst + ")";
      case CONST:
        if (type.getClass().equals(FloatType.class)) {
          return DLX.toFP32FromFP16(value) + "";
        }
        return value + "";
      case PROC:
        return "[" + value + "]";
      case VAR:
        return (
          var.name +
          (
            var.type.getClass().equals(FuncType.class)
              ? ""
              : "_" + var.my_assign
          )
        );
    }
    return "Error(Could not format Result to any given type)";
  }
}
