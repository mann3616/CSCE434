package ssa;

import java.util.ArrayList;
import java.util.List;

public class Block {

  public String label;
  public List<Instruction> instructions;
  public static int block_num = 1;
  public int my_num;
  public List<Block> edges;
  boolean hasBreak;
  public List<String> edgeLabels;

  public Block() {
    instructions = new ArrayList<>();
    edges = new ArrayList<>();
    edgeLabels = new ArrayList<>();
    my_num = Block.block_num++;
    hasBreak = false;
    label = "";
  }

  public void addInstruction(Instruction inst) {
    instructions.add(inst);
  }

  public void addEdge(Block block, String edgeLabel) {
    edges.add(block);
    edgeLabels.add(edgeLabel);
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
      if (!b.instructions.isEmpty()) {
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
      }
      i++;
    }
    return asString.toString();
  }
}
