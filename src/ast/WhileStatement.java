package ast;

public class WhileStatement extends Node implements Statement{

    protected WhileStatement(int lineNum, int charPos) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }
}
