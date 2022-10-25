package ssa;

import java.util.ArrayList;

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
    CMP,
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
  ArrayList<Result> func_params;
  op inst;

  public Instruction(op inst, ArrayList<Result> func_params) {
    this.func_params = func_params;
    this.inst = inst;
    this.left = null;
    this.right = null;
    my_num = Instruction.instruction_num++;
  }

  public Instruction(op inst, Result left, Result right) {
    this.func_params = null;
    this.inst = inst;
    this.left = left;
    this.right = right;
    my_num = Instruction.instruction_num++;
  }

  public Instruction(op inst, Result right) {
    this.func_params = null;
    this.inst = inst;
    this.left = null;
    this.right = right;
    my_num = Instruction.instruction_num++;
  }

  public Instruction(op inst) {
    this.func_params = null;
    this.inst = inst;
    this.left = null;
    this.right = null;
    my_num = Instruction.instruction_num++;
  }

  @Override
  public String toString() {
    if (func_params != null) {
      String call = my_num + ": " + inst.name() + " ";
      for (Result i : func_params) {
        call += i + " ";
      }
    } else if (right == null && left == null) {
      return my_num + ": " + inst.name();
    } else if (left == null) {
      return my_num + ": " + inst.name() + " " + right;
    }
    return my_num + ": " + inst.name() + " " + left + " " + right;
  }
}
