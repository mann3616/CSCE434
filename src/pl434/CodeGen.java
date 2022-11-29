package pl434;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import ssa.*;
import types.*;

public class CodeGen {

  SSA ssa;
  Block currBlock;
  ArrayList<Integer> dlx_inst = new ArrayList<>();
  static final int FP = 28;
  static final int SP = 29;
  ArrayList<Instruction> inOrder = new ArrayList<>();
  HashMap<Block, Integer> blockPC = new HashMap<>();
  RegisterAlloc regAll;

  int count = 0;
  static final int GLB = 30;

  public CodeGen(SSA ssa) {
    this.ssa = ssa;
    ssa.flipAllBreaks();
  }

  public int[] generateCode() {
    Block main = ssa.MAIN;
    generateFromBlock(main);
    for (Block b : ssa.roots) {
      if (b == main) continue;
      generateFromBlock(b);
    }
    int[] instConvert = new int[dlx_inst.size()];
    int x = 0;
    for (Integer i : dlx_inst) {
      instConvert[x] = i;
      x++;
    }
    return instConvert;
  }

  public void generateFromBlock(Block root) {
    Stack<Block> bfs = new Stack<>();
    HashSet<Block> visited = new HashSet<>();
    bfs.push(root);
    while (!bfs.isEmpty()) {
      Stack<Block> bfsClone = new Stack<>();
      while (!bfs.isEmpty()) {
        Block block = bfs.pop();
        if (visited.contains(block)) {
          continue;
        }
        visited.add(block);
        for (Block b : block.edges) {
          bfsClone.push(b);
        }
        generateInOrder(block);
      }
      bfs.addAll(bfsClone);
    }
  }

  public void generateInOrder(Block block) {
    blockPC.put(block, inOrder.size());
    for (Instruction i : block.instructions) {
      inOrder.add(i);
    }
  }

  public void generateInstructions() {
    for (Instruction i : inOrder) {
      switch (i.inst) {
        case CALL:
          HashSet<Integer> registersUsed = new HashSet<>();
          functionCall(registersUsed);
          break;
        case STORE:
        case LOAD:
        case PHI:
        // Use this to figure out which block we originated from and use the corresponding register from there
        case MOVE:
          count--;
          break;
        case BGE:
          dlx_inst.add(
            DLX.assemble(DLX.BGE, i.getRegister(), blockPC.get(i.right.proc))
          );
          break;
        case BGT:
          dlx_inst.add(
            DLX.assemble(DLX.BGT, i.getRegister(), blockPC.get(i.right.proc))
          );
          break;
        case BLE:
          dlx_inst.add(
            DLX.assemble(DLX.BLE, i.getRegister(), blockPC.get(i.right.proc))
          );
          break;
        case BLT:
          dlx_inst.add(
            DLX.assemble(DLX.BLT, i.getRegister(), blockPC.get(i.right.proc))
          );
          break;
        case BNE:
          dlx_inst.add(
            DLX.assemble(DLX.BNE, i.getRegister(), blockPC.get(i.right.proc))
          );
          break;
        case BEQ:
          dlx_inst.add(
            DLX.assemble(DLX.BEQ, i.getRegister(), blockPC.get(i.right.proc))
          );
          break;
        case BRA:
          dlx_inst.add(DLX.assemble(DLX.BSR, blockPC.get(i.right.proc)));
          break;
        default:
          generateSimpleInstruction(i);
      }
      count++;
    }
  }

  public void generateSimpleInstruction(Instruction inst) {
    switch (inst.inst) {
      case OR:
        makeDLXAssembly(DLX.OR, inst);
        break;
      case AND:
        makeDLXAssembly(DLX.AND, inst);
        break;
      case ADD:
      case ADDA:
        makeDLXAssembly(DLX.ADD, inst);
        break;
      case CMP:
        makeDLXAssembly(DLX.CMP, inst);
        break;
      case SUB:
        makeDLXAssembly(DLX.SUB, inst);
        break;
      case MUL:
        makeDLXAssembly(DLX.MUL, inst);
        break;
      case DIV:
        makeDLXAssembly(DLX.DIV, inst);
        break;
      case POW:
        makeDLXAssembly(DLX.POW, inst);
        break;
      case MOD:
        makeDLXAssembly(DLX.MOD, inst);
        break;
      case NEG: // Dont need to check for constant because optimization should fold
        dlx_inst.add(
          DLX.assemble(DLX.XORI, inst.getRegister(), inst.right.regno)
        );
        break;
      case READ:
        dlx_inst.add(read(inst));
        break;
      case WRITE:
      case WRITENL:
        dlx_inst.add(write(inst));
        break;
    }
  }

  public int read(Instruction inst) {
    if (inst.right.type.getClass().equals(FloatType.class)) {
      return DLX.assemble(DLX.RDF, inst.getRegister());
    } else if (inst.right.type.getClass().equals(IntType.class)) {
      return DLX.assemble(DLX.RDI, inst.getRegister());
    } else if (inst.right.type.getClass().equals(BoolType.class)) {
      return DLX.assemble(DLX.RDB, inst.getRegister());
    }
    System.out.println("No Type Listed for READ");
    return -1;
  }

  public int write(Instruction inst) {
    if (inst.right == null) {
      return DLX.assemble(DLX.WRL);
    } else if (inst.right.type.getClass().equals(FloatType.class)) {
      return DLX.assemble(DLX.WRF, inst.right.regno);
    } else if (inst.right.type.getClass().equals(IntType.class)) {
      return DLX.assemble(DLX.WRI, inst.right.regno);
    } else if (inst.right.type.getClass().equals(BoolType.class)) {
      return DLX.assemble(DLX.WRB, inst.right.regno);
    }
    System.out.println("No Type Listed for WRITE");
    return -1;
  }

  static final int I = 0;
  static final int ICL = 20;
  static final int ICR = 21;
  static final int F = 7;
  static final int FCL = 27;
  static final int FCR = 28;
  static final int B = 13;
  static final int BCL = 33;
  static final int BCR = 34;

  public int genTypeCTX(Instruction inst) {
    if (
      inst.right.kind == Result.CONST ||
      (inst.left != null && inst.left.kind == Result.CONST)
    ) {
      if (inst.right.type.getClass().equals(FloatType.class)) {
        if (inst.right.kind == Result.CONST) {
          return FCR;
        } else {
          return FCL;
        }
      } else if (inst.right.type.getClass().equals(BoolType.class)) {
        if (inst.right.kind == Result.CONST) {
          return BCR;
        } else {
          return BCL;
        }
      } else if (inst.right.type.getClass().equals(IntType.class)) {
        if (inst.right.kind == Result.CONST) {
          return ICR;
        } else {
          return ICL;
        }
      }
    } else {
      if (inst.right.type.getClass().equals(FloatType.class)) {
        return F;
      } else if (inst.right.type.getClass().equals(BoolType.class)) {
        return B;
      } else if (inst.right.type.getClass().equals(IntType.class)) {
        return I;
      }
    }
    return -1;
  }

  public void makeDLXAssembly(int op, Instruction inst) {
    int offset = genTypeCTX(inst);
    Result conRes = inst.left;
    Result notCon = inst.right;
    switch (offset) {
      case FCR:
        offset--;
        conRes = inst.right;
        notCon = inst.left;
      case FCL:
      case F:
        dlx_inst.add(
          DLX.assemble(
            (op == DLX.CMP ? op - 1 : op) + offset,
            inst.getRegister(),
            conRes.regno,
            notCon.regno
          )
        );
        break;
      case ICR:
      case BCR:
        offset--;
        conRes = inst.right;
        notCon = inst.left;
      case BCL:
      case ICL:
      case I:
      case B:
        dlx_inst.add(
          DLX.assemble(
            op + offset,
            inst.getRegister(),
            notCon.regno,
            conRes.value
          )
        );
        break;
    }
  }

  public void functionCall(HashSet<Integer> registersIn) {
    // Saving Registers
    saveRegisters(registersIn);
    // Add return value slot +
    // Loading Registers
    loadRegisters(registersIn);
  }

  public void saveRegisters(HashSet<Integer> registersIn) {
    for (int regToSave : registersIn) {
      dlx_inst.add(DLX.assemble(DLX.PSH, regToSave, 31, -4));
    }
  }

  public void loadRegisters(HashSet<Integer> registersIn) {
    for (int regToSave : registersIn) {
      dlx_inst.add(DLX.assemble(DLX.PSH, regToSave, 31, 4));
    }
  }
}
