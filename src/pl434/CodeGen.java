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
  Result gdb = new Result();
  Result frame = new Result();
  Result stack = new Result();
  Result reg0 = new Result();
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
    pc.regno = 31;
    gdb.kind = Result.GDB;
    frame.kind = Result.REG;
    stack.kind = Result.REG;
    reg0.kind = Result.REG;
    global.kind = Result.CONST;
    global.value = ssa.global;
    global_count = ssa.global;
    reg0.regno = 0;
    frame.regno = 28;
    stack.regno = 29;
    this.ssa = ssa;
    ssa.flipAllBreaks();
  }

  public int[] generateCode() {
    Block main = ssa.MAIN;
    //generateFromBlock(main);
    add(DLX.assemble(DLX.ADDI, SP, GLB, ssa.global * -4));
    add(DLX.assemble(DLX.ADD, FP, SP, 0)); // Frame and stack pointer is all set-up

    generateFromBlockDFS(main, new HashSet<>());
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

  public void generateFromBlockDFS(Block root, HashSet<Block> visited) {
    if (visited.contains(root)) {
      return;
    }
    visited.add(root);
    generateInOrder(root);
    for (Block b : root.edges) {
      generateFromBlockDFS(b, visited);
    }
  }

  public void generateInOrder(Block block) {
    blockPC.put(block, inOrder.size() + 2); // +2 because that is the amount of instructions added before hand
    for (Instruction i : block.instructions) {
      // Adding store and load instructiosn required prior
      if (!i.storeThese.isEmpty()) {
        for (Result r : i.storeThese) {
          if (r.addy == -1) {
            r.addy = pointer_address++ * -4;
          }
          inOrder.add(new Instruction(op.STORE, r));
          inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
        }
        inStorage.addAll(i.storeThese);
      }
      // Load Items that must be loaded
      if (i.left != null && inStorage.contains(i.left)) {
        inOrder.add(new Instruction(op.LOAD, i.left));
        inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
        inStorage.remove(i.left);
      }
      if (i.right != null && inStorage.contains(i.right)) {
        inOrder.add(new Instruction(op.LOAD, i.right));
        inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
        inStorage.remove(i.right);
      }
      // Load items in func_call
      if (i.inst == op.CALL) {
        for (Result r : i.func_params) {
          // Load parameter if stored
          if (inStorage.contains(r)) {
            inOrder.add(new Instruction(op.LOAD, r));
            inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
            inStorage.remove(r);
          }
        }
        functionCall(i); // Add all save Reg and Load reg instructions with SP + FP
      }
      if (i.inst != op.PHI && i.inst != op.MOVE) {
        inOrder.add(i);
      }
    }
  }

  public void generateInstructions() {
    for (Instruction i : inOrder) {
      // Store items to free up regs
      switch (i.inst) {
        case STORE:
          if (i.right == stack) {
            add(DLX.assemble(DLX.PSH, i.left.regno, i.right.regno, -4));
          }
          break;
        case LOAD:
          if (i.right == stack) {
            add(DLX.assemble(DLX.POP, i.left.regno, i.right.regno, -4));
          }
          break;
        case BGE:
          add(
            DLX.assemble(
              DLX.BGE,
              i.right.regno,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case BGT:
          add(
            DLX.assemble(
              DLX.BGT,
              i.right.regno,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case BLE:
          add(
            DLX.assemble(
              DLX.BLE,
              i.right.regno,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case BLT:
          add(
            DLX.assemble(
              DLX.BLT,
              i.right.regno,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case BNE:
          add(
            DLX.assemble(
              DLX.BNE,
              i.right.regno,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case BEQ:
          add(
            DLX.assemble(
              DLX.BEQ,
              i.right.regno,
              blockPC.get(i.right.proc) - blockPC.get(i.blockLoc)
            )
          );
          break;
        case CALL:
          add(DLX.assemble(DLX.JSR, blockPC.get(i.right.proc) * 4));
          break;
        case RET:
          add(DLX.assemble(DLX.STW, i.right.regno, FP, 4)); // STW return value into FP + 1
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
        add(DLX.assemble(DLX.XORI, inst.getRegister(), inst.right.regno));
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
        add(
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
        add(
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
      reg.regno = regToSave;
      inOrder.add(new Instruction(op.STORE, reg, stack)); //  Have SP under right to understand this is a push operation
      inOrder.get(inOrder.size() - 1).blockLoc = bloc;
    }
  }

  public void loadRegisters(HashSet<Integer> registersIn, Block bloc) {
    for (int regToSave : registersIn) {
      Result reg = new Result();
      reg.kind = Result.REG;
      reg.regno = regToSave;
      inOrder.add(new Instruction(op.LOAD, reg, stack));
      inOrder.get(inOrder.size() - 1).blockLoc = bloc;
    }
  }

  public void add(int dlxinst) {
    dlx_inst.add(dlxinst);
    count++;
  }
}
