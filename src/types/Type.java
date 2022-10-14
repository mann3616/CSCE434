package types;

import java.util.Iterator;

public abstract class Type {

  // arithmetic
  public Type mod(Type that) {
    if (
      !this.getClass().equals(that.getClass()) ||
      this.getClass().equals(BoolType.class) ||
      that.getClass().equals(BoolType.class)
    ) {
      return new ErrorType("Cannot modulo " + this + " by " + that + ".");
    }
    return that;
  }

  public Type mul(Type that) {
    if (
      !this.getClass().equals(that.getClass()) ||
      this.getClass().equals(BoolType.class) ||
      that.getClass().equals(BoolType.class)
    ) {
      return new ErrorType("Cannot multiply " + this + " with " + that + ".");
    }
    return that;
  }

  public Type div(Type that) {
    if (
      !this.getClass().equals(that.getClass()) ||
      this.getClass().equals(BoolType.class) ||
      that.getClass().equals(BoolType.class)
    ) {
      return new ErrorType("Cannot divide " + this + " by " + that + ".");
    }
    return that;
  }

  public Type add(Type that) {
    if (
      !this.getClass().equals(that.getClass()) ||
      this.getClass().equals(BoolType.class) ||
      that.getClass().equals(BoolType.class)
    ) {
      return new ErrorType("Cannot add " + this + " to " + that + ".");
    }
    return that;
  }

  public Type sub(Type that) {
    if (
      !this.getClass().equals(that.getClass()) ||
      this.getClass().equals(BoolType.class) ||
      that.getClass().equals(BoolType.class)
    ) {
      return new ErrorType("Cannot subtract " + this + " from " + that + ".");
    }
    return that;
  }

  // boolean
  public Type and(Type that) {
    if (
      !this.getClass().equals(BoolType.class) ||
      !that.getClass().equals(BoolType.class)
    ) {
      String thisString = this.toString();
      String thatString = that.toString();
      if (this.getClass().equals(ErrorType.class)) {
        thisString = "ErrorType(" + thisString + ")";
      }
      if (that.getClass().equals(ErrorType.class)) {
        thatString = "ErrorType(" + thatString + ")";
      }
      return new ErrorType(
        "Cannot compute " + thisString + " and " + thatString + "."
      );
    }
    return that;
  }

  public Type or(Type that) {
    if (
      !this.getClass().equals(BoolType.class) ||
      !that.getClass().equals(BoolType.class)
    ) {
      String thisString = this.toString();
      String thatString = that.toString();
      if (this.getClass().equals(ErrorType.class)) {
        thisString = "ErrorType(" + thisString + ")";
      }
      if (that.getClass().equals(ErrorType.class)) {
        thatString = "ErrorType(" + thisString + ")";
      }
      return new ErrorType(
        "Cannot compute " + thisString + " or " + thatString + "."
      );
    }
    return that;
  }

  public Type not() {
    if (!this.getClass().equals(BoolType.class)) {
      if (this.getClass().equals(ErrorType.class)) {
        return new ErrorType("Cannot negate ErrorType(" + this + ").");
      }
      return new ErrorType("Cannot negate " + this + ".");
    }
    return this;
  }

  // relational
  public Type compare(Type that) {
    if (!this.getClass().equals(that.getClass())) {
      String thisString = this.toString();
      String thatString = that.toString();
      if (this.getClass().equals(ErrorType.class)) {
        thisString = "ErrorType(" + thisString + ")";
      }
      if (that.getClass().equals(ErrorType.class)) {
        thatString = "ErrorType(" + thisString + ")";
      }
      return new ErrorType(
        "Cannot compare " + thisString + " with " + thatString + "."
      );
    }
    return that;
  }

  // designator
  public Type deref() {
    if (this.getClass().equals(FuncType.class)) {
      return new ErrorType("Cannot dereference " + this);
    }
    return this;
  }

  public Type index(Type that) {
    if (!that.getClass().equals(IntType.class)) {
      return new ErrorType("Cannot index " + this + " with " + that + ".");
    }
    return this;
  }

  // statements
  public Type assign(Type source) {
    if (
      !this.getClass().equals(source.getClass()) ||
      this.getClass().equals(VoidType.class) ||
      source.getClass().equals(VoidType.class)
    ) {
      return new ErrorType("Cannot assign " + source + " to " + this + ".");
    }
    return this;
  }

  public Type call(Type args) {
    FuncType t = (FuncType) this;
    Iterator<Type> t1 = t.params.iterator();
    Iterator<Type> t2 = ((TypeList) args).iterator();
    while (t1.hasNext() && t2.hasNext()) {
      if (!t1.next().getClass().equals(t2.next().getClass())) {
        return new ErrorType(args.toString());
      }
    }
    if (t1.hasNext() || t2.hasNext()) {
      return new ErrorType(args.toString());
    }
    return args;
  }
}
