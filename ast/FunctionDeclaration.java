package ast;

import pl434.*;
import pl434.Compiler.ReturnStatementInformation;

public class FunctionDeclaration extends Node implements Declaration {

  public Symbol function;
  private FunctionBody body;
  private pl434.Compiler.ReturnStatementInformation returnStatementInformation;

  public FunctionDeclaration(
    int lineNum,
    int charPos,
    Symbol function,
    FunctionBody body,
    pl434.Compiler.ReturnStatementInformation returnStatementInformation
  ) {
    //TODO Auto-generated constructor stub
    super(lineNum, charPos);
    this.body = body;
    this.function = function;
    this.returnStatementInformation = returnStatementInformation;
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

  public ReturnStatementInformation returnStatementInformation() {
    return this.returnStatementInformation;
  }
}
