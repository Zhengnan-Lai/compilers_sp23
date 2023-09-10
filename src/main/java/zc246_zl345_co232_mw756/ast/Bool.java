package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

//thomas
public class Bool extends Expr {
    public final long value;

    public Bool(int lineNumber, int columnNumber, long value) {
        super(lineNumber, columnNumber);
        this.value = value;
    }

    public void printNode(SExpPrinter printer) {
        if(value == 1L) printer.printAtom("true");
        else printer.printAtom("false");
    }

    public void accept(Visitor v) {
        v.visitBool(this);
    }

    @Override
    public Node constFold() {
        return this;
    }
}
