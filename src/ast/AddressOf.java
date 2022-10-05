package ast;
import pl434.*;
public class AddressOf extends Node {
    private Symbol symbol;
    protected AddressOf(int lineNum, int charPos, Symbol symbol) {
        super(lineNum, charPos);
        //TODO Auto-generated constructor stub
        this.symbol = symbol;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        // TODO Auto-generated method stub
        visitor.visit(this);
    }
    public Symbol symbol(){
        return this.symbol;
    }
}
