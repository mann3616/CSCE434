package ssa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import pl434.Symbol;
import types.FuncType;

public class Block {

  // Block info
  public static int block_num = 1;
  public int my_num;
  public String label;
  public List<Instruction> instructions;
  boolean hasBreak;

  // Graphing info
  public List<Block> edges;
  public HashSet<Block> edgeSet;
  public List<Block> parents;

  // Dom tree graph info
  public HashSet<Block> visited;
  public HashSet<Block> domFront;
  public HashSet<Block> idomChildren;
  Block iDom;
  public LinkedHashSet<Block> doms;
  public List<String> edgeLabels;

  // Phi info (set of vars), and var to list of instructions
  public HashSet<Symbol> blockVars;
  public HashMap<Symbol, LinkedHashSet<Instruction>> symbolLocation;

  public Block() {
    symbolLocation = new HashMap<>();
    visited = new HashSet<>();
    iDom = null;
    domFront = new HashSet<>();
    parents = new ArrayList<>();
    instructions = new ArrayList<>();
    edges = new ArrayList<>();
    edgeLabels = new ArrayList<>();
    doms = new LinkedHashSet<>();
    idomChildren = new HashSet<>();
    my_num = Block.block_num++;
    hasBreak = false;
    label = "";
    doms.add(this);
  }

  public void addInstruction(Instruction inst) {
    instructions.add(inst);
    // Add left Result if it's a symbol that is not a function
    if (
      inst.left != null &&
      inst.left.kind == Result.VAR &&
      inst.left.var.type.getClass().equals(FuncType.class)
    ) {
      blockVars.add(inst.left.var);
      if (!symbolLocation.containsKey(inst.left.var)) {
        symbolLocation.put(inst.left.var, new LinkedHashSet<>());
      }
      symbolLocation.get(inst.left.var).add(inst);
    }
    // Add right Result if it's a symbol that is not a function
    if (
      inst.right != null &&
      inst.right.kind == Result.VAR &&
      inst.right.var.type.getClass().equals(FuncType.class)
    ) {
      blockVars.add(inst.right.var);
      if (!symbolLocation.containsKey(inst.right.var)) {
        symbolLocation.put(inst.right.var, new LinkedHashSet<>());
      }
      symbolLocation.get(inst.right.var).add(inst);
    }
  }

  public void addEdge(Block block, String edgeLabel) {
    edges.add(block);
    edgeLabels.add(edgeLabel);
    block.parents.add(this);
  }

  public void solveIDom() {
    // Loop through all dominating nodes
    for (Block a : doms) {
      // Only check nodes that are not itself
      if (a != this) {
        // Loop through all doms again, but this time make sure no other dom is dominated by b
        boolean noDoms = true;
        for (Block c : doms) {
          if (c != a && c != this) {
            if (c.doms.contains(a)) {
              noDoms = false;
              break;
            }
          }
        }
        if (noDoms) {
          iDom = a;
          a.idomChildren.add(this);
          return;
        }
      }
    }
  }

  @Override
  public String toString() {
    StringBuffer asString = new StringBuffer();
    asString.append(
      "BB" +
      my_num +
      "[shape=record, label=\"<b>" +
      label +
      "\\nBB" +
      my_num +
      "|{"
    );
    for (Instruction i : instructions) {
      asString.append(i + " |");
    }
    asString.delete(asString.length() - 2, asString.length());
    asString.append("}\"];");
    int i = 0;
    for (Block b : edges) {
      asString.append(
        "\nBB" +
        my_num +
        " -> " +
        "BB" +
        b.my_num +
        " " +
        "[label=\"" +
        (hasBreak ? edgeLabels.get(i) : "") +
        "\"" +
        "];"
      );
      i++;
    }
    return asString.toString();
  }
}
