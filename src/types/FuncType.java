package types;

public class FuncType extends Type {

  TypeList params;
  private Type returnType;

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
