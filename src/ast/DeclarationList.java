package ast;

import java.util.ArrayList;
import java.util.Iterator;

public class DeclarationList extends Node implements Iterable<Declaration> {

  public ArrayList<Declaration> list;

  public DeclarationList(int lineNum, int charPos) {
    super(lineNum, charPos);
    list = new ArrayList<>();
    //TODO Auto-generated constructor stub
  }

  @Override
  public void accept(NodeVisitor visitor) {
    // TODO Auto-generated method stub
    visitor.visit(this);
  }

  @Override
  public Iterator<Declaration> iterator() {
    // TODO Auto-generated method stub
    return list.iterator();
  }

  public boolean empty() {
    return list.isEmpty();
  }

  public void add(Declaration decl) {
    list.add(decl);
  }
}
