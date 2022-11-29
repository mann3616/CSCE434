package ssa;

import java.util.HashMap;
import pl434.DLX;
import pl434.Symbol;
import types.FloatType;
import types.FuncType;
import types.IntType;
import types.Type;

public class Result {

  static final int CONST = 0;
  public static final int VAR = 1;
  static final int PROC = 2;
  static final int ADDY = 3;
  public static final int INST = 4;
  static final int GDB = 5;
  public int kind;
  public Instruction inst;
  public Block proc;
  public int regno;
  public int value;
  public Type type = new IntType();

  public int addy;
  public Symbol var;

  public static HashMap<String, Result> allResults = new HashMap<String, Result>();
  public static int num_no_names = 0;

  public void storeResult() {
    if (this.var != null && this.var.name != null) {
      allResults.put(this.var.name, this);
    } else {
      num_no_names++;
    }
  }

  public static void printAllResults() {
    System.out.println("*************************");
    for (String r : allResults.keySet()) {
      System.out.println("Variable " + r);
      System.out.println(allResults.get(r));
    }
    System.out.println("TOTAL OF " + num_no_names + " RESULTS WITH NO NAME");
    System.out.println("*************************");
  }

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

  public boolean isVariable() {
    if (this.kind == VAR) {
      return true;
    }
    return false;
  }
}
