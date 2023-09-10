
package zc246_zl345_co232_mw756.lex;
import java_cup.runtime.Symbol;
import zc246_zl345_co232_mw756.rho.sym;
import zc246_zl345_co232_mw756.errors.LexicalError;
import java.util.ArrayList;


%%

%public
%class YyLex
%line
%column
%cup
%eofval{
  return symbol(sym.EOF, "<EOF>");
%eofval}

%unicode
%pack

%ctorarg boolean isRho
%init{
    this.isRho = isRho;
%init}

%{
        private boolean isRho;

        private Symbol symbol(int type) {
            return new Symbol(type, lineNumber(), column());
        }

        private Symbol symbol(int type, Object value) {
            return new Symbol(type, lineNumber(), column(), value);
        }

        class NumericLiteral extends Symbol {
            public NumericLiteral(Long val) {
                super(zc246_zl345_co232_mw756.rho.sym.INTEGER_LITERAL, lineNumber(), column(), val);
            }
        }

        class StringLiteral extends Symbol {
            public StringLiteral(Long[] s, int startindex) {
                super(zc246_zl345_co232_mw756.rho.sym.STRING_LITERAL, lineNumber(), startindex, s);
            }
        }

        class CharLiteral extends Symbol {
            public CharLiteral(Long s, int startindex) {
                super(zc246_zl345_co232_mw756.rho.sym.CHARACTER_LITERAL, lineNumber(), startindex, s);
            }

            public CharLiteral(Long s) {
                super(zc246_zl345_co232_mw756.rho.sym.CHARACTER_LITERAL, lineNumber(), column()-1, s);
            }
        }

        public String handleEscapes(String s) {
            String s1 = s.replaceAll("\n", "\\\\n");
            return s1.replaceAll("\r", "\\\\r");
        }

        public int lineNumber() {
            return yyline + 1;
        }

        public int column() {
            return yycolumn + 1;
        }

        private ArrayList<Long> string = new ArrayList<>();

        private int startIndex = 0;

        private long hexToString(String s) {
            long hexToInt = Integer.parseInt(s.substring(3, s.length() - 1), 16);
            if (hexToInt <= Integer.parseInt("10FFFF", 16)) return hexToInt;
            throw new LexicalError(lineNumber(), column(), "Hex index out of range: " + yytext());
        }

        private Long parseInt(String s) {
            try {
                return Long.parseLong(s);
            } catch (Exception e) {
                throw new LexicalError(lineNumber(), column(), "Integer out of range: " + yytext());
            }
        }

        private long escapeUnicode(String str){
            return str.codePoints().toArray()[0];
        }

        private Long[] toArr(ArrayList<Long> ints){
            Long[] arr = new Long[ints.size()];
            for(int i = 0; i < ints.size(); i++){
                arr[i] = ints.get(i);
            }
            return arr;
        }

        private int[] toIntArr(ArrayList<Long> ints){
            return ints.stream().mapToInt(Math::toIntExact).toArray();
        }

        private ArrayList<Integer> addAll(ArrayList<Integer> ints, int[] arr){
            for (int i : arr) ints.add(i);
            return ints;
        }
%}

Whitespace = [ \t\f\r\n]
Letter = [a-zA-Z]
Digit = [0-9]
Identifier = {Letter}({Digit}|{Letter}|_|\')*
Integer = "0"|[1-9]{Digit}*
Hex = \\"x{"[0-9A-Fa-f]{1,6}"}"


%state COMMENT, STRING, CHAR

%%

<YYINITIAL> {
    {Whitespace}     { /* ignore */ }
    "if"             { return symbol(sym.IF, yytext()); }
    "else"           { return symbol(sym.ELSE, yytext()); }
    "while"          { return symbol(sym.WHILE, yytext()); }
    "return"         { return symbol(sym.RETURN, yytext()); }
    "length"         { return symbol(sym.LENGTH, yytext()); }
    "use"            { return symbol(sym.USE, yytext()); }
    "record"         { if(isRho) return symbol(sym.RECORD, yytext()); else return symbol(sym.IDENTIFIER, yytext()); }
    "null"           { if(isRho) return symbol(sym.NULL, yytext()); else return symbol(sym.IDENTIFIER, yytext()); }
    "break"          { if(isRho) return symbol(sym.BREAK, yytext()); else return symbol(sym.IDENTIFIER, yytext()); }
    "."              { if(isRho) return symbol(sym.DOT, yytext()); else throw new LexicalError(lineNumber(), column(), "Illegal token: " + yytext()); }
    "_"              { return symbol(sym.UNDERSCORE, yytext()); }
    "="              { return symbol(sym.ASSIGN, yytext()); }
    ","              { return symbol(sym.COMMA, yytext()); }
    ";"              { return symbol(sym.SEMICOLON, yytext()); }
    ":"              { return symbol(sym.COLON, yytext()); }
    "("              { return symbol(sym.LPAREN, yytext()); }
    ")"              { return symbol(sym.RPAREN, yytext()); }
    "{"              { return symbol(sym.LBRACE, yytext()); }
    "}"              { return symbol(sym.RBRACE, yytext()); }
    "["              { return symbol(sym.LBRACKET, yytext()); }
    "]"              { return symbol(sym.RBRACKET, yytext()); }
    "*>>"            { return symbol(sym.HIGHMUL, yytext()); }
    "*"              { return symbol(sym.TIMES, yytext()); }
    "/"              { return symbol(sym.DIVIDE, yytext()); }
    "%"              { return symbol(sym.MODULO, yytext()); }
    "+"              { return symbol(sym.PLUS, yytext()); }
    "-"              { return symbol(sym.MINUS, yytext()); }
    "!"              { return symbol(sym.NOT_LOG, yytext()); }
    "=="             { return symbol(sym.EQUAL, yytext()); }
    "!="             { return symbol(sym.NOT_EQUAL, yytext()); }
    "<"              { return symbol(sym.LT, yytext()); }
    ">"              { return symbol(sym.GT, yytext()); }
    "<="             { return symbol(sym.LEQ, yytext()); }
    ">="             { return symbol(sym.GEQ, yytext()); }
    "&"              { return symbol(sym.AND, yytext()); }
    "|"              { return symbol(sym.OR, yytext()); }
    "int"            { return symbol(sym.INT, yytext()); }
    "bool"           { return symbol(sym.BOOL, yytext()); }
    "true"           { return symbol(sym.TRUE, 1L); }
    "false"          { return symbol(sym.FALSE, 0L); }
    {Identifier}     { return symbol(sym.IDENTIFIER, yytext()); }
    "9223372036854775808" { return symbol(sym.MIN_LONG, yytext()); }
    {Integer}        { return new NumericLiteral(parseInt(yytext())); }
    \"               { string = new ArrayList<>(); startIndex = column(); yybegin(STRING); }
    \/\/             { yybegin(COMMENT); }
    \'               { startIndex = column(); yybegin(CHAR); }
    [^]              { throw new LexicalError(lineNumber(), column(), "Illegal token: " + yytext()); }
}

<STRING>{
    \r|\n            { throw new LexicalError(lineNumber(), startIndex, "Illegal string: " + new String(toIntArr(string), 0, string.size())); }
    \"               { yybegin(YYINITIAL); return new StringLiteral(toArr(string), startIndex); }
    \\n              { string.add((long)10); }
    \\\\             { string.add((long)92); }
    \\\"             { string.add((long)34); }
    \\\'             { string.add((long)39); }
    \\t              { string.add((long)9); }
    \\[""a-zA-Z" "]  { throw new LexicalError(lineNumber(), startIndex, "Illegal esacpe sequence: " + yytext()); }
    {Hex}            { string.add(hexToString(yytext())); }
    .                { string.add(escapeUnicode(yytext())); }
    <<EOF>>          { throw new LexicalError(lineNumber(), startIndex, "Illegal string: " + new String(toIntArr(string), 0, string.size())); }
}

<COMMENT>{
    \n               { yybegin(YYINITIAL); }
    .|\r             { /* ignore */ }
}

<CHAR>{
    \n               { throw new LexicalError(lineNumber(), startIndex, "Illegal char: " + yytext()); }
    \\n\'            { yybegin(YYINITIAL); return new CharLiteral((long)10); }
    \\\\\'           { yybegin(YYINITIAL); return new CharLiteral((long)92); }
    \\\"\'           { yybegin(YYINITIAL); return new CharLiteral((long)34); }
    \\\'\'           { yybegin(YYINITIAL); return new CharLiteral((long)39); }
    \\t\'            { yybegin(YYINITIAL); return new CharLiteral((long)9); }
    {Hex}\'          { yybegin(YYINITIAL); return new CharLiteral(hexToString(yytext().substring(0,yytext().length()-1))); }
    \\[""a-zA-Z]\'   { throw new LexicalError(lineNumber(), startIndex, "Illegal esacpe sequence: " + yytext().substring(0,yytext().length()-1)); }
    [^\']\'          { yybegin(YYINITIAL); return new CharLiteral(escapeUnicode(yytext().substring(0,yytext().length()-1)), startIndex); }
    [^]              { throw new LexicalError(lineNumber(), startIndex, "Illegal character literal"); }
    <<EOF>>          { throw new LexicalError(lineNumber(), startIndex, "Illegal character literal"); }
}
