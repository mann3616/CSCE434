package ast;

public class RepeatStatement extends Node implements Statement{
    private StatementSequence sequence;
    private Expression relation;
    public RepeatStatement(int lineNum, int charPos, StatementSequence sequence, Expression relation) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.sequence = sequence;
        this.relation = relation;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }
    public Expression relation(){
        return this.relation;
    }
    public StatementSequence sequence(){
        return this.sequence;
    }
}
