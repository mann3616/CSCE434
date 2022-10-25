package types;

import pl434.Symbol;

public class FuncType extends Type {

  TypeList params;
  private Type returnType;
  public Symbol funcToRun;

  public FuncType(TypeList params, Type returnType) {
    this.params = params;
    this.returnType = returnType;
  }

  public TypeList params() {
    return this.params;
  }

  public Type returnType() {
    return this.returnType;
  }
}
