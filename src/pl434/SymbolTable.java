package pl434;

import java.util.HashMap;
import java.util.Stack;

public class SymbolTable {

    // TODO: Create Symbol Table structure
    Stack<HashMap<String, Symbol>> stack = new Stack<>();
    int addy;
    public SymbolTable () {
        addy = 0;
        throw new RuntimeException("Create Symbol Table and initialize predefined functions");
    }

    // lookup name in SymbolTable
    public Symbol lookup (String name) throws SymbolNotFoundError {
        if(stack.peek().containsKey(name)){
            return stack.peek().get(name);
        }
        throw new SymbolNotFoundError(name);
    }

    // insert name in SymbolTable
    public Symbol insert (String name) throws RedeclarationError {
        if(stack.peek().containsKey(name)){
            throw new RedeclarationError(name);
        }
        stack.peek().put(name, new Symbol(name, -4 * ++addy));
        return stack.peek().get(name);
    }
    public void addScope(){
        stack.push(new HashMap<>());
    }
    public void popScope(){
        stack.pop();
    }

}

class SymbolNotFoundError extends Error {

    private static final long serialVersionUID = 1L;
    private final String name;

    public SymbolNotFoundError (String name) {
        super("Symbol " + name + " not found.");
        this.name = name;
    }

    public String name () {
        return name;
    }
}

class RedeclarationError extends Error {

    private static final long serialVersionUID = 1L;
    private final String name;

    public RedeclarationError (String name) {
        super("Symbol " + name + " being redeclared.");
        this.name = name;
    }

    public String name () {
        return name;
    }
}
