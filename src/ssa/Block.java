package ssa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import pl434.Symbol;
import ssa.Instruction.op;
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
  HashMap<Symbol, Symbol> phi1 = new HashMap<>();
  HashMap<Symbol, Symbol> phi2 = new HashMap<>();

  // Phi info (set of vars), and var to list of instructions

  // Add all phi's then DFS through and fill in PHI's as needed
  // After we renumber
  public HashSet<Symbol> blockVars;
  public HashMap<Symbol, List<Symbol>> OGtoUse;
  public HashMap<Symbol, Instruction> phis;
  public HashMap<Symbol, LinkedHashSet<Instruction>> symbolLocation;
  public List<Instruction> assigns = new ArrayList<>();
  HashMap<Symbol, Symbol> latest = new HashMap<>();

  public Block() {
    OGtoUse = new HashMap<>();
    phis = new HashMap<>();
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

  // Create instructions for each
  public void createPhiInst() {
    for (Entry<Symbol, Instruction> c : phis.entrySet()) {
      if (!phi1.containsKey(c.getKey()) || !phi2.containsKey(c.getKey())) {
        continue;
      }
      c.getValue().right = new Result();
      c.getValue().right.kind = Result.VAR;
      c.getValue().right.var = phi1.get(c.getKey());
      c.getValue().left = new Result();
      c.getValue().left.var = phi2.get(c.getKey());
      c.getValue().left.kind = Result.VAR;
      instructions.add(0, c.getValue());
    }
  }

  public void findPhiVars() {
    for (Block p : parents) {
      for (Entry<Symbol, Symbol> n : p.latest.entrySet()) {
        if (phis.containsKey(n.getKey())) {
          addPhi(n.getKey(), n.getValue());
        }
      }
    }
  }

  // May need to be updated to resolve bigger or smaller phis
  public void addPhi(Symbol phi, Symbol version) {
    if (!phi1.containsKey(phi)) {
      phi1.put(phi, version);
    } else if (!phi2.containsKey(phi)) {
      phi2.put(phi, version);
    }
  }

  public void addInstruction(Instruction inst) {
    instructions.add(inst);
    // Add left Result if it's a symbol that is not a function
    if (inst.inst.equals(op.MOVE)) {
      assigns.add(inst);
    }
    if (inst.inst.equals(op.LOAD)) {
      return;
    }
    if (
      inst.left != null &&
      inst.left.kind == Result.VAR &&
      inst.left.var.OG.type.getClass().equals(FuncType.class)
    ) {
      blockVars.add(inst.left.var.OG);
      // Add instruction to Symbol
      if (!symbolLocation.containsKey(inst.left.var.OG)) {
        symbolLocation.put(inst.left.var.OG, new LinkedHashSet<>());
      }
      symbolLocation.get(inst.left.var.OG).add(inst);
      // Add new instance of Symbol to it's orignal
      if (!OGtoUse.containsKey(inst.left.var.OG)) {
        OGtoUse.put(inst.left.var.OG, new ArrayList<>());
      }
      OGtoUse.get(inst.left.var.OG).add(inst.left.var);
    }
    // Add right Result if it's a symbol that is not a function
    if (
      !inst.inst.equals(op.STORE) &&
      inst.right != null &&
      inst.right.kind == Result.VAR &&
      inst.right.var.OG.type.getClass().equals(FuncType.class)
    ) {
      blockVars.add(inst.right.var.OG);
      // Add instruction to Symbol
      if (!symbolLocation.containsKey(inst.right.var.OG)) {
        symbolLocation.put(inst.right.var.OG, new LinkedHashSet<>());
      }
      symbolLocation.get(inst.right.var.OG).add(inst);
      // Add new instance of Symbol to it's original
      if (!OGtoUse.containsKey(inst.right.var.OG)) {
        OGtoUse.put(inst.right.var.OG, new ArrayList<>());
      }
      OGtoUse.get(inst.right.var.OG).add(inst.right.var);
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
