package types;

public abstract class Type {

    // arithmetic
    public Type mul (Type that) {
        return new ErrorType("Cannot multiply " + this + " with " + that + ".");
    }

    public Type div (Type that) {
        return new ErrorType("Cannot divide " + this + " by " + that + ".");
    }

    public Type add (Type that) {
        return new ErrorType("Cannot add " + this + " to " + that + ".");
    }

    public Type sub (Type that) {
        return new ErrorType("Cannot subtract " + that + " from " + this + ".");
    }

    // boolean
    public Type and (Type that) {
        return new ErrorType("Cannot compute " + this + " and " + that + ".");
    }

    public Type or (Type that) {
        return new ErrorType("Cannot compute " + this + " or " + that + ".");
    }

    public Type not () {
        return new ErrorType("Cannot negate " + this + ".");
    }

    // relational
    public Type compare (Type that) {
        return new ErrorType("Cannot compare " + this + " with " + that + ".");
    }

    // designator
    public Type deref () {
        return new ErrorType("Cannot dereference " + this);
    }

    public Type index (Type that) {
        return new ErrorType("Cannot index " + this + " with " + that + ".");
    }

    // statements
    public Type assign (Type source) {
        return new ErrorType("Cannot assign " + source + " to " + this + ".");
    }

    public Type call (Type args) {
        return new ErrorType("Cannot call " + this + " using " + args + ".");
    }

}