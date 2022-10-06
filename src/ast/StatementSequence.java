package ast;

import java.util.ArrayList;
import java.util.Iterator;

public class StatementSequence extends Node implements Iterable<Statement>{
    public ArrayList<Statement> statements;
    public StatementSequence(int lineNum, int charPos) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.statements = new ArrayList<Statement>();
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }

    @Override
    public Iterator<Statement> iterator() {
        // TODO Auto-generated method stub
        return statements.iterator();
    }

    public void add(Statement statement){
        statements.add(statement);
    }
}
