package ssa;

import java.util.ArrayList;
import java.util.HashSet;
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
  boolean eliminated = false;
  public int my_num;
  public Result left, right, third; // TODO: third is for the third Result that needs to be printed out
  List<Symbol> doPhiOn;
  ArrayList<Result> func_params;
  public op inst;

  // Used for calculating in and out sets
  public HashSet<String> InSet = new HashSet<String>();
  public HashSet<String> OutSet = new HashSet<String>();

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
    String elimString = "";
    if (eliminated) {
      elimString = "elim";
    }
    if (inst.equals(op.PHI)) {
      return (
        elimString +
        (
          my_num +
          " : " +
          inst.name() +
          " " +
          "(" +
          (
            left.var.instruction == null
              ? "-1"
              : (
                left.var.getVersion() < right.var.getVersion()
                  ? (left.var.getVersion())
                  : (right.var.getVersion())
              )
          ) +
          ") " +
          third +
          " := " +
          right +
          " " +
          left
        )
      );
    } else if (func_params != null) {
      String call = my_num + " : " + inst.name() + " ";
      for (Result i : func_params) {
        call += i + " ";
      }
      return elimString + call.substring(0, call.length() - 1);
    } else if (right == null && left == null) {
      return elimString + my_num + " : " + inst.name();
    } else if (left == null) {
      return elimString + my_num + " : " + inst.name() + " " + right;
    }
    return elimString + my_num + " : " + inst.name() + " " + left + " " + right;
  }
}
