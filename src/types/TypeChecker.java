package types;

import ast.*;
import java.util.ArrayList;
import java.util.List;
import pl434.Symbol;
import pl434.SymbolTable;

//Is abstract just to end error, must take off abstract for Project 5
public class TypeChecker implements NodeVisitor {

  private StringBuilder errorBuffer;
  private Symbol currentFunction;
  private TypeList argList;
  private SymbolTable table;
  private int currIndex;
  private Symbol currFunc;
  boolean checkHasReturn;

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
    currIndex = -1;
    argList = new TypeList();
    errorBuffer = new StringBuilder();
    if (ast.getNode() != null) {
      table = ast.table;
    }
    currFunc = table.lookupFunc("main").get(0);
    checkHasReturn = false;
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
    if (checkHasReturn) {
      for (Statement s : node) {
        if (s.getClass().equals(ReturnStatement.class)) {
          checkHasReturn = false;
        }
      }
      if (checkHasReturn) {
        reportError(
          node.lineNumber(),
          node.charPosition(),
          "Function " +
          currFunc.name +
          " does not return, expected return of " +
          ((FuncType) currFunc.type).returnType() +
          "."
        );
        checkHasReturn = false;
      }
    }
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
    if (node.symbol == null) return;
    Type t = node.symbol.type;
    if (t.getClass().equals(ArrayType.class)) {
      ArrayType at = (ArrayType) t;
      for (int i : at.dimVals) {
        if (i <= 0) {
          reportError(
            node.lineNumber(),
            node.charPosition(),
            "Array " + node.symbol.name() + " has invalid size " + i + "."
          );
        }
      }
    }
  }

  @Override
  public void visit(FunctionDeclaration node) {
    Type ret = ((FuncType) node.function.type).returnType();
    if (!ret.getClass().equals(VoidType.class)) {
      checkHasReturn = true;
    }
    currFunc = node.function;
    node.body().accept(this);
    currentFunction = node.function;
    currFunc = table.lookupFunc("main").get(0);
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
    // Repeat statement is off
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
    Type returned;
    if (node.relation() != null) {
      node.relation().accept(this);
      returned = currentFunction.type;
    } else {
      returned = new VoidType();
    }
    if (
      !returned
        .getClass()
        .equals(((FuncType) currFunc.type).returnType().getClass())
    ) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "Function " +
        currFunc.name() +
        " returns " +
        returned +
        " instead of " +
        ((FuncType) currFunc.type).returnType() +
        "."
      );
    }
  }

  @Override
  public void visit(IfStatement node) {
    // "IfStat requires relation condition not " + cond.getClass() + "."
    node.relation().accept(this);
    // currentfunction holds relation type
    // What if a function is called?
    if (!currentFunction.type.getClass().equals(BoolType.class)) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "IfStat requires bool condition not " + currentFunction.type + "."
      );
    }
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
    Type t = leftType.assign(
      rightType,
      !(leftType instanceof ErrorType),
      node.right() instanceof AddressOf
    );
    if (t.getClass().equals(ErrorType.class)) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) t).message()
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
    node.expression.accept(this);
    currentFunction.type = currentFunction.type.deref();
    if (currentFunction.type.getClass().equals(ErrorType.class)) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) currentFunction.type).message()
      );
    }
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
    // if right isnt a bool then we're going to have an issue
    if (currentFunction.type.getClass().equals(ErrorType.class)) {
      // If right isn't a bool, we have an issue
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) currentFunction.type).message()
      );
    }
  }

  @Override
  public void visit(Power node) {
    // if left is negative throw error
    // if right is negative throw error
    node.left().accept(this);
    Type leftType = currentFunction.type;
    // Number leftNumber = (Number) new Object();
    if (node.left().getClass().equals(IntegerLiteral.class)) {
      int lit = Integer.parseInt(((IntegerLiteral) node.left()).literal());
      if (lit < 0) {
        reportError(
          node.lineNumber(),
          node.charPosition(),
          "Power cannot have a negative base of " + lit + "."
        );
        return;
      }
    }

    node.right().accept(this);
    if (node.right().getClass().equals(IntegerLiteral.class)) {
      int lit = Integer.parseInt(((IntegerLiteral) node.right()).literal());
      if (lit < 0) {
        reportError(
          node.lineNumber(),
          node.charPosition(),
          "Power cannot have a negative exponent of " + lit + "."
        );
        return;
      }
    }
    Type rightType = currentFunction.type;
    if (
      !leftType.getClass().equals(rightType.getClass()) ||
      !leftType.getClass().equals(IntType.class)
    ) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "Cannot raise " + leftType + " to " + rightType + "."
      );
    } else {
      // So they're the same, but is it a number?
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
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) t).message()
      );
    }
    currentFunction.type = t;
  }

  @Override
  public void visit(Division node) {
    // Check if right is 0
    node.left().accept(this);
    Type leftType = currentFunction.type;
    node.right().accept(this);
    if (node.right().getClass().equals(IntegerLiteral.class)) {
      int lit = Integer.parseInt(((IntegerLiteral) node.right()).literal());
      if (lit == 0) {
        reportError(
          node.lineNumber(),
          node.charPosition(),
          "Cannot divide by " + lit + "."
        );
        return;
      }
    }
    Type rightType = currentFunction.type;
    Type t = leftType.div(rightType);
    if (t.getClass().equals(ErrorType.class)) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) t).message()
      );
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
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) t).message()
      );
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
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) t).message()
      );
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
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) t).message()
      );
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
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) t).message()
      );
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
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) t).message()
      );
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
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) t).message()
      );
      currentFunction.name = ((ErrorType) t).message();
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
        // it's a boolean so it's automatically valid
        currentFunction.type = new BoolType();
      }
    }
  }

  @Override
  public void visit(ArgumentList node) {
    for (Expression expr : node) {
      expr.accept(this);
      argList.append(currentFunction.type);
    }
  }

  @Override
  public void visit(FunctionCall node) {
    //TODO: Remove .equals and fix symbol table allocation of new functions with different size parameters
    //TODO: In statementSequence after each statement check if it was a function call and make sure it returned void
    Symbol found = null;
    List<Symbol> sym = node.functions;
    TypeList savedArgList = new TypeList();
    // If we have nested
    savedArgList = argList;
    argList = new TypeList();
    node.list().accept(this);
    Type t = null;
    for (Symbol sim : sym) {
      t = sim.type.call(argList);
      if (!t.getClass().equals(ErrorType.class)) {
        found = sim;
        break;
      }
    }
    // "Call with args " + argTypes + " matches no function signature."
    // "Call with args " + argTypes + " matches multiple function signatures."
    if (found == null || t == null) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "Call with args " +
        argList.toString() +
        " matches no function signature."
      );
      argList = savedArgList;
      return;
    } else if (t.getClass().equals(ErrorType.class)) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "Call with args " +
        ((ErrorType) t).message() +
        " matches no function signature."
      );
      argList = savedArgList;
      return;
    }
    // If this FunctionCall doesn't return an error
    FuncType ft = (FuncType) found.type;
    currentFunction = new Symbol(found.name, ft.returnType());
    argList = savedArgList;
  }

  @Override
  public void visit(ArrayIndex node) {
    currIndex++;
    int saveIndex = currIndex;
    node.left.accept(this);
    Type lt = currentFunction.type;
    if (node.right.getClass().equals(Dereference.class)) {
      currIndex = -1;
    }
    node.right.accept(this);
    if (lt.getClass().equals(ErrorType.class)) {
      currIndex--;
      if (!lt.toString().startsWith("ErrorType(Cannot index AddressOf(")) {
        reportError(
          node.lineNumber(),
          node.charPosition(),
          "Cannot index " + lt + " with " + currentFunction.type + "."
        );
      } else {
        currentFunction.type = lt;
      }
      return;
    }
    currIndex = saveIndex;
    if (!node.symbol.type.getClass().equals(ArrayType.class)) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "Cannot index AddressOf(" +
        node.symbol.type +
        ") with " +
        currentFunction.type +
        "."
      );
      currIndex--;
      return;
    }
    ArrayType thisArr = ((ArrayType) node.symbol.type);
    if (thisArr.dims <= currIndex) {
      currIndex--;
      reportError(
        node.lineNumber(),
        node.charPosition(),
        "Cannot index AddressOf(" +
        thisArr.type +
        ")" +
        " with " +
        currentFunction.type +
        "."
      );
      return;
    }
    thisArr.currDim = currIndex;
    Type t = thisArr.index(currentFunction.type);
    if (t.getClass().equals(ErrorType.class)) {
      reportError(
        node.lineNumber(),
        node.charPosition(),
        ((ErrorType) t).message()
      );
      currentFunction.type = t;
      currIndex--;
      return;
    } else if (node.right.getClass().equals(IntegerLiteral.class)) {
      IntegerLiteral lit = (IntegerLiteral) node.right;
      int litt = Integer.parseInt(lit.literal());
      if (
        litt < 0 ||
        (
          thisArr.dimVals.size() > currIndex &&
          thisArr.dimVals.get(currIndex) > 0 &&
          thisArr.dimVals.get(currIndex) <= litt
        )
      ) {
        t =
          new ErrorType(
            "Array Index Out of Bounds : " +
            litt +
            " for array " +
            node.symbol.name
          );
        reportError(
          node.lineNumber(),
          node.charPosition(),
          ((ErrorType) t).message()
        );
        currIndex--;
        return;
      }
    }
    if (
      lt.getClass().equals(IntType.class) ||
      lt.getClass().equals(BoolType.class) ||
      lt.getClass().equals(FloatType.class)
    ) {
      currentFunction.type = lt;
    } else if (((ArrayType) node.symbol.type).dims == currIndex + 1) {
      currentFunction.type = ((ArrayType) node.symbol.type).type;
    } else {
      int i = ((ArrayType) node.symbol.type).dims - 1;
      ArrayList<Integer> l = new ArrayList<>();
      for (; i > currIndex; i--) {
        l.add(((ArrayType) node.symbol.type).dimVals.get(i));
      }
      currentFunction.type =
        new ArrayType(((ArrayType) node.symbol.type).type, l.size(), l);
    }
    currIndex--;
  }
}
