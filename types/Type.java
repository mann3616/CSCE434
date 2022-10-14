package types;

import java.util.Iterator;

import pl434.Symbol;

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
      String thisString = this.toString();
      String thatString = that.toString();
      return new ErrorType(
        "Cannot add " + thisString + " to " + thatString + "."
      );
    }
    return that;
  }

  public Type sub(Type that) {
    if (
      !this.getClass().equals(that.getClass()) ||
      this.getClass().equals(BoolType.class) ||
      that.getClass().equals(BoolType.class)
    ) {
      String thisString = this.toString();
      String thatString = that.toString();
      return new ErrorType(
        "Cannot subtract " + thisString + " from " + thatString + "."
      );
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
      return new ErrorType(
        "Cannot compute " + thisString + " or " + thatString + "."
      );
    }
    return that;
  }

  public Type not() {
    if (!this.getClass().equals(BoolType.class)) {
      return new ErrorType("Cannot negate " + this + ".");
    }
    return this;
  }

  // relational
  public Type compare(Type that) {
    if (!this.getClass().equals(that.getClass())) {
      String thisString = this.toString();
      String thatString = that.toString();
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
    if ((!that.getClass().equals(IntType.class) && (that.getClass().equals(ArrayType.class) && !((ArrayType) that).type.getClass().equals(IntType.class)))|| !this.getClass().equals(ArrayType.class)) {
      String some = "";
      if(this.getClass().equals(ErrorType.class)){
        some += Symbol.typeToString(this) + " with ";
      }else {
        some += this + "";
      }
      if(that.getClass().equals(ErrorType.class)){
        some += Symbol.typeToString(that) + " with ";
      }else {
        some += that + ".";
      }
      return new ErrorType("Cannot index " + some);
    }
    return this;
  }

  // statements
  public Type assign(Type source, boolean la, boolean ra) {
    Type thisT = (this instanceof ArrayType ? ((ArrayType) this).type : this);
    Type that = (source instanceof ArrayType ? ((ArrayType) source).type : source);
    if (
      !thisT.getClass().equals(that.getClass()) ||
      thisT.getClass().equals(VoidType.class) ||
      that.getClass().equals(VoidType.class)
    ) {
      return new ErrorType("Cannot assign " +(ra ? "AddressOf("+ that + ")" : that) +" to " + (la ? "AddressOf("+ thisT+ ")" : thisT) + ".");
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
