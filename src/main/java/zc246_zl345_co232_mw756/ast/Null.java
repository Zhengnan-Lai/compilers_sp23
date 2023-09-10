package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

public class Null extends Expr{
    public Null(int lineNumber, int columnNumber) {
        super(lineNumber, columnNumber);
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        printer.printAtom("null");
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitNull(this);
    }

    @Override
    public Node constFold() {
        return this;
    }
}
