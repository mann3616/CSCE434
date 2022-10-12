package pl434;

import ast.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import pl434.Token.Kind;
import types.*;

public class Compiler {

  class Result {

    int kind;
    int value;
    int address;
    String lexeme;
    Expression expression;
    int regno = -1;
    int cond, fixuplocation;
    boolean isFloat;

    static final int CONST = 1;
    static final int VAR = 2;
    static final int REG = 3;
    static final int CONDITION = 4;

    public Result clone() {
      Result res = new Result();
      res.kind = kind;
      res.value = value;
      res.address = address;
      res.lexeme = lexeme;
      res.expression = expression;
      res.regno = regno;
      res.cond = cond;
      res.fixuplocation = fixuplocation;
      res.isFloat = isFloat;
      return res;
    }
  }

  class LRU {

    private int size;
    private HashMap<Integer, DoublyLinkedList.Node> hm = new HashMap<>();
    DoublyLinkedList list = new DoublyLinkedList();

    class DoublyLinkedList {

      class Node {

        int item;
        Node previous;
        Node next;

        public Node(int item) {
          this.item = item;
        }
      }

      int size = 0;
      Node head;
      Node tail;

      public DoublyLinkedList() {
        head = new Node(0);
        tail = new Node(0);
        head.next = tail;
        tail.previous = head;
      }

      public Node push(int new_data) {
        Node new_node = new Node(new_data);
        head.next.previous = new_node;
        new_node.next = head.next;
        new_node.previous = head;
        head.next = new_node;
        size++;
        return new_node;
      }

      public int remove() {
        int res = tail.previous.item;
        tail.previous.previous.next = tail;
        tail.previous = tail.previous.previous;
        size--;
        return res;
      }

      public void getOp(Node p) {
        p.previous.next = p.next;
        p.next.previous = p.previous;

        head.next.previous = p;
        p.next = head.next;
        p.previous = head;
        head.next = p;
      }
    }

    public LRU(int size) {
      this.size = size;
      for (int i = 1; i < size - 1; i++) {
        put(i);
      }
    }

    public void put(int val) {
      //if(val== 2){
      //   System.out.println(val);
      //}
      if (hm.containsKey(val)) {
        list.getOp(hm.get(val));
      } else {
        if (list.size == size) {
          list.remove();
        }
        hm.put(val, list.push(val));
      }
    }

    public void get(int val) {
      list.getOp(hm.get(val));
    }

    public int last() {
      list.size--;
      size--;
      return list.remove();
    }

    public void print() {
      DoublyLinkedList.Node n = list.head;
      DoublyLinkedList.Node p = n;
      while (!p.next.equals(list.tail)) {
        p = n;
        System.out.print(p.next.item + " ");
        n = n.next;
      }
    }
  }

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
  private List<Integer> instructions;
  private int maxed = 0;

  // Need to map from IDENT to memory offset

  HashMap<String, MultiArray> dMap = new HashMap<>();
  HashMap<String, paramType> IDENT_TYPE = new HashMap<>();
  HashMap<String, func> IDENT_FUNC = new HashMap<>();
  HashMap<String, Result> IDENT_MEM = new HashMap<>();
  HashMap<Integer, Result> IDENT_REG = new HashMap<>();
  HashMap<String, Type> IDENT_VARTYPE = new HashMap<>();

  LRU lru;

  public Compiler(Scanner scanner, int numRegs) {
    this.scanner = scanner;
    currentToken = this.scanner.next();
    numDataRegisters = numRegs;
    lru = new LRU(this.numDataRegisters);
    instructions = new ArrayList<>();
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
      return null;
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

  public int[] compile() {
    initSymbolTable();
    try {
      computation();
      return instructions.stream().mapToInt(Integer::intValue).toArray();
    } catch (QuitParseException q) {
      errorBuffer.append(
        "SyntaxError(" + lineNumber() + "," + charPosition() + ")"
      );
      errorBuffer.append("[Could not complete parsing.]");
      return new ArrayList<Integer>()
        .stream()
        .mapToInt(Integer::intValue)
        .toArray();
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

  // OPTIONAL: may be useful to have help functions for the following:
  //              - branch forward
  //              - fix branch instructions
  //              - load/store
  //              - computing arithmetic
  //              - free/alloc reg

  // Pre-defined Functions ======================================================
  private void readInt(int rDest) {
    // TODO: implement with DLX.RDI
    instructions.add(DLX.assemble(DLX.RDI, rDest));
  }

  private void readFloat(int rDest) {
    // TODO: implement with DLX.RDF
    instructions.add(DLX.assemble(DLX.RDF, rDest));
  }

  private void readBool(int rDest) {
    // TODO: implement with DLX.RDB
    instructions.add(DLX.assemble(DLX.RDB, rDest));
  }

  private void printInt(int rSrc) {
    // TODO: implement with DLX.WRI
    instructions.add(DLX.assemble(DLX.WRI, rSrc));
  }

  private void printFloat(int rSrc) {
    // TODO: implement with DLX.WRF
    instructions.add(DLX.assemble(DLX.WRF, rSrc));
  }

  private void printBool(int rSrc) {
    // TODO: implement with DLX.WRB
    instructions.add(DLX.assemble(DLX.WRB, rSrc));
  }

  private void println() {
    // TODO: implement with DLX.WRL
    instructions.add(DLX.assemble(DLX.WRL));
  }

  // Grammar Rules ==============================================================

  // function for matching rule that only expects nonterminal's FIRST set
  private Token matchNonTerminal(NonTerminal nt) {
    return expectRetrieve(nt);
  }

  // TODO: copy operators and type grammar rules from Interpreter

  // literal = integerLit | floatLit
  private Token literal() {
    return matchNonTerminal(NonTerminal.LITERAL);
  }

  ArrayList<ArrayList<Integer>> loads = new ArrayList<>();

  private void deallocate(int reg) {
    // if(reg == 6){
    //    System.out.print(reg+" ");
    // }
    if (IDENT_REG.containsKey(reg)) {
      instructions.add(
        DLX.assemble(DLX.STW, reg, 30, 4 * IDENT_REG.get(reg).address)
      );
      for (String i : IDENT_MEM.keySet()) {
        if (IDENT_MEM.get(i).regno == reg) {
          IDENT_MEM.get(i).regno = -1;
          // if(reg == 6){
          //     System.out.println(i + " " + 4*IDENT_REG.get(reg).address);
          // }
          break;
        }
      }
      IDENT_REG.remove(reg);
    }
    // else if(reg == 6){
    //     System.out.println();
    // }
  }

  // designator = ident { "[" relExpr "]" }
  // Problem is that the regno 2 is still under i even though that regno has already been used for other situations
  private Result designator(String scope) {
    int lineNum = lineNumber();
    int charPos = charPosition();

    Token ident = expectRetrieve(Token.Kind.IDENT);

    String var = scope + ":" + ident.lexeme();
    Result x = IDENT_MEM.get(var);
    // if x is null what do we do... report error and return!
    Symbol simba = tryResolveVariable(ident);
    if (IDENT_FUNC.containsKey(ident.lexeme())) {
      x = IDENT_FUNC.get(ident.lexeme()).savior;
    }
    if (x == null) {
      // just set it to something so it doesn't throw an error
      x = IDENT_MEM.get("main:" + ident.lexeme());
    }
    if (x == null) {
      x = IDENT_MEM.get("main:error");
    } else {
      x.lexeme = ident.lexeme();
    }
    // Peek stack here
    // x might return null!

    if (x.regno != -1) {
      //System.out.println("-1: "+var + " is " + x.regno);
      lru.get(x.regno);
      x = x.clone();
    } else {
      int reg = allocate().regno;
      instructions.add(DLX.assemble(DLX.LDW, reg, 30, 4 * x.address));
      if (!loads.isEmpty()) {
        loads
          .get(loads.size() - 1)
          .add(DLX.assemble(DLX.LDW, reg, 30, 4 * x.address));
      }
      x.regno = reg;
    }
    x.expression = new AddressOf(lineNumber(), charPosition(), simba);
    //System.out.println(var + " is " + reg);
    // vartype is probably null here
    Stack<Expression> st = new Stack<>();
    while (accept(Kind.OPEN_BRACKET)) {
      Result intVal = relExpr(scope);
      st.push(intVal.expression);
      expect(Kind.CLOSE_BRACKET);
    }
    while (!st.isEmpty()) {
      x.expression = new ArrayIndex(0, 0, x.expression, st.pop());
    }
    IDENT_REG.put(x.regno, x);
    IDENT_MEM.put(var, x);
    return x;
  }

  private void varDecl(String scope, DeclarationList vars) {
    DeclarationList variableList = new DeclarationList(
      lineNumber(),
      charPosition()
    );
    HashMap<String, Symbol> global = new HashMap<>();
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
      ArrayList<Integer> dims = new ArrayList<>();
      while (accept(Kind.OPEN_BRACKET)) {
        Token intVal = expectRetrieve(Kind.INT_VAL);
        expect(Kind.CLOSE_BRACKET);
        dims.add(Integer.parseInt(intVal.lexeme()));
      }
      if (!dims.isEmpty()) {
        variableType = new ArrayType(variableType, dims.size(), dims);
      }
      do {
        Token currentVariable = expectRetrieve(Kind.IDENT);
        String id = currentVariable.lexeme();
        Result x = new Result();
        x.kind = Result.VAR;
        x.address = ++maxed * -1;
        global.put(id, new Symbol(id, variableType));
        VariableDeclaration generatedVariable = new VariableDeclaration(
          lineNumber(),
          charPosition(),
          global.get(id)
        );
        variableList.add(generatedVariable);
        tryDeclareVariable(currentVariable, global.get(id));
        // store the variable name and its type
        IDENT_VARTYPE.put(scope + ":" + currentVariable.lexeme(), variableType);
        //System.out.println(id + " " + x.address*4);
        if (type.kind() == Kind.FLOAT) {
          x.isFloat = true;
        } else {
          x.isFloat = false;
        }
        IDENT_MEM.put(scope + ":" + id, x);
      } while (accept(Kind.COMMA));
      expect(Kind.SEMICOLON);
    }
    vars.list = variableList.list;
  }

  private Result statSeq(String scope, StatementSequence statementList) {
    // Add all actions to statementList
    StatementSequence currentStatementList = new StatementSequence(
      lineNumber(),
      charPosition()
    );
    Result o = statement(scope, currentStatementList);
    // should add to the passed in thing, not set it
    expect(Kind.SEMICOLON);
    // Generate the statement and add it to here
    while (have(NonTerminal.STATEMENT)) {
      if (o == null) {
        o = statement(scope, currentStatementList);
      } else {
        statement(scope, currentStatementList);
      }
      expect(Kind.SEMICOLON);
      // Now add the finished node
    }
    if (goback != 0) {
      instructions.add(
        goback,
        DLX.assemble(DLX.BSR, instructions.size() - goback + 1)
      );
      goback = 0;
    }
    statementList.statements = currentStatementList.statements;
    // o's regno should not be -1
    return o;
  }

  int goback = 0;

  private Result statement(String scope, StatementSequence statementList) {
    if (have(NonTerminal.STATEMENT)) {
      switch (currentToken.kind()) {
        case WHILE:
          whileStat(scope, statementList);
          break;
        case RETURN:
          Result o = returnStat(scope, statementList);
          goback = instructions.size();
          return o;
        case REPEAT:
          repeatStat(scope, statementList);
          break;
        case IF:
          ifStat(scope, statementList);
          break;
        case LET:
          letStat(scope, statementList);
          break;
        case CALL:
          return funcCall(scope, statementList);
      }
    }
    return null;
  }

  private Result whileStat(String scope, StatementSequence statementList) {
    Token whileToken = currentToken;
    expect(Kind.WHILE);
    Token start = currentToken;
    int before = instructions.size();
    Result rel = relation(scope);
    expect(Kind.DO);
    StatementSequence whileStatementSequence = new StatementSequence(
      lineNumber(),
      charPosition()
    );
    statSeq(scope, whileStatementSequence);
    instructions.add(
      DLX.assemble(DLX.BNE, rel.regno, before - instructions.size())
    );
    instructions.add(
      before,
      DLX.assemble(DLX.BEQ, rel.regno, instructions.size() - before + 2)
    );
    expect(Kind.OD);
    statementList.add(
      new WhileStatement(
        start.lineNumber(),
        start.charPosition(),
        rel.expression,
        whileStatementSequence
      )
    );
    return null;
  }

  private Result repeatStat(String scope, StatementSequence statementList) {
    // create repeat statement
    // use new stat list
    Token repeatToken = currentToken;
    expect(Kind.REPEAT);
    Token start = currentToken;
    StatementSequence repeatStatementList = new StatementSequence(
      lineNumber(),
      charPosition()
    );
    statSeq(scope, repeatStatementList);
    // Don't need to compule
    expect(Kind.UNTIL);
    expect(Kind.OPEN_PAREN);
    // Relation is here
    // Manipulate x.expression
    Result x = relExpr(scope);
    // generate relation statement, add to statement list at end
    // Some expression
    expect(Kind.CLOSE_PAREN);

    statementList.add(
      new RepeatStatement(
        start.lineNumber(),
        start.charPosition(),
        repeatStatementList,
        x.expression
      )
    );
    return null;
  }

  private void letStat(String scope, StatementSequence statementList) {
    // Create assignment, add assignment to statement sequence
    Token letToken = currentToken;
    expect(Kind.LET);
    String id = currentToken.lexeme();
    Result obj = designator(scope);
    if (have(NonTerminal.UNARY_OP)) {
      Token tok = expectRetrieve(NonTerminal.UNARY_OP);
      switch (tok.kind()) {
        case UNI_DEC:
          if (obj.isFloat) {
            instructions.add(
              DLX.assemble(DLX.fSUBI, obj.regno, obj.regno, (float) 1.0)
            );
          } else {
            instructions.add(DLX.assemble(DLX.SUBI, obj.regno, obj.regno, 1));
          }
          statementList.add(
            Node.newAssignment(
              letToken.lineNumber(),
              letToken.charPosition(),
              obj.expression,
              tok,
              new IntegerLiteral(lineNumber(), charPosition(), "1")
            )
          );
          break;
        case UNI_INC:
          if (obj.isFloat) {
            instructions.add(
              DLX.assemble(DLX.fADDI, obj.regno, obj.regno, (float) 1.0)
            );
          } else {
            instructions.add(DLX.assemble(DLX.ADDI, obj.regno, obj.regno, 1));
          }
          statementList.add(
            Node.newAssignment(
              letToken.lineNumber(),
              letToken.charPosition(),
              obj.expression,
              tok,
              new IntegerLiteral(lineNumber(), charPosition(), "1")
            )
          );
          break;
      }
      deallocate(obj.regno);
      return;
    }
    Token tok = expectRetrieve(NonTerminal.ASSIGN_OP);
    if (tok.kind() == Kind.ASSIGN) {
      IDENT_REG.remove(obj.regno);
      Result second = relExpr(scope);
      obj.regno = second.regno;
      IDENT_REG.put(obj.regno, obj);
      deallocate(obj.regno);
      if (IDENT_VARTYPE.get(scope + ":" + id) != null) {
        statementList.add(
          Node.newAssignment(
            letToken.lineNumber(),
            letToken.charPosition(),
            obj.expression,
            tok,
            second.expression
          )
        );
      } else {
        statementList.add(
          Node.newAssignment(
            letToken.lineNumber(),
            letToken.charPosition(),
            obj.expression,
            tok,
            second.expression
          )
        );
      }
      return;
    }
    if (!obj.isFloat) {
      Result second = relExpr(scope);

      switch (tok.kind()) {
        case ADD_ASSIGN:
          instructions.add(
            DLX.assemble(DLX.ADD, obj.regno, obj.regno, second.regno)
          );
          break;
        case MOD_ASSIGN:
          instructions.add(
            DLX.assemble(DLX.MOD, obj.regno, obj.regno, second.regno)
          );
          break;
        case MUL_ASSIGN:
          instructions.add(
            DLX.assemble(DLX.MUL, obj.regno, obj.regno, second.regno)
          );
          break;
        case SUB_ASSIGN:
          instructions.add(
            DLX.assemble(DLX.SUB, obj.regno, obj.regno, second.regno)
          );
          break;
        case DIV_ASSIGN:
          instructions.add(
            DLX.assemble(DLX.DIV, obj.regno, obj.regno, second.regno)
          );
          break;
        case POW_ASSIGN:
          instructions.add(
            DLX.assemble(DLX.POW, obj.regno, obj.regno, second.regno)
          );
          break;
      }
      statementList.add(
        Node.newAssignment(
          letToken.lineNumber(),
          letToken.charPosition(),
          obj.expression,
          tok,
          second.expression
        )
      );
    } else {
      Result second = relExpr(scope);
      switch (tok.kind()) {
        case ADD_ASSIGN:
          instructions.add(
            DLX.assemble(DLX.fADD, obj.regno, obj.regno, second.regno)
          );
          break;
        case MOD_ASSIGN:
          instructions.add(
            DLX.assemble(DLX.fMOD, obj.regno, obj.regno, second.regno)
          );
          break;
        case MUL_ASSIGN:
          instructions.add(
            DLX.assemble(DLX.fMUL, obj.regno, obj.regno, second.regno)
          );
          break;
        case SUB_ASSIGN:
          instructions.add(
            DLX.assemble(DLX.fSUB, obj.regno, obj.regno, second.regno)
          );
          break;
        case DIV_ASSIGN:
          instructions.add(
            DLX.assemble(DLX.fDIV, obj.regno, obj.regno, second.regno)
          );
          break;
      }
      statementList.add(
        Node.newAssignment(
          letToken.lineNumber(),
          letToken.charPosition(),
          obj.expression,
          tok,
          second.expression
        )
      );
    }
    deallocate(obj.regno);
  }

  private Result returnStat(String scope, StatementSequence statementList) {
    Token returnToken = currentToken;
    expect(Kind.RETURN);
    Result x = new Result();
    if (!have(Kind.SEMICOLON)) {
      x = relExpr(scope);
      statementList.add(
        new ReturnStatement(lineNumber(), charPosition(), x.expression)
      );
      return x;
    }
    // Not returning everything so give it a null instance
    statementList.add(new ReturnStatement(lineNumber(), charPosition(), null));
    return null;
  }

  private void ifStat(String scope, StatementSequence statementList) {
    Token ifToken = currentToken;
    expect(Kind.IF);
    loads.add(new ArrayList<Integer>());
    Result b = relation(scope);
    expect(Kind.THEN);
    int getem = instructions.size();
    lru.get(b.regno);
    StatementSequence ifStatementList = new StatementSequence(
      lineNumber(),
      charPosition()
    );
    StatementSequence elseStatementList = new StatementSequence(
      lineNumber(),
      charPosition()
    );
    statSeq(scope, ifStatementList);
    instructions.add(
      getem,
      DLX.assemble(DLX.BEQ, b.regno, instructions.size() - getem + 2)
    );
    getem = instructions.size();
    instructions.addAll(loads.remove(loads.size() - 1));
    lru.get(b.regno);
    if (accept(Kind.ELSE)) {
      loads.add(new ArrayList<Integer>());
      statSeq(scope, elseStatementList);
      instructions.add(
        getem,
        DLX.assemble(DLX.BNE, b.regno, instructions.size() - getem + 1)
      );
      instructions.addAll(loads.remove(loads.size() - 1));
    }
    expect(Kind.FI);
    if (elseStatementList.statements.size() != 0) {
      statementList.add(
        new IfStatement(
          lineNumber(),
          charPosition(),
          b.expression,
          ifStatementList,
          elseStatementList
        )
      );
    } else {
      statementList.add(
        new IfStatement(
          lineNumber(),
          charPosition(),
          b.expression,
          ifStatementList,
          null
        )
      );
    }
  }

  private Result groupExpr(String scope) {
    Result x = new Result();
    x.kind = Result.CONST;
    if (have(Kind.NOT)) {
      Token tok = expectRetrieve(Kind.NOT);
      Result xx = relExpr(scope);
      x.regno = xx.regno;
      instructions.add(DLX.assemble(DLX.XORI, x.regno, xx.regno, 1));
      x.isFloat = false;
      x.expression = Node.newExpression(null, tok, xx.expression);
      return x;
    }
    if (have(NonTerminal.LITERAL)) {
      Token tok = expectRetrieve(NonTerminal.LITERAL);
      x = allocate();
      x.kind = Result.CONST;
      switch (tok.kind()) {
        case INT_VAL:
          int val = Integer.parseInt(tok.lexeme());
          instructions.add(DLX.assemble(DLX.ADDI, x.regno, 0, val));
          x.expression =
            new IntegerLiteral(lineNumber(), charPosition(), tok.lexeme());
          x.isFloat = false;
          return x;
        case FLOAT_VAL:
          float fval = Float.parseFloat(tok.lexeme());
          instructions.add(DLX.assemble(DLX.fADDI, x.regno, 0, fval));
          x.expression =
            new FloatLiteral(lineNumber(), charPosition(), tok.lexeme());
          x.isFloat = true;
          return x;
        case TRUE:
          instructions.add(DLX.assemble(DLX.ADDI, x.regno, 0, 1));
          x.isFloat = false;
          x.expression =
            new BoolLiteral(lineNumber(), charPosition(), tok.lexeme());
          return x;
        case FALSE:
          instructions.add(DLX.assemble(DLX.ADDI, x.regno, 0, 0));
          x.expression =
            new BoolLiteral(lineNumber(), charPosition(), tok.lexeme());
          x.isFloat = false;
          return x;
      }
    }
    if (have(NonTerminal.DESIGNATOR)) {
      Result xx = designator(scope);
      x.regno = allocate().regno;
      instructions.add(DLX.assemble(DLX.ADD, x.regno, xx.regno, 0));
      x.isFloat = xx.isFloat;
      x.lexeme = xx.lexeme;
      x.expression = xx.expression;
      // address of
      // Result should be dereference?
      return x;
    }
    if (have(NonTerminal.RELATION)) {
      Result xx = relation(scope);
      x.expression = xx.expression;
      x.regno = xx.regno;
      lru.get(x.regno);
      x.isFloat = xx.isFloat;
      return x;
    }
    if (accept(NonTerminal.FUNC_CALL)) {
      // Calling a function!
      // make local arg list?
      // when doing call should end up here
      Result xx = funcCall(scope, null);
      x.regno = xx.regno;
      lru.get(x.regno);
      x.isFloat = xx.isFloat;
      x.expression = xx.expression;
      return x;
    }
    String errorMessage = reportSyntaxError(NonTerminal.GROUP_EXPRESSION);
    throw new QuitParseException(errorMessage);
  }

  private Result powExpr(String scope) {
    Result obj = groupExpr(scope);
    Token tok = currentToken;
    Stack<Result> res = new Stack<>();
    Stack<Token> toks = new Stack<>();
    res.push(obj);
    while (accept(NonTerminal.POW_OP)) {
      Result obj2 = groupExpr(scope);
      toks.push(tok);
      res.push(obj2);
      instructions.add(DLX.assemble(DLX.POW, obj.regno, obj.regno, obj2.regno));
      lru.get(obj.regno);
    }
    obj.expression = makeExpression(res, toks);
    return obj;
  }

  private Result multExpr(String scope) {
    Result obj = powExpr(scope);
    Stack<Result> res = new Stack<>();
    Stack<Token> toks = new Stack<>();
    res.push(obj);
    while (have(NonTerminal.MULT_OP)) {
      Token tok = expectRetrieve(NonTerminal.MULT_OP);
      Result obj2 = powExpr(scope);
      obj.isFloat = obj.isFloat || obj2.isFloat;
      toks.push(tok);
      res.push(obj2);
      if (obj.isFloat) {
        switch (tok.kind()) {
          case MUL:
            instructions.add(
              DLX.assemble(DLX.fMUL, obj.regno, obj.regno, obj2.regno)
            );
            break;
          case DIV:
            instructions.add(
              DLX.assemble(DLX.fDIV, obj.regno, obj.regno, obj2.regno)
            );
            break;
          case MOD:
            instructions.add(
              DLX.assemble(DLX.fMOD, obj.regno, obj.regno, obj2.regno)
            );
            break;
          case AND:
            instructions.add(
              DLX.assemble(DLX.AND, obj.regno, obj.regno, obj2.regno)
            );
            break;
        }
      } else {
        switch (tok.kind()) {
          case MUL:
            instructions.add(
              DLX.assemble(DLX.MUL, obj.regno, obj.regno, obj2.regno)
            );
            break;
          case DIV:
            instructions.add(
              DLX.assemble(DLX.DIV, obj.regno, obj.regno, obj2.regno)
            );
            break;
          case MOD:
            instructions.add(
              DLX.assemble(DLX.MOD, obj.regno, obj.regno, obj2.regno)
            );
            break;
          case AND:
            instructions.add(
              DLX.assemble(DLX.AND, obj.regno, obj.regno, obj2.regno)
            );
            break;
        }
      }
      lru.get(obj.regno);
    }
    obj.expression = makeExpression(res, toks);
    return obj;
  }

  private Result addExpr(String scope) {
    Result obj = multExpr(scope);
    Stack<Result> res = new Stack<>();
    Stack<Token> toks = new Stack<>();
    res.push(obj);
    while (have(NonTerminal.ADD_OP)) {
      Token tok = expectRetrieve(NonTerminal.ADD_OP);
      Result obj2 = multExpr(scope);
      obj.isFloat = obj.isFloat || obj2.isFloat;
      toks.push(tok);
      res.push(obj2);
      if (obj.isFloat) {
        switch (tok.kind()) {
          case ADD:
            instructions.add(
              DLX.assemble(DLX.fADD, obj.regno, obj.regno, obj2.regno)
            );
            break;
          case SUB:
            instructions.add(
              DLX.assemble(DLX.fSUB, obj.regno, obj.regno, obj2.regno)
            );
            break;
          case OR:
            instructions.add(
              DLX.assemble(DLX.OR, obj.regno, obj.regno, obj2.regno)
            );
            break;
        }
      } else {
        switch (tok.kind()) {
          case ADD:
            //System.out.println("Is this illegal addExpr");
            instructions.add(
              DLX.assemble(DLX.ADD, obj.regno, obj.regno, obj2.regno)
            );
            break;
          case SUB:
            instructions.add(
              DLX.assemble(DLX.SUB, obj.regno, obj.regno, obj2.regno)
            );
            break;
          case OR:
            instructions.add(
              DLX.assemble(DLX.OR, obj.regno, obj.regno, obj2.regno)
            );
            break;
        }
      }
      lru.get(obj.regno);
    }
    obj.expression = makeExpression(res, toks);
    return obj;
  }

  private Result relExpr(String scope) {
    Result obj = addExpr(scope);
    if (have(NonTerminal.REL_OP)) {
      Token tok = expectRetrieve(NonTerminal.REL_OP);
      Result obj2 = relExpr(scope);
      instructions.add(DLX.assemble(DLX.SUB, obj.regno, obj.regno, obj2.regno));
      switch (tok.kind()) {
        case EQUAL_TO:
          instructions.add(DLX.assemble(DLX.BEQ, obj.regno, 3));
          obj.expression =
            new Relation(
              lineNumber(),
              charPosition(),
              tok,
              obj.expression,
              obj2.expression
            );
          break;
        case NOT_EQUAL:
          instructions.add(DLX.assemble(DLX.BNE, obj.regno, 3));
          obj.expression =
            new Relation(
              lineNumber(),
              charPosition(),
              tok,
              obj.expression,
              obj2.expression
            );
          break;
        case LESS_EQUAL:
          instructions.add(DLX.assemble(DLX.BLE, obj.regno, 3));
          obj.expression =
            new Relation(
              lineNumber(),
              charPosition(),
              tok,
              obj.expression,
              obj2.expression
            );
          break;
        case LESS_THAN:
          instructions.add(DLX.assemble(DLX.BLT, obj.regno, 3));
          obj.expression =
            new Relation(
              lineNumber(),
              charPosition(),
              tok,
              obj.expression,
              obj2.expression
            );
          break;
        case GREATER_EQUAL:
          instructions.add(DLX.assemble(DLX.BGE, obj.regno, 3));
          obj.expression =
            new Relation(
              lineNumber(),
              charPosition(),
              tok,
              obj.expression,
              obj2.expression
            );
          break;
        case GREATER_THAN:
          instructions.add(DLX.assemble(DLX.BGT, obj.regno, 3));
          obj.expression =
            new Relation(
              lineNumber(),
              charPosition(),
              tok,
              obj.expression,
              obj2.expression
            );
          break;
      }
      instructions.add(DLX.assemble(DLX.ADDI, obj.regno, 0, 0));
      instructions.add(DLX.assemble(DLX.BSR, 2));
      instructions.add(DLX.assemble(DLX.ADDI, obj.regno, 0, 1));
      lru.get(obj.regno);
    }
    return obj;
  }

  private Result relation(String scope) {
    expect(Kind.OPEN_PAREN);
    // resolves
    Result o = relExpr(scope);
    expect(Kind.CLOSE_PAREN);
    return o;
  }

  private Expression makeExpression(Stack<Result> res, Stack<Token> toks) {
    if (toks.empty()) {
      return res.pop().expression;
    }
    Result ress = res.pop();
    Token tok = toks.pop();
    return Node.newExpression(makeExpression(res, toks), tok, ress.expression);
  }

  private Result allocate() {
    int reg = lru.last();
    lru.put(reg);
    deallocate(reg);
    Result res = new Result();
    res.regno = reg;
    res.kind = Result.CONST;
    return res;
  }

  //Function
  private Result funcCall(String scope, StatementSequence statementList) {
    // Have something outside of this scope point to the arg list
    // Resolve, and then add to statementList
    // In a new function so we should enter the function scope
    Token funcCallToken = currentToken;
    expect(Kind.CALL);
    Token functionToken = expectRetrieve(Kind.IDENT);
    tryResolveVariable(functionToken);
    String n = functionToken.lexeme();
    expect(Kind.OPEN_PAREN);
    ArgumentList localArgumentList = new ArgumentList(
      lineNumber(),
      charPosition()
    );
    TypeList paramTypeList = new TypeList();
    Type returnType = new VoidType();
    Token parameterToken = currentToken;
    Result a = new Result();
    a.kind = Result.CONST;
    a.lexeme = n;
    Result result = new Result();
    FuncType funcType;
    Symbol function;
    // in a new function so we should have a new scope at some point, dont know how to add that\
    switch (n) {
      case "printInt":
        result = relExpr(scope);
        a.regno = result.regno;
        a.isFloat = false;
        printInt(a.regno);
        returnType = new VoidType();
        paramTypeList.append(new IntType());
        localArgumentList.append(result.expression);
        break;
      case "printFloat":
        result = relExpr(scope);
        a.regno = result.regno;
        a.isFloat = false;
        printFloat(a.regno);
        returnType = new VoidType();
        paramTypeList.append(new FloatType());
        localArgumentList.append(result.expression);
        break;
      case "printBool":
        result = relExpr(scope);
        a.regno = result.regno;
        a.isFloat = false;
        printBool(a.regno);
        returnType = new VoidType();
        paramTypeList.append(new BoolType());
        localArgumentList.append(result.expression);
        break;
      case "readInt":
        a.regno = allocate().regno;
        readInt(a.regno);
        a.isFloat = false;
        returnType = new IntType();
        funcType = new FuncType(paramTypeList, returnType);
        function = new Symbol(n, funcType);
        a.expression =
          new FunctionCall(
            lineNumber(),
            charPosition(),
            function,
            localArgumentList
          );
        break;
      case "readFloat":
        a.regno = allocate().regno;
        readFloat(a.regno);
        a.isFloat = true;
        returnType = new FloatType();
        funcType = new FuncType(paramTypeList, returnType);
        function = new Symbol(n, funcType);
        a.expression =
          new FunctionCall(
            lineNumber(),
            charPosition(),
            function,
            localArgumentList
          );
        break;
      case "readBool":
        a.regno = allocate().regno;
        readBool(a.regno);
        a.isFloat = false;
        returnType = new BoolType();
        funcType = new FuncType(paramTypeList, returnType);
        function = new Symbol(n, funcType);
        a.expression =
          new FunctionCall(
            lineNumber(),
            charPosition(),
            function,
            localArgumentList
          );
        break;
      case "println":
        println();
        break;
      default:
        if (IDENT_FUNC.get(n) != null) {
          ArrayList<Result> vars = new ArrayList<>();
          // This is the function expression thing
          // get regno of function?
          a.regno = allocate().regno;
          while (!have(Kind.CLOSE_PAREN)) {
            Result var = relExpr(scope);
            localArgumentList.append(var.expression);
            vars.add(var);
            accept(Kind.COMMA);
          }
          setFuncVar(vars, n);
          instructions.add(DLX.assemble(DLX.JSR, IDENT_FUNC.get(n).jumper * 4));
          if (IDENT_FUNC.get(n).result != null) {
            lru.get(a.regno);
            instructions.add(
              DLX.assemble(
                DLX.LDW,
                a.regno,
                30,
                IDENT_FUNC.get(n).result.address * -4
              )
            );
          }
          // How to get func type?
          func f = IDENT_FUNC.get(n);
          function = new Symbol(n, f.funcType);
          // function here is being an issue
          // not giving correct arglist or function type
          a.expression =
            new FunctionCall(
              lineNumber(),
              charPosition(),
              function,
              localArgumentList
            );
          // arg list is an issue,, it was declared in here so it will not work because it disappears when we leave the function
          FunctionCall functionCall = new FunctionCall(
            functionToken.lineNumber(),
            functionToken.charPosition(),
            new Symbol(n, f.funcType),
            new ArgumentList(localArgumentList)
          );
          if (!(statementList == null)) {
            statementList.add(functionCall);
          }
          expect(Kind.CLOSE_PAREN);
          return a;
        } else {
          // Parse it, we have name so accept a list of parameters
          // Then we can release to close_paren
          ArrayList<Result> vars = new ArrayList<>();
          // This is the function expression thing
          Result var = relExpr(scope);
          // get regno of function?
          while (!have(Kind.CLOSE_PAREN)) {
            expect(Kind.COMMA);
            vars.add(var);
            var = relExpr(scope);
          }
          // We don't do anything with vars, just let this happen
        }
    }
    expect(Kind.CLOSE_PAREN);
    funcType = new FuncType(paramTypeList, returnType);
    function = new Symbol(n, funcType);
    FunctionCall functionCall = new FunctionCall(
      functionToken.lineNumber(),
      functionToken.charPosition(),
      function,
      new ArgumentList(localArgumentList)
    );
    if (!(statementList == null)) {
      statementList.add(functionCall);
    } else {
      a.expression = functionCall;
    }
    return a;
  }

  private void setFuncVar(ArrayList<Result> res, String scope) {
    int i = 0;
    for (String s : IDENT_FUNC.get(scope).names) {
      //Later change this so that constants get saved
      if (i == res.size()) {
        return;
      }
      IDENT_MEM.get(scope + ":" + s).regno = res.get(i).regno;
      IDENT_REG.put(res.get(i).regno, IDENT_MEM.get(scope + ":" + s));
      deallocate(IDENT_MEM.get(scope + ":" + s).regno);
      i++;
    }
  }

  private void funcDecl(DeclarationList funcs) {
    // Function stuff needs to be fleshed out before DeclarationList logic can be continued
    Token functionIdent = currentToken;
    DeclarationList functionList = new DeclarationList(
      lineNumber(),
      charPosition()
    );
    // Functions have their own statement sequence
    while (accept(NonTerminal.FUNC_DECL)) {
      Token id = expectRetrieve(Kind.IDENT);
      func function = new func(id.lexeme());
      IDENT_FUNC.put(id.lexeme(), function);
      DeclarationList variableList = new DeclarationList(
        lineNumber(),
        charPosition()
      );
      // probably need to pass variable list too
      // abt to declare variables for this
      TypeList typeList = new TypeList();
      enterScope();
      formalParam(id.lexeme(), typeList);
      expect(Kind.COLON);
      Token tok = expectRetrieve(NonTerminal.TYPE_DECL);
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
      // List of param decl
      FuncType functionType = new FuncType(typeList, returnType);
      function.funcType = functionType;
      IDENT_FUNC.remove(id.lexeme());
      IDENT_FUNC.put(id.lexeme(), function);
      // This is just the return type, I think it needs to be better
      // New problem! function body statseq is EMPTY!
      IDENT_FUNC.get(id.lexeme()).funcType = functionType;
      StatementSequence statementList = new StatementSequence(
        lineNumber(),
        charPosition()
      );

      tryDeclareVariable(id, new Symbol(id.lexeme(), functionType));
      // funcbody calls funccall
      // we should give the param definition first before anything
      funcBody(id.lexeme(), variableList, statementList);
      // Func declaration needs to resolve before function call
      FunctionDeclaration functionDeclaration = new FunctionDeclaration(
        functionIdent.lineNumber(),
        functionIdent.charPosition(),
        new Symbol(id.lexeme(), functionType),
        new FunctionBody(
          lineNumber(),
          charPosition(),
          variableList,
          statementList
        )
      );
      functionList.add(functionDeclaration);
      // Add function to scope
      exitScope();
      tryDeclareVariable(id, new Symbol(id.lexeme(), functionType));
    }
    funcs.list = functionList.list;
  }

  private void formalParam(String scope, TypeList typeList) {
    expect(NonTerminal.FORMAL_PARAM);
    TypeList localTypeList = new TypeList();
    if (have(NonTerminal.PARAM_DECL)) {
      paramDecl(scope, localTypeList);
      while (accept(Kind.COMMA)) {
        // should add
        paramDecl(scope, localTypeList);
      }
    }
    expect(Kind.CLOSE_PAREN);
    typeList.list = localTypeList.list;
  }

  private void paramDecl(String scope, TypeList typeList) {
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
    Result x = new Result();
    x.kind = Result.VAR;
    x.address = ++maxed * -1;
    if (type.kind() == Kind.FLOAT) {
      x.isFloat = true;
    } else {
      x.isFloat = false;
    }
    IDENT_FUNC.get(scope).names.add(id);
    IDENT_MEM.put(scope + ":" + id, x);
    IDENT_VARTYPE.put(scope + ":" + id, paramType);
    typeList.append(paramType);
  }

  private void funcBody(
    String scope,
    DeclarationList vars,
    StatementSequence statementSeq
  ) {
    expect(Kind.OPEN_BRACE);
    varDecl(scope, vars);
    int prior = instructions.size();
    instructions.add(
      DLX.assemble(DLX.STW, 31, 30, IDENT_MEM.get(scope).address * 4 * -1)
    );
    deallocate(IDENT_MEM.get(scope).regno);
    // we pass in the statement sequence and it should change but it doesnt
    Result xx = statSeq(scope, statementSeq);
    IDENT_FUNC.get(scope).jumper = prior + 1;
    //if xxregno is -1 then there's an issue
    if (xx != null && xx.regno != -1) {
      instructions.add(
        DLX.assemble(
          DLX.STW,
          xx.regno,
          30,
          IDENT_FUNC.get(scope).result.address * 4 * -1
        )
      );
      instructions.add(
        DLX.assemble(DLX.LDW, 31, 30, IDENT_MEM.get(scope).address * 4 * -1)
      );
      instructions.add(DLX.assemble(DLX.RET, 31));
      int after = instructions.size();
      instructions.add(prior, DLX.assemble(DLX.BSR, after - prior + 1));
    }
    expect(Kind.CLOSE_BRACE);
    expect(Kind.SEMICOLON);
  }

  private paramType saveVar(String scope, Token type, int dim, int[] size) {
    Token id = expectRetrieve(Kind.IDENT);
    IDENT_TYPE.put(scope + ":" + id.lexeme(), new paramType(type, dim));
    dMap.put(scope + ":" + id.lexeme(), new MultiArray(type, dim));
    dMap.get(scope + ":" + id.lexeme()).fillArray(size);
    return IDENT_TYPE.get(scope + ":" + id.lexeme());
  }

  private paramType saveVar(String scope, Token type, int dim) {
    Token id = expectRetrieve(Kind.IDENT);
    IDENT_TYPE.put(scope + ":" + id.lexeme(), new paramType(type, dim));
    dMap.put(scope + ":" + id.lexeme(), new MultiArray(type, dim));
    return IDENT_TYPE.get(scope + ":" + id.lexeme());
  }

  private class paramType {

    public Token _type;
    public int dim;

    public paramType(Token _type, int dim) {
      this._type = _type;
      this.dim = dim;
    }
  }

  private class func {

    public FuncType funcType; // Stores return type
    String name;
    int jumper;
    Result savior;
    Result result;
    public ArrayList<paramType> formalParam = new ArrayList<>();
    public ArrayList<String> names = new ArrayList<>();

    public func(String name) {
      this.name = name;
      savior = new Result();
      savior.address = ++maxed;
      savior.kind = Result.VAR;
      IDENT_MEM.put(name, savior);

      result = new Result();
      result.address = ++maxed;
      result.kind = Result.VAR;
      // Shouldn't this be (name, result) ?
      IDENT_MEM.put(name, savior);
    }
  }

  private class MultiArray {

    public ArrayList<Object> data;
    public Object _data;
    public Token type;
    public int dim;

    public MultiArray(Token type, int dim) {
      this.dim = dim;
      this.type = type;
    }

    public void fillArray(int[] sizes) {
      ArrayList<Object> current = null;
      ArrayList<Object> previous = null;
      for (int i = dim - 1; i >= 0; i--) {
        current = new ArrayList<Object>(sizes[i]);
        if (previous != null) {
          for (int j = 0; j < current.size(); j++) {
            current.add(previous.clone());
          }
        }
        previous = current;
      }
      data = current;
    }

    public Object getObj(int[] vals) {
      ArrayList<Object> curr = data;
      for (int i : vals) {
        if (curr.get(i) instanceof ArrayList<?>) {
          curr = (ArrayList<Object>) curr.get(i);
        } else {
          return curr.get(i);
        }
      }
      return null;
    }
  }

  Computation node;
  DeclarationList vars;
  DeclarationList funcs;
  StatementSequence mainSeq;

  private void computation() {
    initSymbolTable();
    Result res = new Result();
    res.address = 0;
    res.lexeme = "error";
    IDENT_MEM.put("main:error", res);
    enterScope();
    Token mainToken = currentToken;
    expect(Kind.MAIN);

    TypeList list = new TypeList();
    list.append(new IntType());
    FuncType type = new FuncType(list, new VoidType());
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
    tryDeclareVariable(new Token("println", 0, 0), new Symbol("println", type));

    list = new TypeList();
    type = new FuncType(list, new IntType());
    tryDeclareVariable(new Token("readInt", 0, 0), new Symbol("readInt", type));

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

    IDENT_FUNC.put("printInt", new func(""));
    IDENT_FUNC.put("printFloat", new func(""));
    IDENT_FUNC.put("printBool", new func(""));
    IDENT_FUNC.put("readInt", new func(""));
    IDENT_FUNC.put("readFloat", new func(""));
    IDENT_FUNC.put("readBool", new func(""));
    IDENT_FUNC.put("println", new func(""));

    vars = new DeclarationList(lineNumber(), charPosition());
    varDecl("main", vars);

    // Funcs isn't doing anything ...
    funcs = new DeclarationList(lineNumber(), charPosition());
    funcDecl(funcs);

    mainSeq = new StatementSequence(lineNumber(), charPosition());
    expect(Kind.OPEN_BRACE);
    // Now to compile statement sequence ...
    // For inner statSeqs, declare a local StatementSequence and add it to the main one
    statSeq("main", mainSeq);
    expect(Kind.CLOSE_BRACE);
    expect(Kind.PERIOD);
    instructions.add(DLX.assemble(DLX.RET, 0));

    FuncType mainType = new FuncType(new TypeList(), new VoidType());
    this.node =
      new Computation(
        mainToken.lineNumber(),
        mainToken.charPosition(),
        new Symbol(mainToken.lexeme(), mainType),
        vars,
        funcs,
        mainSeq
      );
  }

  public ast.AST genAST() {
    computation();
    if (hasError()) {
      return new ast.AST(null);
    }
    return new ast.AST(node);
  }
}
