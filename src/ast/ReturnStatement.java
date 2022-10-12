package ast;

public class ReturnStatement extends Node implements Statement {

  private Expression relation;

  public ReturnStatement(int lineNum, int charPos, Expression relation) {
    super(lineNum, charPos);
    //TODO Auto-generated constructor stub
    this.relation = relation;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    // TODO Auto-generated method stub
    visitor.visit(this);
  }

  public Expression relation() {
    return this.relation;
  }
}
