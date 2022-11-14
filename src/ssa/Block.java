package ssa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.TreeMap;
import pl434.Symbol;
import ssa.Instruction.op;
import types.FuncType;

public class Block {

  // Block info
  SSA ssa;
  public static int block_num = 1;
  public int my_num;
  public String label;
  public List<Instruction> instructions;
  boolean hasBreak;
  boolean endIfNode = false;
  boolean isJoinNode;

  // Graphing info
  public List<Block> edges;
  public HashSet<Block> edgeSet;
  public List<Block> parents;
  public Instruction firstInst;

  // Dom tree graph info
  public HashSet<Block> visited;
  public HashSet<Block> parsVisited = new HashSet<>();
  public HashSet<Block> domFront;
  public HashSet<Block> idomChildren;
  Block iDom;
  public LinkedHashSet<Block> doms;
  public List<String> edgeLabels;
  HashMap<Symbol, Symbol> phi1 = new HashMap<>();
  HashMap<Symbol, Symbol> phi2 = new HashMap<>();
  HashMap<Block, HashMap<Symbol, Symbol>> phiBlock = new HashMap<>();

  public HashSet<Symbol> blockVars;
  public HashMap<Symbol, List<Symbol>> OGtoUse;
  public HashMap<Symbol, Instruction> phis;
  public HashMap<Symbol, LinkedHashSet<Instruction>> symbolLocation;
  public List<Instruction> assigns = new ArrayList<>();
  HashMap<Symbol, Symbol> latest = new HashMap<>();

  public Block(SSA ssa) {
    this.ssa = ssa;
    isJoinNode = false;
    edgeSet = new HashSet<>();
    OGtoUse = new HashMap<>();
    firstInst = null;
    phis = new HashMap<>();
    // new Comparator<Symbol>() {
    //   @Override
    //   public int compare(Symbol o1, Symbol o2) {
    //     // TODO Auto-generated method stub
    //     if(o1.equals(o2)) {
    //       return 0;
    //     }
    //     return o1.my_assign - o2.my_assign;
    //   }
    // }
    symbolLocation = new HashMap<>();
    blockVars = new HashSet<>();
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
    // This adds them in reverse order
    for (Entry<Symbol, Instruction> c : phis.entrySet()) {
      // This is so we don't do it twice
      if (!phi1.containsKey(c.getKey()) || !phi2.containsKey(c.getKey())) {
        continue;
      }
      if (phi1.get(c.getKey()) == phi2.get(c.getKey())) {
        continue;
      }
      c.getValue().right = new Result();
      c.getValue().right.kind = Result.VAR;
      c.getValue().right.var = phi1.get(c.getKey());
      c.getValue().left = new Result();
      c.getValue().left.var = phi2.get(c.getKey());
      c.getValue().left.kind = Result.VAR;
      int index_num =
        (
          c.getValue().left.var.getVersion() >
            c.getValue().right.var.getVersion()
            ? (c.getValue().left.var.getVersion() + 1)
            : (c.getValue().right.var.getVersion() + 1)
        );
      c.getValue().my_num = index_num;
      c.getValue().third = new Result();
      c.getValue().third.kind = Result.VAR;
      c.getValue().third.var = new Symbol(c.getKey(), true);
      c.getValue().third.var.my_assign = index_num;
      c.getValue().third.var.instruction = c.getValue();
      // Need to visit EVERY block
      if (
        c.getValue().third.var.my_assign > this.latest.get(c.getKey()).my_assign
      ) {
        this.latest.put(c.getKey(), c.getValue().third.var);
      }
      HashSet<Block> visited = new HashSet<>();
      instRenumber(visited, this, false, c.getValue(), null);
      // Finish renumbering blocks
      for (Block re : ssa.blocks) {
        if (!visited.contains(re)) {
          renumBlock(true, re, c.getValue(), null);
          visited.add(re);
        }
      }
      instructions.add(0, c.getValue());
    }
  }

  // Replace above with instruction keep
  public void instRenumber(
    HashSet<Block> visited,
    Block root,
    boolean move1,
    Instruction keep,
    Block prev
  ) {
    // If there is a move then we do not reset latest, if there
    if (visited.contains(root)) {
      return;
    }
    if (prev != null) {
      root.parsVisited.add(prev);
    }
    if (root == this || root.parsVisited.size() == root.parents.size()) {
      visited.add(root);
      root.parsVisited.clear();
    }
    move1 = renumBlock(move1, root, keep, prev);
    for (Block b : root.edges) {
      instRenumber(visited, b, move1, keep, root);
    }
  }

  public boolean renumBlock(
    boolean move1,
    Block root,
    Instruction keep,
    Block prev
  ) {
    Symbol OG = keep.third.var.OG;
    for (Instruction i : root.instructions) {
      if (root.parsVisited.isEmpty() && i.my_num >= keep.my_num) {
        i.my_num++;
      }
      if (i.inst == op.PHI) {
        if (!move1) {
          move1 = phiSwitch(i, keep, root, prev);
        }
        continue;
      }
      if (i.inst.equals(op.MOVE)) {
        if (OG == i.right.var.OG) {
          move1 = true;
        }
      }
      // If we haven't moved into the var then continue setting all instances with PHI var
      if (
        !move1 &&
        root.symbolLocation.containsKey(OG) &&
        root.symbolLocation.get(OG).contains(i)
      ) {
        if (
          i.left != null && i.left.kind == Result.VAR && i.left.var.OG == OG
        ) {
          i.left.var = keep.third.var;
        }
        if (
          i.right != null && i.right.kind == Result.VAR && i.right.var.OG == OG
        ) {
          i.right.var = keep.third.var;
        }
      }
    }
    if (!move1 && root.phis.containsKey(OG) && root != this) {
      if (
        root.phiBlock.get(prev) == root.phi1 &&
        (
          !root.phi1.containsKey(OG) ||
          (
            root.phi1.get(OG).getVersion() < keep.third.var.getVersion() &&
            (
              !root.phi2.containsKey(OG) ||
              keep.third.var.getVersion() != root.phi2.get(OG).getVersion()
            )
          )
        )
      ) {
        // if (root.phi1.containsKey(OG)) {
        //   root.phi2.put(OG, root.phi1.get(OG));
        // }
        root.phi1.put(OG, keep.third.var);
      } else if (
        root.phiBlock.get(prev) == root.phi2 &&
        (
          !root.phi2.containsKey(OG) ||
          (
            root.phi2.get(OG).getVersion() < keep.third.var.getVersion() &&
            (
              !root.phi1.containsKey(OG) ||
              keep.third.var.getVersion() != root.phi1.get(OG).getVersion()
            )
          )
        )
      ) {
        root.phi2.put(OG, keep.third.var);
      }
    }
    return move1;
  }

  public boolean phiSwitch(
    Instruction phi,
    Instruction keep,
    Block root,
    Block prev
  ) {
    Symbol OG = keep.third.var.OG;
    if (phi.third.var.OG != OG) {
      return false;
    }
    if (
      root.phiBlock.get(prev) == root.phi1 &&
      keep.third.var.getVersion() > phi.right.var.getVersion()
    ) {
      phi.right.var = keep.third.var;
      return true;
    } else if (
      root.phiBlock.get(prev) == root.phi2 &&
      keep.third.var.getVersion() > phi.left.var.getVersion()
    ) {
      phi.left.var = keep.third.var;
      return true;
    }
    return false;
  }

  public void findPhiVars() {
    for (Block p : parents) {
      for (Entry<Symbol, Symbol> n : p.latest.entrySet()) {
        // When should we add phi?
        // We add phi's if phis contains this...
        if (phis.containsKey(n.getKey())) {
          addPhi(n.getKey(), n.getValue(), p);
        }
      }
    }
  }

  // May need to be updated to resolve bigger or smaller phis
  public void addPhi(Symbol phi, Symbol version, Block par) {
    // What does this do?
    if (
      phiBlock.get(par) == phi1 &&
      (
        !phi1.containsKey(phi) ||
        (
          phi1.get(phi).getVersion() < version.getVersion() &&
          (
            !phi2.containsKey(phi) ||
            version.getVersion() != phi2.get(phi).getVersion()
          )
        )
      )
    ) {
      // if (phi1.containsKey(phi)) {
      //   phi2.put(phi, phi1.get(phi));
      // }
      phi1.put(phi, version);
    } else if (
      phiBlock.get(par) == phi2 &&
      (
        !phi2.containsKey(phi) ||
        (
          phi2.get(phi).getVersion() < version.getVersion() &&
          (
            !phi1.containsKey(phi) ||
            version.getVersion() != phi1.get(phi).getVersion()
          )
        )
      )
    ) {
      phi2.put(phi, version);
    }
  }

  public void addInstruction(Instruction inst) {
    if (instructions.isEmpty()) {
      firstInst = inst;
    }
    instructions.add(inst);
    // Add left Result if it's a symbol that is not a function
    // if (inst.inst == op.ADD) {
    //   System.out.println("STOP");
    // }
    if (inst.inst.equals(op.MOVE)) {
      assigns.add(inst);
      // if inst.right is a variable type, then we can add it as a block var
      if (inst.right.kind == Result.VAR) {
        blockVars.add(inst.right.var.OG);
        inst.right.var.instruction = inst;
      }
    }
    if (inst.inst.equals(op.LOAD)) {
      return;
    }
    if (
      inst.left != null &&
      inst.left.kind == Result.VAR &&
      !inst.left.var.OG.type.getClass().equals(FuncType.class)
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
      !inst.right.var.OG.type.getClass().equals(FuncType.class)
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
    edgeSet.add(block);
    if (block.parents.isEmpty()) {
      block.phiBlock.put(this, block.phi1);
    } else {
      block.phiBlock.put(this, block.phi2);
    }
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
