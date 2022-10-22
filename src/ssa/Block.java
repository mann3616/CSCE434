package ssa;

import java.util.ArrayList;
import java.util.List;

public class Block {

  public String label;
  public List<Instruction> instructions;
  public static int block_num = 1;
  public int my_num;

  public Block() {
    instructions = new ArrayList<>();
    my_num = Block.block_num++;
  }

  public void addInstruction(Instruction inst) {
    instructions.add(inst);
  }

  @Override
  public String toString() {
    StringBuffer asString = new StringBuffer();
    for (Instruction i : instructions) {
      asString.append(i + "\n");
    }
    return asString.toString();
  }
}
