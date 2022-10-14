package ast;

import types.*;

public class ReturnStatement extends Node implements Statement {

  private Expression relation;
  private Type returnType;

  public ReturnStatement(
    int lineNum,
    int charPos,
    Expression relation,
    Type returnType
  ) {
    super(lineNum, charPos);
    //TODO Auto-generated constructor stub
    this.relation = relation;
    this.returnType = returnType;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    // TODO Auto-generated method stub
    visitor.visit(this);
  }

  public Expression relation() {
    return this.relation;
  }

  public Type returnType() {
    return this.returnType;
  }
}
