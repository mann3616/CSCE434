package ast;

public class Assignment extends Node implements Statement{
    private AddressOf addressOf;
    private Expression right;
    protected Assignment(int lineNum, int charPos, AddressOf addressOf, Expression right) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.addressOf = addressOf;
        this.right = right;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }
    public AddressOf addressOf(){
        return this.addressOf;
    }
    public Expression right(){
        return this.right;
    }    
}
