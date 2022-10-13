package pl434;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;
import java.util.NoSuchElementException;
import pl434.Token.Kind;

public class Scanner implements Iterator<Token> {

  private BufferedReader input; // buffered reader to read file
  private boolean closed; // flag for whether reader is closed or not

  private int lineNum; // current line number
  private int charPos; // character offset on current line

  private String scan; // current lexeme being scanned in
  private int nextChar; // contains the next char (-1 == EOF))

  // reader will be a FileReader over the source file
  public Scanner(Reader reader) {
    input = new BufferedReader(reader);
    charPos = -1;
    lineNum = 0;
    closed = false;
    readChar();
  }

  // signal an error message
  public void Error(String msg, Exception e) {
    System.err.println("Scanner: Line - " + lineNum + ", Char - " + charPos);
    if (e != null) {
      e.printStackTrace();
    }
    System.err.println(msg);
  }

  /*
   * helper function for reading a single char from input
   * can be used to catch and handle any IOExceptions,
   * advance the charPos or lineNum, etc.
   */
  private int readChar() {
    int currChar = nextChar;
    try {
      nextChar = input.read();
      if (nextChar == '\n') {
        lineNum++;
        charPos = 0;
      } else {
        charPos++;
      }
      if (nextChar == -1) {
        input.close();
      }
    } catch (IOException e) {
      nextChar = -1;
    }
    return currChar;
  }

  /*
   * function to query whether or not more characters can be read
   * depends on closed and nextChar
   */
  @Override
  public boolean hasNext() {
    return !closed;
  }

  /*
   *	returns next Token from input
   *
   *  invariants:
   *  1. call assumes that nextChar is already holding an unread character
   *  2. return leaves nextChar containing an untokenized character
   *  3. closes reader when emitting EOF
   */
  @Override
  public Token next() {
    skipToWord();
    if (nextChar == -1) {
      closed = true;
      return Token.EOF(lineNum, charPos);
    }
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    Token token;
    if (isRegionDelimiter() || isRecordDelimiter()) {
      token = new Token("" + (char) readChar(), lineNum, charPos);
    } else if (isOperator()) {
      String nextOperator = nextOperator();
      if (nextOperator.equals("-") && isNumber()) {
        nextOperator += nextWord();
        token = new Token(nextOperator, lineNum, charPos);
      } else if (nextOperator.startsWith("//")) {
        skipLine();
        return next();
      } else if (nextOperator.startsWith("/*")) {
        if (!endOfBlockComment()) {
          token = new Token("", lineNum, charPos);
        } else {
          return next();
        }
      }
      token = new Token(nextOperator, lineNum, charPos);
    } else {
      token = new Token(nextWord(), lineNum, charPos);
    }
    if (token.kind() == Kind.ERROR) {
      Error("Error on Lexeme: " + token.lexeme(), new Exception());
    }
    skipToWord();
    return token;
  }

  private void skipToWord() {
    while (hasNext() && isEmptySpace()) {
      readChar();
    }
  }

  private void skipLine() {
    while (nextChar != '\n' && nextChar != -1) {
      readChar();
    }
  }

  private String nextWord() {
    String fullWord = "";
    boolean isNum = true;
    boolean entered = false;
    while (
      nextChar != -1 &&
      !isRegionDelimiter() &&
      !isRecordDelimiter() &&
      !isEmptySpace() &&
      !isOperator()
    ) {
      entered = true;
      if (!isNumber()) {
        isNum = false;
      }
      fullWord += (char) readChar();
    }
    if (entered && isNum && nextChar == '.') {
      fullWord += (char) readChar() + nextWord();
    }
    return fullWord;
  }

  private String nextOperator() {
    String fullOperator = "";
    fullOperator += (char) readChar();
    if (fullOperator.equals("/") && (nextChar == '/' || nextChar == '*')) {
      return fullOperator + (char) readChar();
    }
    if (
      isOperator() && Kind.getKind(fullOperator + (char) nextChar) != Kind.ERROR
    ) {
      fullOperator += (char) readChar();
    }
    return fullOperator;
  }

  private boolean isRegionDelimiter() {
    return (
      nextChar == '{' ||
      nextChar == '}' ||
      nextChar == '[' ||
      nextChar == ']' ||
      nextChar == '(' ||
      nextChar == ')'
    );
  }

  private boolean isRecordDelimiter() {
    return (
      nextChar == '.' || nextChar == ',' || nextChar == ':' || nextChar == ';'
    );
  }

  private boolean isEmptySpace() {
    return (
      nextChar == ' ' || nextChar == '\n' || nextChar == '\t' || nextChar == 13
    );
  }

  private boolean isOperator() {
    return (
      nextChar == '+' ||
      nextChar == '*' ||
      nextChar == '/' ||
      nextChar == '%' ||
      nextChar == '=' ||
      nextChar == '-' ||
      nextChar == '<' ||
      nextChar == '>' ||
      nextChar == '^' ||
      nextChar == '!'
    );
  }

  private boolean isNumber() {
    return nextChar >= '0' && nextChar <= '9';
  }

  private boolean endOfBlockComment() {
    int last = '&';
    while (nextChar != '/' || last != '*') {
      if (nextChar == -1) {
        return false;
      }
      last = readChar();
    }
    readChar();
    return true;
  }
}
