package ast;

import pl434.Token;

public abstract class Node implements Visitable {

    private int lineNum;
    private int charPos;

    protected Node (int lineNum, int charPos) {
        this.lineNum = lineNum;
        this.charPos = charPos;
    }

    public int lineNumber () {
        return lineNum;
    }

    public int charPosition () {
        return charPos;
    }

    public String getClassInfo () {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString () {
        return this.getClass().getSimpleName();
    }
    // Some factory method
    public static Statement newAssignment (int lineNum, int charPos, Expression dest, Token assignOp, Expression src) {
        switch(assignOp.kind()){
            case ASSIGN: return new Assignment(lineNum, charPos, dest, src);
            case UNI_INC: return new Assignment(lineNum, charPos, dest, new Addition(lineNum, charPos, dest, src));
            case UNI_DEC: return new Assignment(lineNum, charPos, dest, new Subtraction(lineNum, charPos, dest, src));
            case ADD_ASSIGN: return new Assignment(lineNum, charPos, dest, new Addition(lineNum, charPos, dest, src));
            case SUB_ASSIGN: return new Assignment(lineNum, charPos, dest, new Subtraction(lineNum, charPos, dest, src));
            case MUL_ASSIGN: return new Assignment(lineNum, charPos, dest, new Multiplication(lineNum, charPos, dest, src));
            case MOD_ASSIGN: return new Assignment(lineNum, charPos, dest, new Modulo(lineNum, charPos, dest, src));
            case DIV_ASSIGN: return new Assignment(lineNum, charPos, dest, new Division(lineNum, charPos, dest, src));
            case POW_ASSIGN: return new Assignment(lineNum, charPos, dest, new Power(lineNum, charPos, dest, src));
            default: throw new RuntimeException("Unable to make Statement with AssignOperation: " + assignOp.lexeme());
        }
    }
    public static Expression newExpression (Expression leftSide, Token op, Expression rightSide) {
        switch(op.kind()){
            case NOT: return new LogicalNot(0, 0, rightSide);
            case ADD: return new Addition(0, 0, leftSide, rightSide);
            case SUB: return new Subtraction(0, 0, leftSide, rightSide);
            case DIV: return new Division(0, 0, leftSide, rightSide);
            case MUL: return new Multiplication(0, 0, leftSide, rightSide);
            case MOD: return new Modulo(0, 0, leftSide, rightSide);
            case OR: return new LogicalOr(0, 0, leftSide, rightSide);
            case AND: return new LogicalAnd(0, 0, leftSide, rightSide);
            case POW: return new Power(0, 0, leftSide, rightSide);
            case LESS_EQUAL: return new Relation(0, 0, op, leftSide, rightSide);
            case EQUAL_TO: return new Relation(0, 0, op, leftSide, rightSide);
            case NOT_EQUAL: return new Relation(0, 0, op, leftSide, rightSide);
            case LESS_THAN: return new Relation(0, 0, op, leftSide, rightSide);
            case GREATER_EQUAL: return new Relation(0, 0, op, leftSide, rightSide);
            case GREATER_THAN: return new Relation(0, 0, op, leftSide, rightSide);
            default: throw new RuntimeException("Unable to make Expression with Operation: " + op.lexeme());
        }
    }
    public static Expression newLiteral (Token tok) {
        switch(tok.kind()){
            case INT_VAL: return new IntegerLiteral(0, 0, tok.lexeme());
            case FLOAT_VAL: return new IntegerLiteral(0, 0, tok.lexeme());
            case TRUE: return new IntegerLiteral(0, 0, "true");
            case FALSE: return new IntegerLiteral(0, 0, "false");
            default: throw new RuntimeException("Unable to make Expression with Literal Type: " + tok.kind().toString() + " w/ value: " + tok.lexeme());
        }
    }
}
