package ast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArgumentList extends Node implements Iterable<Expression>{
    private List<Expression> list;
    public ArgumentList(int lineNum, int charPos) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.list = new ArrayList<Expression>();
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }

    @Override
    public Iterator<Expression> iterator() {
        // TODO Auto-generated method stub
        return list.iterator();
    }

    public void append(Expression expression){
        list.add(expression);
    }

}
