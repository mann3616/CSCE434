package ast;

public class LogicalNot extends Node implements Expression {

  private Expression right;

  protected LogicalNot(int lineNum, int charPos, Expression right) {
    super(lineNum, charPos);
    //TODO Auto-generated constructor stub
    this.right = right;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    // TODO Auto-generated method stub
    visitor.visit(this);
  }

  public Expression right() {
    return this.right;
  }
}
