package ast;

public class PrettyPrinter implements NodeVisitor {

    private int depth = 0;
    private StringBuilder sb = new StringBuilder();
    private String indent(){
        String indent = "";
        for(int i = 0;i<depth;i++)
            indent+= " ";
        return indent;
    }
    private void println (Node n, String message) {
        sb.append(indent() + n.getClassInfo() + message + "\n");
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
    public void visit (FunctionBody node){
        println(node, "");
        depth++;
        node.declaration().accept(this);
        node.sequence().accept(this);
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
    @Override
    public void visit(RepeatStatement node){
        println(node, "");
        depth++;
        node.sequence().accept(this);
        node.relation().accept(this);
        depth--;
    }
    @Override
    public void visit(WhileStatement node){
        println(node, "");
        depth++;
        node.relation().accept(this);
        node.sequence().accept(this);
        depth--;
    }
    @Override
    public void visit(ReturnStatement node){
        println(node, "");
        depth++;
        node.relation().accept(this);
        depth--;
    }
    @Override
    public void visit(IfStatement node){
        println(node, "");
        depth++;
        node.relation().accept(this);
        node.ifSequence().accept(this);
        node.elseSequence().accept(this);
        depth--;
    }
    @Override
    public void visit(Assignment node){
        println(node, "");
        depth++;
        node.addressOf().accept(this);
        depth--;
    }
    @Override
    public void visit(AddressOf node){
        sb.append(indent() + node.symbol().name() + ":" + node.symbol().getTypeAsString());
        sb.append("\n");
    }
    @Override
    public void visit(Dereference node){
        sb.append(indent() + node.symbol().name() + ":" + node.symbol().getTypeAsString());
        sb.append("\n");
    }
    @Override
    public void visit(BoolLiteral node) {
        // TODO Auto-generated method stub
        println(node, "[" + node.literal() + "]");
    }
    @Override
    public void visit(IntegerLiteral node) {
        // TODO Auto-generated method stub
        println(node, "[" + node.literal() + "]");
    }
    @Override
    public void visit(FloatLiteral node) {
        // TODO Auto-generated method stub
        println(node, "[" + node.literal() + "]");
    }
    // @Override
    // public void visit(ArrayIndex node) {
    //     // TODO Auto-generated method stub
        
    // }
    @Override
    public void visit(LogicalNot node) {
        // TODO Auto-generated method stub
        println(node, "");
        depth++;
        node.right().accept(this);
        depth--;
        
    }
    @Override
    public void visit(Power node) {
        // TODO Auto-generated method stub
        println(node, "");
        depth++;
        node.left().accept(this);
        node.right().accept(this);
        depth--;

    }
    @Override
    public void visit(Multiplication node) {
        // TODO Auto-generated method stub
        println(node, "");
        depth++;
        node.left().accept(this);
        node.right().accept(this);
        depth--;

    }
    @Override
    public void visit(Division node) {
        // TODO Auto-generated method stub
        println(node, "");
        depth++;
        node.left().accept(this);
        node.right().accept(this);
        depth--;
        
    }
    @Override
    public void visit(Modulo node) {
        // TODO Auto-generated method stub
        println(node, "");
        depth++;
        node.left().accept(this);
        node.right().accept(this);
        depth--;
        
    }
    @Override
    public void visit(LogicalAnd node) {
        // TODO Auto-generated method stub
        println(node, "");
        depth++;
        node.left().accept(this);
        node.right().accept(this);
        depth--;
        
    }
    @Override
    public void visit(Addition node) {
        // TODO Auto-generated method stub
        println(node, "");
        depth++;
        node.left().accept(this);
        node.right().accept(this);
        depth--;
        
    }
    @Override
    public void visit(Subtraction node) {
        // TODO Auto-generated method stub
        println(node, "");
        depth++;
        node.left().accept(this);
        node.right().accept(this);
        depth--;
        
    }
    @Override
    public void visit(LogicalOr node) {
        // TODO Auto-generated method stub
        println(node, "");
        depth++;
        node.left().accept(this);
        node.right().accept(this);
        depth--;
        
    }
    @Override
    public void visit(Relation node) {
        println(node, "[" + node.rel() + "]");
        depth++;
        node.left().accept(this);
        node.right().accept(this);
        depth--;
    }
    @Override
    public void visit(ArgumentList node) {
        // TODO Auto-generated method stub
        println(node, "");
        depth++;
        for(Expression expr : node){
            expr.accept(this);
        }
        depth--;
    }
    @Override
    public void visit(FunctionCall node) {
        // TODO Auto-generated method stub
        println(node, "[" + node.function() + "]");
        depth++;
        node.list().accept(this);
        depth--;
    }
}