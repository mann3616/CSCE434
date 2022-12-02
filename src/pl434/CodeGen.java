package pl434;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import ssa.*;
import ssa.Instruction.op;
import types.*;

public class CodeGen {

  SSA ssa;
  Block currBlock;
  ArrayList<Integer> dlx_inst = new ArrayList<>();
  static final int FP = 28;
  static final int SP = 29;
  boolean solveRead = false;
  Result gdb = new Result();
  Result frame = new Result();
  Result stack = new Result();
  Result reg0 = new Result();
  Result reg1 = new Result();
  Result pc = new Result();
  ArrayList<Instruction> inOrder = new ArrayList<>();
  HashMap<Block, Integer> blockPC = new HashMap<>();
  HashSet<Result> inStorage = new HashSet<>();
  RegisterAlloc regAll;
  public static int pointer_address = 1;
  Result global = new Result();
  // Need Memory allocation of addresses

  int global_count;
  int count = 0;
  static final int GLB = 30; // Set to max address of 10000
  static final int PC = 31; // Saves PC

  public CodeGen(SSA ssa) {
    pc.kind = Result.REG;
    pc.inst = new Instruction(op.RET);
    pc.inst.regno = 31;
    gdb.kind = Result.GDB;
    frame.kind = Result.REG;
    stack.kind = Result.REG;
    reg0.kind = Result.REG;
    global.kind = Result.CONST;
    global.value = ssa.global;
    global_count = ssa.global;
    reg0.inst = new Instruction(op.RET);
    reg0.inst.regno = 0;
    reg1.inst = new Instruction(op.RET);
    reg1.inst.regno = 1;
    frame.inst = new Instruction(op.RET);
    frame.inst.regno = 28;
    stack.inst = new Instruction(op.RET);
    stack.inst.regno = 29;
    this.ssa = ssa;
    ssa.flipAllBreaks();
  }

  public int[] generateCode() {
    Block main = ssa.main;
    //generateFromBlock(main);
    add(DLX.assemble(DLX.ADDI, SP, GLB, ssa.global * -4));
    add(DLX.assemble(DLX.ADD, FP, SP, 0)); // Frame and stack pointer is all set-up

    generateFromBlock(main);
    for (Block b : ssa.roots) {
      if (b == main) continue;
      generateFromBlock(b);
    }
    generateInstructions();
    int[] instConvert = new int[dlx_inst.size()];
    int x = 0;
    for (Integer i : dlx_inst) {
      instConvert[x] = i;
      x++;
    }
    return instConvert;
  }

  public void generateFromBlock(Block root) {
    ArrayList<Block> bb = ssa.inOrderBlock(root);
    for (Block b : bb) {
      generateInOrder(b);
    }
  }

  public void generateInOrder(Block block) {
    blockPC.put(block, inOrder.size() + 2); // +2 because that is the amount of instructions added before hand
    for (Instruction i : block.instructions) {
      genIOInstruction(i);
    }
  }

  public void generateInstructions() {
    for (Instruction i : inOrder) {
      // Store items to free up regs
      switch (i.inst) {
        case STORE:
          if (i.right == stack) {
            add(
              DLX.assemble(
                DLX.PSH,
                i.left.inst.regno + 2,
                i.right.inst.regno,
                -4
              )
            );
          }
          break;
        case LOAD:
          if (i.right == stack) {
            add(
              DLX.assemble(
                DLX.POP,
                i.left.inst.regno + 2,
                i.right.inst.regno,
                -4
              )
            );
          }
          break;
        case BGE:
          add(
            DLX.assemble(
              DLX.BGE,
              i.left.inst.regno + 2,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case BGT:
          add(
            DLX.assemble(
              DLX.BGT,
              i.left.inst.regno + 2,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case BLE:
          add(
            DLX.assemble(
              DLX.BLE,
              i.left.inst.regno + 2,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case BLT:
          add(
            DLX.assemble(
              DLX.BLT,
              i.left.inst.regno + 2,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case BNE:
          add(
            DLX.assemble(
              DLX.BNE,
              i.left.inst.regno + 2,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case BEQ:
          add(
            DLX.assemble(
              DLX.BEQ,
              i.left.inst.regno + 2,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case CALL:
          add(DLX.assemble(DLX.JSR, blockPC.get(i.right.proc) * 4));
          break;
        case RET:
          if (i.right.endFunc) {
            add(DLX.assemble(DLX.RET, i.right.value));
          } else {
            add(DLX.assemble(DLX.STW, i.right.inst.regno + 2, FP, 4)); // STW return value into FP + 1
          }
          break;
        case BRA:
          add(
            DLX.assemble(
              DLX.BSR,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        default:
          generateSimpleInstruction(i);
      }
    }
  }

  public int read(Instruction inst) {
    if (inst.readType.getClass().equals(FloatType.class)) {
      return DLX.assemble(DLX.RDF, inst.regno + 2);
    } else if (inst.readType.getClass().equals(IntType.class)) {
      return DLX.assemble(DLX.RDI, inst.regno + 2);
    } else if (inst.readType.getClass().equals(BoolType.class)) {
      return DLX.assemble(DLX.RDB, inst.regno + 2);
    }
    System.out.println("No Type Listed for READ");
    return -1;
  }

  public int write(Instruction inst) {
    if (inst.right == null) {
      return DLX.assemble(DLX.WRL);
    } else if (inst.right.type.getClass().equals(FloatType.class)) {
      if (inst.right.kind == Result.INST) {
        return DLX.assemble(DLX.WRF, inst.right.inst.regno + 2);
      } else if (inst.right.kind == Result.REG) {
        return DLX.assemble(DLX.WRF, inst.right.inst.regno);
      } else if (inst.right.kind == Result.VAR) {
        return DLX.assemble(DLX.WRF, inst.right.var.OG.regno + 2);
      } else {
        return DLX.assemble(DLX.WRF, inst.right.inst.regno + 2);
      }
    } else if (inst.right.type.getClass().equals(IntType.class)) {
      if (inst.right.kind == Result.INST) {
        return DLX.assemble(DLX.WRI, inst.right.inst.regno + 2);
      } else if (inst.right.kind == Result.REG) {
        return DLX.assemble(DLX.WRI, inst.right.inst.regno);
      } else if (inst.right.kind == Result.VAR) {
        return DLX.assemble(DLX.WRI, inst.right.var.OG.regno + 2);
      } else {
        return DLX.assemble(DLX.WRI, inst.right.inst.regno + 2);
      }
    } else if (inst.right.type.getClass().equals(BoolType.class)) {
      if (inst.right.kind == Result.INST) {
        return DLX.assemble(DLX.WRB, inst.right.inst.regno + 2);
      } else if (inst.right.kind == Result.REG) {
        return DLX.assemble(DLX.WRB, inst.right.inst.regno);
      } else if (inst.right.kind == Result.VAR) {
        return DLX.assemble(DLX.WRB, inst.right.var.OG.regno + 2);
      } else {
        return DLX.assemble(DLX.WRB, inst.right.inst.regno + 2);
      }
    }
    System.out.println("No Type Listed for WRITE");
    return -1;
  }

  static final int I = 0;
  static final int ICR = 20;
  static final int ICL = 21;
  static final int F = 7;
  static final int FCR = 27;
  static final int FCL = 28;
  static final int B = 13;
  static final int BCR = 33;
  static final int BCL = 34;

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
    if (conRes.kind == Result.INST) {
      conRes.value = conRes.inst.regno + 2;
    } else if (conRes.kind == Result.VAR) {
      conRes.value = conRes.var.OG.regno + 2;
    } else if (conRes.kind == Result.REG) {
      conRes.value = conRes.inst.regno;
    }

    if (notCon.kind == Result.INST) {
      notCon.value = notCon.inst.regno + 2;
    } else if (notCon.kind == Result.VAR) {
      notCon.value = notCon.var.OG.regno + 2;
    } else if (notCon.kind == Result.REG) {
      notCon.value = notCon.inst.regno;
    }
    // System.out.println(inst + " " + (inst.regno + 2));
    // System.out.println("  " + notCon.value + " this is right");
    // System.out.println("  " + conRes.value + " this is left");

    switch (offset) {
      case FCR:
        offset--;
        Result hold = notCon;
        notCon = conRes;
        conRes = hold;
      case FCL:
      case F:
        add(
          DLX.assemble(
            (op == DLX.CMP ? op - 1 : op) + offset,
            inst.regno + 2,
            conRes.value,
            notCon.value
          )
        );
        break;
      case ICL:
      case BCL:
        offset--;
        Result hold1 = notCon;
        notCon = conRes;
        conRes = hold1;
      case BCR:
      case ICR:
      case I:
      case B:
        add(
          DLX.assemble(op + offset, inst.regno + 2, conRes.value, notCon.value)
        );
        // System.out.println(DLX.instrString(dlx_inst.get(dlx_inst.size() - 1)));
        break;
    }
  }

  public void functionCall(Instruction call) {
    // Saving Registers
    HashSet<Integer> registersIn = new HashSet<>();
    saveRegisters(registersIn, call.blockLoc);

    Symbol funcSymbol = call.func_params.get(call.func_params.size() - 1).var;
    // Add params + Return address because func_params is a list that ends in the function Symbol
    for (Result r : call.func_params) {
      inOrder.add(new Instruction(op.STORE, r, stack));
      inOrder.get(inOrder.size() - 1).blockLoc = call.blockLoc;
    }
    // Save PC from last
    inOrder.add(new Instruction(op.STORE, pc, stack));
    inOrder.get(inOrder.size() - 1).blockLoc = call.blockLoc;
    // Save Frame Pointer
    inOrder.add(new Instruction(op.STORE, frame, stack));
    inOrder.get(inOrder.size() - 1).blockLoc = call.blockLoc;

    // Locals
    Result funcGlobal = new Result();
    funcGlobal.kind = Result.CONST;
    funcGlobal.value = funcSymbol.global_counter * -4;
    inOrder.add(new Instruction(op.ADD, frame, funcGlobal)); // Update FP to nxt frame
    inOrder.get(inOrder.size() - 1).blockLoc = call.blockLoc;
    // Global Symbols + Parameter are FP + addy*4 // Deal with this later lol
    // Local Symbols are FP - addy*4
    // Change his register implementation prolly
    Result skipTo = new Result();
    skipTo.kind = Result.PROC;
    skipTo.proc = funcSymbol.func_block;
    inOrder.add(new Instruction(op.BRA, skipTo)); // Skip to function
    inOrder.get(inOrder.size() - 1).blockLoc = call.blockLoc;

    // Load Return value into return register
    inOrder.add(
      new Instruction(
        op.LOAD,
        call.func_params.get(call.func_params.size() - 1),
        stack
      )
    );
    inOrder.get(inOrder.size() - 1).blockLoc = call.blockLoc;

    inOrder.add(new Instruction(op.LOAD, frame, stack)); // Load frame from stack
    inOrder.get(inOrder.size() - 1).blockLoc = call.blockLoc;
    inOrder.add(new Instruction(op.LOAD, pc, stack)); // Load pc from stack
    inOrder.get(inOrder.size() - 1).blockLoc = call.blockLoc;

    // Loading Registers
    loadRegisters(registersIn, call.blockLoc);
  }

  public void saveRegisters(HashSet<Integer> registersIn, Block bloc) {
    // Need to differentiate STORE push and LOAD pop
    for (int regToSave : registersIn) {
      Result reg = new Result();
      reg.kind = Result.REG;
      reg.inst = new Instruction(op.RET);
      reg.inst.regno = regToSave + 2;
      inOrder.add(new Instruction(op.STORE, reg, stack)); //  Have SP under right to understand this is a push operation
      inOrder.get(inOrder.size() - 1).blockLoc = bloc;
    }
  }

  public void loadRegisters(HashSet<Integer> registersIn, Block bloc) {
    for (int regToSave : registersIn) {
      Result reg = new Result();
      reg.kind = Result.REG;
      reg.inst = new Instruction(op.RET);
      reg.inst.regno = regToSave + 2;

      inOrder.add(new Instruction(op.LOAD, reg, stack));
      inOrder.get(inOrder.size() - 1).blockLoc = bloc;
    }
  }

  public void add(int dlxinst) {
    dlx_inst.add(dlxinst);
    count++;
  }

  public void genIOInstruction(Instruction i) {
    // Adding store and load instructiosn required prior
    if (!i.storeThese.isEmpty()) {
      for (Result r : i.storeThese) {
        inOrder.add(new Instruction(op.STORE, r, stack));
        inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
      }
      inStorage.addAll(i.storeThese);
    }
    // Load Items that must be loaded
    if (i.left != null && inStorage.contains(i.left)) {
      inOrder.add(new Instruction(op.LOAD, i.left, stack));
      inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
      inStorage.remove(i.left);
    }
    if (i.right != null && inStorage.contains(i.right)) {
      inOrder.add(new Instruction(op.LOAD, i.right, stack));
      inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
      inStorage.remove(i.right);
    }
    // Load items in func_call
    Instruction ii = i;
    switch (i.inst) {
      case CALL:
        for (Result r : i.func_params) {
          // Load parameter if stored
          if (inStorage.contains(r)) {
            inOrder.add(new Instruction(op.LOAD, r, stack));
            inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
            inStorage.remove(r);
          }
        }
        functionCall(i); // Add all save Reg and Load reg instructions with SP + FP
        break;
      case WRITE:
        // ii = new Instruction(op.ADD, reg0, i.right);
        // ii.regno = i.right.regno;
        if (i.right.kind == Result.CONST) {
          inOrder.add(new Instruction(op.ADD, reg1, i.right));
          inOrder.get(inOrder.size() - 1).regno = -1;
          Result r = new Result();
          r.kind = Result.INST;
          r.inst = inOrder.get(inOrder.size() - 1);
          i.right = r;
        }
        inOrder.add(i);
      case READ:
        break;
      case MOVE:
        if (ii.left.kind == Result.INST && ii.left.inst.inst == op.READ) {
          ii.left.inst.regno = ii.right.var.OG.regno;
          inOrder.add(ii.left.inst);
        } else {
          ii = new Instruction(op.ADD, reg0, i.left);
          ii.regno = i.right.var.OG.regno;
          inOrder.add(ii);
        }
        break;
      default:
        inOrder.add(ii);
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
        add(DLX.assemble(DLX.XORI, inst.regno, inst.right.inst.regno + 2));
        break;
      case READ:
        add(read(inst));
        break;
      case WRITE:
      case WRITENL:
        add(write(inst));
        break;
    }
  }
}
