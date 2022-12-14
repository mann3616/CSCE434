package pl434;

import java.util.ArrayList;
import javax.management.RuntimeErrorException;
import ssa.Block;
import ssa.Instruction;
import types.*;

public class Symbol {

  public String name;
  public Type type;
  public int address = -1;
  public int regno = -1;
  public static int static_assign = 0;
  public int my_assign;
  public Instruction instruction;
  public boolean assign;
  public boolean builtinFunc;
  public boolean loaded;
  public Symbol OG;
  public int global_counter = -1; // Only for function
  public Block func_block;
  public static final int GLOBAL = 0;
  public static final int LOCAL = 1;
  public static final int PARAM = 2;
  public int scope = 0;
  public ArrayList<Symbol> params = new ArrayList<>();

  public Symbol(String name, Type type) {
    loaded = false;
    instruction = null;
    builtinFunc = false;
    assign = false;
    this.name = name;
    this.type = type;
    my_assign = -1;
    OG = this;
  }

  public Symbol(Symbol simba, boolean assign) {
    if (this.equals(simba)) {
      throw new RuntimeErrorException(null, "Can't set Symbol to itself");
    }
    this.OG = simba.OG;
    this.my_assign = Instruction.instruction_num;
    if (!assign && !simba.assign) {
      // If this variable has not been assigned yet
      instruction = null;
      this.assign = false;
      my_assign = -1;
    } else if (simba.assign && !assign) {
      // If this variable has been assigned previously and we are currently not assigning to this var
      this.assign = true;
      this.my_assign = simba.my_assign;
      if (simba.instruction != null) {
        instruction = simba.instruction;
      } else {
        instruction = null;
      }
    } else {
      // Variable is being assigned and the parent symbol needs the newly updated version #
      this.assign = true;
      simba.assign = true;
      simba.my_assign = this.my_assign;
    }
    this.name = simba.name;
    this.type = simba.type;
  }

  public String name() {
    return name;
  }

  public String functionString() {
    FuncType t = (FuncType) type;
    String ret = "";
    ret += name();
    ret += ":(";
    String param = "";
    for (Type tt : t.params()) {
      param += ", " + typeToString(tt);
    }
    String paramList = "";
    if (!param.equals("")) {
      paramList = param.substring(2);
    }
    ret += paramList + ")->" + typeToString(t.returnType());
    return ret;
  }

  public String getTypeAsString() {
    return typeToString(type);
  }

  public static String typeToString(Type tt) {
    String param = "";
    // If this doesn't work use the toString functions
    if (tt.getClass().equals(FloatType.class)) {
      param += "float";
    } else if (tt.getClass().equals(BoolType.class)) {
      param += "bool";
    } else if (tt.getClass().equals(IntType.class)) {
      param += "int";
    } else if (tt.getClass().equals(VoidType.class)) {
      param += "void";
    } else if (tt.getClass().equals(ErrorType.class)) {
      param += "ErrorType(" + ((ErrorType) tt).message() + ")";
    } else {
      ArrayType t = (ArrayType) tt;
      param += typeToString(t.type());
      for (int i = 0; i < t.dims(); i++) {
        if (t.dimVals().isEmpty()) {
          param += "[]";
        } else {
          param += "[" + t.dimVals().get(i) + "]";
        }
      }
    }
    return param;
  }

  public int getVersion() {
    if (instruction == null) {
      return -1;
    }
    return instruction.my_num;
  }

  public int getAddress() {
    return OG.address;
  }

  public int getRegister() {
    if (OG.regno == -1) {
      throw new NoRegisterAssigned(
        "No register assigned to var <" + name + "> in version #" + getVersion()
      );
    }
    return OG.regno;
  }

  public boolean isLoaded() {
    return OG.loaded;
  }

  public void load() {
    OG.loaded = true;
  }

  public void unload() {
    OG.loaded = false;
  }

  @Override
  public String toString() {
    return name + (instruction == null ? "_-1" : "_" + instruction.my_num);
  }
}

class NoRegisterAssigned extends RuntimeException {

  public NoRegisterAssigned(String ex) {
    super(ex);
  }
}
