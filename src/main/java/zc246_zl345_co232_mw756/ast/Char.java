package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.Lexer;
import zc246_zl345_co232_mw756.visitor.Visitor;

// thomas
public class Char extends Expr {
    public final Long value;

    public Char(int lineNumber, int columnNumber, Long value) {
        super(lineNumber, columnNumber);
        this.value = value;
    }

    public void printNode(SExpPrinter printer) {
        printer.printAtom("'" + Lexer.escape(value) + "'");
    }

    public void accept(Visitor v) {
        v.visitChar(this);
    }

    @Override
    public Node constFold() {
        return this;
    }
}
