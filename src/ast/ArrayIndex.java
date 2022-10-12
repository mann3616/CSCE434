package ast;

public class ArrayIndex extends Node implements Expression {

  public Expression left;
  public Expression right;

  public ArrayIndex(
    int lineNum,
    int charPos,
    Expression left,
    Expression right
  ) {
    super(lineNum, charPos);
    //TODO Auto-generated constructor stub
    this.left = left;
    this.right = right;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    // TODO Auto-generated method stub
    visitor.visit(this);
  }
}
