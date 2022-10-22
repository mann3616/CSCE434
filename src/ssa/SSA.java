package ssa;

import ast.*;
import java.util.ArrayList;
import java.util.List;
import pl434.DLX;
import pl434.Symbol;
import ssa.Instruction.op;
import types.*;

public class SSA implements NodeVisitor {

  Block currBlock;
  Result currRes;
  List<Block> blocks;

  public SSA() {
    blocks = new ArrayList<>();
  }

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
    currBlock = new Block();
    Block save = currBlock;
    // Make node.main() not annoying
    node.variables().accept(this);
    node.functions().accept(this);
    currBlock = save;
    node.mainStatementSequence().accept(this);
    blocks.add(0, currBlock);
  }

  @Override
  public void visit(RepeatStatement node) {
    node.sequence().accept(this);
    node.relation().accept(this);
  }

  @Override
  public void visit(WhileStatement node) {
    node.relation().accept(this);
    node.sequence().accept(this);
  }

  @Override
  public void visit(ReturnStatement node) {
    if (node.relation() != null) {
      node.relation().accept(this);
    }
  }

  @Override
  public void visit(IfStatement node) {
    node.relation().accept(this);
    node.ifSequence().accept(this);
    if (node.elseSequence() != null) {
      node.elseSequence().accept(this);
    }
  }

  @Override
  public void visit(Assignment node) {
    node.addressOf().accept(this);
    Result addressOf = currRes;
    node.right().accept(this);
    Result right = currRes;
    addInstruction(new Instruction(op.MOVE, addressOf, right));
  }

  @Override
  public void visit(AddressOf node) {
    currRes = new Result();
    currRes.kind = Result.VAR;
    currRes.var = new Symbol(node.symbol());
  }

  @Override
  public void visit(Dereference node) {
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
    node.right().accept(this);
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
    node.right().accept(this);
  }

  @Override
  public void visit(Relation node) {
    node.left().accept(this);
    node.right().accept(this);
  }

  @Override
  public void visit(ArgumentList node) {
    for (Expression expr : node) {
      expr.accept(this);
    }
  }

  @Override
  public void visit(FunctionCall node) {
    node.list().accept(this);
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

  public String asDotGraph() {
    StringBuffer graph = new StringBuffer();
    for (Block b : blocks) {
      graph.append(b + "\n");
    }
    return graph.toString();
  }
}
