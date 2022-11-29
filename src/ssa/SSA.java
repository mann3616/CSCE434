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
  List<Block> roots;
  HashMap<Symbol, Symbol> allSymbols = new HashMap<>();
  ArrayList<Result> params;
  Stack<Result> indices;
  boolean assign;
  int currDim;
  Optimize opt;

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
  public void visit(VariableDeclaration node) {}

  @Override
  public void visit(FunctionDeclaration node) {
    currBlock.label = node.function.name;
    roots.add(currBlock);
    node.body().accept(this);
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
    roots.add(currBlock);
    node.mainStatementSequence().accept(this);
    Result end = new Result();
    end.kind = Result.CONST;
    end.value = 0;
    end.type = new IntType();
    addInstruction(new Instruction(op.RET, null, end));
    addCurr();
    //removeEmpties();
    DominatorTree tree = new DominatorTree(this);
    // Do we add the phi instructions now?
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
    buildPhi();
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
    addCurr();

    //Adding else block (even if it is not an else) to jump statement for relation() and adds edge
    oldBlock.addEdge(currBlock, "else");

    if (node.elseSequence() != null) {
      node.elseSequence().accept(this);
      //Add reference to the currBlock so that these blocks can be related to after the if/else statement
      Block lastElseBlock = currBlock;
      addCurr();
      lastElseBlock.addEdge(currBlock, "");
    }
    lastThen.addEdge(currBlock, "");
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
  }

  @Override
  public void visit(Dereference node) {
    this.assign = false;
    node.expression.accept(this);
    if (node.expression.getClass().equals(ArrayIndex.class)) {
      addInstruction(new Instruction(op.LOAD, currRes));
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
  }

  @Override
  public void visit(IntegerLiteral node) {
    currRes = new Result();
    currRes.kind = Result.CONST;
    currRes.value = Integer.parseInt(node.literal());
    currRes.type = new IntType();
  }

  @Override
  public void visit(FloatLiteral node) {
    currRes = new Result();
    //currRes.value = DLX.fromFP32ToFP16(Float.parseFloat(node.literal()));
    currRes.kind = Result.CONST;
    currRes.type = new FloatType();
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
      switch (node.function.name) {
        case "printInt":
        case "printFloat":
        case "printBool":
          addInstruction(new Instruction(op.WRITE, params.get(0)));
          break;
        case "printLn":
          addInstruction(new Instruction(op.WRITENL));
          break;
        case "readInt":
        case "readFloat":
        case "readBool":
          addInstruction(new Instruction(op.READ));
          break;
      }
      return;
    }
    // Way call instruction print works is that it will print params list first and then lastly print the function name
    Result this_func = new Result();
    this_func.kind = Result.VAR;
    this_func.var = node.function;
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
    } else {
      // MUL the addy by 4 to fit PC DLX format
      Result nxtMaxDim = new Result();
      nxtMaxDim.kind = Result.CONST;
      nxtMaxDim.value = 4;
      addInstruction(new Instruction(op.MUL, currRes, nxtMaxDim));
      currBlock.instructions.get(currBlock.instructions.size() - 1).isArrayMul =
        true;
      Result mResult = currRes;

      // Get left result (should be a variable) and set it as kind addy since we are getting the address of this result
      node.left.accept(this);
      currRes.kind = Result.ADDY;
      Result leftRes = currRes;

      // Make the GDB Result to have x: ADD GDB var.name
      Result gdb = new Result();
      gdb.kind = Result.GDB;
      addInstruction(new Instruction(op.ADD, gdb, leftRes));

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
    currBlock.addInstruction(inst);
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
            //&& !b.edges.get(i).isJoinNode
          ) {
            for (Block bb : b.edges.get(i).edges) {
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

  public void fixUpSSA() {
    // Removing all eliminated instructions
    for (Block b : blocks) {
      int size = b.instructions.size();
      for (int j = 0; j < size; j++) {
        Instruction i = b.instructions.get(j);
        if (!i.eliminated) continue;
        for (Block fixer : blocks) {
          for (Instruction ii : fixer.instructions) {
            if (i.my_num <= ii.my_num) continue;
            ii.my_num--;
          }
        }
        b.instructions.remove(j--);
        size--;
      }
    }
    // Finished removing all eliminated instructions
    // Set empty block edges correctly
    removeEmpties();
    //Remove all empty blocks
    blocks.removeIf(o -> o.instructions.isEmpty());
  }
}
