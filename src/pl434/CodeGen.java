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
  HashSet<Symbol> inStorage = new HashSet<>();
  HashSet<Instruction> inStorageIns = new HashSet<>();
  RegisterAlloc regAll;
  public int distanceFromGlobal = 0;
  HashMap<String, Boolean> inUse = new HashMap<>();
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

  boolean inFunc = false;

  public int[] generateCode() {
    Block main = ssa.main;
    for (Block b : ssa.inOrderBlock(main)) {
      for (Instruction i : b.instructions) {
        for (String j : i.OutSet) {
          inUse.put(j, false); // False since none are in use at the moment
        }
      }
    }
    //generateFromBlock(main);
    add(DLX.assemble(DLX.ADDI, SP, GLB, ssa.global * -4)); // Skip return to normalize all STW and LDW
    add(DLX.assemble(DLX.ADD, FP, SP, 0)); // Frame and stack pointer is all set-up

    ssa.setAddresses(main);
    generateFromBlock(main);
    inFunc = true;
    for (Block b : ssa.roots) {
      if (b == main) continue;
      ssa.setAddresses(b);
      generateFromBlock(b);
    }
    generateInstructions();
    // System.out.println("We have " + dlx_inst.size() + " instructions");
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
    blockPC.put(block, inOrder.size()); // +2 because that is the amount of instructions added before hand
    if (block != ssa.main) {
      inOrder.add(new Instruction(op.STORE, pc, stack));
      inOrder.get(inOrder.size() - 1).addy = -1; // save return address
    }
    for (Instruction i : block.instructions) {
      genIOInstruction(i);
    }
  }

  public void generateInstructions() {
    int index = 0;
    for (Instruction i : inOrder) {
      // Store items to free up regs
      // System.out.println(dlx_inst.size());
      if (i.func_params != null) {
        for (Result r : i.func_params) {
          switch (r.kind) {
            case Result.INST:
              r.value = r.inst.regno + 2;
              break;
            case Result.VAR:
              r.value = r.var.OG.regno + 2;
              break;
            case Result.REG:
              r.value = r.inst.regno;
              break;
            case Result.PROC:
              r.value = blockPC.get(r.proc);
              break;
          }
        }
      }
      if (i.left != null) {
        switch (i.left.kind) {
          case Result.INST:
            i.left.value = i.left.inst.regno + 2;
            break;
          case Result.VAR:
            if (i.inst == op.STORE || i.inst == op.LOAD) {
              i.addy = i.left.var.OG.address;
            }
            i.left.value = i.left.var.OG.regno + 2;
            break;
          case Result.REG:
            i.left.value = i.left.inst.regno;
            break;
          case Result.PROC:
            i.left.value = blockPC.get(i.left.proc);
            break;
        }
      }
      if (i.right != null) {
        switch (i.right.kind) {
          case Result.INST:
            i.right.value = i.right.inst.regno + 2;
            break;
          case Result.VAR:
            i.right.value = i.right.var.OG.regno + 2;
            break;
          case Result.REG:
            i.right.value = i.right.inst.regno;
            break;
          case Result.PROC:
            i.right.value = blockPC.get(i.right.proc);
            break;
        }
      }
      switch (i.inst) {
        case STORE:
          if (i.regno == -1) {
            if (i.right == stack) {
              add(
                DLX.assemble(DLX.PSH, i.left.value, i.right.value, i.addy * 4)
              );
            } else if (i.right == frame) {
              add(
                DLX.assemble(DLX.STW, i.left.value, i.right.value, i.addy * 4)
              );
            } else {
              add(
                DLX.assemble(DLX.STW, i.left.value, i.right.value, i.addy * 4)
              );
            } // Maybe instead push params + variables AFTER frame so that I go down instead to my own territory and for globals I go down from 9999
          } else {
            add(DLX.assemble(DLX.STX, i.regno, i.right.value, i.left.value));
          }
          break;
        case LOAD:
          if (i.regno == -1) {
            if (i.right == stack) {
              add(
                DLX.assemble(DLX.POP, i.left.value, i.right.value, i.addy * 4)
              );
            } else if (i.right == frame) {
              add(
                DLX.assemble(DLX.LDW, i.left.value, i.right.value, i.addy * 4)
              );
            } else {
              add(
                DLX.assemble(DLX.LDW, i.left.value, i.right.value, i.addy * 4)
              );
            } // Maybe instead push params + variables AFTER frame so that I go down instead to my own territory and for globals I go down from 9999
          } else {
            add(DLX.assemble(DLX.LDX, i.left.value, i.right.value, i.regno));
          }
          break;
        case BGE:
          add(DLX.assemble(DLX.BGE, i.left.value, i.right.value - index));
          break;
        case BGT:
          add(DLX.assemble(DLX.BGT, i.left.value, i.right.value - index));
          break;
        case BLE:
          add(DLX.assemble(DLX.BLE, i.left.value, i.right.value - index));
          break;
        case BLT:
          add(
            DLX.assemble(
              DLX.BLT,
              i.left.value,
              i.right.value - index // PHI instruction isnt being used
            )
          );
          break;
        case BNE:
          add(DLX.assemble(DLX.BNE, i.left.value, i.right.value - index));
          break;
        case BEQ:
          add(DLX.assemble(DLX.BEQ, i.left.value, i.right.value - index));
          break;
        case CALL:
          add(DLX.assemble(DLX.JSR, i.right.value * 4));
          break;
        case RET:
          add(DLX.assemble(DLX.RET, i.right.value));
          break;
        case BRA:
          add(DLX.assemble(DLX.BSR, i.right.value - index));
          break;
        default:
          generateSimpleInstruction(i);
      }
      index++;
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
  static final int BCR = 20;
  static final int BCL = 21;

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
        offset--;
        Result hold1 = notCon;
        notCon = conRes;
        conRes = hold1;
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

    Result func = call.func_params.get(call.func_params.size() - 1);
    // At return set stack pointer at value of frame pointer so we dont have to pop
    // Design
    // save Registers
    ArrayList<Integer> registersIn = Compiler.iMap.get(call);
    saveRegisters(registersIn, call.blockLoc);
    Symbol funcSymbol = func.var;
    int count = 1; // Give params addresses
    for (Symbol sim : funcSymbol.params) {
      sim.address = count++;
    }
    // Skip allocated memory
    // Save frame pointer // Point here as Frame Pointer tho
    inOrder.add(new Instruction(op.STORE, frame, stack));
    inOrder.get(inOrder.size() - 1).addy = funcSymbol.global_counter * -1;
    // Point here as frame pointer
    inOrder.add(new Instruction(op.ADD, stack, reg0)); // Set frame pointer to the location of stack
    inOrder.get(inOrder.size() - 1).regno = FP - 2; // + 2 later

    // Save param space // Push these
    for (int i = call.func_params.size() - 2; i >= 0; i--) { // Use stack instead to find
      Result r = call.func_params.get(i);
      inOrder.add(new Instruction(op.STORE, r, stack)); // Put params in stack
      inOrder.get(inOrder.size() - 1).blockLoc = call.blockLoc;
      inOrder.get(inOrder.size() - 1).addy = -1; // Will be fine until ARRAYS
    }
    // Stack Pointer points HERE // And now we are here

    Result skipTo = new Result();
    skipTo.kind = Result.PROC;
    skipTo.proc = funcSymbol.func_block;
    inOrder.add(new Instruction(op.BRA, skipTo)); // Skip to function
    inOrder.get(inOrder.size() - 1).blockLoc = call.blockLoc; // Once at call push 30 to stack

    // Pop all by using old frame Pointer
    inOrder.add(new Instruction(op.LOAD, frame, frame)); // Load old frame
    inOrder.get(inOrder.size() - 1).addy = 0; //
    inOrder.add(new Instruction(op.ADD, frame, reg0)); // Put stack back to frame
    inOrder.get(inOrder.size() - 1).regno = SP - 2; //

    // Loading Registers
    loadRegisters(registersIn, call.blockLoc);
  }

  public void saveRegisters(ArrayList<Integer> registersIn, Block bloc) {
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

  public void loadRegisters(ArrayList<Integer> registersIn, Block bloc) {
    for (int i = registersIn.size() - 1; i >= 0; i--) {
      int regToSave = registersIn.get(i);
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
    Instruction ii = i;
    {
      if (!i.storeThese.isEmpty()) {
        for (Result r : i.storeThese) {
          inOrder.add(new Instruction(op.STORE, r, frame));
          if (r.kind == Result.VAR) {
            inStorage.add(r.var.OG);
          } else {
            inStorageIns.add(r.inst);
          }
        }
      }
      // Load Items that must be loaded
      if (
        i.left != null &&
        i.left.isVariable() &&
        inStorage.contains(i.left.var.OG)
      ) {
        // Do something to load correctly
        inOrder.add(new Instruction(op.LOAD, i.left, frame)); // Go up from frame
        inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
        inStorage.remove(i.left.var.OG);
      } else if (
        i.left != null &&
        i.left.isInstruction() &&
        inStorageIns.contains(i.left.inst)
      ) {
        inOrder.add(new Instruction(op.LOAD, i.left, frame)); // Go up from frame
        inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
        inStorageIns.remove(i.right.inst);
      }
      if (
        i.right != null &&
        i.right.isVariable() &&
        inStorage.contains(i.right.var.OG)
      ) {
        inOrder.add(new Instruction(op.LOAD, i.right, frame));
        inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
        inStorage.remove(i.right.var.OG);
      } else if (
        i.right != null &&
        i.right.isInstruction() &&
        inStorageIns.contains(i.right.inst)
      ) {
        inOrder.add(new Instruction(op.LOAD, i.right, frame)); // Go up from frame
        inOrder.get(inOrder.size() - 1).blockLoc = i.blockLoc;
        inStorageIns.remove(i.right.inst);
      }
      // Load items in func_call
      if (
        i.left != null &&
        i.left.kind == Result.INST &&
        i.left.inst.inst == op.READ &&
        i.inst != op.MOVE
      ) {
        inOrder.add(i.left.inst); // add read
      }

      if (
        i.right != null &&
        i.right.kind == Result.INST &&
        i.right.inst.inst == op.READ &&
        i.inst != op.MOVE
      ) {
        inOrder.add(i.right.inst); // add read
      }

      if (i.func_params != null && i.inst != op.MOVE) {
        for (Result r : i.func_params) {
          if (r.kind == Result.INST && r.inst.inst == op.READ) {
            inOrder.add(r.inst);
          }
        }
      }
    }

    switch (i.inst) {
      case POW:
      case DIV:
      case SUB:
        if (i.left.kind == Result.CONST) {
          inOrder.add(new Instruction(op.ADD, reg0, i.left));
          inOrder.get(inOrder.size() - 1).regno = -1; // Place into 1
          i.left.inst = inOrder.get(inOrder.size() - 1);
          i.left.kind = Result.INST;
        }
        inOrder.add(i);
        break;
      case PHI:
        break;
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
        } else if (
          ii.left.kind == Result.INST && ii.left.inst.inst == op.CALL
        ) {
          reg1.kind = Result.REG;
          reg1.inst.regno = 1;
          ii = new Instruction(op.ADD, reg0, reg1);
          ii.regno = i.right.var.OG.regno;
          inOrder.add(ii);
        } else {
          ii = new Instruction(op.ADD, reg0, i.left);
          ii.regno = i.right.var.OG.regno;
          inOrder.add(ii);
        }
        break;
      case NEG:
        if (i.right.kind == Result.CONST) {
          inOrder.add(new Instruction(op.ADD, reg1, i.right));
          inOrder.get(inOrder.size() - 1).regno = -1;
          Result r = new Result();
          r.kind = Result.INST;
          r.inst = inOrder.get(inOrder.size() - 1);
          i.right = r;
        }
        inOrder.add(i);
        break;
      case RET:
        if (i.right != null && !i.right.endFunc) { // Store from right to R29 + 4 (going up)
          inOrder.add(new Instruction(op.ADD, i.right, reg0));
          inOrder.get(inOrder.size() - 1).regno = -1; // Stores to R1
        }
        if (inFunc) {
          inOrder.add(new Instruction(op.LOAD, pc, frame)); // Retrieves Return address into pc
          inOrder.get(inOrder.size() - 1).addy = -1; // 1 below frame
          inOrder.add(new Instruction(op.RET, pc)); // Return using pc
          inOrder.get(inOrder.size() - 1).right.endFunc = true;
        } else {
          inOrder.add(new Instruction(op.RET, reg0)); // Return using pc
          inOrder.get(inOrder.size() - 1).right.endFunc = true;
        }
        break;
      default:
        inOrder.add(ii);
    }
  }

  public void generateSimpleInstruction(Instruction inst) {
    switch (inst.inst) {
      case PHI:
        break;
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
        if (inst.right.kind == Result.VAR) {
          add(
            DLX.assemble(
              DLX.XORI,
              inst.regno + 2,
              inst.right.var.OG.regno + 2,
              1
            )
          );
        } else {
          add(
            DLX.assemble(DLX.XORI, inst.regno + 2, inst.right.inst.regno + 2, 1)
          );
        }
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
