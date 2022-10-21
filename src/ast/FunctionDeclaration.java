package ast;

import pl434.*;

public class FunctionDeclaration extends Node implements Declaration {

  public Symbol function;
  private FunctionBody body;

  public FunctionDeclaration(
    int lineNum,
    int charPos,
    FunctionBody body,
    Symbol function
  ) {
    //TODO Auto-generated constructor stub
    super(lineNum, charPos);
    this.body = body;
    this.function = function;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    // TODO Auto-generated method stub
    visitor.visit(this);
  }

  public String function() {
    return this.function.functionString();
  }

  public FunctionBody body() {
    return this.body;
  }
}
