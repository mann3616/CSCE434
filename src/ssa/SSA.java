package ssa;

import ast.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import pl434.DLX;
import pl434.Symbol;
import ssa.Instruction.op;
import types.*;

public class SSA implements NodeVisitor {

  Block currBlock;
  Result currRes;
  HashMap<Integer, Block> hmblocks = new HashMap<>();
  List<Block> blocks;
  ArrayList<Result> params;
  boolean assign;

  public SSA() {
    currBlock = new Block();
    this.assign = false;
    params = new ArrayList<>();
    blocks = new ArrayList<>();
  }

  public void visit(StatementSequence node) {
    for (Statement s : node) {
      s.accept(this);
    }
    //if (!currBlock.instructions.isEmpty()) {
    addCurr();
    // } else {
    //   for (Block b : blocks) {
    //     for (int i = b.edges.size() - 1; i >= 0; i--) {
    //       if (b.edges.get(i).my_num == currBlock.my_num) {
    //         b.edges.remove(i);
    //         b.edgeLabels.remove(i);
    //       }
    //     }
    //   }
    // }
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
    node.body().accept(this);
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
    node.mainStatementSequence().accept(this);
  }

  @Override
  public void visit(RepeatStatement node) {
    Block oldBlock = currBlock;
    //TODO: Connect relation to sequence (just incase there are split blocks during sequence part)
    Block begin = currBlock;
    if (!currBlock.instructions.isEmpty()) {
      addCurr();
      oldBlock.addEdge(begin, "");
    }
    node.sequence().accept(this);
    Block endSeq = blocks.get(blocks.size() - 1);
    Block save = currBlock;
    currBlock = endSeq;
    node.relation().accept(this);
    Block relBlock = currBlock;
    currBlock = save;
    addCurr();
    relBlock.addEdge(begin, "then");
    if (node.relation().getClass().equals(Relation.class)) {
      addRelInstJump(
        relBlock,
        ((Relation) node.relation()).rel(),
        currRes,
        currBlock,
        "else"
      );
    } else {
      addRelInstJump(relBlock, "==", currRes, currBlock, "else");
    }
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
    Result relRes = currRes;
    //Added relationBlock as an edge

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
    addRelInstJump(blocks.get(blocks.size() - 1), "", null, oldBlock, "");
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

    oldBlock.addEdge(currBlock, "then");
    node.ifSequence().accept(this);

    //Adding else block (even if it is not an else) to jump statement for relation() and adds edge
    if (node.relation().getClass().equals(Relation.class)) {
      addRelInstJump(
        oldBlock,
        ((Relation) node.relation()).rel(),
        relRes,
        currBlock,
        "else"
      );
    } else {
      addRelInstJump(oldBlock, "==", relRes, currBlock, "else");
    }
    if (node.elseSequence() != null) {
      node.elseSequence().accept(this);
      oldBlock.edges.get(1).addEdge(currBlock, "");
      //Add reference to the currBlock so that these blocks can be related to after the if/else statement
    }
    //Add reference to the currBlock for the "then" sequence
    oldBlock.edges.get(0).addEdge(currBlock, "else");
  }

  @Override
  public void visit(Assignment node) {
    node.right().accept(this);
    Result right = currRes;
    this.assign = true;
    node.addressOf().accept(this);
    this.assign = false;
    Result addressOf = currRes;
    addInstruction(new Instruction(op.MOVE, right, addressOf));
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
    currRes.value = DLX.fromFP32ToFP16(Float.parseFloat(node.literal()));
    currRes.kind = Result.CONST;
    currRes.type = new FloatType();
  }

  @Override
  public void visit(LogicalNot node) {
    node.right().accept(this);
    Result right = currRes;
    Result one = new Result();
    one.kind = Result.CONST;
    one.type = new IntType();
    one.value = 1;
    addInstruction(new Instruction(op.XOR, one, right));
  }

  @Override
  public void visit(Power node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    Result right = currRes;
    addInstruction(new Instruction(op.POW, left, right));
  }

  @Override
  public void visit(Multiplication node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    Result right = currRes;
    addInstruction(new Instruction(op.MUL, left, right));
  }

  @Override
  public void visit(Division node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    Result right = currRes;
    addInstruction(new Instruction(op.DIV, left, right));
  }

  @Override
  public void visit(Modulo node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    Result right = currRes;
    addInstruction(new Instruction(op.MOD, left, right));
  }

  @Override
  public void visit(LogicalAnd node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    Result right = currRes;
    addInstruction(new Instruction(op.AND, left, right));
  }

  @Override
  public void visit(Addition node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    Result right = currRes;
    addInstruction(new Instruction(op.ADD, left, right));
  }

  @Override
  public void visit(Subtraction node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    Result right = currRes;
    addInstruction(new Instruction(op.SUB, left, right));
  }

  @Override
  public void visit(LogicalOr node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    Result right = currRes;
    addInstruction(new Instruction(op.OR, left, right));
  }

  @Override
  public void visit(Relation node) {
    node.left().accept(this);
    Result left = currRes;
    node.right().accept(this);
    Result right = currRes;
    addInstruction(new Instruction(op.CMP, left, right));
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

  @Override
  public void visit(ArrayIndex node) {
    node.left.accept(this);
    node.right.accept(this);
  }

  public void addInstruction(Instruction inst) {
    currRes = new Result();
    currRes.kind = Result.INST;
    currRes.inst = inst.my_num;
    currBlock.addInstruction(inst);
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
    currBlock = new Block();
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

  public void addRelInstJump(
    Block block,
    String tok,
    Result left,
    Block oblock,
    String edgeL
  ) {
    Result right = new Result();
    right.kind = Result.PROC;
    right.value = oblock.my_num;
    block.addEdge(oblock, edgeL);
    block.hasBreak = true;
    switch (tok) {
      case "==":
        block.addInstruction(new Instruction(op.BNE, left, right));
        break;
      case "!=":
        block.addInstruction(new Instruction(op.BEQ, left, right));
        break;
      case ">=":
        block.addInstruction(new Instruction(op.BLT, left, right));
        break;
      case "<=":
        block.addInstruction(new Instruction(op.BGT, left, right));
        break;
      case "<":
        block.addInstruction(new Instruction(op.BGE, left, right));
        break;
      case ">":
        block.addInstruction(new Instruction(op.BLE, left, right));
        break;
      default:
        block.addInstruction(new Instruction(op.BRA, right));
        break;
    }
  }
}
