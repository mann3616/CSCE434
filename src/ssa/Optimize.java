package ssa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import ssa.Instruction.op;

public class Optimize {

  SSA ssa;

  public Optimize(SSA ssa) {
    this.ssa = ssa;
  }

  public void dead_code_elim() {}

  public boolean copy_propogation() {
    boolean changed = false;
    // TODO: make revisions to PHI for copy_propogation
    // (Maybe? cuz we can also compare between two different symbols as well)
    for (Block b : ssa.blocks) {
      for (Instruction i : b.instructions) {
        // Is left a variable?
        if (i.left != null && i.left.kind == Result.VAR) {
          //Left is a variable, okay then using the MOVE instruction refed by the variable are we assigning a var?
          if (
            i.left.var.instruction != null &&
            i.left.var.instruction.left.kind == Result.VAR
          ) {
            i.left = i.left.var.instruction.left;
            changed = true;
          }
        }
        // Same as above but for the right side
        if (
          i.right != null && i.right.kind == Result.VAR && i.inst != op.MOVE
        ) {
          if (
            i.right.var.instruction != null &&
            i.right.var.instruction.left.kind == Result.VAR
          ) {
            i.right = i.right.var.instruction.left;
            changed = true;
          }
        }
      }
    }
    return changed;
  }

  public void subexpr_elim() {}

  public boolean constant_propogation() {
    boolean changed = false;
    for (Block b : ssa.blocks) {
      for (Instruction i : b.instructions) {
        // Is left a variable?
        if (i.left != null && i.left.kind == Result.VAR) {
          //Left is a variable, okay then using the MOVE instruction refed by the variable are we assigning a const?
          if (
            i.left.var.instruction != null &&
            i.left.var.instruction.left.kind == Result.CONST
          ) {
            i.left = i.left.var.instruction.left;
            changed = true;
          }
        }
        // Same as above but for the right side
        if (
          i.right != null && i.right.kind == Result.VAR && i.inst != op.MOVE
        ) {
          if (
            i.right.var.instruction != null &&
            i.right.var.instruction.left.kind == Result.CONST
          ) {
            i.right = i.right.var.instruction.left;
            changed = true;
          }
        }
      }
    }
    return changed;
  }

  public boolean constant_folding() {
    boolean changed = false;
    for (Block b : ssa.blocks) {
      int index = 0;
      int new_val = -101;
      for (Instruction i : b.instructions) {
        if (i.eliminated) {
          index++;
          continue;
        }
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
          case CMP:
            if (i.right.kind == Result.CONST && i.left.kind == Result.CONST) {
              new_val =
                (
                  i.right.value - i.left.value >= 0
                    ? (i.right.value - i.left.value != 0 ? 1 : 0)
                    : -1
                );
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
