package pl434;


public class SymbolTable {

    // TODO: Create Symbol Table structure

    public SymbolTable () {
        throw new RuntimeException("Create Symbol Table and initialize predefined functions");
    }

    // lookup name in SymbolTable
    public Symbol lookup (String name) throws SymbolNotFoundError {
        throw new RuntimeException("implement lookup variable");
    }

    // insert name in SymbolTable
    public Symbol insert (String name) throws RedeclarationError {
        throw new RuntimeException("implement insert variable");
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
