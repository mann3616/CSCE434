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
  public void visit(StatementSequence node) {
    for (Statement s : node) {
      s.accept(this);
    }
  }

  @Override
  public void visit(FunctionBody node) {
    node.declaration().accept(this);
    node.sequence().accept(this);
  }

  @Override
  public void visit(VariableDeclaration node) {}

  @Override
  public void visit(FunctionDeclaration node) {
    node.body().accept(this);
  }

  @Override
  public void visit(DeclarationList node) {
    if (node.empty()) return;
    for (Declaration d : node) {
      d.accept(this);
    }
  }

  @Override
  public void visit(Computation node) {
    // Make node.main() not annoying
    if (node == null) return;
    node.variables().accept(this);
    node.functions().accept(this);
    node.mainStatementSequence().accept(this);
  }

  @Override
  public void visit(RepeatStatement node) {
    node.sequence().accept(this);
    node.relation().accept(this);
  }

  @Override
  public void visit(WhileStatement node) {
    node.relation().accept(this);
    node.sequence().accept(this);
  }

  @Override
  public void visit(ReturnStatement node) {
    if (node.relation() != null) {
      node.relation().accept(this);
    }
  }

  @Override
  public void visit(IfStatement node) {
    node.relation().accept(this);
    node.ifSequence().accept(this);
    if (node.elseSequence() != null) {
      node.elseSequence().accept(this);
    }
  }

  @Override
  public void visit(Assignment node) {
    node.addressOf().accept(this);
    node.right().accept(this);
  }

  @Override
  public void visit(AddressOf node) {}

  @Override
  public void visit(Dereference node) {}

  @Override
  public void visit(BoolLiteral node) {}

  @Override
  public void visit(IntegerLiteral node) {}

  @Override
  public void visit(FloatLiteral node) {}

  @Override
  public void visit(LogicalNot node) {
    node.right().accept(this);
  }

  @Override
  public void visit(Power node) {
    // TODO Auto-generated method stub
    node.left().accept(this);
    node.right().accept(this);
  }

  @Override
  public void visit(Multiplication node) {
    // TODO Auto-generated method stub
    node.left().accept(this);
    node.right().accept(this);
  }

  @Override
  public void visit(Division node) {
    node.left().accept(this);
    node.right().accept(this);
  }

  @Override
  public void visit(Modulo node) {
    node.left().accept(this);
    node.right().accept(this);
  }

  @Override
  public void visit(LogicalAnd node) {
    node.left().accept(this);
    node.right().accept(this);
  }

  @Override
  public void visit(Addition node) {
    node.left().accept(this);
    node.right().accept(this);
  }

  @Override
  public void visit(Subtraction node) {
    node.left().accept(this);
    node.right().accept(this);
  }

  @Override
  public void visit(LogicalOr node) {
    node.left().accept(this);
    node.right().accept(this);
  }

  @Override
  public void visit(Relation node) {
    node.left().accept(this);
    node.right().accept(this);
  }

  @Override
  public void visit(ArgumentList node) {
    for (Expression expr : node) {
      expr.accept(this);
    }
  }

  @Override
  public void visit(FunctionCall node) {
    node.list().accept(this);
  }

  @Override
  public void visit(ArrayIndex node) {
    node.left.accept(this);
    node.right.accept(this);
  }
}
