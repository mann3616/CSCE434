package ast;

public class PrettyPrinter implements NodeVisitor {

    private int depth = 0;
    private StringBuilder sb = new StringBuilder();

    private void println (Node n, String message) {
        String indent = "";
        for (int i = 0; i < depth; i++) {
            indent += "  ";
        }
        sb.append(indent + n.getClassInfo() + message + "\n");
    }

    @Override
    public String toString () {
        return sb.toString();
    }

    @Override
    public void visit (StatementSequence node) {
        println(node, "");
        depth++;
        for (Statement s : node) {
            s.accept(this);
        }
        depth--;
    }

    @Override
    public void visit (VariableDeclaration node) {
        println(node, "[" + node.symbol() + "]");
    }

    @Override
    public void visit (FunctionDeclaration node) {
        println(node, "[" + node.function() + "]");
        depth++;
        node.body().accept(this);
        depth--;
    }

    @Override
    public void visit (DeclarationList node) {
        if (node.empty()) return;
        println(node, "");
        depth++;
        for (Declaration d : node) {
            d.accept(this);
        }
        depth--;
    }

    @Override
    public void visit (Computation node) {
        println(node, "[" + node.main() + "]");
        depth++;
        node.variables().accept(this);
        node.functions().accept(this);
        node.mainStatementSequence().accept(this);
        depth--;
    }
}