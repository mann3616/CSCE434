package ssa;

import ast.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.function.Predicate;
import pl434.DLX;
import pl434.Symbol;
import ssa.Instruction.op;
import types.*;

// TODO: Make Two Pass throughs in the CFG
// The first re organizes elements of the CFG (empty blocks that are meant to be connected etc.)
// The second will find merging points of a CFG and use PHI to merge same vars (see lecture slide 08-SSA.pdf)
public class SSA implements NodeVisitor {

  // To Calculate PHI
  Block currBlock;
  Result currRes;
  HashMap<Integer, Block> hmblocks = new HashMap<>();
  List<Block> blocks;
  public List<Block> roots;
  public Block main;
  HashMap<Symbol, Symbol> allSymbols = new HashMap<>();
  ArrayList<Result> params;
  Stack<Result> indices;
  boolean assign;
  int currDim;
  Optimize opt;
  public int global = 1;

  public ArrayList<Instruction> allInstructions = new ArrayList<>();

  // TODO: test012, test014
  public SSA() {
    currBlock = new Block(this);
    currDim = -1;
    this.assign = false;
    params = new ArrayList<>();
    blocks = new ArrayList<>();
    roots = new ArrayList<>();
    indices = new Stack<>();
    opt = new Optimize(this);
  }

  public void buildPhi() {
    for (Block b : roots) {
      findInnerByDomFront(new HashSet<>(), b);
    }
  }

  public void buildPhiEasy() {
    for (Block b : roots) {
      ArrayList<Block> bb = inOrderBlock(b);
      for (Block bbb : bb) {
        bbb.findPhiVars();
        bbb.createPhiInst();
      }
    }
  }

  public void findInnerByDomFront(HashSet<Block> visited, Block curr) {
    if (visited.contains(curr)) {
      return;
    }
    visited.add(curr);
    for (Block b : curr.edges) {
      if (!curr.domFront.contains(b)) {
        findInnerByDomFront(visited, b);
      }
    }
    //System.out.println(curr.my_num);
    curr.findPhiVars();
    curr.createPhiInst();
    for (Block b : curr.edges) {
      if (curr.domFront.contains(b)) {
        findInnerByDomFront(visited, b);
      }
    }
  }

  @Override
  public void visit(StatementSequence node) {
    for (Statement s : node) {
      s.accept(this);
    }
  }

  @Override
  public void visit(FunctionBody node) {
    node.declaration().accept(this);
    node.sequence().accept(this);
  }

  @Override
  public void visit(VariableDeclaration node) {
    node.symbol.address = global;
    if (node.symbol.type.getClass().equals(ArrayType.class)) {
      int mul = 1;
      for (int i : ((ArrayType) node.symbol.type).dimVals) {
        mul *= i;
      }
      global += mul;
    } else {
      global++;
    }
  }

  @Override
  public void visit(FunctionDeclaration node) {
    currBlock.label = node.function.name;
    roots.add(currBlock);
    currBlock.function = node.function;
    currBlock.function.func_block = currBlock;
    int hold = global;
    global = 1;
    node.body().accept(this);
    node.function.global_counter = global;
    global = hold;
    // Result end = new Result();
    // end.kind = Result.CONST;
    // end.value = 0;
    // end.endFunc = true;
    // end.storeResult();
    //addInstruction(new Instruction(op.RET, null, end));
    addCurr();
  }

  @Override
  public void visit(DeclarationList node) {
    for (Declaration d : node) {
      d.accept(this);
    }
  }

  @Override
  public void visit(Computation node) {
    // Make node.main() not annoying
    node.variables().accept(this);
    node.functions().accept(this);
    currBlock.label = "main";
    main = currBlock;
    roots.add(currBlock);
    node.mainStatementSequence().accept(this);
    Result end = new Result();
    end.kind = Result.CONST;
    end.value = 0;
    end.endFunc = true;
    end.storeResult();
    addInstruction(new Instruction(op.RET, null, end));
    addCurr();
    //removeEmpties();
    DominatorTree tree = new DominatorTree(this);
    // Do we add the phi instructions now?
    //System.out.println(asDotGraph());
    Comparator c = new Comparator<Block>() {
      @Override
      public int compare(Block o1, Block o2) {
        // TODO Auto-generated method stub
        if (o1.firstInst == null || o2.firstInst == null) {
          return 0;
        }
        return o1.firstInst.my_num - o2.firstInst.my_num;
      }
    };
    for (Block b : blocks) {
      b.edges.sort(c);
      b.parents.sort(c);
    }
    for (Block b : roots) {
      tree.buildTree(b);
      System.out.println();
      tree.iterPhi(new HashSet<>(), b);
      tree.iterPhi(new HashSet<>(), b);
      tree.iterPhi(new HashSet<>(), b);
      tree.iterPhi(new HashSet<>(), b);
      tree.iterPhi(new HashSet<>(), b);
      tree.iterPhi(new HashSet<>(), b);
      tree.iterPhi(new HashSet<>(), b);
      tree.iterPhi(new HashSet<>(), b);
      tree.iterPhi(new HashSet<>(), b);
      tree.iterPhi(new HashSet<>(), b);
    }
    setDefaultLatest();
    // buildPhi();
    buildPhiEasy();
    for (Block b : blocks) {
      b.visited.clear();
      for (Instruction i : b.instructions) {
        i.blockLoc = b;
      }
    }
    // //opt.subexpr_elim();
    // HashSet<Block> st = new HashSet<>();
    // for (Block b : blocks) {
    //   if (b.isJoinNode) {
    //     for (Block bb : b.parents) {
    //       if (bb.isJoinNode) {
    //         st.add(b);
    //       }
    //     }
    //   }
    //   if (!st.contains(b)) {
    //     b.findPhiVars();
    //     b.createPhiInst();
    //   }
    //   // Maybe run a renumber here?
    // }
    // for (Block b : st) {
    //   b.findPhiVars();
    //   b.createPhiInst();
    // }
  }

  @Override
  public void visit(RepeatStatement node) {
    Block oldBlock = currBlock;
    // Connecting previous block to repeatBlock unless it is empty
    Block begin = currBlock; // Just initalizing
    if (!currBlock.instructions.isEmpty()) {
      addCurr();
      begin = currBlock;
      oldBlock.addEdge(begin, "");
      begin.isJoinNode = true;
    }
    node.sequence().accept(this);
    Block endSeq = currBlock;
    if (endSeq.hasBreak) {
      addCurr();
      endSeq.addEdge(currBlock, "");
    }
    // Have to save this currBlock to keep Block numbers aligned
    node.relation().accept(this);
    Block relBlock = currBlock;
    Result relRes = currRes;

    // Connecting relation to the start of the repeatStat
    addCurr(); //Wiping Curr
    if (node.relation().getClass().equals(Relation.class)) {
      addRelInstJump(
        relBlock,
        ((Relation) node.relation()).rel(),
        relRes,
        begin,
        "else"
      );
    } else {
      addRelInstJump(relBlock, "==", relRes, begin, "else");
    }
    //Now connect relationBlock to nextCurrBlock
    relBlock.addEdge(currBlock, "then");
  }

  @Override
  public void visit(WhileStatement node) {
    //Save prev block to add relBlock as an edge
    Block oldBlock = currBlock;

    // Do relation and it will be added to the currBlock
    // This will have the PHI ops as well TODO: Implement PHI
    if (!oldBlock.instructions.isEmpty()) {
      addCurr();
      oldBlock.addEdge(currBlock, "");
    }
    node.relation().accept(this);
    currBlock.isJoinNode = true;
    Result relRes = currRes;
    // Now focus on connecting relationBlock to everything
    oldBlock = currBlock;

    //Save relationBlock reference to the list and start on next Block (while sequence)
    addCurr();

    // Adding Instruction jump to the relation block
    if (node.relation().getClass().equals(Relation.class)) {
      addRelInstJump(
        oldBlock,
        ((Relation) node.relation()).rel(),
        relRes,
        currBlock,
        "then"
      );
    } else {
      addRelInstJump(oldBlock, "==", relRes, currBlock, "then");
    }

    node.sequence().accept(this);
    // After doing the sequence now we can add BRA instruction to the end of the last block added by this sequence
    addRelInstJump(currBlock, "", null, oldBlock, "");
    addCurr();
    oldBlock.addEdge(currBlock, "else");
  }

  @Override
  public void visit(ReturnStatement node) {
    if (node.relation() != null) {
      node.relation().accept(this);
      addInstruction(new Instruction(op.RET, null, currRes));
    } else {
      addInstruction(new Instruction(op.RET, null, null));
    }
  }

  @Override
  public void visit(IfStatement node) {
    node.relation().accept(this);
    Result relRes = currRes;
    Block oldBlock = currBlock;
    // Save old block to relate this Block to the ifStat "then" block
    addCurr();

    // Connecting relation to the first block that needs to be run
    if (node.relation().getClass().equals(Relation.class)) {
      addRelInstJump(
        oldBlock,
        ((Relation) node.relation()).rel(),
        relRes,
        currBlock,
        "then"
      );
    } else {
      addRelInstJump(oldBlock, "==", relRes, currBlock, "then");
    }
    node.ifSequence().accept(this);

    // Ending last ifSequence block
    Block lastThen = currBlock;
    if (!lastThen.instructions.isEmpty() || node.elseSequence() != null) {
      addCurr();
    }

    //Adding else block (even if it is not an else) to jump statement for relation() and adds edge
    oldBlock.addEdge(currBlock, "else");

    if (node.elseSequence() != null) {
      Instruction kk = new Instruction(op.BRA);
      lastThen.addInstruction(kk);
      node.elseSequence().accept(this);
      //Add reference to the currBlock so that these blocks can be related to after the if/else statement
      Block lastElseBlock = currBlock;
      addCurr();
      lastElseBlock.addEdge(currBlock, "");
      Result rn = new Result();
      rn.proc = currBlock;
      rn.kind = Result.PROC;
      kk.right = rn;
    }
    if (lastThen != currBlock) {
      lastThen.addEdge(currBlock, "");
    }
    currBlock.isJoinNode = true;
    currBlock.endIfNode = true;
  }

  @Override
  public void visit(Assignment node) {
    node.right().accept(this);
    Result right = currRes;
    this.assign = true;
    node.addressOf().accept(this);
    this.assign = false;
    if (node.addressOf().getClass().equals(ArrayIndex.class)) {
      addInstruction(new Instruction(op.STORE, right, currRes));
    } else {
      Result res = currRes;
      Instruction i = new Instruction(op.MOVE, right, currRes);
      addInstruction(i);
      currBlock.latest.put(res.var.OG, res.var);
      res.var.OG.instruction = i;
      if (!allSymbols.containsKey(res.var.OG)) {
        Symbol def = new Symbol(res.var.OG, true);
        def.my_assign = -1;
        allSymbols.put(res.var.OG, def);
      }
    }
  }

  @Override
  public void visit(AddressOf node) {
    currRes = new Result();
    currRes.kind = Result.VAR;
    currRes.var = new Symbol(node.symbol(), this.assign);
    currRes.storeResult();
  }

  @Override
  public void visit(Dereference node) {
    this.assign = false;
    node.expression.accept(this);
    if (node.expression.getClass().equals(ArrayIndex.class)) {
      addInstruction(new Instruction(op.LOAD, currRes));
    } else {
      currRes.type = currRes.var.type;
    }
  }

  @Override
  public void visit(BoolLiteral node) {
    currRes = new Result();
    currRes.kind = Result.CONST;
    switch (node.literal()) {
      case "true":
        currRes.value = 1;
        break;
      case "false":
        currRes.value = 0;
        break;
    }
    currRes.type = new BoolType();
    currRes.storeResult();
  }

  @Override
  public void visit(IntegerLiteral node) {
    currRes = new Result();
    currRes.kind = Result.CONST;
    currRes.value = Integer.parseInt(node.literal());
    currRes.type = new IntType();
    currRes.storeResult();
  }

  @Override
  public void visit(FloatLiteral node) {
    currRes = new Result();
    //currRes.value = DLX.fromFP32ToFP16(Float.parseFloat(node.literal()));
    currRes.kind = Result.CONST;
    currRes.type = new FloatType();
    currRes.storeResult();
    currRes.fvalue = Float.parseFloat(node.literal());
  }

  @Override
  public void visit(LogicalNot node) {
    node.right().accept(this);
    addInstruction(new Instruction(op.NEG, currRes));
  }

  @Override
  public void visit(Power node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    addInstruction(new Instruction(op.POW, left, currRes));
  }

  @Override
  public void visit(Multiplication node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    addInstruction(new Instruction(op.MUL, left, currRes));
  }

  @Override
  public void visit(Division node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    addInstruction(new Instruction(op.DIV, left, currRes));
  }

  @Override
  public void visit(Modulo node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    addInstruction(new Instruction(op.MOD, left, currRes));
  }

  @Override
  public void visit(LogicalAnd node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    addInstruction(new Instruction(op.AND, left, currRes));
  }

  @Override
  public void visit(Addition node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    addInstruction(new Instruction(op.ADD, left, currRes));
  }

  @Override
  public void visit(Subtraction node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    addInstruction(new Instruction(op.SUB, left, currRes));
  }

  @Override
  public void visit(LogicalOr node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    addInstruction(new Instruction(op.OR, left, currRes));
  }

  @Override
  public void visit(Relation node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    addInstruction(new Instruction(op.CMP, left, currRes));
  }

  @Override
  public void visit(ArgumentList node) {
    for (Expression expr : node) {
      expr.accept(this);
      params.add(currRes);
    }
  }

  @Override
  public void visit(FunctionCall node) {
    //Save argument List just in case FunctionCall is has nested CALL's
    ArrayList<Result> savedArgList = params;
    params = new ArrayList<>();
    node.list().accept(this);
    // If it's a built in function then I use special commands WRITE, WRITENL, and READ
    if (node.function.builtinFunc) {
      int i = 3;
      Type f = null;
      switch (node.function.name) {
        case "printInt":
        case "printFloat":
        case "printBool":
          addInstruction(new Instruction(op.WRITE, params.get(0)));
          break;
        case "println":
          addInstruction(new Instruction(op.WRITENL));
          break;
        case "readInt":
          i--;
        case "readFloat":
          i--;
        case "readBool":
          i--;
          addInstruction(new Instruction(op.READ));
          break;
      }
      switch (i) {
        case 1:
          f = new FloatType();
          break;
        case 2:
          f = new BoolType();
          break;
        case 0:
          f = new IntType();
          break;
      }
      if (i != 3) {
        currBlock.instructions.get(currBlock.instructions.size() - 1).readType =
          f;
      }
      return;
    }
    // Way call instruction print works is that it will print params list first and then lastly print the function name
    Result this_func = new Result();
    this_func.kind = Result.VAR;
    this_func.var = node.function;
    this_func.storeResult();
    this_func.var.regno = 1;
    params.add(this_func);
    addInstruction(new Instruction(op.CALL, params));
    // Set argumentList to previous args
    params = savedArgList;
  }

  // TODO: Use Stack var "indicies" to compute the address of the array
  @Override
  public void visit(ArrayIndex node) {
    boolean nested = false;
    Result resOfPrev = null;
    if (currDim != -1) {
      nested = true;
      resOfPrev = currRes;
    }
    currDim++;
    // Get Result of right to add to prev Mult
    int saveDim = currDim; // Saving currDim just in case node.right is another ArrayIndex
    currDim = -1;
    node.right.accept(this);
    currDim = saveDim;
    Result rightRes = currRes;
    // If this is not the first ArrayIndex then add previous Result to node.right result
    if (nested) {
      addInstruction(new Instruction(op.ADD, resOfPrev, rightRes));
    }

    // If ArrayIndex is not the last, then MUL nextDimiension by node.right result
    if (node.left.getClass().equals(ArrayIndex.class)) {
      Result nxtMaxDim = new Result();
      nxtMaxDim.kind = Result.CONST;
      nxtMaxDim.value =
        ((ArrayType) node.symbol.type).dimVals().get(currDim + 1);
      addInstruction(new Instruction(op.MUL, currRes, nxtMaxDim));
      nxtMaxDim.storeResult();
    } else {
      // MUL the addy by 4 to fit PC DLX format
      Result nxtMaxDim = new Result();
      nxtMaxDim.kind = Result.CONST;
      nxtMaxDim.value = 4;
      nxtMaxDim.storeResult();
      addInstruction(new Instruction(op.MUL, currRes, nxtMaxDim));
      // currBlock.instructions.get(currBlock.instructions.size() - 1).isArrayMul =
      //   true;
      Result mResult = currRes;

      // Get left result (should be a variable) and set it as kind addy since we are getting the address of this result
      node.left.accept(this);
      currRes.kind = Result.ADDY;
      Result leftRes = currRes;

      // Make the GDB Result to have x: ADD GDB var.name
      Result gdb = new Result();
      gdb.kind = Result.GDB;
      addInstruction(new Instruction(op.ADD, gdb, leftRes));
      gdb.storeResult();

      // Doing ADDA instruction using saved mResult and GDB inst
      addInstruction(new Instruction(op.ADDA, currRes, mResult));

      currDim--;
      return;
    }

    node.left.accept(this);

    currDim--;
  }

  public void addInstruction(Instruction inst) {
    currRes = new Result();
    currRes.kind = Result.INST;
    currRes.inst = inst;
    if (currRes.inst.right != null) {
      currRes.type = currRes.inst.right.type;
    }
    currBlock.addInstruction(inst);
    currRes.storeResult();
    // We're now saving all instructions in order ,,, I think
    allInstructions.add(inst);
  }

  public void addBlock(Block b) {
    blocks.add(b);
  }

  public void addCurr() {
    if (!hmblocks.containsKey(currBlock.my_num)) {
      blocks.add(currBlock);
      hmblocks.put(currBlock.my_num, currBlock);
    }
    currRes = new Result();
    currRes.kind = Result.PROC;
    currRes.value = currBlock.my_num;
    currBlock = new Block(this);
    currRes.storeResult();
  }

  public String asDotGraph() {
    StringBuffer graph = new StringBuffer();
    graph.append("digraph G {\n");
    for (Block b : blocks) {
      if (!b.instructions.isEmpty()) {
        graph.append(b + "\n");
      }
    }
    graph.append("}");
    return graph.toString();
  }

  public void removeEmpties() {
    boolean changed = false;
    for (Block b : blocks) {
      if (!b.instructions.isEmpty()) {
        // If this block has an edge that does not have any instructions then remove that block
        int stop = b.edges.size();
        for (int i = 0; i < stop; i++) {
          if (
            b.edges.get(i).instructions.isEmpty()
            // ||
            // (
            //   b.edgeLabels.get(i).equals("then")
            //   // &&
            //   // b.edges.get(i).instructions.get(0).inst == op.MOVE // If the only instruction is a bra instruction maybe add later for now it works
            // )
          ) {
            for (Block bb : b.edges.get(i).edges) {
              for (Instruction in : b.instructions) {
                if (
                  in.right != null &&
                  in.right.kind == Result.PROC &&
                  in.right.proc == b.edges.get(i)
                ) {
                  in.right.proc = bb;
                }
              }
              changed = true;
              b.addEdge(bb, b.edgeLabels.get(i));
              bb.parents.remove(b.edges.get(i));
            }
            b.edges.remove(i--);
            stop--;
          }
        }
      }
    }
    if (changed) {
      removeEmpties();
    }
  }

  public void addRelInstJump(
    Block block,
    String tok,
    Result left,
    Block oblock,
    String edgeL
  ) {
    Result right = new Result();
    right.kind = Result.PROC;
    right.proc = oblock;
    block.addEdge(oblock, edgeL);
    block.hasBreak = true;
    switch (tok) {
      case "==":
        block.addInstruction(new Instruction(op.BEQ, left, right));
        break;
      case "!=":
        block.addInstruction(new Instruction(op.BNE, left, right));
        break;
      case ">=":
        block.addInstruction(new Instruction(op.BGE, left, right));
        break;
      case "<=":
        block.addInstruction(new Instruction(op.BLE, left, right));
        break;
      case "<":
        block.addInstruction(new Instruction(op.BLT, left, right));
        break;
      case ">":
        block.addInstruction(new Instruction(op.BGT, left, right));
        break;
      default:
        block.addInstruction(new Instruction(op.BRA, right));
        break;
    }
    right.storeResult();
  }

  public void setDefaultLatest() {
    for (Block b : blocks) {
      for (Entry<Symbol, Symbol> s : allSymbols.entrySet()) {
        if (!b.latest.containsKey(s.getKey())) {
          b.latest.put(s.getKey(), s.getValue());
        }
      }
    }
  }

  public void removeInstruction(Instruction i) {
    for (Block b : blocks) {
      for (Instruction ii : b.instructions) {
        if (i.my_num > ii.my_num) continue;
        ii.my_num--;
      }
    }
    for (Block b : blocks) {
      b.instructions.remove(i);
    }
  }

  public void fixUpSSA() {
    // Removing all eliminated instructions
    for (Block b : blocks) {
      int size = b.instructions.size();
      for (int j = 0; j < size; j++) {
        Instruction i = b.instructions.get(j);
        if (!i.eliminated) continue;
        removeInstruction(i);
        j--;
        size--;
      }
    }
    // Finished removing all eliminated instructions
    // Set empty block edges correctly
    removeEmpties();
    //Remove all empty blocks
    blocks.removeIf(o -> o.instructions.isEmpty());
    // ArrayList<Block> toRm = new ArrayList<>();
    // for (Block b : blocks) {
    //   if (b.edges.size() > 1) {
    //     Instruction last = b.instructions.get(b.instructions.size() - 1);
    //     if (last.inst == op.BRA) {
    //       Block rm = null;
    //       for (Block jj : b.edges) {
    //         if (jj != last.right.proc) {
    //           rm = last.right.proc;
    //           break;
    //         }
    //       }
    //       b.edges.remove(rm);
    //       toRm.add(rm);
    //     }
    //   }
    // }
    // blocks.removeAll(toRm);
  }

  public void flipAllBreaks() {
    for (Block b : blocks) {
      if (b.instructions.isEmpty()) {
        continue;
      }
      Instruction brake = b.instructions.get(b.instructions.size() - 1);
      boolean cont = false;
      switch (brake.inst) {
        case BNE:
          brake.inst = op.BEQ;
          break;
        case BEQ:
          brake.inst = op.BNE;
          break;
        case BGT:
          brake.inst = op.BLE;
          break;
        case BGE:
          brake.inst = op.BLT;
          break;
        case BLT:
          brake.inst = op.BGE;
          break;
        case BLE:
          brake.inst = op.BGT;
          break;
        default:
          cont = true;
      }
      if (cont) continue;
      for (Block c : b.edges) {
        if (brake.right.proc != c) {
          brake.right.proc = c;
          break;
        }
      }
    }
  }

  public void instantiateUsedAt() {
    for (Block b : blocks) {
      for (Instruction i : b.instructions) {
        if (i.eliminated) continue;
        if (i.left != null && i.left.kind == Result.INST) {
          i.left.inst.usedAt = i;
        }
        if (i.right != null && i.right.kind == Result.INST) {
          i.right.inst.usedAt = i;
        }
      }
    }
  }

  public ArrayList<Instruction> getAllInstruction(Block b) {
    ArrayList<Block> visited = inOrderBlock(b);
    ArrayList<Instruction> instructions = new ArrayList<>();
    for (Block c : visited) {
      for (Instruction i : c.instructions) {
        instructions.add(i);
      }
    }
    return instructions;
  }

  public void countUpResults() {
    ArrayList<ArrayList<Result>> res = new ArrayList<>();
    for (Block b : blocks) {
      for (Instruction j : b.instructions) {
        if (j.inst == op.CALL) {
          for (int x = 0; x < j.func_params.size() - 1; x++) {
            addToRes(res, j.func_params.get(x));
          }
        } else {
          if (j.left != null) {
            addToRes(res, j.left);
          }
          if (j.right != null) {
            addToRes(res, j.right);
          }
          if (j.third != null) {
            addToRes(res, j.third);
          }
        }
      }
    }
    for (ArrayList<Result> r : res) {
      for (Result rr : r) {
        rr.result_count = r.size();
      }
    }
  }

  public void addToRes(ArrayList<ArrayList<Result>> res, Result r) {
    int change = 0;
    for (ArrayList<Result> i : res) {
      if (i.get(0).compare(r)) break;
      change++;
    }
    if (change == res.size()) {
      res.add(new ArrayList<>());
      res.get(res.size() - 1).add(r);
    } else {
      res.get(change).add(r);
    }
  }

  public ArrayList<Block> inOrderBlock(Block root) {
    ArrayList<Block> b = new ArrayList<>();
    boolean begin = false;
    for (Block c : blocks) {
      if (c == root) {
        begin = true;
      }
      if (!begin) continue;
      if (root == main && c.label.equals("main")) {
        b.add(c);
      } else if (root != main && c.function == root.function) {
        b.add(c);
      } else {
        begin = false;
      }
    }
    return b;
  }

  public void setAddresses(Block root) {
    HashSet<Symbol> symbols = new HashSet<>();
    int count = 1;
    for (Instruction i : getAllInstruction(root)) {
      switch (i.inst) {
        case PHI:
          if (!symbols.contains(i.third.var.OG)) {
            i.third.var.OG.address = count++;
            symbols.add(i.third.var.OG);
          }
          break;
        case MOVE:
          if (!symbols.contains(i.right.var.OG)) {
            i.right.var.OG.address = count++;
            symbols.add(i.right.var.OG);
          }
          break;
      }
    }
  }
}
