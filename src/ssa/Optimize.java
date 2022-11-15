package ssa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import pl434.Symbol;
import ssa.Instruction.op;
import types.VoidType;

public class Optimize {

  SSA ssa;
  Symbol subSymbol;

  public Optimize(SSA ssa) {
    subSymbol = new Symbol("$t", new VoidType());
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

  public boolean subexpr_elim() {
    boolean changed = false;
    // Operators where order matters will need to have left and right exactly the same
    // Operators where it doesn't then left and right must exist either as left or right
    // So lets work backwards
    // TODO: Be able to find the common expression between two starts of expression

    // TODO: Find these start of expression
    // Doesnt matter that we check the right places cuz versioning will make sure the right things get elim
    // So once we come across an expression we add the whole thing to the list and then pass through whole graph and find exact copies
    // Not all expressions are of format
    // OP !INST !INST
    // OP INST (INST || !INST)
    // Cuz parantheses

    //Clearing visited map on each block

    changed = true;
    for (Block b : ssa.roots) {
      findAvailableExpr(new HashSet<>(), b, null);
    }
    for (Block b : ssa.blocks) {
      b.visited.clear();
    }
    return changed;
    // Make list of available expressions before an instruction is ran
    // Will be a hashmap of Instruction to List of instructions
    // Start going through instruction list
    // Using last instruction update available for the nxt instruction

  }

  //TODO: Implement another traversal that does several things
  // 1.   Using the equivExpr list. Find each Instruction's block and find the common dominator for all.
  // 2.   Do this in the same pathing taken for findAvailableExpr
  // 3.   When using the findAvailableExpr start from the bottom to the top and
  // 4.   Use the versioning to figure out where to place the value
  // Case 0: an expresion is teh format (x+y) * (x+y) - just check if that is the rootExpr
  // Case 1:  z = (x+y)*2 | b = (x+y)*5 | c = (x+y) *2 - t0 = x+y | t1 = t0*2 | z = t1 | b = t0*5 | c = t1 (this takes two passes of subexprElim)
  // ISSUE: if block children could have the same expression as each other but never know since they were ran from a parent
  // Solution: Compare all blocks that do not dominate each other !

  //Calculates Available Expressions
  public void findAvailableExpr(
    HashSet<Block> visited,
    Block root,
    Instruction last
  ) {
    if (visited.contains(root)) {
      return;
    }
    if (root.visited.size() == root.parents.size()) {
      visited.add(root);
    }
    calculateAvailability(root, last);
    for (Block e : root.edges) {
      // Has already been visited by this parent?
      if (!e.visited.contains(root)) {
        e.visited.add(root);
        findAvailableExpr(
          visited,
          e,
          root.instructions.get(root.instructions.size() - 1)
        );
      }
    }
    if (visited.contains(root)) {
      for (Instruction j : root.instructions) {
        System.out.println((j.eliminated ? "elim - " : "") + j.my_num);
        for (Instruction k : j.availableExpr) {
          System.out.println("  " + k);
          for (Instruction q : k.equivList) {
            System.out.println("    " + q);
          }
        }
        if (j.availableExpr.isEmpty()) {
          System.out.println("  EMPTY");
        }
      }
    }
  }

  public void calculateAvailability(Block root, Instruction last) {
    Instruction prev = last;
    for (Instruction i : root.instructions) {
      availableInstruction(root, i, prev);
      prev = i;
    }
  }

  public void availableInstruction(
    Block root,
    Instruction currExpr,
    Instruction last
  ) {
    if (last != null) {
      // Only add the instructions that do not exist (only meaningful for blocks that get looped around too)
      int size = currExpr.availableExpr.size();
      for (Instruction lastAvailable : last.availableExpr) {
        boolean exists = false;
        for (int p = 0; p < size; p++) {
          if (currExpr.availableExpr.get(p).compare(lastAvailable)) {
            exists = true;
            break;
          }
        }
        if (!exists) {
          currExpr.availableExpr.add(lastAvailable);
        }
      }
    }
    if (currExpr.eliminated) return; // If elim then skip
    // If it's a move statement then we remove all instances with the previous variable in it
    if (currExpr.inst == op.MOVE || currExpr.inst == op.PHI) {
      Symbol elim =
        (
          currExpr.inst == op.PHI
            ? currExpr.third.var.OG
            : currExpr.right.var.OG
        );
      int size = currExpr.availableExpr.size();
      for (int k = 0; k < size; k++) {
        // If the left side is a variable and is a version of OG
        Instruction exprToRm = currExpr.availableExpr.get(k);
        boolean alreadyRemove = false;
        // Remove if left var matches OG
        if (
          exprToRm.left != null &&
          exprToRm.left.kind == Result.VAR &&
          exprToRm.left.var.OG == elim
        ) {
          currExpr.availableExpr.remove(k--);
          size--;
          alreadyRemove = true;
        }
        // If left var matched then remove instruction related to this one
        if (alreadyRemove && exprToRm.right.kind == Result.INST) {
          System.out.println(
            "Trying to remove right instruction " +
            exprToRm.right.inst.my_num +
            " success? " +
            currExpr.availableExpr.remove(exprToRm.right.inst)
          );
          k--;
          size--;
        }
        // If left var did not match and right matches then remove
        else if (
          !alreadyRemove &&
          exprToRm.right != null &&
          exprToRm.right.kind == Result.VAR &&
          exprToRm.right.var.OG == elim
        ) {
          alreadyRemove = true;
          currExpr.availableExpr.remove(k--);
          size--;
          // Remove related instructions to this one
          if (exprToRm.left != null && exprToRm.left.kind == Result.INST) {
            System.out.println(
              "Trying to remove left instruction " +
              exprToRm.left.inst.my_num +
              " success? " +
              currExpr.availableExpr.remove(exprToRm.left.inst)
            );
            k--;
            size--;
          }
        }
        // If none of the above occurred check that if left || right are instructions then check that the instruction has not been removed
        else {
          if (
            exprToRm.left != null &&
            exprToRm.left.kind == Result.INST &&
            !currExpr.availableExpr.contains(exprToRm.left.inst)
          ) {
            currExpr.availableExpr.remove(k--);
            size--;
          } else if (
            exprToRm.right != null &&
            exprToRm.right.kind == Result.INST &&
            !currExpr.availableExpr.contains(exprToRm.right.inst)
          ) {
            currExpr.availableExpr.remove(k--);
            size--;
          }
        }
      }
    }
    // If is not an expression type then skip after making sure rootExpr is set
    if (!isExpr(currExpr)) {
      if (last != null && isExpr(last)) {
        last.rootExpr = true; // Might be completely useless
      }
      return;
    }
    // So now that it is an expression we must add the expression if it doesnt already exist in the list
    boolean exists = false;
    for (Instruction subexpr : currExpr.availableExpr) {
      // If the comparison is true then break
      if (currExpr.compare(subexpr)) {
        exists = true;
        // If the subExpr is not a direct copy but still equal,
        // then we add the other instruction to the list of copies
        if (subexpr != currExpr && !subexpr.equivList.contains(currExpr)) {
          subexpr.equivList.add(currExpr);
        }
        break;
      }
    }
    // Add if nothing compared
    if (!exists) {
      currExpr.availableExpr.add(currExpr);
    }
  }

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
            boolean rightConst = i.right.kind == Result.CONST;
            boolean leftConst = i.left.kind == Result.CONST;
            if (rightConst && leftConst) {
              new_val = i.right.value * i.left.value;
              changed = i.eliminated = true;
            }
            if (
              (rightConst && i.right.value == 0) ||
              (leftConst && i.left.value == 0)
            ) {
              new_val = 0;
              changed = i.eliminated = true;
            }
            // Add something for multiplying by 1
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

  public static boolean isExpr(Instruction instruction) {
    switch (instruction.inst) {
      case ADD:
        if (instruction.left.kind == Result.GDB) {
          return false;
        }
      case MUL:
        if (instruction.isArrayMul) {
          return false;
        }
      case DIV:
      case SUB:
      case MOD:
      case AND:
      case OR:
      case NEG:
      case CMP:
      case BLE:
      case BNE:
      case BEQ:
      case BLT:
      case BGT:
        return true;
      default:
        return false;
    }
  }
}
