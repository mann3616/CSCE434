package pl434;

import ast.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import org.apache.commons.cli.Options;
import pl434.Token.Kind;
import ssa.*;
import types.*;

public class Compiler {

  // Error Reporting ============================================================
  private StringBuilder errorBuffer = new StringBuilder();

  private String reportSyntaxError(NonTerminal nt) {
    String message =
      "SyntaxError(" +
      lineNumber() +
      "," +
      charPosition() +
      ")[Expected a token from " +
      nt.name() +
      " but got " +
      currentToken.kind +
      ".]";
    errorBuffer.append(message + "\n");
    return message;
  }

  private String reportSyntaxError(Token.Kind kind) {
    String message =
      "SyntaxError(" +
      lineNumber() +
      "," +
      charPosition() +
      ")[Expected " +
      kind +
      " but got " +
      currentToken.kind +
      ".]";
    errorBuffer.append(message + "\n");
    return message;
  }

  private String reportResolveSymbolError(
    String name,
    int lineNum,
    int charPos
  ) {
    String message =
      "ResolveSymbolError(" +
      lineNum +
      "," +
      charPos +
      ")[Could not find " +
      name +
      ".]";
    errorBuffer.append(message + "\n");
    return message;
  }

  private String reportDeclareSymbolError(
    String name,
    int lineNum,
    int charPos
  ) {
    String message =
      "DeclareSymbolError(" +
      lineNum +
      "," +
      charPos +
      ")[" +
      name +
      " already exists.]";
    errorBuffer.append(message + "\n");
    return message;
  }

  public String errorReport() {
    return errorBuffer.toString();
  }

  public boolean hasError() {
    return errorBuffer.length() != 0;
  }

  private class QuitParseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public QuitParseException(String errorMessage) {
      super(errorMessage);
    }
  }

  private int lineNumber() {
    return currentToken.lineNumber();
  }

  private int charPosition() {
    return currentToken.charPosition();
  }

  // Compiler ===================================================================
  private Scanner scanner;
  private Token currentToken;

  private int numDataRegisters; // available registers are [1..numDataRegisters]

  private Optimize optimize;

  // Need to map from IDENT to memory offset

  public Compiler(Scanner scanner, int numRegs) {
    this.scanner = scanner;
    currentToken = this.scanner.next();
    numDataRegisters = numRegs;
  }

  private SymbolTable symbolTable;

  private void initSymbolTable() {
    symbolTable = new SymbolTable();
  }

  private void enterScope() {
    symbolTable.addScope();
  }

  private void exitScope() {
    symbolTable.popScope();
  }

  private Symbol tryResolveVariable(Token ident) {
    //TODO: Try resolving variable, handle SymbolNotFoundError
    try {
      return symbolTable.lookup(ident.lexeme());
    } catch (SymbolNotFoundError e) {
      reportResolveSymbolError(
        ident.lexeme(),
        ident.lineNumber() + 1,
        ident.charPosition()
      );
      return new Symbol(
        "Error",
        new ErrorType("Variable Could Not Be Resolved")
      );
    }
  }

  private ArrayList<Symbol> tryResolveFunction(Token ident) {
    //TODO: Try resolving variable, handle SymbolNotFoundError
    try {
      return symbolTable.lookupFunc(ident.lexeme());
    } catch (SymbolNotFoundError e) {
      reportResolveSymbolError(
        ident.lexeme(),
        ident.lineNumber() + 1,
        ident.charPosition()
      );
      return new ArrayList<>();
    }
  }

  private Symbol tryDeclareVariable(Token ident, Symbol sym) {
    try {
      return symbolTable.insert(ident.lexeme(), sym);
    } catch (RedeclarationError e) {
      reportDeclareSymbolError(
        ident.lexeme(),
        ident.lineNumber() + 1,
        ident.charPosition()
      );
      return null;
    }
  }

  // Helper Methods =============================================================
  private boolean have(Token.Kind kind) {
    return currentToken.kind() == kind;
  }

  private boolean have(NonTerminal nt) {
    return nt.firstSet().contains(currentToken.kind);
  }

  private boolean accept(Token.Kind kind) {
    if (have(kind)) {
      try {
        currentToken = scanner.next();
      } catch (NoSuchElementException e) {
        if (!kind.equals(Token.Kind.EOF)) {
          String errorMessage = reportSyntaxError(kind);
          throw new QuitParseException(errorMessage);
        }
      }
      return true;
    }
    return false;
  }

  private boolean accept(NonTerminal nt) {
    if (have(nt)) {
      currentToken = scanner.next();
      return true;
    }
    return false;
  }

  private boolean expect(Token.Kind kind) {
    if (accept(kind)) {
      return true;
    }
    String errorMessage = reportSyntaxError(kind);
    throw new QuitParseException(errorMessage);
  }

  private boolean expect(NonTerminal nt) {
    if (accept(nt)) {
      return true;
    }
    String errorMessage = reportSyntaxError(nt);
    throw new QuitParseException(errorMessage);
  }

  private Token expectRetrieve(Token.Kind kind) {
    Token tok = currentToken;
    if (accept(kind)) {
      return tok;
    }
    String errorMessage = reportSyntaxError(kind);
    throw new QuitParseException(errorMessage);
  }

  private Token expectRetrieve(NonTerminal nt) {
    Token tok = currentToken;
    if (accept(nt)) {
      return tok;
    }
    String errorMessage = reportSyntaxError(nt);
    throw new QuitParseException(errorMessage);
  }

  private Token matchNonTerminal(NonTerminal nt) {
    return expectRetrieve(nt);
  }

  // literal = integerLit | floatLit
  private Token literal() {
    return matchNonTerminal(NonTerminal.LITERAL);
  }

  private Dereference designator() {
    int lineNum = lineNumber();
    int charPos = charPosition();

    Token ident = expectRetrieve(Token.Kind.IDENT);
    // if x is null what do we do... report error and return!
    Symbol simba = tryResolveVariable(ident);
    Expression expr = new AddressOf(lineNumber(), charPosition(), simba);
    //System.out.println(var + " is " + reg);
    Stack<Expression> st = new Stack<>();
    while (accept(Kind.OPEN_BRACKET)) {
      st.push(relExpr());
      expect(Kind.CLOSE_BRACKET);
    }
    while (!st.isEmpty()) {
      expr =
        new ArrayIndex(lineNumber(), charPosition(), expr, st.pop(), simba);
    }
    return new Dereference(lineNum, charPos, expr);
  }

  private DeclarationList varDecl() {
    DeclarationList variableList = new DeclarationList(
      lineNumber(),
      charPosition()
    );

    while (have(NonTerminal.VAR_DECL)) {
      Token type = expectRetrieve(NonTerminal.VAR_DECL);
      // Give the variable a type depending on type case
      Type variableType = new VoidType();
      switch (type.kind()) {
        case FLOAT:
          variableType = new FloatType();
          break;
        case INT:
          variableType = new IntType();
          break;
        case BOOL:
          variableType = new BoolType();
          break;
      }
      // Parse brackets
      ArrayList<Integer> dims = new ArrayList<>();
      while (accept(Kind.OPEN_BRACKET)) {
        Token intVal = expectRetrieve(Kind.INT_VAL);
        expect(Kind.CLOSE_BRACKET);
        dims.add(Integer.parseInt(intVal.lexeme()));
      }
      // Form ArrayType if had brackets
      if (!dims.isEmpty()) {
        variableType = new ArrayType(variableType, dims.size(), dims);
      }
      // Parse IDENT's and declareVariable
      do {
        Token currentVariable = expectRetrieve(Kind.IDENT);
        String id = currentVariable.lexeme();
        // Declare Variable in Symbol table make node
        variableList.add(
          new VariableDeclaration(
            lineNumber(),
            charPosition(),
            tryDeclareVariable(currentVariable, new Symbol(id, variableType))
          )
        );
        // Store variable decl node
      } while (accept(Kind.COMMA));
      expect(Kind.SEMICOLON);
    }
    return variableList;
  }

  private StatementSequence statSeq() {
    StatementSequence currentStatementList = new StatementSequence(
      lineNumber(),
      charPosition()
    );
    currentStatementList.add(statement());
    expect(Kind.SEMICOLON);
    while (have(NonTerminal.STATEMENT)) {
      currentStatementList.add(statement());
      expect(Kind.SEMICOLON);
    }
    return currentStatementList;
  }

  private Statement statement() {
    if (have(NonTerminal.STATEMENT)) {
      switch (currentToken.kind()) {
        case WHILE:
          return whileStat();
        case RETURN:
          return returnStat();
        case REPEAT:
          return repeatStat();
        case IF:
          return ifStat();
        case LET:
          return letStat();
        case CALL:
          return funcCall();
      }
    }
    return null;
  }

  private WhileStatement whileStat() {
    Token whileToken = currentToken;
    expect(Kind.WHILE);
    Token start = currentToken;
    Expression relExpr = relation();
    expect(Kind.DO);
    StatementSequence whileStatementSequence = statSeq();
    expect(Kind.OD);
    return new WhileStatement(
      whileToken.lineNumber(),
      whileToken.charPosition(),
      relExpr,
      whileStatementSequence
    );
  }

  private RepeatStatement repeatStat() {
    // create repeat statement
    // use new stat list
    Token repeatToken = currentToken;
    expect(Kind.REPEAT);
    Token start = currentToken;
    StatementSequence repeatStatementList = statSeq();
    // Don't need to compule
    expect(Kind.UNTIL);

    Expression relExpr = relation();

    return new RepeatStatement(
      repeatToken.lineNumber(),
      repeatToken.charPosition(),
      repeatStatementList,
      relExpr
    );
  }

  private Statement letStat() {
    // Create assignment, add assignment to statement sequence
    Token letToken = currentToken;
    expect(Kind.LET);
    Expression assignTo = designator().expression;
    if (have(NonTerminal.UNARY_OP)) {
      Token tok = expectRetrieve(NonTerminal.UNARY_OP);
      return Node.newAssignment(
        letToken.lineNumber(),
        letToken.charPosition(),
        assignTo,
        tok,
        new IntegerLiteral(lineNumber(), charPosition(), "1")
      );
    }
    Token tok = expectRetrieve(NonTerminal.ASSIGN_OP);
    Expression second = relExpr();
    return Node.newAssignment(
      letToken.lineNumber(),
      letToken.charPosition(),
      assignTo,
      tok,
      second
    );
  }

  private ReturnStatement returnStat() {
    Token returnToken = currentToken;
    expect(Kind.RETURN);
    return new ReturnStatement(
      returnToken.lineNumber(),
      returnToken.charPosition(),
      relExpr()
    );
  }

  private IfStatement ifStat() {
    Token ifToken = currentToken;
    expect(Kind.IF);
    Expression b = relation();
    expect(Kind.THEN);
    StatementSequence ifStatementList = statSeq();
    StatementSequence elseStatementList = null;
    if (accept(Kind.ELSE)) {
      elseStatementList = statSeq();
    }
    expect(Kind.FI);
    return new IfStatement(
      ifToken.lineNumber(),
      ifToken.charPosition(),
      b,
      ifStatementList,
      elseStatementList
    );
  }

  private Expression groupExpr() {
    if (have(Kind.NOT)) {
      return Node.newExpression(null, expectRetrieve(Kind.NOT), relExpr());
    }
    if (have(NonTerminal.LITERAL)) {
      return Node.newLiteral(expectRetrieve(NonTerminal.LITERAL));
    }
    if (have(NonTerminal.DESIGNATOR)) {
      return designator();
    }
    if (have(NonTerminal.RELATION)) {
      return relation();
    }
    if (have(NonTerminal.FUNC_CALL)) {
      return funcCall();
    }
    return null;
  }

  private Expression powExpr() {
    Stack<Expression> res = new Stack<>();
    Stack<Token> toks = new Stack<>();
    res.push(groupExpr());
    while (have(NonTerminal.POW_OP)) {
      toks.push(expectRetrieve(NonTerminal.POW_OP));
      res.push(groupExpr());
    }
    return makeExpression(res, toks);
  }

  private Expression multExpr() {
    Stack<Expression> res = new Stack<>();
    Stack<Token> toks = new Stack<>();
    res.push(powExpr());
    while (have(NonTerminal.MULT_OP)) {
      toks.push(expectRetrieve(NonTerminal.MULT_OP));
      res.push(powExpr());
    }
    return makeExpression(res, toks);
  }

  private Expression addExpr() {
    Stack<Expression> res = new Stack<>();
    Stack<Token> toks = new Stack<>();
    res.push(multExpr());
    while (have(NonTerminal.ADD_OP)) {
      toks.push(expectRetrieve(NonTerminal.ADD_OP));
      res.push(multExpr());
    }
    return makeExpression(res, toks);
  }

  private Expression relExpr() {
    Stack<Expression> res = new Stack<>();
    Stack<Token> toks = new Stack<>();
    res.push(addExpr());
    while (have(NonTerminal.REL_OP)) {
      toks.push(expectRetrieve(NonTerminal.REL_OP));
      res.push(addExpr());
    }
    return makeExpression(res, toks);
  }

  private Expression relation() {
    expect(Kind.OPEN_PAREN);
    // resolves
    Expression o = relExpr();
    expect(Kind.CLOSE_PAREN);
    return o;
  }

  private Expression makeExpression(Stack<Expression> res, Stack<Token> toks) {
    if (toks.empty()) {
      return res.pop();
    }
    Expression ress = res.pop();
    Token tok = toks.pop();
    return Node.newExpression(makeExpression(res, toks), tok, ress);
  }

  //Function
  private FunctionCall funcCall() {
    Token funcCallToken = currentToken;
    expect(Kind.CALL);
    Token functionToken = expectRetrieve(Kind.IDENT);
    ArrayList<Symbol> li = tryResolveFunction(functionToken);
    expect(Kind.OPEN_PAREN);
    ArgumentList localArgumentList = new ArgumentList(
      lineNumber(),
      charPosition()
    );
    while (!accept(Kind.CLOSE_PAREN)) {
      localArgumentList.append(relExpr());
      accept(Kind.COMMA);
    }
    return new FunctionCall(
      funcCallToken.lineNumber(),
      funcCallToken.charPosition(),
      functionToken.lexeme(),
      li,
      localArgumentList
    );
  }

  private DeclarationList funcDecl() {
    Token functionIdent = currentToken;
    DeclarationList functionList = new DeclarationList(
      lineNumber(),
      charPosition()
    );
    while (accept(NonTerminal.FUNC_DECL)) {
      Token id = expectRetrieve(Kind.IDENT);
      enterScope();
      // TODO: Have this return a list of symbol instead and refactor funcCall
      // TODO: have scope at a certain point accessible through the nodes (e.g. maybe declare and resolve during graph generation)
      //Get params for Function
      TypeList variableList = formalParam();
      expect(Kind.COLON);
      Token tok = expectRetrieve(NonTerminal.TYPE_DECL);
      // Get returnType
      Type returnType = new VoidType();
      switch (tok.kind()) {
        case BOOL:
          returnType = new BoolType();
          break;
        case INT:
          returnType = new IntType();
          break;
        case FLOAT:
          returnType = new FloatType();
          break;
      }
      // FunctionDeclaration added to List
      FuncType functionType = new FuncType(variableList, returnType);
      Symbol simba = tryDeclareVariable(
        id,
        new Symbol(id.lexeme(), functionType)
      );
      functionList.add(
        new FunctionDeclaration(
          id.lineNumber(),
          id.charPosition(),
          funcBody(),
          simba
        )
      );
      // Scope exits in funcBody()
    }
    return functionList;
  }

  private TypeList formalParam() {
    TypeList list = new TypeList();
    expect(NonTerminal.FORMAL_PARAM);
    if (have(NonTerminal.PARAM_DECL)) {
      list.append(paramDecl());
      while (accept(Kind.COMMA)) {
        // should add
        list.append(paramDecl());
      }
    }
    expect(Kind.CLOSE_PAREN);
    return list;
  }

  private Type paramDecl() {
    // create declaration, add to variableList.list
    Token type = expectRetrieve(NonTerminal.PARAM_DECL);
    Type paramType = new VoidType();
    switch (type.kind()) {
      case BOOL:
        paramType = new BoolType();
        break;
      case FLOAT:
        paramType = new FloatType();
        break;
      case INT:
        paramType = new IntType();
        break;
    }
    int i = 0;
    while (accept(Kind.OPEN_BRACKET)) {
      expect(Kind.CLOSE_BRACKET);
      i++;
    }
    Token variable = expectRetrieve(Kind.IDENT);
    if (i != 0) {
      paramType = new ArrayType(paramType, i);
    }
    String id = variable.lexeme();
    tryDeclareVariable(variable, new Symbol(variable.lexeme(), paramType));
    return paramType;
  }

  private FunctionBody funcBody() {
    int ln = lineNumber();
    int ch = charPosition();
    expect(Kind.OPEN_BRACE);
    FunctionBody body = new FunctionBody(ln, ch, varDecl(), statSeq());
    expect(Kind.CLOSE_BRACE);
    expect(Kind.SEMICOLON);
    exitScope();
    return body;
  }

  Computation node;
  DeclarationList vars;
  DeclarationList funcs;
  StatementSequence mainSeq;

  private void computation() {
    initSymbolTable();
    enterScope();
    Token mainToken = currentToken;
    expect(Kind.MAIN);

    TypeList list = new TypeList();
    list.append(new IntType());
    FuncType type = new FuncType(list, new VoidType());
    // Declaring builtin Functions
    {
      tryDeclareVariable(
        new Token("printInt", 0, 0),
        new Symbol("printInt", type)
      );

      list = new TypeList();
      list.append(new FloatType());
      type = new FuncType(list, new VoidType());
      tryDeclareVariable(
        new Token("printFloat", 0, 0),
        new Symbol("printFloat", type)
      );

      list = new TypeList();
      list.append(new BoolType());
      type = new FuncType(list, new VoidType());
      tryDeclareVariable(
        new Token("printBool", 0, 0),
        new Symbol("printBool", type)
      );

      list = new TypeList();
      type = new FuncType(list, new VoidType());
      tryDeclareVariable(
        new Token("println", 0, 0),
        new Symbol("println", type)
      );

      list = new TypeList();
      type = new FuncType(list, new IntType());
      tryDeclareVariable(
        new Token("readInt", 0, 0),
        new Symbol("readInt", type)
      );

      list = new TypeList();
      type = new FuncType(list, new FloatType());
      tryDeclareVariable(
        new Token("readFloat", 0, 0),
        new Symbol("readFloat", type)
      );

      list = new TypeList();
      type = new FuncType(list, new BoolType());
      tryDeclareVariable(
        new Token("readBool", 0, 0),
        new Symbol("readBool", type)
      );
    }

    vars = varDecl();
    funcs = funcDecl();
    expect(Kind.OPEN_BRACE);
    mainSeq = statSeq();
    expect(Kind.CLOSE_BRACE);
    expect(Kind.PERIOD);

    FuncType mainType = new FuncType(new TypeList(), new VoidType());
    this.node =
      new Computation(
        mainToken.lineNumber(),
        mainToken.charPosition(),
        tryDeclareVariable(mainToken, new Symbol(mainToken.lexeme(), mainType)),
        vars,
        funcs,
        mainSeq
      );
  }

  public ast.AST genAST() {
    computation();
    return new ast.AST(node, symbolTable);
  }

  SSA ssa;

  public SSA genSSA(AST ast) {
    ssa = new SSA();
    ssa.visit(node);
    return ssa;
  }

  public String optimization(List<String> optArguments, Options options) {
    optimize = new Optimize(ssa);
    // Not sure why it wants the Options object in here
    boolean maxSelected = optArguments.contains("max");
    boolean maxOptSelected = optArguments.contains("maxOpt");

    //User may pick both max and maxOpt which is incorrect argument use
    //So in that case we simply give maxOpt priority to avoid such issues
    if (maxOptSelected) {
      // In maxOpt, order matters. Repeat the chosen optimizations in their input order until convergence
      boolean notConverged = false;
      while (notConverged) {
        SSA initialSsa = ssa;
        runChosenArguments(optArguments);
        notConverged = (initialSsa != ssa);
      }
    } else if (maxSelected) {
      // In max, order does not matter. Try every optimization until convergence
      boolean notConverged = false;
      while (notConverged) {
        SSA initialSsa = ssa;
        runEveryArgument();
        notConverged = (initialSsa != ssa);
      }
    } else {
      runChosenArguments(optArguments);
    }

    // Now take the SSA and print it as a dotGraph
    return ssa.asDotGraph();
  }

  private void runChosenArguments(List<String> optArguments) {
    // These should directly fiddle with the SSA
    for (int i = 0; i < optArguments.size(); i++) {
      switch (optArguments.get(i)) {
        case "cp":
          //Constant propogation
          break;
        case "cf":
          //Constant folding
          break;
        case "cpp":
          //Copy Propogation
          break;
        case "cse":
          //Common subexpression elimination
          break;
        case "dce":
          optimize.dead_code_elim();
          break;
        case "ofe":
          optimize.orphan_function();
          break;
      }
    }
  }

  private void runEveryArgument() {
    //Call every single optimization func every run
    //Constant propogation

    //Constant folding

    //Copy Propogation

    //Common subexpression elimination

    //Dead Code Elimination

    //Orphan function elimination

  }
}
