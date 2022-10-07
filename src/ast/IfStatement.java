package ast;

public class IfStatement extends Node implements Statement{
    private Expression relation;
    private StatementSequence ifSequence, elseSequence;
    public IfStatement(int lineNum, int charPos, Expression relation, StatementSequence ifSequence, StatementSequence elseSequence) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.relation = relation;
        this.ifSequence = ifSequence;
        this.elseSequence = elseSequence;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }
    public Expression relation(){
        return this.relation;
    }
    public StatementSequence ifSequence(){
        return this.ifSequence;
    }
    public StatementSequence elseSequence(){
        return this.elseSequence;
    }
}
