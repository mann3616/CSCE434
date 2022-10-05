package ast;

public class Subtraction extends Node implements Expression{

    private Expression left, right;
    protected Subtraction(int lineNum, int charPos, Expression left, Expression right) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
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
}
