package pl434;

import ssa.Instruction;

public class VariableInfo {

  public Integer opening;
  public Integer closing;
  public Instruction instruction;

  public VariableInfo(int opening, Instruction instruction) {
    this.opening = opening;
    this.instruction = instruction;
  }

  public VariableInfo(int opening, int closing) {
    this.opening = opening;
    this.closing = closing;
  }

  public String toString() {
    String openingString = "";
    String closingString = "";
    if (opening == null) {
      openingString = "null";
    } else {
      openingString = opening.toString();
    }
    if (closing == null) {
      closingString = "null";
    } else {
      closingString = closing.toString();
    }
    return "[" + openingString + "," + closingString + "]";
  }
}
