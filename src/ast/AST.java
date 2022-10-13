package ast;

import pl434.SymbolTable;

public class AST {

  private PrettyPrinter printer;
  // TODO: Create AST structure of your choice
  private Computation node;
  public SymbolTable table;

  public AST(Computation node, SymbolTable table) {
    this.node = node;
    this.printer = new PrettyPrinter();
    this.table = table;
  }

  public String printPreOrder() {
    // TODO: Return the pre order traversal of AST. Use "\n" as separator.
    // Use the enum ASTNonTerminal provided for naming convention.
    if (node == null) return "";
    printer.visit(node);
    //return "";
    return printer.toString();
  }

  public Computation getNode() {
    return this.node;
  }
}
