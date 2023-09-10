package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.Lexer;
import zc246_zl345_co232_mw756.visitor.Visitor;


//thomas
public class Str extends Expr {
    public Long[] value;

    public Str(int lineNumber, int columnNumber, Long[] value) {
        super(lineNumber, columnNumber);
        this.value = value;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.printAtom("\"" + Lexer.escapeAll(value) + "\"");
    }

    public void accept(Visitor v) {
        v.visitStr(this);
    }

    @Override
    public Node constFold() {
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (long l : value) {
            sb.append((char) l);
        }
        return sb.toString();
    }
}
