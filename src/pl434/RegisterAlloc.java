package pl434;

public class RegisterAlloc {

  // This class holds the instruction number of when a variable was assigned to a register
  Integer instruction_number;
  String variable;

  public RegisterAlloc(Integer instruction_number, String variable) {
    this.instruction_number = instruction_number;
    this.variable = variable;
  }

  public String toString() {
    return instruction_number + ": " + variable;
  }
}
