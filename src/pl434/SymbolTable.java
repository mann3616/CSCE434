package pl434;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import types.*;

public class SymbolTable {

  // TODO: Create Symbol Table structure
  Stack<HashMap<String, Symbol>> stack = new Stack<>();
  HashMap<String, ArrayList<Symbol>> st = new HashMap<>();

  public SymbolTable() {
    //throw new RuntimeException("Create Symbol Table and initialize predefined functions");
  }

  // lookup name in SymbolTable
  public Symbol lookup(String name) throws SymbolNotFoundError {
    //loop through stacks
    Stack<HashMap<String, Symbol>> currentStack = (Stack<HashMap<String, Symbol>>) stack.clone();
    while (!currentStack.peek().containsKey(name) && currentStack.size() > 1) {
      currentStack.pop();
    }
    if (currentStack.peek().containsKey(name)) {
      return currentStack.peek().get(name);
    }
    throw new SymbolNotFoundError(name);
  }

  public ArrayList<Symbol> lookupFunc(String name) throws SymbolNotFoundError {
    if (st.containsKey(name)) {
      return st.get(name);
    }
    throw new SymbolNotFoundError(name);
  }

  // insert name in SymbolTable
  public Symbol insert(String name, Symbol sym) throws RedeclarationError {
    if (sym.type.getClass().equals(FuncType.class)) {
      if (st.containsKey(name)) {
        Iterator<Symbol> it = st.get(name).iterator();
        FuncType symt = (FuncType) sym.type;
        while (it.hasNext()) {
          Symbol simba = it.next();
          FuncType simt = (FuncType) simba.type;
          if (symt.params().list.size() != simt.params().list.size()) {
            continue;
          }
          boolean b = false;
          for (int i = 0; i < simt.params().list.size(); i++) {
            if (
              !simt
                .params()
                .list.get(i)
                .getClass()
                .equals(symt.params().list.get(i).getClass())
            ) {
              b = true;
              break;
            }
          }
          if (b) {
            continue;
          }
          throw new RedeclarationError(name);
        }
        st.get(name).add(sym);
      } else {
        st.put(name, new ArrayList<>());
        st.get(name).add(sym);
      }
      return sym;
    }
    if (stack.peek().containsKey(name)) {
      throw new RedeclarationError(name);
    }
    stack.peek().put(name, sym);
    return stack.peek().get(name);
  }

  public void addScope() {
    stack.push(new HashMap<>());
  }

  public void popScope() {
    stack.pop();
  }
}

class SymbolNotFoundError extends Error {

  private static final long serialVersionUID = 1L;
  private final String name;

  public SymbolNotFoundError(String name) {
    super("Error parsing file.\nResolveSymbolError");
    this.name = name;
  }

  public String name() {
    return name;
  }
}

class RedeclarationError extends Error {

  private static final long serialVersionUID = 1L;
  private final String name;

  public RedeclarationError(String name) {
    super("Error parsing file.\nDeclareSymbolError");
    this.name = name;
  }

  public String name() {
    return name;
  }
}
