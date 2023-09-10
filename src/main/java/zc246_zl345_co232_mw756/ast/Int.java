package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

public class Int extends Expr {
    public final long value;

    public Int(int lineNumber, int columnNumber, long value) {
        super(lineNumber, columnNumber);
        this.value = value;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        if (this.value == Long.MIN_VALUE) {
            printer.startList();
            printer.printAtom("-");
            printer.printAtom("9223372036854775808");
            printer.endList();
        } else {
            printer.printAtom(String.valueOf(value));
        }
    }

    @Override
    public void accept(Visitor v) {
        v.visitInt(this);
    }

    @Override
    public Node constFold() {
        return this;
    }
}
