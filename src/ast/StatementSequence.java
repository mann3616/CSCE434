package ast;

import java.util.ArrayList;
import java.util.Iterator;

public class StatementSequence extends Node implements Iterable<Statement>{
    ArrayList<Statement> statement;
    protected StatementSequence(int lineNum, int charPos, ArrayList<Statement> statement) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.statement = statement;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }

    @Override
    public Iterator<Statement> iterator() {
        // TODO Auto-generated method stub
        return statement.iterator();
    }


}
