package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

public class FieldAccess extends Expr{
    public Expr expr;
    public Identifier field;
    public Record record;

    public FieldAccess(int lineNumber, int columnNumber, Expr expr, Identifier field) {
        super(lineNumber, columnNumber);
        this.expr = expr;
        this.field = field;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        printer.printAtom(".");
        expr.printNode(printer);
        field.printNode(printer);
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitFieldAccess(this);
    }

    @Override
    public Node constFold() {
        expr = (Expr) expr.constFold();
        return this;
    }
}
