package pl434;

import ast.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import org.apache.commons.cli.Options;
import pl434.RegisterAlloc;
import pl434.Token.Kind;
import pl434.VariableInfo;
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
      while (runChosenArguments(optArguments));
    } else if (maxSelected) {
      // In max, order does not matter. Try every optimization until convergence
      boolean notConverged = false;
      while (runEveryArgument());
    } else {
      runChosenArguments(optArguments);
    }

    // Now take the SSA and print it as a dotGraph
    return ssa.asDotGraph();
  }

  private boolean runChosenArguments(List<String> optArguments) {
    // These should directly fiddle with the SSA
    boolean change = false;
    for (int i = 0; i < optArguments.size(); i++) {
      switch (optArguments.get(i)) {
        case "cp":
          //Constant propogation
          while (optimize.constant_propogation()) {
            change = true;
          }
          break;
        case "cf":
          //Constant folding
          while (optimize.constant_folding()) {
            change = true;
          }
          break;
        case "cpp":
          //Copy Propogation
          while (optimize.copy_propogation()) {
            change = true;
          }
          break;
        case "cse":
          //Common subexpression elimination
          while (optimize.subexpr_elim()) {
            change = true;
          }
          break;
        case "dce":
          while (optimize.dead_code_elim()) {
            change = true;
          }
          break;
        case "ofe":
          while (optimize.orphan_function()) {
            change = true;
          }
          break;
      }
    }
    return change;
  }

  private boolean runEveryArgument() {
    boolean change = false;
    while (optimize.constant_propogation()) {
      change = true;
    }
    while (optimize.constant_folding()) {
      change = true;
    }
    while (optimize.copy_propogation()) {
      change = true;
    }
    while (optimize.subexpr_elim()) {
      change = true;
    }
    while (optimize.dead_code_elim()) {
      change = true;
    }
    while (optimize.orphan_function()) {
      change = true;
    }
    return change;
  }

  public int[] genCode() {
    return null;
  }

  HashMap<Block, HashMap<Integer, ArrayList<RegisterAlloc>>> allRegisterMaps = new HashMap<Block, HashMap<Integer, ArrayList<RegisterAlloc>>>();
  HashMap<Block, HashMap<String, ArrayList<VariableInfo>>> allLiveRanges = new HashMap<Block, HashMap<String, ArrayList<VariableInfo>>>();
  HashMap<Block, HashMap<String, VariableInfo>> allLiveIntervals = new HashMap<>();

  public void printLiveInfo(Block block) {
    // After calculate liveness, all instructions have insets and outsets
    printLiveness(block);
    printLiveIntervals(allLiveIntervals.get(block));
  }

  public void initializeLiveness(Block block) {
    // Calculates all live sets
    calculateLiveness(block);

    // Populates the variable hashmap to get all variables
    allLiveRanges.put(block, new HashMap<String, ArrayList<VariableInfo>>());
    populateliveRanges(allLiveRanges.get(block), block);

    // Calculates the intervals
    calculateliveRanges(allLiveRanges.get(block), block);

    // Prints liveRanges
    // printliveRanges(allLiveRanges.get(block));

    // HashMap<String, ArrayList<Pair>> liveRanges contains liveRanges, convert to liveInterval
    // IE, a = [1,3],[6,11], [14,39] -> a = [1,39]
    allLiveIntervals.put(block, new HashMap<String, VariableInfo>());
    initializeLiveIntervals(
      allLiveIntervals.get(block),
      allLiveRanges.get(block)
    );
    calculateLiveIntervals(
      allLiveIntervals.get(block),
      allLiveRanges.get(block)
    );
  }

  public void regAlloc(int numRegs) {
    ssa.fixUpSSA(); // LOOK HERE LANCE! - Momo <3
    ssa.instantiateUsedAt();
    ssa.countUpResults();
    for (Block block : ssa.roots) {
      initializeLiveness(block);
      // Next step is to actually distribute registers
      // Initialize RegisterMap with each key being a register number
      initializeRegisterMap(block, numRegs);
      allocateRegisters(
        block,
        numRegs,
        allRegisterMaps.get(block),
        allLiveIntervals.get(block)
      );

      // Prints all the underlying notes
      printLiveInfo(block);
      printRegisterAllocation(allRegisterMaps.get(block));
    }
    // Result.printAllResults();

    return;
  }

  // Creates live in and live out sets for all instructions
  private void calculateLiveness(Block block) {
    ArrayList<Instruction> instructionSet = new ArrayList<Instruction>(
      block.instructions
    );
    boolean change_detected;
    do {
      change_detected = false;
      // Traverse backwards
      // for every instruction in SSA
      for (int i = instructionSet.size() - 1; i >= 0; i--) {
        Instruction currentInstruction = instructionSet.get(i);
        if (currentInstruction.isEliminated()) {
          continue;
        }
        // First save the current in-set and out-set
        HashSet<String> originalInSet = currentInstruction.InSet;
        HashSet<String> originalOutSet = currentInstruction.OutSet;

        HashSet<String> definedSet = new HashSet<>();
        HashSet<String> usedSet = new HashSet<>();
        // MOV e f means move value of e into f
        switch (currentInstruction.inst) {
          case MOVE:
            if (currentInstruction.left.isVariable()) {
              usedSet.add(currentInstruction.left.var.name);
            } else if (currentInstruction.left.kind == Result.INST) {
              usedSet.add("(" + currentInstruction.left.inst.my_num + ")");
            }
            if (currentInstruction.right.isVariable()) {
              definedSet.add(currentInstruction.right.var.name);
            } else {
              // If the 'right' in a MOV operation is not a variable, then we know that we're saving the instruction
              definedSet.add("(" + currentInstruction.my_num + ")");
            }
            break;
          case CALL:
            System.out.println(
              "called func on instruction " + currentInstruction.my_num
            );
            definedSet.add("(" + currentInstruction.my_num + ")");
            break;
          default:
            if (currentInstruction.left != null) {
              if (currentInstruction.left.isVariable()) {
                usedSet.add(currentInstruction.left.var.name);
              } else {
                if (currentInstruction.left.kind == Result.INST) {
                  usedSet.add("(" + currentInstruction.left.inst.my_num + ")");
                }
              }
            }
            if (currentInstruction.right != null) {
              if (currentInstruction.right.isVariable()) {
                usedSet.add(currentInstruction.right.var.name);
              } else {
                if (currentInstruction.right.kind == Result.INST) {
                  usedSet.add("(" + currentInstruction.right.inst.my_num + ")");
                }
              }
            }

            if (
              currentInstruction.left != null &&
              !currentInstruction.left.isVariable() &&
              currentInstruction.right != null &&
              !currentInstruction.right.isVariable()
            ) {
              // This is the following case:
              // 1: ADD 3 4
              // No variables, but the result must be saved in a register.
              // So we can add '1' to the outset
              // Remember, variables can't start with numbers so this is valid
              definedSet.add("(" + currentInstruction.my_num + ")");
            } else if (
              currentInstruction.left != null &&
              !currentInstruction.left.isVariable() &&
              currentInstruction.right != null &&
              currentInstruction.right.isVariable()
            ) {
              // This is the following case:
              // 1: ADD 3 a_1
              definedSet.add("(" + currentInstruction.my_num + ")");
            } else if (
              currentInstruction.left != null &&
              currentInstruction.left.isVariable() &&
              currentInstruction.right != null &&
              !currentInstruction.right.isVariable()
            ) {
              // This is the following case:
              // 1: ADD a_1 3
              definedSet.add("(" + currentInstruction.my_num + ")");
            }

            break;
        }

        // For out, find the union of previous variables in the in set for each succeeding node of n
        // out[n] := ∪ {in[s] | s ε succ[n]}
        // outSet of a node = the union of all the inSets of n's successors
        // Successor is simply j + 1
        if (!((i + 1) >= instructionSet.size())) {
          currentInstruction.OutSet.addAll(instructionSet.get(i + 1).InSet);
        }

        // in[n] := use[n] ∪ (out[n] - def[n])
        // (out[n] - def[n])
        HashSet<String> temporaryOutSet = new HashSet<String>();

        temporaryOutSet.addAll(currentInstruction.OutSet);
        temporaryOutSet.removeAll(definedSet);
        // use[n]
        usedSet.addAll(temporaryOutSet);
        currentInstruction.InSet = usedSet;

        boolean inSetChanged =
          (!originalInSet.equals(currentInstruction.InSet));
        boolean outSetChanged =
          (!originalOutSet.equals(currentInstruction.OutSet));
        if (inSetChanged || outSetChanged) {
          // This only needs to trigger once to repeat the loop
          change_detected = true;
        }
      }
      // Iterate, until IN and OUT set are constants for last two consecutive iterations.
    } while (change_detected);
  }

  private void printLiveness(Block block) {
    for (Instruction instruction : block.instructions) {
      System.out.println("-----------------------------------");
      System.out.println(instruction);
      System.out.println("InSet: " + instruction.InSet);
      System.out.println("OutSet: " + instruction.OutSet);
    }
    System.out.println();
  }

  private void printRegisterAllocation(
    HashMap<Integer, ArrayList<RegisterAlloc>> registerMap
  ) {
    System.out.println();
    for (Integer Register : registerMap.keySet()) {
      System.out.println("Allocation History for Register #" + Register + ":");
      if (registerMap.get(Register) != null) {
        for (RegisterAlloc regAllocation : registerMap.get(Register)) {
          System.out.println(regAllocation);
        }
        System.out.println("----------------------");
      }
    }
    System.out.println();
  }

  public ArrayList<String> findDeclarationOrder(
    HashMap<String, VariableInfo> liveIntervals
  ) {
    ArrayList<String> declarations = new ArrayList<>();
    // Iterate through all variables, and one by one remove the one with the lowest "opening"
    HashSet<String> availableVariables = new HashSet<String>(
      liveIntervals.keySet()
    );
    while (availableVariables.size() != 0) {
      String lowest_variable = availableVariables.iterator().next();
      int lowest_opening = liveIntervals.get(lowest_variable).opening;
      for (String variable : availableVariables) {
        // If this variable has the most low opening, add to declarations, remove from available variables
        int opening = liveIntervals.get(variable).opening;
        if (opening < lowest_opening) {
          lowest_opening = opening;
          lowest_variable = variable;
        }
      }
      declarations.add(lowest_variable);
      availableVariables.remove(lowest_variable);
    }

    return declarations;
  }

  private void allocateRegisters(
    Block block,
    int numRegs,
    HashMap<Integer, ArrayList<RegisterAlloc>> registerMap,
    HashMap<String, VariableInfo> liveIntervals
  ) {
    // We begin with the earliest opening variables
    ArrayList<String> declarations = findDeclarationOrder(
      allLiveIntervals.get(block)
    );
    // Fill registerMap here
    for (String variable : declarations) {
      // We will always start from the left most register
      boolean successfully_allocated = false;
      for (int registerNumber = 0; registerNumber < numRegs; registerNumber++) {
        if (registerMap.get(registerNumber) == null) {
          // This register has not been used before, so we are clear to assign
          int instruction_number = liveIntervals.get(variable).opening;
          ArrayList<RegisterAlloc> allocationHistory = new ArrayList<RegisterAlloc>();
          allocationHistory.add(
            new RegisterAlloc(instruction_number, variable)
          );
          Instruction k = ssa.allInstructions.get(instruction_number);
          //k.getResult().regno = registerNumber;
          registerMap.put((Integer) registerNumber, allocationHistory);
          successfully_allocated = true;
          break;
        } else {
          // The register has been used before
          // Check if it is available
          int instruction_number = liveIntervals.get(variable).opening;
          String currentlyStoredVariable = registerMap
            .get(registerNumber)
            .get(registerMap.get(registerNumber).size() - 1)
            .variable;
          int deathInstruction = liveIntervals.get(currentlyStoredVariable)
            .closing;

          // The current register is holding a dead variable, so we can replace it
          boolean holdingDeadVariable = instruction_number >= deathInstruction;
          if (holdingDeadVariable) {
            RegisterAlloc placement = new RegisterAlloc(
              Integer.valueOf(instruction_number),
              variable
            );
            registerMap.get(registerNumber).add(placement);
            successfully_allocated = true;
            break;
          } else { // If the instruction is not dead then we need to store first - TEMPORARY cuz im just storing what I am on and loading it later
            vaInstruction.storeThese.add(killInstruction.getResult()); // Place under store
            successfully_allocated = true;
          }
        }
      }

      if (!successfully_allocated) {
        System.out.println("We must spill!");
        System.out.println(
          "It was not possible to find room for variable " + variable
        );
        // Here we spill
        // Evict something from memory
        // Is there a good heuristic for this?
        // Find element with closest closing to this one's opening -- my code does the least used var
        // Evict that one
        // ------------- Momo code for eviction due to spilling --------------//
        // Get Instruction
        int instruction_num = liveIntervals.get(variable).opening;
        Instruction thisInstruction = liveIntervals.get(variable).instruction;
        // Get result that best matches
        Result loadResult = null;
        int min = 100000;
        for (
          int registerNumber = 0;
          registerNumber < numRegs;
          registerNumber++
        ) {
          String vars = registerMap
            .get(registerNumber)
            .get(registerMap.get(registerNumber).size() - 1)
            .variable;
          Instruction check = liveIntervals.get(vars).instruction; // MOVE is
          if (
            liveIntervals.get(vars).closing > instruction_num &&
            !vars.equals(variable) &&
            !instructionContainsResult(thisInstruction, check.getResult()) &&
            min > check.getResult().result_count
          ) {
            min = check.getResult().result_count;
            loadResult = check.getResult();
          }
        }
        thisInstruction.storeThese.add(loadResult); // Place under store

        continue;
      }
    }
  }

  private void printLiveIntervals(HashMap<String, VariableInfo> liveIntervals) {
    for (String variable : liveIntervals.keySet()) {
      System.out.println("Live Interval for variable \"" + variable + "\"");
      System.out.println(liveIntervals.get(variable));
    }
    System.out.println();
  }

  private void initializeLiveIntervals(
    HashMap<String, VariableInfo> liveIntervals,
    HashMap<String, ArrayList<VariableInfo>> liveRanges
  ) {
    for (String variable : liveRanges.keySet()) {
      liveIntervals.put(variable, null);
    }
  }

  private void calculateLiveIntervals(
    HashMap<String, VariableInfo> liveIntervals,
    HashMap<String, ArrayList<VariableInfo>> liveRanges
  ) {
    for (String variable : liveRanges.keySet()) {
      int left_boundary = liveRanges.get(variable).get(0).opening;
      int right_boundary = liveRanges
        .get(variable)
        .get(liveRanges.get(variable).size() - 1)
        .closing;
      liveIntervals.put(
        variable,
        new VariableInfo(left_boundary, right_boundary)
      );
    }
  }

  private void initializeRegisterMap(Block block, int numRegs) {
    allRegisterMaps.put(
      block,
      new HashMap<Integer, ArrayList<RegisterAlloc>>()
    );
    for (int i = 0; i < numRegs; i++) {
      allRegisterMaps.get(block).put(i, null);
    }
  }

  private void calculateliveRanges(
    HashMap<String, ArrayList<VariableInfo>> liveRanges,
    Block b
  ) {
    for (Instruction instruction : b.instructions) {
      for (String variable : instruction.InSet) {
        ArrayList<VariableInfo> PairList = liveRanges.get(variable);
        if (PairList.size() == 0) {
          VariableInfo newInterval = new VariableInfo(
            instruction.my_num,
            instruction
          );
          liveRanges.get(variable).add(newInterval);
        } else {
          VariableInfo mostRecentPair = PairList.get(PairList.size() - 1);
          if (mostRecentPair.closing == null) {
            // If the interval has not been closed, check if it closes now
            if (!instruction.OutSet.contains(variable)) {
              // If the outset doesn't contain this variable, it means it was used this line. So it closes
              mostRecentPair.closing = instruction.my_num;
            }
          } else {
            // There isn't currently a live interval - the most recent pair was a complete interval
            // A new interval must be created
            VariableInfo newInterval = new VariableInfo(
              instruction.my_num,
              instruction
            );
            liveRanges.get(variable).add(newInterval);
          }
        }
      }

      for (String variable : instruction.OutSet) {
        if (!instruction.InSet.contains(variable)) {
          // If a variable is not in the inset but in the outset, then it was defined on that instruction
          // Therefore a new interval starts here, regardless if the last pair was closed
          VariableInfo newInterval = new VariableInfo(
            instruction.my_num,
            instruction
          );
          liveRanges.get(variable).add(newInterval);
        }
      }
    }
  }

  private void populateliveRanges(
    HashMap<String, ArrayList<VariableInfo>> liveRanges,
    Block b
  ) {
    for (Instruction instruction : b.instructions) {
      for (String variable : instruction.InSet) {
        if (!liveRanges.containsKey(variable)) {
          // If this variable has not been added to the global list, add it
          ArrayList<VariableInfo> blankIntervalList = new ArrayList<VariableInfo>();
          liveRanges.put(variable, blankIntervalList);
        }
      }
    }
  }

  private void printliveRanges(
    HashMap<String, ArrayList<VariableInfo>> liveRanges
  ) {
    for (String variable : liveRanges.keySet()) {
      System.out.println("Live Ranges for variable \"" + variable + "\"");
      System.out.println(liveRanges.get(variable));
    }
    System.out.println();
  }
}
