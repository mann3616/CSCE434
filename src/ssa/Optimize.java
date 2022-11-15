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

  public boolean dead_code_elim() {
    // Give every block an in-set and and out-set
    // The in-sets and out-sets already exist within the block class

    ArrayList<Instruction> instructionSet = ssa.allInstructions;
    boolean change_detected;
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
    } while (change_detected);

    boolean change_made = false;
    // Get the instruction set of the main block

    for (Block b : ssa.roots) {
      System.out.println("Block " + b);
    }
    for (Instruction instruction : instructionSet) {
      if (instruction.inst == op.MOVE) {
        // Right is a variable that is being assigned to
        // If the outset does not contain a variable that is being defined,
        // Then it means that this definition is unused, so remove it
        if (!instruction.OutSet.contains(instruction.right.var.name)) {
          change_made = true;
          instruction.eliminated = true;
        }
      }
    }
    // Next, do block checking
    return change_made;
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
    // All constant subexpr can be ignored

    changed = false;
    for (Block b : ssa.roots) {
      findAvailableExpr(new HashSet<>(), b, null);
    }
    for (Block b : ssa.blocks) {
      b.visited.clear();
    }
    for (Block b : ssa.blocks) {
      for (Instruction i : b.instructions) {
        if (i.eliminated) continue;
        if (i.left != null && i.left.kind == Result.INST) {
          i.left.inst.usedAt = i;
        }
        if (i.right != null && i.right.kind == Result.INST) {
          i.right.inst.usedAt = i;
        }
      }
    }
    // Do Find Best Place
    // Clean up
    for (Block b : ssa.roots) {
      changed = findBestPlace(new HashSet<>(), b) || changed;
    }
    for (Block b : ssa.blocks) {
      for (Instruction i : b.instructions) {
        if (i.left != null && i.left.kind == Result.INST) {
          i.left.inst.usedAt = null;
        }
        if (i.right != null && i.right.kind == Result.INST) {
          i.right.inst.usedAt = null;
        }
        i.availableExpr.clear();
        i.equivList.clear();
      }
      b.visited.clear();
    }
    return changed;
    // Make list of available expressions before an instruction is ran
    // Will be a hashmap of Instruction to List of instructions
    // Start going through instruction list
    // Using last instruction update available for the nxt instruction

  }

  /*
   * Iterate through all instructions
   * If an instruction has a nonempty equivList and no other instruction has an equivList with it inside
   * The subExpr must encapsulate as much as possible while keeping the same equivList size
   * Gonna need something that says if this expression is the mainExpr
   */
  public boolean findBestPlace(HashSet<Block> visited, Block root) {
    boolean change = false;
    if (visited.contains(root)) {
      return change;
    }
    visited.add(root);
    int k = -1;
    List<Instruction> fuList = new ArrayList<>();
    for (Instruction i : root.instructions) {
      // If the instruction is not the mainEquiv or the equivList is empty then skip
      k++;
      if (i.eliminated || !i.mainEquiv || i.equivList.isEmpty()) {
        continue;
      }
      // Is where it's used also have the same equivalences
      // Need to do it like this because the nxt instruction might not be for it
      if (i.usedAt != null) {
        // Make sure the equivList from i is the equivList of curr but all the instruction nums are + 1
        boolean equiv = true;
        for (Instruction nxt : i.equivList) {
          // Basically if where they were usedAt do not compare
          // then we know this is the best we can get on this branch of instruction
          if (!nxt.usedAt.compare(i.usedAt)) {
            equiv = false;
            break;
          }
        }
        // If equivalent then go to nxt instruction
        if (equiv) {
          continue;
        }
      }
      // So now that we have made sure that this is the biggest we can do then we replace all with new symbol
      // First get the list of instructions that will occur
      instructionList(fuList, i);
      // Now that we have updated the list with the instructions then we add the delimiter
      Result res = new Result();
      res.kind = Result.VAR;
      res.var = new Symbol(subSymbol, true);
      fuList.add(new Instruction(op.MOVE, null, res)); // Set left to null until after forLoop
    }
    // I should now have a fullList of things that need to be created and made
    change = !fuList.isEmpty();
    List<Instruction> single = new ArrayList<>();
    Instruction before = null;
    Block find = null;
    int f = -1;
    // If after is null then all I have to do is put it in the very root node
    int sing = 0;
    int instBehind = 0;
    for (int i = 0; i < fuList.size(); i++) {
      Instruction thisInst = fuList.get(i);
      if (thisInst.inst == op.MOVE) {
        thisInst.left = new Result();
        thisInst.left.kind = Result.INST;
        thisInst.left.inst = single.get(single.size() - 1);
        thisInst.right.var.instruction = thisInst;
        single.add(thisInst);
        for (Instruction place : single) {
          int prev = find.instructions.get(f + 1).my_num;
          renumber(prev);
          place.my_num = prev;
          find.instructions.add(f + 1, place);
          f++;
        }
        Instruction rep = fuList.get(i - 1).usedAt;
        if (
          rep.left != null &&
          rep.left.kind == Result.INST &&
          rep.left.inst == rep
        ) {
          rep.left.inst = null;
          rep.left.kind = Result.VAR;
          rep.left.var = thisInst.right.var;
        } else {
          rep.right.inst = null;
          rep.right.kind = Result.VAR;
          rep.right.var = thisInst.right.var;
        }
        for (Instruction rr : fuList.get(i - 1).equivList) {
          rep = rr.usedAt;
          if (
            rep.left != null &&
            rep.left.kind == Result.INST &&
            rep.left.inst == rep
          ) {
            rep.left.inst = null;
            rep.left.kind = Result.VAR;
            rep.left.var = thisInst.right.var;
          } else {
            rep.right.inst = null;
            rep.right.kind = Result.VAR;
            rep.right.var = thisInst.right.var;
          }
        }

        single.clear(); // Clear instructions for op.MOVE
        instBehind = 0;
        sing = 0;
        continue;
      }
      // Before and f have been created
      if (i == 0 || fuList.get(i - 1).inst == op.MOVE) {
        // Find After (common Dominator between equivList and itself)
        boolean itself = true;
        // If they all have the same dom of it then it will go there
        for (Instruction a : thisInst.equivList) {
          if (!a.blockLoc.doms.contains(thisInst.blockLoc)) {
            itself = false;
          }
        }
        if (!itself) {
          find = findBlock(new HashSet<>(), thisInst, thisInst.blockLoc);
          // Find best place to put it
          f = find.instructions.size() - 1;
          for (; f >= 0; f--) {
            if (
              !find.instructions.get(f).eliminated &&
              (
                find.instructions.get(f).inst == op.MOVE ||
                find.instructions.get(f).inst == op.PHI
              ) ||
              f == 0
            ) {
              before = find.instructions.get(f);
              f =
                (
                  f == 0 &&
                    find.instructions.get(f).inst != op.MOVE &&
                    find.instructions.get(f).inst != op.PHI
                    ? -1
                    : f
                );
              break;
            }
          }
        } else {
          // Find Best place to put it
          find = thisInst.blockLoc;
          f = 0;
          for (Instruction ff : find.instructions) {
            if (thisInst == ff) {
              break;
            }
            f++;
          }
          for (; f >= 0; f--) {
            if (
              !find.instructions.get(f).eliminated &&
              (
                find.instructions.get(f).inst == op.MOVE ||
                find.instructions.get(f).inst == op.PHI
              ) ||
              f == 0
            ) {
              before = find.instructions.get(f);
              f = (f == 0 ? -1 : f);
              break;
            }
          }
        }
      }
      // Create new instructions based on these in fuList
      Result right = new Result();
      right.kind = thisInst.right.kind;
      if (right.kind == Result.INST) {
        right.inst = single.get(sing - instBehind--);
      }
      if (right.kind == Result.VAR) {
        right.var = thisInst.right.var;
      }
      if (right.kind == Result.CONST) {
        right.value = thisInst.right.value;
      }
      if (right.kind == Result.PROC) {
        right.proc = thisInst.right.proc;
      }
      Result left = null;
      if (thisInst.left != null) {
        left = new Result();
        left.kind = thisInst.left.kind;
        if (left.kind == Result.INST) {
          left.inst = single.get(sing - instBehind--);
        } else if (right.kind != Result.INST) {
          instBehind++;
        }
        if (left.kind == Result.VAR) {
          left.var = thisInst.left.var;
        }
        if (left.kind == Result.CONST) {
          left.value = thisInst.left.value;
        }
        Instruction jk = new Instruction(thisInst.inst, left, right);
        jk.blockLoc = find;
        single.add(jk);
      } else {
        if (right.kind != Result.INST) {
          instBehind++;
        }
        Instruction jk = new Instruction(thisInst.inst, null, right);
        jk.blockLoc = find;
        single.add(jk);
      }
      thisInst.eliminated = true;
      for (Instruction kInstruction : thisInst.equivList) {
        kInstruction.eliminated = true;
      }
      sing++;
    }
    for (Block e : root.edges) {
      change = findBestPlace(visited, e) || change;
    }
    return change;
  }

  public void renumber(int ren) {
    for (Block b : ssa.blocks) {
      for (Instruction i : b.instructions) {
        if (i.my_num >= ren) {
          i.my_num++;
        }
      }
    }
  }

  public void instructionList(List<Instruction> insts, Instruction root) {
    if (root.left != null && root.left.kind == Result.INST) {
      instructionList(insts, root.left.inst);
    }
    if (root.right != null && root.right.kind == Result.INST) {
      instructionList(insts, root.right.inst);
    }
    insts.add(root);
  }

  public Block findBlock(
    HashSet<Block> visited,
    Instruction thisInst,
    Block root
  ) {
    if (visited.contains(root)) {
      return root;
    }
    visited.add(root);
    if (thisInst.blockLoc.doms.contains(root)) {
      boolean done = true;
      for (Instruction bb : thisInst.equivList) {
        if (!bb.blockLoc.doms.contains(root)) {
          done = false;
        }
      }
      if (done) {
        return root;
      }
    }
    for (Block b : root.parents) {
      Block a = findBlock(visited, thisInst, b);
      if (a != null) {
        return a;
      }
    }
    return null;
  }

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
    //Only for printing
    if (visited.contains(root)) {
      for (Instruction j : root.instructions) {
        System.out.println(
          (j.eliminated ? "elim - " : "") +
          j.my_num +
          (j.mainEquiv ? " isMainEquiv" : "")
        );
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
            if (lastAvailable != currExpr.availableExpr.get(p)) {
              currExpr.availableExpr.get(p).equivList.add(lastAvailable); // IS ORDER DEPENDENT BUT SHOULD WORK IF THE NEXT STEP IS COMPUTED IN THE SAME ORDER AS THIS ONE
              lastAvailable.mainEquiv = false;
              // Add the expressions equivList to the currExpr
              for (Instruction q : lastAvailable.equivList) {
                q.mainEquiv = false;
                currExpr.availableExpr.get(p).equivList.add(q);
              }
            }
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
          if (exprToRm.left != null && exprToRm.left.kind == Result.INST) {
            boolean hasAvailable = false;
            for (Instruction contains : currExpr.availableExpr) {
              if (contains.compare(exprToRm.left.inst)) {
                hasAvailable = true;
                break;
              }
            }
            if (!hasAvailable) {
              currExpr.availableExpr.remove(k--);
              size--;
            }
          } else if (
            exprToRm.right != null && exprToRm.right.kind == Result.INST
          ) {
            boolean hasAvailable = false;
            for (Instruction contains : currExpr.availableExpr) {
              if (contains.compare(exprToRm.right.inst)) {
                hasAvailable = true;
                break;
              }
            }
            if (!hasAvailable) {
              currExpr.availableExpr.remove(k--);
              size--;
            }
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
        if (!subexpr.equivList.contains(currExpr)) {
          currExpr.mainEquiv = false;
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
    // TODO: Deal with special arithmetic cases of 0 and 1
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
            if (rightConst && i.right.value == 1) {
              if (leftConst) {
                new_val = i.left.value;
                changed = i.eliminated = true;
              }
            } else if (leftConst && i.left.value == 1) {
              if (rightConst) {
                new_val = i.right.value;
                changed = i.eliminated = true;
              }
            }

            if (rightConst && i.right.value == -1) {
              if (leftConst) {
                new_val = i.left.value * -1;
                changed = i.eliminated = true;
              }
            } else if (leftConst && i.left.value == -1) {
              if (rightConst) {
                new_val = i.right.value * -1;
                changed = i.eliminated = true;
              }
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
              if (i.right.value == 1) {
                new_val = i.left.value;
              } else if (i.right.value == -1) {
                new_val = i.left.value * -1;
              }
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

  public boolean orphan_function() {
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
    boolean change_made = false;
    for (Block b : ssa.blocks) {
      if (functions.contains(b.label)) {
        change_made = true;
        blocksToRemove.add(b);
      }
    }
    ssa.blocks.removeAll(blocksToRemove);
    return change_made;
  }

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
