package ast;

import pl434.Token;

public class Relation extends Node implements Expression{
    private Token rel;
    private Expression left;
    private Expression right;
    public Relation(int lineNum, int charPos, Token rel, Expression left, Expression right) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.rel = rel;
        this.left = left;
        this.right = right;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }
    public Expression left(){
        return this.left;
    }
    public Expression right(){
        return this.right;
    }
    public String rel(){
        return rel.lexeme();
    }
}
