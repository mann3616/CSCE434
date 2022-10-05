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
        return this.getClass().getName() + "(" + lineNumber() + "," + charPosition() + ")";
    }

    @Override
    public String toString () {
        return this.getClass().getName() + "(" + lineNumber() + "," + charPosition() + ")";
    }
    // Some factory method
    public static Statement newAssignment (int lineNum, int charPos, Expression dest, Token assignOp, Expression src) {

    }
    public static Expression newExpression (Expression leftSide, Token op, Expression rightSide) {

    }
    public static Expression newLiteral (Token tok) {
    }
}
