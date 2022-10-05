package ast;

import java.util.Iterator;
import java.util.List;

public class ArgumentList extends Node implements Iterable<Expression>{
    private List<Expression> list;
    protected ArgumentList(int lineNum, int charPos, List<Expression> list) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.list = list;
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

}
