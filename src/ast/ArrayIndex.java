package ast;
import pl434.Symbol;
public class ArrayIndex extends Node implements Expression {

  public Expression left;
  public Expression right;
  public Symbol symbol;
  public ArrayIndex(
    int lineNum,
    int charPos,
    Expression left,
    Expression right,
    Symbol symbol
  ) {
    super(lineNum, charPos);
    //TODO Auto-generated constructor stub
    this.left = left;
    this.right = right;
    this.symbol = symbol;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    // TODO Auto-generated method stub
    visitor.visit(this);
  }
}
