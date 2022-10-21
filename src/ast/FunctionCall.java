package ast;

import java.util.ArrayList;
import pl434.Symbol;

public class FunctionCall extends Node implements Expression, Statement {

  String name;
  public Symbol function;
  public ArrayList<Symbol> functions;
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

  public FunctionCall(
    int lineNum,
    int charPos,
    String name,
    ArrayList<Symbol> function,
    ArgumentList list
  ) {
    super(lineNum, charPos);
    this.functions = function;
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
