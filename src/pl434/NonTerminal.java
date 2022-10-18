package pl434;

import java.util.HashSet;
import java.util.Set;

public enum NonTerminal {
  // nonterminal FIRST sets for grammar

  // operators
  REL_OP(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        add(Token.Kind.EQUAL_TO);
        add(Token.Kind.NOT_EQUAL);
        add(Token.Kind.LESS_THAN);
        add(Token.Kind.LESS_EQUAL);
        add(Token.Kind.GREATER_EQUAL);
        add(Token.Kind.GREATER_THAN);
      }
    }
  ),
  ASSIGN_OP(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        add(Token.Kind.ASSIGN);
        add(Token.Kind.ADD_ASSIGN);
        add(Token.Kind.DIV_ASSIGN);
        add(Token.Kind.MOD_ASSIGN);
        add(Token.Kind.MUL_ASSIGN);
        add(Token.Kind.POW_ASSIGN);
        add(Token.Kind.SUB_ASSIGN);
      }
    }
  ),
  POW_OP(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        add(Token.Kind.POW);
      }
    }
  ),
  ADD_OP(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        add(Token.Kind.ADD);
        add(Token.Kind.SUB);
        add(Token.Kind.OR);
      }
    }
  ),
  MULT_OP(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        add(Token.Kind.MUL);
        add(Token.Kind.DIV);
        add(Token.Kind.AND);
        add(Token.Kind.MOD);
      }
    }
  ),
  UNARY_OP(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        add(Token.Kind.UNI_DEC);
        add(Token.Kind.UNI_INC);
      }
    }
  ),

  // literals (integer and float handled by Scanner)
  BOOL_LIT(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        add(Token.Kind.TRUE);
        add(Token.Kind.FALSE);
      }
    }
  ),
  LITERAL(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        add(Token.Kind.INT_VAL);
        add(Token.Kind.FLOAT_VAL);
        add(Token.Kind.TRUE);
        add(Token.Kind.FALSE);
      }
    }
  ),

  // designator (ident handled by Scanner)
  DESIGNATOR(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement designator FIRST set");
        add(Token.Kind.IDENT);
      }
    }
  ),

  ADD_EXPRESSION(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement expression FIRST set");
      }
    }
  ),
  MULT_EXPRESSION(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement expression FIRST set");
      }
    }
  ),
  POW_EXPRESSION(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement expression FIRST set");
      }
    }
  ),
  RELATION_EXPRESSION(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement term FIRST set");
      }
    }
  ),
  GROUP_EXPRESSION(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement expression FIRST set");
      }
    }
  ),
  RELATION(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement relation FIRST set");
        add(Token.Kind.OPEN_PAREN);
      }
    }
  ),
  CONDITION(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement condition FIRST set");
      }
    }
  ),

  // statements
  ASSIGN(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement assign FIRST set");
        add(Token.Kind.LET);
      }
    }
  ),
  FUNC_CALL(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement funcCall FIRST set");
        add(Token.Kind.CALL);
      }
    }
  ),
  IF_STAT(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement ifStat FIRST set");
        add(Token.Kind.IF);
      }
    }
  ),
  WHILE_STAT(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement whileStat FIRST set");
        add(Token.Kind.WHILE);
      }
    }
  ),
  REPEAT_STAT(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement repeatStat FIRST set");
        add(Token.Kind.REPEAT);
      }
    }
  ),
  RETURN_STAT(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement returnStat FIRST set");
        add(Token.Kind.RETURN);
      }
    }
  ),
  STATEMENT(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement statement FIRST set");
        add(Token.Kind.RETURN);
        add(Token.Kind.REPEAT);
        add(Token.Kind.WHILE);
        add(Token.Kind.IF);
        add(Token.Kind.LET);
        add(Token.Kind.CALL);
      }
    }
  ),
  STAT_SEQ(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement statSeq FIRST set");

      }
    }
  ),

  // declarations
  TYPE_DECL(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement typeDecl FIRST set");
        add(Token.Kind.INT);
        add(Token.Kind.FLOAT);
        add(Token.Kind.BOOL);
        add(Token.Kind.VOID);
      }
    }
  ),
  VAR_DECL(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement varDecl FIRST set");
        add(Token.Kind.INT);
        add(Token.Kind.FLOAT);
        add(Token.Kind.BOOL);
      }
    }
  ),
  PARAM_DECL(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement paramDecl FIRST set");
        add(Token.Kind.INT);
        add(Token.Kind.FLOAT);
        add(Token.Kind.BOOL);
      }
    }
  ),

  // functions
  FORMAL_PARAM(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement formalParam FIRST set");
        add(Token.Kind.OPEN_PAREN);
      }
    }
  ),
  FUNC_BODY(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement funcBody FIRST set");
        add(Token.Kind.OPEN_BRACE);
      }
    }
  ),
  FUNC_DECL(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement funcDecl FIRST set");
        add(Token.Kind.FUNC);
      }
    }
  ),

  // computation
  COMPUTATION(
    new HashSet<Token.Kind>() {
      private static final long serialVersionUID = 1L;

      {
        //throw new RuntimeException("implement computation FIRST set");
      }
    }
  );

  private final Set<Token.Kind> firstSet = new HashSet<>();

  private NonTerminal(Set<Token.Kind> set) {
    firstSet.addAll(set);
  }

  public final Set<Token.Kind> firstSet() {
    return firstSet;
  }
}
