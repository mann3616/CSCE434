package pl434;

import java.util.regex.Pattern;

public class Token {

    public enum Kind {
        // boolean operators
        AND("and"),
        OR("or"),
        NOT("not"),

        // arithmetic operators
        POW("^"),

        MUL("*"),
        DIV("/"),
        MOD("%"),

        ADD("+"),
        SUB("-"),

        // relational operators
        EQUAL_TO("=="),
        NOT_EQUAL("!="),
        LESS_THAN("<"),
        LESS_EQUAL("<="),
        GREATER_EQUAL(">="),
        GREATER_THAN(">"),

        // assignment operators
        LET("let"),
        ASSIGN("="),
        ADD_ASSIGN("+="),
        SUB_ASSIGN("-="),
        MUL_ASSIGN("*="),
        DIV_ASSIGN("/="),
        MOD_ASSIGN("%="),
        POW_ASSIGN("^="),

        // unary increment/decrement
        UNI_INC("++"),
        UNI_DEC("--"),

        // primitive types
        VOID("void"),
        BOOL("bool"),
        INT("int"),
        FLOAT("float"),

        // boolean literals
        TRUE("true"),
        FALSE("false"),

        // region delimiters
        OPEN_PAREN("("),
        CLOSE_PAREN(")"),
        OPEN_BRACE("{"),
        CLOSE_BRACE("}"),
        OPEN_BRACKET("["),
        CLOSE_BRACKET("]"),

        // field/record delimiters
        COMMA(","),
        COLON(":"),
        SEMICOLON(";"),
        PERIOD("."),

        // control flow statements
        IF("if"),
        THEN("then"),
        ELSE("else"),
        FI("fi"),

        WHILE("while"),
        DO("do"),
        OD("od"),

        REPEAT("repeat"),
        UNTIL("until"),

        CALL("call"),
        RETURN("return"),

        // keywords
        MAIN("main"),
        FUNC("function"),

        // special cases
        INT_VAL(),
        FLOAT_VAL(),
        IDENT(),

        EOF(),

        ERROR();

        private String defaultLexeme;
        public static final Pattern isInt = Pattern.compile("^-?[0-9]+$");
        public static final Pattern isFloat = Pattern.compile("^-?[0-9]*\\.[0-9]+$");
        public static final Pattern isIdent = Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*$");
        Kind () {
            defaultLexeme = "";
        }

        Kind (String lexeme) {
            defaultLexeme = lexeme;
        }

        public boolean hasStaticLexeme () {
            return defaultLexeme != null;
        }

        // OPTIONAL: convenience function - boolean matches (String lexeme)
        //           to report whether a Token.Kind has the given lexeme
        //           may be useful
        public static Kind getKind(String lexeme){
            if(lexeme.length()==0){
                return ERROR;
            }
            if(isInt.matcher(lexeme).matches()){
                return INT_VAL;
            }
            if(isFloat.matcher(lexeme).matches()){
                return FLOAT_VAL;
            }
            for(Kind k : Kind.values()){
                if(k.defaultLexeme.equals(lexeme)){
                    return k;
                }
            }
            if(isIdent.matcher(lexeme).matches()){
                return IDENT;
            }
            return ERROR;
        }
    }

    private int lineNum;
    private int charPos;
    Kind kind;  // package-private
    private String lexeme = "";


    // TODO: implement remaining factory functions for handling special cases (EOF below)

    public static Token EOF (int linePos, int charPos) {
        Token tok = new Token(linePos, charPos);
        tok.kind = Kind.EOF;
        return tok;
    }

    private Token (int lineNum, int charPos) {
        this.lineNum = lineNum;
        this.charPos = charPos;
        // no lexeme provide, signal error
        this.kind = Kind.ERROR;
        this.lexeme = "No Lexeme Given";
    }

    public Token (String lexeme, int lineNum, int charPos) {
        this.lineNum = lineNum;
        this.kind = Kind.getKind(lexeme);
        this.lexeme = (this.kind != Kind.ERROR ? lexeme : "Unrecognized lexeme: " + lexeme);
        this.charPos = charPos - lexeme().length();
    }

    public int lineNumber () {
        return lineNum;
    }

    public int charPosition () {
        return charPos;
    }

    public String lexeme () {
        return lexeme;
    }

    public Kind kind () {
        return kind;
    }

    // TODO: function to query a token about its kind - boolean is (Token.Kind kind)

    // OPTIONAL: add any additional helper or convenience methods
    //           that you find make for a cleaner design

    @Override
    public String toString () {
        return "Line: " + lineNum + ", Char: " + charPos + ", Lexeme: " + lexeme;
    }
}
