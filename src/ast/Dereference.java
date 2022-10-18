package ast;

import pl434.Symbol;

public class Dereference extends Node implements Expression {

  public Expression expression;

  public Dereference(int lineNum, int charPos, Expression expression) {
    super(lineNum, charPos);
    //TODO Auto-generated constructor stub
    this.expression= expression;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    // TODO Auto-generated method stub
    visitor.visit(this);
  }
}
