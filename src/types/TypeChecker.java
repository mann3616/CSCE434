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
  private TypeList argList;
  private int numNestedFunctions;

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

  public boolean check(ast.AST ast) {
    // Take the node and begin parsing
    argList = new TypeList();
    errorBuffer = new StringBuilder();
    numNestedFunctions = 0;
    visit(ast.getNode());
    return hasError();
  }

  private void reportError(int lineNum, int charPos, String message) {
    errorBuffer.append("TypeError(" + (lineNum + 1) + "," + charPos + ")");
    errorBuffer.append("[" + message + "]" + "\n");
    currentFunction = new Symbol(message, new ErrorType(message));
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
    // TODO: add print AddressOf(Type) capability if one of the expressions is an AddressOf
    // Check program test001 output vs test001.out to understand
    node.addressOf().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    if (!leftType.getClass().equals(rightType.getClass())) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "Cannot assign " + rightType + " to AddressOf(" + leftType + ")."
      );
    }
  }

  @Override
  public void visit(AddressOf node) {
    currentFunction =
      new Symbol(
        "AddressOf(" + node.symbol().getTypeAsString() + ")",
        node.symbol().type
      );
  }

  @Override
  public void visit(Dereference node) {
    currentFunction = node.symbol();
  }

  @Override
  public void visit(BoolLiteral node) {
    currentFunction = new Symbol(node.literal(), new BoolType());
  }

  @Override
  public void visit(IntegerLiteral node) {
    currentFunction = new Symbol(node.literal(), new IntType());
  }

  @Override
  public void visit(FloatLiteral node) {
    currentFunction = new Symbol(node.literal(), new FloatType());
  }

  @Override
  public void visit(LogicalNot node) {
    // TODO: check if "right()" is of BoolType
    node.right().accept(this);
  }

  @Override
  public void visit(Power node) {
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    if (!leftType.getClass().equals(rightType.getClass())) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        leftType.add(rightType).toString()
      );
    }
  }

  @Override
  public void visit(Multiplication node) {
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    if (!leftType.getClass().equals(rightType.getClass())) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        leftType.add(rightType).toString()
      );
    }
  }

  @Override
  public void visit(Division node) {
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    if (!leftType.getClass().equals(rightType.getClass())) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        leftType.add(rightType).toString()
      );
    }
  }

  @Override
  public void visit(Modulo node) {
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    if (!leftType.getClass().equals(rightType.getClass())) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        leftType.add(rightType).toString()
      );
    }
  }

  @Override
  public void visit(Addition node) {
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    if (!leftType.getClass().equals(rightType.getClass())) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        leftType.add(rightType).toString()
      );
    }
  }

  @Override
  public void visit(Subtraction node) {
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    if (!leftType.getClass().equals(rightType.getClass())) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        leftType.add(rightType).toString()
      );
    }
  }

  @Override
  public void visit(LogicalAnd node) {
    // TODO: add checks to see if types are bool
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    if (!leftType.getClass().equals(rightType.getClass())) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        leftType.add(rightType).toString()
      );
    }
  }

  @Override
  public void visit(LogicalOr node) {
    // TODO: add checks to see if types are bool
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    if (!leftType.getClass().equals(rightType.getClass())) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        leftType.add(rightType).toString()
      );
    }
  }

  @Override
  public void visit(Relation node) {
    // TODO: add checks to see if types are bool
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    if (!leftType.getClass().equals(rightType.getClass())) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        leftType.add(rightType).toString()
      );
    }
  }

  @Override
  public void visit(ArgumentList node) {
    for (Expression expr : node) {
      // When you accept an expression, currentFunction changes
      expr.accept(this);
      // All expressions will resolve with some form of type
      argList.append(currentFunction.type);
    }
  }

  @Override
  public void visit(FunctionCall node) {
    numNestedFunctions++;
    // FunctionCall members:
    // public Symbol function;
    // public ArgumentList list;

    // FuncType members:
    // private TypeList params;
    // private Type returnType;

    FuncType funcType = (FuncType) node.function.type;
    Type nodeReturnType = ((FuncType) node.function.type).returnType();
    TypeList savedArgList = new TypeList();
    // If we have nested
    if (numNestedFunctions > 1) {
      savedArgList = argList;
    }
    argList = new TypeList();
    node.list().accept(this);

    // "Call with args " + argTypes + " matches no function signature."
    // "Call with args " + argTypes + " matches multiple function signatures."
    // if provided parameters are not equal to wanted parameters
    if (!argList.equals(funcType.params())) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "Call with args " + argList + " matches no function signature."
      );
    } else {
      // If this FunctionCall doesn't return an error
      currentFunction = new Symbol(node.function.name(), nodeReturnType);
    }

    // Perhaps append current function return type to argList?
    argList = savedArgList;
    numNestedFunctions--;
  }

  @Override
  public void visit(ArrayIndex node) {
    node.left.accept(this);
    node.right.accept(this);
  }
}
