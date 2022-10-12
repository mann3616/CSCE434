package types;

public class ErrorType extends Type {

  private String message;

  public ErrorType(String message) {
    this.message = message;
  }

  public String toString() {
    return message;
  }
}
