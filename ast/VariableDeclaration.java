package ast;

import pl434.*;

public class VariableDeclaration extends Node implements Declaration {

  public Symbol symbol;

  public VariableDeclaration(int lineNum, int charPos, Symbol symbol) {
    //TODO Auto-generated constructor stub
    super(lineNum, charPos);
    this.symbol = symbol;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    // TODO Auto-generated method stub
    visitor.visit(this);
  }

  public String symbol() {
    return symbol.name() + ":" + symbol.getTypeAsString();
  }
}
