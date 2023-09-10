package zc246_zl345_co232_mw756.ast;

import edu.cornell.cs.cs4120.util.SExpPrinter;
import zc246_zl345_co232_mw756.context.PrimitiveType;
import zc246_zl345_co232_mw756.errors.SemanticError;
import zc246_zl345_co232_mw756.visitor.Visitor;

import java.util.List;

public class Return extends Statement {
    public final List<Expr> values;

    public PrimitiveType[] returnType;

    public Return(int lineNumber, int columnNumber, List<Expr> values) {
        super(lineNumber, columnNumber);
        this.values = values;
    }

    public List<Expr> getValues() {
        return values;
    }

    @Override
    public void printNode(SExpPrinter printer) {
        printer.startUnifiedList();
        printer.printAtom("return");
        for (Expr e : values) {
            e.printNode(printer);
        }
        printer.endList();
    }

    @Override
    public void accept(Visitor v) {
        v.visitReturn(this);
    }

    @Override
    public Node constFold() {
        values.replaceAll(value -> (Expr) value.constFold());
        return this;
    }

    @Override
    public void checkReturn(PrimitiveType[] rets) {
        if (rets.length != returnType.length) {
            throw new SemanticError(lineNumber, columnNumber, "Mismatched number of return values");
        }
        for (int i = 0; i < rets.length; i++) {
            if (!rets[i].equals(returnType[i])) {
                throw new SemanticError(lineNumber, columnNumber, "Expected " + rets[i] + ", but found " + returnType[i]);
            }
        }
    }
}
