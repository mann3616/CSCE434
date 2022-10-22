package ssa;

public class Instruction {

  public enum op {
    NEG,
    ADD,
    SUB,
    MUL,
    MOD,
    POW,
    AND,
    OR,
    XOR,
    DIV,
    BEQ,
    BNE,
    BLE,
    BLT,
    BGE,
    BGT,
    LOAD,
    STORE,
    MOVE,
    PHI,
    READ,
    WRITE,
    WRITENL,
    CALL,
    RET,
    BRA,
  }

  public static int instruction_num = 0;
  public int my_num;
  Result left, right;
  op inst;

  public Instruction(op inst, Result left, Result right) {
    this.inst = inst;
    this.left = left;
    this.right = right;
    my_num = Instruction.instruction_num++;
  }

  @Override
  public String toString() {
    return my_num + ": " + inst.name() + " " + left + " " + right;
  }
}
