package ssa;

public class Optimize {

  SSA ssa;

  public Optimize(SSA ssa) {
    this.ssa = ssa;
  }

  public void dead_code_elim() {}

  public void copy_propogation() {}

  public void constant_propogation() {}

  public void subexpr_elim() {}

  public void constant_folding() {}
}
