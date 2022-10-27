package ssa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

public class Block {

  public String label;
  public List<Instruction> instructions;
  public static int block_num = 1;
  public int my_num;
  public List<Block> parents;
  public List<Block> edges;
  public HashSet<Block> edgeSet;
  Block iDom;
  public LinkedHashSet<Block> doms;
  boolean hasBreak;
  public List<String> edgeLabels;

  public Block() {
    iDom = null;
    parents = new ArrayList<>();
    instructions = new ArrayList<>();
    edges = new ArrayList<>();
    edgeLabels = new ArrayList<>();
    doms = new LinkedHashSet<>();
    my_num = Block.block_num++;
    hasBreak = false;
    label = "";
    doms.add(this);
  }

  public void addInstruction(Instruction inst) {
    instructions.add(inst);
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
