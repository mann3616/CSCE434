package ast;

public class FloatLiteral extends Node implements Expression{
    private String literal;
    public FloatLiteral(int lineNum, int charPos, String literal) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.literal = literal;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }
    public String literal(){
        return this.literal;
    }
}
