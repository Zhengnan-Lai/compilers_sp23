package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.ArrayList;
import java.util.List;

public class ArrayLiteral extends Expr {
    public final List<Expr> values;

    public ArrayLiteral(int lineNumber, int columnNumber, List<Expr> values) {
        super(lineNumber, columnNumber);
        this.values = values;
    }

    public List<Expr> getValues() {
        return values;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        for (Expr value : values) {
            value.printNode(printer);
        }
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitArrayLiteral(this);
    }

    @Override
    public Node constFold() {
        values.replaceAll(expr -> (Expr) expr.constFold());
        return this;
    }

    public static ArrayLiteral fromStr(Str s){
        ArrayList<Expr> list = new ArrayList<>();
        for (Long l: s.value) {
            list.add(new Int(s.lineNumber, s.columnNumber, l));
        }
        return new ArrayLiteral(s.lineNumber, s.columnNumber, list);
    }
}
