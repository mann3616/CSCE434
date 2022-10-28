package ssa;

import java.util.ArrayList;
import java.util.List;
import pl434.Symbol;

public class Instruction {

  public enum op {
    NEG,
    ADDA,
    ADD,
    SUB,
    MUL,
    MOD,
    POW,
    AND,
    OR,
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
  Result left, right, third, fourth; // TODO: third is for the third Result that needs to be printed out and stuff MAY need a 4th
  List<Symbol> doPhiOn;
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
    doPhiOn = new ArrayList<>();
    this.func_params = null;
    this.inst = inst;
    this.left = null;
    this.right = null;
    my_num = Instruction.instruction_num++;
  }

  @Override
  public String toString() {
    // TODO: change this if you got time, but tbh i doubt the teacher will care
    if (inst.equals(op.PHI)) {
      return (
        my_num +
        " : " +
        inst.name() +
        " " +
        "(" +
        (
          left.var.my_assign < right.var.my_assign
            ? (left.var.my_assign)
            : (right.var.my_assign)
        ) +
        ") " +
        third +
        " := " +
        right +
        " " +
        left
      );
    } else if (func_params != null) {
      String call = my_num + " : " + inst.name() + " ";
      for (Result i : func_params) {
        call += i + " ";
      }
      return call.substring(0, call.length() - 1);
    } else if (right == null && left == null) {
      return my_num + " : " + inst.name();
    } else if (left == null) {
      return my_num + " : " + inst.name() + " " + right;
    }
    return my_num + " : " + inst.name() + " " + left + " " + right;
  }
}
