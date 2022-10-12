package types;

import ast.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import pl434.Symbol;

//Is abstract just to end error, must take off abstract for Project 5
public class TypeChecker implements NodeVisitor {

  private StringBuilder errorBuffer;
  private Symbol currentFunction;

  /*
   * Useful error strings:
   *
   * "Call with args " + argTypes + " matches no function signature."
   * "Call with args " + argTypes + " matches multiple function signatures."
   *
   * "IfStat requires relation condition not " + cond.getClass() + "."
   * "WhileStat requires relation condition not " + cond.getClass() + "."
   * "RepeatStat requires relation condition not " + cond.getClass() + "."
   *
   * "Function " + currentFunction.name() + " returns " + statRetType + " instead of " + funcRetType + "."
   *
   * "Variable " + var.name() + " has invalid type " + var.type() + "."
   * "Array " + var.name() + " has invalid base type " + baseType + "."
   *
   *
   * "Function " + currentFunction.name() + " has a void arg at pos " + i + "."
   * "Function " + currentFunction.name() + " has an error in arg at pos " + i + ": " + ((ErrorType) t).message())
   * "Not all paths in function " + currentFunction.name() + " return."
   */

  private void reportError(int lineNum, int charPos, String message) {
    errorBuffer.append("TypeError(" + lineNum + "," + charPos + ")");
    errorBuffer.append("[" + message + "]" + "\n");
  }

  public boolean hasError() {
    return errorBuffer.length() != 0;
  }

  public String errorReport() {
    return errorBuffer.toString();
  }

  @Override
  public void visit(Computation node) {
    node.variables().accept(this);
    node.functions().accept(this);
    node.mainStatementSequence().accept(this);
  }

  @Override
  public void visit(BoolLiteral node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(IntegerLiteral node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(FloatLiteral node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(AddressOf node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(ArrayIndex node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(Dereference node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(LogicalNot node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(Power node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(Multiplication node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(Division node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(Modulo node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(LogicalAnd node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(Addition node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(Subtraction node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(LogicalOr node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(Relation node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(Assignment node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(ArgumentList node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(FunctionCall node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(IfStatement node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(WhileStatement node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(RepeatStatement node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(ReturnStatement node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(StatementSequence node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(VariableDeclaration node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(FunctionBody node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(FunctionDeclaration node) {
    // TODO Auto-generated method stub

  }

  @Override
  public void visit(DeclarationList node) {
    // TODO Auto-generated method stub
    for (Declaration d : node) {
      d.accept(this);
    }
  }
}
