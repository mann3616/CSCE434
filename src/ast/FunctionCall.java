package ast;

import pl434.Symbol;

public class FunctionCall extends Node implements Expression, Statement{
    private Symbol function;
    private ArgumentList list;
    protected FunctionCall(int lineNum, int charPos, Symbol function, ArgumentList list) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.function= function;
        this.list = list;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }
    public String function(){
        return this.function.functionString();
    }
    public ArgumentList list(){
        return this.list;
    }

}
