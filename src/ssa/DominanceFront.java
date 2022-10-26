package ssa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DominanceFront {

  HashMap<Block, Set<Block>> dfs;

  public DominanceFront() {
    dfs = new HashMap<>();
  }

  public void getDF(Block start) {
    Set<Block> df = new HashSet<>();

    for (Block preds : start.edges) {
      //if(preds)
    }
  }
}
