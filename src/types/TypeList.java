package types;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TypeList extends Type implements Iterable<Type> {

  public List<Type> list;

  public TypeList() {
    list = new ArrayList<>();
  }

  public void append(Type type) {
    list.add(type);
  }

  public List<Type> getList() {
    return list;
  }

  @Override
  public Iterator<Type> iterator() {
    return list.iterator();
  }

  //TODO more helper here

  public String toString() {
    String message = "TypeList(";
    for (Type t : list) {
      if (t instanceof ErrorType) {
        message += "ErrorType(" + t + ")";
      } else {
        message += t;
      }
    }
    message += ")";
    return message;
  }
}
