package ssa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;
import pl434.Symbol;
import ssa.Instruction.op;

public class DominatorTree {

  HashSet<Block> possibleBlocks = new HashSet<>();

  Block objective;

  public DominatorTree(SSA ssa) {
    for (Block b : ssa.blocks) {
      possibleBlocks.add(b);
    }
  }

  public void iterPhi(HashSet<Block> visited, Block root) {
    // Add all PHI to all blocks
    if (visited.contains(root)) {
      return;
    }
    visited.add(root);
    // Explore edges and do the same
    for (Block e : root.edges) {
      if (!root.doms.contains(e)) {
        for (Symbol i : root.latest.keySet()) {
          if (!e.latest.containsKey(i)) {
            e.latest.put(i, root.latest.get(i));
          }
        }
      }
      iterPhi(visited, e);
    }
    for (Block df : root.domFront) {
      addPhi(root, df);
    }
  }

  // df gets PHI for a symbol if needed
  public void addPhi(Block root, Block df) {
    for (Symbol x : root.blockVars) {
      // You take dominance frontiers
      df.phis.put(x, new Instruction(op.PHI));
    }
    if (root.isJoinNode) {
      for (Symbol x : root.phis.keySet()) {
        df.phis.put(x, new Instruction(op.PHI));
      }
    }
  }

  public void buildTree(Block root) {
    // Go through full Map using BFS, and compare parents dominances
    Stack<Block> bfsStack = new Stack<>();
    HashSet<Block> visited = new HashSet<>();
    bfsStack.add(root);
    while (!bfsStack.isEmpty()) {
      Stack<Block> nxtBlocks = new Stack<>();
      while (!bfsStack.isEmpty()) {
        Block nxt = bfsStack.pop();
        if (visited.contains(nxt)) {
          continue;
        } else {
          visited.add(nxt);
          for (Block child : nxt.edges) {
            if (!child.visited.contains(nxt) && !nxt.doms.contains(child)) {
              compareParents(child, nxt);
              nxtBlocks.add(child);
              child.visited.add(nxt);
            }
          }
        }
      }
      bfsStack.clear();
      bfsStack.addAll(nxtBlocks);
    }

    // Solve for Immediate dominance
    for (Block b : visited) {
      b.solveIDom();
    }
    compDF(new HashSet<>(), root);
    printDoms(new HashSet<>(), root);
  }

  public void compDF(HashSet<Block> visited, Block root) {
    // overflow error?
    // local + LRS
    if (visited.contains(root)) {
      return;
    }
    if (root.my_num == 2) {
      System.out.println();
    }
    // Comp Local
    compLocal(root);
    visited.add(root);

    //Iterate through all DT children
    for (Block c : root.idomChildren) {
      // Recur and compute the child
      compDF(visited, c);
      // Add all child DF to this DF
      for (Block cDF : c.domFront) {
        if (!cDF.doms.contains(root) || cDF == root) {
          root.domFront.add(cDF);
        }
      }
    }
  }

  public void compLocal(Block root) {
    for (Block b : root.edges) {
      if (!b.doms.contains(root) || root == b || b.endIfNode) {
        root.domFront.add(b);
      }
    }
  }

  public void printDoms(HashSet<Block> visited, Block root) {
    if (visited.contains(root)) {
      return;
    }
    visited.add(root);
    System.out.print("BB" + root.my_num + " : ");
    if (root.iDom != null) {
      System.out.println("BB" + root.iDom.my_num);
    } else {
      System.out.println("NONE");
    }
    for (Block b : root.doms) {
      System.out.println("  BB" + b.my_num);
    }
    System.out.print("  DomFront:");
    for (Block b : root.domFront) {
      System.out.print(" BB" + b.my_num);
    }
    System.out.println();
    for (int i = 0; i < root.edges.size(); i++) {
      printDoms(visited, root.edges.get(i));
    }
  }

  public void compareParents(Block block, Block parent) {
    if (block.doms.size() == 1) {
      for (Block dom : parent.doms) {
        if (dom != block) {
          block.doms.add(dom);
        }
      }
    } else {
      ArrayList<Block> intersectComp = new ArrayList<>();
      for (Block dom : block.doms) {
        if (!parent.doms.contains(dom) && dom != block) {
          intersectComp.add(dom);
        }
      }
      for (Block dom : intersectComp) {
        block.doms.remove(dom);
      }
    }
  }
}
