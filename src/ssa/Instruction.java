package ssa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
  public boolean eliminated = false;
  public Instruction usedAt = null;
  public Result refResult = null;
  Block blockLoc = null;
  public int my_num;
  public Result left, right, third; // TODO: third is for the third Result that needs to be printed out
  HashMap<Block, Result> phiBtoR = new HashMap<>();
  List<Symbol> doPhiOn;
  public ArrayList<Result> func_params;
  List<Instruction> availableExpr = new ArrayList<>();
  HashSet<Instruction> equivList = new HashSet<>();
  public HashSet<Result> storeThese = new HashSet<>();
  public HashSet<Result> loadThese = new HashSet<>();
  boolean rootExpr = false;
  //boolean isArrayMul = false;
  boolean mainEquiv = true;
  public op inst;

  public boolean isEliminated() {
    return eliminated;
  }

  // Used for calculating in and out sets
  public HashSet<String> InSet = new HashSet<String>();
  public HashSet<String> OutSet = new HashSet<String>();

  public Result getResult() {
    if (usedAt == null) {
      return null;
    }
    if (usedAt.right.kind == Result.INST && usedAt.right.inst == this) {
      return usedAt.right;
    }
    return usedAt.left;
  }

  public Instruction(op inst, ArrayList<Result> func_params) {
    this.func_params = func_params;
    this.inst = inst;
    this.left = null;
    this.right = null;
    for (Result r : func_params) {
      if (r.kind == Result.INST) {
        r.inst.usedAt = this;
      }
    }
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

  public boolean compare(Instruction c) {
    boolean rightC;
    if (c.right != null && right != null && c.right.kind == right.kind) {
      rightC = (right.kind == Result.CONST && right.value == c.right.value);
      rightC =
        rightC ||
        (
          right.kind == Result.VAR &&
          right.var.getVersion() == c.right.var.getVersion()
        );
      rightC =
        rightC || (right.kind == Result.PROC && right.proc == c.right.proc);
      rightC =
        rightC ||
        (right.kind == Result.INST && right.inst.compare(c.right.inst)); // Compare the two instructions
    } else if (c.right == null && right == null) {
      rightC = true;
    } else {
      rightC = false;
    }
    boolean leftC;
    if (c.left != null && left != null && c.left.kind == left.kind) {
      leftC = (left.kind == Result.CONST && left.value == c.left.value);
      leftC =
        leftC ||
        (
          left.kind == Result.VAR &&
          left.var.getVersion() == c.left.var.getVersion()
        );
      leftC = leftC || (left.kind == Result.PROC && left.proc == c.left.proc);
      leftC =
        leftC || (left.kind == Result.INST && left.inst.compare(c.left.inst));
    } else if (c.left == null && left == null) {
      leftC = true;
    } else {
      leftC = false;
    }
    if (!leftC && !rightC && inst == c.inst && Optimize.isExpr(this)) {
      if (left != null && left.kind == c.right.kind) {
        leftC =
          leftC || (left.kind == Result.CONST && left.value == c.right.value);
        leftC =
          leftC ||
          (
            left.kind == Result.VAR &&
            left.var.getVersion() == c.right.var.getVersion()
          );
        leftC =
          leftC ||
          (left.kind == Result.INST && left.inst.compare(c.right.inst));
      }
      if (c.left != null && right.kind == c.left.kind) {
        rightC =
          rightC || (right.kind == Result.CONST && right.value == c.left.value);
        rightC =
          rightC ||
          (
            right.kind == Result.VAR &&
            right.var.getVersion() == c.left.var.getVersion()
          );
        rightC =
          rightC ||
          (right.kind == Result.INST && right.inst.compare(c.left.inst));
      }
    }
    return c.inst == inst && leftC && rightC;
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
            left.var == null || // For now so that no errors populate
              right.var == null ||
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

  public int getRegister() {
    if (usedAt == null) {
      return -1;
    }
    if (usedAt.right.kind == Result.INST && usedAt.right.inst == this) {
      return usedAt.right.regno;
    }
    return usedAt.left.regno;
  }
}
