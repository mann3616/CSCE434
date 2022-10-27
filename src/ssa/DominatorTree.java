package ssa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public class DominatorTree {

  HashSet<Block> possibleBlocks = new HashSet<>();

  Block objective;

  public DominatorTree(SSA ssa) {
    for (Block b : ssa.blocks) {
      possibleBlocks.add(b);
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
            if (!visited.contains(child)) {
              compareParents(child, nxt);
              nxtBlocks.add(child);
            }
          }
        }
      }
      bfsStack.clear();
      bfsStack.addAll(nxtBlocks);
    }

    for (Block b : visited) {
      b.solveIDom();
    }
    printDoms(new HashSet<>(), root);
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
    for (int i = 0; i < root.edges.size(); i++) {
      printDoms(visited, root.edges.get(i));
    }
  }

  public void compareParents(Block block, Block parent) {
    if (block.doms.size() == 1) {
      for (Block dom : parent.doms) {
        block.doms.add(dom);
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
