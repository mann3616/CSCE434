package ast;

import pl434.Symbol;

public class FunctionCall extends Node implements Expression, Statement {

  public Symbol function;
  public ArgumentList list;

  public FunctionCall(
    int lineNum,
    int charPos,
    Symbol function,
    ArgumentList list
  ) {
    super(lineNum, charPos);
    this.function = function;
    this.list = list;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  public String function() {
    return this.function.functionString();
  }

  public ArgumentList list() {
    return this.list;
  }
}
