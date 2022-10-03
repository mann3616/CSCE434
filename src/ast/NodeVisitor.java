package ast;

public interface NodeVisitor {

    // literal
    public void visit (BoolLiteral node);
    public void visit (IntegerLiteral node);
    public void visit (FloatLiteral node);

    // designator
    public void visit (AddressOf node);
    public void visit (ArrayIndex node);
    public void visit (Dereference node);

    // groupExpr
    public void visit (LogicalNot node);
    // powExpr
    public void visit (Power node);
    // multExpr
    public void visit (Multiplication node);
    public void visit (Division node);
    public void visit (Modulo node);
    public void visit (LogicalAnd node);
    // addExpr
    public void visit (Addition node);
    public void visit (Subtraction node);
    public void visit (LogicalOr node);
    // relExpr
    public void visit (Relation node);

    // assign
    public void visit (Assignment node);
    // funcCall
    public void visit (ArgumentList node);
    public void visit (FunctionCall node);
    // ifStat
    public void visit (IfStatement node);
    // whileStat
    public void visit (WhileStatement node);
    // repeatStat
    public void visit (RepeatStatement node);
    // returnStat
    public void visit (ReturnStatement node);
    // statSeq
    public void visit (StatementSequence node);

    // varDecl
    public void visit (VariableDeclaration node);
    // funcBody
    public void visit (FunctionBody node);
    // funcDecl
    public void visit (FunctionDeclaration node);

    // computation
    public void visit (DeclarationList node);
    public void visit (Computation node);
}
