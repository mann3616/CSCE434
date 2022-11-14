package ssa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import pl434.Symbol;
import ssa.Instruction.op;

public class Optimize {

  SSA ssa;

  public Optimize(SSA ssa) {
    this.ssa = ssa;
  }

  public void dead_code_elim() {
    // Give every block an in-set and and out-set
    // The in-sets and out-sets already exist within the block class

    ArrayList<Instruction> instructionSet = ssa.allInstructions;
    boolean change_detected;
    int loop_count = 0;
    do {
      change_detected = false;
      // Traverse backwards
      // for every instruction in SSA
      for (int i = instructionSet.size() - 1; i >= 0; i--) {
        Instruction currentInstruction = instructionSet.get(i);
        // First save the current in-set and out-set
        HashSet<String> originalInSet = currentInstruction.InSet;
        HashSet<String> originalOutSet = currentInstruction.OutSet;

        HashSet<String> definedSet = new HashSet<>();
        HashSet<String> usedSet = new HashSet<>();
        // MOV e f means move value of e into f
        switch (currentInstruction.inst) {
          case MOVE:
            if (currentInstruction.left.isVariable()) {
              usedSet.add(currentInstruction.left.var.name);
            }
            if (currentInstruction.right.isVariable()) {
              definedSet.add(currentInstruction.right.var.name);
            }
            break;
          default:
            if (
              currentInstruction.left != null &&
              currentInstruction.left.isVariable()
            ) {
              usedSet.add(currentInstruction.left.var.name);
            }
            if (
              currentInstruction.right != null &&
              currentInstruction.right.isVariable()
            ) {
              usedSet.add(currentInstruction.right.var.name);
            }
            // We can figure out what to do with phi later
            break;
        }

        // For out, find the union of previous variables in the in set for each succeeding node of n
        // out[n] := ∪ {in[s] | s ε succ[n]}
        // outSet of a node = the union of all the inSets of n's successors
        for (int j = instructionSet.size() - 1; j > i; j--) {
          currentInstruction.OutSet.addAll(instructionSet.get(j).InSet);
        }

        // in[n] := use[n] ∪ (out[n] - def[n])
        // (out[n] - def[n])
        HashSet<String> temporaryOutSet = new HashSet<String>();
        temporaryOutSet.addAll(currentInstruction.OutSet);
        temporaryOutSet.removeAll(definedSet);
        // use[n]
        usedSet.addAll(temporaryOutSet);
        currentInstruction.InSet = usedSet;

        boolean inSetChanged =
          (!originalInSet.equals(currentInstruction.InSet));
        boolean outSetChanged =
          (!originalOutSet.equals(currentInstruction.OutSet));
        if (inSetChanged || outSetChanged) {
          // This only needs to trigger once to repeat the loop
          change_detected = true;
        }
      }
      // Iterate, until IN and OUT set are constants for last two consecutive iterations.
      loop_count++;
    } while (change_detected);

    for (Instruction instruction : instructionSet) {
      if (instruction.inst == op.MOVE) {
        // Right is a variable that is being assigned to
        // If the outset does not contain a variable that is being defined,
        // Then it means that this definition is unused, so remove it
        if (!instruction.OutSet.contains(instruction.right.var.name)) {
          instruction.eliminated = true;
        }
      }
    }
    // Next, do block checking

  }

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

  public void orphan_function() {
    // Get the list of functions, check every instruction and remove the functions that have been called,
    // Elim the entire block of a function that isn't used
    // Issue - How to deal with functions with multiple parameters?
    // IE, overloaded functions?
    // For now, we assume there's only one definition of a function
    ArrayList<String> functions = new ArrayList<>();
    for (Block b : ssa.roots) {
      if (b.label != "main") {
        functions.add(b.label);
      }
    }

    //Loop through all instructions
    for (Instruction i : ssa.allInstructions) {
      // If the instruction is a function call, remove the
      if (i.inst == Instruction.op.CALL) {
        String function_name = i.func_params.get(0).toString();
        functions.remove(function_name);
      }
    }

    ArrayList<Block> blocksToRemove = new ArrayList<>();
    for (Block b : ssa.blocks) {
      if (functions.contains(b.label)) {
        System.out.println("Removing function: " + b.label);
        blocksToRemove.add(b);
      }
    }
    ssa.blocks.removeAll(blocksToRemove);
  }
}
