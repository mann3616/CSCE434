package ast;

public class FunctionBody extends Node{
    private DeclarationList list;
    private StatementSequence seq;
    
    public FunctionBody(int lineNum, int charPos, DeclarationList list, StatementSequence seq) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.list = list;
        this.seq = seq;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }
    public DeclarationList declaration(){
        return this.list;
    }
    public StatementSequence sequence(){
        return this.seq;
    }
}
