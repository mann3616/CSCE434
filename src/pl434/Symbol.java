package pl434;

import javax.management.RuntimeErrorException;
import types.*;

public class Symbol {

  public String name;
  // TODO: Add other parameters like type

  public Type type;
  public static int static_assign = 0;
  public int my_assign;

  public Symbol(String name, Type type) {
    this.name = name;
    this.type = type;
    my_assign = -1;
  }

  public Symbol(Symbol simba) {
    if (this.equals(simba)) {
      throw new RuntimeErrorException(null, "Can't set Symbol to itself");
    }
    this.my_assign = Symbol.static_assign++;
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

  @Override
  public String toString() {
    return name + my_assign;
  }
}
