package ssa;

import java.util.ArrayList;
import java.util.List;

public class Optimize {

  SSA ssa;

  public Optimize(SSA ssa) {
    this.ssa = ssa;
  }

  public void dead_code_elim() {}

  public void copy_propogation() {}

  public void constant_propogation() {}

  public void subexpr_elim() {}

  public boolean constant_folding() {
    boolean changed = false;
    for (Block b : ssa.roots) {
      int index = 0;
      int new_val = -101;
      for (Instruction i : b.instructions) {
        switch (i.inst) {
          case NEG:
            if (i.right.kind == Result.CONST) {
              new_val = (i.right.value == 1 ? 0 : 1);
              changed = true;
              changed = i.eliminated = true;
            }
            break;
          case MUL:
            if (i.right.kind == Result.CONST && i.left.kind == Result.CONST) {
              new_val = i.right.value * i.left.value;
              changed = i.eliminated = true;
            }
            break;
          case MOD:
            if (i.right.kind == Result.CONST && i.left.kind == Result.CONST) {
              new_val = i.left.value % i.right.value;
              changed = i.eliminated = true;
            }
            break;
          case ADD:
            if (i.right.kind == Result.CONST && i.left.kind == Result.CONST) {
              new_val = i.right.value + i.left.value;
              changed = i.eliminated = true;
            }
            break;
          case SUB:
            if (i.right.kind == Result.CONST && i.left.kind == Result.CONST) {
              new_val = i.left.value - i.right.value;
              changed = i.eliminated = true;
            }
            break;
          case DIV:
            if (i.right.kind == Result.CONST && i.left.kind == Result.CONST) {
              new_val = i.left.value / i.right.value;
              changed = i.eliminated = true;
            }
            break;
          case AND:
            if (i.right.kind == Result.CONST && i.left.kind == Result.CONST) {
              new_val = i.right.value & i.left.value;
              changed = i.eliminated = true;
            }
            break;
          case OR:
            if (i.right.kind == Result.CONST && i.left.kind == Result.CONST) {
              new_val = i.right.value | i.left.value;
              changed = i.eliminated = true;
            }
            break;
        }
        index++;
        if (i.eliminated) {
          Instruction nxt = b.instructions.get(index);
          if (nxt.left.kind == Result.INST && nxt.left.inst == i) {
            nxt.left.kind = Result.CONST;
            nxt.left.value = new_val;
          }
          if (nxt.right.kind == Result.INST && nxt.right.inst == i) {
            nxt.right.kind = Result.CONST;
            nxt.right.value = new_val;
          }
        }
      }
    }
    return changed;
  }

  public void orphan_function() {}
}
