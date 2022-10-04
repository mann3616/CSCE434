package ast;

public interface Visitable {

    public void accept (NodeVisitor visitor);
}
