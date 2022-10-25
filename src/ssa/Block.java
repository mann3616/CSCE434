package ssa;

import java.util.ArrayList;
import java.util.List;

public class Block {

  public String label;
  public List<Instruction> instructions;
  public static int block_num = 1;
  public int my_num;
  public List<Block> edges;

  public Block() {
    instructions = new ArrayList<>();
    edges = new ArrayList<>();
    my_num = Block.block_num++;
    label = "";
  }

  public void addInstruction(Instruction inst) {
    instructions.add(inst);
  }

  public void addEdge(Block block) {
    edges.add(block);
  }

  public String printShortLabel() {
    return "BB" + my_num + " " + "[label=\"" + label + "\"" + "];";
  }

  @Override
  public String toString() {
    StringBuffer asString = new StringBuffer();
    asString.append(
      "BB" +
      my_num +
      "[shape=record, label=\"<b>" +
      (label.equals("then") || label.equals("else") ? "" : label) +
      "\\nBB" +
      my_num +
      "|{"
    );
    for (Instruction i : instructions) {
      asString.append(i + " |");
    }
    asString.delete(asString.length() - 2, asString.length());
    asString.append("}\"];");
    for (Block b : edges) {
      if (!b.instructions.isEmpty()) {
        asString.append("\nBB" + my_num + " -> " + b.printShortLabel());
      }
    }
    return asString.toString();
  }
}
