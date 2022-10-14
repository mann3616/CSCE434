package ast;

public class WhileStatement extends Node implements Statement {

  private Expression relation;
  private StatementSequence sequence;

  public WhileStatement(
    int lineNum,
    int charPos,
    Expression relation,
    StatementSequence sequence
  ) {
    super(lineNum, charPos);
    //TODO Auto-generated constructor stub
    this.relation = relation;
    this.sequence = sequence;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    // TODO Auto-generated method stub
    visitor.visit(this);
  }

  public Expression relation() {
    return this.relation;
  }

  public StatementSequence sequence() {
    return this.sequence;
  }
}
