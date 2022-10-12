package ast;

public class AST {

  private PrettyPrinter printer;
  // TODO: Create AST structure of your choice
  private Computation node;

  public AST(Computation node) {
    this.node = node;
    this.printer = new PrettyPrinter();
  }

  public String printPreOrder() {
    // TODO: Return the pre order traversal of AST. Use "\n" as separator.
    // Use the enum ASTNonTerminal provided for naming convention.
    if (node == null) return "";
    printer.visit(node);
    //return "";
    return printer.toString();
  }
}
