package ast;

import pl434.*;
public class FunctionDeclaration extends Node implements Declaration{
    private Symbol function;
    private FunctionBody body;
    protected FunctionDeclaration(int lineNum, int charPos, Symbol function, FunctionBody body) {
        //TODO Auto-generated constructor stub
        super(lineNum, charPos);
        this.body = body;
        this.function = function;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }
    public String function(){
        return this.function.functionString();
    }
    public FunctionBody body(){
        return this.body;
    }
}
