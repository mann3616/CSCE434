package pl434;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import pl434.Token.Kind;

public class Compiler {
    class Result{
        int kind;
        int value;
        int address;
        int regno = -1;
        int cond, fixuplocation;
        boolean isFloat;

        final static int CONST = 1;
        final static int VAR = 2;
        final static int REG = 3;
        final static int CONDITION = 4;
    }
    class LRU{
        private int size;
        private HashMap<Integer, DoublyLinkedList.Node> hm = new HashMap<>();
        DoublyLinkedList list = new DoublyLinkedList();
        class DoublyLinkedList{
            class Node{
                int item;
                Node previous;
                Node next;
                public Node(int item){
                    this.item = item;
                }
            }
            int size = 0;
            Node head;
            Node tail;
            public DoublyLinkedList(){
                head = new Node(0);
                tail = new Node(0);
                head.next = tail;
                tail.previous = head;
            }
            public Node push(int new_data){
                Node new_node = new Node(new_data);
                head.next.previous = new_node;
                new_node.next = head.next;
                new_node.previous = head;
                head.next = new_node;
                size++;
                return new_node;
            }
            public int remove(){
                int res = tail.previous.item;
                tail.previous.previous.next = tail;
                tail.previous = tail.previous.previous;
                size--;
                return res;
            }
            public void getOp(Node p){
                p.previous.next = p.next;
                p.next.previous = p.previous;

                head.next.previous = p;
                p.next = head.next;
                p.previous = head;
                head.next = p;
            }
        }
        public LRU(int size){
            this.size = size;
            for(int i = 1;i<size-1;i++){
                put(i);
            }
        }
        public void put(int val){
                //if(val== 2){
                 //   System.out.println(val);
                //}
            if(hm.containsKey(val)){
                list.getOp(hm.get(val));
            }else{
                if(list.size == size){
                    list.remove();
                }
                hm.put(val, list.push(val));
            }
        }
        public void get(int val){
            list.getOp(hm.get(val));
        }
        public int last(){
            list.size--;
            size--;
            return list.remove();
        }
        public void print(){
            DoublyLinkedList.Node n = list.head;
            DoublyLinkedList.Node p = n;
            while(!p.next.equals(list.tail)){
                p = n;
                System.out.print(p.next.item+ " ");
                n = n.next;
            }
        }
    }
    // Error Reporting ============================================================
    private StringBuilder errorBuffer = new StringBuilder();

    private String reportSyntaxError (NonTerminal nt) {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected a token from " + nt.name() + " but got " + currentToken.kind + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }

    private String reportSyntaxError (Token.Kind kind) {
        String message = "SyntaxError(" + lineNumber() + "," + charPosition() + ")[Expected " + kind + " but got " + currentToken.kind + ".]";
        errorBuffer.append(message + "\n");
        return message;
    }

    public String errorReport () {
        return errorBuffer.toString();
    }

    public boolean hasError () {
        return errorBuffer.length() != 0;
    }

    private class QuitParseException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public QuitParseException (String errorMessage) {
            super(errorMessage);
        }
    }

    private int lineNumber () {
        return currentToken.lineNumber();
    }

    private int charPosition () {
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
    LRU lru;

    public Compiler (Scanner scanner, int numRegs) {
        this.scanner = scanner;
        currentToken = this.scanner.next();
        numDataRegisters = numRegs;
        lru = new LRU(this.numDataRegisters);
        instructions = new ArrayList<>();
    }

    public int[] compile () {
        try {
            computation();
            return instructions.stream().mapToInt(Integer::intValue).toArray();
        }
        catch (QuitParseException q) {
            errorBuffer.append("SyntaxError(" + lineNumber() + "," + charPosition() + ")");
            errorBuffer.append("[Could not complete parsing.]");
            return new ArrayList<Integer>().stream().mapToInt(Integer::intValue).toArray();
        }
    }

// Helper Methods =============================================================
    private boolean have (Token.Kind kind) {
        return currentToken.kind() == kind;
    }

    private boolean have (NonTerminal nt) {
        return nt.firstSet().contains(currentToken.kind);
    }

    private boolean accept (Token.Kind kind) {
        if (have(kind)) {
            try {
                currentToken = scanner.next();
            }
            catch (NoSuchElementException e) {
                if (!kind.equals(Token.Kind.EOF)) {
                    String errorMessage = reportSyntaxError(kind);
                    throw new QuitParseException(errorMessage);
                }
            }
            return true;
        }
        return false;
    }

    private boolean accept (NonTerminal nt) {
        if (have(nt)) {
            currentToken = scanner.next();
            return true;
        }
        return false;
    }

    private boolean expect (Token.Kind kind) {
        if (accept(kind)) {
            return true;
        }
        String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
    }

    private boolean expect (NonTerminal nt) {
        if (accept(nt)) {
            return true;
        }
        String errorMessage = reportSyntaxError(nt);
        throw new QuitParseException(errorMessage);
    }

    private Token expectRetrieve (Token.Kind kind) {
        Token tok = currentToken;
        if (accept(kind)) {
            return tok;
        }
        String errorMessage = reportSyntaxError(kind);
        throw new QuitParseException(errorMessage);
    }

    private Token expectRetrieve (NonTerminal nt) {
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
    private void readInt (int rDest) {
        // TODO: implement with DLX.RDI
        instructions.add(DLX.assemble(DLX.RDI, rDest));
    }

    private void readFloat (int rDest) {
        // TODO: implement with DLX.RDF
        instructions.add(DLX.assemble(DLX.RDF, rDest));
    }

    private void readBool (int rDest) {
        // TODO: implement with DLX.RDB
        instructions.add(DLX.assemble(DLX.RDB, rDest));
    }

    private void printInt (int rSrc) {
        // TODO: implement with DLX.WRI
        instructions.add(DLX.assemble(DLX.WRI, rSrc));
    }

    private void printFloat (int rSrc) {
        // TODO: implement with DLX.WRF
        instructions.add(DLX.assemble(DLX.WRF, rSrc));
    }

    private void printBool (int rSrc) {
        // TODO: implement with DLX.WRB
        instructions.add(DLX.assemble(DLX.WRB, rSrc));
    }

    private void println () {
        // TODO: implement with DLX.WRL
        instructions.add(DLX.assemble(DLX.WRL));
    }
// Grammar Rules ==============================================================

    // function for matching rule that only expects nonterminal's FIRST set
    private Token matchNonTerminal (NonTerminal nt) {
        return expectRetrieve(nt);
    }

    // TODO: copy operators and type grammar rules from Interpreter

    // literal = integerLit | floatLit
    private Token literal () {
        return matchNonTerminal(NonTerminal.LITERAL);
    }
    ArrayList<ArrayList<Integer>> loads = new ArrayList<>();
    private void deallocate(int reg){
        // if(reg == 6){
        //    System.out.print(reg+" ");
        // }
        if(IDENT_REG.containsKey(reg)){
            instructions.add(DLX.assemble(DLX.STW, reg, 30, 4*IDENT_REG.get(reg).address));
            for(String i: IDENT_MEM.keySet()){
                if(IDENT_MEM.get(i).regno == reg){
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
    private Result designator (String scope) {
        int lineNum = lineNumber();
        int charPos = charPosition();

        Token ident = expectRetrieve(Token.Kind.IDENT);

        String var = scope+":"+ident.lexeme();
        Result x = IDENT_MEM.get(var);
        if(x.regno != -1){
            //System.out.println("-1: "+var + " is " + x.regno);
            lru.get(x.regno);
            return x;
        }
        int reg = allocate().regno;
        //System.out.println(var + " is " + reg);
        instructions.add(DLX.assemble(DLX.LDW, reg, 30, 4*x.address));
        if(!loads.isEmpty()){
            loads.get(loads.size()-1).add(DLX.assemble(DLX.LDW, reg, 30, 4*x.address));
        }
        x.regno = reg;
        IDENT_REG.put(reg, x);
        IDENT_MEM.put(var, x);
        return x;
    }

    private void varDecl(String scope){
        while(have(NonTerminal.VAR_DECL)){
            Token type= expectRetrieve(NonTerminal.VAR_DECL);
            String id = expectRetrieve(Kind.IDENT).lexeme();
            Result x = new Result();
            x.kind = Result.VAR;
            x.address = ++maxed * -1;
            //System.out.println(id + " " + x.address*4);
            if(type.kind() == Kind.FLOAT){
                x.isFloat = true;
            }else {
                x.isFloat = false;
            }
            IDENT_MEM.put(scope+":"+id, x);
            while(accept(Kind.COMMA)){
                id = expectRetrieve(Kind.IDENT).lexeme();
                x = new Result();
                x.kind = Result.VAR;
                x.address = ++maxed * -1;
                //System.out.println(id + " " + x.address*4);
                if(type.kind() == Kind.FLOAT){
                    x.isFloat = true;
                }else {
                    x.isFloat = false;
                }
                IDENT_MEM.put(scope+":"+id, x);
            }
            expect(Kind.SEMICOLON);
        }
    }
    private Result statSeq(String scope){
        Result o = statement(scope);
        expect(Kind.SEMICOLON);
        while(have(NonTerminal.STATEMENT)){
            if(o ==null){
                o = statement(scope);
            }else{
                statement(scope);
            }
            expect(Kind.SEMICOLON);
        }
        if(goback!=0){
            instructions.add(goback, DLX.assemble(DLX.BSR, instructions.size()-goback+1));
            goback = 0;
        }
        return o;
    }
    int goback = 0;
    private Result statement(String scope){
        Token tok = expectRetrieve(NonTerminal.STATEMENT);
        switch(tok.kind()){
            case WHILE: whileStat(scope); break;
            case RETURN: Result o = returnStat(scope); 
            goback = instructions.size();
            return o; 
            case REPEAT: break;
            case IF: ifStat(scope); break;
            case LET: letStat(scope); break;
            case CALL: funcCall(scope); break;
        }
        return null;
    }
    private Result whileStat(String scope){
        expect(Kind.WHILE);
        int before = instructions.size();
        Result rel = relation(scope);
        expect(Kind.DO);
        statSeq(scope);
        instructions.add(DLX.assemble(DLX.BNE, rel.regno, before - instructions.size()));
        instructions.add(before, DLX.assemble(DLX.BEQ, rel.regno, instructions.size()-before+2));
        expect(Kind.OD);
        return null;
    }
    private void letStat(String scope){
        String id = currentToken.lexeme();
        Result obj = designator(scope);
        if(have(NonTerminal.UNARY_OP)){
            Token tok = expectRetrieve(NonTerminal.UNARY_OP);
            switch(tok.kind()){
                case UNI_DEC: 
                if(obj.isFloat){
                    instructions.add(DLX.assemble(DLX.fSUBI, obj.regno, obj.regno, (float)1.0));
                }else{
                    instructions.add(DLX.assemble(DLX.SUBI, obj.regno, obj.regno, 1));
                }
                break;
                case UNI_INC: 
                if(obj.isFloat){
                    instructions.add(DLX.assemble(DLX.fADDI, obj.regno, obj.regno, (float)1.0));
                }else{
                    instructions.add(DLX.assemble(DLX.ADDI, obj.regno, obj.regno, 1));
                }
                break;
            }
            deallocate(obj.regno);
            return;
        }
        Token tok = expectRetrieve(NonTerminal.ASSIGN_OP);
        if(tok.kind()== Kind.ASSIGN){
            IDENT_REG.remove(obj.regno);
            Result second = relExpr(scope);
            obj.regno = second.regno;
            IDENT_REG.put(obj.regno, obj);
            deallocate(obj.regno);
            return;
        }
        if(!obj.isFloat){
            Result second = relExpr(scope);
            switch(tok.kind()){
                case ADD_ASSIGN:
                instructions.add(DLX.assemble(DLX.ADD, obj.regno, obj.regno, second.regno));
                break;
                case MOD_ASSIGN:
                instructions.add(DLX.assemble(DLX.MOD, obj.regno, obj.regno, second.regno));
                break;
                case MUL_ASSIGN:
                instructions.add(DLX.assemble(DLX.MUL, obj.regno, obj.regno, second.regno));
                break;
                case SUB_ASSIGN:
                instructions.add(DLX.assemble(DLX.SUB, obj.regno, obj.regno, second.regno));
                break;
                case DIV_ASSIGN:
                instructions.add(DLX.assemble(DLX.DIV, obj.regno, obj.regno, second.regno));
                break;
                case POW_ASSIGN:
                instructions.add(DLX.assemble(DLX.POW, obj.regno, obj.regno, second.regno));
                break;
            }
        }else{
            Result second = relExpr(scope);
            switch(tok.kind()){
                case ADD_ASSIGN:
                instructions.add(DLX.assemble(DLX.fADD, obj.regno, obj.regno, second.regno));
                break;
                case MOD_ASSIGN:
                instructions.add(DLX.assemble(DLX.fMOD, obj.regno, obj.regno, second.regno));
                break;
                case MUL_ASSIGN:
                instructions.add(DLX.assemble(DLX.fMUL, obj.regno, obj.regno, second.regno));
                break;
                case SUB_ASSIGN:
                instructions.add(DLX.assemble(DLX.fSUB, obj.regno, obj.regno, second.regno));
                break;
                case DIV_ASSIGN:
                instructions.add(DLX.assemble(DLX.fDIV, obj.regno, obj.regno, second.regno));
                break;
            }
        }
        deallocate(obj.regno);
    }
    private Result returnStat(String scope){
        if(!have(Kind.SEMICOLON)){
            return relExpr(scope);
        }
        return null;
    }
    private void ifStat(String scope){
        loads.add(new ArrayList<Integer>());
        Result b = relation(scope);
        expect(Kind.THEN);
        int getem = instructions.size();
        lru.get(b.regno);
        statSeq(scope);
        instructions.add(getem, DLX.assemble(DLX.BEQ, b.regno, instructions.size()-getem+2));
        getem = instructions.size();
        instructions.addAll(loads.remove(loads.size()-1));
        lru.get(b.regno);
        if(accept(Kind.ELSE)){
            loads.add(new ArrayList<Integer>());
            statSeq(scope);
            instructions.add(getem, DLX.assemble(DLX.BNE, b.regno, instructions.size()-getem+1));
            instructions.addAll(loads.remove(loads.size()-1));
        }
        expect(Kind.FI);
    }
    private Result groupExpr(String scope){
        Result x = new Result();
        x.kind = Result.CONST;
        if(accept(Kind.NOT)){
            Result xx = relExpr(scope);
            x.regno = xx.regno;
            instructions.add(DLX.assemble(DLX.XORI, x.regno, xx.regno, 1));
            x.isFloat = false;
            return x;
        }
        if(have(NonTerminal.LITERAL)){
            Token tok = expectRetrieve(NonTerminal.LITERAL);
            x = allocate();
            x.kind = Result.CONST;
            switch(tok.kind()){
                case INT_VAL:
                int val = Integer.parseInt(tok.lexeme());
                instructions.add(DLX.assemble(DLX.ADDI, x.regno, 0, val));
                x.isFloat = false;
                return x;
                case FLOAT_VAL:
                float fval = Float.parseFloat(tok.lexeme());
                instructions.add(DLX.assemble(DLX.fADDI, x.regno, 0, fval));
                x.isFloat = true;
                return x;
                case TRUE:
                instructions.add(DLX.assemble(DLX.ADDI, x.regno, 0, 1));
                x.isFloat = false;
                return x;
                case FALSE:
                instructions.add(DLX.assemble(DLX.ADDI, x.regno, 0, 0));
                x.isFloat = false;
                return x;
            }
        }
        if(have(NonTerminal.DESIGNATOR)){
            Result xx = designator(scope);
            x.regno = allocate().regno;
            instructions.add(DLX.assemble(DLX.ADD, x.regno, xx.regno, 0));
            x.isFloat = xx.isFloat;
            return x;
        }
        if(have(NonTerminal.RELATION)){
            Result xx = relation(scope);
            x.regno = xx.regno;
            lru.get(x.regno);
            x.isFloat = xx.isFloat;
            return x;
        }
        if(accept(NonTerminal.FUNC_CALL)){
            Result xx = funcCall(scope);
            x.regno = xx.regno;
            lru.get(x.regno);
            x.isFloat = xx.isFloat;
            return x;
        }
        String errorMessage = reportSyntaxError(NonTerminal.GROUP_EXPRESSION);
        throw new QuitParseException(errorMessage);
    }
    private Result powExpr(String scope){
        Result obj = groupExpr(scope);
        if(accept(NonTerminal.POW_OP)){
            Result obj2 = powExpr(scope);
            instructions.add(DLX.assemble(DLX.POW, obj.regno, obj.regno, obj2.regno));
            lru.get(obj.regno);
        }
        return obj;
    }
    private Result multExpr(String scope){
        Result obj = powExpr(scope);
        if(have(NonTerminal.MULT_OP)){
            Token tok = expectRetrieve(NonTerminal.MULT_OP);
            Result obj2 = multExpr(scope);
            obj.isFloat = obj.isFloat || obj2.isFloat;
            if(obj.isFloat){
                switch(tok.kind()){
                    case MUL: 
                    instructions.add(DLX.assemble(DLX.fMUL, obj.regno, obj.regno, obj2.regno));
                    break;
                    case DIV:
                    instructions.add(DLX.assemble(DLX.fDIV, obj.regno, obj.regno, obj2.regno));
                    break;
                    case MOD: 
                    instructions.add(DLX.assemble(DLX.fMOD, obj.regno, obj.regno, obj2.regno));
                    break;
                    case AND: 
                    instructions.add(DLX.assemble(DLX.AND, obj.regno, obj.regno, obj2.regno));
                    break;
                }
            }else {
                switch(tok.kind()){
                    case MUL: 
                    instructions.add(DLX.assemble(DLX.MUL, obj.regno, obj.regno, obj2.regno));
                    break;
                    case DIV:
                    instructions.add(DLX.assemble(DLX.DIV, obj.regno, obj.regno, obj2.regno));
                    break;
                    case MOD: 
                    instructions.add(DLX.assemble(DLX.MOD, obj.regno, obj.regno, obj2.regno));
                    break;
                    case AND: 
                    instructions.add(DLX.assemble(DLX.AND, obj.regno, obj.regno, obj2.regno));
                    break;
                }
            }
            lru.get(obj.regno);
        }
        return obj;
    }
    private Result addExpr(String scope){
        Result obj = multExpr(scope);
        if(have(NonTerminal.ADD_OP)){
            Token tok = expectRetrieve(NonTerminal.ADD_OP);
            Result obj2 = addExpr(scope);
            obj.isFloat = obj.isFloat || obj2.isFloat;
            if(obj.isFloat){
                switch(tok.kind()){
                    case ADD: 
                    instructions.add(DLX.assemble(DLX.fADD, obj.regno, obj.regno, obj2.regno));
                    break;
                    case SUB:
                    instructions.add(DLX.assemble(DLX.fSUB, obj.regno, obj.regno, obj2.regno));
                    break;
                    case OR: 
                    instructions.add(DLX.assemble(DLX.OR, obj.regno, obj.regno, obj2.regno));
                    break;
                }
            }else {
                switch(tok.kind()){
                    case ADD: 
                    //System.out.println("Is this illegal addExpr");
                    instructions.add(DLX.assemble(DLX.ADD, obj.regno, obj.regno, obj2.regno));
                    break;
                    case SUB:
                    instructions.add(DLX.assemble(DLX.SUB, obj.regno, obj.regno, obj2.regno));
                    break;
                    case OR: 
                    instructions.add(DLX.assemble(DLX.OR, obj.regno, obj.regno, obj2.regno));
                    break;
                }
            }
            lru.get(obj.regno);
        }
        return obj;
    }
    private Result relExpr(String scope){
        Result obj = addExpr(scope);
        if(have(NonTerminal.REL_OP)){
            Token tok = expectRetrieve(NonTerminal.REL_OP);
            Result obj2 = relExpr(scope);
            instructions.add(DLX.assemble(DLX.SUB, obj.regno, obj.regno, obj2.regno));
            switch(tok.kind()){
                case EQUAL_TO:
                instructions.add(DLX.assemble(DLX.BEQ, obj.regno, 3));
                break;
                case NOT_EQUAL:
                instructions.add(DLX.assemble(DLX.BNE, obj.regno, 3));
                break;
                case LESS_EQUAL:
                instructions.add(DLX.assemble(DLX.BLE, obj.regno, 3));
                break;
                case LESS_THAN:
                instructions.add(DLX.assemble(DLX.BLT, obj.regno, 3));
                break;
                case GREATER_EQUAL:
                instructions.add(DLX.assemble(DLX.BGE, obj.regno, 3));
                break;
                case GREATER_THAN:
                instructions.add(DLX.assemble(DLX.BGT, obj.regno, 3));
                break;
            }
            instructions.add(DLX.assemble(DLX.ADDI, obj.regno, 0, 0));
            instructions.add(DLX.assemble(DLX.BSR, 2));
            instructions.add(DLX.assemble(DLX.ADDI, obj.regno, 0, 1));
            lru.get(obj.regno);
        }
        return obj;
    }
    private Result relation(String scope){
        expect(Kind.OPEN_PAREN);
        Result o = relExpr(scope);
        expect(Kind.CLOSE_PAREN);
        return o;
    }
    private Result allocate(){
        int reg = lru.last();
        lru.put(reg);
        deallocate(reg);
        Result res = new Result();
        res.regno = reg;
        res.kind = Result.CONST;
        return res;
    }
    //Function
    private Result funcCall(String scope){
        String n = expectRetrieve(Kind.IDENT).lexeme();
        expect(Kind.OPEN_PAREN);
        Result a = new Result();
        a.kind = Result.CONST;
        switch (n){
            case "printInt":
            a.regno = relExpr(scope).regno;
            a.isFloat = false;
            printInt(a.regno);
            break;
            case "printFloat":
            a.regno = relExpr(scope).regno;
            a.isFloat = true;
            printFloat(a.regno);
            break;
            case "printBool":
            a.regno = relExpr(scope).regno;
            a.isFloat = false;
            printBool(a.regno);
            break;
            case "readInt":
            a.regno = allocate().regno;
            readInt(a.regno);
            a.isFloat = false;
            break;
            case "readFloat":
            a.regno = allocate().regno;
            readFloat(a.regno);
            a.isFloat = true;
            break;
            case "readBool":
            a.regno = allocate().regno;
            readBool(a.regno);
            a.isFloat = false;
            break;
            case "println":
            println();
            break;
            default:
            if(have(Kind.IDENT)){
                ArrayList<Result> vars = new ArrayList<>();
                Result var = relExpr(scope);
                while(!have(Kind.CLOSE_PAREN)){
                    expect(Kind.COMMA);
                    vars.add(var);
                    var = relExpr(scope);
                }
                vars.add(var);
                setFuncVar(vars, n);
                instructions.add(DLX.assemble(DLX.JSR, IDENT_FUNC.get(n).jumper*4));
                if(IDENT_FUNC.get(n).result != null){
                    lru.get(a.regno);
                    instructions.add(DLX.assemble(DLX.LDW, a.regno, 30, IDENT_FUNC.get(n).result.address*-4));
                }
            }
        }
        expect(Kind.CLOSE_PAREN);
        return a;
    }
    private void setFuncVar(ArrayList<Result> res, String scope){
        int i = 0;
        for(String s : IDENT_FUNC.get(scope).names){
            //Later change this so that constants get saved 
            IDENT_MEM.get(scope+":"+s).regno = res.get(i).regno;
            IDENT_REG.put(res.get(i).regno, IDENT_MEM.get(scope + ":"+s));
            deallocate(IDENT_MEM.get(scope+":"+s).regno);
            i++;
        }
    }
    private void funcDecl(){
        while(accept(NonTerminal.FUNC_DECL)){
            Token id = expectRetrieve(Kind.IDENT);
            IDENT_FUNC.put(id.lexeme(), new func(id.lexeme()));
            formalParam(id.lexeme());
            expect(Kind.COLON);
            Token tok = expectRetrieve(NonTerminal.TYPE_DECL);
            IDENT_FUNC.get(id.lexeme()).type = tok;
            funcBody(id.lexeme());
        }
    }
    private void formalParam(String scope){
        expect(NonTerminal.FORMAL_PARAM);
        if(have(NonTerminal.PARAM_DECL)){
            paramDecl(scope);
            while(accept(Kind.COMMA)){
                paramDecl(scope);
            }
        }
        expect(Kind.CLOSE_PAREN);
    }
    private void paramDecl(String scope){
        Token type = expectRetrieve(NonTerminal.PARAM_DECL);
        String id = expectRetrieve(Kind.IDENT).lexeme();
        Result x = new Result();
        x.kind = Result.VAR;
        x.address = ++maxed * -1;
        if(type.kind() == Kind.FLOAT){
            x.isFloat = true;
        }else {
            x.isFloat = false;
        }
        IDENT_FUNC.get(scope).names.add(id);
        IDENT_MEM.put(scope+":"+id, x);
    }
    private void funcBody(String scope){
        expect(Kind.OPEN_BRACE);
        varDecl(scope);
        int prior = instructions.size();
        instructions.add(DLX.assemble(DLX.STW, 31, 30, IDENT_MEM.get(scope).address*4*-1));
        deallocate(IDENT_MEM.get(scope).regno);
        Result xx = statSeq(scope);
        IDENT_FUNC.get(scope).jumper = prior+1;
        instructions.add(DLX.assemble(DLX.STW, xx.regno, 30, IDENT_FUNC.get(scope).result.address*4*-1));
        instructions.add(DLX.assemble(DLX.LDW, 31, 30, IDENT_MEM.get(scope).address*4*-1));
        instructions.add(DLX.assemble(DLX.RET, 31));
        int after = instructions.size();
        instructions.add(prior, DLX.assemble(DLX.BSR, after - prior + 1));
        expect(Kind.CLOSE_BRACE);
        expect(Kind.SEMICOLON);
    }
    private paramType saveVar(String scope, Token type, int dim, int[] size){
        Token id = expectRetrieve(Kind.IDENT);
        IDENT_TYPE.put(scope+":"+id.lexeme(), new paramType(type, dim));
        dMap.put(scope+":"+id.lexeme(), new MultiArray(type, dim));
        dMap.get(scope+":"+id.lexeme()).fillArray(size);
        return IDENT_TYPE.get(scope+":"+id.lexeme());
    }
    private paramType saveVar(String scope, Token type, int dim){
        Token id = expectRetrieve(Kind.IDENT);
        IDENT_TYPE.put(scope+":"+id.lexeme(), new paramType(type, dim));
        dMap.put(scope+":"+id.lexeme(), new MultiArray(type, dim));
        return IDENT_TYPE.get(scope+":"+id.lexeme());
    }
    private class paramType{
        public Token _type;
        public int dim;
        public paramType(Token _type, int dim){
            this._type = _type;
            this.dim = dim;
        }
    }
    private class func{
        Token type;
        String name;
        int jumper;
        Result savior;
        Result result;
        public ArrayList<paramType> formalParam = new ArrayList<>();
        public ArrayList<String> names = new ArrayList<>();
        public func(String name){
            this.name = name;
            savior = new Result();
            savior.address = ++maxed;
            savior.kind = Result.VAR;
            IDENT_MEM.put(name, savior);

            result = new Result();
            result.address = ++maxed;
            result.kind = Result.VAR;
            IDENT_MEM.put(name, savior);
        }
    }
    private class MultiArray{
        public ArrayList<Object> data;
        public Object _data;
        public Token type;
        public int dim;
        public MultiArray(Token type, int dim){
            this.dim = dim;
            this.type = type;
        }
        public void fillArray(int[] sizes){
            ArrayList<Object> current = null;
            ArrayList<Object> previous = null;
            for(int i = dim-1; i>=0; i--){
                current = new ArrayList<Object>(sizes[i]);
                if(previous!=null){
                    for(int j = 0;j<current.size();j++){
                        current.add(previous.clone());
                    }
                }
                previous = current;
            }
            data = current;
        }
        public Object getObj(int[] vals){
            ArrayList<Object> curr = data;
            for(int i : vals){
                if (curr.get(i) instanceof ArrayList<?>){
                    curr = (ArrayList<Object>) curr.get(i);
                }else{
                    return curr.get(i);
                }
            }
            return null;
        }
    }
    private void computation () {
        expect(Kind.MAIN);
        IDENT_FUNC.put("printInt", new func(""));
        IDENT_FUNC.put("printFloat", new func(""));
        IDENT_FUNC.put("printBool", new func(""));
        IDENT_FUNC.put("readInt", new func(""));
        IDENT_FUNC.put("readFloat", new func(""));
        IDENT_FUNC.put("readBool", new func(""));
        IDENT_FUNC.put("println", new func(""));
        varDecl("main");
        funcDecl();
        expect(Kind.OPEN_BRACE);
        statSeq("main");
        expect(Kind.CLOSE_BRACE);
        expect(Kind.PERIOD);
        instructions.add(DLX.assemble(DLX.RET, 0));

    }
}
