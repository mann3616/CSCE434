package types;

import ast.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import pl434.Symbol;
import pl434.SymbolTable;

//Is abstract just to end error, must take off abstract for Project 5
public class TypeChecker implements NodeVisitor {

  private StringBuilder errorBuffer;
  private Symbol currentFunction;
  private TypeList argList;
  private int numNestedFunctions;
  private SymbolTable table;

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
    if (ast.getNode() != null) {
      table = ast.table;
    }
    visit(ast.getNode());
    return !hasError();
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
  public void visit(VariableDeclaration node) {
    Type t = node.symbol.type;
    if (t.getClass().equals(ArrayType.class)) {
      ArrayType at = (ArrayType) t;
      for (int i : at.dimVals) {
        if (i <= 0) {
          reportError(
            node.charPosition(),
            node.lineNumber(),
            "Array " + node.symbol.name() + " has invalid size " + i
          );
        }
      }
    }
  }

  @Override
  public void visit(FunctionDeclaration node) {
    Type ret = ((FuncType) node.function.type).returnType();
    // returnStatementInformation() only returns null if there is no return statement in the statseq
    // What if there's more than one return statement...
    // Maybe make a returnStatementList into a list of returnStatementInformation
    currentFunction = node.function;
    if (node.returnStatementInformation() == null) {
      // It's ok if a function returns nothing?
      // ints, floats, bool can return nothing or themselves
      // if (!ret.getClass().equals(VoidType.class)) {
      //   // the return type isn't void so we have an issue
      //   reportError(
      //     node.lineNumber(),
      //     node.charPosition(),
      //     "Function " +
      //     currentFunction.name() +
      //     " returns " +
      //     (new VoidType()) +
      //     " instead of " +
      //     ret +
      //     "."
      //   );
      // }
    } else {
      Type returnedType = node.returnStatementInformation().returnType;
      boolean diffReturnTypes = !returnedType.getClass().equals(ret.getClass());
      // ints, floats, bool can return nothing or themselves
      if (diffReturnTypes) {
        reportError(
          node.returnStatementInformation().lineNum,
          node.returnStatementInformation().charPos,
          "Function " +
          currentFunction.name() +
          " returns " +
          node.returnStatementInformation().returnType +
          " instead of " +
          ret +
          "."
        );
      }
    }
    node.body().accept(this);
    currentFunction.type = ret;
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
    // "RepeatStat requires relation condition not " + cond.getClass() + "."
    node.sequence().accept(this);
    node.relation().accept(this);
    if (!currentFunction.type.getClass().equals(BoolType.class)) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "RepeatStat requires bool condition not " +
        currentFunction.getTypeAsString() +
        "."
      );
    }
  }

  @Override
  public void visit(WhileStatement node) {
    // "WhileStat requires relation condition not " + cond.getClass() + "."
    node.relation().accept(this);
    // If theres no relation, then current function should just be the first variable found
    // after relation, currentFunction should be a bool
    if (!currentFunction.type.getClass().equals(BoolType.class)) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "WhileStat requires bool condition not " +
        currentFunction.getTypeAsString() +
        "."
      );
    }
    // Current function should own relation now
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
    // "IfStat requires relation condition not " + cond.getClass() + "."
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
    //Not yet tested
    node.right().accept(this);
    currentFunction.type = currentFunction.type.not();
    if (!currentFunction.type.getClass().equals(ErrorType.class)) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        currentFunction.type.toString()
      );
    }
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
    Type t = leftType.mul(rightType);
    if (t.getClass().equals(ErrorType.class)) {
      reportError(node.lineNumber(), node.charPosition(), t.toString());
    }
    currentFunction.type = t;
  }

  @Override
  public void visit(Division node) {
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    Type t = leftType.div(rightType);
    if (t.getClass().equals(ErrorType.class)) {
      reportError(node.lineNumber(), node.charPosition(), t.toString());
    }
    currentFunction.type = t;
  }

  @Override
  public void visit(Modulo node) {
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    Type t = leftType.mod(rightType);
    if (t.getClass().equals(ErrorType.class)) {
      reportError(node.lineNumber(), node.charPosition(), t.toString());
    }
    currentFunction.type = t;
  }

  @Override
  public void visit(Addition node) {
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    Type t = leftType.add(rightType);
    if (t.getClass().equals(ErrorType.class)) {
      reportError(node.lineNumber(), node.charPosition(), t.toString());
    }
    currentFunction.type = t;
  }

  @Override
  public void visit(Subtraction node) {
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    Type t = leftType.sub(rightType);
    if (t.getClass().equals(ErrorType.class)) {
      reportError(node.lineNumber(), node.charPosition(), t.toString());
    }
    currentFunction.type = t;
  }

  @Override
  public void visit(LogicalAnd node) {
    // TODO: add checks to see if types are bool
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    Type t = leftType.and(rightType);
    if (t.getClass().equals(ErrorType.class)) {
      reportError(node.lineNumber(), node.charPosition(), t.toString());
    }
    currentFunction.type = t;
  }

  @Override
  public void visit(LogicalOr node) {
    // TODO: add checks to see if types are bool
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    Type rightType = currentFunction.type;
    Type t = leftType.or(rightType);
    if (t.getClass().equals(ErrorType.class)) {
      reportError(node.lineNumber(), node.charPosition(), t.toString());
    }
    currentFunction.type = t;
  }

  @Override
  public void visit(Relation node) {
    node.left().accept(this);
    Type leftType = currentFunction.type;
    // Right may be null
    node.right().accept(this);
    Type rightType = currentFunction.type;
    Type t = leftType.compare(rightType);

    //If left and right are not of the same type, throw an error
    if (t.getClass().equals(ErrorType.class)) {
      reportError(node.lineNumber(), node.charPosition(), t.toString());
      currentFunction.type = t;
    } else {
      if (
        leftType.getClass().equals(IntType.class) ||
        leftType.getClass().equals(FloatType.class)
      ) {
        // it's a number
        if (node.rel() == "not") {
          // can't negate a number
          currentFunction.type = leftType.not();
        } else {
          // all other expressions are valid
          currentFunction.type = new BoolType();
        }
      } else {
        // it's a boolean
        // only not, equal_to, and not_equal are valid
        //   if (node.rel() == "not" || node.rel() == "==" || node.rel() == "!=") {
        //     // these are the only safe operators
        //     currentFunction.type = new BoolType();
        //   } else {
        //     // Don't know what to do if it's not... Like what error string to put
        //     // bool > bool returns what?

        //   }
        // }
        currentFunction.type = new BoolType();
      }
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
    // FunctionCall members:
    // public Symbol function;
    // public ArgumentList list;

    // FuncType members:
    // private TypeList params;
    // private Type returnType;
    //TODO: Remove .equals and fix symbol table allocation of new functions with different size parameters
    //TODO: In statementSequence after each statement check if it was a function call and make sure it returned void
    FuncType funcType = (FuncType) node.function.type;
    Type nodeReturnType = ((FuncType) node.function.type).returnType();
    List<Symbol> sym = table.lookupFunc(node.function.name());
    TypeList savedArgList = new TypeList();
    // If we have nested
    savedArgList = argList;
    argList = new TypeList();
    node.list().accept(this);
    Type t = null;
    for (Symbol sim : sym) {
      t = sim.type.call(argList);
      if (!t.getClass().equals(ErrorType.class)) {
        nodeReturnType = ((FuncType) sim.type).returnType();
        break;
      }
    }
    // "Call with args " + argTypes + " matches no function signature."
    // "Call with args " + argTypes + " matches multiple function signatures."
    if (t == null) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "Call with args " +
        argList.toString() +
        " matches no function signature."
      );
    } else if (t.getClass().equals(ErrorType.class)) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "Call with args " + t.toString() + " matches no function signature."
      );
    }
    // If this FunctionCall doesn't return an error
    currentFunction = new Symbol(node.function.name(), nodeReturnType);

    // Perhaps append current function return type to argList?
    argList = savedArgList;
  }

  @Override
  public void visit(ArrayIndex node) {
    node.left.accept(this);
    node.right.accept(this);
  }
}
